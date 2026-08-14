package com.example.tickerdemo.domain.usecase

import com.example.tickerdemo.domain.model.Quote
import com.example.tickerdemo.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveQuotesUseCase(
    private val quoteRepository: QuoteRepository,
) {
    operator fun invoke(): Flow<Quote> = quoteRepository.quotes
}
