package com.example.project_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ModuleAdapter(
    private val modules: List<Module>,
    private val onClick: (Module) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_module, parent, false)
        return ModuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = modules[position]
        holder.bind(module)
        holder.itemView.setOnClickListener { onClick(module) }
        holder.menuButton.setOnClickListener {
            showPopupMenu(holder.menuButton, module.id)
        }
    }

    override fun getItemCount() = modules.size

    private fun showPopupMenu(view: View, scheduleId: String) {
        val popup = PopupMenu(view.context, view)
        popup.inflate(R.menu.module_menu) // 右側功能表的選項
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_delete -> {
                    onDelete(scheduleId) // 點擊刪除時的操作
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    inner class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val menuButton: ImageView = itemView.findViewById(R.id.module_menu)
        private val icon: ImageView = itemView.findViewById(R.id.module_icon)
        private val nameTextView: TextView = itemView.findViewById(R.id.module_name)
        private val dateRangeTextView: TextView = itemView.findViewById(R.id.module_date_range)

        fun bind(module: Module) {
            nameTextView.text = module.name
            dateRangeTextView.text = "${module.startDate} - ${module.endDate}"
            // 如果需要修改圖示，這裡可以根據條件改變圖標
        }
    }
}


