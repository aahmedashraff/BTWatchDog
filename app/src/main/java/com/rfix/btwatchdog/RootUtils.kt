package com.rfix.btwatchdog

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.provider.Settings

object RootUtils {

    /** As a system app we hold WRITE_SECURE_SETTINGS, so check that instead of su. */
    fun hasSystemAccess(context: Context): Boolean {
        return try {
            Settings.Global.putInt(
                context.contentResolver,
                "ble_scan_always_enabled", 1
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun runBluetoothFix(context: Context): Boolean {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false

            // Toggle the radio using the system-level adapter API
            adapter.disable()
            Thread.sleep(1500)
            adapter.enable()

            // Apply the scan/idle settings directly
            Settings.Global.putInt(context.contentResolver, "ble_scan_always_enabled", 1)
            Settings.Global.putInt(context.contentResolver, "wifi_scan_always_enabled", 1)

            true
        } catch (e: Exception) {
            false
        }
    }
}
