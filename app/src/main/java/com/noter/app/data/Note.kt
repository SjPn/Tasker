package com.noter.app.data

data class Note(
    val id: String = System.currentTimeMillis().toString(),
    var text: String = "",
    var isCompleted: Boolean = false
) 