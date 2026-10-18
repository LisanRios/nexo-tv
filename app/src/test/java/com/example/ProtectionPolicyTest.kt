package com.example

import android.net.Uri
import com.example.domain.model.SitePolicy
import com.example.protection.DomainListRequestFilter
import com.example.protection.HostNormalizer
import com.example.protection.NavigationDecision
import com.example.protection.StrictProtectionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProtectionPolicyTest {

    @Test
    fun testHostNormalizer_exactAndSubdomainMatches() {
        val rule = "ads.example.test"

        // 1. Exact match
        assertTrue(HostNormalizer.matchesRule("ads.example.test", rule))

        // 2. Subdomain with dot boundary
        assertTrue(HostNormalizer.matchesRule("x.ads.example.test", rule))
        assertTrue(HostNormalizer.matchesRule("deep.sub.ads.example.test", rule))

        // 3. Deceptive prefixes without dot boundary MUST NOT match
        assertFalse(HostNormalizer.matchesRule("notads.example.test", rule))

        // 4. Deceptive suffixes MUST NOT match
        assertFalse(HostNormalizer.matchesRule("ads.example.test.evil.test", rule))
    }

    @Test
    fun testNavigationDecision_blocksDangerousSchemes() {
        val engine = StrictProtectionEngine()

        val intentUri = Uri.parse("intent://play.google.com/store/apps/details?id=com.evil#Intent;scheme=market;action=android.intent.action.VIEW;end")
        val decision = engine.evaluateNavigation(intentUri, "tvlibre.st", null)
        assertTrue(decision is NavigationDecision.Block)

        val fileUri = Uri.parse("file:///data/data/com.aistudio.nexotv.wkqspz/databases/nexotv.db")
        val fileDecision = engine.evaluateNavigation(fileUri, "tvlibre.st", null)
        assertTrue(fileDecision is NavigationDecision.Block)

        val jsUri = Uri.parse("javascript:alert(1)")
        val jsDecision = engine.evaluateNavigation(jsUri, "tvlibre.st", null)
        assertTrue(jsDecision is NavigationDecision.Block)
    }

    @Test
    fun testNavigationDecision_containsMainDocumentOrigin() {
        val engine = StrictProtectionEngine()

        // Same origin navigation is allowed
        val sameOriginUri = Uri.parse("https://tvlibre.st/agenda/partido-123")
        val sameDecision = engine.evaluateNavigation(sameOriginUri, "tvlibre.st", null)
        assertTrue(sameDecision is NavigationDecision.Allow)

        // Escape to unauthorized foreign domain is blocked
        val foreignUri = Uri.parse("https://malicious-redirect-network.com/landing")
        val foreignDecision = engine.evaluateNavigation(foreignUri, "tvlibre.st", null)
        assertTrue(foreignDecision is NavigationDecision.Block)

        // Destination explicitly authorized in SitePolicy is allowed
        val policy = SitePolicy(
            originHost = "tvlibre.st",
            authorizedNavigationHosts = listOf("cdn.sportsbroadcast.net")
        )
        val cdnUri = Uri.parse("https://cdn.sportsbroadcast.net/live/stream1")
        val cdnDecision = engine.evaluateNavigation(cdnUri, "tvlibre.st", policy)
        assertTrue(cdnDecision is NavigationDecision.Allow)
    }

    @Test
    fun testRequestFilter_atomicReplacementMaintainsValidRulesOnFailure() {
        val initialRules = setOf("ad1.com", "ad2.com", "ad3.com", "ad4.com", "ad5.com", "ad6.com", "ad7.com", "ad8.com", "ad9.com", "ad10.com")
        val filter = DomainListRequestFilter(initialRules)
        assertEquals(10, filter.getRuleCount())

        // Attempting to replace with empty list should fail and preserve existing rules
        val failedEmpty = filter.updateRules(emptySet())
        assertFalse(failedEmpty)
        assertEquals(10, filter.getRuleCount())

        // Attempting to replace with fewer than 10 rules (suspicious/truncated) should fail
        val failedFew = filter.updateRules(setOf("single.ad.com"))
        assertFalse(failedFew)
        assertEquals(10, filter.getRuleCount())
    }
}
