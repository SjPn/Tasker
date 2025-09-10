package com.noter.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.noter.app.adapter.NotesAdapter
import com.noter.app.data.DataManager
import com.noter.app.data.Note
import com.noter.app.databinding.ActivityNotesBinding
import java.time.LocalDate

class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var currentDate: LocalDate
    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Глобально отключаем звуки кликов на экране заметок
        try { com.noter.app.util.ViewUtils.disableSoundEffectsDeep(binding.root) } catch (_: Exception) {}

        // Инициализируем DataManager
        dataManager = DataManager(this)

        // Получаем данные из Intent
        val dateString = intent.getStringExtra("date") ?: LocalDate.now().toString()
        val title = intent.getStringExtra("title") ?: "Notes"
        currentDate = LocalDate.parse(dateString)

        setupUI(title)
        setupRecyclerView()
        setupSwipeToGoBack()
        setupSimpleSwipeGesture()
    }

    private fun setupUI(title: String) {
        binding.dayTitleTextView.text = title
        
        binding.addNoteButton.setOnClickListener {
            notesAdapter.addNote()
        }
        
        // Handle click on title to clear focus from notes
        binding.dayTitleTextView.setOnClickListener {
            val currentFocus = currentFocus
            if (currentFocus != null) {
                currentFocus.clearFocus()
            }
        }
        
        // Добавляем обработчик на весь экран для снятия фокуса
        binding.root.setOnClickListener {
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
            currentDate = currentDate,
            onAllTasksCompleted = {
                try {
                    val konfetti = binding.confettiNotes
                    val party: nl.dionsegijn.konfetti.core.Party =
                        nl.dionsegijn.konfetti.core.PartyFactory(
                            nl.dionsegijn.konfetti.core.emitter.Emitter(1, java.util.concurrent.TimeUnit.SECONDS).perSecond(250)
                        )
                            .spread(360)
                            .timeToLive(1200L)
                            .position(0.5, 0.0)
                            .build()
                    konfetti.start(party)
                } catch (_: Exception) {}
            }
        )

        binding.notesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotesActivity)
            adapter = notesAdapter
        }
        
        // Загружаем сохраненные заметки
        loadNotes()
    }
    
    private fun setupSwipeToGoBack() {
        // Временно отключаем ItemTouchHelper для исключения конфликтов
        // val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        //     0,
        //     ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        // ) {
        //     override fun onMove(
        //         recyclerView: androidx.recyclerview.widget.RecyclerView,
        //         viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
        //         target: androidx.recyclerview.widget.RecyclerView.ViewHolder
        //     ): Boolean {
        //         return false
        //     }

        //     override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
        //         when (direction) {
        //             ItemTouchHelper.LEFT, ItemTouchHelper.RIGHT -> {
        //                 // Свайп влево или вправо - возврат в главное окно
        //                 finish()
        //             }
        //         }
        //     }
            
        //     override fun onChildDraw(
        //         c: android.graphics.Canvas,
        //         recyclerView: androidx.recyclerview.widget.RecyclerView,
        //         viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
        //         dX: Float,
        //         dY: Float,
        //         actionState: Int,
        //         isCurrentlyActive: Boolean
        //     ) {
        //         // Показываем визуальную обратную связь при свайпе
        //         if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
        //             val itemView = viewHolder.itemView
        //             val alpha = 1f - kotlin.math.abs(dX) / itemView.width
        //             itemView.alpha = alpha.coerceIn(0.3f, 1f)
                    
        //             // Добавляем тень для эффекта глубины
        //             itemView.elevation = if (isCurrentlyActive) 8f else 4f
                    
        //             // Показываем подсказку о возврате
        //             if (kotlin.math.abs(dX) > itemView.width * 0.3f) {
        //                 itemView.background = android.graphics.drawable.ColorDrawable(
        //                     android.graphics.Color.parseColor("#4CAF50")
        //                 )
        //             }
        //         }
        //         super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        //     }
            
        //     override fun clearView(
        //         recyclerView: androidx.recyclerview.widget.RecyclerView,
        //         viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder
        //     ) {
        //         super.clearView(recyclerView, viewHolder)
        //         // Восстанавливаем нормальное состояние элемента
        //         viewHolder.itemView.alpha = 1f
        //         viewHolder.itemView.elevation = 0f
        //         viewHolder.itemView.background = null
        //     }
        // })
        
        // itemTouchHelper.attachToRecyclerView(binding.notesRecyclerView)
    }
    
    private fun saveNotes() {
        val notes = notesAdapter.getNotes()
        dataManager.saveNotes(currentDate, notes)
        dataManager.invalidateCache(currentDate)
        android.util.Log.d("NotesActivity", "Saved ${notes.size} notes for $currentDate")
    }
    
    private fun loadNotes() {
        val savedNotes = dataManager.loadNotes(currentDate)
        notesAdapter.submitList(savedNotes)
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("NotesActivity", "onResume called, reloading notes")
        // Перезагружаем заметки при возвращении в активность
        loadNotes()
    }
    
    override fun onPause() {
        super.onPause()
        android.util.Log.d("NotesActivity", "onPause called, saving notes")
        // Сохраняем заметки при выходе из активности
        saveNotes()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Очищаем кэш при нехватке памяти
        dataManager.clearCache()
    }

    private fun setupSimpleSwipeGesture() {
        // Используем GestureDetector для надежного распознавания свайпов
        val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: android.view.MotionEvent?,
                e2: android.view.MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                
                // Если горизонтальный свайп больше 100px и в 2 раза больше вертикального
                if (kotlin.math.abs(deltaX) > 100 && kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) * 2) {
                    android.util.Log.d("NotesActivity", "Fling detected: deltaX=$deltaX, deltaY=$deltaY, velocityX=$velocityX")
                    finish()
                    return true
                }
                return false
            }
        })
        
        // Функция для снятия фокуса
        fun clearFocusIfNeeded() {
            val currentFocus = currentFocus
            if (currentFocus != null) {
                currentFocus.clearFocus()
            }
        }
        
        // Добавляем обработчик на корневой layout
        binding.root.setOnTouchListener { _, event ->
            // Сначала обрабатываем свайп
            val gestureHandled = gestureDetector.onTouchEvent(event)
            
            // Если это не свайп и это ACTION_UP (клик), то убираем фокус
            if (!gestureHandled && event.action == android.view.MotionEvent.ACTION_UP) {
                clearFocusIfNeeded()
            }
            
            false // Не перехватываем событие, позволяем другим обработчикам работать
        }
        
        // Добавляем обработчик на заголовок
        binding.dayTitleTextView.setOnTouchListener { _, event ->
            // Сначала обрабатываем свайп
            val gestureHandled = gestureDetector.onTouchEvent(event)
            
            // Если это не свайп и это ACTION_UP (клик), то убираем фокус
            if (!gestureHandled && event.action == android.view.MotionEvent.ACTION_UP) {
                clearFocusIfNeeded()
            }
            
            false // Не перехватываем событие
        }
        
        // Добавляем обработчик на RecyclerView
        binding.notesRecyclerView.setOnTouchListener { _, event ->
            // Сначала обрабатываем свайп
            val gestureHandled = gestureDetector.onTouchEvent(event)
            
            // Если это не свайп и это ACTION_UP (клик), то убираем фокус
            if (!gestureHandled && event.action == android.view.MotionEvent.ACTION_UP) {
                clearFocusIfNeeded()
            }
            
            false // Не перехватываем событие, позволяем RecyclerView работать
        }
        
        // Добавляем обработчик на кнопку добавления
        binding.addNoteButton.setOnTouchListener { _, event ->
            // Сначала обрабатываем свайп
            val gestureHandled = gestureDetector.onTouchEvent(event)
            
            // Если это не свайп и это ACTION_UP (клик), то убираем фокус
            if (!gestureHandled && event.action == android.view.MotionEvent.ACTION_UP) {
                clearFocusIfNeeded()
            }
            
            false // Не перехватываем событие, позволяем кнопке работать
        }
    }
} 