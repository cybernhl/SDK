package chat.im.trilivechat.chatedin.core

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.Url

/**
 * DomainRedirectPlugin 的配置類別。
 * 供 `install(DomainRedirectPlugin)` 時傳入動態取得當前活躍 Domain 的 Lambda 回調。
 */
class DomainPluginConfig {
    /** 傳回當前活躍 Domain 的 Lambda 函式（例如：{ currentDomain }） */
    var activeDomainProvider: () -> String = { "" }
}

/**
 * 客製化 Ktor Client 外掛 (DomainRedirectPlugin)。
 * 在 HTTP 出站 Request 階段攔截請求，將協議 (protocol)、主機 (host) 與埠號 (port)
 * 動態覆寫為當前活躍的 Domain，實現即時動態網址重定向與 Failover 轉發。
 */
val DomainRedirectPlugin = createClientPlugin("DomainRedirectPlugin", ::DomainPluginConfig) {
    val domainProvider = pluginConfig.activeDomainProvider

    onRequest { request, _ ->
        val currentDomain = domainProvider()
        if (currentDomain.isNotBlank()) {
            try {
                val targetUrl = Url(if (currentDomain.endsWith("/")) currentDomain else "$currentDomain/")
                request.url.protocol = targetUrl.protocol
                request.url.host = targetUrl.host
                request.url.port = targetUrl.port
            } catch (_: Exception) {
                // 若解析失敗保留原始 URL
            }
        }
    }
}
