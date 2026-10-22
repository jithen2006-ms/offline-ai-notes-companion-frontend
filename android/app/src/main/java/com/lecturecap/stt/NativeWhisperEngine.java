package com.lecturecap.stt;

public final class NativeWhisperEngine {

    static {
        System.loadLibrary("lecturecap_native");
    }

    private NativeWhisperEngine() {
    }

    public static native boolean nativeInitModel(String modelPath);

    public static native void nativeReleaseModel();

    public static native String nativeTranscribe(float[] audio);
}