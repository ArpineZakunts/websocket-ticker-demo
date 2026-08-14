package com.example.tickerdemo.presentation.ticker

import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote
import com.example.tickerdemo.domain.repository.QuoteRepository
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

    @Test
    fun `quotes emitted after StartWatching are merged into state by symbol`() = runTest {
        val viewModel = TickerViewModel(
            observeQuotes = ObserveQuotesUseCase(fakeRepository),
            observeConnectionStatus = ObserveConnectionStatusUseCase(fakeRepository),
        )

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
        val viewModel = TickerViewModel(
            observeQuotes = ObserveQuotesUseCase(fakeRepository),
            observeConnectionStatus = ObserveConnectionStatusUseCase(fakeRepository),
        )

        viewModel.onAction(TickerAction.StartWatching)
        dispatcher.scheduler.runCurrent()

        fakeRepository.emitStatus(ConnectionStatus.Reconnecting(attempt = 1, delayMillis = 1_000))
        dispatcher.scheduler.runCurrent()

        assertEquals(ConnectionStatus.Reconnecting(1, 1_000), viewModel.state.value.connectionStatus)
    }
}

private class FakeQuoteRepository : QuoteRepository {

    private val quotes = MutableSharedFlow<Quote>(extraBufferCapacity = 16)
    private val statuses = MutableSharedFlow<ConnectionStatus>(extraBufferCapacity = 16)

    suspend fun emitQuote(quote: Quote) = quotes.emit(quote)
    suspend fun emitStatus(status: ConnectionStatus) = statuses.emit(status)

    override fun observeQuotes(symbols: List<String>): Flow<Quote> = quotes
    override fun observeConnectionStatus(): Flow<ConnectionStatus> = statuses
}
