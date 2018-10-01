package org.mitre.fluxnotes.data.session

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = arrayOf(Session::class), version = 1)
abstract class SessionDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        private var INSTANCE: SessionDatabase? = null

        fun getInstance(context: Context): SessionDatabase? {
            if (INSTANCE == null) {
                synchronized(SessionDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                            SessionDatabase::class.java,
                            "session_database.db")
                            .build()
                }
            }
            return INSTANCE
        }
    }
}