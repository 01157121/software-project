package com.example.project_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ModuleAdapter(private val moduleList: List<Module>) : RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {

    // 定義每個項目的 ViewHolder
    class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val moduleName: TextView = itemView.findViewById(R.id.module_name)
    }

    // 創建 ViewHolder，並將單個模塊的布局填充到 RecyclerView 項目中
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_module, parent, false)
        return ModuleViewHolder(view)
    }

    // 綁定資料到 ViewHolder
    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = moduleList[position]
        holder.moduleName.text = module.name
    }

    // 返回項目數量
    override fun getItemCount(): Int = moduleList.size
}
