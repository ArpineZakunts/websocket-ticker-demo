package com.example.tickerdemo.domain.model

data class Quote(
    val symbol: String,
    val price: Double,
    val changePercent: Double,
    val timestampMillis: Long,
)

sealed interface ConnectionStatus {
    data object Connecting : ConnectionStatus
    data object Connected : ConnectionStatus
    data class Reconnecting(val attempt: Int, val delayMillis: Long) : ConnectionStatus
    data class Failed(val reason: String) : ConnectionStatus
}
