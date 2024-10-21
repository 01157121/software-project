package com.example.project_android

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val loginHintText = findViewById<TextView>(R.id.login_hint)
        val mainLayout=findViewById<LinearLayout>(R.id.main_layout)
        val animator = ObjectAnimator.ofFloat(loginHintText, "alpha", 1f, 0f)
        animator.duration = 1000
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.repeatMode = ObjectAnimator.REVERSE
        animator.start()


        mainLayout.setOnClickListener {
            val intent = Intent(this, PlanningActivity::class.java)
            startActivity(intent)
        }
    }
}