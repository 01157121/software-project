package com.example.project_android

import android.os.Bundle
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PlanningActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var scheduleNameTextView: TextView
    private lateinit var dateRangeTextView: TextView
    private val dateList: MutableList<Date> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planning)

        val scheduleName = intent.getStringExtra("SCHEDULE_NAME")
        val startDateStr = intent.getStringExtra("START_DATE")
        val endDateStr = intent.getStringExtra("END_DATE")
        val Members = intent.getStringArrayListExtra("MEMBERS_LIST")
        val validMembers: ArrayList<String> = ArrayList(Members!!.filterNotNull())
        // 設定 UI 元素
        scheduleNameTextView = findViewById(R.id.schedule_name_text)
        dateRangeTextView = findViewById(R.id.date_range_text)
        viewPager = findViewById(R.id.view_pager)

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        try {
            val startDate = dateFormat.parse(startDateStr)
            val endDate = dateFormat.parse(endDateStr)

            // 生成日期範圍
            generateDateRange(startDate, endDate)

            // 更新 UI
            scheduleNameTextView.text = scheduleName
            dateRangeTextView.text = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"

            // 設定 ViewPager2
            val adapter = SchedulePagerAdapter(this, dateList, validMembers)
            viewPager.adapter = adapter

        } catch (e: ParseException) {
            e.printStackTrace()
            // 處理解析失敗的情況
        }
    }

    // 生成時間表
    private fun generateDateRange(startDate: Date?, endDate: Date?) {
        if (startDate == null || endDate == null) return

        val calendar = Calendar.getInstance()
        calendar.time = startDate

        while (calendar.time.before(endDate) || calendar.time.equals(endDate)) {
            dateList.add(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1) // 增加一天
        }
    }
}