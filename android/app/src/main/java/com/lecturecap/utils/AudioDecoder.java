package com.lecturecap.utils;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioDecoder {

    private static final String TAG = "AudioDecoder";
    public static final int TARGET_SAMPLE_RATE = 16000;

    public interface ProgressCallback {
        void onProgress(int percent, String message);
    }

    /**
     * Decodes an Android audio file (Uri) into a 16kHz Mono 16-bit PCM float array [-1.0f, 1.0f].
     */
    public static float[] decodeTo16kHzMonoPcm(Context context, Uri audioUri, ProgressCallback callback) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(context, audioUri, null);

        int audioTrackIndex = -1;
        MediaFormat format = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat trackFormat = extractor.getTrackFormat(i);
            String mime = trackFormat.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                audioTrackIndex = i;
                format = trackFormat;
                break;
            }
        }

        if (audioTrackIndex == -1 || format == null) {
            extractor.release();
            throw new IllegalArgumentException("No valid audio track found in selected file.");
        }

        extractor.selectTrack(audioTrackIndex);
        String mime = format.getString(MediaFormat.KEY_MIME);
        int inputSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int inputChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        long durationUs = format.containsKey(MediaFormat.KEY_DURATION) ? format.getLong(MediaFormat.KEY_DURATION) : 0;

        Log.d(TAG, "Audio format: mime=" + mime + ", sampleRate=" + inputSampleRate + ", channels=" + inputChannels + ", durationMs=" + (durationUs / 1000));

        MediaCodec decoder = MediaCodec.createDecoderByType(mime);
        decoder.configure(format, null, null, 0);
        decoder.start();

        ByteArrayOutputStream pcmStream = new ByteArrayOutputStream();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        boolean isEOS = false;
        long totalDecodedUs = 0;

        while (!isEOS) {
            int inputBufferIndex = decoder.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferIndex);
                if (inputBuffer != null) {
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }

            int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = decoder.getOutputBuffer(outputBufferIndex);
                if (outputBuffer != null && bufferInfo.size > 0) {
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    byte[] chunk = new byte[bufferInfo.size];
                    outputBuffer.get(chunk);
                    pcmStream.write(chunk, 0, chunk.length);

                    totalDecodedUs = bufferInfo.presentationTimeUs;
                    if (durationUs > 0 && callback != null) {
                        int progress = (int) Math.min(100, (totalDecodedUs * 100) / durationUs);
                        callback.onProgress(progress, "Decoding audio frames (" + progress + "%)...");
                    }
                }
                decoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
            }
        }

        decoder.stop();
        decoder.release();
        extractor.release();

        byte[] rawPcmBytes = pcmStream.toByteArray();
        short[] pcmShorts = new short[rawPcmBytes.length / 2];
        ByteBuffer.wrap(rawPcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcmShorts);

        // Convert to Mono if Multi-channel
        short[] monoShorts;
        if (inputChannels > 1) {
            monoShorts = new short[pcmShorts.length / inputChannels];
            for (int i = 0; i < monoShorts.length; i++) {
                int sum = 0;
                for (int c = 0; c < inputChannels; c++) {
                    sum += pcmShorts[i * inputChannels + c];
                }
                monoShorts[i] = (short) (sum / inputChannels);
            }
        } else {
            monoShorts = pcmShorts;
        }

        // Resample to 16kHz if necessary
        short[] resampledShorts;
        if (inputSampleRate != TARGET_SAMPLE_RATE && inputSampleRate > 0) {
            double ratio = (double) TARGET_SAMPLE_RATE / inputSampleRate;
            int targetLength = (int) (monoShorts.length * ratio);
            resampledShorts = new short[targetLength];
            for (int i = 0; i < targetLength; i++) {
                int srcIdx = (int) (i / ratio);
                if (srcIdx < monoShorts.length) {
                    resampledShorts[i] = monoShorts[srcIdx];
                }
            }
        } else {
            resampledShorts = monoShorts;
        }

        // Convert short[] to float[] [-1.0f, 1.0f]
        float[] floatPcm = new float[resampledShorts.length];
        for (int i = 0; i < resampledShorts.length; i++) {
            floatPcm[i] = resampledShorts[i] / 32768.0f;
        }

        Log.d(TAG, "Successfully decoded audio to 16kHz Mono Float PCM (" + floatPcm.length + " samples, " + (floatPcm.length / TARGET_SAMPLE_RATE) + "s)");
        return floatPcm;
    }
}
