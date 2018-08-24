package org.mitre.fluxnotes.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

const val IP = "10.7.9.168"
const val URL = "http://${IP}:3000/watson"
const val EXTRA_MESSAGE = "org.mitre.fluxnotes.NLP_REQUEST"
const val BOUNDARY = "BHH2P347U89HFSDOIFJQP2"
const val MULTIPART_FORMDATA = "multipart/form-data;boundary=$BOUNDARY"

class NLPService : Service() {

    companion object {
        const val TAG = "NLPService"
    }

    private val mBinder = NLPBinder()

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    inner class NLPBinder : Binder() {
        fun getService(): NLPService {
            return this@NLPService
        }
    }

}
