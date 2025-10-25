package com.example.linknpark.data

import android.util.Log
import com.example.linknpark.model.User
import com.example.linknpark.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository : AuthRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    
    private var currentUser: User? = null
    
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            Log.d("FirebaseAuth", "Attempting login for email: $email")
            
            // Query Firestore for user with matching email
            val querySnapshot = usersCollection
                .whereEqualTo("email", email)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                Log.w("FirebaseAuth", "No user found with email: $email")
                return Result.failure(Exception("User not found"))
            }
            
            // Get the first matching document
            val userDoc = querySnapshot.documents.first()
            val userData = userDoc.data
            
            if (userData == null) {
                Log.e("FirebaseAuth", "User document is null")
                return Result.failure(Exception("Invalid user data"))
            }
            
            // Extract user data
            val storedPassword = userData["password"] as? String ?: ""
            val userName = userData["name"] as? String ?: ""
            val userRole = userData["role"] as? String ?: "DRIVER"
            
            Log.d("FirebaseAuth", "Found user: $userName with role: $userRole")
            
            // Verify password (Note: This is not secure - in production use Firebase Auth)
            if (storedPassword != password) {
                Log.w("FirebaseAuth", "Password mismatch")
                return Result.failure(Exception("Invalid password"))
            }
            
            // Create User object
            val user = User(
                uid = userDoc.id,
                email = email,
                name = userName,
                password = storedPassword,
                role = UserRole.fromString(userRole)
            )
            
            currentUser = user
            Log.d("FirebaseAuth", "Login successful for: ${user.name}")
            
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Login failed", e)
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }
    
    override suspend fun getCurrentUser(): User? {
        return currentUser
    }

    fun getCurrentUserSync(): User? {
        return currentUser
    }
    
    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<User> {
        return try {
            Log.d("FirebaseAuth", "Attempting registration for email: $email")
            
            // Check if user already exists
            val existingUser = usersCollection
                .whereEqualTo("email", email)
                .get()
                .await()
            
            if (!existingUser.isEmpty) {
                Log.w("FirebaseAuth", "User already exists with email: $email")
                return Result.failure(Exception("User with this email already exists"))
            }
            
            // Create new user document
            val userData = hashMapOf(
                "name" to name,
                "email" to email,
                "password" to password, // Note: Not secure for production
                "role" to role.name
            )
            
            // Add user to Firestore
            val documentReference = usersCollection.add(userData).await()
            Log.d("FirebaseAuth", "User registered with ID: ${documentReference.id}")
            
            // Create User object
            val user = User(
                uid = documentReference.id,
                email = email,
                name = name,
                password = password,
                role = role
            )
            
            currentUser = user
            Log.d("FirebaseAuth", "Registration successful for: ${user.name}")
            
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Registration failed", e)
            Result.failure(Exception("Registration failed: ${e.message}"))
        }
    }
    
    override suspend fun logout() {
        currentUser = null
        Log.d("FirebaseAuth", "User logged out")
    }
}

