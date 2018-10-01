package org.mitre.fluxnotes.data.session

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import org.mitre.fluxnotes.R

class SessionListActivity : AppCompatActivity() {

    private lateinit var mSessionViewModel: SessionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_list)

        val recyclerView = findViewById<RecyclerView>(R.id.session_recyclerview)
        val adapter = SessionListAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        mSessionViewModel = ViewModelProviders.of(this).get(SessionViewModel::class.java)
        mSessionViewModel.getAllSessions().observe(this, Observer {
            adapter.setSessionsList(it!!)
        })
    }
}
