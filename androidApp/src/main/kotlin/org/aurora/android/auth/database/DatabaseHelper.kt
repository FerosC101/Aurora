package org.aurora.android.auth.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "aurora.db"
        private const val DATABASE_VERSION = 1

        // Users table
        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_FULL_NAME = "full_name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD_HASH = "password_hash"
        const val COLUMN_CREATED_AT = "created_at"

        // Sessions table
        const val TABLE_SESSIONS = "sessions"
        const val COLUMN_SESSION_ID = "session_id"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_EXPIRES_AT = "expires_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_FULL_NAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD_HASH TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """)

        // Create sessions table
        db.execSQL("""
            CREATE TABLE $TABLE_SESSIONS (
                $COLUMN_SESSION_ID TEXT PRIMARY KEY,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_EXPIRES_AT INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID) ON DELETE CASCADE
            )
        """)

        // Create index on email for faster lookups
        db.execSQL("CREATE INDEX idx_email ON $TABLE_USERS($COLUMN_EMAIL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }
}
