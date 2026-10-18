package com.example.protection

import android.net.Uri
import java.net.IDN
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

data class FilterDecision(
    val shouldBlock: Boolean,
    val matchedRule: String? = null,
    val reason: String? = null
)

interface RequestFilter {
    fun shouldBlockResource(url: Uri, mainDocumentHost: String?): FilterDecision
    fun getRuleCount(): Int
    fun updateRules(newRules: Set<String>): Boolean
}

/**
 * Normalizes host strings per RFC standards:
 * - lowercase
 * - trimmed trailing dots
 * - Punycode (IDN) conversion
 */
object HostNormalizer {
    fun normalize(rawHost: String?): String {
        if (rawHost.isNullOrBlank()) return ""
        val trimmed = rawHost.trim().trimEnd('.').lowercase(Locale.ROOT)
        return try {
            IDN.toASCII(trimmed)
        } catch (_: Exception) {
            trimmed
        }
    }

    /**
     * Checks if targetHost matches ruleHost exactly or as a proper subdomain.
     * Rule: "ads.example.test" matches:
     * - "ads.example.test" -> TRUE
     * - "sub.ads.example.test" -> TRUE
     * - "notads.example.test" -> FALSE (boundary required)
     * - "ads.example.test.evil.test" -> FALSE
     */
    fun matchesRule(targetHost: String, ruleHost: String): Boolean {
        if (targetHost.isEmpty() || ruleHost.isEmpty()) return false
        if (targetHost == ruleHost) return true
        if (targetHost.endsWith(".$ruleHost")) return true
        return false
    }
}

class DomainListRequestFilter(
    initialBlockedDomains: Set<String> = emptySet()
) : RequestFilter {

    private val rulesRef = AtomicReference<Set<String>>(
        initialBlockedDomains.map { HostNormalizer.normalize(it) }.filter { it.isNotBlank() }.toSet()
    )

    override fun shouldBlockResource(url: Uri, mainDocumentHost: String?): FilterDecision {
        val host = HostNormalizer.normalize(url.host)
        if (host.isEmpty()) {
            val scheme = url.scheme?.lowercase(Locale.ROOT)
            if (scheme != "http" && scheme != "https" && scheme != "data" && scheme != "blob") {
                return FilterDecision(shouldBlock = true, matchedRule = scheme, reason = "Esquema no permitido ($scheme)")
            }
            return FilterDecision(shouldBlock = false)
        }

        // Check against blocked hosts set
        val currentRules = rulesRef.get()
        if (currentRules.contains(host)) {
            return FilterDecision(shouldBlock = true, matchedRule = host, reason = "Dominio publicitario/rastreador bloqueado: $host")
        }

        // Subdomain check: iterate parts or check parents
        var dotIndex = host.indexOf('.')
        while (dotIndex != -1) {
            val parentHost = host.substring(dotIndex + 1)
            if (currentRules.contains(parentHost)) {
                return FilterDecision(
                    shouldBlock = true,
                    matchedRule = parentHost,
                    reason = "Subdominio de regla bloqueada: $parentHost"
                )
            }
            dotIndex = host.indexOf('.', dotIndex + 1)
        }

        return FilterDecision(shouldBlock = false)
    }

    override fun getRuleCount(): Int = rulesRef.get().size

    override fun updateRules(newRules: Set<String>): Boolean {
        if (newRules.isEmpty()) return false
        val normalized = newRules
            .map { HostNormalizer.normalize(it) }
            .filter { it.isNotBlank() && it.contains('.') && !it.startsWith("#") }
            .toSet()

        if (normalized.size < 10) {
            // Suspicious or invalid list; keep previous valid rules
            return false
        }
        rulesRef.set(normalized)
        return true
    }
}
