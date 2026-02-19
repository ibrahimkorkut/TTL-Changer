package com.ttlchanger.app

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class TTLApplication : Application() {

    companion object {
        lateinit var instance: TTLApplication
            private set

        val prefs: SharedPreferences
            get() = PreferenceManager.getDefaultSharedPreferences(instance)

        const val PREF_TTL_VALUE = "ttl_value"
        const val PREF_IPV6_ENABLED = "ipv6_enabled"
        const val PREF_APPLY_ON_BOOT = "apply_on_boot"
        const val PREF_LAST_TTL = "last_ttl_value"

        const val DEFAULT_TTL = 64
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
