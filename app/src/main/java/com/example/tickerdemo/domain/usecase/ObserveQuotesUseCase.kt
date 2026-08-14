package com.example.tickerdemo.domain.usecase

import com.example.tickerdemo.domain.model.Quote
import com.example.tickerdemo.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveQuotesUseCase(
    private val quoteRepository: QuoteRepository,
) {
    operator fun invoke(symbols: List<String>): Flow<Quote> {
        require(symbols.isNotEmpty()) { "Must subscribe to at least one symbol" }
        return quoteRepository.observeQuotes(symbols)
    }
}
