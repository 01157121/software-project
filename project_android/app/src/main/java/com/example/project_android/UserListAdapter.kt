package com.example.project_android
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

// 資料模型
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String // "主辦人" 或 "旅伴"
)

class UserListAdapter(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private var userList: List<User> = emptyList(), // 用戶清單
    private val onUserClick: (String) -> Unit // 點擊事件 callback，傳遞用戶 ID
) : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    // ViewHolder 類別
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.user_name_text)
        val userRoleTextView: TextView = itemView.findViewById(R.id.user_role_text)
    }

    // 創建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    // 綁定資料
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userNameTextView.text = user.name
        holder.userRoleTextView.text = user.role

        // 點擊事件
        holder.itemView.setOnClickListener {
            onUserClick(user.id) // 傳遞用戶 ID 到 `showUserDetails`
        }
    }

    // 用戶數量
    override fun getItemCount(): Int = userList.size

    // 設置用戶清單
    fun setUsers(ownerId: String, collaborators: List<String>) {
        val updatedList = mutableListOf<User>()
        val tasks = mutableListOf<Task<DocumentSnapshot>>() // 用來追蹤所有 Firestore 請求

        // 加載主辦人
        if (ownerId.isNotEmpty()) {
            val ownerTask = db.collection("users").document(ownerId).get().addOnCompleteListener { task ->
                val username = if (task.isSuccessful && task.result != null) {
                    task.result?.getString("username") ?: "主辦人"
                } else {
                    "載入失敗"
                }
                val email = if (task.isSuccessful && task.result != null) {
                    task.result?.getString("email") ?: "無郵件"
                } else {
                    "無郵件"
                }
                updatedList.add(User(id = ownerId, name = username, email = email, role = "主辦人"))
            }
            tasks.add(ownerTask)
        }

        // 加載旅伴
        collaborators.forEach { collaboratorId ->
            val collaboratorTask = db.collection("users").document(collaboratorId).get().addOnCompleteListener { task ->
                val username = if (task.isSuccessful && task.result != null) {
                    task.result?.getString("username") ?: "旅伴"
                } else {
                    "載入失敗"
                }
                val email = if (task.isSuccessful && task.result != null) {
                    task.result?.getString("email") ?: "無郵件"
                } else {
                    "無郵件"
                }
                updatedList.add(User(id = collaboratorId, name = username, email = email, role = "旅伴"))
            }
            tasks.add(collaboratorTask)
        }

        // 等待所有請求完成
        Tasks.whenAllComplete(tasks).addOnCompleteListener {
            // 所有請求完成後更新數據
            userList = updatedList
            notifyDataSetChanged()
        }
    }


}
