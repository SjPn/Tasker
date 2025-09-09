package com.noter.app.data

import java.time.LocalDate

data class Day(
    val date: LocalDate,
    val dayOfWeek: String,
    val notes: List<Note> = emptyList()
) {
    fun getDisplayTitle(): String {
        val dayNames = listOf(
            "Monday", "Tuesday", "Wednesday", "Thursday", 
            "Friday", "Saturday", "Sunday"
        )
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        
        return "${dayNames[date.dayOfWeek.value - 1]}, ${date.dayOfMonth} ${monthNames[date.monthValue - 1]}"
    }
    
    fun getNotesPreview(): String {
        val nonEmptyNotes = notes.filter { it.text.trim().isNotEmpty() }
        android.util.Log.d("Day", "Date: $date, Total notes: ${notes.size}, Non-empty: ${nonEmptyNotes.size}")
        return if (nonEmptyNotes.isEmpty()) {
            "No tasks"
        } else {
            val preview = nonEmptyNotes.take(3).joinToString("\n") { note ->
                val text = note.text.trim()
                val previewText = if (text.length > 50) {
                    text.substring(0, 47) + "..."
                } else {
                    text
                }
                "• $previewText"
            }
            if (nonEmptyNotes.size > 3) {
                "$preview\n..."
            } else {
                preview
            }
        }
    }
} 