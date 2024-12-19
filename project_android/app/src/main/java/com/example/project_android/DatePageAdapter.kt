import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_android.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class DatePageAdapter(
    private val context: Context,
    private val startDate: Date,
    private val endDate: Date
) : RecyclerView.Adapter<DatePageAdapter.DateViewHolder>() {

    private val adapterDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val totalDays: Int

    init {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        val startTime = calendar.timeInMillis

        calendar.time = endDate
        val endTime = calendar.timeInMillis

        totalDays = ((endTime - startTime) / (1000 * 60 * 60 * 24)).toInt() + 1 // 包含當天
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_date_page, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val currentDate = getDateForPosition(position)
        holder.dateTextView.text = adapterDateFormat.format(currentDate)

        holder.addButton.setOnClickListener {
            // 顯示行程創建對話框
            showCreatePlan(currentDate, holder)
        }
    }

    override fun getItemCount(): Int = totalDays

    private fun getDateForPosition(position: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar.add(Calendar.DAY_OF_YEAR, position)
        return calendar.time
    }

    private fun showCreatePlan(selectedDate: Date, holder: DateViewHolder) {
        // 顯示行程創建對話框
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_plan, null)

        val dateButton: Button = dialogView.findViewById(R.id.date_button)
        val startTimeButton: Button = dialogView.findViewById(R.id.start_time_button)
        val endTimeButton: Button = dialogView.findViewById(R.id.end_time_button)

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        dateButton.text = dateFormat.format(selectedDate)

        var startHour = 0
        var startMinute = 0
        var endHour = 0
        var endMinute = 0

        dateButton.setOnClickListener {
            // 顯示日期選擇器
            showDatePickerDialog { selectedDate ->
                dateButton.text = selectedDate
            }
        }

        startTimeButton.setOnClickListener {
            // 顯示時間選擇器
            showTimePickerDialog { hour, minute ->
                startHour = hour
                startMinute = minute
                startTimeButton.text = String.format("%02d:%02d", hour, minute)
            }
        }

        endTimeButton.setOnClickListener {
            // 顯示時間選擇器
            showTimePickerDialog { hour, minute ->
                endHour = hour
                endMinute = minute
                endTimeButton.text = String.format("%02d:%02d", hour, minute)
            }
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("新增行程")
            .setView(dialogView)
            .setPositiveButton("新增") { _, _ ->
                val planName = dialogView.findViewById<EditText>(R.id.plan_name).text.toString()
                addScheduleToContainer(planName, selectedDate, startHour, startMinute, endHour, endMinute, holder)
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }

    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            onDateSelected("$year/${month + 1}/$day")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePickerDialog(onTimeSelected: (Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(context, { _, hour, minute ->
            onTimeSelected(hour, minute)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun addScheduleToContainer(planName: String, selectedDate: Date, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int, holder: DateViewHolder) {
        // 格式化行程資料
        val scheduleText = "$planName - ${String.format("%02d:%02d", startHour, startMinute)} - ${String.format("%02d:%02d", endHour, endMinute)}"

        // 創建新的 TextView 動態添加
        val newScheduleView = TextView(context).apply {
            text = scheduleText
            textSize = 16f
            setPadding(8, 8, 8, 8)
        }

        // 將 TextView 添加到行程列表容器中
        holder.scheduleListContainer.addView(newScheduleView)
    }

    inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.date_text)
        val addButton: FloatingActionButton = view.findViewById(R.id.add_button)
        val scheduleListContainer: LinearLayout = view.findViewById(R.id.schedule_list_container)
    }
}


