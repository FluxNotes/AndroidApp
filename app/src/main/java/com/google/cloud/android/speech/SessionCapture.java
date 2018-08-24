package com.google.cloud.android.speech;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

public class SessionCapture {

    private boolean capture;
    private boolean captureRunning;
    private static final String FILE_PATH = "/sdcard";
    private static final String SESSION_CAPTURE_FOLDER = "Session";
    private static final String AUDIO_TEMP_FILE = "record_temp.raw";
    private static final String AUDIO_CAPTURE_FILE = "audio.wav";
    private static final String SPEECH_TO_TEXT_CAPTURE_FILE = "speechToText.txt";
    private static final String NLP_CAPTURE_FILE = "nlp_response.json";
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 1;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 2;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 4;
    private int sizeInBytes;
    private int audioSampleRate;
    private long byteCount;

    private File workingpath;
    private File sessionDir;
    private File audioFile;
    private File tempfile;
    private File sToTFile;
    private File nlpFile;


    public SessionCapture(Context pc, Activity pa ) {
        setEnableCapture(false);
        captureRunning = false;
        audioSampleRate = 0;
        sizeInBytes = 0;

        if (ContextCompat.checkSelfPermission(pc, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it.
            ActivityCompat.requestPermissions(pa,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(pc, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it.
            ActivityCompat.requestPermissions(pa,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    public void captureAudioSample(byte[] data, int size, int sampleRate){
        audioSampleRate = sampleRate;
        this.sizeInBytes = AudioRecord.getMinBufferSize(audioSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if(captureRunning && sampleRate != 0){
            try {
                OutputStream os = new FileOutputStream(tempfile, (tempfile.length()>0));
                os.write(data, 0, size);
                byteCount+=size;

            } catch (FileNotFoundException fnfe) {
                Log.d("SPEECH Audio Capture ", fnfe.getMessage());
                fnfe.printStackTrace();
            } catch (IOException ioe){
                Log.d("SPEECH Audio Capture ", ioe.getMessage());
                ioe.printStackTrace();
            }
        }
    }

    public void captureNlpSample(String sample){
        if(captureRunning){

        }
    }

    public void captureStoTSample(String sample){
        Log.d("SPEECH S2T Capture ", " CR:" + captureRunning);
        if(captureRunning){
            try {
                FileWriter fw = new FileWriter( sToTFile.getAbsoluteFile( ), (sToTFile.length()>0));
                BufferedWriter bw = new BufferedWriter( fw );
                bw.write( sample );
                bw.close( );
            } catch( IOException e ) {
                Log.d("SPEECH S2T Capture ", e.getMessage());
                e.printStackTrace( );
            }
            Log.d("SPEECH S2T Capture", sample);
        }
    }

    public void setEnableCapture(boolean capture){
        this.capture = capture;
        Log.d("SPEECH Capture", "capture:" + capture);
    }

    public boolean getEnableCapture(){
        return capture;
    }

    public void startCapture(){
        byteCount = 0;
        // Start capturing create an input stream to a file
        // and push whatever samples come in.
        Log.d("SPEECH Capture", " START " + "CR: " + captureRunning);
        if(!captureRunning && capture){

            // Create files to store the session samples
            workingpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            Log.d("SPEECH", "Storage State: " +Environment.getExternalStorageState());
            try {
                sessionDir = new File(workingpath,SESSION_CAPTURE_FOLDER);
            }catch (Exception e1){
                Log.d("SPEECH Capture", " START-> Error Creating " + sessionDir.getAbsolutePath() + "  " + e1.getMessage());}


            if(!sessionDir.exists()){
                if(!sessionDir.mkdirs())
                {
                    Log.d("SPEECH Capture", "START-> Error Creating " + sessionDir.getAbsolutePath());
                } else{
                    Log.d("SPEECH Capture", " START-> Created " + sessionDir.getAbsolutePath());}
            }
            try {
                audioFile = new File(sessionDir.getAbsolutePath() + "/" + AUDIO_CAPTURE_FILE);
            }catch (Exception e1){
                Log.d("SPEECH Capture", " START-> Error Creating " + audioFile.getAbsolutePath() + "  " + e1.getMessage());}
            try{
                sToTFile = new File(sessionDir.getAbsolutePath() + "/" + SPEECH_TO_TEXT_CAPTURE_FILE);
            }catch (Exception e2){
                Log.d("SPEECH Capture", " START-> Error Creating " + sToTFile.getAbsolutePath() + "  " + e2.getMessage());}
            try{
                nlpFile = new File(sessionDir.getAbsolutePath() + "/" + NLP_CAPTURE_FILE);
            }catch (Exception e3){
                Log.d("SPEECH Capture", " START-> Error Creating " + nlpFile.getAbsolutePath() + "  " + e3.getMessage());}
            try{
                tempfile = new File(sessionDir.getAbsolutePath() + "/" + AUDIO_TEMP_FILE);
            }catch (Exception e4){
                Log.d("SPEECH Capture", " START-> Error Creating " + tempfile.getAbsolutePath() + "  " + e4.getMessage());}
            captureRunning = true;
        }
    }

    public void stopCapture(){
        // Done capturing, move the captured samples into a
        // new file formatted as a WAV file and flush the capture file.
        Log.d("SPEECH Capture", " STOP " + "CR: " + captureRunning);
        if(captureRunning){
            captureRunning = false;
            Log.d("SPEECH", "Byte Count:"+byteCount +"  TempFileSize:"+tempfile.length());
            copyWaveFile(tempfile.getAbsolutePath(), audioFile.getAbsolutePath());
            Log.d("SPEECH Capture", " START-> Deleted " + tempfile.getAbsolutePath());
            tempfile.delete();

            // Zip up the session and store it.
            if(sessionDir.exists()) {
                ZipHelper sessionFile = new ZipHelper();
                try {
                    sessionFile.zipDir(sessionDir.getAbsolutePath(), getSessionFileName());
                } catch (Exception e){
                    Log.e("SPEECH","Error creating Session Zip. " + e.getMessage());
                }
                deleteFolder(sessionDir);
            }
        }
    }

    /**
     * Audio Capture
     */

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = audioSampleRate;
        int channels = 1; // mono
        long byteRate = 2 * longSampleRate;

        byte[] data = new byte[sizeInBytes];

        Log.d("SPEECH Capture", " Copied " + inFilename + " into " + outFilename);
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels; // 1 = mono;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample 16-PCM
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    private String getSessionFileName(){
        Date dNow = new Date( );
        SimpleDateFormat ft =
                new SimpleDateFormat ("yyyyMMdd_hhmmss");
        return (workingpath.getAbsolutePath() + "/Session_" + ft.format(dNow) + ".zip");
    }
}

class ZipHelper
{
    public void zipDir(String dirName, String nameZipFile) throws IOException {
        Log.d("SPEECH", "Zipping up "+dirName+". Placing it in "+nameZipFile);
        ZipOutputStream zip = null;
        FileOutputStream fW = null;
        fW = new FileOutputStream(nameZipFile);
        zip = new ZipOutputStream(fW);
        addFolderToZip("", dirName, zip);
        zip.close();
        fW.close();
    }

    private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws IOException {
        File folder = new File(srcFolder);
        if (folder.list().length == 0) {
            addFileToZip(path , srcFolder, zip, true);
        }
        else {
            for (String fileName : folder.list()) {
                if (path.equals("")) {
                    addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip, false);
                }
                else {
                    addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip, false);
                }
            }
        }
    }

    private void addFileToZip(String path, String srcFile, ZipOutputStream zip, boolean flag) throws IOException {
        File folder = new File(srcFile);
        if (flag) {
            zip.putNextEntry(new ZipEntry(path + "/" +folder.getName() + "/"));
        }
        else {
            if (folder.isDirectory()) {
                addFolderToZip(path, srcFile, zip);
            }
            else {
                byte[] buf = new byte[1024];
                int len;
                FileInputStream in = new FileInputStream(srcFile);
                zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            }
        }
    }
}


