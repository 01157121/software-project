package com.example.project_android

import DatePageAdapter
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
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
    private lateinit var scheduleName: String
    private lateinit var feedbackButton: FloatingActionButton // 回饋按鈕
    private var accountingResultDialog: AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planning)

        // 接收 Intent 数据
        scheduleName = intent.getStringExtra("SCHEDULE_NAME")?: throw IllegalArgumentException("SCHEDULE_NAME is required")
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
        feedbackButton.setOnClickListener {
            val intent = Intent(this, FeedbackActivity::class.java)
            intent.putExtra("SCHEDULE_ID", scheduleId)
            startActivity(intent)
        }
        feedbackButton.visibility = if (endDate >= today) View.GONE else View.VISIBLE

        //checkIfFeedbackNeeded()
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
                R.id.nav_export -> showExportDialog()
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

    // 顯示登出確認對話框
    private fun showExportDialog() {
        AlertDialog.Builder(this)
            .setTitle("匯出")
            .setMessage("確定要匯出整份行程表嗎？")
            .setPositiveButton("確定") { _, _ ->
                exportSchedule()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun exportSchedule() {
        val db = FirebaseFirestore.getInstance()
        val scheduleRef = db.collection("schedules").document(scheduleId).collection("dates")
        val dateDataList = mutableListOf<Pair<String, List<Map<String, Any>>>>()

        scheduleRef.get()
            .addOnSuccessListener { dateDocuments ->
                if (dateDocuments.isEmpty) {
                    showToast("沒有行程可匯出")
                    return@addOnSuccessListener
                }

                val tasks = mutableListOf<Task<QuerySnapshot>>()

                for (dateDoc in dateDocuments) {
                    val date = dateDoc.id // 日期名稱
                    val plansRef = dateDoc.reference.collection("plans")
                    tasks.add(plansRef.get().addOnSuccessListener { plans ->
                        // 提取所有行程並排序
                        val sortedPlans = plans.map { planDoc ->
                            val planName = planDoc.getString("planName") ?: "未命名"
                            val startHour = planDoc.getLong("startHour")?.toInt() ?: 0
                            val startMinute = planDoc.getLong("startMinute")?.toInt() ?: 0
                            val endHour = planDoc.getLong("endHour")?.toInt() ?: 0
                            val endMinute = planDoc.getLong("endMinute")?.toInt() ?: 0

                            mapOf(
                                "planName" to planName,
                                "startHour" to startHour,
                                "startMinute" to startMinute,
                                "endHour" to endHour,
                                "endMinute" to endMinute
                            )
                        }.sortedWith(compareBy(
                            { it["startHour"] as Int },
                            { it["startMinute"] as Int }
                        ))

                        // 將日期和對應的行程存入暫存集合
                        dateDataList.add(date to sortedPlans)
                    })
                }

                // 等待所有異步操作完成
                Tasks.whenAllSuccess<QuerySnapshot>(tasks)
                    .addOnSuccessListener {
                        // 按日期排序
                        dateDataList.sortBy { it.first } // 假設日期名稱可以按字典序排序

                        // 生成 CSV
                        val csvBuilder = StringBuilder()
                        for ((date, plans) in dateDataList) {
                            // 添加日期標題
                            csvBuilder.append("日期: $date\n")
                            csvBuilder.append("時間,行程名稱\n") // 中文標題

                            // 填入行程資料
                            for (plan in plans) {
                                val timeRange = String.format(
                                    "%02d:%02d - %02d:%02d",
                                    plan["startHour"], plan["startMinute"],
                                    plan["endHour"], plan["endMinute"]
                                )
                                csvBuilder.append("$timeRange,${plan["planName"]}\n")
                            }

                            csvBuilder.append("\n") // 分隔不同日期
                        }

                        // 匯出成 Excel 文件
                        val csvContent = csvBuilder.toString()
                        convertCsvToExcel(scheduleName, csvContent)
                    }
                    .addOnFailureListener { e ->
                        showToast("匯出行程失敗: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showToast("讀取行程失敗: ${e.message}")
            }
    }


    private fun goToHomePage() {
        val intent = Intent(this, LobbyActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun convertCsvToExcel(scheduleName: String, csvContent: String) {
        val workbook = XSSFWorkbook() // 創建工作簿

        try {
            // 將 CSV 內容按日期分組
            val dateSections = csvContent.split("\n\n") // 每個日期用空行分隔
            for (section in dateSections) {
                val lines = section.split("\n").filter { it.isNotBlank() }
                if (lines.isEmpty()) continue

                // 第一行是日期標題
                val date = lines[0].replace("日期: ", "").trim()

                // 確保工作表名稱合法，替換非法字符
                val safeSheetName = date.replace("[\\/:*?\"<>|]".toRegex(), "_")
                val sheet = workbook.createSheet(safeSheetName)

                // 設定欄位寬度
                sheet.setColumnWidth(0, 5000) // 時間欄寬度
                sheet.setColumnWidth(1, 10000) // 行程名稱欄寬度

                // 其餘行是表頭和內容
                for ((rowIndex, line) in lines.drop(1).withIndex()) {
                    val excelRow = sheet.createRow(rowIndex)
                    val cells = line.split(",")

                    for ((cellIndex, cell) in cells.withIndex()) {
                        excelRow.createCell(cellIndex).setCellValue(cell)
                    }
                }
            }

            // 匯出到下載目錄
            exportScheduleToDownloads(workbook, scheduleName, csvContent)
        } catch (e: Exception) {
            showToast("CSV 轉 XLSX 失敗: ${e.message}")
        } finally {
            try {
                workbook.close() // 確保工作簿正確關閉
            } catch (e: Exception) {
                showToast("關閉 Workbook 時發生錯誤: ${e.message}")
            }
        }
    }


    private fun exportScheduleToDownloads(workbook: XSSFWorkbook, scheduleName: String, csvContent: String) {
        val xlsxFileName = "$scheduleName.xlsx"
        val csvFileName = "$scheduleName.csv"

        // 匯出 XLSX 檔案
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = applicationContext.contentResolver

            // 匯出 XLSX
            val xlsxContentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, xlsxFileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val xlsxUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, xlsxContentValues)
            xlsxUri?.let {
                try {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        workbook.write(outputStream)
                    }
                    showToast("XLSX 行程表已匯出到下載目錄: $xlsxFileName")
                } catch (e: Exception) {
                    showToast("匯出 XLSX 失敗: ${e.message}")
                }
            } ?: showToast("無法創建 XLSX 檔案 URI")

            // 匯出 CSV
            val csvContentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, csvFileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val csvUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, csvContentValues)
            csvUri?.let {
                try {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        // 在寫入 CSV 時添加 BOM
                        val bom = "\uFEFF"
                        outputStream.write((bom + csvContent).toByteArray(Charsets.UTF_8))
                    }
                    showToast("CSV 行程表已匯出到下載目錄: $csvFileName")
                } catch (e: Exception) {
                    showToast("匯出 CSV 失敗: ${e.message}")
                }
            } ?: showToast("無法創建 CSV 檔案 URI")
        } else {
            // Android 10 以下版本
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            // 匯出 XLSX
            val xlsxFile = File(downloadsDir, xlsxFileName)
            try {
                FileOutputStream(xlsxFile).use { outputStream ->
                    workbook.write(outputStream)
                }
                showToast("XLSX 行程表已匯出到下載目錄: $xlsxFileName")
            } catch (e: Exception) {
                showToast("匯出 XLSX 失敗: ${e.message}")
            }

            // 匯出 CSV
            val csvFile = File(downloadsDir, csvFileName)
            try {
                FileOutputStream(csvFile).use { outputStream ->
                    // 在寫入 CSV 時添加 BOM
                    val bom = "\uFEFF"
                    outputStream.write((bom + csvContent).toByteArray(Charsets.UTF_8))
                }
                showToast("CSV 行程表已匯出到下載目錄: $csvFileName")
            } catch (e: Exception) {
                showToast("匯出 CSV 失敗: ${e.message}")
            }
        }

        // 確保 Workbook 資源正確關閉
        try {
            workbook.close()
        } catch (e: Exception) {
            showToast("關閉 Workbook 時發生錯誤: ${e.message}")
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
        // 如果舊對話框存在，先關閉
        accountingResultDialog?.dismiss()
        accountingResultDialog = null
        if (results.isEmpty()) {
            showToast("沒有找到任何分帳紀錄")
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_accounting_results, null)
        val resultsContainer = dialogView.findViewById<LinearLayout>(R.id.accounting_results_container)
        val closeButton = dialogView.findViewById<Button>(R.id.close_button)

        val balanceMap = mutableMapOf<String, Double>() // 用於記錄每個人的最終欠款/付款情況

        results.forEach { (title, resultList) ->
            val totalAmount = resultList.sumOf { (it["amount"] as? Double) ?: 0.0 }
            val resultCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.item_background)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params
            }

            val titleView = TextView(this).apply {
                text = "$title (總金額: $totalAmount)"
                textSize = 18f
                setPadding(8, 8, 8, 8)
                setTypeface(null, Typeface.BOLD)
            }
            resultCard.addView(titleView)

            resultList.forEach { result ->
                val debtor = result["debtor"] as? String ?: "未知"
                val creditor = result["creditor"] as? String ?: "未知"
                val amount = result["amount"] as? Double ?: 0.0
                if (debtor == creditor) return@forEach
                balanceMap[debtor] = (balanceMap[debtor] ?: 0.0) - amount
                balanceMap[creditor] = (balanceMap[creditor] ?: 0.0) + amount
                val resultView = TextView(this).apply {
                    text = "欠款者: $debtor\n付款者: $creditor\n金額: $amount"
                    textSize = 16f
                    setPadding(8, 8, 8, 8)
                }
                resultCard.addView(resultView)
            }

            // 添加長按事件
            resultCard.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("刪除分帳紀錄")
                    .setMessage("確定要刪除分帳紀錄 \"$title\" 嗎？")
                    .setPositiveButton("刪除") { _, _ ->
                        deleteAccountingResultFromFirestore(scheduleId, title)
                    }
                    .setNegativeButton("取消", null)
                    .show()
                true
            }

            resultsContainer.addView(resultCard)
        }

        val summaryTitleView = TextView(this).apply {
            text = "整合結果"
            textSize = 20f
            setPadding(16, 16, 16, 8)
            setTypeface(null, Typeface.BOLD)
        }
        resultsContainer.addView(summaryTitleView)

        val finalResults = balanceMap.filterValues { it != 0.0 }
        finalResults.forEach { (person, balance) ->
            val summaryView = TextView(this).apply {
                val balanceText = if (balance > 0) "應收款: $balance" else "應付款: ${-balance}"
                text = "$person: $balanceText"
                textSize = 16f
                setPadding(16, 8, 16, 8)
            }
            resultsContainer.addView(summaryView)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        accountingResultDialog = dialog // 將新的對話框存儲起來
    }
    private fun deleteAccountingResultFromFirestore(scheduleId: String, accountingTitle: String) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("accounting_results")
            .whereEqualTo("title", accountingTitle)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    showToast("找不到分帳紀錄")
                    return@addOnSuccessListener
                }

                for (document in querySnapshot.documents) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            showToast("分帳紀錄已刪除")
                            // 重新載入分帳結果
                            showAccountingResult()
                        }
                        .addOnFailureListener { e ->
                            showToast("刪除分帳紀錄失敗: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                showToast("無法查詢分帳紀錄: ${e.message}")
            }
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
