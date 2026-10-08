package com.example.tickerdemo.presentation.ticker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tickerdemo.domain.usecase.ConnectToQuoteFeedUseCase
import com.example.tickerdemo.domain.usecase.DisconnectFromQuoteFeedUseCase
import com.example.tickerdemo.domain.usecase.ObserveConnectionStatusUseCase
import com.example.tickerdemo.domain.usecase.ObserveQuotesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val WATCHED_SYMBOLS = listOf("AAPL", "MSFT", "NVDA", "TSLA", "BTC-USD", "ETH-USD", "EUR-USD", "GBP-USD")

class TickerViewModel(
    private val observeQuotes: ObserveQuotesUseCase,
    private val observeConnectionStatus: ObserveConnectionStatusUseCase,
    private val connectToQuoteFeed: ConnectToQuoteFeedUseCase,
    private val disconnectFromQuoteFeed: DisconnectFromQuoteFeedUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TickerState())
    val state: StateFlow<TickerState> = _state.asStateFlow()

    private var watchJob: Job? = null

    fun onAction(action: TickerAction) {
        when (action) {
            TickerAction.StartWatching -> startWatching()
            TickerAction.StopWatching -> stopWatching()
        }
    }

    private fun startWatching() {
        if (watchJob != null) return

        watchJob = viewModelScope.launch {
            connectToQuoteFeed(WATCHED_SYMBOLS)

                observeQuotes().onEach { quote ->
                    _state.update { it.copy(quotesBySymbol = it.quotesBySymbol + (quote.symbol to quote)) }
                }.launchIn(viewModelScope)
                observeConnectionStatus().onEach { status ->
                    _state.update { it.copy(connectionStatus = status) }
                }.launchIn(viewModelScope)
        }
    }

    private fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
        viewModelScope.launch { disconnectFromQuoteFeed() }
    }

    override fun onCleared() {
        stopWatching()
    }
}
