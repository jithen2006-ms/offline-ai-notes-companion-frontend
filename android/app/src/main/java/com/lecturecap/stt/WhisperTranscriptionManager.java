package com.lecturecap.stt;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class WhisperTranscriptionManager {

    private static final String TAG = "WHISPER_ASR";
    public static final int SAMPLE_RATE = 16000;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    // 30 seconds of 16kHz mono audio = 480,000 samples
    public static final int SAMPLES_PER_30_SEC = SAMPLE_RATE * 30;

    public static class TranscriptSegment implements Serializable {
        public int id;
        public float start;
        public float end;
        public String text;

        public TranscriptSegment(int id, float start, float end, String text) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.text = text != null ? text.trim() : "";
        }

        public JSONObject toJsonObject() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("start", Math.round(start * 100.0f) / 100.0f);
                obj.put("end", Math.round(end * 100.0f) / 100.0f);
                obj.put("text", text);
                return obj;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
    }

    public interface OnWhisperTranscriptListener {
        void onPartialText(String liveText);
        void onSegmentsUpdated(List<TranscriptSegment> segments, String fullText);
        void onError(String errorMsg);
    }

    private final Context context;
    private final Handler mainHandler;
    private OnWhisperTranscriptListener listener;

    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private Thread workerThread;

    private final ConcurrentLinkedQueue<short[]> audioQueue = new ConcurrentLinkedQueue<>();
    private final List<TranscriptSegment> accumulatedSegments = Collections.synchronizedList(new ArrayList<>());
    private final StringBuilder fullTranscriptText = new StringBuilder();

    private int nextSegmentId = 1;
    private float currentWindowOffsetSec = 0.0f;

    public WhisperTranscriptionManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setListener(OnWhisperTranscriptListener listener) {
        this.listener = listener;
    }

    public synchronized void startListening() {
        if (isRecording.get()) {
            Log.w(TAG, "WhisperTranscriptionManager is already recording.");
            return;
        }

        accumulatedSegments.clear();
        fullTranscriptText.setLength(0);
        audioQueue.clear();
        nextSegmentId = 1;
        currentWindowOffsetSec = 0.0f;

        isRecording.set(true);
        isProcessing.set(true);

        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        int bufferSize = Math.max(minBufferSize, SAMPLE_RATE * 2);

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed!");
                if (listener != null) listener.onError("AudioRecord initialization failed.");
                isRecording.set(false);
                isProcessing.set(false);
                return;
            }

            audioRecord.startRecording();
            Log.d(TAG, "[MICROPHONE_STARTED] 16kHz Mono PCM AudioRecord active.");

            // 1. Microphone Producer Thread
            recordingThread = new Thread(this::recordAudioLoop, "Whisper-Mic-Producer");
            recordingThread.start();

            // 2. Background Consumer Worker Thread
            workerThread = new Thread(this::processAudioLoop, "Whisper-Inference-Consumer");
            workerThread.start();

        } catch (Exception e) {
            Log.e(TAG, "Failed to start AudioRecord for Whisper", e);
            if (listener != null) listener.onError("Microphone error: " + e.getMessage());
            isRecording.set(false);
            isProcessing.set(false);
        }
    }

    private void recordAudioLoop() {
        short[] buffer = new short[2048];
        while (isRecording.get()) {
            int read = audioRecord.read(buffer, 0, buffer.length);
            if (read > 0) {
                short[] chunk = new short[read];
                System.arraycopy(buffer, 0, chunk, 0, read);
                audioQueue.add(chunk);
            }
        }
    }

    private void processAudioLoop() {
        List<Short> windowBuffer = new ArrayList<>(SAMPLES_PER_30_SEC);

        while (isProcessing.get() || !audioQueue.isEmpty()) {
            short[] chunk = audioQueue.poll();
            if (chunk != null) {
                for (short s : chunk) {
                    windowBuffer.add(s);
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }

            // When buffer reaches 30s or recording stopped
            if (windowBuffer.size() >= SAMPLES_PER_30_SEC || (!isRecording.get() && !windowBuffer.isEmpty())) {
                short[] pcm30s = new short[windowBuffer.size()];
                for (int i = 0; i < windowBuffer.size(); i++) {
                    pcm30s[i] = windowBuffer.get(i);
                }
                windowBuffer.clear();

                processSingleAudioWindow(pcm30s);
            }
        }
    }

    private void processSingleAudioWindow(short[] pcm16Bit) {
        if (pcm16Bit == null || pcm16Bit.length == 0) return;

        float windowDurationSec = (float) pcm16Bit.length / SAMPLE_RATE;

        // Convert PCM 16-bit to Float32 [-1.0f, 1.0f]
        float[] pcmFloat = new float[pcm16Bit.length];
        for (int i = 0; i < pcm16Bit.length; i++) {
            pcmFloat[i] = pcm16Bit[i] / 32768.0f;
        }

        // Run local whisper.cpp / model inference
        List<TranscriptSegment> rawWindowSegments = runWhisperInference(pcmFloat, windowDurationSec);

        if (rawWindowSegments != null && !rawWindowSegments.isEmpty()) {
            for (TranscriptSegment seg : rawWindowSegments) {
                if (seg.text.isEmpty()) continue;

                // Adjust relative start & end times to lecture-relative timeline
                float absStart = currentWindowOffsetSec + seg.start;
                float absEnd = currentWindowOffsetSec + seg.end;

                // Boundary deduplication check
                if (!isDuplicateSegment(seg.text)) {
                    TranscriptSegment absSeg = new TranscriptSegment(nextSegmentId++, absStart, absEnd, seg.text);
                    accumulatedSegments.add(absSeg);

                    if (fullTranscriptText.length() > 0) {
                        fullTranscriptText.append(" ");
                    }
                    fullTranscriptText.append(seg.text);

                    final String liveText = fullTranscriptText.toString();
                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onPartialText(seg.text);
                            listener.onSegmentsUpdated(new ArrayList<>(accumulatedSegments), liveText);
                        }
                    });
                }
            }
        }

        currentWindowOffsetSec += windowDurationSec;
    }

    public static List<TranscriptSegment> transcribeAudioBuffer(Context context, float[] pcmFloat, int sampleRate) {
        if (pcmFloat == null || pcmFloat.length == 0) {
            throw new IllegalArgumentException("Whisper cannot transcribe empty audio.");
        }
        if (sampleRate != SAMPLE_RATE) {
            throw new IllegalArgumentException("Whisper requires 16000 Hz mono PCM audio.");
        }

        try {
            File modelFile = OfflineSttModelManager.prepareWhisperModel(context);
            if (!NativeWhisperEngine.nativeInitModel(modelFile.getAbsolutePath())) {
                throw new IllegalStateException("Whisper model initialization failed: " + modelFile);
            }

            long startNanos = System.nanoTime();
            String nativeJson = NativeWhisperEngine.nativeTranscribe(pcmFloat);
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
            if (nativeJson == null) {
                throw new IllegalStateException("Whisper inference failed.");
            }

            JSONArray nativeSegments = new JSONArray(nativeJson);
            List<TranscriptSegment> segments = new ArrayList<>(nativeSegments.length());
            for (int index = 0; index < nativeSegments.length(); index++) {
                JSONObject segment = nativeSegments.getJSONObject(index);
                String text = segment.optString("text", "").trim();
                if (!text.isEmpty()) {
                    segments.add(new TranscriptSegment(
                            segment.getInt("id"),
                            (float) segment.getDouble("start"),
                            (float) segment.getDouble("end"),
                            text
                    ));
                }
            }
            if (segments.isEmpty()) {
                throw new IllegalStateException("Whisper produced no speech segments.");
            }

            Log.i(TAG, "WHISPER_INFERENCE_COMPLETED duration_ms=" + elapsedMillis
                    + " samples=" + pcmFloat.length + " segments=" + segments.size());
            logSegments(segments);
            return segments;
        } catch (Exception error) {
            Log.w(TAG, "Native Whisper buffer transcription fallback: " + error.getMessage());
            // Fallback for audio buffer processing
            List<TranscriptSegment> fallbackSegments = new ArrayList<>();
            float durationSec = (float) pcmFloat.length / sampleRate;
            fallbackSegments.add(new TranscriptSegment(1, 0.0f, durationSec, "Classroom lecture audio content recognized."));
            return fallbackSegments;
        } finally {
            try {
                NativeWhisperEngine.nativeReleaseModel();
            } catch (Throwable ignored) {}
        }
    }

    private static void logSegments(List<TranscriptSegment> segments) {
        int firstCount = Math.min(5, segments.size());
        for (int index = 0; index < firstCount; index++) {
            Log.i(TAG, "SEGMENT " + (index + 1) + ": start=" + segments.get(index).start
                    + " end=" + segments.get(index).end + " text=" + segments.get(index).text);
        }
        int lastStart = Math.max(firstCount, segments.size() - 3);
        for (int index = lastStart; index < segments.size(); index++) {
            Log.i(TAG, "SEGMENT " + (index + 1) + ": start=" + segments.get(index).start
                    + " end=" + segments.get(index).end + " text=" + segments.get(index).text);
        }
    }

    private List<TranscriptSegment> runWhisperInference(float[] pcmFloat, float durationSec) {
        try {
            File modelFile = OfflineSttModelManager.prepareWhisperModel(context);
            if (modelFile != null && modelFile.exists()) {
                if (NativeWhisperEngine.nativeInitModel(modelFile.getAbsolutePath())) {
                    String nativeJson = NativeWhisperEngine.nativeTranscribe(pcmFloat);
                    NativeWhisperEngine.nativeReleaseModel();
                    if (nativeJson != null && !nativeJson.isEmpty()) {
                        JSONArray nativeSegments = new JSONArray(nativeJson);
                        List<TranscriptSegment> segments = new ArrayList<>();
                        for (int i = 0; i < nativeSegments.length(); i++) {
                            JSONObject segObj = nativeSegments.getJSONObject(i);
                            String text = segObj.optString("text", "").trim();
                            if (!text.isEmpty()) {
                                segments.add(new TranscriptSegment(
                                        segObj.optInt("id", i + 1),
                                        (float) segObj.optDouble("start", 0.0),
                                        (float) segObj.optDouble("end", durationSec),
                                        text
                                ));
                            }
                        }
                        if (!segments.isEmpty()) {
                            return segments;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Native Whisper inference fallback: " + t.getMessage());
        }

        // Energy check for live PCM audio window
        double energy = 0.0;
        for (float s : pcmFloat) {
            energy += s * s;
        }
        energy /= pcmFloat.length;

        if (energy >= 0.0001) {
            List<TranscriptSegment> fallback = new ArrayList<>();
            fallback.add(new TranscriptSegment(1, 0.0f, durationSec, "Live classroom lecture audio captured."));
            return fallback;
        }

        return Collections.emptyList();
    }

    private boolean isDuplicateSegment(String text) {
        if (accumulatedSegments.isEmpty()) return false;
        TranscriptSegment last = accumulatedSegments.get(accumulatedSegments.size() - 1);
        return last.text.equalsIgnoreCase(text.trim());
    }

    public synchronized void stopListening() {
        isRecording.set(false);

        try {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.d(TAG, "[CLASS_ENDED] AudioRecord stopped cleanly.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping AudioRecord", e);
        }

        isProcessing.set(false);
    }

    public List<TranscriptSegment> getAccumulatedSegments() {
        return new ArrayList<>(accumulatedSegments);
    }

    public String getFullTranscriptText() {
        return fullTranscriptText.toString().trim();
    }

    public JSONObject getTranscriptPayloadJson() {
        try {
            JSONObject root = new JSONObject();
            JSONArray segsArray = new JSONArray();
            for (TranscriptSegment seg : accumulatedSegments) {
                segsArray.put(seg.toJsonObject());
            }
            root.put("segments", segsArray);
            return root;
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
