package com.example.project_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

        // 設置 RecyclerView 的布局管理器
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
//        findViewById<ImageButton>(R.id.add_video_button).setOnClickListener {
//            addMediaVideo("video")
//        }

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




//    private fun addMediaVideo(type: String) {
//        // 顯示文件選擇器或相機
//        // 這裡你可以用 Android API 或第三方庫處理
//        showToast("$type 功能未實現")
//    }

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
