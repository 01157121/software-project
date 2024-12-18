package com.example.project_android

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_android.models.User


class UserListAdapter(
    private val userList: List<User>, // 傳遞的用戶清單
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.user_name_text)
        val userRoleTextView: TextView = itemView.findViewById(R.id.user_role_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false) // 對應單個用戶的佈局
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        // 設定用戶名稱與角色
        holder.userNameTextView.text = user.name
        holder.userRoleTextView.text = user.role

        // 設置點擊事件
        holder.itemView.setOnClickListener {
            onUserClick(user.id) // 傳遞用戶 ID 給 callback
        }
    }

    override fun getItemCount(): Int = userList.size

    // 更新用戶資料的方法
    fun setUsers(owner: User, collaborators: List<User>) {
        userList.clear() // 清空舊的資料
        userList.add(owner) // 添加主辦人
        userList.addAll(collaborators) // 添加旅伴
        notifyDataSetChanged() // 通知 RecyclerView 資料變更
    }

    override fun getItemViewType(position: Int): Int {
        return if (userList[position].isHost) TYPE_HOST else TYPE_COLLABORATOR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            TYPE_HOST -> {
                val view = inflater.inflate(R.layout.dialog_user_details, parent, false)
                HostViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.dialog_user_details, parent, false)
                CollaboratorViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val user = userList[position]
        when (holder) {
            is HostViewHolder -> holder.bind(user)
            is CollaboratorViewHolder -> holder.bind(user)
        }
    }

    override fun getItemCount(): Int = userList.size

    inner class HostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val userNameText: TextView = view.findViewById(R.id.user_name)
        private val userEmailText: TextView = view.findViewById(R.id.user_email)

        fun bind(user: User) {
            userNameText.text = user.name
            userEmailText.text = user.email
            itemView.setOnClickListener { onUserClick(user) }
        }
    }

    inner class CollaboratorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val userNameText: TextView = view.findViewById(R.id.user_name)
        private val userEmailText: TextView = view.findViewById(R.id.user_email)

        fun bind(user: User) {
            userNameText.text = user.name
            userEmailText.text = user.email
            itemView.setOnClickListener { onUserClick(user) }
        }
    }
}
