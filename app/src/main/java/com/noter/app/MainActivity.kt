package com.noter.app

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.noter.app.adapter.DaysAdapter
import com.noter.app.data.DataManager
import com.noter.app.data.Day
import com.noter.app.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var daysAdapter: DaysAdapter
    private lateinit var dataManager: DataManager
    private val days = mutableListOf<Day>()
    private var currentDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = DataManager(this)
        setupRecyclerView()
        generateDays()
        updateInfoBlock()
    }

    private fun setupRecyclerView() {
        daysAdapter = DaysAdapter { day ->
            // Открываем экран заметок для выбранного дня
            val intent = Intent(this, NotesActivity::class.java).apply {
                putExtra("date", day.date.toString())
                putExtra("title", day.getDisplayTitle())
            }
            startActivity(intent)
        }

        binding.daysRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = daysAdapter
        }
        
        // Плавные свайпы с возможностью перемещения по дням
        binding.daysRecyclerView.addOnItemTouchListener(object : androidx.recyclerview.widget.RecyclerView.OnItemTouchListener {
            private var startY = 0f
            private var startX = 0f
            private var isScrolling = false
            
            override fun onInterceptTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = e.y
                        startX = e.x
                        isScrolling = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val diffY = e.y - startY
                        val diffX = e.x - startX
                        
                        // Если свайп вертикальный и достаточно длинный (увеличили порог до 150 пикселей)
                        if (kotlin.math.abs(diffY) > 150 && kotlin.math.abs(diffY) > kotlin.math.abs(diffX)) {
                            isScrolling = true
                            return true
                        }
                    }
                }
                return false
            }
            
            override fun onTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: MotionEvent) {
                when (e.action) {
                    MotionEvent.ACTION_MOVE -> {
                        val diffY = e.y - startY
                        
                        if (isScrolling && kotlin.math.abs(diffY) > 150) {
                            // Вычисляем количество дней для перемещения на основе силы свайпа (порог 150 пикселей)
                            val daysToMove = (diffY / 150).toInt()
                            
                            if (daysToMove != 0) {
                                if (diffY > 0) {
                                    // Свайп вниз - назад по дням
                                    moveDays(-daysToMove)
                                } else {
                                    // Свайп вверх - вперед по дням
                                    moveDays(-daysToMove)
                                }
                                startY = e.y
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        isScrolling = false
                    }
                }
            }
            
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun generateDays() {
        days.clear()
        
        // Генерируем 7 дней недели начиная с текущего дня
        for (i in 0..6) {
            val dayDate = currentDate.plusDays(i.toLong())
            val dayOfWeek = dayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("en"))
            
            // Загружаем заметки для этого дня
            val notes = dataManager.loadNotes(dayDate)
            val day = Day(dayDate, dayOfWeek, notes)
            
            days.add(day)
        }

        daysAdapter.submitList(days)
        daysAdapter.updateCurrentDate(LocalDate.now())
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении из NotesActivity
        updateDaysData()
        updateInfoBlock()
    }
    
    private fun updateDaysData() {
        // Обновляем заметки для всех дней
        val updatedDays = days.map { day ->
            val notes = dataManager.loadNotes(day.date)
            Day(day.date, day.dayOfWeek, notes)
        }
        
        days.clear()
        days.addAll(updatedDays)
        daysAdapter.submitList(days)
        daysAdapter.updateCurrentDate(LocalDate.now())
        
        // Обновляем информационный блок
        updateInfoBlock()
    }
    
    private fun moveDays(days: Int) {
        currentDate = currentDate.plusDays(days.toLong())
        generateDays()
        
        // Плавная анимация перемещения
        val direction = if (days > 0) 1f else -1f
        binding.daysRecyclerView.animate()
            .alpha(0.7f)
            .translationY(direction * 30f)
            .setDuration(150)
            .withEndAction {
                binding.daysRecyclerView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(150)
                    .start()
            }
            .start()
        
        // Отладочная информация
        android.util.Log.d("MainActivity", "Moved $days days to: ${currentDate}")
    }
    
    private fun updateInfoBlock() {
        val today = LocalDate.now()
        val todayNotes = dataManager.loadNotes(today)
        
        // Устанавливаем заголовок с днем недели и датой
        val dayOfWeek = today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("en"))
        val dayOfMonth = today.dayOfMonth
        val month = today.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("en"))
        binding.infoTitleTextView.text = "$dayOfWeek, $dayOfMonth $month"
        
        if (todayNotes.isEmpty()) {
            // No notes for today
            binding.infoMessageTextView.text = "You have no tasks for today. Great day for rest!"
        } else {
            // Есть заметки, проверяем их статус
            val completedNotes = todayNotes.count { it.isCompleted }
            val totalNotes = todayNotes.size
            val unfinishedNotes = totalNotes - completedNotes
            
            if (unfinishedNotes == 0) {
                // All tasks completed
                binding.infoMessageTextView.text = "Excellent, well done. All tasks completed."
                binding.infoCard.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E8")) // Light green
            } else {
                // There are unfinished tasks
                binding.infoMessageTextView.text = "You have $unfinishedNotes unfinished tasks"
                binding.infoCard.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF3E0")) // Light orange
            }
        }
    }
} 