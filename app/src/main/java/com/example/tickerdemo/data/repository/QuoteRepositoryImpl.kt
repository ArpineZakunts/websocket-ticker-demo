package com.example.tickerdemo.data.repository

import com.example.tickerdemo.data.remote.QuoteSocketClient
import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote
import com.example.tickerdemo.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow

class QuoteRepositoryImpl(
    private val socketClient: QuoteSocketClient,
) : QuoteRepository {

    override val connectionStatus: Flow<ConnectionStatus> get() = socketClient.connectionStatus
    override val quotes: Flow<Quote> get() = socketClient.quotes

    override suspend fun connect(symbols: List<String>) = socketClient.initSocket(symbols)

    override suspend fun disconnect() = socketClient.close()
}
