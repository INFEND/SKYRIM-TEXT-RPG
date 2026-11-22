package dev.infend.skyrimtext;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Creates TEXT-ONLY image prompts using the same authorized ChatGPT account
 * and Responses endpoint as the game's Work/Codex narration.
 * Never requests image generation from ChatGPT Work.
 */
final class WorkImagePromptClient {
    interface Callback { void finished(String prompt, String error); }
    private static final String ENDPOINT = "https://api.openai.com/v1/responses";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private WorkImagePromptClient() {}

    static void generate(Activity host, String kind, String name, String facts,
                         String currentDraft, Callback callback) {
        ChatGptOAuth oauth = new ChatGptOAuth(host);
        if (!oauth.hasAuthorizedPlan()) {
            oauth.shutdown();
            callback.finished(null, "Подключи аккаунт ChatGPT в настройках ИИ.");
            return;
        }
        String model = host.getSharedPreferences("skyrim_text_v1", Activity.MODE_PRIVATE)
            .getString("ai_chatgpt_model", "").trim();
        if (!model.matches("[a-zA-Z0-9._-]{2,100}")) {
            oauth.shutdown();
            callback.finished(null, "Сначала выбери модель ChatGPT в настройках ИИ.");
            return;
        }
        String category = "npc".equals(kind) ? "персонажа" :
            "item".equals(kind) ? "предмета" : "локации";
        String input = "Создай один готовый текстовый промпт для генератора изображений. "
            + "Объект: " + category + " «" + crop(name, 200) + "». "
            + "Подтверждённые сведения: " + crop(facts, 1000) + ". "
            + "Текущий черновик (ориентир, можно полностью переписать): "
            + crop(currentDraft, 2200) + ".";
        oauth.getToken((token, tokenError) -> {
            oauth.shutdown();
            if (host.isFinishing() || host.isDestroyed()) return;
            if (tokenError != null || token == null || token.isEmpty()) {
                callback.finished(null, tokenError == null ? "Не удалось получить доступ к ChatGPT" : tokenError);
                return;
            }
            new Thread(() -> {
                String prompt = null, error = null;
                HttpURLConnection connection = null;
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("model", model);
                    payload.put("instructions",
                        "Ты художник-постановщик The Elder Scrolls V: Skyrim. "
                        + "Верни ТОЛЬКО один финальный художественный промпт на русском языке, 35–90 слов. "
                        + "Используй каноничные названия и подтверждённые визуальные особенности Skyrim. "
                        + "Для локации передай узнаваемую архитектуру, композицию, время суток и свет; "
                        + "для NPC внешность, расу, одежду и ракурс, если они известны; "
                        + "для предмета материал, форму и целостный ракурс. "
                        + "Качественная фотореалистичная визуализация без надписей и UI. "
                        + "Не выдумывай неизвестные канонические детали. "
                        + "Никаких обращений к пользователю, нумерации, списков, markdown, "
                        + "названий файлов, WebP, размеров, инструкций по импорту и слов про Work или API. "
                        + "Не задавай вопросов и не добавляй пояснений.");
                    payload.put("input", new JSONArray().put(new JSONObject()
                        .put("role", "user").put("content", input)));
                    payload.put("stream", true);
                    payload.put("store", false);
                    connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(16000);
                    connection.setReadTimeout(90000);
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    connection.setRequestProperty("Accept", "text/event-stream");
                    connection.setDoOutput(true);
                    try (OutputStream out = connection.getOutputStream()) {
                        out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    int status = connection.getResponseCode();
                    if (status < 200 || status > 299)
                        throw new IllegalStateException("ChatGPT отклонил запрос (HTTP " + status + ")");
                    StringBuilder result = new StringBuilder();
                    boolean complete = false;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data:")) continue;
                            String eventData = line.substring(5).trim();
                            if ("[DONE]".equals(eventData)) break;
                            if (eventData.isEmpty()) continue;
                            JSONObject event;
                            try { event = new JSONObject(eventData); }
                            catch (Exception ignored) { continue; }
                            String type = event.optString("type", "");
                            if ("response.output_text.delta".equals(type)) {
                                String delta = event.optString("delta", "");
                                if (result.length() + delta.length() > 12000)
                                    throw new IllegalStateException("Ответ слишком длинный");
                                result.append(delta);
                            } else if ("response.completed".equals(type)) {
                                complete = true;
                            } else if ("response.failed".equals(type) || "error".equals(type)) {
                                JSONObject failure = event.optJSONObject("error");
                                JSONObject response = event.optJSONObject("response");
                                if (failure == null && response != null)
                                    failure = response.optJSONObject("error");
                                throw new IllegalStateException(failure == null
                                    ? "Генерация промпта прервана"
                                    : crop(failure.optString("message", "Ошибка генерации"), 220));
                            }
                        }
                    }
                    String text = result.toString().trim();
                    if (!complete || text.isEmpty())
                        throw new IllegalStateException("ChatGPT не завершил создание промпта");
                    prompt = text;
                } catch (Exception ex) {
                    error = ex instanceof java.net.SocketTimeoutException ? "Превышено время ожидания ChatGPT"
                        : ex instanceof java.net.UnknownHostException ? "Нет подключения к интернету"
                        : ex.getMessage() == null ? "Ошибка генерации промпта" : ex.getMessage();
                } finally {
                    if (connection != null) connection.disconnect();
                }
                final String answer = prompt, failure = error;
                MAIN.post(() -> {
                    if (!host.isFinishing() && !host.isDestroyed())
                        callback.finished(answer, failure);
                });
            }, "skyrim-work-image-prompt").start();
        });
    }

    private static String crop(String text, int limit) {
        if (text == null) return "";
        String cleaned = text.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").trim();
        return cleaned.length() <= limit ? cleaned : cleaned.substring(0, limit);
    }
}
