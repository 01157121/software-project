package com.example.project_android

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.util.*

class SchedulePagerAdapter(fragmentActivity: FragmentActivity, private val dateList: List<Date>) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return dateList.size
    }

    override fun createFragment(position: Int): Fragment {
        val date = dateList[position]
        return ScheduleFragment.newInstance(date)
    }
}
