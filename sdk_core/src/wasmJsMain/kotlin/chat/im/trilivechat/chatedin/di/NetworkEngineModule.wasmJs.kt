package chat.im.trilivechat.chatedin.di

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkEngineModule: Module = module {
    single<HttpClientEngineFactory<*>> {
        Js
    }
}
