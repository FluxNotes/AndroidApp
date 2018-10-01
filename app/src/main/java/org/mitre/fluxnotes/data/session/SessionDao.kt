package org.mitre.fluxnotes.data.session

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface SessionDao {

    @Insert
    fun insert(session: Session)

    @Query("SELECT * FROM session_table ORDER BY id ASC")
    fun getAllSessions(): LiveData<List<Session>>

}