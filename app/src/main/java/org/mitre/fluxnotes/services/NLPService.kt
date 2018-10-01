package org.mitre.fluxnotes.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

const val IP = "10.0.0.85"
const val PORT = "3000"
const val URL = "http://$IP:$PORT/watson"
const val BOUNDARY = "BHH2P347U89HFSDOIFJQP2"
const val MULTIPART_FORMDATA = "multipart/form-data;boundary=$BOUNDARY"

class NLPService : Service() {

    companion object {
        const val TAG = "NLPService"

        fun from(binder: IBinder): NLPService? {
            if (binder is NLPBinder) {
                return binder.getService()
            } else {
                return null
            }
        }
    }

    private val mBinder = NLPBinder()

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    fun processTranscription(text: String, listener: ResponseListener) {
        val queue = Volley.newRequestQueue(this)

        val request = object : StringRequest(Request.Method.POST, URL,
                Response.Listener<String> { response ->
                    Log.d(TAG, "SUCCESS")
                    Log.d(TAG, response)

                    listener.processingComplete(response)

                },
                Response.ErrorListener { error ->
                    Log.d(TAG, error.toString())
                    listener.processingComplete(null)
                }
        ) {

            override fun getBodyContentType(): String {
                return MULTIPART_FORMDATA
            }

            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params.put("text", text)
                val map: List<String> = params.map {
                    (key, value) -> "--$BOUNDARY\r\nContent-Disposition: form-data; name=\"$key\"\r\n\r\n$value\r\n"
                }

                val ret = "${map.joinToString("")}\r\n--$BOUNDARY--\r\n"
                return ret.toByteArray()
            }

            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Content-Type", MULTIPART_FORMDATA)
                headers.put("Cache-Control", "no-cache")

                return headers
            }
        }
        Log.d(TAG, request.headers.toString())
        Log.d(TAG, String(request.body))
        queue.add(request)
    }

    inner class NLPBinder : Binder() {
        fun getService(): NLPService {
            return this@NLPService
        }
    }

    interface ResponseListener {
        fun processingComplete(response: String?)
    }

}
