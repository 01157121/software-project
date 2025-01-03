package com.example.project_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore


class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        firestore = Firebase.firestore
        val usernameInput = findViewById<EditText>(R.id.username_input)
        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirm_password_input)
        val registerButton = findViewById<Button>(R.id.register_button)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "請填寫所有欄位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "密碼不一致，請重新輸入", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            registerUser(username, email, password)
        }
        val cancelButton = findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                val userId = user.uid
                                saveUserToDatabase(userId, username, email)
                                Toast.makeText(
                                    this,
                                    "註冊成功！驗證郵件已發送，請檢查您的信箱。",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Log.e("RegisterActivity", "Email verification failed: ${emailTask.exception}")
                                Toast.makeText(
                                    this,
                                    "無法發送驗證郵件: ${emailTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    Log.e("RegisterActivity", "Registration failed: ${task.exception}")
                    Toast.makeText(
                        this,
                        "註冊失敗: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }



    private fun saveUserToDatabase(userId: String, username: String, email: String) {
        val userData = hashMapOf(
            "username" to username,
            "email" to email
        )

        // 儲存用戶數據到 Firestore
        firestore.collection("users").document(userId)
            .set(userData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "註冊成功！", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish() // 可選：結束當前活動
                } else {
                    Log.e("RegisterActivity", "Firestore error: ${task.exception}")
                    Toast.makeText(
                        this,
                        "儲存用戶數據失敗: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
