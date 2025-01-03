package com.example.project_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
class LoginActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db= Firebase.firestore
        val emailEditText = findViewById<EditText>(R.id.email)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login_button)
        val registerButton = findViewById<Button>(R.id.register_button)
        val googleSignInButton = findViewById<com.google.android.gms.common.SignInButton>(R.id.googleSignInButton)
        val users = mutableMapOf<String, String>()

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null && !user.isEmailVerified) {
                                // 提示用戶驗證電子郵件
                                Toast.makeText(this, "您的信箱尚未驗證，請檢查郵件並完成驗證。", Toast.LENGTH_SHORT).show()

                                // 提供重新發送驗證郵件的選項
                                resendVerificationEmail(user)

                                // 登出未驗證的用戶
                                auth.signOut()
                            } else {
                                Toast.makeText(this, "登入成功", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, LobbyActivity::class.java)
                                startActivity(intent)
                                finish() // 關閉當前活動
                            }
                        } else {
                            Toast.makeText(this, "登入失敗: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "請填寫空格", Toast.LENGTH_SHORT).show()
            }
        }



        registerButton.setOnClickListener {
            // Navigate to RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        //google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Google Sign-In Button Click Listener
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }
    // 提供重新發送驗證郵件的功能
    private fun resendVerificationEmail(user: FirebaseUser) {
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "驗證郵件已重新發送，請檢查您的信箱。", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "無法發送驗證郵件: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign-in failed", e)
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "歡迎, ${user?.displayName}!", Toast.LENGTH_SHORT).show()
                    saveUserToDatabase(user?.uid!!, user?.displayName!!, user?.email!!)
                    val intent = Intent(this, LobbyActivity::class.java)
                    startActivity(intent)
                } else {
                    Log.w("GoogleSignIn", "Firebase authentication failed", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

//    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
//        if (account == null) {
//            Toast.makeText(this, "Google account is null", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
//        auth.signInWithCredential(credential)
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) {
//                    val user = auth.currentUser
//                    user?.let {
//                        saveUserToDatabase(it.uid, it.displayName ?: "Unknown", it.email ?: "No Email")
//                        val intent = Intent(this, LobbyActivity::class.java).apply {
//                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                        }
//                        startActivity(intent)
//                        finish()
//                    } ?: run {
//                        Toast.makeText(this, "User is null after authentication", Toast.LENGTH_SHORT).show()
//                    }
//                } else {
//                    Log.w("GoogleSignIn", "Firebase authentication failed: ${task.exception?.message}", task.exception)
//                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
//                }
//            }
//    }


    private fun saveUserToDatabase(userId: String, username: String, email: String) {
        val userData = hashMapOf(
            "username" to username,
            "email" to email
        )

        // 儲存用戶數據到 Firestore
        db.collection("users").document(userId)
            .set(userData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "註冊成功！", Toast.LENGTH_SHORT).show()
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



