package com.shuyu.gsyvideoplayer.subtitle;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.shuyu.gsyvideoplayer.utils.Debuger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GSYSubtitleLoader {

    public interface Callback {
        void onLoaded(GSYSubtitleSource source, GSYSubtitleProvider provider);

        void onFailed(GSYSubtitleSource source, Exception error);
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Future<?> currentTask;

    public void load(final Context context, final GSYSubtitleSource source, final Callback callback) {
        cancelCurrent();
        currentTask = executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String data = read(context, source);
                    GSYSubtitleParser parser = GSYSubtitleParserFactory.create(source.getMimeType());
                    List<GSYSubtitleCue> cues = parser.parse(data);
                    final GSYSubtitleProvider provider = new GSYSubtitleProvider(cues);
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onLoaded(source, provider);
                        }
                    });
                } catch (final Exception e) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    Debuger.printfWarning("GSY subtitle load failed: " + e.getMessage());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailed(source, e);
                        }
                    });
                }
            }
        });
    }

    public void cancelCurrent() {
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    public void release() {
        cancelCurrent();
        executorService.shutdownNow();
    }

    private String read(Context context, GSYSubtitleSource source) throws Exception {
        InputStream inputStream = null;
        HttpURLConnection connection = null;
        try {
            String url = source.getUrl();
            if (TextUtils.isEmpty(url)) {
                throw new IllegalArgumentException("Subtitle url is empty");
            }
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                for (Map.Entry<String, String> entry : source.getHeaders().entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
                inputStream = new BufferedInputStream(connection.getInputStream());
            } else if ("file".equalsIgnoreCase(scheme)) {
                inputStream = new BufferedInputStream(new FileInputStream(new File(uri.getPath())));
            } else if (TextUtils.isEmpty(scheme)) {
                inputStream = new BufferedInputStream(new FileInputStream(new File(url)));
            } else {
                inputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
            }
            byte[] bytes = readAll(inputStream);
            String charsetName = TextUtils.isEmpty(source.getCharsetName()) ? "UTF-8" : source.getCharsetName();
            return new String(bytes, Charset.forName(charsetName));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private byte[] readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }
}
