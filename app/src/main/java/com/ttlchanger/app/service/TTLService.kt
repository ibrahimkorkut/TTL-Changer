package com.ttlchanger.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ttlchanger.app.RootManager
import com.ttlchanger.app.TTLApplication

/**
 * Background service for applying TTL values.
 * Used by widgets and boot receiver for background operations.
 */
class TTLService : Service() {

    companion object {
        private const val TAG = "TTLService"
        const val EXTRA_TTL_VALUE = "extra_ttl_value"
        const val EXTRA_IPV6_ENABLED = "extra_ipv6_enabled"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ttlValue = intent?.getIntExtra(EXTRA_TTL_VALUE, -1) ?: -1
        val ipv6Enabled = intent?.getBooleanExtra(EXTRA_IPV6_ENABLED, false) ?: false

        if (ttlValue in 1..255) {
            Thread {
                Log.d(TAG, "Applying TTL: $ttlValue (IPv6: $ipv6Enabled)")
                val result = RootManager.applyTTL(ttlValue, ipv6Enabled)
                Log.d(TAG, "TTL apply result: IPv4=${result.ipv4Applied}, IPv6=${result.ipv6Applied}")
                stopSelf(startId)
            }.start()
        } else {
            Log.w(TAG, "Invalid TTL value: $ttlValue")
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }
}
