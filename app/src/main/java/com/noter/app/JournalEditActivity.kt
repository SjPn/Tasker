package com.noter.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.noter.app.data.DataManager
import com.noter.app.data.JournalEntry
import com.noter.app.databinding.ActivityJournalEditBinding

class JournalEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJournalEditBinding
    private lateinit var dataManager: DataManager
    private var entry: JournalEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = DataManager(this)

        val id = intent.getStringExtra("entry_id")
        if (id != null) {
            entry = dataManager.loadJournal().find { it.id == id }
        }
        if (entry == null) entry = JournalEntry()

        binding.contentEditText.setText(entry!!.content)
    }

    override fun onBackPressed() {
        saveAndFinish()
    }

    private fun saveAndFinish() {
        val contentRaw = binding.contentEditText.text.toString()
        val content = contentRaw
        val list = dataManager.loadJournal().toMutableList()
        val e = entry!!

        // Если запись пустая (после trim) — удаляем её
        if (contentRaw.trim().isEmpty()) {
            val idxDel = list.indexOfFirst { it.id == e.id }
            if (idxDel >= 0) list.removeAt(idxDel)
            dataManager.saveJournal(list)
            dataManager.invalidateJournalCache()
            finish()
            return
        }

        e.content = content
        e.updatedAt = java.time.LocalDateTime.now()
        e.updatedAtMillis = System.currentTimeMillis()
        val idx = list.indexOfFirst { it.id == e.id }
        if (idx >= 0) list[idx] = e else list.add(0, e)
        dataManager.saveJournal(list)
        dataManager.invalidateJournalCache()
        // Мгновенно сообщаем списку о необходимости обновиться при возвращении
        finish()
    }
}


