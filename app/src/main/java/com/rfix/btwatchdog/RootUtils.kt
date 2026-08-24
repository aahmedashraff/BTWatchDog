package com.rfix.btwatchdog

import java.io.DataOutputStream

object RootUtils {
    fun hasRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("id\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun runBluetoothFix(): Boolean {
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
            for (cmd in commands) os.writeBytes("$cmd\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
