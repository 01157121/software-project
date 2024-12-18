package com.example.project_android
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


data class User(
    val id: String,
    val name: String,
    val role: String
)

class UserListAdapter(
    private val userList: List<User>,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.user_name_text)
        val userRoleTextView: TextView = itemView.findViewById(R.id.user_role_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userNameTextView.text = user.name
        holder.userRoleTextView.text = user.role

        holder.itemView.setOnClickListener {
            onUserClick(user.id)
        }
    }

    override fun getItemCount(): Int = userList.size
}
