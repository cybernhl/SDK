package chat.im.trilivechat.chatedin.di

import idv.neo.module.di.tag.HTTP_CLIENT_DEFAULT_PLUGINS_TAG
import idv.neo.module.di.tag.HTTP_CLIENT_PLUGINS_TAG
import idv.neo.module.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import org.koin.core.qualifier.named
import org.koin.dsl.module

val httpClientModule = module {
    single<HttpClient> {
        createHttpClient(
            get(),
            mutableListOf<HttpClientConfig<*>.() -> Unit>().apply {
                getOrNull<List<HttpClientConfig<*>.() -> Unit>>(
                    named(
                        HTTP_CLIENT_DEFAULT_PLUGINS_TAG
                    )
                )?.let {
                    addAll(it)
                }

                getOrNull<List<HttpClientConfig<*>.() -> Unit>>(named(HTTP_CLIENT_PLUGINS_TAG))?.let {
                    addAll(it)
                }
            }
        )
    }
}