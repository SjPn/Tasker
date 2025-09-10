package com.noter.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.noter.app.data.DataManager
import com.noter.app.data.JournalEntry
import com.noter.app.databinding.FragmentFutureNotesBinding
import com.noter.app.databinding.FragmentOverdueNotesBinding
import com.noter.app.databinding.FragmentMainBinding
import com.noter.app.databinding.FragmentJournalBinding

class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: com.noter.app.adapter.JournalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager(requireContext())
        adapter = com.noter.app.adapter.JournalAdapter(
            onItemClick = { entry ->
                val intent = Intent(requireContext(), JournalEditActivity::class.java)
                intent.putExtra("entry_id", entry.id)
                startActivity(intent)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
        // Улучшаем плавность скролла
        binding.recyclerView.itemAnimator = null

        binding.addButton.setOnClickListener {
            val intent = Intent(requireContext(), JournalEditActivity::class.java)
            startActivity(intent)
        }

        refreshData()
        try { com.noter.app.util.ViewUtils.disableSoundEffectsDeep(binding.root) } catch (_: Exception) {}

        // Подстраховка: обновлять при показе страницы ViewPager2
        try {
            (requireActivity() as? MainPagerActivity)?.let { act ->
                act.findViewById<androidx.viewpager2.widget.ViewPager2>(com.noter.app.R.id.viewPager)?.registerOnPageChangeCallback(
                    object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            if (position == 3) refreshData()
                        }
                    }
                )
            }
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    fun refreshData() {
        val items = dataManager.loadJournal()
        adapter.submit(items)
        binding.recyclerView.post { binding.recyclerView.invalidateItemDecorations() }
    }
}


