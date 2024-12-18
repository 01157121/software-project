package com.example.project_android

import android.app.DatePickerDialog
import android.app.FragmentManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.app.TimePickerDialog
import android.os.PersistableBundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.window.OnBackInvokedDispatcher
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class PlanningActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var scheduleNameTextView: TextView
    private lateinit var dateRangeTextView: TextView
    private val dateList: MutableList<Date> = mutableListOf()
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var addButton: FloatingActionButton
    private lateinit var AddAccountingBtn: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planning)

        // 從 Intent 中接收行程名稱和日期範圍和成員名單
        val scheduleName = intent.getStringExtra("SCHEDULE_NAME")
        val startDateStr = intent.getStringExtra("START_DATE")
        val endDateStr = intent.getStringExtra("END_DATE")
        val Members = intent.getStringArrayListExtra("MEMBERS_LIST")
        // 初始化 UI 元素
        scheduleNameTextView = findViewById(R.id.schedule_name_text)
        dateRangeTextView = findViewById(R.id.date_range_text)
//        viewPager = findViewById(R.id.view_pager)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) // 日期格式化工具
        try {
            // 將日期字串解析成 Date 物件
            val startDate = dateFormat.parse(startDateStr)
            val endDate = dateFormat.parse(endDateStr)

            // 根據開始與結束日期生成日期範圍
            generateDateRange(startDate, endDate)

            // 更新行程名稱與日期範圍顯示
            scheduleNameTextView.text = scheduleName
            dateRangeTextView.text = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"

//            // 設定 ViewPager2 的適配器
//            val adapter = SchedulePagerAdapter(this, dateList)
//            viewPager.adapter = adapter

        } catch (e: ParseException) {
            e.printStackTrace()
            // 處理日期解析失敗的情況
        }


        val scheduleListContainer = findViewById<LinearLayout>(R.id.schedule_list_container)
        // 設定浮動按鈕的點擊事件，顯示新增行程的對話框
        addButton = findViewById(R.id.add_plan_button)
        addButton.setOnClickListener {
            showCreatePlan(scheduleListContainer)
        }
<<<<<<< Updated upstream
        // 新增分帳按鈕功能
        val addAccountingContainer =
        AddAccountingBtn = findViewById(R.id.add_accounting_button)
        AddAccountingBtn.setOnClickListener {
            addAccounting(scheduleListContainer)
=======
        // 設定選單圖示的點擊事件
        val menuIcon: ImageView = findViewById(R.id.menu_icon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(Gravity.START)
        }

        // 設置 NavigationView 的選項點擊事件
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_user_list -> showUserList()
                R.id.nav_save -> saveSchedule()
                R.id.nav_export -> exportSchedule()
                R.id.nav_home -> goToHomePage()
            }
            drawerLayout.closeDrawer(Gravity.START)
            true
>>>>>>> Stashed changes
        }
    }



    // 生成日期範圍的函數
    private fun generateDateRange(startDate: Date?, endDate: Date?) {
        if (startDate == null || endDate == null) return

        val calendar = Calendar.getInstance()
        calendar.time = startDate

        // 循環生成從開始日期到結束日期的每一天
        while (calendar.time.before(endDate) || calendar.time.equals(endDate)) {
            dateList.add(calendar.time) // 將日期加入列表
            calendar.add(Calendar.DAY_OF_YEAR, 1) // 日期加一天
        }
    }

    var startHour: Int = 0
    var startMinute: Int = 0
    var endHour: Int = 0
    var endMinute: Int = 0

    // 顯示新增行程的對話框
    private fun showCreatePlan(container: LinearLayout) {
        // 載入對話框的自訂佈局
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_plan, null)
        val dateButton: View = dialogView.findViewById(R.id.date_button)
        val startTimeButton: View = dialogView.findViewById(R.id.start_time_button)
//        val startTimeButtonText = findViewById<TextView>(R.id.start_time_button_text) // 顯示選擇的時間
        val endTimeButton: View = dialogView.findViewById(R.id.end_time_button)
//        val endTimeButtonText = findViewById<TextView>(R.id.end_time_button_text)


        // 設定開始日期按鈕的點擊事件
        dateButton.setOnClickListener {
            showDatePickerDialog { selectedDate ->
                // 將選中的日期顯示在按鈕上
                dateButton.findViewById<View>(R.id.date_button_text).apply {
                    (this as? TextView)?.text = selectedDate
                }
            }
        }



        startTimeButton.setOnClickListener {
            showTimePickerDialog { hourOfDay, minute ->
                // 將選擇的時間顯示在 TextView 上
                startTimeButton.findViewById<View>(R.id.start_time_button_text).apply {
                    (this as? TextView)?.text = String.format("%02d:%02d", hourOfDay, minute)
                    startHour = hourOfDay
                    startMinute = minute
                }

            }
        }

        endTimeButton.setOnClickListener {
            showTimePickerDialog { hourOfDay, minute ->
                // 將選擇的時間顯示在 TextView 上
                endTimeButton.findViewById<View>(R.id.end_time_button_text).apply {
                    (this as? TextView)?.text = String.format("%02d:%02d", hourOfDay, minute)
                    endHour = hourOfDay
                    endMinute = minute
                }

            }
        }

        // 建立並顯示對話框
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("新增行程")
            .setView(dialogView) // 將自訂佈局設為對話框內容
            .setPositiveButton("新增") { _, _ ->
                // 從對話框中獲取使用者輸入的行程名稱和日期範圍
                val planName = dialogView.findViewById<EditText>(R.id.plan_name).text.toString()
                val selectedDate = dialogView.findViewById<TextView>(R.id.date_button_text).text.toString()
//                val startTime = dialogView.findViewById<TextView>(R.id.start_time_button_text).text
//                val endTime = dialogView.findViewById<TextView>(R.id.end_time_button_text).text

                addScheduleToContainer(container, planName, selectedDate)


            }
            .setNegativeButton("取消", null) // 設置取消按鈕
            .create()
        dialog.show()

        // 為開始和結束日期按鈕設置重複點擊事件 (修正對話框的行為)
        dialogView.findViewById<Button>(R.id.date_button).setOnClickListener {
            showDatePickerDialog { selectedDate ->
                dialogView.findViewById<TextView>(R.id.date_button_text).text = selectedDate
            }
        }
        dialogView.findViewById<Button>(R.id.start_time_button).setOnClickListener {
            showTimePickerDialog { hourOfDay, minute ->
                dialogView.findViewById<TextView>(R.id.start_time_button_text).text = String.format("%02d:%02d", hourOfDay, minute)
                startHour = hourOfDay
                startMinute = minute
            }
        }
        dialogView.findViewById<Button>(R.id.end_time_button).setOnClickListener {
            showTimePickerDialog { hourOfDay, minute ->
                dialogView.findViewById<TextView>(R.id.end_time_button_text).text = String.format("%02d:%02d", hourOfDay, minute)
                endHour = hourOfDay
                endMinute = minute
            }
        }
    }
    // 顯示日期選擇器的函數
    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 初始化日期選擇對話框
        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // 格式化選中的日期
            val selectedDate = "$selectedYear/${selectedMonth + 1}/$selectedDay"
            onDateSelected(selectedDate) // 回傳選中的日期
        }, year, month, day)

        datePickerDialog.show() // 顯示對話框
    }

    // 顯示時間選擇對話框
    private fun showTimePickerDialog(onTimeSelected: (Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // 初始化 TimePickerDialog
        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            onTimeSelected(hourOfDay, minute) // 回傳選擇的時間
        }, currentHour, currentMinute, true) // `true` 表示使用 24 小時制，`false` 則是 12 小時制

        timePickerDialog.show() // 顯示對話框
    }


    private fun addScheduleToContainer(
        container: LinearLayout,
        planName: String,
        date: String
    ) {
        val scheduleView = layoutInflater.inflate(R.layout.activity_planning_part_of_item, container, false)

        // 設定行程資訊
        scheduleView.findViewById<TextView>(R.id.plan_name_text).text = planName
        scheduleView.findViewById<TextView>(R.id.date_text).text = date
        scheduleView.findViewById<TextView>(R.id.time_range_text).text =
            String.format("%02d:%02d-%02d:%02d", startHour, startMinute, endHour, endMinute)

        // 新增行程到容器
        container.addView(scheduleView)
    }

    //drawer內容
    private fun showUserList() {
        // 展示用戶清單的邏輯
        // 例如：顯示一個對話框或跳轉到新活動
    }

    private fun saveSchedule() {
        // 存檔的邏輯
        // 例如：將行程儲存到 Firestore
    }

    private fun exportSchedule() {
        // 匯出的邏輯
        // 例如：將行程導出為文件格式
    }

    private fun goToHomePage() {
        val intent = Intent(this, LobbyActivity::class.java)
        startActivity(intent)
        finish()
    }
}
