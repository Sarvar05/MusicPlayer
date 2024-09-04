package com.example.m.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.m.fragments.FavouriteFragment
import com.example.m.fragments.HomeFragment
import com.example.m.fragments.Recent2Fragment
import com.example.m.fragments.RecentFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4 // Number of fragments

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> FavouriteFragment()
            2 -> Recent2Fragment()
            3 -> RecentFragment()


            else -> HomeFragment() // Fallback fragment
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> "Треки"
            1 -> "Избранные"
            2 -> "Недавние"
            3 -> "Плэйлисты"
            else -> "Треки"
        }
    }
}
