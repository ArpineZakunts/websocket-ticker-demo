package com.example.tickerdemo.domain.usecase

import com.example.tickerdemo.domain.repository.QuoteRepository

class ConnectToQuoteFeedUseCase(
    private val quoteRepository: QuoteRepository,
) {
    suspend operator fun invoke(symbols: List<String>) {
        require(symbols.isNotEmpty()) { "Must subscribe to at least one symbol" }
        quoteRepository.connect(symbols)
    }
}
