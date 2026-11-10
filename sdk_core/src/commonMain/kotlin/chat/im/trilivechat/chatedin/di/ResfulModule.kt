package chat.im.trilivechat.chatedin.di

import de.jensklingenberg.ktorfit.Ktorfit
import idv.neo.module.network.resful.ktorfitBuilder
import org.koin.core.qualifier.named
import org.koin.dsl.module

val resfulModule = module {
    single<Ktorfit> {
        val url =  get<String>(named(idv.neo.module.di.tag.APP_API_URL_TAG))
        ktorfitBuilder(url, get())
    }
}