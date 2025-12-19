package com.nextcs.aurora.auth.repository

import android.content.ContentValues
import android.content.Context
import com.nextcs.aurora.auth.database.DatabaseHelper
import com.nextcs.aurora.auth.model.User
import java.util.UUID

class UserRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun createUser(fullName: String, email: String, passwordHash: String): Result<User> {
        return try {
            val db = dbHelper.writableDatabase
            val id = UUID.randomUUID().toString()
            val createdAt = System.currentTimeMillis()

            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_ID, id)
                put(DatabaseHelper.COLUMN_FULL_NAME, fullName)
                put(DatabaseHelper.COLUMN_EMAIL, email)
                put(DatabaseHelper.COLUMN_PASSWORD_HASH, passwordHash)
                put(DatabaseHelper.COLUMN_CREATED_AT, createdAt)
            }

            val result = db.insert(DatabaseHelper.TABLE_USERS, null, values)
            if (result != -1L) {
                Result.success(User(id, fullName, email, passwordHash, createdAt))
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun findByEmail(email: String): User? {
        return try {
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                "${DatabaseHelper.COLUMN_EMAIL} = ?",
                arrayOf(email),
                null,
                null,
                null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    User(
                        id = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                        fullName = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FULL_NAME)),
                        email = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL)),
                        passwordHash = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD_HASH)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT))
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun findById(id: String): User? {
        return try {
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                "${DatabaseHelper.COLUMN_ID} = ?",
                arrayOf(id),
                null,
                null,
                null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    User(
                        id = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                        fullName = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FULL_NAME)),
                        email = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL)),
                        passwordHash = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD_HASH)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT))
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
