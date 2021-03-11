package com.sizeadviser.sizeadviser

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DatabaseHandler(context: Context,
                              factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME,
        factory, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE gender_info (user_uuid TEXT, gender INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS gender_info")
        onCreate(db)
    }

    fun createGITable() {
        this.writableDatabase.execSQL("CREATE TABLE gender_info (user_uuid TEXT, gender INTEGER)")
    }

    fun addGenderInfo(uuid: String, genderInt: Int) {
        val values = ContentValues()
        values.put("user_uuid", uuid)
        values.put("gender", genderInt)
        val db = this.writableDatabase
        db.insert("gender_info", null, values)
        db.close()
    }

    fun getGenderIntOf(uuid: String): Int {
        val db = this.readableDatabase
        val query = db.rawQuery("SELECT gender FROM gender_info WHERE user_uuid='$uuid'", null)
        return try {
            query!!.moveToFirst()
            query.getInt(query.getColumnIndex("gender"))
        } catch (e: android.database.CursorIndexOutOfBoundsException) {
            -1
        }
    }

    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "sizeAdviser.db"
    }
}