package org.mitre.fluxnotes.data.session

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val mRepository: SessionRepository = SessionRepository(application)
    private var mAllSessions: LiveData<List<Session>>

    init {
        mAllSessions = mRepository.getAllSessions()
    }

    fun getAllSessions(): LiveData<List<Session>> {
        return mAllSessions
    }
}