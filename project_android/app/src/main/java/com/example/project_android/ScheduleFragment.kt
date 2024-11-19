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

//import com.example.project_android.PlanningActivity.scheduleName

class ScheduleFragment : Fragment() {

    companion object {
        private const val ARG_DATE = "date"

        const val TAG = "ScheduleFragment"

        fun newInstance(date: Date): ScheduleFragment {
            val fragment = ScheduleFragment()
            val args = Bundle()
            args.putSerializable(ARG_DATE, date)
            fragment.arguments = args
            return fragment
        }

//        private const val ARG_PLAN_NAME = "plan_name"
//        private const val ARG_SELECTED_DATE = "selected_date"
//        private const val ARG_START_HOUR = "start_hour"
//        private const val ARG_END_HOUR = "end_hour"
//
//        fun newInstance_create_plan(planName: String, selectedDate: String, startHour: Int, endHour: Int): ScheduleFragment {
//            val fragment = ScheduleFragment()
//            val args = Bundle().apply {
//                putString(ARG_PLAN_NAME, planName)
//                putString(ARG_SELECTED_DATE, selectedDate)
//                putInt(ARG_START_HOUR, startHour)
//                putInt(ARG_END_HOUR, endHour)
//            }
//            fragment.arguments = args
//            return fragment
//        }
//
////        fun newInstance_create_plan(testData: TestData) =
////            ScheduleFragment().apply {
////                arguments = Bundle().apply {
////                    putString("name", testData.name)
////                    putString("content", testData.content)
////                }
////            }
////        fun newInstance(planName: String, selectedDate: String, startTime: String, endTime: String): ScheduleFragment {
////            val fragment = ScheduleFragment()
////            val args = Bundle().apply {
////                putString("PLAN_NAME", planName)
////                putString("SELECTED_DATE", selectedDate)
////                putString("START_TIME", startTime)
////                putString("END_TIME", endTime)
////            }
////            fragment.arguments = args
////            return fragment
////        }
    }
//
////    private lateinit var testData: TestData
////
//    private lateinit var rootView: View
////    private lateinit var test_text: TextView
////
////    override fun onCreate(savedInstanceState: Bundle?) {
////        super.onCreate(savedInstanceState)
////
////        testData = TestData()
////        arguments?.let {
////            testData.name = it.getString("name", "")
////            testData.content = it.getString("content", "")
////        }
////    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        rootView = inflater.inflate(R.layout.fragment_schedule, container, false)
//
////        test_text = rootView.findViewById(R.id.test_text)
//
//        return rootView
//    }
//
//
////    val planName = arguments?.getString("PLAN_NAME")
////    val selectedDate = arguments?.getString("SELECTED_DATE")
////    val startTime = arguments?.getString("START_TIME")
////    val endTime = arguments?.getString("END_TIME")
//
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // 生成24小時排程
//        val tableLayout = view.findViewById<TableLayout>(R.id.schedule_table)
////        generateScheduleTable(tableLayout, 0, 23, "行程名稱")
//
////        val message = "${testData.name}, ${testData.content}"
////        test_text.text = message
////        // 從 Intent 中接收行程名稱和日期範圍
////        val planName = intent.getStringExtra("PLAN_NAME")
////        val selectedDate = intent.getStringExtra("SELECTED_DATE")
////        val startTime = intent.getStringExtra("START_TIME")
////        val endTime = intent.getStringExtra("END_TIME")
//
//        val planName = arguments?.getString(ARG_PLAN_NAME)
//        val selectedDate = arguments?.getString(ARG_SELECTED_DATE)
//        val startHour = arguments?.getInt(ARG_START_HOUR)
//        val endHour = arguments?.getInt(ARG_END_HOUR)
//
//        if (startHour == null || endHour == null || planName == null){
//            generateScheduleTable(tableLayout, 0, 23, "行程名稱")
//        }else{
//            val dialogView = layoutInflater.inflate(R.layout.fragment_schedule, null)
//            val idTestText = dialogView.findViewById<TextView>(R.id.test_text)
//            idTestText.text = planName
////            generateScheduleTable(tableLayout, startHour, endHour, planName)
//        }
//
//
////        // 這裡你可以根據取得的資料來顯示或做其他處理
////        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
////
////        // 顯示計劃名稱
////        val planTextView = view.findViewById<TextView>(R.id.plan_name_text)
////        planTextView.text = planName
////
////        // 顯示選擇的日期
////        val dateTextView = view.findViewById<TextView>(R.id.date_text)
////        dateTextView.text = selectedDate
////
////        // 顯示起始時間與結束時間（如果需要）
////        val timeTextView = view.findViewById<TextView>(R.id.time_range_text)
////        timeTextView.text = "$startTime - $endTime"
//
//        // 獲取日期參數
//        val date = arguments?.getSerializable(ARG_DATE) as? Date // 使用安全的轉換
//        if (date == null) {
//            // 如果日期是 null，可以顯示錯誤或使用當前日期
//            val currentDate = Date() // 使用當前日期作為後備
//            displayDate(currentDate, view)
//        } else {
//            displayDate(date, view)
//        }
//
//
//    }
//
//    private fun displayDate(date: Date, view: View) {
//        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
//
////        val planName = arguments?.get("String")
////        println(planName)
//
//        // 設定日期標題
//        val dateTextView = view.findViewById<TextView>(R.id.date_text)
//        dateTextView.text = dateFormat.format(date)
//    }
//
//    // 生成24小時行程表
//    private fun generateScheduleTable(tableLayout: TableLayout, startTime: Int, dayEndTime: Int, planName: String) {
//
//
//        for (i in startTime..dayEndTime) {
//            val tableRow = TableRow(context)
//            val timeText = TextView(context).apply {
//                text = String.format("%02d:00", i) // 格式化時間顯示
//            }
//            tableRow.addView(timeText)
//
//            val eventText = EditText(context).apply {
//                hint = planName
//            }
//            tableRow.addView(eventText)
//
//            tableLayout.addView(tableRow)
//        }
//    }
}
