package org.mitre.fluxnotes;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.mitre.fluxnotes.fragments.MessageDialogFragment;
import org.mitre.fluxnotes.services.NLPService;
import org.mitre.fluxnotes.services.SpeechService;
import org.mitre.fluxnotes.tools.VoiceRecorder;
import org.mitre.fluxnotes.tools.SessionCapture;

public class RecordEncounterActivity extends AppCompatActivity implements MessageDialogFragment.Listener, NLPService.ResponseListener {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private SpeechService mSpeechService;

    private NLPService mNLPService;

    // Speech to Text Result Collection
    public enum SessionState {BEGIN, END, PROCESS}
    private SessionState currentSessionState;
    private StringBuilder spToTxtResult;
    private boolean collectSpToTxt;
    private SessionCapture sessionCapture;

    private VoiceRecorder mVoiceRecorder;
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            //showStatus(true);
            if (mSpeechService != null) {
                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (mSpeechService != null) {
                mSpeechService.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            //showStatus(false);
            if (mSpeechService != null) {
                mSpeechService.finishRecognizing();
            }
        }

        @Override
        public void onVoiceCapture(byte[] data, int size, int sampleRate){
            if(sessionCapture != null){
                sessionCapture.captureAudioSample(data, size, sampleRate);
            }
        }

    };

    // View references
    private ImageView mStatusImage;
    private TextView mStatusText;
    private Button mButtonImage;
    private TextView mButtonText;

    private final ServiceConnection mSpeechServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.addListener(mSpeechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService.removeListener(mSpeechServiceListener);
            mSpeechService = null;
        }

    };

    private final ServiceConnection mNLPServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mNLPService = NLPService.Companion.from(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mNLPService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_encounter);

        mStatusText = findViewById(R.id.statusText);
        mStatusImage = findViewById(R.id.statusImage);
        mButtonText = findViewById(R.id.buttonText);
        mButtonImage = findViewById(R.id.buttonImage);

        currentSessionState = SessionState.BEGIN;
        collectSpToTxt = false;
        spToTxtResult = new StringBuilder();
        sessionCapture = new SessionCapture(this, this);
    }

    // TODO: Need some way to enable and disable session capture from the UI?
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sessionCapture_Off:
                if(sessionCapture != null){
                    sessionCapture.setEnableCapture(false);}
                return true;
            case R.id.sessionCapture_On:
                if(sessionCapture != null){
                    sessionCapture.setEnableCapture(true);
                }
                return true;
            case R.id.audioPlayback:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    */

    @Override
    protected void onStart() {
        super.onStart();

        // Flush Speech to Text Result storage
        collectSpToTxt = false;
        spToTxtResult.setLength(0);
        //Set Status Image
        mStatusImage.setImageResource(0);
        //Set Status Text
        mStatusText.setText("");
        //Set Button Color
        mButtonImage.setBackground(getDrawable(R.drawable.beginbutton));
        //Set Button Text
        mButtonText.setText(getResources().getString(R.string.begin_encounter));
        //Set initial session state
        currentSessionState = SessionState.BEGIN;



        // Prepare Cloud Speech API
        bindService(new Intent(this, SpeechService.class), mSpeechServiceConnection, BIND_AUTO_CREATE);

        bindService(new Intent(this, NLPService.class), mNLPServiceConnection, BIND_AUTO_CREATE);

        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecorder();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stopVoiceRecorder();
        collectSpToTxt = false;
        // Stop Cloud Speech API
        unbindService(mSpeechServiceConnection);
        unbindService(mNLPServiceConnection);

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecorder();
            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void buttonOnClick(View v){
        Button button = (Button) v;
        switch (currentSessionState)
        {
            case BEGIN:
                // If capturing is enabled then start doing so.
                sessionCapture.startCapture();
                //Set Status Text
                mStatusText.setText("Recording...");
                //Set Button Shape
                mButtonImage.setBackground(getDrawable(R.drawable.endbutton));
                //Set Button Text
                mButtonText.setText(getResources().getString(R.string.end_encounter));
                //Set Status Image
                mStatusImage.setImageResource(R.mipmap.sound_wave);
                // Stop Collection
                collectSpToTxt = false;
                // Flush text Storage
                spToTxtResult.setLength(0);
                // Start Collection
                collectSpToTxt = true;
                Log.d("SPEECH", "Start collection");
                startVoiceRecorder();
                currentSessionState = SessionState.END;
                break;
            case END:
                //Set Status Image
                mStatusImage.setImageResource(R.mipmap.cloud);
                //Set Status Text
                mStatusText.setText("Processing...");
                //Set Button Color
                mButtonImage.setBackgroundColor(getResources().getColor(R.color.TextGray));
                //Set Button Text
                mButtonText.setText("");
                // Stop Collection
                collectSpToTxt = false;
                stopVoiceRecorder();
                // Post Intent
                Log.d("SPEECH", "Stop collection");
                Log.d("SPEECH", spToTxtResult.toString());
                //Intent intent = new Intent("org.mitre.fluxnotes.NLP_REQUEST");
                //intent.putExtra("text", spToTxtResult.toString());
                //startActivity(intent);
                //sendBroadcast(intent);
                mNLPService.processTranscription(spToTxtResult.toString(), this);
                currentSessionState = SessionState.PROCESS;
                break;
                /*
            case R.id.action_sample:
                intent = new Intent("org.mitre.fluxnotes.NLP_REQUEST");
                intent.putExtra("text", "All right. Hello, my name is dr. Houston. I'm a medical oncologist. I'm here with Debra a 45 year old woman who is a nurse assistant with metastatic recurrent breast cancer too. Oh, right young month. That's initially stage 1A infiltrating ductal. Carcinoma Deborah. How are you doing today? I'm not doing very well dear what's been going on? So it appears that I've had more numbness and tingling in my fingers my hands my feet. Um, this is really affecting it a lot of what I need to do during the day guys. This has this when did this start started shortly after my new treatment? Oh the tax and the receptive. Yeah. Okay. So those those medications are part of the the new regiment that we started six weeks ago. Right right. Gotcha. Um,Certainly notice. These These toxicities are side effects the muscle that you're describing. Um, the you mentioned tingling. Yeah, so the peripheral sensory neuropathy. Those are common side effects are toxicities that are associated with those two medications. Um, it's something that certainly be concerned. Mm. I guess a follow-up question I would have is are these are is the numbness and tingling. Is it affecting your normal day-to-day activities? Well this I'm having trouble going up and down the stairs. I'm having trouble getting in and out of the shower even getting dressed. Um, I'm just wondering how long this numbness tingling are going to um occur. I mean, is this something that's that we think is going to happen for a couple of weeks or it's a great question. So hopefully these side effects will not get worse and to hopefully subside because they're related. to the medication, soumIs aren't um likely to be permanent that sense they should go away. Um, but something that we should keep track of and ensure that they don't get worse so that if your activities are failing living are getting more, uh, pronounced affected, um, we should probably consider different treatment regimens. Um and taking there's no medications. How long are they going to stay in my system? I mean over time are they going to diminish diminish over time? Yeah, it should be something that um, you know, staying your support week or two, but then should be flushed out. And is there something else I can do either another medication to try to um, reduce the symptom of feeling or um change my diet question. So unfortunately, there's nothing really that can be done other than just resting and taking it easy. Um if you have troubleIf you have worse trouble.Breathing anything like that. You should contact our office immediately. One of the things I wanted to make sure that we went over uh was the results from your CT scan. So we had an enhanced, uh, CT scan done last week. We're uh metastatic cancer and some of the results, um at a high level while one unit here that nothing's changed. So it hasn't gotten worse and the fact that we just started the new treatment regimen six weeks ago. You don't expect things to get better this quickly. So they found the pulmonary nodules 4 to 5 millimeters in diameter along the right along side as well as some superficial, uh, uh post-surgical changes to the right breast in the axilla area, which would be uh, normal given that the mastectomy that that we had. Um, so the nodules haven't gotten bigger. Um, your disease is stable. That's what we How we got?Getting bigger there are more of them. Um, it's not progressing. It is stable. That's a very good thing. So my recommendation is let's document the toxicities. Let's make sure we understand that the and peripheral neuropathy I things to be concerned about in to watch and let's continue to track to make sure that those nodules start to diminish in size. So in terms of patients, you see what's a percentage, um of patients that you see that maybe I'm somewhat identical to in that there's been no change what if percentages that maybe have gotten worse or even gotten better at this point. I would say that every patient is their own unique story. Um,\n");
                sendBroadcast(intent);

                return true;
                */
            default:
                break;

        }
    }

    public void processingComplete(String response){
        // Capture the NLP result.
        sessionCapture.captureNlpSample(response);

        /* Update display for next session... */
        //Set Status Image
        mStatusImage.setImageResource(0);
        //Set Status Text
        mStatusText.setText("");
        //Set Button Color
        mButtonImage.setBackground(getDrawable(R.drawable.beginbutton));
        //Set Button Text
        mButtonText.setText(getResources().getString(R.string.begin_encounter));
        //Set initial session state
        currentSessionState = SessionState.BEGIN;

        // Processing is complete end session capture.
        sessionCapture.stopCapture();

        // Display NLP response.
        Intent intent = new Intent(this, DisplayResultsActivity.class);
        intent.putExtra("text", response);
        startActivity(intent);
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    private void showStatus(final boolean hearingVoice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("SPEECH", "Hearing Voice:" + (hearingVoice ? "Yes":"No"));
            }
        });
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }
    static int count=0;
    private final SpeechService.Listener mSpeechServiceListener =
            new SpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    Log.d("RECORD", "ST text: " + text);
                    if (isFinal && mVoiceRecorder != null) {
                        mVoiceRecorder.dismiss();
                    }
                    if (!TextUtils.isEmpty(text)) {
                        Log.d("RECORD", "ST FINAL text: " + text);
                        if(collectSpToTxt && isFinal) {
                            spToTxtResult.append(text);
                            sessionCapture.captureStoTSample(text);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    //Log.d("SPEECH", text);
                                    count=0;
                                } else {
                                    //Log.d("SPEECH","Block Processed" + ++count);
                                }
                            }
                        });
                    }
                }
            };
}
