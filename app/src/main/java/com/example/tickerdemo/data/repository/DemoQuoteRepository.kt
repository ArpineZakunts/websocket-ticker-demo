package com.example.tickerdemo.data.repository

import com.example.tickerdemo.data.remote.ReconnectStrategy
import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote
import com.example.tickerdemo.domain.repository.QuoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TICK_INTERVAL_MILLIS = 600L
private const val TICKS_BEFORE_SIMULATED_DROP = 40
private const val SIMULATED_FAILED_ATTEMPTS = 2

private val BASE_PRICES = mapOf(
    "AAPL" to 228.52,
    "MSFT" to 416.06,
    "NVDA" to 118.85,
    "TSLA" to 251.38,
    "BTC-USD" to 62_450.10,
    "ETH-USD" to 2_438.72,
    "EUR-USD" to 1.0935,
    "GBP-USD" to 1.3087,
)

/**
 * Offline stand-in for the WebSocket feed, used in debug builds so the app shows live data
 * without a backend. Prices follow a small random walk, and the feed simulates one connection
 * drop so the reconnect-with-backoff status can be seen in the UI.
 */
class DemoQuoteRepository(
    private val reconnectStrategy: ReconnectStrategy,
    private val random: Random = Random.Default,
) : QuoteRepository {

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Connecting)
    override val connectionStatus: Flow<ConnectionStatus> get() = _connectionStatus.asStateFlow()

    private val _quotes = MutableSharedFlow<Quote>(replay = BASE_PRICES.size, extraBufferCapacity = 64)
    override val quotes: Flow<Quote> get() = _quotes.asSharedFlow()

    private var feedJob: Job? = null

    override suspend fun connect(symbols: List<String>) {
        feedJob?.cancelAndJoin()
        feedJob = CoroutineScope(Dispatchers.Default).launch { runFeed(symbols) }
    }

    override suspend fun disconnect() {
        feedJob?.cancelAndJoin()
        feedJob = null
        _connectionStatus.value = ConnectionStatus.Connecting
    }

    private suspend fun runFeed(symbols: List<String>) {
        val openPrices = symbols.associateWith { BASE_PRICES[it] ?: 100.0 }
        val prices = openPrices.toMutableMap()
        var hasDropped = false

        _connectionStatus.value = ConnectionStatus.Connecting
        delay(800)
        _connectionStatus.value = ConnectionStatus.Connected
        symbols.forEach { emitQuote(it, prices.getValue(it), openPrices.getValue(it)) }

        var tick = 0
        // delay() is cancellable, so disconnect() stops this loop.
        while (true) {
            if (!hasDropped && tick == TICKS_BEFORE_SIMULATED_DROP) {
                hasDropped = true
                for (attempt in 1..SIMULATED_FAILED_ATTEMPTS) {
                    val delayMillis = reconnectStrategy.nextDelayMillis(attempt)
                    _connectionStatus.value = ConnectionStatus.Reconnecting(attempt, delayMillis)
                    delay(delayMillis)
                }
                _connectionStatus.value = ConnectionStatus.Connected
            }

            val symbol = symbols[random.nextInt(symbols.size)]
            val price = prices.getValue(symbol) * (1 + random.nextDouble(-0.0015, 0.0015))
            prices[symbol] = price
            emitQuote(symbol, price, openPrices.getValue(symbol))
            tick += 1
            delay(TICK_INTERVAL_MILLIS)
        }
    }

    private suspend fun emitQuote(symbol: String, price: Double, open: Double) {
        _quotes.emit(
            Quote(
                symbol = symbol,
                price = price,
                changePercent = (price - open) / open * 100,
                timestampMillis = System.currentTimeMillis(),
            ),
        )
    }
}
