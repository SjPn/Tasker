package com.noter.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.noter.app.adapter.OverdueNotesAdapter
import com.noter.app.data.DataManager
import com.noter.app.data.Note
import com.noter.app.data.OverdueNote
import com.noter.app.databinding.FragmentOverdueNotesBinding
import java.time.LocalDate

class OverdueNotesFragment : Fragment() {

    private var _binding: FragmentOverdueNotesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var dataManager: DataManager
    private lateinit var notesAdapter: OverdueNotesAdapter
    private val overdueNotes = mutableListOf<OverdueNote>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOverdueNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        dataManager = DataManager(requireContext())
        setupRecyclerView()
        setupMenuButton()
        loadOverdueNotes()
        updateInfoBlock()
    }

    private fun setupRecyclerView() {
        notesAdapter = OverdueNotesAdapter(
            onNoteChanged = { overdueNote ->
                // Обновляем заметку в DataManager
                updateNote(overdueNote)
            },
            onNoteDeleted = { overdueNote ->
                // Удаляем заметку из списка
                overdueNotes.remove(overdueNote)
                updateEmptyState()
                notesAdapter.submitList(overdueNotes.toList())
            }
        )

        binding.overdueNotesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notesAdapter
        }
    }

    private fun loadOverdueNotes() {
        overdueNotes.clear()
        
        // Получаем все заметки за последние 30 дней
        val today = LocalDate.now()
        for (i in 1..30) {
            val date = today.minusDays(i.toLong())
            // Очищаем кэш для этой даты, чтобы получить свежие данные
            dataManager.invalidateCache(date)
            val notes = dataManager.loadNotes(date)
            
            // Добавляем только незавершенные заметки
            val incompleteNotes = notes.filter { !it.isCompleted }
            overdueNotes.addAll(incompleteNotes.map { OverdueNote(it, date) })
        }
        
        // Сортируем по дате (самые старые сначала)
        overdueNotes.sortBy { it.date }
        
        updateEmptyState()
        notesAdapter.submitList(overdueNotes.toList())
        updateInfoBlock()
        
        android.util.Log.d("OverdueNotesFragment", "Loaded ${overdueNotes.size} overdue notes")
    }

    private fun updateNote(overdueNote: OverdueNote) {
        try {
            android.util.Log.d("OverdueNotesFragment", "Updating note: ${overdueNote.id}, completed: ${overdueNote.isCompleted}")
            
            // Загружаем все заметки для этой даты
            val allNotesForDate = dataManager.loadNotes(overdueNote.date).toMutableList()
            
            // Находим и обновляем нужную заметку
            val noteIndex = allNotesForDate.indexOfFirst { it.id == overdueNote.note.id }
            if (noteIndex != -1) {
                allNotesForDate[noteIndex] = overdueNote.note
                android.util.Log.d("OverdueNotesFragment", "Updated note in allNotesForDate at index $noteIndex")
            } else {
                // Если заметка не найдена, добавляем её
                allNotesForDate.add(overdueNote.note)
                android.util.Log.d("OverdueNotesFragment", "Added new note to allNotesForDate")
            }
            
            // Сохраняем весь обновленный список заметок для этой даты
            dataManager.saveNotes(overdueNote.date, allNotesForDate)
            android.util.Log.d("OverdueNotesFragment", "Saved ${allNotesForDate.size} notes for date ${overdueNote.date}")
            
            // Очищаем кэш для этой даты, чтобы другие экраны увидели изменения
            dataManager.invalidateCache(overdueNote.date)
            
            // Если заметка стала завершенной, удаляем её из списка просроченных
            if (overdueNote.isCompleted) {
                android.util.Log.d("OverdueNotesFragment", "Note ${overdueNote.id} completed, removing from list")
                overdueNotes.remove(overdueNote)
                updateEmptyState()
                notesAdapter.submitList(overdueNotes.toList())
            } else {
                // Если заметка не завершена, обновляем её в списке
                val index = overdueNotes.indexOfFirst { it.id == overdueNote.id }
                if (index != -1) {
                    android.util.Log.d("OverdueNotesFragment", "Updating note ${overdueNote.id} at index $index")
                    overdueNotes[index] = overdueNote
                    notesAdapter.notifyItemChanged(index)
                } else {
                    android.util.Log.w("OverdueNotesFragment", "Note ${overdueNote.id} not found in list")
                }
            }
            
            // Обновляем информационный блок
            updateInfoBlock()
            
            android.util.Log.d("OverdueNotesFragment", "Successfully updated note: ${overdueNote.id}")
        } catch (e: Exception) {
            android.util.Log.e("OverdueNotesFragment", "Error updating note: ${overdueNote.id}", e)
        }
    }

    private fun updateEmptyState() {
        if (overdueNotes.isEmpty()) {
            binding.emptyStateTextView.visibility = View.VISIBLE
            binding.overdueNotesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateTextView.visibility = View.GONE
            binding.overdueNotesRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возвращении на экран
        refreshOverdueNotes()
    }
    
    // Метод для принудительного обновления просроченных заметок
    fun refreshOverdueNotes() {
        android.util.Log.d("OverdueNotesFragment", "refreshOverdueNotes called")
        if (::dataManager.isInitialized) {
            loadOverdueNotes()
        }
    }

    private fun setupMenuButton() {
        binding.menuButton.setOnClickListener {
            showPopupMenu(it)
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_export -> {
                    (requireActivity() as? MainPagerActivity)?.exportData()
                    true
                }
                R.id.action_import -> {
                    (requireActivity() as? MainPagerActivity)?.importData()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun updateInfoBlock() {
        binding.infoTitleTextView.text = "Overdue Tasks"
        
        if (overdueNotes.isEmpty()) {
            binding.infoMessageTextView.text = "No overdue tasks"
        } else {
            val totalNotes = overdueNotes.size
            val completedNotes = overdueNotes.count { it.isCompleted }
            val unfinishedNotes = totalNotes - completedNotes
            
            if (unfinishedNotes == 0) {
                binding.infoMessageTextView.text = "All overdue tasks completed!"
            } else {
                binding.infoMessageTextView.text = "You have $unfinishedNotes overdue tasks"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 