package com.example.tickerdemo.presentation.ticker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tickerdemo.domain.usecase.ObserveConnectionStatusUseCase
import com.example.tickerdemo.domain.usecase.ObserveQuotesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val WATCHED_SYMBOLS = listOf("AAPL", "TSLA", "BTC-USD", "EUR-USD")

class TickerViewModel(
    private val observeQuotes: ObserveQuotesUseCase,
    private val observeConnectionStatus: ObserveConnectionStatusUseCase,
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
            launch {
                observeQuotes(WATCHED_SYMBOLS).onEach { quote ->
                    _state.update { it.copy(quotesBySymbol = it.quotesBySymbol + (quote.symbol to quote)) }
                }.collect()
            }
            launch {
                observeConnectionStatus().onEach { status ->
                    _state.update { it.copy(connectionStatus = status) }
                }.collect()
            }
        }
    }

    private fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
    }

    override fun onCleared() {
        stopWatching()
    }
}
