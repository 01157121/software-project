import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.example.project_android.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;

class DatePageAdapter(
    private val context: Context,
    private val startDate: Date,
    private val endDate: Date,
    private val scheduleID: String
) : RecyclerView.Adapter<DatePageAdapter.DateViewHolder>() {

    private val adapterDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private val totalDays: Int;
    private val db = FirebaseFirestore.getInstance();
    private val planMap: MutableMap<String, MutableList<Plan>> = mutableMapOf();

    init {
        val calendar = Calendar.getInstance();
        calendar.time = startDate;
        totalDays = ((endDate.time - startDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_date_page, parent, false);
        return DateViewHolder(view);
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val currentDate = getDateForPosition(position);
        val dateKey = adapterDateFormat.format(currentDate);
        holder.dateTextView.text = dateKey;
        redrawSchedules(holder, dateKey);
        holder.addButton.setOnClickListener {
            showCreatePlanDialog(currentDate, holder, dateKey);
        };
    }

    override fun getItemCount(): Int = totalDays;

    private fun getDateForPosition(position: Int): Date {
        val calendar = Calendar.getInstance();
        calendar.time = startDate;
        calendar.add(Calendar.DAY_OF_YEAR, position);
        return calendar.time;
    }

    private fun showCreatePlanDialog(selectedDate: Date, holder: DateViewHolder, dateKey: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_plan, null);
        val dateTextView: TextView = dialogView.findViewById(R.id.show_date_text);
        val startTimeButton: Button = dialogView.findViewById(R.id.start_time_button);
        val endTimeButton: Button = dialogView.findViewById(R.id.end_time_button);
        val planNameInput: EditText = dialogView.findViewById(R.id.plan_name);
        dateTextView.text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(selectedDate);

        var startHour = 0;
        var startMinute = 0;
        var endHour = 0;
        var endMinute = 0;

        startTimeButton.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                startHour = hour;
                startMinute = minute;
                startTimeButton.text = String.format("%02d:%02d", hour, minute);
            };
        };

        endTimeButton.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                endHour = hour;
                endMinute = minute;
                endTimeButton.text = String.format("%02d:%02d", hour, minute);
            };
        };

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("新增行程")
            .setView(dialogView)
            .setPositiveButton("新增") { _, _ ->
                if (isValidTime(startHour, startMinute, endHour, endMinute)) {
                    val planName = planNameInput.text.toString();
                    val plan = Plan(planName, startHour, startMinute, endHour, endMinute, generateUniquePlanId());
                    addPlan(plan, holder, dateKey);
                } else {
                    showErrorDialog("時間錯誤", "開始時間必須早於結束時間，請重新選擇時間。");
                }
            }
            .setNegativeButton("取消", null)
            .create();

        dialog.show();
    }

    private fun addPlan(plan: Plan, holder: DateViewHolder, dateKey: String) {
        planMap.computeIfAbsent(dateKey) { mutableListOf() }.apply {
            if (any { it == plan }) {
                showToast("行程已存在，請勿重複新增");
                return;
            }
            add(plan);
            sortWith(compareBy({ it.startHour }, { it.startMinute }));
        };

        db.collection("schedules").document(scheduleID)
            .collection("dates").document(dateKey)
            .collection("plans").document(plan.planId)
            .set(plan)
            .addOnSuccessListener {
                showToast("行程已新增");
                redrawSchedules(holder, dateKey);
            }
            .addOnFailureListener {
                showToast("行程新增失敗：${it.message}");
            };
    }

    @SuppressLint("DefaultLocale")
    private fun redrawSchedules(holder: DateViewHolder, dateKey: String) {
        holder.scheduleListContainer.removeAllViews();
        planMap[dateKey]?.forEach { plan ->
            val view = LayoutInflater.from(context).inflate(R.layout.item_schedule, holder.scheduleListContainer, false);
            val nameTextView: TextView = view.findViewById(R.id.schedule_name);
            val timeTextView: TextView = view.findViewById(R.id.schedule_time);
            nameTextView.text = plan.planName;
            timeTextView.text = String.format("%02d:%02d - %02d:%02d", plan.startHour, plan.startMinute, plan.endHour, plan.endMinute);

            view.setOnClickListener {
                showEditDialog(plan, dateKey, holder);
            };

            holder.scheduleListContainer.addView(view);
        };
    }

    private fun showEditDialog(plan: Plan, dateKey: String, holder: DateViewHolder) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_schedule, null);
        val startHourPicker: NumberPicker = dialogView.findViewById(R.id.start_hour_picker);
        val startMinutePicker: NumberPicker = dialogView.findViewById(R.id.start_minute_picker);
        val endHourPicker: NumberPicker = dialogView.findViewById(R.id.end_hour_picker);
        val endMinutePicker: NumberPicker = dialogView.findViewById(R.id.end_minute_picker);
        val deleteButton:Button = dialogView.findViewById(R.id.delete_plan)
        startHourPicker.apply { minValue = 0; maxValue = 23; value = plan.startHour; };
        startMinutePicker.apply { minValue = 0; maxValue = 59; value = plan.startMinute; };
        endHourPicker.apply { minValue = 0; maxValue = 23; value = plan.endHour; };
        endMinutePicker.apply { minValue = 0; maxValue = 59; value = plan.endMinute; };

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("編輯行程時間")
            .setView(dialogView)
            .setPositiveButton("確定") { _, _ ->
                val updatedPlan = plan.copy(
                    startHour = startHourPicker.value,
                    startMinute = startMinutePicker.value,
                    endHour = endHourPicker.value,
                    endMinute = endMinutePicker.value
                );

                if (isValidTime(updatedPlan.startHour, updatedPlan.startMinute, updatedPlan.endHour, updatedPlan.endMinute)) {
                    updatePlan(updatedPlan, dateKey, holder);
                } else {
                    showErrorDialog("時間錯誤", "開始時間必須早於結束時間，請重新選擇時間。");
                }
            }
            .setNegativeButton("取消", null)
            .create();

        deleteButton.setOnClickListener {
            dialog.dismiss() // 關閉當前對話框
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("刪除行程")
                .setMessage("確定要刪除此行程嗎？")
                .setPositiveButton("確定") { _, _ ->
                    deletePlan(plan, dateKey, holder)
                }
                .setNegativeButton("取消", null)
                .create()
                .show()
        }
        dialog.show();
    }

    private fun deletePlan(plan: Plan, dateKey: String, holder: DateViewHolder) {
        val plans = planMap[dateKey]

        if (plans != null && plans.contains(plan)) {
            plans.remove(plan)

            // 更新資料庫
            db.collection("schedules").document(scheduleID)
                .collection("dates").document(dateKey)
                .collection("plans").document(plan.planId)
                .delete()
                .addOnSuccessListener {
                    showToast("行程已刪除")
                    redrawSchedules(holder, dateKey)
                }
                .addOnFailureListener { e ->
                    showToast("行程刪除失敗：${e.message}")
                }
        } else {
            showToast("找不到要刪除的行程")
        }
    }

    private fun updatePlan(plan: Plan, dateKey: String, holder: DateViewHolder) {
        planMap[dateKey]?.apply {
            removeIf { it.planId == plan.planId };
            add(plan);
            sortWith(compareBy({ it.startHour }, { it.startMinute }));
        };

        db.collection("schedules").document(scheduleID)
            .collection("dates").document(dateKey)
            .collection("plans").document(plan.planId)
            .set(plan)
            .addOnSuccessListener {
                showToast("行程已更新");
                redrawSchedules(holder, dateKey);
            }
            .addOnFailureListener {
                showToast("行程更新失敗：${it.message}");
            };
    }

    private fun showTimePickerDialog(onTimeSelected: (Int, Int) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null);
        val hourPicker: NumberPicker = dialogView.findViewById(R.id.hour_picker);
        val minutePicker: NumberPicker = dialogView.findViewById(R.id.minute_picker);

        hourPicker.minValue = 0;
        hourPicker.maxValue = 23;
        minutePicker.minValue = 0;
        minutePicker.maxValue = 59;

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("選擇時間")
            .setView(dialogView)
            .setPositiveButton("確定") { _, _ ->
                onTimeSelected(hourPicker.value, minutePicker.value);
            }
            .setNegativeButton("取消", null)
            .create();

        dialog.show();
    }

    private fun isValidTime(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): Boolean {
        return (startHour < endHour) || (startHour == endHour && startMinute < endMinute);
    }

    private fun showErrorDialog(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("確定", null)
            .create()
            .show();
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private fun generateUniquePlanId(): String = UUID.randomUUID().toString();

    inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.date_text);
        val addButton: FloatingActionButton = view.findViewById(R.id.add_button);
        val scheduleListContainer: LinearLayout = view.findViewById(R.id.schedule_list_container);
    }

    data class Plan(
        val planName: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val planId: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true;
            if (other !is Plan) return false;
            return planName == other.planName && startHour == other.startHour &&
                    startMinute == other.startMinute && endHour == other.endHour &&
                    endMinute == other.endMinute;
        }

        override fun hashCode(): Int {
            return Objects.hash(planName, startHour, startMinute, endHour, endMinute);
        }
    }
}
