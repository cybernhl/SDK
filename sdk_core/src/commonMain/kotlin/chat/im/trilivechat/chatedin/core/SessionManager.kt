package chat.im.trilivechat.chatedin.core

import chat.im.trilivechat.chatedin.api.TriliveChatApi
import chat.im.trilivechat.chatedin.api.createTriliveChatApi
import chat.im.trilivechat.chatedin.state.SessionState
import de.jensklingenberg.ktorfit.Ktorfit
import idv.neo.entity.cs.trilivechat.request.BindTokenRequest
import idv.neo.entity.cs.trilivechat.request.ChatRequest
import idv.neo.entity.cs.trilivechat.request.CreateSessionRequest
import idv.neo.entity.cs.trilivechat.request.GreetRequest
import idv.neo.entity.cs.trilivechat.response.BindTokenResponse
import idv.neo.entity.cs.trilivechat.response.ChatResponse
import idv.neo.entity.cs.trilivechat.response.GreetResponse
import idv.neo.entity.cs.trilivechat.response.SessionDetailResponse
import idv.neo.module.utils.isUrl
import idv.neo.module.utils.parseUrlParams
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * 集團業務用客服系統 - 會話與流程管理器 (SessionManager)。
 * 支援不使用 DI 直接實例化、自訂 `HttpClient`、動態多 Domain 切換、Flag 強制切換與自動容錯移轉 (Failover) 機制。
 * 透過 Ktor `DomainRedirectPlugin` 在 HttpClient 請求層級動態覆寫主機網址，確保 Domain 切換即時生效。
 */
class SessionManager(
    domains: List<String> = listOf("https://trilivechat.chat/"),
    private val httpClient: HttpClient? = null,
    private var customApi: TriliveChatApi? = null
) : SessionProvider {

    constructor(
        baseUrl: String,
        api: TriliveChatApi? = null
    ) : this(
        domains = listOf(baseUrl),
        httpClient = null,
        customApi = api
    )

    private val _domains = MutableStateFlow(normalizeDomains(domains))
    val domainsState: StateFlow<List<String>> = _domains.asStateFlow()

    private var activeDomainIndex = 0

    val currentDomain: String
        get() = _domains.value.getOrElse(activeDomainIndex) { "https://trilivechat.chat/" }

    private val effectiveHttpClient: HttpClient by lazy {
        httpClient ?: createDefaultHttpClient { currentDomain }
    }

    private var api: TriliveChatApi = customApi ?: createApiForDomain(currentDomain, effectiveHttpClient)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Uninitialized)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var currentConfig: LiveChatConfig? = null
    private val mutex = Mutex()

    /**
     * 動態更新 Domain 列表（含主 Domain 與備用 Domain 列表）
     */
    fun updateDomains(newDomains: List<String>) {
        if (newDomains.isEmpty()) return
        val normalized = normalizeDomains(newDomains)
        _domains.value = normalized
        activeDomainIndex = 0
        rebuildApi()
    }

    /**
     * 動態切換單一 Base URL
     */
    fun updateBaseUrl(newBaseUrl: String) {
        updateDomains(listOf(newBaseUrl))
    }

    /**
     * 強制 Flag 切換至下一個備用 Domain
     * @return 切換後的新 Domain 網址
     */
    fun forceSwitchDomain(): String {
        val list = _domains.value
        if (list.size > 1) {
            val oldDomain = currentDomain
            activeDomainIndex = (activeDomainIndex + 1) % list.size
            println("[TriliveChatSDK] Force Domain Switch Flag Triggered! Switched from $oldDomain to $currentDomain")
            rebuildApi()
        } else {
            println("[TriliveChatSDK] Force Domain Switch requested, but only 1 domain configured ($currentDomain).")
        }
        return currentDomain
    }

    /**
     * 標記目前 Domain 失效並強制觸發 Domain 替換
     */
    fun markCurrentDomainFailed(reason: String? = null) {
        println("[TriliveChatSDK] Marking domain failed ($currentDomain). Reason: $reason")
        forceSwitchDomain()
    }

    private fun rebuildApi() {
        if (customApi == null) {
            api = createApiForDomain(currentDomain, effectiveHttpClient)
        }
    }

    /**
     * 判斷是否屬於需要進行 Domain 容錯移轉 (Failover) 的網路/伺服器層級異常
     */
    private fun isDomainFailure(e: Throwable): Boolean {
        return when (e) {
            is HttpRequestTimeoutException,
            is SocketTimeoutException,
            is ServerResponseException -> true // 5xx Server Errors (502, 503, 504)
            else -> {
                val msg = e.message?.lowercase() ?: ""
                msg.contains("connect") || msg.contains("timeout") || msg.contains("unreachable") || msg.contains("host")
            }
        }
    }

    /**
     * 執行具備自動 Domain Failover (容錯移轉) 的 API 呼叫。
     * 若目前的 Domain 請求遇到網路/伺服器層異常，會自動觸發切換至備用 Domain 並重試。
     */
    private suspend fun <T> executeWithFailover(block: suspend (api: TriliveChatApi) -> T): T {
        val domainList = _domains.value
        val totalDomains = domainList.size
        var lastException: Exception? = null

        for (attempt in 0 until totalDomains) {
            val targetDomain = currentDomain
            try {
                return block(api)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastException = e

                if (isDomainFailure(e) && totalDomains > 1) {
                    println("[TriliveChatSDK] Domain $targetDomain failed (${e.message}, attempt=$attempt). Triggering Failover...")
                    forceSwitchDomain()
                } else {
                    // 非 Domain 層級錯誤（如業務邏輯拒絕），不盲目切換
                    throw e
                }
            }
        }
        throw lastException ?: IllegalStateException("All configured domains failed.")
    }

    override fun getActiveSessionId(): String? {
        return (_sessionState.value as? SessionState.Active)?.sessionId
    }

    override fun updateConfig(config: LiveChatConfig) {
        currentConfig = config
    }

    override fun getConfig(): LiveChatConfig? = currentConfig

    override suspend fun requireSessionId(forceNew: Boolean): String {
        if (!forceNew) {
            getActiveSessionId()?.let { return it }
        }

        val config = currentConfig
            ?: throw IllegalStateException("LiveChatConfig is not initialized. Please call updateConfig or provide siteId and agentId.")

        return mutex.withLock {
            if (!forceNew) {
                getActiveSessionId()?.let { return it }
            }

            println("[TriliveChatSDK] Initializing Session (siteId=${config.siteId}, agentId=${config.agentId}, domain=$currentDomain)...")
            _sessionState.value = SessionState.Initializing
            try {
                val response = executeWithFailover { api ->
                    api.createSession(
                        CreateSessionRequest(
                            siteId = config.siteId,
                            agentId = config.agentId,
                            token = config.token
                        )
                    )
                }

                val sid = response.sessionId
                if (response.success && !sid.isNullOrBlank()) {
                    val isBound = !config.token.isNullOrBlank()
                    println("[TriliveChatSDK] Session Created Successfully: $sid (isBound=$isBound, domain=$currentDomain)")
                    _sessionState.value = SessionState.Active(
                        sessionId = sid,
                        isBound = isBound
                    )
                    sid
                } else {
                    val errorMsg = response.detail ?: "Failed to create session"
                    println("[TriliveChatSDK] Session Creation Failed: $errorMsg")
                    _sessionState.value = SessionState.Error(errorMsg)
                    throw IllegalStateException(errorMsg)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("[TriliveChatSDK] Session Creation Exception: ${e.message}")
                _sessionState.value = SessionState.Error(e.message ?: "Network error creating session")
                throw e
            }
        }
    }

    override suspend fun bindToken(token: String): BindTokenResponse {
        val currentSid = requireSessionId()
        println("[TriliveChatSDK] Binding Token for Session $currentSid...")
        return try {
            val response = executeWithFailover { api ->
                api.bindToken(
                    sessionId = currentSid,
                    request = BindTokenRequest(token = token)
                )
            }

            if (response.success) {
                println("[TriliveChatSDK] Token Bound Successfully: username=${response.username}")
                _sessionState.value = SessionState.Active(
                    sessionId = currentSid,
                    isBound = true,
                    username = response.username
                )
                currentConfig = currentConfig?.copy(token = token)
            } else {
                println("[TriliveChatSDK] Token Binding Failed: detail=${response.detail}")
            }
            response
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            println("[TriliveChatSDK] Token Binding Exception: ${e.message}")
            BindTokenResponse(success = false, detail = e.message)
        }
    }

    override fun clearSession() {
        println("[TriliveChatSDK] Session Cleared")
        _sessionState.value = SessionState.Uninitialized
    }

    /**
     * 觸發開場問候 (`/webhook/greet`)
     */
    suspend fun getGreeting(): GreetResponse {
        val sid = requireSessionId()
        return executeWithFailover { api ->
            api.getGreeting(GreetRequest(sessionId = sid))
        }
    }

    /**
     * 發送聊天訊息 (`/webhook/chat`)
     */
    suspend fun sendChat(chatInput: String): ChatResponse {
        val sid = requireSessionId()
        return executeWithFailover { api ->
            api.sendChat(
                ChatRequest(
                    chatInput = chatInput,
                    sessionId = sid,
                    metadata = mapOf("userAgent" to "TriliveChatSDK KMP")
                )
            )
        }
    }

    /**
     * 獲取會話詳情 (`/api/client/sessions/{sessionId}`)
     */
    suspend fun fetchSessionDetail(sessionId: String): SessionDetailResponse {
        return executeWithFailover { api ->
            api.getSessionDetail(sessionId = sessionId)
        }
    }

    /**
     * 處理傳入的 Hyperlink 並回傳供外部瀏覽器開啟的最終客服對話 URL。
     *
     * 相容性與驗證機制：
     * 1. 檢查 `rawUrl.isUrl()` 是否為合法的絕對網址。
     * 2. 透過 [parseUrlParams] 解析 Query 參數：
     *    - 主要必須 Key：`site_id` 與 `agent_id`（必須兩者皆存在且不為空白）
     *    - 次要必須 Key：`site_domain` 與 `use_protocol`（若存在，自動更換 SessionManager 的 Base Domain）
     *    - 選填 Key：`token` (或 `player_token`)、`session_id` (或 `id`)
     * 3. 若為符合條件的新版客服網址，自動進行生命週期驗證與 Session 建立，並回傳格式化後的有效網址。
     * 4. 若不包含主要必須 Key（或非標準 URL），則原樣回傳 `rawUrl` 供舊版流程開啟。
     *
     * @param rawUrl 傳入的原始 Hyperlink 網址
     * @return 供外部系統瀏覽器開啟的最終網址
     */
    suspend fun processHyperlinkAndGetChatURL(rawUrl: String): String {
        if (!rawUrl.isUrl()) {
            return rawUrl
        }

        val params = rawUrl.parseUrlParams()

        // 1. 主要必須 Key：site_id 與 agent_id
        val siteId = params["site_id"]
        val agentId = params["agent_id"]

        if (siteId.isNullOrBlank() || agentId.isNullOrBlank()) {
            return rawUrl
        }

        // 2. 次要必須 Key：site_domain 與 use_protocol
        val siteDomain = params["site_domain"]
        val useProtocol = params["use_protocol"]

        if (!siteDomain.isNullOrBlank()) {
            val protocol = useProtocol?.ifBlank { "https" } ?: "https"
            val cleanDomain = siteDomain.removePrefix("http://").removePrefix("https://").removeSuffix("/")
            val newBaseDomain = "$protocol://$cleanDomain/"
            updateBaseUrl(newBaseDomain)
        }

        val token = params["token"] ?: params["player_token"]
        val existingSessionId = params["session_id"] ?: params["id"]

        return getChatSessionURL(
            siteId = siteId,
            agentId = agentId,
            token = token,
            existingSessionId = existingSessionId
        )
    }

    /**
     * 取得供系統瀏覽器開啟的客服對話完整網址。
     *
     * 根據文件「會話生命週期」與「動態網址」規範：
     * 1. 支援傳入 `existingSessionId` 或檢查當前 Session。
     * 2. 自動向後端確認該 Session 是否仍然有效 (`GET /api/client/sessions/{id}`)，若目前 Domain 失敗會自動嘗試備用 Domain。
     * 3. 若有效則直接復用；若已過期或失效（如 404 / 異常），將自動重新呼叫 `createSession` 取得新的 `sessionId`。
     * 4. 回傳格式如：`https://active-domain.chat/chat.html?id=<sessionId>`
     *
     * @param siteId 商戶站台 ID（必填）
     * @param agentId 客服專員 ID（必填）
     * @param token 玩家驗證 Token（選填）
     * @param existingSessionId 現有的 Session ID（選填）
     * @return 完整的聊天頁面網址
     */
    suspend fun getChatSessionURL(
        siteId: String,
        agentId: String,
        token: String? = null,
        existingSessionId: String? = null
    ): String {
        updateConfig(LiveChatConfig(siteId = siteId, agentId = agentId, token = token))

        var validSessionId: String? = null

        // 1. 檢查傳入或已存在的 Session 是否有效 (具備 Domain Failover)
        val targetSid = existingSessionId ?: getActiveSessionId()
        if (!targetSid.isNullOrBlank()) {
            try {
                val detail = executeWithFailover { api -> api.getSessionDetail(sessionId = targetSid) }
                println("[TriliveChatSDK] SessionDetailResponse for $targetSid: success=${detail.success}, status=${detail.status}, detail=${detail.detail}")
                if (detail.success) {
                    validSessionId = targetSid
                    println("[TriliveChatSDK] Session $targetSid is valid, reusing.")
                } else {
                    println("[TriliveChatSDK] Session $targetSid is expired or invalid (success=false). Server detail: ${detail.detail}")
                    clearSession()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("[TriliveChatSDK] Session validation exception for $targetSid (Server error / 404 / expired error page): ${e.message}")
                clearSession()
            }
        }

        // 2. 若無有效 Session，自動重新請求建立新 Session (具備 Domain Failover)
        if (validSessionId == null) {
            validSessionId = requireSessionId(forceNew = true)
        }

        val cleanDomain = currentDomain.removeSuffix("/")
        return "$cleanDomain/chat.html?id=$validSessionId"
    }

    private companion object {
        private fun normalizeDomains(domains: List<String>): List<String> {
            return domains.map { domain ->
                if (domain.endsWith("/")) domain else "$domain/"
            }
        }

        private fun createApiForDomain(domain: String, client: HttpClient): TriliveChatApi {
            val cleanUrl = if (domain.endsWith("/")) domain else "$domain/"
            val ktorfit = Ktorfit.Builder()
                .baseUrl(cleanUrl)
                .httpClient(client)
                .build()
            return ktorfit.createTriliveChatApi()
        }

        private fun createDefaultHttpClient(activeDomainProvider: () -> String): HttpClient {
            return HttpClient {
                install(DomainRedirectPlugin) {
                    this.activeDomainProvider = activeDomainProvider
                }
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        coerceInputValues = true
                        isLenient = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000L
                    connectTimeoutMillis = 60000L
                    socketTimeoutMillis = 60000L
                }
            }
        }
    }
}
