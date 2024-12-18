package com.example.project_android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.util.Calendar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.bumptech.glide.Glide

class LobbyActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: FloatingActionButton
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)
        val userId = auth.currentUser?.uid
        val usernameTextView = findViewById<TextView>(R.id.user_name)
        if (userId != null) {
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document != null) {
                    val username = document.getString("username")
                    usernameTextView.text = username ?: "使用者"
                }
            }.addOnFailureListener {
                usernameTextView.text = "載入失敗"
            }
        } else {
            usernameTextView.text = "未登入"
        }
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.navigation_view)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.open_drawer, R.string.close_drawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navView.setNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    showProfileDialog()
                }
                R.id.nav_settings -> {

                }
            }
            drawerLayout.closeDrawers()
            true
        }
        recyclerView = findViewById(R.id.module_list)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = ModuleAdapter(getModuleList()) // 假設你有一個模塊清單資料

        addButton = findViewById(R.id.add_button)
        addButton.setOnClickListener {
            showCreateScheduleDialog()
        }
    }

    private fun showProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile, null)

        val profileImageView = dialogView.findViewById<ImageView>(R.id.profile_image)
        val usernameTextView = dialogView.findViewById<TextView>(R.id.profile_username)
        val emailTextView = dialogView.findViewById<TextView>(R.id.profile_email)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document != null) {
                    val username = document.getString("username")
                    val email = document.getString("email")
                    val avatarUrl = document.getString("avatarUrl")

                    usernameTextView.text = username
                    emailTextView.text = email

                    if (!avatarUrl.isNullOrEmpty()) {
                        // 使用 Glide 或 Picasso 加載頭像
                        Glide.with(this).load(avatarUrl).into(profileImageView)
                    }
                }
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("個人資料")
            .setView(dialogView)
            .setPositiveButton("關閉", null)
            .create()
        dialog.show()
    }



    fun openDrawer(view: View) {
        drawerLayout.openDrawer(GravityCompat.START)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun getModuleList(): List<Module> {
        return listOf(
            Module("預設推薦行程"),
            Module("其他人的行程"),
            Module("其他人的行程"),
            Module("其他人的行程")
            // 更多模塊...
        )
    }

    // 顯示創建行程表的對話框
    private fun showCreateScheduleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_schedule, null)
        val startDateButton: View = dialogView.findViewById(R.id.start_date_button)
        val endDateButton: View = dialogView.findViewById(R.id.end_date_button)
        val AddNewMenberButton: View = dialogView.findViewById(R.id.AddNewMenber) //新增成員按鈕
        // 初始化 Spinner 的邏輯
        val memberSpinner = dialogView.findViewById<Spinner>(R.id.member_spinner)
        // 預設成員列表
        val memberList = mutableListOf("")
        // Adapter 設定
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, memberList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        memberSpinner.adapter = adapter
        startDateButton.setOnClickListener {
            showDatePickerDialog { selectedDate ->
                // 將選中的日期設置到按鈕上
                startDateButton.findViewById<View>(R.id.start_date_button_text).apply {
                    (this as? TextView)?.text = selectedDate
                }
            }
        }
        endDateButton.setOnClickListener {
            showDatePickerDialog { selectedDate ->
                // 同樣設置結束日期
                endDateButton.findViewById<View>(R.id.end_date_button_text).apply {
                    (this as? TextView)?.text = selectedDate
                }
            }
        }
        //讓新增成員按鈕
        AddNewMenberButton.setOnClickListener {
            showAddMemberDialog { memberName ->
                // 處理新增的成員名稱（例如，保存到列表或顯示）
                // 同時更新下拉式選單
                if (memberList.contains(memberName)) {
                    Toast.makeText(this, "該成員已存在，無法重複新增", Toast.LENGTH_SHORT).show()
                } else {
                    // 新增成員到列表並更新 Adapter
                    memberList.add(memberName)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "新增成員：$memberName", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("創建行程表")
            .setView(dialogView)
            .setPositiveButton("創建") { _, _ ->
                val scheduleName = dialogView.findViewById<EditText>(R.id.schedule_name).text.toString()
                val startDate = dialogView.findViewById<TextView>(R.id.start_date_button_text).text.toString()
                val endDate = dialogView.findViewById<TextView>(R.id.end_date_button_text).text.toString()

                val members = memberList.toList()

                if (scheduleName.isBlank() || startDate.isBlank() || endDate.isBlank()) {

                    showToast("請填寫完整信息")
                    return@setPositiveButton
                }

                // 生成行程表 ID
                val scheduleId = generateUniqueScheduleId()

                // 將行程表存入 Firestore
                saveScheduleToFirestore(scheduleId, scheduleName, startDate, endDate)

                val intent = Intent(this, PlanningActivity::class.java)
                intent.putExtra("SCHEDULE_NAME", scheduleName)
                intent.putExtra("START_DATE", startDate)
                intent.putExtra("END_DATE", endDate)
                intent.putExtra("SCHEDULE_ID", scheduleId)
                intent.putStringArrayListExtra("MEMBERS_LIST", ArrayList(members))
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()

        dialogView.findViewById<Button>(R.id.start_date_button).setOnClickListener {
            showDatePickerDialog { selectedDate ->
                dialogView.findViewById<TextView>(R.id.start_date_button_text).text = selectedDate
            }
        }

        dialogView.findViewById<Button>(R.id.end_date_button).setOnClickListener {
            showDatePickerDialog { selectedDate ->
                dialogView.findViewById<TextView>(R.id.end_date_button_text).text = selectedDate
            }
        }
    }

    private fun generateUniqueScheduleId(): String {
        return java.util.UUID.randomUUID().toString()
    }

    // 顯示日期選擇器
    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = "$selectedYear/${selectedMonth + 1}/$selectedDay"
            onDateSelected(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    //fun 來跳出新增成員的dialog
    private fun showAddMemberDialog(onMemberAdded: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_member, null)
        val memberNameEditText = dialogView.findViewById<EditText>(R.id.member_name_edit_text)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("新增成員")
            .setView(dialogView)
            .setPositiveButton("確認") { _, _ ->
                val memberName = memberNameEditText.text.toString()
                if (memberName.isNotBlank()) {
                    onMemberAdded(memberName)
                } else {
                    Toast.makeText(this, "請輸入有效的名字", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }


    private fun saveScheduleToFirestore(scheduleId: String, name: String, startDate: String, endDate: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            showToast("用戶未登入")
            return
        }

        val scheduleData = hashMapOf(
            "scheduleId" to scheduleId,
            "name" to name,
            "startDate" to startDate,
            "endDate" to endDate,
            "createdBy" to currentUserId,
            "collaborators" to listOf(currentUserId) // 默認創建者為唯一編輯者
        )
        db.collection("schedules")
            .document(scheduleId)
            .set(scheduleData)
            .addOnSuccessListener {
                saveScheduleToUserCollection(currentUserId, scheduleId)

            }
            .addOnFailureListener { e ->
                showToast("行程表創建失敗: ${e.message}")
            }
    }

    private fun saveScheduleToUserCollection(userId: String, scheduleId: String) {
        val userScheduleData = hashMapOf(
            "scheduleId" to scheduleId
        )

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .collection("schedules")
            .document(scheduleId) // 使用行程表 ID 作為文檔 ID
            .set(userScheduleData)
            .addOnSuccessListener {
                showToast("行程表已成功添加！")
            }
            .addOnFailureListener { e ->
                showToast("行程表添加到用戶資料失敗: ${e.message}")
            }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
