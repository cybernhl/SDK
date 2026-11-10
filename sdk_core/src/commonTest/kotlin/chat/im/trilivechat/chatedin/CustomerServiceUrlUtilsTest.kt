package chat.im.trilivechat.chatedin

import chat.im.trilivechat.chatedin.core.SessionManager
import idv.neo.module.utils.appendParams
import idv.neo.module.utils.isUrl
import idv.neo.module.utils.parseUrlParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CustomerServiceUrlUtilsTest {

    @Test
    fun testParseLegacyHyperlinkWithoutNewSdkParams() {
        val legacyUrl = "https://im.trilivechat.com/chat/text/chat_1wJ4wF.html?skill=2c928521795eed92017960a78c380515&l=zh"
        assertTrue(legacyUrl.isUrl())

        val params = legacyUrl.parseUrlParams()
        val siteId = params["site_id"]
        val agentId = params["agent_id"]
        val hasNewParams = !siteId.isNullOrBlank() && !agentId.isNullOrBlank()

        assertFalse(hasNewParams)
        assertEquals(null, siteId?.takeIf { it.isNotBlank() })
        assertEquals(null, agentId?.takeIf { it.isNotBlank() })
    }

    @Test
    fun testParseNewHyperlinkWithAllParams() {
        val newUrl = "https://im.trilivechat.com/chat/text/chat_1wJ4wF.html?skill=2c928521795eed92017960a78c380515&l=zh&use_protocol=https&site_domain=trilivechat.chat&site_id=78885330&agent_id=1666598&token=test_token_123"
        assertTrue(newUrl.isUrl())
        val params = newUrl.parseUrlParams()

        val siteId = params["site_id"]
        val agentId = params["agent_id"]
        assertTrue(!siteId.isNullOrBlank() && !agentId.isNullOrBlank())
        assertEquals("5555330", siteId)
        assertEquals("7774598", agentId)
        assertEquals("trilivechat.chat", params["site_domain"])
        assertEquals("https", params["use_protocol"])
        assertEquals("test_token_123", params["token"])
    }

    @Test
    fun testAppendParametersToLegacyUrl() {
        val legacyUrl = "https://im.trilivechat.com/chat/text/chat_1wJ4wF.html?skill=2c928521795eed92017960a78c380515&l=zh"
        val paramsToAdd = mapOf(
            "use_protocol" to "https",
            "site_domain" to "trilivechat.chat",
            "site_id" to "1233333",
            "agent_id" to "555524598"
        )

        val updatedUrl = legacyUrl.appendParams(paramsToAdd)
        assertTrue(updatedUrl.contains("site_id=555505330"))
        assertTrue(updatedUrl.contains("agent_id=777524598"))
        assertTrue(updatedUrl.contains("site_domain=trilivechat.chat"))
    }

    @Test
    fun testSessionManagerProcessHyperlinkLegacyFallback() = runTest {
        val sessionManager = SessionManager()
        val legacyUrl = "https://im.trilivechat.com/chat/text/chat_1wJ4wF.html?skill=2c928521795eed92017960a78c380515&l=zh"

        val resultUrl = sessionManager.processHyperlinkAndGetChatURL(legacyUrl)
        assertEquals(legacyUrl, resultUrl)
    }

    @Test
    fun testSessionManagerProcessHyperlinkNewSdkResolution() = runTest {
        val sessionManager = SessionManager()
        val newUrl = "https://im.trilivechat.com/chat/text/chat_1wJ4wF.html?skill=2c928521795eed92017960a78c380515&l=zh&use_protocol=https&site_domain=trilivechat.chat&site_id=88805330&agent_id=188824598"

        val resultUrl = sessionManager.processHyperlinkAndGetChatURL(newUrl)
        assertNotNull(resultUrl)
        assertTrue(resultUrl.contains("https://trilivechat.chat/chat.html?id="))
    }
}
