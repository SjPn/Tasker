package com.noter.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.noter.app.adapter.NotesAdapter
import com.noter.app.data.DataManager
import com.noter.app.data.Note
import com.noter.app.databinding.FragmentFutureNotesBinding

class FutureNotesFragment : Fragment() {

    private var _binding: FragmentFutureNotesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var dataManager: DataManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFutureNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Инициализируем DataManager
        dataManager = DataManager(requireContext())

        setupUI()
        setupMenuButton()
        setupRecyclerView()
    }

    private fun setupUI() {
        binding.titleTextView.text = "Future Tasks"
        
        binding.addNoteButton.setOnClickListener {
            notesAdapter.addNote()
        }
        
        // Handle click on title to clear focus from notes
        binding.titleTextView.setOnClickListener {
            val currentFocus = requireActivity().currentFocus
            if (currentFocus != null) {
                currentFocus.clearFocus()
            }
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
        val notes = notesAdapter.getNotes()
        val uncheckedNotes = notes.count { !it.isCompleted }
        
        when {
            uncheckedNotes <= 5 -> {
                binding.infoMessageTextView.text = getString(R.string.work_is_not_wolf)
            }
            uncheckedNotes <= 10 -> {
                binding.infoMessageTextView.text = getString(R.string.dont_sit_straight)
            }
            uncheckedNotes > 15 -> {
                binding.infoMessageTextView.text = getString(R.string.lazy_pig)
            }
            else -> {
                binding.infoMessageTextView.text = getString(R.string.future_tasks_description)
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
            isFutureNotes = true
        )

        binding.notesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notesAdapter
        }
        
        // Загружаем сохраненные заметки
        loadNotes()
        
        // Обновляем информационный блок после инициализации адаптера
        updateInfoBlock()
        
        // Handle click on empty space to remove focus from edit text
        binding.root.setOnClickListener {
            // Hide keyboard and remove focus from any edit text
            val currentFocus = requireActivity().currentFocus
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
                val currentFocus = requireActivity().currentFocus
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
        updateInfoBlock() // Обновляем информационный блок после сохранения
    }
    
    private fun loadNotes() {
        // Инвалидируем кэш будущих заметок перед загрузкой
        dataManager.invalidateFutureNotesCache()
        val savedNotes = dataManager.loadFutureNotes()
        notesAdapter.submitList(savedNotes)
        updateInfoBlock() // Обновляем информационный блок после загрузки
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("FutureNotesFragment", "onResume called")
        // Обновляем данные при возвращении из других экранов
        loadNotes()
        updateInfoBlock() // Обновляем информационный блок
    }
    
    override fun onPause() {
        super.onPause()
        android.util.Log.d("FutureNotesFragment", "onPause called, saving notes")
        // Сохраняем заметки при выходе из фрагмента
        saveNotes()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Очищаем кэш при нехватке памяти
        dataManager.clearCache()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // Метод для принудительного обновления данных
    fun refreshData() {
        android.util.Log.d("FutureNotesFragment", "refreshData called")
        if (::dataManager.isInitialized) {
            // Загружаем данные заново
            loadNotes()
            updateInfoBlock() // Обновляем информационный блок
            android.util.Log.d("FutureNotesFragment", "refreshData completed")
        }
    }
} 