package com.example.tickerdemo.data.remote

import android.util.Log
import com.example.tickerdemo.data.remote.dto.QuoteDto
import com.example.tickerdemo.data.remote.dto.SubscribeRequest
import com.example.tickerdemo.data.remote.dto.toDomain
import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "QuoteSocketClient"
private const val PING_INTERVAL_MILLIS = 15_000L

/**
 * Owns the raw WebSocket connection: subscribe handshake, ping keepalive, frame decoding,
 * and reconnect-with-backoff. Lifecycle is explicit ([initSocket]/[close]) rather than tied
 * to Flow collection, so a caller decides exactly when the socket opens and closes.
 */
class QuoteSocketClient(
    private val httpClient: HttpClient,
    private val host: String,
    private val port: Int,
    private val reconnectStrategy: ReconnectStrategy = ReconnectStrategy(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val sessionRef = AtomicReference<DefaultClientWebSocketSession?>(null)

    // Guards connectAndPingJob start/stop and the session lifecycle it owns, so
    // initSocket()/close() can't race each other or leave two reconnect loops running.
    private val connectMutex = Mutex()

    private val isAvailable = MutableStateFlow(true)
    private var connectAndPingJob: Job? = null
    private var symbolsToWatch: List<String> = emptyList()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Connecting)
    val connectionStatus: Flow<ConnectionStatus> get() = _connectionStatus.asStateFlow()

    private val _quotes = MutableSharedFlow<Quote>(extraBufferCapacity = 64)
    val quotes: Flow<Quote> get() = _quotes.asSharedFlow()

    suspend fun initSocket(symbols: List<String>) = connectMutex.withLock {
        symbolsToWatch = symbols
        connectAndPingJob?.cancelAndJoin()
        sessionRef.getAndSet(null)?.close()
        isAvailable.value = true
        connectAndPingJob = connectAndPing()
    }

    suspend fun close() = connectMutex.withLock {
        isAvailable.value = false
        connectAndPingJob?.cancelAndJoin()
        connectAndPingJob = null
        sessionRef.getAndSet(null)?.close()
        _connectionStatus.value = ConnectionStatus.Connecting
    }

    private fun connectAndPing() = CoroutineScope(Dispatchers.IO).launch {
        var attempt = 0
        while (isAvailable.value) {
            try {
                val session = connectSocket()
                attempt = 0
                supervisorScope {
                    launch { pingSocket(session) }
                    listenToSocket(session)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Connection error: ${error.message}")
                attempt += 1
                val delayMillis = reconnectStrategy.nextDelayMillis(attempt)
                _connectionStatus.value = ConnectionStatus.Reconnecting(attempt, delayMillis)
                delay(delayMillis)
            }
        }
    }

    // Only ever called from within the connectAndPing loop, so this get-or-create
    // doesn't need its own lock — but it must still be a single atomic read of
    // sessionRef to avoid a check-then-act race against close().
    private suspend fun connectSocket(): DefaultClientWebSocketSession {
        val current = sessionRef.get()
        if (current != null && current.isActive) return current

        val newSession = httpClient.webSocketSession {
            url(scheme = "wss", host = host, port = port, path = "/quotes")
        }
        newSession.send(Frame.Text(json.encodeToString(SubscribeRequest.serializer(), SubscribeRequest(symbols = symbolsToWatch))))
        sessionRef.set(newSession)
        _connectionStatus.value = ConnectionStatus.Connected
        Log.d(TAG, "Connected, watching $symbolsToWatch")
        return newSession
    }

    private suspend fun pingSocket(session: DefaultClientWebSocketSession) {
        while (session.isActive) {
            delay(PING_INTERVAL_MILLIS)
            session.send(Frame.Ping(ByteArray(0)))
        }
    }

    private suspend fun listenToSocket(session: DefaultClientWebSocketSession) {
        session.incoming
            .consumeAsFlow()
            .mapNotNull { frame -> (frame as? Frame.Text)?.let { decodeQuote(it.readText()) } }
            .catch { error -> Log.e(TAG, "Error in incoming flow: ${error.message}") }
            .onEach { quote -> _quotes.emit(quote) }
            .flowOn(Dispatchers.IO)
            .collect()
    }

    private fun decodeQuote(dataString: String): Quote? = try {
        json.decodeFromString(QuoteDto.serializer(), dataString).toDomain()
    } catch (error: Exception) {
        Log.d(TAG, "Quote parse failed: ${error.message}")
        null
    }
}
