package com.example.project_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class FeedbackActivity : AppCompatActivity() {

    private lateinit var feedbackEditText: EditText
    private lateinit var previewRecyclerView: RecyclerView
    private val mediaList = mutableListOf<Media>() // 儲存媒體的資料
    private val db = FirebaseFirestore.getInstance()

    private val PICK_IMAGE_REQUEST = 1  // 請選擇圖像的請求碼

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        feedbackEditText = findViewById(R.id.feedback_edit_text)
        previewRecyclerView = findViewById(R.id.preview_recycler_view)

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
            submitFeedback()
        }
    }


    // 開啟相簿選擇圖片
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
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
        val media = Media(type, uri.toString())
        mediaList.add(media)
        // 更新 RecyclerView，這裡需要創建一個 RecyclerView Adapter 顯示媒體預覽
        // 目前僅作為添加操作，根據需求可擴充 RecyclerView 以顯示圖片預覽
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

        val feedbackData = hashMapOf(
            "text" to feedbackText,
            "media" to mediaList.map { it.toMap() },
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("schedules")
            .document(scheduleId)
            .collection("feedback")
            .add(feedbackData)
            .addOnSuccessListener {
                showToast("回饋已提交！")
                finish()
            }
            .addOnFailureListener {
                showToast("提交失敗：${it.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

data class Media(val type: String, val uri: String) {
    fun toMap(): Map<String, String> = mapOf("type" to type, "uri" to uri)
}
