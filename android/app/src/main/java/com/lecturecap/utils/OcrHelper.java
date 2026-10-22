package com.lecturecap.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lecturecap.model.OcrElement;

public class OcrHelper {

    private static final String TAG = "OcrHelper";

    public interface OcrCallback {
        void onSuccess(String rawText, List<String> lines, List<OcrElement> spatialElements);
        void onError(String errorMessage);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void processUri(Context context, Uri uri, OcrCallback callback) {
        executor.execute(() -> {
            try {
                String mimeType = context.getContentResolver().getType(uri);
                Log.d(TAG, "Processing URI: " + uri + " (MIME: " + mimeType + ")");

                List<Bitmap> bitmaps = new ArrayList<>();

                if (mimeType != null && mimeType.contains("pdf")) {
                    bitmaps.addAll(renderPdfPages(context, uri));
                } else {
                    InputStream inputStream = context.getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (inputStream != null) inputStream.close();
                    if (bitmap != null) {
                        bitmaps.add(bitmap);
                    }
                }

                if (bitmaps.isEmpty()) {
                    mainHandler.post(() -> callback.onError("Could not decode image or PDF pages from selected file."));
                    return;
                }

                // Process bitmaps with ML Kit
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                StringBuilder fullTextBuilder = new StringBuilder();
                List<String> allLines = new ArrayList<>();
                List<OcrElement> allElements = new ArrayList<>();

                for (int i = 0; i < bitmaps.size(); i++) {
                    Bitmap bmp = bitmaps.get(i);
                    InputImage image = InputImage.fromBitmap(bmp, 0);

                    final int pageIndex = i;
                    recognizer.process(image)
                            .addOnSuccessListener(visionText -> {
                                String text = visionText.getText();
                                fullTextBuilder.append(text).append("\n");

                                for (Text.TextBlock block : visionText.getTextBlocks()) {
                                    for (Text.Line line : block.getLines()) {
                                        String lineStr = line.getText().trim();
                                        if (!lineStr.isEmpty()) {
                                            allLines.add(lineStr);
                                            allElements.add(new OcrElement(lineStr, line.getBoundingBox()));
                                        }
                                    }
                                }

                                if (pageIndex == bitmaps.size() - 1) {
                                    mainHandler.post(() -> callback.onSuccess(fullTextBuilder.toString(), allLines, allElements));
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "OCR recognition error", e);
                                mainHandler.post(() -> callback.onError("OCR recognition failed: " + e.getMessage()));
                            });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening URI for OCR", e);
                mainHandler.post(() -> callback.onError("Failed to read file: " + e.getMessage()));
            }
        });
    }

    private static List<Bitmap> renderPdfPages(Context context, Uri uri) {
        List<Bitmap> bitmaps = new ArrayList<>();
        File tempFile = null;
        try {
            tempFile = File.createTempFile("ocr_pdf_", ".pdf", context.getCacheDir());
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(pfd);
            int pageCount = Math.min(renderer.getPageCount(), 5); // Limit max 5 pages

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                bitmaps.add(bitmap);
                page.close();
            }

            renderer.close();
            pfd.close();
        } catch (Exception e) {
            Log.e(TAG, "Error rendering PDF pages", e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
        return bitmaps;
    }
}
