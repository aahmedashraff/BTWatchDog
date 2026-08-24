package com.rfix.btwatchdog

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Date
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val REQUIRED_PERMISSIONS = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (REQUIRED_PERMISSIONS.isNotEmpty() &&
            !REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 100)
        }

        findViewById<Button>(R.id.runFixButton).setOnClickListener {
            findViewById<TextView>(R.id.statusRoot).text = "Running fix..."
            thread {
                val success = RootUtils.runBluetoothFix()
                val prefs = getSharedPreferences("starfix_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putLong("last_fix_time", System.currentTimeMillis())
                    .putBoolean("last_fix_success", success)
                    .apply()
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (success) "Fix applied successfully" else "Fix failed — check root access",
                        Toast.LENGTH_SHORT
                    ).show()
                    refreshStatus()
                }
            }
        }

        findViewById<Button>(R.id.checkStatusButton).setOnClickListener {
            refreshStatus(showToast = true)
        }

        refreshStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshStatus()
    }

    private fun refreshStatus(showToast: Boolean = false) {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val btOn = btAdapter?.isEnabled == true
        setDot(R.id.dotBluetooth, btOn)
        findViewById<TextView>(R.id.statusBluetooth).text =
            if (btOn) "Bluetooth: on" else "Bluetooth: off"

        val prefs = getSharedPreferences("starfix_prefs", Context.MODE_PRIVATE)
        val lastFixTime = prefs.getLong("last_fix_time", -1L)
        val lastFixSuccess = prefs.getBoolean("last_fix_success", false)
        findViewById<TextView>(R.id.lastCheckedText).text = if (lastFixTime > 0) {
            val time = DateFormat.format("MMM dd, hh:mm a", Date(lastFixTime))
            "Last automatic fix: $time (${if (lastFixSuccess) "success" else "failed"})"
        } else {
            "No automatic fix has run yet"
        }

        findViewById<TextView>(R.id.statusRoot).text = "Root access: checking..."
        thread {
            val rooted = RootUtils.hasRootAccess()
            runOnUiThread {
                setDot(R.id.dotRoot, rooted)
                findViewById<TextView>(R.id.statusRoot).text =
                    if (rooted) "Root access: granted" else "Root access: not available"
                if (showToast) {
                    val msg = if (rooted) "All checks complete — root OK" else
                        "Root not available — auto-fix won't work on this device"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setDot(viewId: Int, ok: Boolean) {
        val dot = findViewById<View>(viewId)
        val color = if (ok) ContextCompat.getColor(this, R.color.dot_green)
                    else ContextCompat.getColor(this, R.color.dot_red)
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(color)
        dot.background = drawable
    }
}
