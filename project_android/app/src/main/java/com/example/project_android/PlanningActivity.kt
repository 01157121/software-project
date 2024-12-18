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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    private val db = FirebaseFirestore.getInstance()
    private lateinit var scheduleId: String
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
        members = intent.getStringArrayListExtra("MEMBERS_LIST")?: arrayListOf()
        scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: throw IllegalArgumentException("SCHEDULE_ID is required")



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
        intent.putExtra("SCHEDULE_ID", scheduleId)
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
            val newResults = mutableListOf<Map<String, Any>>()
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
                    newResults.add(
                        convertToMap( Triple(checkBox.text.toString(), whoPaid, amount) )
                    )
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
            val accountingResultId = generateUniqueAccountingResultId()
            saveAccountingResultToFirestore(scheduleId,accountingResultId,newResults)
            //accountingResults.addAll(newResults)

            Toast.makeText(this, "分帳結果已保存", Toast.LENGTH_SHORT).show()

            dialog.dismiss()

        }

        dialog.show()

    }
    //把資料修改成firebase可儲存的格式
    private fun convertToMap(triple: Triple<String, String, Double>):Map<String, Any> {
        return mapOf(
            "debtor" to triple.first,   // 欠款者
            "creditor" to triple.second, // 付款者
            "amount" to triple.third    // 金額
        )
    }

    private fun showAccountingResult() {
        db.collection("schedules")
            .document(scheduleId)
            .collection("accounting_results")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    showToast("沒有找到任何分帳紀錄")
                    return@addOnSuccessListener
                }

                val allResults = mutableListOf<Map<String, Any>>()

                for (document in querySnapshot.documents) {
                    val results = document.get("results") as? List<Map<String, Any>>
                    if (results != null) {
                        allResults.addAll(results)
                    }
                }

                // 顯示對話框
                showAccountingResultDialog(allResults)
            }
            .addOnFailureListener { e ->
                showToast("無法讀取分帳紀錄: ${e.message}")
            }
    }
    //用簡單dialog顯示分帳紀錄
    private fun showAccountingResultDialog(results: List<Map<String, Any>>) {
        // 加載自定義對話框佈局
        val dialogView = layoutInflater.inflate(R.layout.dialog_accounting_results, null)
        val resultsContainer = dialogView.findViewById<LinearLayout>(R.id.accounting_results_container)
        val closeButton = dialogView.findViewById<Button>(R.id.close_button)

        // 動態生成每個分帳結果
        results.forEach { result ->
            val debtor = result["debtor"] as? String ?: "未知"
            val creditor = result["creditor"] as? String ?: "未知"
            val amount = result["amount"] as? Double ?: 0.0

            val resultView = TextView(this).apply {
                text = "欠款者: $debtor\n付款者: $creditor\n金額: $amount\n"
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }
            resultsContainer.addView(resultView)
        }

        // 創建並顯示對話框
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // 設置關閉按鈕邏輯
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveAccountingResultToFirestore(scheduleId: String, AccountingResultId: String, results: List<Map<String, Any>>) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            showToast("用戶未登入")
            return
        }

        val accountingData = hashMapOf(
            "results" to results
        )
        db.collection("schedules")
            .document(scheduleId)
            .collection("accounting_results")
            .document(AccountingResultId)
            .set(accountingData)
            .addOnSuccessListener {
                showToast("分帳紀錄添加成功！")
            }
            .addOnFailureListener { e ->
                showToast("分帳紀錄添加到用戶資料失敗: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun generateUniqueAccountingResultId(): String {
        return java.util.UUID.randomUUID().toString()
    }
}
