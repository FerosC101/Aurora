package org.aurora.auth.repository

import org.aurora.auth.model.User
import org.aurora.database.Database
import java.sql.SQLException

class UserRepository {
    
    fun createUser(fullName: String, email: String, passwordHash: String): Result<User> {
        return try {
            val conn = Database.getConnection()
            val statement = conn.prepareStatement(
                "INSERT INTO users (full_name, email, password_hash, created_at) VALUES (?, ?, ?, ?)"
            )
            
            val createdAt = System.currentTimeMillis()
            statement.setString(1, fullName)
            statement.setString(2, email)
            statement.setString(3, passwordHash)
            statement.setLong(4, createdAt)
            
            val rowsAffected = statement.executeUpdate()
            statement.close()
            
            if (rowsAffected > 0) {
                // Get the last inserted ID
                val idStatement = conn.prepareStatement("SELECT last_insert_rowid()")
                val resultSet = idStatement.executeQuery()
                val userId = if (resultSet.next()) resultSet.getLong(1) else 0L
                resultSet.close()
                idStatement.close()
                
                Result.success(User(
                    id = userId,
                    fullName = fullName,
                    email = email,
                    passwordHash = passwordHash,
                    createdAt = createdAt
                ))
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: SQLException) {
            if (e.message?.contains("UNIQUE constraint failed") == true) {
                Result.failure(Exception("Email already exists"))
            } else {
                Result.failure(e)
            }
        }
    }
    
    fun findByEmail(email: String): User? {
        return try {
            val conn = Database.getConnection()
            val statement = conn.prepareStatement(
                "SELECT id, full_name, email, password_hash, created_at FROM users WHERE email = ?"
            )
            statement.setString(1, email)
            
            val resultSet = statement.executeQuery()
            
            if (resultSet.next()) {
                val user = User(
                    id = resultSet.getLong("id"),
                    fullName = resultSet.getString("full_name"),
                    email = resultSet.getString("email"),
                    passwordHash = resultSet.getString("password_hash"),
                    createdAt = resultSet.getLong("created_at")
                )
                resultSet.close()
                statement.close()
                user
            } else {
                resultSet.close()
                statement.close()
                null
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            null
        }
    }
    
    fun findById(id: Long): User? {
        return try {
            val conn = Database.getConnection()
            val statement = conn.prepareStatement(
                "SELECT id, full_name, email, password_hash, created_at FROM users WHERE id = ?"
            )
            statement.setLong(1, id)
            
            val resultSet = statement.executeQuery()
            
            if (resultSet.next()) {
                val user = User(
                    id = resultSet.getLong("id"),
                    fullName = resultSet.getString("full_name"),
                    email = resultSet.getString("email"),
                    passwordHash = resultSet.getString("password_hash"),
                    createdAt = resultSet.getLong("created_at")
                )
                resultSet.close()
                statement.close()
                user
            } else {
                resultSet.close()
                statement.close()
                null
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            null
        }
    }
    
    fun deleteUser(id: Long): Boolean {
        return try {
            val conn = Database.getConnection()
            val statement = conn.prepareStatement("DELETE FROM users WHERE id = ?")
            statement.setLong(1, id)
            
            val rowsAffected = statement.executeUpdate()
            statement.close()
            rowsAffected > 0
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }
}
