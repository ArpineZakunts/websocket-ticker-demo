package com.example.tickerdemo.data.remote

import com.example.tickerdemo.data.remote.dto.QuoteDto
import com.example.tickerdemo.data.remote.dto.SubscribeRequest
import com.example.tickerdemo.data.remote.dto.toDomain
import com.example.tickerdemo.domain.model.ConnectionStatus
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

/**
 * Owns the raw WebSocket connection: subscribe handshake, incoming-frame decoding, and
 * reconnect-with-backoff on any failure. Emits both quote updates and connection-status
 * changes on a single flow so the repository can multiplex them without a second socket.
 */
class QuoteSocketClient(
    private val httpClient: HttpClient,
    private val host: String,
    private val port: Int,
    private val reconnectStrategy: ReconnectStrategy = ReconnectStrategy(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun connect(symbols: List<String>): Flow<QuoteSocketEvent> = callbackFlow {
        var attempt = 0

        while (isActive) {
            trySend(QuoteSocketEvent.StatusChanged(ConnectionStatus.Connecting))

            try {
                httpClient.webSocket(host = host, port = port, path = "/quotes") {
                    send(Frame.Text(json.encodeToString(SubscribeRequest.serializer(), SubscribeRequest(symbols = symbols))))

                    attempt = 0
                    trySend(QuoteSocketEvent.StatusChanged(ConnectionStatus.Connected))

                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val dto = json.decodeFromString(QuoteDto.serializer(), frame.readText())
                        trySend(QuoteSocketEvent.QuoteReceived(dto.toDomain()))
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                attempt += 1
                val delayMillis = reconnectStrategy.nextDelayMillis(attempt)
                trySend(QuoteSocketEvent.StatusChanged(ConnectionStatus.Reconnecting(attempt, delayMillis)))
                delay(delayMillis)
            }
        }

        awaitClose { /* session is closed by the webSocket{} block when this scope cancels */ }
    }
}
