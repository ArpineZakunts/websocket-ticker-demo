package com.example.tickerdemo.data.remote.dto

import com.example.tickerdemo.domain.model.Quote

/**
 * Owns validation of the raw DTO; callers trust this and don't re-check these fields.
 */
fun QuoteDto.toDomain(): Quote {
    require(symbol.isNotBlank()) { "QuoteDto.symbol must not be blank" }
    require(price >= 0.0) { "QuoteDto.price must not be negative" }

    return Quote(
        symbol = symbol.uppercase(),
        price = price,
        changePercent = changePercent,
        timestampMillis = timestamp,
    )
}
