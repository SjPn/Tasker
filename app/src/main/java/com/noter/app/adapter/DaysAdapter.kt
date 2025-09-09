package com.noter.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.noter.app.data.Day
import com.noter.app.databinding.ItemDayBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DaysAdapter(
    private val onDayClick: (Day) -> Unit
) : ListAdapter<Day, DaysAdapter.DayViewHolder>(DaysDiffCallback()) {

    private var currentDate = LocalDate.now()

    fun updateCurrentDate(date: LocalDate) {
        currentDate = date
        android.util.Log.d("DaysAdapter", "Current date updated to: $date")
        notifyDataSetChanged() // Только для обновления выделения текущего дня
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        android.util.Log.d("DaysAdapter", "Created new view holder")
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = getItem(position)
        android.util.Log.d("DaysAdapter", "Binding view holder for position $position: ${day.date}")
        holder.bind(day)
    }

    inner class DayViewHolder(
        private val binding: ItemDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDayClick(getItem(position))
                }
            }
        }

        fun bind(day: Day) {
            try {
                // Устанавливаем дату в том же формате, что и на левом экране
                val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
                binding.dayDateTextView.text = day.date.format(formatter)
                
                binding.notesPreviewTextView.text = day.getNotesPreview()
                android.util.Log.d("DaysAdapter", "Binding day: ${day.date}, notes: ${day.notes.size}, preview: ${day.getNotesPreview()}")
                
                // Progress: percent completed + dynamic track color
                val totalNotes = day.notes.size
                val completedNotes = day.notes.count { it.isCompleted }
                val percent = if (totalNotes > 0) (completedNotes * 100) / totalNotes else 0

                // Toggle UI elements depending on whether tasks exist
                if (totalNotes == 0) {
                    // Показываем серый граф без процентов и без надписи
                    binding.progressIndicator.visibility = android.view.View.VISIBLE
                    binding.progressPercentText.visibility = android.view.View.GONE
                    binding.noTasksText.visibility = android.view.View.GONE
                } else {
                    binding.progressIndicator.visibility = android.view.View.VISIBLE
                    binding.progressPercentText.visibility = android.view.View.VISIBLE
                    binding.noTasksText.visibility = android.view.View.GONE
                }

                binding.progressPercentText.text = "$percent%"
                binding.progressIndicator.progress = percent
                // Indicator (completed part) is always blue
                binding.progressIndicator.setIndicatorColor(DARK_BLUE)
                // Track (remaining part) color rules:
                // - No tasks at all: stay gray
                // - Future day with tasks: light red
                // - Past/today with tasks: gray
                val trackColor = when {
                    totalNotes == 0 -> LIGHT_GRAY
                    percent < 100 -> LIGHT_RED
                    else -> LIGHT_GRAY
                }
                try {
                    binding.progressIndicator.trackColor = trackColor
                } catch (_: Throwable) {
                    // Fallback for older material versions
                }

                // Выделяем текущий день темно-синей окантовкой (в 2 раза толще)
                if (day.date == currentDate) {
                    binding.root.strokeWidth = 8
                    binding.root.strokeColor = DARK_BLUE
                    binding.dayDateTextView.setTextColor(DARK_BLUE)
                } else {
                    binding.root.strokeWidth = 0
                    binding.dayDateTextView.setTextColor(binding.root.context.getColor(com.noter.app.R.color.text_secondary))
                }
            } catch (e: Exception) {
                android.util.Log.e("DaysAdapter", "Error binding day: ${day.date}", e)
            }
        }
    }

    companion object {
        private val DARK_BLUE = android.graphics.Color.parseColor("#1976D2")
        // Use the same red as on the left screen (overdue tasks)
        private val LIGHT_RED = android.graphics.Color.parseColor("#F44336")
        private val LIGHT_GRAY = android.graphics.Color.parseColor("#E0E0E0")
    }
}

class DaysDiffCallback : DiffUtil.ItemCallback<Day>() {
    override fun areItemsTheSame(oldItem: Day, newItem: Day): Boolean {
        return oldItem.date == newItem.date
    }

    override fun areContentsTheSame(oldItem: Day, newItem: Day): Boolean {
        return oldItem == newItem
    }
} 