package com.noter.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class DataManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("notes_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cache = mutableMapOf<LocalDate, List<Note>>()
    private var futureNotesCache: List<Note>? = null
    
    fun saveNotes(date: LocalDate, notes: List<Note>) {
        val key = "notes_${date}"
        val notesJson = gson.toJson(notes)
        sharedPreferences.edit().putString(key, notesJson).apply()
        cache[date] = notes
    }
    
    fun loadNotes(date: LocalDate): List<Note> {
        // Проверяем кэш сначала
        cache[date]?.let { return it }
        
        val key = "notes_${date}"
        val notesJson = sharedPreferences.getString(key, null)
        val notes = if (notesJson != null) {
            try {
                val type = object : TypeToken<List<Note>>() {}.type
                gson.fromJson<List<Note>>(notesJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        // Сохраняем в кэш
        cache[date] = notes
        return notes
    }
    
    fun deleteNotes(date: LocalDate) {
        val key = "notes_${date}"
        sharedPreferences.edit().remove(key).apply()
        cache.remove(date)
    }
    
    // Методы для будущих заметок
    fun saveFutureNotes(notes: List<Note>) {
        val key = "future_notes"
        val notesJson = gson.toJson(notes)
        sharedPreferences.edit().putString(key, notesJson).apply()
        // Сохраняем в кэш
        futureNotesCache = notes
    }
    
    fun loadFutureNotes(): List<Note> {
        // Проверяем кэш сначала
        futureNotesCache?.let { return it }
        
        val key = "future_notes"
        val notesJson = sharedPreferences.getString(key, null)
        val notes = if (notesJson != null) {
            try {
                val type = object : TypeToken<List<Note>>() {}.type
                gson.fromJson<List<Note>>(notesJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        // Сохраняем в кэш
        futureNotesCache = notes
        return notes
    }
    
    // Методы для работы с кэшем
    fun invalidateCache(date: LocalDate) {
        cache.remove(date)
    }
    
    fun invalidateFutureNotesCache() {
        futureNotesCache = null
    }
    
    fun clearCache() {
        cache.clear()
        futureNotesCache = null
    }
    
    // Методы для экспорта/импорта всех данных
    fun exportAllData(): String {
        val allData = mutableMapOf<String, Any>()
        
        // Экспортируем заметки за последние 30 дней
        for (i in -15..15) {
            val date = LocalDate.now().plusDays(i.toLong())
            val notes = loadNotes(date)
            if (notes.isNotEmpty()) {
                allData["notes_${date}"] = notes
            }
        }
        
        // Экспортируем будущие заметки
        val futureNotes = loadFutureNotes()
        if (futureNotes.isNotEmpty()) {
            allData["future_notes"] = futureNotes
        }
        
        return gson.toJson(allData)
    }
    
    fun importAllData(jsonData: String): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val allData = gson.fromJson<Map<String, Any>>(jsonData, type)
            
            allData.forEach { (key, value) ->
                when {
                    key.startsWith("notes_") -> {
                        val dateStr = key.removePrefix("notes_")
                        val date = LocalDate.parse(dateStr)
                        val notesJson = gson.toJson(value)
                        val notesType = object : TypeToken<List<Note>>() {}.type
                        val notes = gson.fromJson<List<Note>>(notesJson, notesType) ?: emptyList()
                        saveNotes(date, notes)
                    }
                    key == "future_notes" -> {
                        val notesJson = gson.toJson(value)
                        val notesType = object : TypeToken<List<Note>>() {}.type
                        val notes = gson.fromJson<List<Note>>(notesJson, notesType) ?: emptyList()
                        saveFutureNotes(notes)
                    }
                }
            }
            
            // Очищаем кэш после импорта
            clearCache()
            true
        } catch (e: Exception) {
            android.util.Log.e("DataManager", "Import error", e)
            false
        }
    }
} 