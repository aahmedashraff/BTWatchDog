package com.rfix.btwatchdog

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (hasPermissions()) {
                startWatchdog()
                refreshStatus()
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 100)
            }
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            stopService(Intent(this, BluetoothWatchdogService::class.java))
            refreshStatus()
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
        if (hasPermissions()) {
            startWatchdog()
            refreshStatus()
        }
    }

    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startWatchdog() {
        val intent = Intent(this, BluetoothWatchdogService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun refreshStatus(showToast: Boolean = false) {
        // Service state — instant, no background thread needed
        val serviceRunning = BluetoothWatchdogService.isRunning
        setDot(R.id.dotService, serviceRunning)
        findViewById<TextView>(R.id.statusService).text =
            if (serviceRunning) "Watchdog: running" else "Watchdog: stopped"

        // Bluetooth adapter state — instant
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val btOn = btAdapter?.isEnabled == true
        setDot(R.id.dotBluetooth, btOn)
        findViewById<TextView>(R.id.statusBluetooth).text =
            if (btOn) "Bluetooth: on" else "Bluetooth: off"

        // Root check can block briefly — run off the main thread
        findViewById<TextView>(R.id.statusRoot).text = "Root access: checking..."
        thread {
            val rooted = RootUtils.hasRootAccess()
            runOnUiThread {
                setDot(R.id.dotRoot, rooted)
                findViewById<TextView>(R.id.statusRoot).text =
                    if (rooted) "Root access: granted" else "Root access: not available"

                val now = DateFormat.format("hh:mm:ss a", Date())
                findViewById<TextView>(R.id.lastCheckedText).text = "Last checked: $now"

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
