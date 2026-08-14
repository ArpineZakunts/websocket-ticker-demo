package com.example.tickerdemo.di

import com.example.tickerdemo.data.remote.QuoteSocketClient
import com.example.tickerdemo.data.remote.ReconnectStrategy
import com.example.tickerdemo.data.repository.QuoteRepositoryImpl
import com.example.tickerdemo.domain.repository.QuoteRepository
import com.example.tickerdemo.domain.usecase.ObserveConnectionStatusUseCase
import com.example.tickerdemo.domain.usecase.ObserveQuotesUseCase
import com.example.tickerdemo.presentation.ticker.TickerViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private const val QUOTE_SERVER_HOST = "quotes.example-ticker-demo.com"
private const val QUOTE_SERVER_PORT = 443

val networkModule = module {
    single {
        HttpClient(Android) {
            install(WebSockets)
        }
    }
    single { ReconnectStrategy() }
    single {
        QuoteSocketClient(
            httpClient = get(),
            host = QUOTE_SERVER_HOST,
            port = QUOTE_SERVER_PORT,
            reconnectStrategy = get(),
        )
    }
    // Survives individual screen lifecycles so the shared socket in the repository
    // isn't torn down by a single ViewModel's onCleared().
    single { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}

val repositoryModule = module {
    single<QuoteRepository> { QuoteRepositoryImpl(socketClient = get(), repositoryScope = get()) }
}

val useCaseModule = module {
    factory { ObserveQuotesUseCase(quoteRepository = get()) }
    factory { ObserveConnectionStatusUseCase(quoteRepository = get()) }
}

val viewModelModule = module {
    viewModel { TickerViewModel(observeQuotes = get(), observeConnectionStatus = get()) }
}

val appModules = listOf(networkModule, repositoryModule, useCaseModule, viewModelModule)
