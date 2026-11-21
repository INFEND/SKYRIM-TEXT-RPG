package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User-approved, paid OpenAI Images API calls. The signed-in ChatGPT/Work
 * session is never used as an API credential.
 */
final class OpenAiImageApi {
    interface Callback { void finished(byte[] webp, String error); }
    static final String[] MODELS = {
        "gpt-image-2.6-flare", "gpt-image-2.6-sunburst", "gpt-image-2.5-flare"
    };
    static final String[] SIZES = {"1024x1024", "1536x1024", "1024x1536"};
    static final String[] QUALITIES = {"medium", "high", "low"};
    private static final int MAX_RESPONSE_BYTES = 48 * 1024 * 1024;
    private static final int MAX_IMAGE_BYTES = 24 * 1024 * 1024;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private OpenAiImageApi() {}

    static final class Request {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile HttpURLConnection connection;
        void cancel() {
            cancelled.set(true);
            HttpURLConnection open = connection;
            if (open != null) open.disconnect();
        }
        boolean cancelled() { return cancelled.get(); }
    }

    static Request request(Context context, String text, String model,
                           String size, String quality, File source, Callback callback) {
        Request task = new Request();
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            byte[] output = null;
            String problem = null;
            HttpURLConnection connection = null;
            try {
                if (task.cancelled()) return;
                if (text == null || text.trim().isEmpty() || text.length() > 32000)
                    throw new IllegalArgumentException("Описание должно содержать от 1 до 32000 символов");
                if (!allowed(MODELS, model) || !allowed(SIZES, size) || !allowed(QUALITIES, quality))
                    throw new IllegalArgumentException("Неподдерживаемые параметры изображения");
                String key = AiKeyVault.read(app);
                if (key.isEmpty()) throw new IllegalStateException("Добавь OpenAI API-ключ в настройках");
                boolean editing = source != null;
                JSONObject payload = new JSONObject();
                payload.put("model", model).put("prompt", text.trim())
                       .put("n", 1).put("size", size).put("quality", quality)
                       .put("output_format", "webp").put("output_compression", 88);
                if (editing) {
                    byte[] ref = sourceImage(source);
                    JSONObject image = new JSONObject().put("image_url",
                        "data:image/webp;base64," + Base64.encodeToString(ref, Base64.NO_WRAP));
                    payload.put("images", new JSONArray().put(image));
                }
                URL endpoint = new URL(editing
                    ? "https://api.openai.com/v1/images/edits"
                    : "https://api.openai.com/v1/images/generations");
                connection = (HttpURLConnection) endpoint.openConnection();
                task.connection = connection;
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(240000);
                connection.setUseCaches(false);
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Bearer " + key);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream out = connection.getOutputStream()) { out.write(body); }
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    String server = "";
                    try (InputStream in = connection.getErrorStream()) {
                        if (in != null) {
                            JSONObject error = new JSONObject(new String(readLimited(in, 8192),
                                StandardCharsets.UTF_8)).optJSONObject("error");
                            if (error != null) server = error.optString("message", "");
                        }
                    } catch (Exception ignored) {}
                    throw new IllegalStateException("OpenAI HTTP " + code
                        + (server.isEmpty() ? "" : ": " + clip(server, 250)));
                }
                byte[] response;
                try (InputStream in = connection.getInputStream()) {
                    response = readLimited(in, MAX_RESPONSE_BYTES);
                }
                JSONObject json = new JSONObject(new String(response, StandardCharsets.UTF_8));
                JSONArray data = json.optJSONArray("data");
                String encoded = data == null || data.length() == 0 ? ""
                    : data.getJSONObject(0).optString("b64_json", "");
                if (encoded.isEmpty() || encoded.length() > MAX_IMAGE_BYTES * 4 / 3 + 16)
                    throw new IllegalStateException("API не вернул подходящий файл изображения");
                output = Base64.decode(encoded, Base64.DEFAULT);
                if (output.length == 0 || output.length > MAX_IMAGE_BYTES)
                    throw new IllegalStateException("Ответ изображения слишком большой");
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(output, 0, output.length, options);
                if (options.outWidth < 32 || options.outHeight < 32 ||
                    options.outWidth > 8192 || options.outHeight > 8192)
                    throw new IllegalStateException("API вернул изображение неверного размера");
            } catch (Exception | OutOfMemoryError e) {
                problem = e.getMessage() == null ? "Неизвестная ошибка генерации" : e.getMessage();
            } finally {
                task.connection = null;
                if (connection != null) connection.disconnect();
            }
            if (task.cancelled()) return;
            final byte[] result = output;
            final String error = problem;
            MAIN.post(() -> { if (!task.cancelled()) callback.finished(result, error); });
        }, "skyrim-openai-image").start();
        return task;
    }

    private static byte[] sourceImage(File source) throws Exception {
        if (!source.isFile() || source.length() < 1 || source.length() > MAX_IMAGE_BYTES)
            throw new IllegalArgumentException("Исходное изображение недоступно");
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
        if (bounds.outWidth < 32 || bounds.outHeight < 32 ||
            (long) bounds.outWidth * bounds.outHeight > 120000000L)
            throw new IllegalArgumentException("Исходное изображение слишком большое");
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1;
        while (Math.max(bounds.outWidth / opts.inSampleSize,
                        bounds.outHeight / opts.inSampleSize) > 2048)
            opts.inSampleSize *= 2;
        Bitmap bmp = BitmapFactory.decodeFile(source.getAbsolutePath(), opts);
        if (bmp == null) throw new IllegalArgumentException("Не удалось прочитать исходное изображение");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!bmp.compress(Bitmap.CompressFormat.WEBP, 88, out))
                throw new IllegalStateException("Не удалось подготовить исходное изображение");
            byte[] ref = out.toByteArray();
            if (ref.length > 14 * 1024 * 1024)
                throw new IllegalArgumentException("Исходное изображение после подготовки слишком большое");
            return ref;
        } finally { bmp.recycle(); }
    }
    private static byte[] readLimited(InputStream in, int limit) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() > limit - n)
                    throw new IllegalStateException("Превышен лимит размера ответа API");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }
    private static boolean allowed(String[] options, String input) {
        for (String option : options) if (option.equals(input)) return true;
        return false;
    }
    private static String clip(String input, int length) {
        return input.length() > length ? input.substring(0, length) : input;
    }
}
