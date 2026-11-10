package chat.im.trilivechat.chatedin.di

import chat.im.trilivechat.chatedin.core.SessionCoordinator
import chat.im.trilivechat.chatedin.core.SessionManager
import chat.im.trilivechat.chatedin.core.SessionProvider
import chat.im.trilivechat.chatedin.core.SessionStateHolder
import chat.im.trilivechat.chatedin.api.TriliveChatApi
import chat.im.trilivechat.chatedin.api.createTriliveChatApi
import chat.im.trilivechat.chatedin.repository.TriliveChatRepository
import de.jensklingenberg.ktorfit.Ktorfit
import idv.neo.module.di.tag.APP_API_URL_TAG
import org.koin.core.qualifier.named
import org.koin.dsl.module

val commonModule = module {
    includes(
        networkEngineModule,
        defaultHttpClientPluginModule,
        httpClientModule,
        resfulModule
    )

    /**
     * TriliveChat 官方客服 API 基底網址（SDK 全域單一真實來源）。
     */
    single(named(APP_API_URL_TAG)) {
        "https://trilivechat.chat/"
    }

    single<TriliveChatApi> {
        get<Ktorfit>().createTriliveChatApi()
    }

    single { SessionStateHolder() }

    single<SessionProvider> {
        SessionCoordinator(
            api = get(),
            stateHolder = get()
        )
    }

    single {
        SessionManager(
            baseUrl = get(named(APP_API_URL_TAG)),
            api = get()
        )
    }

    single { TriliveChatRepository(api = get(), sessionProvider = get()) }
}
