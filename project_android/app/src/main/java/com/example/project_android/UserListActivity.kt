package com.example.project_android
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UserListActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var addUserButton: FloatingActionButton

    private val scheduleId: String = "your_schedule_id" // Replace with the actual ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        firestore = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.user_list_recycler_view)
        addUserButton = findViewById(R.id.add_user_button)

        recyclerView.layoutManager = LinearLayoutManager(this)

        loadUserList()

        addUserButton.setOnClickListener {
            showAddUserDialog()
        }
    }

    private fun loadUserList() {
        firestore.collection("schedules").document(scheduleId)
            .get()
            .addOnSuccessListener { document ->
                val collaborators = document["collaborators"] as? List<Map<String, String>> ?: listOf()
                val userList = collaborators.map {
                    User(
                        id = it["id"] ?: "",
                        name = it["name"] ?: "Unknown",
                        role = if (it["id"] == document["ownerId"]) "主辦人" else "旅伴"
                    )
                }

                recyclerView.adapter = UserListAdapter(userList) { userId ->
                    showUserDetails(userId)
                }
            }
    }

    private fun showUserDetails(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val userName = document["name"] as? String ?: "Unknown"
                val userEmail = document["email"] as? String ?: "Unknown"

                val dialogView = layoutInflater.inflate(R.layout.dialog_user_details, null)
                dialogView.findViewById<TextView>(R.id.user_name_text).text = userName
                dialogView.findViewById<TextView>(R.id.user_email_text).text = userEmail

                val dialog = AlertDialog.Builder(this)
                    .setTitle("用戶詳細資料")
                    .setView(dialogView)
                    .setNegativeButton("關閉", null)
                    .create()
                dialog.show()
            }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.email_edit_text)

        val dialog = AlertDialog.Builder(this)
            .setTitle("新增用戶")
            .setView(dialogView)
            .setPositiveButton("加入") { _, _ ->
                val email = emailEditText.text.toString()

                firestore.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val userId = querySnapshot.documents[0].id

                            firestore.collection("users").document(userId)
                                .update("schedules", FieldValue.arrayUnion(scheduleId))

                            firestore.collection("schedules").document(scheduleId)
                                .update("collaborators", FieldValue.arrayUnion(userId))

                            loadUserList() // Refresh the list
                        }
                    }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }
}
