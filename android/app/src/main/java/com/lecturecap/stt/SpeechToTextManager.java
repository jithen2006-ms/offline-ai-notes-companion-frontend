package com.lecturecap.stt;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.List;

public class SpeechToTextManager {

    private static final String TAG = "LectureCap_STT";

    public interface OnTranscriptUpdateListener {
        void onPartialTranscript(String liveText);
        void onTranscriptUpdated(String fullAccumulatedTranscript);
        void onError(String errorMsg);
    }

    private final Context context;
    private final WhisperTranscriptionManager whisperManager;
    private OnTranscriptUpdateListener updateListener;

    public SpeechToTextManager(Context context) {
        this.context = context.getApplicationContext();
        this.whisperManager = new WhisperTranscriptionManager(this.context);

        this.whisperManager.setListener(new WhisperTranscriptionManager.OnWhisperTranscriptListener() {
            @Override
            public void onPartialText(String liveText) {
                if (updateListener != null) {
                    updateListener.onPartialTranscript(liveText);
                }
            }

            @Override
            public void onSegmentsUpdated(List<WhisperTranscriptionManager.TranscriptSegment> segments, String fullText) {
                if (updateListener != null) {
                    updateListener.onTranscriptUpdated(fullText);
                }
            }

            @Override
            public void onError(String errorMsg) {
                if (updateListener != null) {
                    updateListener.onError(errorMsg);
                }
            }
        });
    }

    public void setUpdateListener(OnTranscriptUpdateListener listener) {
        this.updateListener = listener;
    }

    public void startListening() {
        Log.d(TAG, "[WHISPER_ASR_STARTED] Starting local 16kHz PCM Whisper ASR audio capture...");
        whisperManager.startListening();
    }

    public void stopListening() {
        Log.d(TAG, "[WHISPER_ASR_STOPPED] Stopping Whisper ASR audio capture...");
        whisperManager.stopListening();
    }

    public String getFinalTranscript() {
        return whisperManager.getFullTranscriptText();
    }

    public JSONObject getTranscriptPayloadJson() {
        return whisperManager.getTranscriptPayloadJson();
    }

    public List<WhisperTranscriptionManager.TranscriptSegment> getSegments() {
        return whisperManager.getAccumulatedSegments();
    }

    public boolean hasMeaningfulSpeech() {
        String text = getFinalTranscript();
        return text != null && text.trim().length() > 5;
    }
}
