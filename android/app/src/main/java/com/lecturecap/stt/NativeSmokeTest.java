package com.lecturecap.stt;

public final class NativeSmokeTest {

    static {
        System.loadLibrary("lecturecap_native");
    }

    private NativeSmokeTest() {
    }

    public static native int nativeSmokeTest();
}
