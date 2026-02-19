package com.ttlchanger.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ttlchanger.app.RootManager
import com.ttlchanger.app.TTLApplication

/**
 * Broadcast receiver that triggers on device boot to automatically
 * apply the last saved TTL value if the user has enabled this feature.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TTL_BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed detected")

            val prefs = TTLApplication.prefs
            val applyOnBoot = prefs.getBoolean(TTLApplication.PREF_APPLY_ON_BOOT, false)

            if (applyOnBoot) {
                val lastTtl = prefs.getInt(TTLApplication.PREF_LAST_TTL, TTLApplication.DEFAULT_TTL)
                val ipv6Enabled = prefs.getBoolean(TTLApplication.PREF_IPV6_ENABLED, false)

                Log.d(TAG, "Applying TTL on boot: $lastTtl (IPv6: $ipv6Enabled)")

                Thread {
                    // Wait a moment for system to fully boot
                    Thread.sleep(5000)

                    val result = RootManager.applyTTL(lastTtl, ipv6Enabled)
                    Log.d(TAG, "Boot TTL apply result: IPv4=${result.ipv4Applied}, IPv6=${result.ipv6Applied}")
                }.start()
            } else {
                Log.d(TAG, "Apply on boot is disabled, skipping")
            }
        }
    }
}
