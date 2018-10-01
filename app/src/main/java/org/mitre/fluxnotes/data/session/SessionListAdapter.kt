package org.mitre.fluxnotes.data.session

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.mitre.fluxnotes.R

const val TAG: String = "SessionListAdapter"

class SessionListAdapter(val context: Context) : RecyclerView.Adapter<SessionListAdapter.ViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mSessions: List<Session>? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sessionDateView: TextView = itemView.findViewById(R.id.session_date_tv)
        val sessionFilePath: TextView = itemView.findViewById(R.id.session_audio_filepath_tv)
        val sessionTranscription: TextView = itemView.findViewById(R.id.session_transcription_tv)

        init {
            itemView.setOnClickListener(View.OnClickListener {
                Log.d(TAG, "item $adapterPosition clicked")
                val intent = Intent(context, DisplaySessionActivity::class.java)
                intent.putExtra("SESSION_KEY", adapterPosition)
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val itemView = mInflater.inflate(R.layout.session_recyclerview_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = mSessions?.get(position) ?: Session("No Sessions")
        holder.sessionDateView.text = session.timestamp
        holder.sessionFilePath.text = session.audioFilePath
        holder.sessionTranscription.text = "${if (session.transcriptionText.length > 200) session.transcriptionText.substring(0..200) + "..." else session.transcriptionText }"
        Log.d("SESSIONLISTADAPTER", "here's a thing ${session.audioFilePath}")
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "Number of sessions: ${mSessions?.size ?: 0}")
        return mSessions?.size ?: 0
    }

    fun setSessionsList(sessions: List<Session>) {
        mSessions = sessions
        notifyDataSetChanged()
    }
}