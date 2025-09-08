package com.noter.app.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.noter.app.MainFragment
import com.noter.app.FutureNotesFragment
import com.noter.app.OverdueNotesFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragments = mutableMapOf<Int, Fragment>()

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> OverdueNotesFragment()
            1 -> MainFragment()
            2 -> FutureNotesFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
        fragments[position] = fragment
        return fragment
    }

    fun getFragment(position: Int): Fragment? {
        return fragments[position]
    }
} 