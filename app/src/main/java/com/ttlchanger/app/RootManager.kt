package com.ttlchanger.app

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Utility class for executing root (su) commands and reading/writing TTL values.
 * Requires a rooted device with su binary available.
 */
object RootManager {

    /**
     * Check if root access is available
     */
    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("id\n")
            os.writeBytes("exit\n")
            os.flush()
            os.close()

            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Execute a root command and return its output
     */
    fun executeRootCommand(command: String): CommandResult {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            os.close()

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            val error = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                error.appendLine(line)
            }

            val exitValue = process.waitFor()

            reader.close()
            errorReader.close()

            CommandResult(
                success = exitValue == 0,
                output = output.toString().trim(),
                error = error.toString().trim()
            )
        } catch (e: Exception) {
            CommandResult(
                success = false,
                output = "",
                error = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Read the current IPv4 TTL value
     */
    fun readCurrentTTL(): Int? {
        val result = executeRootCommand("cat /proc/sys/net/ipv4/ip_default_ttl")
        return if (result.success) {
            result.output.trim().toIntOrNull()
        } else {
            null
        }
    }

    /**
     * Read the current IPv6 hop limit
     */
    fun readCurrentIPv6HopLimit(): Int? {
        val result = executeRootCommand("cat /proc/sys/net/ipv6/conf/all/hop_limit")
        return if (result.success) {
            result.output.trim().toIntOrNull()
        } else {
            null
        }
    }

    /**
     * Write IPv4 TTL value
     */
    fun writeTTL(ttlValue: Int): Boolean {
        val result = executeRootCommand("echo $ttlValue > /proc/sys/net/ipv4/ip_default_ttl")
        return result.success
    }

    /**
     * Write IPv6 hop limit
     */
    fun writeIPv6HopLimit(hopLimit: Int): Boolean {
        val result = executeRootCommand("echo $hopLimit > /proc/sys/net/ipv6/conf/all/hop_limit")
        return result.success
    }

    /**
     * Apply TTL for both IPv4 and optionally IPv6
     */
    fun applyTTL(ttlValue: Int, includeIPv6: Boolean): ApplyResult {
        val ipv4Success = writeTTL(ttlValue)

        var ipv6Success = true
        if (includeIPv6) {
            ipv6Success = writeIPv6HopLimit(ttlValue)
        }

        // Also set iptables rules for persistence during the session
        executeRootCommand("iptables -t mangle -A POSTROUTING -j TTL --ttl-set $ttlValue")

        if (includeIPv6) {
            executeRootCommand("ip6tables -t mangle -A POSTROUTING -j HL --hl-set $ttlValue")
        }

        return ApplyResult(
            ipv4Applied = ipv4Success,
            ipv6Applied = if (includeIPv6) ipv6Success else null
        )
    }

    /**
     * Reset TTL iptables rules
     */
    fun resetIPTablesRules(): Boolean {
        val result1 = executeRootCommand("iptables -t mangle -F POSTROUTING")
        val result2 = executeRootCommand("ip6tables -t mangle -F POSTROUTING")
        return result1.success && result2.success
    }

    data class CommandResult(
        val success: Boolean,
        val output: String,
        val error: String
    )

    data class ApplyResult(
        val ipv4Applied: Boolean,
        val ipv6Applied: Boolean?
    )
}
