package com.ttlchanger.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ttlchanger.app.R
import com.ttlchanger.app.RootManager
import com.ttlchanger.app.TTLApplication

/**
 * One-tap widget that displays the current TTL value and allows
 * users to apply their saved TTL with a single tap.
 */
class TTLWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_TAP = "com.ttlchanger.app.ACTION_WIDGET_TAP"

        fun updateWidget(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TTLWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = TTLApplication.prefs
            val lastTtl = prefs.getInt(TTLApplication.PREF_LAST_TTL, TTLApplication.DEFAULT_TTL)

            val views = RemoteViews(context.packageName, R.layout.widget_ttl)
            views.setTextViewText(R.id.widget_ttl_value, lastTtl.toString())

            // Set up tap action
            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = "com.ttlchanger.app.ACTION_APPLY_TTL"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }
}
