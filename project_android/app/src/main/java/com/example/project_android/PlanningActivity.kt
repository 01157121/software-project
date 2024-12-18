package com.example.project_android

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PlanningActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var scheduleNameTextView: TextView
    private lateinit var dateRangeTextView: TextView
    private val dateList: MutableList<Date> = mutableListOf()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var addButton: FloatingActionButton
    private lateinit var addAccountingButton: Button
    private lateinit var showAccountingResultButton: Button
    private val accountingResults = mutableListOf<Triple<String, String, Double>>() // 用於保存分帳結果
    private lateinit var members: ArrayList<String>
    var startHour: Int = 0
    var startMinute: Int = 0
    var endHour: Int = 0
    var endMinute: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planning)

        // 接收 Intent 数据
        val scheduleName = intent.getStringExtra("SCHEDULE_NAME")
        val startDateStr = intent.getStringExtra("START_DATE")
        val endDateStr = intent.getStringExtra("END_DATE")
        val members = intent.getStringArrayListExtra("MEMBERS_LIST")?: arrayListOf()
        val scheduleId = intent.getStringExtra("SCHEDULE_ID")

        // 初始化 UI 元素
        scheduleNameTextView = findViewById(R.id.schedule_name_text)
        dateRangeTextView = findViewById(R.id.date_range_text)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        addButton = findViewById(R.id.add_plan_button)

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        try {
            val startDate = dateFormat.parse(startDateStr)
            val endDate = dateFormat.parse(endDateStr)
            generateDateRange(startDate, endDate)

            scheduleNameTextView.text = scheduleName
            dateRangeTextView.text = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        val scheduleListContainer = findViewById<LinearLayout>(R.id.schedule_list_container)

        // 新增计划
        addButton.setOnClickListener {
            showCreatePlan(scheduleListContainer)
        }

        // 設定選單圖示的點擊事件
        val menuIcon: ImageView = findViewById(R.id.menu_icon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(Gravity.START)
        }

        // 导航菜单点击事件
        setupNavigationMenu()
    }
    // 生成日期範圍的函數
    private fun generateDateRange(startDate: Date?, endDate: Date?) {
        if (startDate == null || endDate == null) return
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        while (calendar.time.before(endDate) || calendar.time == endDate) {
            dateList.add(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun showCreatePlan(container: LinearLayout) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_plan, null)

        val dateButton: Button = dialogView.findViewById(R.id.date_button)
        val startTimeButton: Button = dialogView.findViewById(R.id.start_time_button)
        val endTimeButton: Button = dialogView.findViewById(R.id.end_time_button)

        dateButton.setOnClickListener {
            showDatePickerDialog { selectedDate ->
                dialogView.findViewById<TextView>(R.id.date_button_text).text = selectedDate
            }
        }

        startTimeButton.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                startHour = hour
                startMinute = minute
                dialogView.findViewById<TextView>(R.id.start_time_button_text).text =
                    String.format("%02d:%02d", hour, minute)
            }
        }

        endTimeButton.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                endHour = hour
                endMinute = minute
                dialogView.findViewById<TextView>(R.id.end_time_button_text).text =
                    String.format("%02d:%02d", hour, minute)
            }
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("新增行程")
            .setView(dialogView)
            .setPositiveButton("新增") { _, _ ->
                val planName = dialogView.findViewById<EditText>(R.id.plan_name).text.toString()
                val selectedDate = dialogView.findViewById<TextView>(R.id.date_button_text).text.toString()
                addScheduleToContainer(container, planName, selectedDate)
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }

    private fun addScheduleToContainer(container: LinearLayout, planName: String, date: String) {
        val scheduleView = layoutInflater.inflate(R.layout.activity_planning_part_of_item, container, false)

        scheduleView.findViewById<TextView>(R.id.plan_name_text).text = planName
        scheduleView.findViewById<TextView>(R.id.date_text).text = date
        scheduleView.findViewById<TextView>(R.id.time_range_text).text =
            String.format("%02d:%02d-%02d:%02d", startHour, startMinute, endHour, endMinute)

        container.addView(scheduleView)
    }

    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            onDateSelected("$year/${month + 1}/$day")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePickerDialog(onTimeSelected: (Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            onTimeSelected(hour, minute)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun setupNavigationMenu() {
        findViewById<ImageView>(R.id.menu_icon).setOnClickListener {
            drawerLayout.openDrawer(Gravity.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_accounting -> addAccounting()
                R.id.show_accounting_result -> showAccountingResult()
                R.id.nav_user_list -> showUserList()
                R.id.nav_save -> saveSchedule()
                R.id.nav_export -> exportSchedule()
                R.id.nav_home -> goToHomePage()
            }
            drawerLayout.closeDrawer(Gravity.START)
            true
        }
    }

    private fun showUserList() {
        val intent = Intent(this, UserListActivity::class.java)
        intent.putExtra("SCHEDULE_ID", intent.getStringExtra("SCHEDULE_ID"))
        startActivity(intent)
    }

    private fun saveSchedule() {
        // 保存行程
    }

    private fun exportSchedule() {
        // 导出行程
    }

    private fun goToHomePage() {
        val intent = Intent(this, LobbyActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun addAccounting() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_split_bill, null)

        val dialog = AlertDialog.Builder(this)
            .setTitle("分帳")
            .setView(dialogView)
            .create()

        // 設置 Adapter 給付款者的 ListView
        val filteredMembers = members?.filter { it.isNotBlank() } ?: arrayListOf()
        val singleChoiceAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_single_choice,
            filteredMembers
        )
        val whoPaidList = dialogView.findViewById<ListView>(R.id.who_paid_list)
        whoPaidList.adapter = singleChoiceAdapter
        whoPaidList.choiceMode = ListView.CHOICE_MODE_SINGLE

        // 動態生成欠款者的選項
        val whoOwesContainer = dialogView.findViewById<LinearLayout>(R.id.who_owes_container)
        filteredMembers.forEach { member ->
            val itemView = layoutInflater.inflate(R.layout.item_who_owes, null)

            val checkBox = itemView.findViewById<CheckBox>(R.id.checkbox_who_owes)
            itemView.findViewById<EditText>(R.id.amount_input)

            checkBox.text = member
            whoOwesContainer.addView(itemView)
        }

        // 按鈕邏輯
        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.submit_button).setOnClickListener {
            val selectedWhoPaidPosition = whoPaidList.checkedItemPosition
            if (selectedWhoPaidPosition == -1) {
                Toast.makeText(this, "請選擇付錢者", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val whoPaid = filteredMembers[selectedWhoPaidPosition]
            val newResults = mutableListOf<Triple<String, String, Double>>() // 暫存新結果
            var totalOwed = 0.0 // 總欠款金額
            for (i in 0 until whoOwesContainer.childCount) {
                val itemView = whoOwesContainer.getChildAt(i)
                val checkBox = itemView.findViewById<CheckBox>(R.id.checkbox_who_owes)
                val amountInput = itemView.findViewById<EditText>(R.id.amount_input)

                if (checkBox.isChecked) {
                    val amount = amountInput.text.toString().toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        Toast.makeText(this, "請輸入有效金額", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    totalOwed += amount // 累計欠款金額
                    if (checkBox.text.toString() != whoPaid) {
                        // 只有欠款者與付款者不同時才保存到結果中
                        newResults.add(Triple(checkBox.text.toString(), whoPaid, amount))
                    }
                }
            }

            if (totalOwed == 0.0) {
                Toast.makeText(this, "請選擇至少一名欠款者並輸入金額", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amountPaidInput = dialogView.findViewById<EditText>(R.id.amount_paid_input)
            val amountPaid = amountPaidInput.text.toString().toDoubleOrNull()
            if (amountPaid == null || totalOwed != amountPaid) {
                Toast.makeText(this, "總欠款金額必須等於付款金額", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 更新分帳結果
            accountingResults.clear()
            accountingResults.addAll(newResults)

            Toast.makeText(this, "分帳結果已保存", Toast.LENGTH_SHORT).show()

            dialog.dismiss()

        }

        dialog.show()

    }

    private fun showAccountingResult() {
        if (accountingResults.isEmpty()) {
            Toast.makeText(this, "目前沒有分帳結果", Toast.LENGTH_SHORT).show()
        } else {
            val resultText = accountingResults.joinToString("\n") { (debtor, creditor, amount) ->
                "$debtor 欠 $creditor $amount"
            }
            Toast.makeText(this, resultText, Toast.LENGTH_LONG).show()
        }
    }
}
