package com.noter.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.noter.app.adapter.NotesAdapter
import com.noter.app.data.DataManager
import com.noter.app.data.Note
import com.noter.app.databinding.ActivityFutureNotesBinding
import java.time.LocalDate

class FutureNotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFutureNotesBinding
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFutureNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем DataManager
        dataManager = DataManager(this)

        setupUI()
        setupRecyclerView()
    }

    private fun setupUI() {
        binding.titleTextView.text = "Не распределенные дела"
        
        binding.addNoteButton.setOnClickListener {
            notesAdapter.addNote()
        }
        
        // Handle click on title to clear focus from notes
        binding.titleTextView.setOnClickListener {
            val currentFocus = currentFocus
            if (currentFocus != null) {
                currentFocus.clearFocus()
            }
        }
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onNoteChanged = { _ ->
                // Заметка изменена - сохраняем все заметки
                saveNotes()
            },
            onNoteDeleted = { _ ->
                // Заметка удалена - сохраняем все заметки
                saveNotes()
            },
            currentDate = LocalDate.now() // Для будущих заметок используем текущую дату
        )

        binding.notesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FutureNotesActivity)
            adapter = notesAdapter
        }
        
        // Загружаем сохраненные заметки
        loadNotes()
        
        // Handle click on empty space to remove focus from edit text
        binding.root.setOnClickListener {
            // Hide keyboard and remove focus from any edit text
            val currentFocus = currentFocus
            if (currentFocus != null) {
                currentFocus.clearFocus()
            }
        }
        
        // Handle click on RecyclerView empty space
        binding.notesRecyclerView.setOnTouchListener { _, event ->
            // Check if touch is on empty space (not on a note item)
            val childView = binding.notesRecyclerView.findChildViewUnder(event.x, event.y)
            if (childView == null) {
                // Touch is on empty space, clear focus to save current note
                val currentFocus = currentFocus
                if (currentFocus != null) {
                    currentFocus.clearFocus()
                }
                return@setOnTouchListener true
            }
            false
        }
    }
    
    private fun saveNotes() {
        val notes = notesAdapter.getNotes()
        dataManager.saveFutureNotes(notes)
    }
    
    private fun loadNotes() {
        val savedNotes = dataManager.loadFutureNotes()
        notesAdapter.submitList(savedNotes)
    }
    
    override fun onPause() {
        super.onPause()
        android.util.Log.d("FutureNotesActivity", "onPause called, saving notes")
        // Сохраняем заметки при выходе из активности
        saveNotes()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Очищаем кэш при нехватке памяти
        dataManager.clearCache()
    }
} 