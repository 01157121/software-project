package com.example.project_android

import DatePageAdapter
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PlanningActivity : AppCompatActivity() {
    private lateinit var scheduleNameTextView: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val accountingResults = mutableListOf<Triple<String, String, Double>>() // 用於保存分帳結果
    private lateinit var members: ArrayList<String>
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var scheduleId: String
    private lateinit var feedbackButton: FloatingActionButton // 回饋按鈕

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planning)

        // 接收 Intent 数据
        val scheduleName = intent.getStringExtra("SCHEDULE_NAME")
        val startDateStr = intent.getStringExtra("START_DATE")
        val endDateStr = intent.getStringExtra("END_DATE")
        scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: throw IllegalArgumentException("SCHEDULE_ID is required")



        // 初始化 UI 元素
        scheduleNameTextView = findViewById(R.id.schedule_name_text)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        scheduleNameTextView.text = scheduleName

        // 設定選單圖示的點擊事件
        val menuIcon: ImageView = findViewById(R.id.menu_icon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(Gravity.START)
        }

        // 导航菜单点击事件
        setupNavigationMenu()

        val viewPager: ViewPager2= findViewById(R.id.view_pager)

        // 定義日期範圍
        val activityDateFormat  = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val startDate = activityDateFormat.parse(startDateStr)!! // 開始日期
        val endDate = activityDateFormat.parse(endDateStr)!!   // 結束日期

        // 設定 Adapter
        val adapter = DatePageAdapter(this, startDate, endDate,scheduleId)
        viewPager.adapter = adapter

        // 設定初始頁為今天
        val today = Date()
        val todayIndex = ((today.time - startDate.time) / (1000 * 60 * 60 * 24)).toInt()
        if (todayIndex in 0 until adapter.itemCount) {
            viewPager.setCurrentItem(todayIndex, false)
        }
        // 回饋功能
        feedbackButton = findViewById(R.id.feedback_button)
        feedbackButton.visibility = View.GONE // 預設隱藏

        checkIfFeedbackNeeded()
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

    private fun checkIfFeedbackNeeded() {
        val currentDate = Calendar.getInstance().time
        val userId = auth.currentUser?.uid ?: return

        db.collection("schedules")
            .whereArrayContains("collaborators", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach { document ->
                    val endDateStr = document.getString("endDate") ?: return@forEach

                    // 添加日期解析的異常處理
                    val endDate = try {
                        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).parse(endDateStr)
                    } catch (e: ParseException) {
                        e.printStackTrace()
                        null // 如果解析失敗，返回 null
                    }

                    if (endDate != null && currentDate.after(endDate)) {
                        feedbackButton.visibility = View.VISIBLE
                        feedbackButton.setOnClickListener {
                            val scheduleId = document.id
                            val intent = Intent(this, FeedbackActivity::class.java)
                            intent.putExtra("SCHEDULE_ID", scheduleId)
                            startActivity(intent)
                        }
                    }
                }
            }
            .addOnFailureListener {
                showToast("無法檢查回饋狀態")
            }
    }

    private fun addAccounting() {
        members = ArrayList()
        val dialogView = layoutInflater.inflate(R.layout.dialog_split_bill, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("分帳")
            .setView(dialogView)
            .create()

        db.collection("schedules")
            .document(scheduleId)
            .get()
            .addOnSuccessListener { scheduleDocument ->
                if (scheduleDocument.exists()) {
                    // 獲取 collaborators 的 ID 清單
                    val collaborators = scheduleDocument.get("collaborators") as? List<String> ?: emptyList()

                    // 查找每個 collaborator 的 username
                    val tasks = collaborators.map { userId ->
                        db.collection("users").document(userId).get()
                    }

                    Tasks.whenAllSuccess<DocumentSnapshot>(tasks).addOnSuccessListener { documents ->
                        documents.forEach { userDocument ->
                            val username = userDocument.getString("username") ?: "未知用戶"
                            members.add(username) // 添加到 members 清單
                        }
                        setupAccountingDialog(dialog, dialogView)
                    }.addOnFailureListener { e ->
                        showToast("無法獲取成員資料：${e.message}")
                    }
                } else {
                    showToast("無法找到指定的行程")
                }
            }
            .addOnFailureListener { e ->
                showToast("讀取行程失敗：${e.message}")
            }



        dialog.show()

    }
    private fun setupAccountingDialog(dialog: AlertDialog, dialogView: View) {

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

            val accountingNameInput = dialogView.findViewById<EditText>(R.id.accounting_name)
            val accountingName = accountingNameInput.text.toString().takeIf { it.isNotBlank() } ?: "分帳項目"
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
            saveAccountingResultToFirestore(scheduleId,accountingResultId,accountingName,newResults)
            //accountingResults.addAll(newResults)

            Toast.makeText(this, "分帳結果已保存", Toast.LENGTH_SHORT).show()

            dialog.dismiss()

        }
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

                val allResults = mutableListOf<Pair<String, List<Map<String, Any>>>>()

                for (document in querySnapshot.documents) {
                    val title = document.getString("title") ?: "無標題"
                    val results = document.get("results") as? List<Map<String, Any>> ?: emptyList()
                    allResults.add(Pair(title, results))
                }

                // 顯示對話框
                showAccountingResultDialog(allResults)
            }
            .addOnFailureListener { e ->
                showToast("無法讀取分帳紀錄: ${e.message}")
            }
    }
    //用簡單dialog顯示分帳紀錄
    private fun showAccountingResultDialog(results: List<Pair<String, List<Map<String, Any>>>>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_accounting_results, null)
        val resultsContainer = dialogView.findViewById<LinearLayout>(R.id.accounting_results_container)
        val closeButton = dialogView.findViewById<Button>(R.id.close_button)

        val balanceMap = mutableMapOf<String, Double>() // 用於記錄每個人的最終欠款/付款情況

        // 動態生成每個分帳結果
        results.forEach { (title, resultList) ->
            // 計算總金額
            val totalAmount = resultList.sumOf { (it["amount"] as? Double) ?: 0.0 }

            // 創建外框容器
            val resultCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.item_background) // 自訂背景
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params
            }

            // 顯示分帳標題及總金額
            val titleView = TextView(this).apply {
                text = "$title (總金額: $totalAmount)"
                textSize = 18f
                setPadding(8, 8, 8, 8)
                setTypeface(null, Typeface.BOLD)
            }
            resultCard.addView(titleView)

            // 顯示該標題下的詳細內容
            resultList.forEach { result ->
                val debtor = result["debtor"] as? String ?: "未知"
                val creditor = result["creditor"] as? String ?: "未知"
                val amount = result["amount"] as? Double ?: 0.0

                // 略過欠款者與付款者相同的情況
                if (debtor == creditor) return@forEach

                // 記錄欠款與付款數據
                balanceMap[debtor] = (balanceMap[debtor] ?: 0.0) - amount
                balanceMap[creditor] = (balanceMap[creditor] ?: 0.0) + amount

                val resultView = TextView(this).apply {
                    text = "欠款者: $debtor\n付款者: $creditor\n金額: $amount"
                    textSize = 16f
                    setPadding(8, 8, 8, 8)
                }
                resultCard.addView(resultView)
            }

            // 添加外框容器到結果顯示區域
            resultsContainer.addView(resultCard)
        }

        // 計算整合結果
        val finalResults = balanceMap.filterValues { it != 0.0 }

        // 添加整合結果標題
        val summaryTitleView = TextView(this).apply {
            text = "整合結果"
            textSize = 20f
            setPadding(16, 16, 16, 8)
            setTypeface(null, Typeface.BOLD)
        }
        resultsContainer.addView(summaryTitleView)

        // 顯示整合後的每人結餘
        finalResults.forEach { (person, balance) ->
            val summaryView = TextView(this).apply {
                val balanceText = if (balance > 0) "應收款: $balance" else "應付款: ${-balance}"
                text = "$person: $balanceText"
                textSize = 16f
                setPadding(16, 8, 16, 8)
            }
            resultsContainer.addView(summaryView)
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

    private fun saveAccountingResultToFirestore(scheduleId: String, AccountingResultId: String,accounting_title: String, results: List<Map<String, Any>>) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            showToast("用戶未登入")
            return
        }

        val accountingData = hashMapOf(
            "title" to accounting_title, // 新增的標題欄位
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
