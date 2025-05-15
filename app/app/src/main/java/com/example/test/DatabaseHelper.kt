package com.example.test

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "WeatherApp.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_FAVORITES = "favorites"
        private const val COLUMN_ID = "id"
        private const val COLUMN_CITY = "city"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_FAVORITES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CITY TEXT UNIQUE
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        onCreate(db)
    }

    fun addFavorite(city: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CITY, city)
        }
        val result = db.insertWithOnConflict(TABLE_FAVORITES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        db.close()
        return result != -1L
    }

    fun removeFavorite(city: String): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_FAVORITES, "$COLUMN_CITY = ?", arrayOf(city))
        db.close()
        return result > 0
    }

    fun getAllFavorites(): List<String> {
        val favorites = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.query(TABLE_FAVORITES, arrayOf(COLUMN_CITY), null, null, null, null, null)
        while (cursor.moveToNext()) {
            favorites.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CITY)))
        }
        cursor.close()
        db.close()
        return favorites
    }

    fun isFavorite(city: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_FAVORITES, arrayOf(COLUMN_CITY), "$COLUMN_CITY = ?", arrayOf(city), null, null, null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }
}