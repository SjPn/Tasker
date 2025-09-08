package com.noter.app.data

import java.time.LocalDate

data class OverdueNote(
    val note: Note,
    val date: LocalDate
) {
    val id: String get() = note.id
    var text: String
        get() = note.text
        set(value) { note.text = value }
    var isCompleted: Boolean
        get() = note.isCompleted
        set(value) { note.isCompleted = value }
}


