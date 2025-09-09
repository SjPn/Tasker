package com.noter.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.noter.app.adapter.DaysAdapter
import com.noter.app.data.DataManager
import com.noter.app.data.Day
import com.noter.app.databinding.FragmentMainBinding
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartyFactory
import nl.dionsegijn.konfetti.core.emitter.Emitter

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var daysAdapter: DaysAdapter
    private lateinit var dataManager: DataManager
    private var currentDate = LocalDate.now()
    
    // Система бесконечной навигации
    private var allDays = mutableListOf<Day>() // Все загруженные дни
    private var currentCenterIndex = 0 // Индекс центрального дня в списке
    private val initialDaysCount = 100 // Начальное количество дней (50 в прошлое + 50 в будущее)
    
    // Параметры для бесконечной прокрутки
    private val loadMoreThreshold = 10 // За сколько элементов до края начинать подгрузку
    private var isLoadingMore = false // Флаг для предотвращения множественных загрузок

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        dataManager = DataManager(requireContext())
        setupRecyclerView()
        setupInfoBlockClick()
        setupMenuButton()
        generateDays()
        updateInfoBlock()
        
        // Дополнительное центрирование через некоторое время после создания view
        binding.root.postDelayed({
            val today = LocalDate.now()
            val todayIndex = allDays.indexOfFirst { it.date == today }
            if (todayIndex != -1) {
                android.util.Log.d("MainFragment", "Additional centering to today at index: $todayIndex")
                centerDayOnScreen(todayIndex)
            }
        }, 500) // 500ms задержка для полной инициализации
    }

    private fun setupRecyclerView() {
        daysAdapter = DaysAdapter { day ->
            // Открываем экран заметок для выбранного дня
            val intent = Intent(requireContext(), NotesActivity::class.java).apply {
                putExtra("date", day.date.toString())
                putExtra("title", day.getDisplayTitle())
            }
            startActivity(intent)
        }

        binding.daysRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = daysAdapter
            
            // Добавляем отступы между элементами
            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: android.view.View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    outRect.top = if (position == 0) 0 else 8
                    outRect.bottom = 8
                }
            })
            
            // Добавляем слушатель прокрутки для бесконечной навигации
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    if (isLoadingMore) return // Предотвращаем множественные загрузки
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                    val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                    val totalItemCount = layoutManager.itemCount
                    
                    android.util.Log.d("MainFragment", "SCROLL: first=$firstVisiblePosition, last=$lastVisiblePosition, total=$totalItemCount")
                    
                    // Проверяем, нужно ли подгрузить дни в начало (прошлое)
                    if (firstVisiblePosition <= loadMoreThreshold) {
                        android.util.Log.d("MainFragment", "LOAD MORE PAST: firstVisible=$firstVisiblePosition, threshold=$loadMoreThreshold")
                        loadMoreDaysToPast()
                    }
                    
                    // Проверяем, нужно ли подгрузить дни в конец (будущее)
                    if (lastVisiblePosition >= totalItemCount - loadMoreThreshold) {
                        android.util.Log.d("MainFragment", "LOAD MORE FUTURE: lastVisible=$lastVisiblePosition, total=$totalItemCount, threshold=$loadMoreThreshold")
                        loadMoreDaysToFuture()
                    }
                }
            })
        }
    }
    
    private fun setupInfoBlockClick() {
        binding.infoCard.setOnClickListener {
            android.util.Log.d("MainFragment", "Info block clicked - centering to today")
            centerToToday()
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
                    exportData()
                    true
                }
                R.id.action_import -> {
                    importData()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun exportData() {
        try {
            val activity = requireActivity() as? MainPagerActivity
            activity?.exportData()
        } catch (e: Exception) {
            android.util.Log.e("MainFragment", "Export error", e)
            android.widget.Toast.makeText(requireContext(), getString(R.string.export_error), android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun importData() {
        try {
            val activity = requireActivity() as? MainPagerActivity
            activity?.importData()
        } catch (e: Exception) {
            android.util.Log.e("MainFragment", "Import error", e)
            android.widget.Toast.makeText(requireContext(), getString(R.string.import_error), android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun centerToToday() {
        val today = LocalDate.now()
        
        // Ищем индекс сегодняшнего дня в списке
        val todayIndex = allDays.indexOfFirst { it.date == today }
        
        if (todayIndex != -1) {
            // Сегодняшний день найден в списке
            android.util.Log.d("MainFragment", "Today found at index: $todayIndex")
            
            // Обновляем центральный индекс
            currentCenterIndex = todayIndex
            currentDate = today
            
            // Прокручиваем к сегодняшнему дню с центрированием на экране
            centerDayOnScreen(todayIndex)
            
            // Добавляем визуальную обратную связь
            binding.infoCard.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    binding.infoCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
        } else {
            // Сегодняшний день не найден - пересоздаем список с центром на сегодня
            android.util.Log.d("MainFragment", "Today not found in list, regenerating with today as center")
            currentDate = today
            generateDays()
        }
    }
    
    private fun centerDayOnScreen(dayIndex: Int) {
        binding.daysRecyclerView.post {
            val layoutManager = binding.daysRecyclerView.layoutManager as LinearLayoutManager
            
            // Проверяем, что RecyclerView готов
            if (binding.daysRecyclerView.height <= 0) {
                android.util.Log.d("MainFragment", "RecyclerView height is 0, retrying...")
                centerDayOnScreenWithDelay(dayIndex)
                return@post
            }
            
            // Получаем размеры экрана и элемента
            val screenHeight = binding.daysRecyclerView.height
            val itemHeight = binding.daysRecyclerView.getChildAt(0)?.height ?: 200 // Увеличиваем примерную высоту
            
            // Выравниваем элемент строго к верху контента RecyclerView
            val offset = 0
            
            // Прокручиваем к элементу с позиционированием в верхней части
            layoutManager.scrollToPositionWithOffset(dayIndex, offset)
            
            android.util.Log.d("MainFragment", "Positioned day at top at index: $dayIndex with offset: $offset, screenHeight: $screenHeight, itemHeight: $itemHeight")
        }
    }
    
    private fun centerDayOnScreenWithDelay(dayIndex: Int) {
        // Добавляем задержку для полной инициализации RecyclerView
        binding.daysRecyclerView.postDelayed({
            // Проверяем, что RecyclerView готов
            if (binding.daysRecyclerView.height > 0 && binding.daysRecyclerView.childCount > 0) {
                centerDayOnScreen(dayIndex)
            } else {
                // Если еще не готов, пробуем еще раз через небольшую задержку
                android.util.Log.d("MainFragment", "RecyclerView not ready, retrying centering...")
                centerDayOnScreenWithDelay(dayIndex)
            }
        }, 300) // Увеличиваем задержку до 300ms
    }
    
    private fun centerDayOnScreenWithMultipleAttempts(dayIndex: Int, attempt: Int = 0) {
        val maxAttempts = 15 // Увеличиваем максимальное количество попыток
        
        if (attempt >= maxAttempts) {
            android.util.Log.e("MainFragment", "Failed to center after $maxAttempts attempts")
            return
        }
        
        binding.daysRecyclerView.postDelayed({
            // Проверяем готовность RecyclerView более тщательно
            val isReady = binding.daysRecyclerView.height > 0 && 
                         binding.daysRecyclerView.childCount > 0 &&
                         binding.daysRecyclerView.layoutManager != null &&
                         dayIndex < binding.daysRecyclerView.adapter?.itemCount ?: 0
            
            if (isReady) {
                android.util.Log.d("MainFragment", "RecyclerView ready on attempt ${attempt + 1}, centering...")
                centerDayOnScreen(dayIndex)
            } else {
                android.util.Log.d("MainFragment", "RecyclerView not ready on attempt ${attempt + 1}, retrying...")
                centerDayOnScreenWithMultipleAttempts(dayIndex, attempt + 1)
            }
        }, 300) // Увеличиваем задержку до 300ms
    }

    private fun loadMoreDaysToPast() {
        if (isLoadingMore) return
        
        isLoadingMore = true
        android.util.Log.d("MainFragment", "LOADING MORE DAYS TO PAST")
        
        // Добавляем дни в начало списка
        val expansionSize = 20
        val firstDate = allDays.first().date
        val newDays = mutableListOf<Day>()
        
        for (i in expansionSize downTo 1) {
            val newDate = firstDate.minusDays(i.toLong())
            val dayOfWeek = newDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val notes = dataManager.loadNotes(newDate)
            val day = Day(newDate, dayOfWeek, notes.toList())
            newDays.add(day)
        }
        
        // Добавляем новые дни в начало списка
        allDays.addAll(0, newDays)
        
        // Корректируем центральный индекс
        currentCenterIndex += expansionSize
        
        android.util.Log.d("MainFragment", "PAST LOADED: added $expansionSize days, new center=$currentCenterIndex, total=${allDays.size}")
        
        // Обновляем адаптер
        daysAdapter.submitList(allDays.toList())
        
        // Прокручиваем к правильной позиции, чтобы пользователь не заметил добавления
        binding.daysRecyclerView.post {
            val layoutManager = binding.daysRecyclerView.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(expansionSize, 0)
            isLoadingMore = false
        }
    }
    
    private fun loadMoreDaysToFuture() {
        if (isLoadingMore) return
        
        isLoadingMore = true
        android.util.Log.d("MainFragment", "LOADING MORE DAYS TO FUTURE")
        
        // Добавляем дни в конец списка
        val expansionSize = 20
        val lastDate = allDays.last().date
        
        for (i in 1..expansionSize) {
            val newDate = lastDate.plusDays(i.toLong())
            val dayOfWeek = newDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val notes = dataManager.loadNotes(newDate)
            val day = Day(newDate, dayOfWeek, notes.toList())
            allDays.add(day)
        }
        
        android.util.Log.d("MainFragment", "FUTURE LOADED: added $expansionSize days, center=$currentCenterIndex, total=${allDays.size}")
        
        // Обновляем адаптер
        daysAdapter.submitList(allDays.toList())
        
        binding.daysRecyclerView.post {
            isLoadingMore = false
        }
    }

    private fun generateDays() {
        android.util.Log.d("MainFragment", "Generating initial days list")
        
        // Генерируем большой список дней (100 дней: 50 в прошлое + 50 в будущее)
        allDays.clear()
        val halfRange = initialDaysCount / 2
        
        for (i in -halfRange..halfRange) {
            val dayDate = currentDate.plusDays(i.toLong())
            val dayOfWeek = dayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            
            // Загружаем заметки для этого дня
            val notes = dataManager.loadNotes(dayDate)
            val day = Day(dayDate, dayOfWeek, notes.toList())
            allDays.add(day)
        }
        
        // Центральный день (сегодня) находится в середине списка
        currentCenterIndex = halfRange
        
        android.util.Log.d("MainFragment", "Generated ${allDays.size} days from ${allDays.first().date} to ${allDays.last().date}")
        android.util.Log.d("MainFragment", "Center index: $currentCenterIndex, center date: ${allDays[currentCenterIndex].date}")
        
        // Отправляем весь список в адаптер
        daysAdapter.submitList(allDays)
        daysAdapter.updateCurrentDate(LocalDate.now())
        
        // Центрируем текущий день на экране при запуске с множественными попытками
        centerDayOnScreenWithMultipleAttempts(currentCenterIndex)
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainFragment", "onResume called")
        
        // Проверяем, нужно ли вернуться к текущей неделе
        val today = LocalDate.now()
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(currentDate, today)
        
        // Если мы далеко от текущей недели (больше 3 дней), возвращаемся к ней
        if (kotlin.math.abs(daysDiff) > 3) {
            android.util.Log.d("MainFragment", "Too far from current week, returning to today")
            currentDate = today
            generateDays() // Пересоздаем список с центром на сегодня
        } else {
            // Обновляем данные при возвращении из NotesActivity
            updateDaysData()
        }
        updateInfoBlock()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Очищаем кэш при нехватке памяти
        dataManager.clearCache()
    }
    
    private fun updateDaysData() {
        // Обновляем заметки для всех дней в списке
        android.util.Log.d("MainFragment", "Updating days data, total days: ${allDays.size}")
        
        val updatedDays = allDays.map { day ->
            dataManager.invalidateCache(day.date) // Очищаем кэш
            val notes = dataManager.loadNotes(day.date)
            android.util.Log.d("MainFragment", "Day ${day.date}: loaded ${notes.size} notes")
            Day(day.date, day.dayOfWeek, notes.toList())
        }
        
        allDays.clear()
        allDays.addAll(updatedDays)
        
        android.util.Log.d("MainFragment", "Submitting ${allDays.size} updated days to adapter")
        daysAdapter.submitList(allDays)
        daysAdapter.updateCurrentDate(LocalDate.now())
        
        // Обновляем информационный блок
        updateInfoBlock()
    }
    
    private fun updateInfoBlock() {
        val today = LocalDate.now()
        val todayDay = allDays.find { it.date == today }
        
        binding.infoTitleTextView.text = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH))
        
        if (todayDay != null && todayDay.notes.isNotEmpty()) {
            val uncheckedNotes = todayDay.notes.count { !it.isCompleted }
            val totalNotes = todayDay.notes.size
            
            if (uncheckedNotes == 0) {
                binding.infoMessageTextView.text = getString(R.string.all_tasks_completed)
                binding.infoCard.strokeColor = SUCCESS_COLOR
                // Конфетти перенесено в NotesActivity – здесь больше не запускаем
            } else {
                binding.infoMessageTextView.text = getString(R.string.tasks_remaining, uncheckedNotes, totalNotes)
                binding.infoCard.strokeColor = ERROR_COLOR
            }
        } else {
            binding.infoMessageTextView.text = getString(R.string.no_tasks_today)
            binding.infoCard.strokeColor = ERROR_COLOR
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // Метод для принудительного обновления данных
    fun refreshData() {
        android.util.Log.d("MainFragment", "refreshData called")
        if (::dataManager.isInitialized) {
            // Очищаем кэш для всех дней
            allDays.forEach { day ->
                dataManager.invalidateCache(day.date)
            }
            // Обновляем данные
            updateDaysData()
            android.util.Log.d("MainFragment", "refreshData completed")
        }
    }
    
    companion object {
        private val SUCCESS_COLOR = android.graphics.Color.parseColor("#1976D2") // Темно-синий
        private val ERROR_COLOR = android.graphics.Color.parseColor("#D32F2F") // Красный
    }
} 