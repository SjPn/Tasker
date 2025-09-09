package com.noter.app.adapter

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noter.app.NoteDetailActivity
import com.noter.app.data.Note
import com.noter.app.databinding.ItemNoteBinding
import com.noter.app.R
import java.time.LocalDate

class NotesAdapter(
    private val onNoteChanged: (Note) -> Unit,
    private val onNoteDeleted: (Note) -> Unit,
    private val currentDate: LocalDate = LocalDate.now(),
    private val isFutureNotes: Boolean = false,
    private val onAllTasksCompleted: () -> Unit = {}
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private var notes = listOf<Note>()
    private var lastAllCompleted = false

    fun submitList(newNotes: List<Note>) {
        updateNotes(newNotes)
    }
    
    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
        evaluateCompletion()
    }
    
    fun getNotes(): List<Note> {
        return notes
    }

    private fun evaluateCompletion() {
        val hasTasks = notes.any { it.text.trim().isNotEmpty() }
        val allCompleted = hasTasks && notes.filter { it.text.trim().isNotEmpty() }.all { it.isCompleted }
        if (allCompleted && !lastAllCompleted) {
            lastAllCompleted = true
            onAllTasksCompleted()
        }
        if (!allCompleted) {
            lastAllCompleted = false
        }
    }

    fun addNote() {
        val newNote = Note()
        val newList = notes.toMutableList()
        newList.add(newNote)
        notes = newList
        notifyItemInserted(notes.size - 1)
        // Уведомляем об изменении
        onNoteChanged(newNote)
        evaluateCompletion()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentNote: Note? = null
        private var textWatcher: TextWatcher? = null
        
        // Функция для обновления окантовки заметки
        private fun updateNoteStroke(note: Note) {
            try {
                android.util.Log.d("NotesAdapter", "updateNoteStroke called for note: ${note.text}, completed: ${note.isCompleted}")
                
                if (note.isCompleted) {
                    binding.root.setStrokeWidth(3)
                    binding.root.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2")))
                    android.util.Log.d("NotesAdapter", "Setting stroke for completed note: ${note.text}")
                } else {
                    binding.root.setStrokeWidth(0)
                    binding.root.setStrokeColor(null)
                    android.util.Log.d("NotesAdapter", "Removing stroke for incomplete note: ${note.text}")
                }
                
                // Принудительно обновляем view
                binding.root.invalidate()
                binding.root.requestLayout()
                
                android.util.Log.d("NotesAdapter", "Stroke update completed for note: ${note.text}")
            } catch (e: Exception) {
                android.util.Log.e("NotesAdapter", "Error updating stroke", e)
            }
        }

        fun bind(note: Note) {
            currentNote = note
            
            // Remove previous text watcher to avoid conflicts
            textWatcher?.let { binding.noteEditText.removeTextChangedListener(it) }
            
            // Set text without triggering text watcher
            binding.noteEditText.setText(note.text)
            binding.noteCheckBox.isChecked = note.isCompleted
            
            // Показываем кнопку разворачивания только если заметка не пустая
            binding.expandNoteButton.visibility = if (note.text.trim().isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            
            // Create new text watcher
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    currentNote?.let { note ->
                        val newText = s?.toString() ?: ""
                        if (note.text != newText) {
                            note.text = newText
                            onNoteChanged(note)
                            
                            // Обновляем видимость кнопки разворачивания
                            binding.expandNoteButton.visibility = if (newText.trim().isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                        }
                    }
                }
            }
            
            binding.noteEditText.addTextChangedListener(textWatcher)
            
            // Handle checkbox changes
            binding.noteCheckBox.setOnCheckedChangeListener { _, isChecked ->
                currentNote?.let { note ->
                    note.isCompleted = isChecked
                    onNoteChanged(note)
                    
                    // Обновляем окантовку в зависимости от статуса галочки
                    updateNoteStroke(note)
                    evaluateCompletion()
                }
            }
            
            // Handle expand button click
            binding.expandNoteButton.setOnClickListener {
                currentNote?.let { note ->
                    // Получаем актуальный текст из EditText
                    val currentText = binding.noteEditText.text.toString()
                    android.util.Log.d("NotesAdapter", "Expand button clicked, current text: '$currentText', note text: '${note.text}'")
                    
                    val intent = Intent(binding.root.context, NoteDetailActivity::class.java).apply {
                        putExtra("note_text", currentText)
                        putExtra("note_id", note.id)
                        putExtra("date", currentDate.toString())
                        putExtra("is_future_note", isFutureNotes)
                    }
                    binding.root.context.startActivity(intent)
                }
            }
            
            // Handle focus change to save note when clicking outside
            binding.noteEditText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    // Gained focus - highlight the note and show delete button
                    binding.root.setCardBackgroundColor(binding.root.context.getColor(R.color.note_highlight))
                    binding.deleteNoteButton.visibility = android.view.View.VISIBLE
                } else {
                    // Lost focus - save the note and remove highlight
                    binding.root.setCardBackgroundColor(binding.root.context.getColor(R.color.background_card))
                    binding.deleteNoteButton.visibility = android.view.View.GONE
                    currentNote?.let { note ->
                        val currentText = binding.noteEditText.text.toString()
                        if (note.text != currentText) {
                            note.text = currentText
                            onNoteChanged(note)
                        }
                        
                        // Восстанавливаем окантовку в зависимости от статуса заметки
                        updateNoteStroke(note)
                        evaluateCompletion()
                        
                        // Remove empty notes (only if it's not the last note)
                        if (currentText.trim().isEmpty() && notes.size > 1) {
                            val position = bindingAdapterPosition
                            if (position != RecyclerView.NO_POSITION) {
                                onNoteDeleted(note)
                                val newList = notes.toMutableList()
                                newList.removeAt(position)
                                notes = newList
                                notifyItemRemoved(position)
                            }
                        }
                    }
                }
            }
            
            // Handle delete button click
            binding.deleteNoteButton.setOnClickListener {
                currentNote?.let { note ->
                    onNoteDeleted(note)
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        // Анимация удаления
                        binding.root.animate()
                            .alpha(0f)
                            .translationX(-binding.root.width.toFloat())
                            .setDuration(300)
                            .withEndAction {
                                val newList = notes.toMutableList()
                                newList.removeAt(position)
                                notes = newList
                                notifyItemRemoved(position)
                            }
                            .start()
                    }
                }
            }
            
            // Handle long click to delete note (alternative method)
            binding.root.setOnLongClickListener {
                currentNote?.let { note ->
                    onNoteDeleted(note)
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        // Анимация удаления
                        binding.root.animate()
                            .alpha(0f)
                            .translationX(binding.root.width.toFloat())
                            .setDuration(300)
                            .withEndAction {
                                val newList = notes.toMutableList()
                                newList.removeAt(position)
                                notes = newList
                                notifyItemRemoved(position)
                            }
                            .start()
                    }
                }
                true
            }
            
            // Устанавливаем начальную окантовку в зависимости от статуса заметки
            android.util.Log.d("NotesAdapter", "Initial binding for note: ${note.text}, completed: ${note.isCompleted}")
            updateNoteStroke(note)
        }
    }
} 