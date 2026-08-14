package com.example.tickerdemo.di

import com.example.tickerdemo.data.remote.QuoteSocketClient
import com.example.tickerdemo.data.remote.ReconnectStrategy
import com.example.tickerdemo.data.repository.QuoteRepositoryImpl
import com.example.tickerdemo.domain.repository.QuoteRepository
import com.example.tickerdemo.domain.usecase.ConnectToQuoteFeedUseCase
import com.example.tickerdemo.domain.usecase.DisconnectFromQuoteFeedUseCase
import com.example.tickerdemo.domain.usecase.ObserveConnectionStatusUseCase
import com.example.tickerdemo.domain.usecase.ObserveQuotesUseCase
import com.example.tickerdemo.presentation.ticker.TickerViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.websocket.WebSockets
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
}

val repositoryModule = module {
    single<QuoteRepository> { QuoteRepositoryImpl(socketClient = get()) }
}

val useCaseModule = module {
    factory { ObserveQuotesUseCase(quoteRepository = get()) }
    factory { ObserveConnectionStatusUseCase(quoteRepository = get()) }
    factory { ConnectToQuoteFeedUseCase(quoteRepository = get()) }
    factory { DisconnectFromQuoteFeedUseCase(quoteRepository = get()) }
}

val viewModelModule = module {
    viewModel {
        TickerViewModel(
            observeQuotes = get(),
            observeConnectionStatus = get(),
            connectToQuoteFeed = get(),
            disconnectFromQuoteFeed = get(),
        )
    }
}

val appModules = listOf(networkModule, repositoryModule, useCaseModule, viewModelModule)
