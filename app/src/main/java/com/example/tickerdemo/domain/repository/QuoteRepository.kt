package com.example.tickerdemo.domain.repository

import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {

    /**
     * Subscribes to a live quote stream for the given symbols. The returned flow stays
     * open for as long as it's collected and survives transient disconnects by
     * reconnecting internally — callers don't need to retry.
     */
    fun observeQuotes(symbols: List<String>): Flow<Quote>

    fun observeConnectionStatus(): Flow<ConnectionStatus>
}
