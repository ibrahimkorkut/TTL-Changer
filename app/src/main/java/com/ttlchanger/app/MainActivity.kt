package com.ttlchanger.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var currentTtlText: TextView
    private lateinit var currentIpv6Text: TextView
    private lateinit var ttlInput: TextInputEditText
    private lateinit var ttlInputLayout: TextInputLayout
    private lateinit var ipv6Switch: MaterialSwitch
    private lateinit var bootApplySwitch: MaterialSwitch
    private lateinit var applyButton: Button
    private lateinit var readButton: Button
    private lateinit var resetButton: Button
    private lateinit var statusText: TextView
    private lateinit var rootStatusText: TextView
    private lateinit var rootStatusIcon: ImageView
    private lateinit var rootStatusCard: MaterialCardView
    private lateinit var progressBar: ProgressBar
    private lateinit var ipv6Card: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadPreferences()
        checkRootAccess()
        readCurrentTTL()
    }

    private fun initViews() {
        currentTtlText = findViewById(R.id.tv_current_ttl)
        currentIpv6Text = findViewById(R.id.tv_current_ipv6)
        ttlInput = findViewById(R.id.et_ttl_value)
        ttlInputLayout = findViewById(R.id.til_ttl_value)
        ipv6Switch = findViewById(R.id.switch_ipv6)
        bootApplySwitch = findViewById(R.id.switch_boot_apply)
        applyButton = findViewById(R.id.btn_apply)
        readButton = findViewById(R.id.btn_read)
        resetButton = findViewById(R.id.btn_reset)
        statusText = findViewById(R.id.tv_status)
        rootStatusText = findViewById(R.id.tv_root_status)
        rootStatusIcon = findViewById(R.id.iv_root_status)
        rootStatusCard = findViewById(R.id.card_root_status)
        progressBar = findViewById(R.id.progress_bar)
        ipv6Card = findViewById(R.id.card_ipv6_info)

        // Quick TTL preset buttons
        val presetButtons = listOf(
            findViewById<Button>(R.id.btn_preset_64),
            findViewById<Button>(R.id.btn_preset_65),
            findViewById<Button>(R.id.btn_preset_128),
            findViewById<Button>(R.id.btn_preset_255)
        )

        val presetValues = listOf(64, 65, 128, 255)

        presetButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                ttlInput.setText(presetValues[index].toString())
            }
        }

        // Apply button
        applyButton.setOnClickListener {
            applyTTL()
        }

        // Read button
        readButton.setOnClickListener {
            readCurrentTTL()
        }

        // Reset button
        resetButton.setOnClickListener {
            resetTTL()
        }

        // IPv6 switch listener
        ipv6Switch.setOnCheckedChangeListener { _, isChecked ->
            TTLApplication.prefs.edit()
                .putBoolean(TTLApplication.PREF_IPV6_ENABLED, isChecked)
                .apply()

            ipv6Card.visibility = if (isChecked) View.VISIBLE else View.GONE

            if (isChecked) {
                readIPv6HopLimit()
            }
        }

        // Boot apply switch listener
        bootApplySwitch.setOnCheckedChangeListener { _, isChecked ->
            TTLApplication.prefs.edit()
                .putBoolean(TTLApplication.PREF_APPLY_ON_BOOT, isChecked)
                .apply()

            val message = if (isChecked) {
                "TTL will be applied automatically on boot"
            } else {
                "Auto-apply on boot disabled"
            }
            showSnackbar(message)
        }
    }

    private fun loadPreferences() {
        val prefs = TTLApplication.prefs

        val ipv6Enabled = prefs.getBoolean(TTLApplication.PREF_IPV6_ENABLED, false)
        val bootApply = prefs.getBoolean(TTLApplication.PREF_APPLY_ON_BOOT, false)
        val lastTtl = prefs.getInt(TTLApplication.PREF_LAST_TTL, TTLApplication.DEFAULT_TTL)

        ipv6Switch.isChecked = ipv6Enabled
        bootApplySwitch.isChecked = bootApply
        ttlInput.setText(lastTtl.toString())

        ipv6Card.visibility = if (ipv6Enabled) View.VISIBLE else View.GONE
    }

    private fun checkRootAccess() {
        Thread {
            val hasRoot = RootManager.isRootAvailable()
            runOnUiThread {
                if (hasRoot) {
                    rootStatusText.text = "Root access granted"
                    rootStatusText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
                    rootStatusIcon.setImageResource(R.drawable.ic_check_circle)
                    rootStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.success_green))
                    enableControls(true)
                } else {
                    rootStatusText.text = "Root access denied — App requires root!"
                    rootStatusText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
                    rootStatusIcon.setImageResource(R.drawable.ic_error)
                    rootStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_red))
                    enableControls(false)
                }
            }
        }.start()
    }

    private fun enableControls(enabled: Boolean) {
        applyButton.isEnabled = enabled
        readButton.isEnabled = enabled
        resetButton.isEnabled = enabled
        ttlInput.isEnabled = enabled
        ipv6Switch.isEnabled = enabled
        bootApplySwitch.isEnabled = enabled
    }

    private fun readCurrentTTL() {
        showProgress(true)
        Thread {
            val ttl = RootManager.readCurrentTTL()
            val ipv6Hop = if (ipv6Switch.isChecked) RootManager.readCurrentIPv6HopLimit() else null

            runOnUiThread {
                showProgress(false)
                if (ttl != null) {
                    currentTtlText.text = ttl.toString()
                    statusText.text = "Current TTL read successfully"
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
                } else {
                    currentTtlText.text = "N/A"
                    statusText.text = "Failed to read TTL"
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
                }

                if (ipv6Hop != null) {
                    currentIpv6Text.text = ipv6Hop.toString()
                }
            }
        }.start()
    }

    private fun readIPv6HopLimit() {
        Thread {
            val ipv6Hop = RootManager.readCurrentIPv6HopLimit()
            runOnUiThread {
                if (ipv6Hop != null) {
                    currentIpv6Text.text = ipv6Hop.toString()
                }
            }
        }.start()
    }

    private fun applyTTL() {
        val ttlStr = ttlInput.text?.toString()?.trim()
        if (ttlStr.isNullOrEmpty()) {
            ttlInputLayout.error = "Please enter a TTL value"
            return
        }

        val ttlValue = ttlStr.toIntOrNull()
        if (ttlValue == null || ttlValue < 1 || ttlValue > 255) {
            ttlInputLayout.error = "TTL must be between 1 and 255"
            return
        }

        ttlInputLayout.error = null
        showProgress(true)

        Thread {
            val includeIPv6 = ipv6Switch.isChecked
            val result = RootManager.applyTTL(ttlValue, includeIPv6)

            // Save last used TTL value
            TTLApplication.prefs.edit()
                .putInt(TTLApplication.PREF_LAST_TTL, ttlValue)
                .apply()

            runOnUiThread {
                showProgress(false)

                val message = buildString {
                    if (result.ipv4Applied) {
                        append("✓ IPv4 TTL set to $ttlValue")
                    } else {
                        append("✗ Failed to set IPv4 TTL")
                    }
                    result.ipv6Applied?.let { ipv6 ->
                        append("\n")
                        if (ipv6) {
                            append("✓ IPv6 Hop Limit set to $ttlValue")
                        } else {
                            append("✗ Failed to set IPv6 Hop Limit")
                        }
                    }
                }

                statusText.text = message
                if (result.ipv4Applied) {
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
                    currentTtlText.text = ttlValue.toString()
                    if (includeIPv6 && result.ipv6Applied == true) {
                        currentIpv6Text.text = ttlValue.toString()
                    }
                } else {
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
                }

                showSnackbar("TTL applied: $ttlValue")
            }
        }.start()
    }

    private fun resetTTL() {
        showProgress(true)
        Thread {
            val defaultTtl = TTLApplication.DEFAULT_TTL
            val includeIPv6 = ipv6Switch.isChecked

            RootManager.resetIPTablesRules()
            val result = RootManager.applyTTL(defaultTtl, includeIPv6)

            runOnUiThread {
                showProgress(false)
                if (result.ipv4Applied) {
                    currentTtlText.text = defaultTtl.toString()
                    ttlInput.setText(defaultTtl.toString())
                    statusText.text = "TTL reset to default ($defaultTtl)"
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
                    showSnackbar("TTL reset to $defaultTtl")
                } else {
                    statusText.text = "Failed to reset TTL"
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
                }
            }
        }.start()
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(R.id.root_layout), message, Snackbar.LENGTH_SHORT).show()
    }
}
