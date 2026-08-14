package com.example.tickerdemo.presentation.ticker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote
import org.koin.androidx.compose.koinViewModel

@Composable
fun TickerScreen(
    viewModel: TickerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    DisposableEffect(viewModel) {
        viewModel.onAction(TickerAction.StartWatching)
        onDispose { viewModel.onAction(TickerAction.StopWatching) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ConnectionStatusLabel(state.connectionStatus)

        LazyColumn {
            items(state.quotes) { quote ->
                QuoteRow(quote)
            }
        }
    }
}

@Composable
private fun ConnectionStatusLabel(status: ConnectionStatus) {
    val text = when (status) {
        ConnectionStatus.Connecting -> "Connecting…"
        ConnectionStatus.Connected -> "Live"
        is ConnectionStatus.Reconnecting -> "Reconnecting (attempt ${status.attempt}, next in ${status.delayMillis}ms)"
        is ConnectionStatus.Failed -> "Disconnected: ${status.reason}"
    }
    Text(text = text, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun QuoteRow(quote: Quote) {
    val sign = if (quote.changePercent >= 0) "+" else ""
    Text(text = "${quote.symbol}  ${quote.price}  ($sign${quote.changePercent}%)")
}
