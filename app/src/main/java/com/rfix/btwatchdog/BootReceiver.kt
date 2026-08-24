package com.rfix.btwatchdog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val success = RootUtils.runBluetoothFix()
            val prefs = context.getSharedPreferences("starfix_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("last_fix_time", System.currentTimeMillis())
                .putBoolean("last_fix_success", success)
                .apply()
        }
    }
}
