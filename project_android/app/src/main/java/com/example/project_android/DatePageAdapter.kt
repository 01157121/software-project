import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.project_android.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
data class Schedule(
    val planName: String,
    var startHour: Int,
    var startMinute: Int,
    var endHour: Int,
    var endMinute: Int
)
class DatePageAdapter(
    private val context: Context,
    private val startDate: Date,
    private val endDate: Date,
    private val schedules: MutableList<Schedule> = mutableListOf()
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
        val show_current_date_textview: TextView = dialogView.findViewById(R.id.show_date_text)

        val startTimeButton: Button = dialogView.findViewById(R.id.start_time_button)
        val endTimeButton: Button = dialogView.findViewById(R.id.end_time_button)
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        var startHour = 0
        var startMinute = 0
        var endHour = 0
        var endMinute = 0
        show_current_date_textview.text = dateFormat.format(selectedDate)
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
                val isValidTime = (startHour < endHour) || (startHour == endHour && startMinute < endMinute)
                if (!isValidTime) {
                    // 如果開始時間不早於結束時間，顯示錯誤訊息
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("時間錯誤")
                        .setMessage("開始時間必須早於結束時間，請重新選擇時間。")
                        .setPositiveButton("確定", null)
                        .create()
                        .show()
                }
                else{
                    val planName = dialogView.findViewById<EditText>(R.id.plan_name).text.toString()
                    addScheduleToContainer(planName, startHour, startMinute, endHour, endMinute, holder)
                }
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }
    private fun showTimePickerDialog(onTimeSelected: (Int, Int) -> Unit) {
        // 使用自定義的布局
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null)

        // 獲取滾輪選擇器
        val hourPicker: NumberPicker = dialogView.findViewById(R.id.hour_picker)
        val minutePicker: NumberPicker = dialogView.findViewById(R.id.minute_picker)

        // 設置滾輪範圍
        hourPicker.minValue = 0
        hourPicker.maxValue = 23
        minutePicker.minValue = 0
        minutePicker.maxValue = 59

        // 初始化當前時間
        val calendar = Calendar.getInstance()
        hourPicker.value = calendar.get(Calendar.HOUR_OF_DAY)
        minutePicker.value = calendar.get(Calendar.MINUTE)

        // 顯示自定義對話框
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("選擇時間")
            .setView(dialogView)
            .setPositiveButton("確定") { _, _ ->
                val selectedHour = hourPicker.value
                val selectedMinute = minutePicker.value
                onTimeSelected(selectedHour, selectedMinute)
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }

    private fun addScheduleToContainer(
        planName: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        holder: DateViewHolder
    ) {
        // 新增行程到清單
        val newSchedule = Schedule(planName, startHour, startMinute, endHour, endMinute)
        schedules.add(newSchedule)

        // 按開始時間排序
        schedules.sortWith(compareBy({ it.startHour }, { it.startMinute }))

        // 重新繪製行程列表
        redrawSchedules(holder)
    }

    @SuppressLint("DefaultLocale")
    private fun redrawSchedules(holder: DateViewHolder) {
        // 清空容器
        holder.scheduleListContainer.removeAllViews()

        // 根據排序後的行程重新繪製
        for (schedule in schedules) {
            val scheduleView = LayoutInflater.from(context).inflate(R.layout.item_schedule, holder.scheduleListContainer, false)

            val scheduleNameTextView: TextView = scheduleView.findViewById(R.id.schedule_name)
            val scheduleTimeTextView: TextView = scheduleView.findViewById(R.id.schedule_time)
            val scheduleContainer: View = scheduleView.findViewById(R.id.schedule_container)

            scheduleNameTextView.text = schedule.planName
            scheduleTimeTextView.text = String.format(
                "%02d:%02d - %02d:%02d",
                schedule.startHour,
                schedule.startMinute,
                schedule.endHour,
                schedule.endMinute
            )

            scheduleContainer.setOnClickListener {
                showEditTimeDialog(
                    schedule.startHour, schedule.startMinute,
                    schedule.endHour, schedule.endMinute
                ) { newStartHour, newStartMinute, newEndHour, newEndMinute ->
                    schedule.startHour = newStartHour
                    schedule.startMinute = newStartMinute
                    schedule.endHour = newEndHour
                    schedule.endMinute = newEndMinute

                    // 重新排序和繪製
                    schedules.sortWith(compareBy({ it.startHour }, { it.startMinute }))
                    redrawSchedules(holder)
                }
            }

            holder.scheduleListContainer.addView(scheduleView)
        }
    }

    private fun showEditTimeDialog(
        currentStartHour: Int,
        currentStartMinute: Int,
        currentEndHour: Int,
        currentEndMinute: Int,
        onTimeUpdated: (Int, Int, Int, Int) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_time_picker, null)
        val startHourPicker: NumberPicker = dialogView.findViewById(R.id.start_hour_picker)
        val startMinutePicker: NumberPicker = dialogView.findViewById(R.id.start_minute_picker)
        val endHourPicker: NumberPicker = dialogView.findViewById(R.id.end_hour_picker)
        val endMinutePicker: NumberPicker = dialogView.findViewById(R.id.end_minute_picker)

        startHourPicker.minValue = 0
        startHourPicker.maxValue = 23
        startHourPicker.value = currentStartHour

        startMinutePicker.minValue = 0
        startMinutePicker.maxValue = 59
        startMinutePicker.value = currentStartMinute

        endHourPicker.minValue = 0
        endHourPicker.maxValue = 23
        endHourPicker.value = currentEndHour

        endMinutePicker.minValue = 0
        endMinutePicker.maxValue = 59
        endMinutePicker.value = currentEndMinute

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("編輯行程時間")
            .setView(dialogView)
            .setPositiveButton("確定") { _, _ ->

                val newStartHour = startHourPicker.value
                val newStartMinute = startMinutePicker.value
                val newEndHour = endHourPicker.value
                val newEndMinute = endMinutePicker.value
                val isValidTime = (newStartHour < newEndHour) || (newStartHour == newEndHour && newStartMinute < newEndMinute)
                if (!isValidTime) {
                    // 如果開始時間不早於結束時間，顯示錯誤訊息
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("時間錯誤")
                        .setMessage("開始時間必須早於結束時間，請重新選擇時間。")
                        .setPositiveButton("確定", null)
                        .create()
                        .show()
                }
                else{
                    onTimeUpdated(newStartHour, newStartMinute, newEndHour, newEndMinute)
                }
            }
            .setNegativeButton("取消", null)
            .create()
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.date_text)
        val addButton: FloatingActionButton = view.findViewById(R.id.add_button)
        val scheduleListContainer: LinearLayout = view.findViewById(R.id.schedule_list_container)
    }
}



