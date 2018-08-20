/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.android.speech;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements MessageDialogFragment.Listener {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final String STATE_RESULTS = "results";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private SpeechService mSpeechService;

    // Speech to Text Result Collection
    public enum SessionState {BEGIN, END, PROCESS};
    private SessionState currentSessionState;
    private StringBuilder spToTxtResult;
    private boolean collectSpToTxt;


    private VoiceRecorder mVoiceRecorder;
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            showStatus(true);
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
            showStatus(false);
            if (mSpeechService != null) {
                mSpeechService.finishRecognizing();
            }
        }

    };

    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;

    // View references
    private ImageView mStatusImage;
    private TextView mStatusText;
    private Button mButtonImage;
    private TextView mButtonText;

    //private RecyclerView mRecyclerView;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.addListener(mSpeechServiceListener);
            //mStatus.setVisibility(View.VISIBLE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fluxnote2spch_main);

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.startColor, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.stopColor, theme);

        //setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mStatusText = (TextView) findViewById(R.id.statusText);
        mStatusImage = (ImageView) findViewById(R.id.statusImage);
        mButtonText = (TextView) findViewById(R.id.buttonText);
        mButtonImage = (Button) findViewById(R.id.buttonImage);

        //final ArrayList<String> results = savedInstanceState == null ? null :
        //        savedInstanceState.getStringArrayList(STATE_RESULTS);

        currentSessionState = SessionState.BEGIN;
        collectSpToTxt = false;
        spToTxtResult = new StringBuilder();
    }

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
        mButtonText.setText("Begin Encounter");
        //Set initial session state
        currentSessionState = SessionState.BEGIN;



        // Prepare Cloud Speech API
        bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);

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
        mSpeechService.removeListener(mSpeechServiceListener);
        unbindService(mServiceConnection);
        mSpeechService = null;

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
                //Set Status Text
                mStatusText.setText("Recording...");
                //Set Button Shape
                mButtonImage.setBackground(getDrawable(R.drawable.endbutton));
                //Set Button Text
                mButtonText.setText("End Encounter");
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
                // Post Intent
                Log.d("SPEECH", "Stop collection");
                Log.d("SPEECH", spToTxtResult.toString());
                Intent intent = new Intent("org.mitre.fluxnotes.NLP_REQUEST");
                intent.putExtra("text", spToTxtResult.toString());
                sendBroadcast(intent);
                currentSessionState = SessionState.PROCESS;
                break;
            case PROCESS:
            default:
                break;

        }
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

                    if (isFinal) {
                        mVoiceRecorder.dismiss();
                    }
                    if (!TextUtils.isEmpty(text)) {
                        if(collectSpToTxt && isFinal) {
                            spToTxtResult.append(text);
                            Log.d("SPEECH", spToTxtResult.toString());
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    Log.d("SPEECH", text);
                                    count=0;
                                } else {
                                    Log.d("SPEECH","Block Processed" + ++count);
                                }
                            }
                        });
                    }
                }
            };
}
