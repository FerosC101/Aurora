package org.aurora.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object Database {
    private const val DB_URL = "jdbc:sqlite:aurora.db"
    private var connection: Connection? = null
    
    fun getConnection(): Connection {
        if (connection == null || connection?.isClosed == true) {
            connection = DriverManager.getConnection(DB_URL)
            initializeTables()
        }
        return connection!!
    }
    
    private fun initializeTables() {
        val conn = connection ?: return
        
        try {
            val statement = conn.createStatement()
            
            // Create users table
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create sessions table for tracking logged-in users
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    token TEXT UNIQUE NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """.trimIndent())
            
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
    
    fun close() {
        try {
            connection?.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}
