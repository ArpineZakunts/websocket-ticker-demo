package com.example.tickerdemo.presentation.ticker

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Gain = Color(0xFF4ADE80)
private val Loss = Color(0xFFF87171)
private val Warning = Color(0xFFFBBF24)

private val DISPLAY_NAMES = mapOf(
    "AAPL" to "Apple Inc.",
    "MSFT" to "Microsoft Corp.",
    "NVDA" to "NVIDIA Corp.",
    "TSLA" to "Tesla, Inc.",
    "BTC-USD" to "Bitcoin / US Dollar",
    "ETH-USD" to "Ethereum / US Dollar",
    "EUR-USD" to "Euro / US Dollar",
    "GBP-USD" to "British Pound / US Dollar",
)

@Composable
fun TickerScreen(
    viewModel: TickerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    DisposableEffect(viewModel) {
        viewModel.onAction(TickerAction.StartWatching)
        onDispose { viewModel.onAction(TickerAction.StopWatching) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Header(state.connectionStatus)

        if (state.quotes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(state.quotes, key = { it.symbol }) { quote ->
                    QuoteCard(quote)
                }
            }
        }
    }
}

@Composable
private fun Header(status: ConnectionStatus) {
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 20.dp)) {
        Text(
            text = "Markets",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        ConnectionStatusPill(status)
    }
}

@Composable
private fun ConnectionStatusPill(status: ConnectionStatus) {
    val (text, color) = when (status) {
        ConnectionStatus.Connecting -> "Connecting…" to Warning
        ConnectionStatus.Connected -> "Live · WebSocket connected" to Gain
        is ConnectionStatus.Reconnecting ->
            "Reconnecting · attempt ${status.attempt}, retry in ${status.delayMillis / 1000.0}s" to Warning
        is ConnectionStatus.Failed -> "Disconnected: ${status.reason}" to Loss
    }
    val animatedColor by animateColorAsState(color, label = "statusColor")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(animatedColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(animatedColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = animatedColor)
    }
}

@Composable
private fun QuoteCard(quote: Quote) {
    val isGain = quote.changePercent >= 0
    val trendColor = if (isGain) Gain else Loss

    // Briefly tint the card in the direction of each new tick.
    var previousPrice by remember(quote.symbol) { mutableDoubleStateOf(quote.price) }
    val flash = remember { Animatable(Color.Transparent) }
    LaunchedEffect(quote.price) {
        if (quote.price != previousPrice) {
            val tickColor = if (quote.price > previousPrice) Gain else Loss
            previousPrice = quote.price
            flash.snapTo(tickColor.copy(alpha = 0.18f))
            flash.animateTo(Color.Transparent, tween(durationMillis = 700))
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(flash.value)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quote.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = DISPLAY_NAMES[quote.symbol] ?: quote.symbol,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(quote),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(quote.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "%s%.2f%%", if (isGain) "▲ " else "▼ ", kotlin.math.abs(quote.changePercent)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = trendColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(trendColor.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private fun formatPrice(quote: Quote): String = when {
    quote.price < 10 -> String.format(Locale.US, "%.4f", quote.price)
    else -> String.format(Locale.US, "$%,.2f", quote.price)
}

private fun formatTime(timestampMillis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestampMillis))
