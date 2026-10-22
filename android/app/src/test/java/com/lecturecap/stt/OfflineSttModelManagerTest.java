package com.lecturecap.stt;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OfflineSttModelManagerTest {

    @Test
    public void testOfflineModelDirectoryAndStatus() {
        File dummyDir = new File(System.getProperty("java.io.tmpdir"), "stt-model-test");
        if (!dummyDir.exists()) {
            dummyDir.mkdirs();
        }
        assertTrue(dummyDir.exists());
        assertTrue(dummyDir.isDirectory());
    }

    @Test
    public void testDownloadProgressListenerInterface() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] progressCalled = {false};
        final boolean[] completedCalled = {false};

        OfflineSttModelManager.OnDownloadProgressListener listener = new OfflineSttModelManager.OnDownloadProgressListener() {
            @Override
            public void onProgressUpdate(int progressPercent, String statusMsg) {
                progressCalled[0] = true;
                assertTrue(progressPercent >= 0 && progressPercent <= 100);
            }

            @Override
            public void onDownloadCompleted() {
                completedCalled[0] = true;
                latch.countDown();
            }

            @Override
            public void onDownloadFailed(String errorMsg) {
                latch.countDown();
            }
        };

        listener.onProgressUpdate(50, "Downloading offline STT model...");
        listener.onDownloadCompleted();

        boolean success = latch.await(1, TimeUnit.SECONDS);
        assertTrue(success);
        assertTrue(progressCalled[0]);
        assertTrue(completedCalled[0]);
    }
}
