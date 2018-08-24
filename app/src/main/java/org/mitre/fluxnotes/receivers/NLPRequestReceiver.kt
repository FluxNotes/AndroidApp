package org.mitre.fluxnotes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.mitre.fluxnotes.DisplayResultsActivity

const val IP = "10.7.9.168"
const val URL = "http://${IP}:3000/watson"
const val EXTRA_MESSAGE = "org.mitre.fluxnotes.NLP_REQUEST"
const val BOUNDARY = "BHH2P347U89HFSDOIFJQP2"
const val MULTIPART_FORMDATA = "multipart/form-data;boundary=$BOUNDARY"

class NLPRequestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.d("NLPRequestReceiver", "NLPRequestReceiver:Received Intent")
        val text = intent.getStringExtra("text")
        val queue = Volley.newRequestQueue(context)

        val request = object : StringRequest(Request.Method.POST, URL,
                Response.Listener<String> { response ->
                    Log.d("SPEECH", "NLPRequestReceiver:SUCCESS")
                    Log.d("SPEECH", "NLPRequestReceiver: " + response)

                    val intent = Intent(context, DisplayResultsActivity::class.java).apply {
                        putExtra("RESULT", response)
                        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    //startActivity(intent)
                    context.startActivity(intent)

                },
                Response.ErrorListener { error ->
                    Log.d("SPEECH", "NLPRequestReceiver: " + error.toString())
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
        Log.d("SPEECH", request.headers.toString())
        Log.d("SPEECH", String(request.body))
        queue.add(request)
    }
}
