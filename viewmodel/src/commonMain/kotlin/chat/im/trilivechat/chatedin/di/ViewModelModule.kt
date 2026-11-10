package chat.im.trilivechat.chatedin.di

import chat.im.trilivechat.chatedin.viewmodel.TriliveChatViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { TriliveChatViewModel(repository = get(), sessionProvider = get()) }
}
