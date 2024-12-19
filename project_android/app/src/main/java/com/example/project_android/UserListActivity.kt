package com.example.project_android
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class UserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserListAdapter
    private lateinit var addUserButton: FloatingActionButton
    private var scheduleId: String? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        recyclerView = findViewById(R.id.recycler_view)
        addUserButton = findViewById(R.id.fab_add_user)
        scheduleId = intent.getStringExtra("SCHEDULE_ID")
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UserListAdapter { userId -> showUserDetails(userId, scheduleId ?: "", currentUserId ?: "") }
        recyclerView.adapter = adapter


        scheduleId?.let { id ->
            fetchUserList(id) // 在這裡 id 是非空的
        } ?: run {
            Toast.makeText(this, "Invalid schedule ID", Toast.LENGTH_SHORT).show()
            finish()
        }

        addUserButton.setOnClickListener {
            scheduleId?.let { id ->
                showAddUserDialog(id) // 在這裡 id 是非空的
            } ?: run {
                Toast.makeText(this, "Invalid schedule ID", Toast.LENGTH_SHORT).show()
            }
        }

    }
    

    private fun fetchUserList(scheduleId: String) {
        firestore.collection("schedules").document(scheduleId).get()
            .addOnSuccessListener { document ->
                val owner = document.getString("createdBy") ?: "Unknown"
                val collaborators = document.get("collaborators") as? List<String> ?: emptyList()

                adapter.setUsers(owner, collaborators)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUserDetails(userId: String, scheduleId: String, currentUserId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = User(
                    id = userId,
                    name = document.getString("username") ?: "Unknown",
                    email = document.getString("email") ?: "Unknown",
                    role = ""
                )

                // 加載行程以檢查是否為 createBy 使用者
                firestore.collection("schedules").document(scheduleId).get()
                    .addOnSuccessListener { scheduleDoc ->
                        val createBy = scheduleDoc.getString("createdBy") ?: ""

                        val view = LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null)
                        view.findViewById<TextView>(R.id.user_name).text = user.name
                        view.findViewById<TextView>(R.id.user_email).text = user.email

                        val dialogBuilder = AlertDialog.Builder(this)
                            .setTitle("User Details")
                            .setView(view)
                            .setPositiveButton("OK", null)

                        // 如果是 createBy，顯示「移除旅伴」按鈕
                        if (currentUserId == createBy) {
                            dialogBuilder.setNegativeButton("移除旅伴") { _, _ ->
                                removeCollaborator(scheduleId, userId)
                            }
                        }

                        dialogBuilder.show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "無法檢查行程建立者", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user details", Toast.LENGTH_SHORT).show()
            }
    }

    // 移除旅伴的邏輯
    private fun removeCollaborator(scheduleId: String, userId: String) {
        val scheduleRef = firestore.collection("schedules").document(scheduleId)
        val userScheduleRef = firestore.collection("users").document(userId).collection("schedules").document(scheduleId)

        firestore.runTransaction { transaction ->
            // 移除 `collaborators` 中的 userId
            val scheduleSnapshot = transaction.get(scheduleRef)
            val collaborators = scheduleSnapshot.get("collaborators") as? MutableList<String> ?: mutableListOf()
            if (collaborators.contains(userId)) {
                collaborators.remove(userId)
                transaction.update(scheduleRef, "collaborators", collaborators)
            }

            // 刪除 `users` 中該行程的記錄
            transaction.delete(userScheduleRef)
        }.addOnSuccessListener {
            Toast.makeText(this, "旅伴已成功移除", Toast.LENGTH_SHORT).show()
            fetchUserList(scheduleId)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "移除旅伴失敗：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showAddUserDialog(scheduleId: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null)
        val emailInput = view.findViewById<EditText>(R.id.add_user_email_input)

        AlertDialog.Builder(this)
            .setTitle("Add User")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val email = emailInput.text.toString()
                if (email.isNotEmpty()) {
                    addUserToSchedule(scheduleId, email)
                } else {
                    Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addUserToSchedule(scheduleId: String, email: String) {
        firestore.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val userDoc = querySnapshot.documents[0]
                    val userId = userDoc.id

                    // 更新用戶的日程
                    val userScheduleData = hashMapOf(
                        "scheduleId" to scheduleId
                    )
                    firestore.collection("users")
                        .document(userId)
                        .collection("schedules")
                        .document(scheduleId) // 使用行程表 ID 作為文檔 ID
                        .set(userScheduleData)
                        .addOnSuccessListener {
                            // 更新日程的合作者
                            firestore.collection("schedules").document(scheduleId).update("collaborators", FieldValue.arrayUnion(userId))
                                .addOnSuccessListener {
                                    Toast.makeText(this, "User added successfully", Toast.LENGTH_SHORT).show()
                                    fetchUserList(scheduleId)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to update collaborators", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update user's schedules", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to search for user", Toast.LENGTH_SHORT).show()
            }
    }

}
