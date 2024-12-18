package com.example.project_android.models

data class User(
    val id: String,
    val name: String,
    val email: String,
    val isHost: Boolean // 是否為主辦人
)
