package com.example.project_android

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*


class ScheduleFragment : Fragment() {

    companion object {
        private const val ARG_DATE = "date"
        private const val ARG_MEMBERS = "members"

        // 修改 newInstance 方法，接收 date 和 members
        fun newInstance(date: Date, members: List<String>): ScheduleFragment {
            val fragment = ScheduleFragment()
            val args = Bundle()
            args.putSerializable(ARG_DATE, date)
            args.putStringArrayList(ARG_MEMBERS, ArrayList(members)) // 傳遞成員列表
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 獲取日期參數

        val date = arguments?.getSerializable(ARG_DATE) as? Date // 使用安全的轉換
        if (date == null) {
            // 如果日期是 null，可以顯示錯誤或使用當前日期
            val currentDate = Date() // 使用當前日期作為後備
            displayDate(currentDate, view)
        } else {
            displayDate(date, view)
        }

        // 生成24小時排程
        val tableLayout = view.findViewById<TableLayout>(R.id.schedule_table)
        generateScheduleTable(tableLayout)
        // 生成分帳頁面
        val SplitBillButton = view.findViewById<Button>(R.id.SplitBillButton)
        SplitBillButton.setOnClickListener {
            // 彈出對話框
            val dialogView = layoutInflater.inflate(R.layout.dialog_split_bill, null)

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("分帳")
                .setView(dialogView)
                .create()

            // 獲取必要的 UI 元件
            val whoPaidList = dialogView.findViewById<ListView>(R.id.who_paid_list)
            val whoOwesContainer = dialogView.findViewById<LinearLayout>(R.id.who_owes_container)
            val amountPaidInput = dialogView.findViewById<EditText>(R.id.amount_paid_input)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)
            val submitButton = dialogView.findViewById<Button>(R.id.submit_button)

            // 設置 Adapter 給付款者的 ListView
            val members = (arguments?.getSerializable(ARG_MEMBERS) as? ArrayList<String>)
                ?.filter { it.isNotBlank() } ?: arrayListOf()
            val singleChoiceAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_single_choice,
                members
            )
            whoPaidList.adapter = singleChoiceAdapter
            whoPaidList.choiceMode = ListView.CHOICE_MODE_SINGLE

            // 動態生成欠款者的選項
            members.forEach { member ->
                val itemView = layoutInflater.inflate(R.layout.item_who_owes, null)

                val checkBox = itemView.findViewById<CheckBox>(R.id.checkbox_who_owes)
                val amountInput = itemView.findViewById<EditText>(R.id.amount_input)

                checkBox.text = member
                whoOwesContainer.addView(itemView)
            }

            // 按鈕邏輯
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            submitButton.setOnClickListener {
                // 收集數據並進行分帳邏輯
                val selectedWhoPaidPosition = whoPaidList.checkedItemPosition
                if (selectedWhoPaidPosition == -1) {
                    Toast.makeText(requireContext(), "請選擇付錢者", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val whoPaid = members[selectedWhoPaidPosition]

                val whoOwes = mutableListOf<Pair<String, Double>>()
                for (i in 0 until whoOwesContainer.childCount) {
                    val itemView = whoOwesContainer.getChildAt(i)
                    val checkBox = itemView.findViewById<CheckBox>(R.id.checkbox_who_owes)
                    val amountInput = itemView.findViewById<EditText>(R.id.amount_input)

                    if (checkBox.isChecked) {
                        val amount = amountInput.text.toString().toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            Toast.makeText(requireContext(), "請輸入有效金額", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        whoOwes.add(checkBox.text.toString() to amount)
                    }
                }

                if (whoOwes.isEmpty()) {
                    Toast.makeText(requireContext(), "請選擇至少一名欠款者並輸入金額", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val totalOwed = whoOwes.sumOf { it.second }
                val amountPaid = amountPaidInput.text.toString().toDoubleOrNull()
                if (amountPaid == null || totalOwed != amountPaid) {
                    Toast.makeText(requireContext(), "總欠款金額必須等於付款金額", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 顯示結果
                val summary = StringBuilder()
                whoOwes.forEach {
                    // 過濾掉欠款者和付款者相同的情況
                    if (it.first != whoPaid) {
                        summary.append("${it.first} 欠 $whoPaid ${it.second}\n")
                    }
                }

                if (summary.isEmpty()) {
                    Toast.makeText(requireContext(), "無需分帳，付款者和欠款者相同", Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle("分帳結果")
                        .setMessage(summary.toString())
                        .setPositiveButton("確定") { _, _ -> }
                        .show()
                }

                dialog.dismiss()
            }

            dialog.show()
        }




    }



    private fun displayDate(date: Date, view: View) {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        // 設定日期標題
        val dateTextView = view.findViewById<TextView>(R.id.date_text)
        dateTextView.text = dateFormat.format(date)
    }

    // 生成24小時行程表
    private fun generateScheduleTable(tableLayout: TableLayout) {
        for (i in 0..23) {
            val tableRow = TableRow(context)
            val timeText = TextView(context).apply {
                text = String.format("%02d:00", i) // 格式化時間顯示
            }
            tableRow.addView(timeText)

            val eventText = EditText(context).apply {
                hint = "輸入行程"
            }
            tableRow.addView(eventText)

            tableLayout.addView(tableRow)
        }
    }
}
