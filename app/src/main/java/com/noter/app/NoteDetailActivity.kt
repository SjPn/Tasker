package com.noter.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.noter.app.data.DataManager
import com.noter.app.data.Note
import com.noter.app.databinding.ActivityNoteDetailBinding
import java.time.LocalDate

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var dataManager: DataManager
    private var originalNoteText: String = ""
    private var noteId: String = ""
    private var currentDate: LocalDate = LocalDate.now()
    private var isFutureNote: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем DataManager
        dataManager = DataManager(this)

        // Получаем данные из Intent
        originalNoteText = intent.getStringExtra("note_text") ?: ""
        noteId = intent.getStringExtra("note_id") ?: ""
        val dateString = intent.getStringExtra("date") ?: LocalDate.now().toString()
        currentDate = LocalDate.parse(dateString)
        isFutureNote = intent.getBooleanExtra("is_future_note", false)
        
        android.util.Log.d("NoteDetailActivity", "onCreate - Received data:")
        android.util.Log.d("NoteDetailActivity", "Original text: '$originalNoteText'")
        android.util.Log.d("NoteDetailActivity", "Note ID: '$noteId'")
        android.util.Log.d("NoteDetailActivity", "Date string: '$dateString'")
        android.util.Log.d("NoteDetailActivity", "Parsed date: $currentDate")
        android.util.Log.d("NoteDetailActivity", "Is future note: $isFutureNote")

        setupUI()
    }

    private fun setupUI() {
        // Устанавливаем текст заметки
        binding.noteEditText.setText(originalNoteText)
        
        // Устанавливаем заголовок
        binding.toolbarTitle.text = "Редактирование заметки"
        
        // Обработчик кнопки "Назад"
        binding.backButton.setOnClickListener {
            saveAndFinish()
        }
        
        // Обработчик кнопки "Сохранить"
        binding.saveButton.setOnClickListener {
            saveAndFinish()
        }
        
        // Обработчик кнопки "Удалить"
        binding.deleteButton.setOnClickListener {
            deleteNoteAndFinish()
        }
        
        // Автоматически фокусируемся на поле ввода
        binding.noteEditText.requestFocus()
        
        // Показываем клавиатуру
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.noteEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun saveAndFinish() {
        val updatedText = binding.noteEditText.text.toString()
        
        android.util.Log.d("NoteDetailActivity", "saveAndFinish called")
        android.util.Log.d("NoteDetailActivity", "Original text: '$originalNoteText'")
        android.util.Log.d("NoteDetailActivity", "Updated text: '$updatedText'")
        android.util.Log.d("NoteDetailActivity", "Note ID: $noteId")
        android.util.Log.d("NoteDetailActivity", "Date: $currentDate")
        
        // Если текст изменился, сохраняем изменения
        if (updatedText != originalNoteText) {
            android.util.Log.d("NoteDetailActivity", "Text changed, saving...")
            saveNoteChanges(updatedText)
        } else {
            android.util.Log.d("NoteDetailActivity", "Text unchanged, skipping save")
        }
        
        finish()
    }
    
    private fun deleteNoteAndFinish() {
        // Удаляем заметку в зависимости от типа
        if (isFutureNote) {
            val notes = dataManager.loadFutureNotes().toMutableList()
            val noteToDelete = notes.find { it.id == noteId }
            noteToDelete?.let {
                notes.remove(it)
                dataManager.saveFutureNotes(notes)
                dataManager.invalidateFutureNotesCache()
            }
        } else {
            val notes = dataManager.loadNotes(currentDate).toMutableList()
            val noteToDelete = notes.find { it.id == noteId }
            noteToDelete?.let {
                notes.remove(it)
                dataManager.saveNotes(currentDate, notes)
                dataManager.invalidateCache(currentDate)
            }
        }
        
        finish()
    }
    
    private fun saveNoteChanges(newText: String) {
        android.util.Log.d("NoteDetailActivity", "saveNoteChanges called with text: '$newText'")
        android.util.Log.d("NoteDetailActivity", "Is future note: $isFutureNote")
        
        if (isFutureNote) {
            // Сохраняем в будущие заметки
            val notes = dataManager.loadFutureNotes().toMutableList()
            android.util.Log.d("NoteDetailActivity", "Loaded ${notes.size} future notes")
            
            val noteToUpdate = notes.find { it.id == noteId }
            android.util.Log.d("NoteDetailActivity", "Found future note to update: ${noteToUpdate != null}")
            
            noteToUpdate?.let { note ->
                android.util.Log.d("NoteDetailActivity", "Updating future note: ${note.id}, old text: '${note.text}'")
                note.text = newText
                android.util.Log.d("NoteDetailActivity", "New text set: '${note.text}'")
                
                dataManager.saveFutureNotes(notes)
                android.util.Log.d("NoteDetailActivity", "Future notes saved to DataManager")
                
                dataManager.invalidateFutureNotesCache()
                android.util.Log.d("NoteDetailActivity", "Future notes cache invalidated")
            } ?: run {
                android.util.Log.e("NoteDetailActivity", "Future note with ID $noteId not found!")
            }
        } else {
            // Сохраняем в обычные заметки по дате
            val notes = dataManager.loadNotes(currentDate).toMutableList()
            android.util.Log.d("NoteDetailActivity", "Loaded ${notes.size} notes for date $currentDate")
            
            val noteToUpdate = notes.find { it.id == noteId }
            android.util.Log.d("NoteDetailActivity", "Found note to update: ${noteToUpdate != null}")
            
            noteToUpdate?.let { note ->
                android.util.Log.d("NoteDetailActivity", "Updating note: ${note.id}, old text: '${note.text}'")
                note.text = newText
                android.util.Log.d("NoteDetailActivity", "New text set: '${note.text}'")
                
                dataManager.saveNotes(currentDate, notes)
                android.util.Log.d("NoteDetailActivity", "Notes saved to DataManager")
                
                dataManager.invalidateCache(currentDate)
                android.util.Log.d("NoteDetailActivity", "Cache invalidated")
            } ?: run {
                android.util.Log.e("NoteDetailActivity", "Note with ID $noteId not found!")
            }
        }
    }
    
    override fun onBackPressed() {
        saveAndFinish()
    }
} 