package org.aurora.android.auth.service

import android.content.Context
import org.aurora.android.auth.model.User
import org.aurora.android.auth.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

class AuthService(context: Context) {
    private val userRepository = UserRepository(context)

    fun register(fullName: String, email: String, password: String): Result<User> {
        // Validate input
        if (fullName.isBlank()) {
            return Result.failure(Exception("Full name is required"))
        }

        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(Exception("Valid email is required"))
        }

        if (password.length < 8) {
            return Result.failure(Exception("Password must be at least 8 characters"))
        }

        // Check if user already exists
        if (userRepository.findByEmail(email) != null) {
            return Result.failure(Exception("Email already registered"))
        }

        // Hash password
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())

        // Create user
        return userRepository.createUser(fullName, email, passwordHash)
    }

    fun login(email: String, password: String): Result<User> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Email and password are required"))
        }

        val user = userRepository.findByEmail(email)
            ?: return Result.failure(Exception("Invalid email or password"))

        if (!BCrypt.checkpw(password, user.passwordHash)) {
            return Result.failure(Exception("Invalid email or password"))
        }

        return Result.success(user)
    }
}
