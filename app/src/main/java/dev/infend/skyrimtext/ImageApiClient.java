package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opt-in, user-key OpenAI Images API. It never uses ChatGPT/Work OAuth tokens.
 * Each invocation requires confirmation in WorkImageEditor. No retries, silent
 * fallback or automatic charging. All output stays in app-private storage.
 */
final class ImageApiClient {
    interface Callback { void complete(List<byte[]> images, String error); }
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final int MAX_IMAGE = 24 * 1024 * 1024;
    private static final int MAX_RESPONSE = 96 * 1024 * 1024;
    private static final String ROOT = "https://api.openai.com/v1/images/";
    private ImageApiClient() {}

    static boolean busy() { return RUNNING.get(); }
    static boolean modelAllowed(String model) {
        return "gpt-image-2.6-sunburst".equals(model)
            || "gpt-image-2.6-flare".equals(model)
            || "gpt-image-2.5-sunburst".equals(model)
            || "gpt-image-2.5-flare".equals(model)
            || "gpt-image-1.5".equals(model);
    }

    static void request(Context context, String prompt, String model, String quality,
                        String size, int count, Uri reference, Callback callback) {
        if (!RUNNING.compareAndSet(false, true)) {
            MAIN.post(() -> callback.complete(null, "Дождись завершения предыдущего запроса"));
            return;
        }
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            List<byte[]> images = null;
            String error = null;
            try {
                if (prompt == null || prompt.trim().isEmpty() || prompt.length() > 32000)
                    throw new IllegalArgumentException("Описание должно содержать от 1 до 32000 символов");
                if (!modelAllowed(model)) throw new IllegalArgumentException("Модель изображений не поддерживается");
                if (!("low".equals(quality) || "medium".equals(quality) || "high".equals(quality)))
                    throw new IllegalArgumentException("Неизвестное качество");
                if (!("1024x1024".equals(size) || "1536x1024".equals(size)
                        || "1024x1536".equals(size)))
                    throw new IllegalArgumentException("Неизвестное разрешение");
                if (count < 1 || count > 5) throw new IllegalArgumentException("Можно создать 1–5 изображений");
                String key = AiKeyVault.read(app);
                if (key.isEmpty()) throw new IllegalArgumentException("Сначала сохрани OpenAI API-ключ");
                HttpURLConnection conn = null;
                try {
                    boolean editing = reference != null;
                    conn = (HttpURLConnection) new URL(ROOT + (editing ? "edits" : "generations")).openConnection();
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(240000);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Authorization", "Bearer " + key);
                    conn.setRequestProperty("Accept", "application/json");
                    if (editing) {
                        String boundary = "SkyrimImage" + java.util.UUID.randomUUID().toString().replace("-", "");
                        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                        conn.setChunkedStreamingMode(8192);
                        try (OutputStream out = conn.getOutputStream()) {
                            field(out, boundary, "model", model);
                            field(out, boundary, "prompt", prompt.trim());
                            field(out, boundary, "quality", quality);
                            field(out, boundary, "size", size);
                            field(out, boundary, "n", String.valueOf(count));
                            field(out, boundary, "output_format", "webp");
                            field(out, boundary, "output_compression", "90");
                            byte[] ref = referenceWebp(app, reference);
                            out.write(("--" + boundary + "\r\n"
                                + "Content-Disposition: form-data; name=\"image[]\"; filename=\"reference.webp\"\r\n"
                                + "Content-Type: image/webp\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                            out.write(ref);
                            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                        }
                    } else {
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        JSONObject request = new JSONObject()
                            .put("model", model).put("prompt", prompt.trim()).put("quality", quality)
                            .put("size", size).put("n", count).put("output_format", "webp")
                            .put("output_compression", 90);
                        byte[] body = request.toString().getBytes(StandardCharsets.UTF_8);
                        conn.setFixedLengthStreamingMode(body.length);
                        try (OutputStream out = conn.getOutputStream()) { out.write(body); }
                    }
                    int status = conn.getResponseCode();
                    if (status < 200 || status >= 300) {
                        String details = "";
                        InputStream failure = conn.getErrorStream();
                        if (failure != null) {
                            String raw = new String(readLimited(failure, 8192), StandardCharsets.UTF_8);
                            try { details = new JSONObject(raw).optJSONObject("error").optString("message", ""); }
                            catch (Exception ignored) { details = ""; }
                        }
                        if (status == 401) details = "Проверь API-ключ";
                        else if (status == 429) details = "Лимит запросов или бюджет API исчерпан";
                        else if (status >= 500) details = "Ошибка сервера OpenAI";
                        throw new IllegalStateException("OpenAI API: HTTP " + status
                            + (details.isEmpty() ? "" : " · " + details.substring(0, Math.min(250, details.length()))));
                    }
                    byte[] reply = readLimited(conn.getInputStream(), MAX_RESPONSE);
                    JSONArray data = new JSONObject(new String(reply, StandardCharsets.UTF_8)).optJSONArray("data");
                    if (data == null || data.length() == 0)
                        throw new IllegalStateException("OpenAI API не вернул изображений");
                    if (data.length() > count)
                        throw new IllegalStateException("API вернул неожиданное количество изображений");
                    images = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        String encoded = data.getJSONObject(i).optString("b64_json", "");
                        if (encoded.isEmpty() || encoded.length() > MAX_IMAGE * 2)
                            throw new IllegalStateException("Слишком большой или пустой ответ изображения");
                        byte[] decoded = Base64.decode(encoded, Base64.DEFAULT);
                        if (decoded.length == 0 || decoded.length > MAX_IMAGE)
                            throw new IllegalStateException("Превышен предел 24 МБ на изображение");
                        BitmapFactory.Options bounds = new BitmapFactory.Options();
                        bounds.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(decoded, 0, decoded.length, bounds);
                        if (bounds.outWidth < 32 || bounds.outHeight < 32
                                || (long) bounds.outWidth * bounds.outHeight > 120000000L)
                            throw new IllegalStateException("API вернул повреждённое изображение");
                        images.add(decoded);
                    }
                } finally { if (conn != null) conn.disconnect(); }
            } catch (Exception | OutOfMemoryError failure) {
                error = failure.getMessage() == null ? "Не удалось создать изображение" : failure.getMessage();
                images = null;
            } finally { RUNNING.set(false); }
            final List<byte[]> result = images;
            final String message = error;
            MAIN.post(() -> callback.complete(result, message));
        }, "skyrim-openai-images").start();
    }

    private static void field(OutputStream out, String boundary, String name, String value) throws Exception {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\""
                + name + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    /** Converts Android-supported files into one bounded WebP reference, never uploads paths. */
    private static byte[] referenceWebp(Context app, Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = app.getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IllegalArgumentException("Исходное изображение недоступно");
            BitmapFactory.decodeStream(stream, null, bounds);
        }
        if (bounds.outWidth < 32 || bounds.outHeight < 32
                || (long) bounds.outWidth * bounds.outHeight > 120000000L)
            throw new IllegalArgumentException("Слишком большой или повреждённый исходник");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth / options.inSampleSize,
                bounds.outHeight / options.inSampleSize) > 4096) options.inSampleSize *= 2;
        Bitmap bitmap;
        try (InputStream stream = app.getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IllegalArgumentException("Не удалось прочитать исходник");
            bitmap = BitmapFactory.decodeStream(stream, null, options);
        }
        if (bitmap == null) throw new IllegalArgumentException("Не удалось декодировать исходник");
        try {
            int edge = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (edge > 2048) {
                float ratio = 2048f / edge;
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                    Math.max(1, Math.round(bitmap.getWidth() * ratio)),
                    Math.max(1, Math.round(bitmap.getHeight() * ratio)), true);
                bitmap.recycle();
                bitmap = scaled;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (!bitmap.compress(Bitmap.CompressFormat.WEBP, 92, bytes))
                throw new IllegalArgumentException("Не удалось подготовить изображение");
            if (bytes.size() > MAX_IMAGE) throw new IllegalArgumentException("Исходник превышает 24 МБ");
            return bytes.toByteArray();
        } finally { bitmap.recycle(); }
    }

    private static byte[] readLimited(InputStream input, int limit) throws Exception {
        try (InputStream stream = new BufferedInputStream(input);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = stream.read(buffer)) != -1) {
                if (output.size() + n > limit) throw new IllegalStateException("Ответ API слишком большой");
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        }
    }
}
