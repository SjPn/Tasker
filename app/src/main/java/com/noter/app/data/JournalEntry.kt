package com.noter.app.data

import java.time.LocalDateTime

data class JournalEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    var content: String = "",
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    var updatedAtMillis: Long = System.currentTimeMillis()
)

