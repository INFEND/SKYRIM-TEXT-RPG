package dev.infend.skyrimtext;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thin HTTP adapter for the independently installed PocketOrca-LLM llama-server.
 * Never executes the generated text or mutates GameEngine. Connections cannot
 * leave loopback, and no downloaded executable/model enters the game APK.
 */
final class PocketOrcaClient {
    private static final int DEFAULT_PORT = 8080;
    private int serverPort() {
        int port=context.getSharedPreferences("skyrim_text_v1",Context.MODE_PRIVATE)
            .getInt("ai_pocketorca_port",DEFAULT_PORT);
        return port>=1024&&port<=65535?port:DEFAULT_PORT;
    }
    private String baseUrl() { return "http://127.0.0.1:"+serverPort()+"/v1/"; }
    private static final int MAX_RESPONSE = 80_000;
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile HttpURLConnection connection;
    private boolean running;
    private boolean closed;
    private int generation;

    interface ProbeListener {
        void onResult(boolean available, String description);
    }

    PocketOrcaClient(Context context) {
        this.context = context.getApplicationContext();
    }

    synchronized boolean isRunning() {
        return running;
    }

    private synchronized boolean active(int id) {
        return !closed && running && id == generation;
    }

    private HttpURLConnection connect(String route, String key, boolean post) throws IOException {
        // Only constant, trusted loopback routes are used. No user URL, DNS,
        // cleartext LAN access, redirects, or system HTTP proxy is accepted.
        if (!"models".equals(route) && !"chat/completions".equals(route))
            throw new IOException("Недопустимый маршрут PocketOrca-LLM");
        URL url = new URL(baseUrl() + route);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod(post ? "POST" : "GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(post ? 180_000 : 8000);
        conn.setRequestProperty("Accept", post ? "text/event-stream" : "application/json");
        if (key != null && !key.isEmpty())
            conn.setRequestProperty("Authorization", "Bearer " + key);
        if (post) {
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
        }
        return conn;
    }

    private static String readSmall(InputStream stream) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[1024];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                if (text.length() + n > 64_000) throw new IOException("Ответ PocketOrca-LLM слишком большой");
                text.append(buffer, 0, n);
            }
        }
        return text.toString();
    }

    private String model(String key) throws Exception {
        HttpURLConnection conn = connect("models", key, false);
        try {
            int code = conn.getResponseCode();
            if (code == 401 || code == 403)
                throw new IOException("PocketOrca-LLM отклонила API-ключ");
            if (code != 200)
                throw new IOException("Сервер PocketOrca-LLM вернул HTTP " + code);
            JSONObject result = new JSONObject(readSmall(conn.getInputStream()));
            JSONArray models = result.optJSONArray("data");
            if (models == null || models.length() == 0)
                throw new IOException("PocketOrca-LLM запущена, но модель не загружена");
            String selected = context.getSharedPreferences("skyrim_text_v1", Context.MODE_PRIVATE)
                    .getString("ai_pocketorca_model", "");
            String first = "";
            for (int i = 0; i < models.length(); i++) {
                JSONObject item = models.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "").trim();
                if (id.isEmpty()) continue;
                if (first.isEmpty()) first = id;
                if (id.equals(selected)) return id;
            }
            if (first.isEmpty()) throw new IOException("PocketOrca-LLM не сообщила идентификатор модели");
            return first;
        } finally {
            conn.disconnect();
        }
    }

    void probe(ProbeListener callback) {
        if (isRunning()) {
            main.post(() -> callback.onResult(false, "Сначала заверши генерацию"));
            return;
        }
        worker.execute(() -> {
            boolean ok = false;
            String message;
            try {
                String key = PocketOrcaKeyVault.read(context);
                String id = model(key);
                context.getSharedPreferences("skyrim_text_v1", Context.MODE_PRIVATE)
                        .edit().putString("ai_pocketorca_last_model", id).apply();
                message = "PocketOrca-LLM доступна. Модель: " + id;
                ok = true;
            } catch (Exception e) {
                message = friendlyError(e);
            }
            final boolean success = ok;
            final String result = message;
            main.post(() -> callback.onResult(success, result));
        });
    }


    /** Only the external PocketOrca profile receives this literary direction. */
    private static final String POCKETORCA_STYLE =
        "ХУДОЖЕСТВЕННЫЙ СТИЛЬ: полноценная русская проза в духе атмосферного Skyrim, не сухая сводка и не игровой отчёт. "+
        "Показывай место через конкретные звуки, свет, холод, запах, движение и мелкие наблюдения героя. "+
        "Меняй ритм длинных и коротких абзацев, создавай живые эмоционально достоверные диалоги и подспудное напряжение. "+
        "NPC говорят по-разному и действуют из своих мотивов. Не повторяй имя героя в каждом предложении, избегай штампов, "+
        "не перечисляй механики или варианты действий внутри сцены. Стремись к 4–8 содержательным абзацам, "+
        "разбивай реплики на строки. Не переписывай канонические факты, не выдавай несыгранное за событие. "+
        "Формат JSON и ограничения эффектов из основного системного промпта обязательны. ";
    synchronized boolean generate(GameEngine game, String history, String command,
                                  boolean controlled, boolean chanceOnly,
                                  AiStoryClient.Listener listener) {
        if (running || closed) return false;
        running = true;
        final int id = ++generation;
        // Build prompts on the worker: prompt/context exceptions must never crash the Activity.
        try {
            worker.execute(() -> {
                try {
                    if (!active(id)) return;
                    String system = AiStoryClient.localSystemPrompt(controlled, chanceOnly);
                    if (!chanceOnly) system += POCKETORCA_STYLE;
                    String prompt = chanceOnly
                        ? AiStoryClient.localChancePrompt(game, command)
                        : AiStoryClient.localUserPrompt(game, history, command, controlled);
                    final int maxTokens = chanceOnly ? 192 : controlled ? 6144 : 2048;
                    execute(id, system, prompt, maxTokens, listener);
                } catch (RuntimeException failure) {
                    android.util.Log.e("PocketOrca", "Cannot prepare local request", failure);
                    String message = "Не удалось подготовить запрос PocketOrca: " +
                        failure.getClass().getSimpleName();
                    main.post(() -> {
                        if (!active(id)) return;
                        synchronized (PocketOrcaClient.this) { running = false; }
                        listener.onError(message);
                    });
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException unavailable) {
            running = false;
            return false;
        }
        return true;
    }

    private void execute(int id, String system, String prompt, int maxTokens,
                         AiStoryClient.Listener listener) {
        HttpURLConnection conn = null;
        try {
            String key = PocketOrcaKeyVault.read(context);
            notifyStatus(id, listener, "Подключение к PocketOrca-LLM…");
            String loadedModel = model(key);
            if (!active(id)) return;
            context.getSharedPreferences("skyrim_text_v1", Context.MODE_PRIVATE)
                    .edit().putString("ai_pocketorca_last_model", loadedModel).apply();
            notifyStatus(id, listener, "Модель: " + loadedModel + " | генерация…");

            JSONObject payload = new JSONObject();
            payload.put("model", loadedModel);
            payload.put("stream", true);
            payload.put("max_tokens", maxTokens);
            payload.put("temperature", 0.78);
            payload.put("top_p", 0.92);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", system));
            messages.put(new JSONObject().put("role", "user").put("content", prompt));
            payload.put("messages", messages);
            byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
            conn = connect("chat/completions", key, true);
            synchronized (this) {
                if (!active(id)) return;
                connection = conn;
            }
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body);
            }
            int code = conn.getResponseCode();
            if (code == 401 || code == 403)
                throw new IOException("PocketOrca-LLM отклонила API-ключ");
            if (code < 200 || code >= 300)
                throw new IOException("Ошибка генерации PocketOrca-LLM: HTTP " + code);

            StringBuilder answer = new StringBuilder();
            StringBuilder event = new StringBuilder();
            boolean done = false;
            boolean finishSeen = false;
            long lastUpdate = 0L;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (active(id) && (line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        if (event.length() > 0) event.append('\n');
                        event.append(line.substring(5).trim());
                    } else if (!line.isEmpty() || event.length() == 0) {
                        continue;
                    }
                    // llama-server sends one data line per SSE event. Also
                    // accept multi-line data frames and a terminal [DONE].
                    if (line.isEmpty() && event.length() > 0) {
                        String data = event.toString();
                        event.setLength(0);
                        if ("[DONE]".equals(data)) { done = true; break; }
                        JSONObject frame = new JSONObject(data);
                        if (frame.has("error")) throw new IOException("PocketOrca-LLM сообщила об ошибке генерации");
                        JSONArray choices = frame.optJSONArray("choices");
                        if (choices == null || choices.length() == 0) continue;
                        JSONObject choice = choices.optJSONObject(0);
                        if (choice == null) continue;
                        JSONObject delta = choice.optJSONObject("delta");
                        String part = delta == null ? "" : delta.optString("content", "");
                        if (!part.isEmpty()) {
                            if (answer.length() + part.length() > MAX_RESPONSE)
                                throw new IOException("Превышен размер ответа ИИ");
                            answer.append(part);
                            long now = android.os.SystemClock.elapsedRealtime();
                            if (now - lastUpdate >= 300L) {
                                String snapshot = answer.toString();
                                main.post(() -> { if (active(id)) {
                                    try { listener.onText(snapshot); }
                                    catch(RuntimeException uiError) { android.util.Log.e("PocketOrca","Streaming UI error",uiError); }
                                } });
                                lastUpdate = now;
                            }
                        }
                        if (!choice.isNull("finish_reason") && choice.has("finish_reason"))
                            finishSeen = true;
                    }
                }
                if (!done && active(id) && event.length() > 0) {
                    String data = event.toString();
                    if ("[DONE]".equals(data)) done = true;
                    else {
                        JSONObject frame = new JSONObject(data);
                        JSONArray choices = frame.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject choice = choices.optJSONObject(0);
                            if (choice != null && choice.has("finish_reason") &&
                                    !choice.isNull("finish_reason")) finishSeen = true;
                        }
                    }
                }
            }
            if (!active(id)) return;
            if (!done && !finishSeen)
                throw new IOException("Поток PocketOrca-LLM прервался до завершения ответа");
            if (answer.length() == 0)
                throw new IOException("PocketOrca-LLM вернула пустой ответ");
            String full = answer.toString();
            main.post(() -> {
                if (!active(id)) return;
                synchronized (PocketOrcaClient.this) { running = false; }
                try {
                    listener.onText(full);
                    listener.onComplete(full);
                } catch(RuntimeException uiError) {
                    android.util.Log.e("PocketOrca","Completion UI error",uiError);
                    listener.onError("Ошибка отображения ответа PocketOrca. Состояние сохранения проверь в игре.");
                }
            });
        } catch (Exception e) {
            android.util.Log.e("PocketOrca","Local inference HTTP failure",e);
            String message = friendlyError(e);
            main.post(() -> {
                if (!active(id)) return;
                synchronized (PocketOrcaClient.this) { running = false; }
                listener.onError(message);
            });
        } finally {
            synchronized (this) {
                if (connection == conn) connection = null;
            }
            if (conn != null) conn.disconnect();
        }
    }

    private void notifyStatus(int id, AiStoryClient.Listener listener, String message) {
        main.post(() -> { if (active(id)) listener.onLocalStatus(message); });
    }

    private String friendlyError(Exception exception) {
        if (exception instanceof ConnectException)
            return "PocketOrca-LLM не отвечает на 127.0.0.1:" + serverPort() + ". Запусти API-сервер и загрузи модель";
        if (exception instanceof SocketTimeoutException)
            return "PocketOrca-LLM слишком долго не отвечает";
        if (exception instanceof java.net.UnknownServiceException)
            return "Локальный HTTP запрещён настройками Android";
        if (exception instanceof IOException)
            return exception.getMessage() == null ? "Ошибка связи с PocketOrca-LLM" : exception.getMessage();
        return "PocketOrca-LLM: ошибка формата ответа или настроек сервера";
    }

    synchronized void cancel() {
        generation++;
        running = false;
        HttpURLConnection conn = connection;
        if (conn != null) conn.disconnect();
        connection = null;
    }

    synchronized void shutdown() {
        cancel();
        closed = true;
        worker.shutdownNow();
    }
}
