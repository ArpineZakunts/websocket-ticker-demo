package com.example.tickerdemo.domain.repository

import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {

    val connectionStatus: Flow<ConnectionStatus>
    val quotes: Flow<Quote>

    /** Opens the socket and subscribes to [symbols]. Safe to call again to re-subscribe. */
    suspend fun connect(symbols: List<String>)

    suspend fun disconnect()
}
