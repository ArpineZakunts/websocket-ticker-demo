package com.example.tickerdemo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuoteDto(
    val symbol: String,
    val price: Double,
    @SerialName("change_percent") val changePercent: Double,
    val timestamp: Long,
)

@Serializable
data class SubscribeRequest(
    val action: String = "subscribe",
    val symbols: List<String>,
)
