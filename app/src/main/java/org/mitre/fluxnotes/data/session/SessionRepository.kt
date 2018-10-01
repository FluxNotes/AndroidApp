package org.mitre.fluxnotes.data.session

import android.app.Application
import android.arch.lifecycle.LiveData
import android.os.AsyncTask

class SessionRepository(application: Application) {
    private lateinit var mSessionDao: SessionDao
    private lateinit var mAllSessions: LiveData<List<Session>>

    init {
        val db = SessionDatabase.getInstance(application.applicationContext)
        if (db != null) {
            mSessionDao = db.sessionDao()
            mAllSessions = mSessionDao.getAllSessions()
        }
    }

    fun getAllSessions(): LiveData<List<Session>> {
        return mAllSessions
    }

    fun insert(session: Session) {
        insertAysncTask(mSessionDao).execute(session)
    }

    inner class insertAysncTask(val dao: SessionDao) : AsyncTask<Session, Unit, Unit>() {
        override fun doInBackground(vararg p0: Session?) {
            dao.insert(p0[0]!!)
        }
    }
}