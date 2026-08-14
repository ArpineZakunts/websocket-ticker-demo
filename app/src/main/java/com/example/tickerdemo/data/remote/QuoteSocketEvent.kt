package com.example.tickerdemo.data.remote

import com.example.tickerdemo.domain.model.ConnectionStatus
import com.example.tickerdemo.domain.model.Quote

sealed interface QuoteSocketEvent {
    data class QuoteReceived(val quote: Quote) : QuoteSocketEvent
    data class StatusChanged(val status: ConnectionStatus) : QuoteSocketEvent
}
