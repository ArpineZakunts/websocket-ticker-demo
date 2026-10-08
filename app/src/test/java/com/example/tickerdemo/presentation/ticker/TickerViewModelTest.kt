package com.example.tickerdemo.presentation.ticker

import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote
import com.example.tickerdemo.domain.repository.QuoteRepository
import com.example.tickerdemo.domain.usecase.ConnectToQuoteFeedUseCase
import com.example.tickerdemo.domain.usecase.DisconnectFromQuoteFeedUseCase
import com.example.tickerdemo.domain.usecase.ObserveConnectionStatusUseCase
import com.example.tickerdemo.domain.usecase.ObserveQuotesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TickerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeQuoteRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = TickerViewModel(
        observeQuotes = ObserveQuotesUseCase(fakeRepository),
        observeConnectionStatus = ObserveConnectionStatusUseCase(fakeRepository),
        connectToQuoteFeed = ConnectToQuoteFeedUseCase(fakeRepository),
        disconnectFromQuoteFeed = DisconnectFromQuoteFeedUseCase(fakeRepository),
    )

    @Test
    fun `starting watching connects with the watched symbols`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onAction(TickerAction.StartWatching)
        dispatcher.scheduler.runCurrent()

        assertEquals(
            listOf("AAPL", "MSFT", "NVDA", "TSLA", "BTC-USD", "ETH-USD", "EUR-USD", "GBP-USD"),
            fakeRepository.lastConnectedSymbols,
        )
    }

    @Test
    fun `quotes emitted after StartWatching are merged into state by symbol`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onAction(TickerAction.StartWatching)
        dispatcher.scheduler.runCurrent()

        fakeRepository.emitQuote(Quote("AAPL", 190.0, 0.5, 1L))
        fakeRepository.emitQuote(Quote("TSLA", 250.0, -1.2, 2L))
        fakeRepository.emitQuote(Quote("AAPL", 191.5, 0.6, 3L)) // update, not a new row
        dispatcher.scheduler.runCurrent()

        val quotes = viewModel.state.value.quotes
        assertEquals(2, quotes.size)
        assertEquals(191.5, quotes.first { it.symbol == "AAPL" }.price, 0.0)
    }

    @Test
    fun `connection status updates flow into state`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onAction(TickerAction.StartWatching)
        dispatcher.scheduler.runCurrent()

        fakeRepository.emitStatus(ConnectionStatus.Reconnecting(attempt = 1, delayMillis = 1_000))
        dispatcher.scheduler.runCurrent()

        assertEquals(ConnectionStatus.Reconnecting(1, 1_000), viewModel.state.value.connectionStatus)
    }

    @Test
    fun `stopping watching disconnects`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onAction(TickerAction.StartWatching)
        dispatcher.scheduler.runCurrent()
        viewModel.onAction(TickerAction.StopWatching)
        dispatcher.scheduler.runCurrent()

        assertTrue(fakeRepository.disconnectCalled)
    }
}

private class FakeQuoteRepository : QuoteRepository {

    private val quotesFlow = MutableSharedFlow<Quote>(extraBufferCapacity = 16)
    private val statusesFlow = MutableSharedFlow<ConnectionStatus>(extraBufferCapacity = 16)

    var lastConnectedSymbols: List<String>? = null
        private set
    var disconnectCalled: Boolean = false
        private set

    suspend fun emitQuote(quote: Quote) = quotesFlow.emit(quote)
    suspend fun emitStatus(status: ConnectionStatus) = statusesFlow.emit(status)

    override val connectionStatus: Flow<ConnectionStatus> get() = statusesFlow
    override val quotes: Flow<Quote> get() = quotesFlow

    override suspend fun connect(symbols: List<String>) {
        lastConnectedSymbols = symbols
    }

    override suspend fun disconnect() {
        disconnectCalled = true
    }
}
