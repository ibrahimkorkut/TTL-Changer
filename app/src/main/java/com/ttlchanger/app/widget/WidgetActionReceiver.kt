package com.ttlchanger.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.ttlchanger.app.RootManager
import com.ttlchanger.app.TTLApplication

/**
 * Receives tap actions from the TTL widget and applies the saved TTL value.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WidgetAction"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.ttlchanger.app.ACTION_APPLY_TTL") {
            val prefs = TTLApplication.prefs
            val ttlValue = prefs.getInt(TTLApplication.PREF_LAST_TTL, TTLApplication.DEFAULT_TTL)
            val ipv6Enabled = prefs.getBoolean(TTLApplication.PREF_IPV6_ENABLED, false)

            Log.d(TAG, "Widget tap: Applying TTL $ttlValue (IPv6: $ipv6Enabled)")

            Thread {
                val result = RootManager.applyTTL(ttlValue, ipv6Enabled)

                android.os.Handler(context.mainLooper).post {
                    if (result.ipv4Applied) {
                        Toast.makeText(context, "TTL set to $ttlValue ✓", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to apply TTL", Toast.LENGTH_SHORT).show()
                    }

                    // Update the widget display
                    TTLWidgetProvider.updateWidget(context)
                }
            }.start()
        }
    }
}
