package com.example.linknpark.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val password: String = "", // Note: In production, never store passwords in plain text
    val role: UserRole = UserRole.DRIVER
)

enum class UserRole {
    DRIVER,
    STAFF;

    companion object {
        fun fromString(value: String): UserRole {
            return when (value.uppercase()) {
                "DRIVER" -> DRIVER
                "STAFF" -> STAFF
                else -> DRIVER
            }
        }
    }
}