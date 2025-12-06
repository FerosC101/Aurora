package org.aurora.auth.service

import org.aurora.auth.model.User
import org.aurora.auth.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

class AuthService(private val userRepository: UserRepository = UserRepository()) {
    
    fun register(fullName: String, email: String, password: String): Result<User> {
        // Validate inputs
        if (fullName.isBlank()) {
            return Result.failure(Exception("Full name cannot be empty"))
        }
        
        if (!isValidEmail(email)) {
            return Result.failure(Exception("Invalid email format"))
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
        // Validate inputs
        if (email.isBlank()) {
            return Result.failure(Exception("Email cannot be empty"))
        }
        
        if (password.isBlank()) {
            return Result.failure(Exception("Password cannot be empty"))
        }
        
        // Find user
        val user = userRepository.findByEmail(email)
            ?: return Result.failure(Exception("Invalid email or password"))
        
        // Verify password
        if (!BCrypt.checkpw(password, user.passwordHash)) {
            return Result.failure(Exception("Invalid email or password"))
        }
        
        return Result.success(user)
    }
    
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
}
