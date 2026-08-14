package com.example.tickerdemo.domain.usecase

import com.example.tickerdemo.domain.repository.QuoteRepository

class DisconnectFromQuoteFeedUseCase(
    private val quoteRepository: QuoteRepository,
) {
    suspend operator fun invoke() = quoteRepository.disconnect()
}
