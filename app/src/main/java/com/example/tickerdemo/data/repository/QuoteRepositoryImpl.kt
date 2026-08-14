package com.example.tickerdemo.data.repository

import com.example.tickerdemo.data.remote.QuoteSocketClient
import com.example.tickerdemo.data.remote.QuoteSocketEvent
import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote
import com.example.tickerdemo.domain.repository.QuoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Multiplexes a single WebSocket connection across every collector via [shareIn]: the socket
 * opens on the first subscriber and closes [STOP_TIMEOUT_MILLIS] after the last one leaves,
 * so screen rotations or quick navigation don't tear the connection down and reconnect.
 */
class QuoteRepositoryImpl(
    private val socketClient: QuoteSocketClient,
    private val repositoryScope: CoroutineScope,
) : QuoteRepository {

    private var sharedEvents: SharedFlow<QuoteSocketEvent>? = null

    override fun observeQuotes(symbols: List<String>): Flow<Quote> =
        eventsFor(symbols).filterIsInstance<QuoteSocketEvent.QuoteReceived>().map { it.quote }

    override fun observeConnectionStatus(): Flow<ConnectionStatus> =
        eventsFor(symbols = emptyList()).filterIsInstance<QuoteSocketEvent.StatusChanged>().map { it.status }

    private fun eventsFor(symbols: List<String>): SharedFlow<QuoteSocketEvent> {
        sharedEvents?.let { return it }

        return socketClient.connect(symbols)
            .shareIn(
                scope = repositoryScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                replay = 0,
            )
            .also { sharedEvents = it }
    }
}
