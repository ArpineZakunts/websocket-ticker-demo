package com.example.tickerdemo.domain.usecase

import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveConnectionStatusUseCase(
    private val quoteRepository: QuoteRepository,
) {
    operator fun invoke(): Flow<ConnectionStatus> = quoteRepository.observeConnectionStatus()
}
