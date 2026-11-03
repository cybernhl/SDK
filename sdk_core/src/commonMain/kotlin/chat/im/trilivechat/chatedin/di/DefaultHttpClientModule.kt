package chat.im.trilivechat.chatedin.di

import idv.neo.module.di.tag.GENERAL_JSON_TAG
import idv.neo.module.di.tag.HTTP_CLIENT_DEFAULT_PLUGINS_TAG
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * 提供預設的 HttpClient Plugins 配置，供 HttpClientModule 注入使用
 */
val defaultHttpClientPluginModule = module {
    single<List<HttpClientConfig<*>.() -> Unit>>(named(HTTP_CLIENT_DEFAULT_PLUGINS_TAG)) {
        val kotlinx_json = getOrNull<Json>(named(GENERAL_JSON_TAG)) ?: Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            coerceInputValues = true
            isLenient = true
        }

        listOf(
            {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            println("[TriliveChatSDK] $message")
                        }
                    }
                    level = LogLevel.ALL
                }
            },
            {
                install(ContentNegotiation) {
                    json(kotlinx_json)
                    json(kotlinx_json, contentType = ContentType.Application.Json)
                    json(kotlinx_json, contentType = ContentType.Text.Html)
                    register(ContentType.Text.Html, KotlinxSerializationConverter(kotlinx_json))
                }
            },
            {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000L
                    connectTimeoutMillis = 60000L
                    socketTimeoutMillis = 60000L
                }
            },
        )
    }
}
