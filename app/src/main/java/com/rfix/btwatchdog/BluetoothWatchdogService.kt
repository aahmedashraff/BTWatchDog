package com.rfix.btwatchdog

import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.DataOutputStream

/**
 * Foreground service that listens for Bluetooth ACL disconnect events
 * (the remote "dropping for a couple seconds") and runs a root-level
 * Bluetooth stack reset to recover the connection immediately.
 *
 * Root is required: fixing this reliably needs shell-level commands
 * (svc bluetooth, settings put secure/global) that a normal app process
 * cannot execute on its own.
 */
class BluetoothWatchdogService : Service() {

    companion object {
        var isRunning = false
            private set
    }

    private val TAG = "BTWatchdog"
    private val handler = Handler(Looper.getMainLooper())
    private var receiver: BroadcastReceiver? = null
    private var lastRecoveryAttempt = 0L
    private val COOLDOWN_MS = 8000L

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForegroundNotification()
        registerBluetoothReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        receiver?.let { unregisterReceiver(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerBluetoothReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    Log.w(TAG, "ACL disconnected: ${device?.name ?: "unknown"}")
                    scheduleRecovery()
                }
            }
        }
        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
    }

    private fun scheduleRecovery() {
        val now = System.currentTimeMillis()
        if (now - lastRecoveryAttempt < COOLDOWN_MS) return
        lastRecoveryAttempt = now

        handler.postDelayed({
            runRootRecovery()
        }, 2500)
    }

    /** Root fallback mirroring bt_fix.sh (toggle radio + idle flags). */
    private fun runRootRecovery(): Boolean {
        val commands = listOf(
            "svc bluetooth disable",
            "sleep 1",
            "svc bluetooth enable",
            "dumpsys deviceidle disable",
            "settings put global ble_scan_always_enabled 1",
            "settings put global wifi_scan_always_enabled 1"
        )
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            for (cmd in commands) {
                os.writeBytes("$cmd\n")
            }
            os.writeBytes("exit\n")
            os.flush()
            val exitCode = process.waitFor()
            Log.i(TAG, "Root recovery executed, exit=$exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.w(TAG, "Root recovery failed: ${e.message}")
            false
        }
    }

    private fun startForegroundNotification() {
        val channelId = "btwatchdog_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Bluetooth Watchdog",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Remote connection watchdog active")
            .setContentText("Monitoring Bluetooth remote connection")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }
}
