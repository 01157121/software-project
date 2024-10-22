package com.example.project_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class ScheduleFragment : Fragment() {

    companion object {
        private const val ARG_DATE = "date"

        fun newInstance(date: Date): ScheduleFragment {
            val fragment = ScheduleFragment()
            val args = Bundle()
            args.putSerializable(ARG_DATE, date)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 獲取日期參數
        val date = arguments?.getSerializable(ARG_DATE) as? Date // 使用安全的轉換
        if (date == null) {
            // 如果日期是 null，可以顯示錯誤或使用當前日期
            val currentDate = Date() // 使用當前日期作為後備
            displayDate(currentDate, view)
        } else {
            displayDate(date, view)
        }

        // 生成24小時排程
        val tableLayout = view.findViewById<TableLayout>(R.id.schedule_table)
        generateScheduleTable(tableLayout)
    }

    private fun displayDate(date: Date, view: View) {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        // 設定日期標題
        val dateTextView = view.findViewById<TextView>(R.id.date_text)
        dateTextView.text = dateFormat.format(date)
    }

    // 生成24小時行程表
    private fun generateScheduleTable(tableLayout: TableLayout) {
        for (i in 0..23) {
            val tableRow = TableRow(context)
            val timeText = TextView(context).apply {
                text = String.format("%02d:00", i) // 格式化時間顯示
            }
            tableRow.addView(timeText)

            val eventText = EditText(context).apply {
                hint = "輸入行程"
            }
            tableRow.addView(eventText)

            tableLayout.addView(tableRow)
        }
    }
}
