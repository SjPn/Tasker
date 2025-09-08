package com.noter.app.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noter.app.data.OverdueNote
import com.noter.app.databinding.ItemOverdueNoteBinding
import com.noter.app.NoteDetailActivity
import com.noter.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class OverdueNotesAdapter(
    private val onNoteChanged: (OverdueNote) -> Unit,
    private val onNoteDeleted: (OverdueNote) -> Unit
) : RecyclerView.Adapter<OverdueNotesAdapter.OverdueNoteViewHolder>() {

    private var notes = listOf<OverdueNote>()

    fun submitList(newNotes: List<OverdueNote>) {
        try {
            android.util.Log.d("OverdueNotesAdapter", "Submitting new list with ${newNotes.size} notes")
            notes = newNotes
            notifyDataSetChanged()
            android.util.Log.d("OverdueNotesAdapter", "Successfully updated list")
        } catch (e: Exception) {
            android.util.Log.e("OverdueNotesAdapter", "Error submitting list", e)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverdueNoteViewHolder {
        val binding = ItemOverdueNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OverdueNoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OverdueNoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    inner class OverdueNoteViewHolder(
        private val binding: ItemOverdueNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentNote: OverdueNote? = null
        private var textWatcher: TextWatcher? = null

        private fun updateNoteStroke(note: OverdueNote) {
            try {
                // Убираем заливку полностью
                binding.root.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                
                if (note.isCompleted) {
                    // Выполненная задача - синяя окантовка
                    binding.root.setStrokeWidth(3)
                    binding.root.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2")))
                } else {
                    // Невыполненная задача - красная окантовка
                    binding.root.setStrokeWidth(3)
                    binding.root.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")))
                }
                
                binding.root.invalidate()
                binding.root.requestLayout()
            } catch (e: Exception) {
                android.util.Log.e("OverdueNotesAdapter", "Error updating stroke", e)
            }
        }

        fun bind(note: OverdueNote) {
            try {
                android.util.Log.d("OverdueNotesAdapter", "Binding note: ${note.id}, text: ${note.text}, completed: ${note.isCompleted}")
                
                currentNote = note
                
                // Remove previous text watcher to avoid conflicts
                textWatcher?.let { binding.noteEditText.removeTextChangedListener(it) }
                
                // Set text without triggering text watcher
                binding.noteEditText.setText(note.text)
                binding.noteCheckBox.isChecked = note.isCompleted
                
                // Set date
                val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
                binding.noteDateTextView.text = note.date.format(formatter)
                
                // Show expand button only if note is not empty
                binding.expandNoteButton.visibility = if (note.text.trim().isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                
                // Add text watcher
                textWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        try {
                            currentNote?.let { overdueNote ->
                                val newText = s?.toString() ?: ""
                                overdueNote.text = newText
                                android.util.Log.d("OverdueNotesAdapter", "Text changed for note ${overdueNote.id}: $newText")
                                onNoteChanged(overdueNote)
                                
                                // Update expand button visibility
                                binding.expandNoteButton.visibility = if (newText.trim().isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("OverdueNotesAdapter", "Error in text watcher", e)
                        }
                    }
                }
                binding.noteEditText.addTextChangedListener(textWatcher)
                
                // Add checkbox listener
                binding.noteCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    try {
                        currentNote?.let { overdueNote ->
                            overdueNote.isCompleted = isChecked
                            android.util.Log.d("OverdueNotesAdapter", "Checkbox changed for note ${overdueNote.id}: $isChecked")
                            updateNoteStroke(overdueNote)
                            onNoteChanged(overdueNote)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueNotesAdapter", "Error in checkbox listener", e)
                    }
                }
                
                // Add focus change listener
                binding.noteEditText.onFocusChangeListener = android.view.View.OnFocusChangeListener { _, hasFocus ->
                    try {
                        currentNote?.let { overdueNote ->
                            // Всегда оставляем прозрачную заливку
                            binding.root.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                            if (!hasFocus) {
                                // При потере фокуса обновляем окантовку
                                updateNoteStroke(overdueNote)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueNotesAdapter", "Error in focus listener", e)
                    }
                }
                
                // Handle expand button click
                binding.expandNoteButton.setOnClickListener {
                    try {
                        currentNote?.let { overdueNote ->
                            // Получаем актуальный текст из EditText
                            val currentText = binding.noteEditText.text.toString()
                            android.util.Log.d("OverdueNotesAdapter", "Expand button clicked, current text: '$currentText', note text: '${overdueNote.text}'")
                            
                            val intent = android.content.Intent(binding.root.context, NoteDetailActivity::class.java).apply {
                                putExtra("note_text", currentText)
                                putExtra("note_id", overdueNote.id)
                                putExtra("date", overdueNote.date.toString())
                            }
                            binding.root.context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueNotesAdapter", "Error in expand button click", e)
                    }
                }
                
                // Initial stroke update
                updateNoteStroke(note)
                
                android.util.Log.d("OverdueNotesAdapter", "Successfully bound note: ${note.id}")
            } catch (e: Exception) {
                android.util.Log.e("OverdueNotesAdapter", "Error binding note: ${note.id}", e)
            }
        }
    }
} 