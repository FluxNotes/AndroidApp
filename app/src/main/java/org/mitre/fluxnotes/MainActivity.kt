package org.mitre.fluxnotes

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.speech.tts.Voice
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.util.Log
import android.view.MenuItem
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import org.mitre.fluxnotes.fragments.MessageDialogFragment
import org.mitre.fluxnotes.services.NLPService
import org.mitre.fluxnotes.services.SpeechService
import org.mitre.fluxnotes.tools.VoiceRecorder

const val SAMPLE_RESULTS = "{\n" +
        "    \"diseaseStatus\": [\n" +
        "        [\n" +
        "            \"stable\"\n" +
        "        ]\n" +
        "    ],\n" +
        "    \"toxicity\": [\n" +
        "        {\n" +
        "            \"analyzed_text\": \" of the the new regiment that we started six weeks ago. Right right. Gotcha. Um,Certainly notice. These These toxicities are side effects the muscle that you're describing. Um, the you mentioned tingling. Yeah, so the peripheral sensory neuropathy. Those are common side effects are toxicities that are associated with those two medications. Um, it's something that certainly be concerned. Mm. I guess a \",\n" +
        "            \"concepts\": [\n" +
        "                {\n" +
        "                    \"dbpedia_resource\": \"http://dbpedia.org/resource/Myalgia\",\n" +
        "                    \"relevance\": 0.847421,\n" +
        "                    \"text\": \"Myalgia\"\n" +
        "                },\n" +
        "                {\n" +
        "                    \"dbpedia_resource\": \"http://dbpedia.org/resource/Peripheral_neuropathy\",\n" +
        "                    \"relevance\": 0.82593,\n" +
        "                    \"text\": \"Peripheral neuropathy\"\n" +
        "                }\n" +
        "            ],\n" +
        "            \"entities\": [],\n" +
        "            \"language\": \"en\",\n" +
        "            \"usage\": {\n" +
        "                \"features\": 2,\n" +
        "                \"text_characters\": 404,\n" +
        "                \"text_units\": 1\n" +
        "            }\n" +
        "        },\n" +
        "        {\n" +
        "            \"analyzed_text\": \"ng that's that we think is going to happen for a couple of weeks or it's a great question. So hopefully these side effects will not get worse and to hopefully subside because they're related. to the medication, soumIs aren't um likel\",\n" +
        "            \"concepts\": [],\n" +
        "            \"entities\": [],\n" +
        "            \"language\": \"en\",\n" +
        "            \"usage\": {\n" +
        "                \"features\": 2,\n" +
        "                \"text_characters\": 233,\n" +
        "                \"text_units\": 1\n" +
        "            }\n" +
        "        },\n" +
        "        {\n" +
        "            \"analyzed_text\": \" Um, it's not progressing. It is stable. That's a very good thing. So my recommendation is let's document the toxicities. Let's make sure we understand that the and peripheral neuropathy I things to be concerned about in to watch a\",\n" +
        "            \"concepts\": [\n" +
        "                {\n" +
        "                    \"dbpedia_resource\": \"http://dbpedia.org/resource/Peripheral_neuropathy\",\n" +
        "                    \"relevance\": 0.949819,\n" +
        "                    \"text\": \"Peripheral neuropathy\"\n" +
        "                }\n" +
        "            ],\n" +
        "            \"entities\": [\n" +
        "                {\n" +
        "                    \"count\": 1,\n" +
        "                    \"disambiguation\": {\n" +
        "                        \"dbpedia_resource\": \"http://dbpedia.org/resource/Peripheral_neuropathy\",\n" +
        "                        \"name\": \"Peripheral neuropathy\",\n" +
        "                        \"subtype\": [\n" +
        "                            \"Disease\"\n" +
        "                        ]\n" +
        "                    },\n" +
        "                    \"relevance\": 0.986086,\n" +
        "                    \"text\": \"peripheral neuropathy\",\n" +
        "                    \"type\": \"HealthCondition\"\n" +
        "                }\n" +
        "            ],\n" +
        "            \"language\": \"en\",\n" +
        "            \"usage\": {\n" +
        "                \"features\": 2,\n" +
        "                \"text_characters\": 231,\n" +
        "                \"text_units\": 1\n" +
        "            }\n" +
        "        }\n" +
        "    ]\n" +
        "}\n"

class MainActivity : AppCompatActivity(), MessageDialogFragment.Listener, NLPService.ResponseListener {

    private lateinit var mDrawerLayout: DrawerLayout

    private var recording: Boolean = false

    private lateinit var mVoiceRecorder: VoiceRecorder
    private lateinit var mSpeechService: SpeechService
    private lateinit var mNLPService: NLPService

    private var capturedText: String = ""

    private val mSpeechServiceListener = object : SpeechService.Listener {
        override fun onSpeechRecognized(text: String?, isFinal: Boolean) {
            Log.d("MAIN", "ST text: $text")
            if (isFinal) {
                mVoiceRecorder.dismiss()
            }
            if (recording && isFinal && !text?.isEmpty()!!) {
                capturedText += text
                Log.d("MAIN", "ST FINAL text: $text")
            }
        }
    }

    private var mVoiceCallback = object : VoiceRecorder.Callback(){
        override fun onVoiceStart() {
            mSpeechService?.startRecognizing(mVoiceRecorder.sampleRate)
        }

        override fun onVoice(data: ByteArray?, size: Int) {
            mSpeechService?.recognize(data, size)
        }

        override fun onVoiceEnd() {
            mSpeechService?.finishRecognizing()
        }

        override fun onVoiceCapture(data: ByteArray?, size: Int, sample_rate: Int) {
            super.onVoiceCapture(data, size, sample_rate)
        }
    }

    private val mSpeechServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            mSpeechService = SpeechService.from(binder)
            mSpeechService.addListener(mSpeechServiceListener)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mSpeechService.removeListener(mSpeechServiceListener)
        }
    }

    private val mNLPServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            mNLPService = NLPService.from(binder!!)!!
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDrawerLayout = findViewById(R.id.drawer_layout)

        /*
        mDrawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View?, slideOffset: Float) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDrawerOpened(drawerView: View?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDrawerClosed(drawerView: View?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDrawerStateChanged(newState: Int) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
        */

        /*
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_logo)
        }
        */

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->

            when (menuItem.itemId) {
                R.id.nav_record_encounter -> {
                    val intent = Intent(this, RecordEncounterActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_display_sample -> {
                    val intent = Intent(this, DisplayResultsActivity::class.java)
                    intent.putExtra("text", SAMPLE_RESULTS)
                    startActivity(intent)
                }
            }

            menuItem.isChecked = true
            setTitle(menuItem.title)

            mDrawerLayout.closeDrawers()

            true
        }

        val webview = findViewById<WebView>(R.id.webview)
        webview.settings.javaScriptEnabled = true
        webview.loadUrl("https://fluxnotes.org/demo2")

        // button listener
        val audioButton = findViewById<Button>(R.id.audio_control_button)
        audioButton.setOnClickListener {
            recording = !recording
            if (it !is Button)
                return@setOnClickListener
            if (recording) {
                Log.d("MAIN", "Recording audio")
                it.text = resources.getText(R.string.end_encounter)
                val icn = resources.getDrawable(R.drawable.ic_mic_off_black_24dp)
                icn.setBounds(0, 0, 40, 40)
                it.setCompoundDrawables(icn, null, null, null)
                mVoiceRecorder.start()
            } else {
                Log.d("MAIN", "Stopping audio recording, processing")
                Log.d("MAIN", "Captured text: $capturedText")
                it.text = resources.getText(R.string.begin_encounter)
                val icn = resources.getDrawable(R.drawable.ic_mic_black_24dp)
                icn.setBounds(0, 0, 40, 40)
                it.setCompoundDrawables(icn, null, null, null)
                mVoiceRecorder.stop()
                mNLPService.processTranscription(capturedText, this@MainActivity)
            }
            Toast.makeText(this@MainActivity, "Recording: $recording", Toast.LENGTH_SHORT).show()
        }

        // voice recorder
        mVoiceRecorder = VoiceRecorder(mVoiceCallback)
    }

    override fun onStart() {
        super.onStart()

        bindService(Intent(this@MainActivity, SpeechService::class.java), mSpeechServiceConnection, Context.BIND_AUTO_CREATE)
        bindService(Intent(this@MainActivity, NLPService::class.java), mNLPServiceConnection, Context.BIND_AUTO_CREATE)

        // request permissions
        if (this@MainActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // TODO start voice recorder
        } else if (this@MainActivity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog()
        } else {
            this@MainActivity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun processingComplete(response: String?) {
        Toast.makeText(this@MainActivity, "SUCCESS", Toast.LENGTH_SHORT).show()
        Log.d("MAIN", response)
    }

    override fun onMessageDialogDismissed() {
        this@MainActivity.requestPermissions(kotlin.arrayOf(Manifest.permission.RECORD_AUDIO), 1)
    }

    private fun showPermissionMessageDialog() {
        MessageDialogFragment.newInstance(getString(R.string.permission_message)).show(supportFragmentManager, "message_dialog")
    }
}
