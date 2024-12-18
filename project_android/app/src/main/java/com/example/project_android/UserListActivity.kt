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
import com.example.project_android.R
import com.example.project_android.UserListAdapter
import com.example.project_android.models.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.ktx.toObject

class UserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserListAdapter
    private lateinit var addUserButton: FloatingActionButton

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        recyclerView = findViewById(R.id.recycler_view)
        addUserButton = findViewById(R.id.fab_add_user)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UserListAdapter { userId -> showUserDetails(userId) }
        recyclerView.adapter = adapter

        val scheduleId = intent.getStringExtra("scheduleId")
        if (scheduleId != null) {
            fetchUserList(scheduleId)
        } else {
            Toast.makeText(this, "Invalid schedule ID", Toast.LENGTH_SHORT).show()
            finish()
        }

        addUserButton.setOnClickListener {
            if (scheduleId != null) {
                showAddUserDialog(scheduleId)
            }
        }
    }

    private fun fetchUserList(scheduleId: String) {
        firestore.collection("schedules").document(scheduleId).get()
            .addOnSuccessListener { document: DocumentSnapshot ->
                val owner = document.getString("owner") ?: "Unknown"
                val collaborators = document.get("collaborators") as? List<String> ?: emptyList()

                val userList = mutableListOf<User>()
                userList.add(User(name = owner, isHost = true)) // 假設 User 類有一個接受名字的構造函數

                collaborators.forEach { collaboratorId ->
                    firestore.collection("users").document(collaboratorId).get()
                        .addOnSuccessListener { userDoc: DocumentSnapshot ->
                            val user = userDoc.toObject<User>()
                            if (user != null) {
                                userList.add(user)
                                // 通知 adapter 更新數據
                               // adapter.updateUsers(userList)
                            }
                        }
                }
            }

            .addOnFailureListener {
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUserDetails(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject<User>()
                if (user != null) {
                    val view = LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null)
                    view.findViewById<TextView>(R.id.user_name).text = user.name
                    view.findViewById<TextView>(R.id.user_email).text = user.email

                    AlertDialog.Builder(this)
                        .setTitle("User Details")
                        .setView(view)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user details", Toast.LENGTH_SHORT).show()
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

                    // Update user's schedules
                    firestore.collection("users").document(userId).update("schedules", FieldValue.arrayUnion(scheduleId))
                        .addOnSuccessListener {
                            // Update schedule's collaborators
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
