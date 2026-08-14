package com.example.tickerdemo.presentation.ticker

import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote

data class TickerState(
    val quotesBySymbol: Map<String, Quote> = emptyMap(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.Connecting,
) {
    val quotes: List<Quote> get() = quotesBySymbol.values.sortedBy { it.symbol }
}

sealed interface TickerAction {
    data object StartWatching : TickerAction
    data object StopWatching : TickerAction
}
