package com.dongnao.livedemo.client;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.dongnao.livedemo.core.RESSoftAudioCore;
import com.dongnao.livedemo.filter.softaudiofilter.BaseSoftAudioFilter;
import com.dongnao.livedemo.model.RTMPConfig;
import com.dongnao.livedemo.model.RTMPCoreParameters;
import com.dongnao.livedemo.rtmp.RESFlvDataCollecter;
import com.dongnao.livedemo.tools.LogTools;


public class RTMPAudioClient {
    RTMPCoreParameters resCoreParameters;
    private final Object syncOp = new Object();
    private AudioRecordThread audioRecordThread;
    private AudioRecord audioRecord;
    private byte[] audioBuffer;
    private RESSoftAudioCore softAudioCore;

    public RTMPAudioClient(RTMPCoreParameters parameters) {
        resCoreParameters = parameters;
    }

    public boolean prepare(RTMPConfig resConfig) {
        synchronized (syncOp) {
            resCoreParameters.audioBufferQueueNum = 5;
            softAudioCore = new RESSoftAudioCore(resCoreParameters);
            if (!softAudioCore.prepare(resConfig)) {
                LogTools.e("RTMPAudioClient,prepare");
                return false;
            }
            resCoreParameters.audioRecoderFormat = AudioFormat.ENCODING_PCM_16BIT;
            resCoreParameters.audioRecoderChannelConfig = AudioFormat.CHANNEL_IN_MONO;
            resCoreParameters.audioRecoderSliceSize = resCoreParameters.mediacodecAACSampleRate / 10;
            resCoreParameters.audioRecoderBufferSize = resCoreParameters.audioRecoderSliceSize * 2;
            resCoreParameters.audioRecoderSource = MediaRecorder.AudioSource.DEFAULT;
            resCoreParameters.audioRecoderSampleRate = resCoreParameters.mediacodecAACSampleRate;
            prepareAudio();
            return true;
        }
    }

    public boolean start(RESFlvDataCollecter flvDataCollecter) {
        synchronized (syncOp) {
            softAudioCore.start(flvDataCollecter);
            audioRecord.startRecording();
            audioRecordThread = new AudioRecordThread();
            audioRecordThread.start();
            LogTools.d("RTMPAudioClient,start()");
            return true;
        }
    }

    public boolean stop() {
        synchronized (syncOp) {
            if(audioRecordThread != null) {
                audioRecordThread.quit();
                try {
                    audioRecordThread.join();
                } catch (InterruptedException ignored) {
                    ignored.printStackTrace();
                }
                softAudioCore.stop();
                audioRecordThread = null;
                audioRecord.stop();
                return true;
            }
            return true;
        }
    }

    public boolean destroy() {
        synchronized (syncOp) {
            audioRecord.release();
            return true;
        }
    }
    public void setSoftAudioFilter(BaseSoftAudioFilter baseSoftAudioFilter) {
        softAudioCore.setAudioFilter(baseSoftAudioFilter);
    }
    public BaseSoftAudioFilter acquireSoftAudioFilter() {
        return softAudioCore.acquireAudioFilter();
    }

    public void releaseSoftAudioFilter() {
        softAudioCore.releaseAudioFilter();
    }

    private boolean prepareAudio() {
        int minBufferSize = AudioRecord.getMinBufferSize(resCoreParameters.audioRecoderSampleRate,
                resCoreParameters.audioRecoderChannelConfig,
                resCoreParameters.audioRecoderFormat);
        audioRecord = new AudioRecord(resCoreParameters.audioRecoderSource,
                resCoreParameters.audioRecoderSampleRate,
                resCoreParameters.audioRecoderChannelConfig,
                resCoreParameters.audioRecoderFormat,
                minBufferSize * 5);
        audioBuffer = new byte[resCoreParameters.audioRecoderBufferSize];
        if (AudioRecord.STATE_INITIALIZED != audioRecord.getState()) {
            LogTools.e("audioRecord.getState()!=AudioRecord.STATE_INITIALIZED!");
            return false;
        }
        if (AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod(resCoreParameters.audioRecoderSliceSize)) {
            LogTools.e("AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod(" + resCoreParameters.audioRecoderSliceSize + ")");
            return false;
        }
        return true;
    }

    class AudioRecordThread extends Thread {
        private boolean isRunning = true;

        AudioRecordThread() {
            isRunning = true;
        }

        public void quit() {
            isRunning = false;
        }

        @Override
        public void run() {
            LogTools.d("AudioRecordThread,tid=" + Thread.currentThread().getId());
            while (isRunning) {
                int size = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                if (isRunning && softAudioCore != null && size > 0) {
                    softAudioCore.queueAudio(audioBuffer);
                }
            }
        }
    }
}
