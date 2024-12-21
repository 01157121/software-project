package com.example.project_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AlertDialog
data class MediaItem(
    val type: String,
    val uri: String
)
class FeedbackActivity : AppCompatActivity() {
    private val mediaList = mutableListOf<MediaItem>()// 儲存媒體的資料
    private lateinit var feedbackEditText: EditText
    private val db = FirebaseFirestore.getInstance()
    private lateinit var scheduleId: String
    private val PICK_IMAGE_REQUEST = 1  // 請選擇圖像的請求碼
    private var feedbackDialog: androidx.appcompat.app.AlertDialog? = null
    private lateinit var previewRecyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: throw IllegalArgumentException("SCHEDULE_ID is required")
        feedbackEditText = findViewById(R.id.feedback_edit_text)

        // 設置 RecyclerView 的布局管理器
        previewRecyclerView = findViewById(R.id.preview_recycler_view)
        previewRecyclerView.layoutManager = LinearLayoutManager(this)

        // 設置 RecyclerView 的適配器
//        previewRecyclerView.adapter = MediaAdapter(mediaList)

        // 返回按鈕
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }

        // 加入圖片、影片按鈕
        findViewById<ImageButton>(R.id.add_image_button).setOnClickListener {
            openImagePicker()
        }
        findViewById<ImageButton>(R.id.add_video_button).setOnClickListener {
            addMediaVideo("video")
        }

        // 送出回饋按鈕
        findViewById<FloatingActionButton>(R.id.submit_feedback_button).setOnClickListener {
            submitFeedback(scheduleId)
        }

        // 查看回饋按鈕
        findViewById<FloatingActionButton>(R.id.view_feedback_button).setOnClickListener {
            loadFeedbackList(scheduleId)
        }
    }

    private fun loadFeedbackList(scheduleId: String) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("feedback")
            .get()
            .addOnSuccessListener { querySnapshot ->
                // 使用 mapNotNull 正確地轉換成 List<Map<String, Any>>
                val feedbackList: List<Map<String, Any>> = querySnapshot.documents.mapNotNull { document ->
                    val feedbackId = document.getString("feedbackId") ?: return@mapNotNull null
                    val text = document.getString("text") ?: "未知回饋"
                    val timestamp = document.getLong("timestamp") ?: 0L
                    mapOf(
                        "feedbackId" to feedbackId,
                        "text" to text,
                        "timestamp" to timestamp
                    ) // 這裡返回 Map<String, Any>
                }

                // 顯示對話框
                showFeedbackDialog(feedbackList)
            }
            .addOnFailureListener { e ->
                showToast("無法加載回饋紀錄: ${e.message}")
            }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun submitFeedback(scheduleId:String) {
        val feedbackText = feedbackEditText.text.toString()

        if (feedbackText.isBlank()) {
            showToast("請輸入回饋內容")
            return
        }

        val feedbackId = generateUniqueFeedbackId()
        val feedbackData = hashMapOf(
            "feedbackId" to feedbackId,
            "text" to feedbackText,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("schedules")
            .document(scheduleId)
            .collection("feedback")
            .document(feedbackId)
            .set(feedbackData)
            .addOnSuccessListener {
                showToast("回饋已提交！")
                finish()
            }
            .addOnFailureListener {
                showToast("提交失敗：${it.message}")
            }
    }
    // 處理選擇圖片後的結果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            data?.data?.let { uri ->
                addMedia("image", uri)
            }
        }
    }

    // 添加媒體到 mediaList 並更新 RecyclerView
    private fun addMedia(type: String, uri: Uri) {
        val media =  MediaItem(type, uri.toString())
        mediaList.add(media)
        // 更新 RecyclerView，這裡需要創建一個 RecyclerView Adapter 顯示媒體預覽
        // 目前僅作為添加操作，根據需求可擴充 RecyclerView 以顯示圖片預覽

        // 顯示圖片到 image_preview 上
//        val imagePreview = findViewById<ImageView>(R.id.image_preview)
//        imagePreview.visibility = View.VISIBLE  // 顯示 ImageView
//        Glide.with(this)
//            .load(uri)
//            .into(imagePreview)

        // 顯示圖片到 image_preview 上
        val imagePreview = findViewById<ImageView>(R.id.image_preview)
        val imagePreview2 = findViewById<ImageView>(R.id.image_preview_2)
        val imagePreview3 = findViewById<ImageView>(R.id.image_preview_3)
        val imagePreview4 = findViewById<ImageView>(R.id.image_preview_4)
        val imagePreview5 = findViewById<ImageView>(R.id.image_preview_5)
        val imagePreview6 = findViewById<ImageView>(R.id.image_preview_6)


        if (imagePreview.visibility == View.GONE) {
            imagePreview.visibility = View.VISIBLE  // 顯示第一張圖片
            Glide.with(this)
                .load(uri)
                .into(imagePreview)
        } else if (imagePreview2.visibility == View.GONE) {
            imagePreview2.visibility = View.VISIBLE  // 顯示第二張圖片
            Glide.with(this)
                .load(uri)
                .into(imagePreview2)
        } else if (imagePreview3.visibility == View.GONE) {
            imagePreview3.visibility = View.VISIBLE  // 顯示第三張圖片
            Glide.with(this)
                .load(uri)
                .into(imagePreview3)
        } else if (imagePreview4.visibility == View.GONE) {
            imagePreview4.visibility = View.VISIBLE  // 顯示第四張圖片
            Glide.with(this)
                .load(uri)
                .into(imagePreview4)
        } else if (imagePreview5.visibility == View.GONE) {
            imagePreview5.visibility = View.VISIBLE  // 顯示第五張圖片
            Glide.with(this)
                .load(uri)
                .into(imagePreview5)
        } else if (imagePreview6.visibility == View.GONE) {
            imagePreview6.visibility = View.VISIBLE  // 顯示第六張圖片
            Glide.with(this)
                .load(uri)
                .into(imagePreview6)
        }

        // 通知適配器更新 RecyclerView
        previewRecyclerView.adapter?.notifyDataSetChanged()
        showToast("圖片已添加")
    }




    private fun addMediaVideo(type: String) {
        // 顯示文件選擇器或相機
        // 這裡你可以用 Android API 或第三方庫處理
        showToast("$type 功能未實現")
    }

    private fun submitFeedback() {
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: return
        val feedbackText = feedbackEditText.text.toString()

        if (feedbackText.isBlank()) {
            showToast("請輸入回饋內容")
            return
        }

        val feedbackId = generateUniqueFeedbackId()
        val feedbackData = hashMapOf(
            "feedbackId" to feedbackId,
            "text" to feedbackText,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("schedules")
            .document(scheduleId)
            .collection("feedback")
            .document(feedbackId)
            .set(feedbackData)
            .addOnSuccessListener {
                showToast("回饋已提交！")
                finish()
            }
            .addOnFailureListener {
                showToast("提交失敗：${it.message}")
            }
    }

    private fun showFeedbackDialog(feedbackList: List<Map<String, Any>>) {
        // 如果舊對話框存在，先關閉
        feedbackDialog?.dismiss()
        feedbackDialog = null

        if (feedbackList.isEmpty()) {
            showToast("沒有找到任何回饋紀錄")
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback_results, null)
        val feedbackContainer = dialogView.findViewById<LinearLayout>(R.id.feedback_results_container)
        val closeButton = dialogView.findViewById<Button>(R.id.close_button)

        feedbackList.forEach { feedback ->
            val feedbackCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.item_background)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params
            }

            val texts = feedback["text"] as? String ?: "未知回饋"
            val timestamp = feedback["timestamp"] as? Long ?: 0L
            val formattedTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(timestamp)

            val textView = TextView(this).apply {
                text = "回饋內容: $texts\n時間: $formattedTime"
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }
            feedbackCard.addView(textView)

            // 長按事件刪除回饋
            feedbackCard.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("刪除回饋紀錄")
                    .setMessage("確定要刪除此回饋紀錄嗎？")
                    .setPositiveButton("刪除") { _, _ ->
                        val feedbackId = feedback["feedbackId"] as? String ?: return@setPositiveButton
                        deleteFeedbackFromFirestore(scheduleId, feedbackId)
                    }
                    .setNegativeButton("取消", null)
                    .show()
                true
            }

            feedbackContainer.addView(feedbackCard)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        feedbackDialog = dialog // 將新的對話框存儲起來
    }

    private fun deleteFeedbackFromFirestore(scheduleId: String, feedbackId: String) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("feedback")
            .document(feedbackId)
            .delete()
            .addOnSuccessListener {
                showToast("回饋紀錄已刪除")
                loadFeedbackList(scheduleId) // 刪除後重新加載
            }
            .addOnFailureListener { e ->
                showToast("刪除回饋紀錄失敗: ${e.message}")
            }
    }

    private fun generateUniqueFeedbackId(): String {
        return java.util.UUID.randomUUID().toString()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}