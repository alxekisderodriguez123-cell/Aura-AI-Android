
package com.aura.ai.chat

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AuraChatDatabase(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION
    ) {

    companion object {
        private const val DATABASE_NAME = "aura_conversations.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_MESSAGES = "messages"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TEXT = "message_text"
        private const val COLUMN_IS_USER = "is_user"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_IS_USER INTEGER NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Las migraciones futuras se implementarán aquí.
    }

    fun insertMessage(message: ChatMessage): ChatMessage {
        val values = ContentValues().apply {
            put(COLUMN_TEXT, message.text)
            put(COLUMN_IS_USER, if (message.isUser) 1 else 0)
            put(COLUMN_TIMESTAMP, message.timestamp)
        }

        val id = writableDatabase.insertOrThrow(
            TABLE_MESSAGES,
            null,
            values
        )

        return message.copy(id = id)
    }

    fun getMessages(): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        readableDatabase.query(
            TABLE_MESSAGES,
            arrayOf(
                COLUMN_ID,
                COLUMN_TEXT,
                COLUMN_IS_USER,
                COLUMN_TIMESTAMP
            ),
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP ASC, $COLUMN_ID ASC"
        ).use { cursor ->

            while (cursor.moveToNext()) {
                messages.add(
                    ChatMessage(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow(COLUMN_ID)
                        ),
                        text = cursor.getString(
                            cursor.getColumnIndexOrThrow(COLUMN_TEXT)
                        ),
                        isUser = cursor.getInt(
                            cursor.getColumnIndexOrThrow(COLUMN_IS_USER)
                        ) == 1,
                        timestamp = cursor.getLong(
                            cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
                        )
                    )
                )
            }
        }

        return messages
    }

    fun deleteAllMessages() {
        writableDatabase.delete(
            TABLE_MESSAGES,
            null,
            null
        )
    }
    }
    
