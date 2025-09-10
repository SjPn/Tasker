package com.noter.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noter.app.data.JournalEntry
import com.noter.app.databinding.ItemJournalBinding

class JournalAdapter(
    private val onItemClick: (JournalEntry) -> Unit
) : RecyclerView.Adapter<JournalAdapter.VH>() {

    private var items: List<JournalEntry> = emptyList()

    fun submit(newItems: List<JournalEntry>) {
        items = newItems.sortedWith(
            compareByDescending<JournalEntry> { it.updatedAtMillis }
                .thenByDescending { it.createdAtMillis }
                .thenByDescending { it.id }
        )
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemJournalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class VH(private val binding: ItemJournalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: JournalEntry) {
            val lines = entry.content.lines()
            val title = lines.firstOrNull()?.trim().orEmpty()
            val rest = lines.drop(1).joinToString(" ").trim()
            binding.titleText.text = title
            binding.snippetText.text = rest
            binding.root.setOnClickListener { onItemClick(entry) }
        }
    }
}


