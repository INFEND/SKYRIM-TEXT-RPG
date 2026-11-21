package dev.infend.skyrimtext;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

/**
 * Local image-asset editor for user-created ChatGPT Work images.
 *
 * ChatGPT/Work has no supported third-party image-generation endpoint under a
 * signed-in ChatGPT session. This editor copies an editable prompt and opens
 * the ChatGPT app, then imports the user-selected image through Android SAF.
 * It never reads cookies, access tokens or ChatGPT's private app storage.
 */
public final class WorkImageEditor {
    public static final int REQUEST_IMPORT = 8319;
    public static final int REQUEST_API_EDIT = 8320;
    public static final int REQUEST_WORK_SHARE = 8321;
    private static final String PREF = "work_image_editor";
    private static final String PENDING = "pending_target";
    private static final String PENDING_API = "pending_api_edit";
    private static final String PENDING_WORK = "pending_work_reference";
    private static final int MAX_IMPORT = 5;
    private static final int MAX_IMAGES = 60;
    private static final int WHITE = 0xfff2f2f2, MUTED = 0xffa0a8ac, BG = 0xff080808;
    private WorkImageEditor() {}

    private static android.content.SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
    private static String stableKey(String kind, String id) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest((kind + "\n" + id).getBytes("UTF-8"));
            StringBuilder value = new StringBuilder();
            for (byte b : bytes) value.append(String.format(Locale.ROOT, "%02x", b & 255));
            return value.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }
    private static File directory(Context c, String kind, String id) {
        return new File(new File(c.getFilesDir(), "work_images"), stableKey(kind, id));
    }
    private static JSONObject metadata(Context c, String kind, String id) {
        try {
            String value = prefs(c).getString("asset_" + stableKey(kind, id), "");
            return value.isEmpty() ? new JSONObject() : new JSONObject(value);
        } catch (Exception e) { return new JSONObject(); }
    }
    private static boolean writeMetadata(Context c, String kind, String id, JSONObject data) {
        return prefs(c).edit().putString("asset_" + stableKey(kind, id), data.toString()).commit();
    }
    private static File safeFile(Context c, String kind, String id, String name) {
        if (name == null || !name.matches("[a-f0-9-]{36}\\.webp")) return null;
        return new File(directory(c, kind, id), name);
    }
    public static boolean hasPrimary(Context c, String kind, String id) {
        JSONObject meta = metadata(c, kind, id);
        File file = safeFile(c, kind, id, meta.optString("primary", ""));
        return file != null && file.isFile();
    }
    public static ImageView primaryView(Activity host, String kind, String id, int accent, Runnable onUpdated) {
        JSONObject meta = metadata(host, kind, id);
        File f = safeFile(host, kind, id, meta.optString("primary", ""));
        if (f == null || !f.isFile()) return null;
        Bitmap picture = sampledFile(f, 1600);
        if (picture == null) return null;
        ImageView view = new ImageView(host);
        view.setImageBitmap(picture);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setAdjustViewBounds(true);
        view.setBackgroundColor(BG);
        view.setContentDescription("Пользовательское изображение. Открыть галерею");
        view.setOnClickListener(v -> showGallery(host, kind, id, accent, onUpdated));
        return view;
    }
    private static Bitmap sampledFile(File file, int max) {
        try {
            BitmapFactory.Options info = new BitmapFactory.Options();
            info.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), info);
            if (info.outWidth <= 0 || info.outHeight <= 0) return null;
            info.inSampleSize = 1;
            while (Math.max(info.outWidth / info.inSampleSize, info.outHeight / info.inSampleSize) > max * 2)
                info.inSampleSize *= 2;
            info.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), info);
        } catch (OutOfMemoryError | Exception ex) { return null; }
    }
    private static TextView label(Activity host, String value, int size, int tint) {
        TextView t = new TextView(host);
        t.setText(value);
        t.setTextColor(tint);
        t.setTextSize(size);
        t.setPadding(0, 9, 0, 9);
        return t;
    }
    private static Button button(Activity host, String title, int accent) {
        Button b = new Button(host);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextColor(accent);
        return b;
    }
    private static LinearLayout container(Activity host) {
        LinearLayout l = new LinearLayout(host);
        l.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (host.getResources().getDisplayMetrics().density * 16);
        l.setPadding(padding, padding, padding, padding);
        l.setBackgroundColor(BG);
        return l;
    }
    private static String prompt(String kind, String name, String facts, int count) {
        String core;
        if ("npc".equals(kind))
            core = "Создай каноничный высокодетализированный портрет персонажа " + name +
                " из The Elder Scrolls V: Skyrim. " + facts +
                " Реалистичная анатомия, характерная одежда, ракурс от волос до живота.";
        else if ("item".equals(kind))
            core = "Создай предметную иллюстрацию " + name + " из Skyrim. " + facts +
                " Сохрани узнаваемую форму, материалы и пропорции. Предмет целиком, без обрезания.";
        else
            core = "Создай фотореалистичное изображение локации " + name +
                " из The Elder Scrolls V: Skyrim. " + facts +
                " Сохрани каноничную географию, архитектуру и узнаваемые ориентиры.";
        if ("location".equals(kind) && count == 5 &&
            ("Солитьюд".equals(name) || "Вайтран".equals(name) || "Рифтен".equals(name) ||
             "Маркарт".equals(name) || "Виндхельм".equals(name) || "Фолкрит".equals(name) ||
             "Ривервуд".equals(name) || "Винтерхолд".equals(name) ||
             "Данстар".equals(name) || "Морфал".equals(name)))
            core += "\nПять отдельных каноничных ракурсов города: 1) главный вход, " +
                "2) сразу после входа, 3) рынок или главная улица, 4) центр, " +
                "5) узнаваемая уникальная достопримечательность. Без коллажей.";
        return core + "\nВысочайшая детализация, современная next-gen графика, естественное освещение, " +
            "единый художественный стиль, без текста и элементов интерфейса." +
            (count > 1 ? "\nПодготовь " + count + " отдельных разных вариантов, не коллаж." : "") +
            "\nИзображение будет импортировано в игру и оптимизировано в WebP.";
    }
    private static void copyPrompt(Activity host, String value) {
        ClipboardManager clip = (ClipboardManager) host.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clip != null) clip.setPrimaryClip(ClipData.newPlainText("SKYRIM · Work", value));
    }
    private static void launchChatGpt(Activity host, String value) {
        copyPrompt(host, value);
        Intent launch = host.getPackageManager().getLaunchIntentForPackage("com.openai.chatgpt");
        if (launch == null) launch = new Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/"));
        try {
            host.startActivity(launch);
            Toast.makeText(host, "Описание скопировано. Выбери Work и вставь текст.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(host, "Текст скопирован. Открой ChatGPT вручную.", Toast.LENGTH_LONG).show();
        }
    }
    private static String promptKey(String kind, String id) {
        return "draft_" + stableKey(kind, id);
    }

    /** An explicit share is the only supported way to pass a picture to ChatGPT. */
    private static void shareWithChatGpt(Activity host, Uri source, String description) {
        copyPrompt(host, description);
        if (source == null) { launchChatGpt(host, description); return; }
        Intent share = new Intent(Intent.ACTION_SEND);
        String type = host.getContentResolver().getType(source);
        share.setType(type != null && type.startsWith("image/") ? type : "image/*");
        share.putExtra(Intent.EXTRA_STREAM, source);
        share.putExtra(Intent.EXTRA_TEXT, description);
        share.setClipData(ClipData.newUri(host.getContentResolver(), "SKYRIM", source));
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent direct = new Intent(share);
        direct.setPackage("com.openai.chatgpt");
        try {
            if (direct.resolveActivity(host.getPackageManager()) != null) host.startActivity(direct);
            else host.startActivity(Intent.createChooser(share, "Отправить исходник в ChatGPT"));
            Toast.makeText(host, "Описание скопировано. Выбери Work и при необходимости вставь текст.",
                Toast.LENGTH_LONG).show();
        } catch (Exception failure) {
            launchChatGpt(host, description);
        }
    }

    private static void selectReference(Activity host, String pendingKey, int request,
                                        String kind, String id, String name, String prompt,
                                        String model, String quality, String size, int count) {
        try {
            JSONObject pending = new JSONObject().put("kind", kind).put("id", id)
                .put("name", name).put("prompt", prompt).put("model", model)
                .put("quality", quality).put("size", size).put("count", count);
            if (!prefs(host).edit().putString(pendingKey, pending.toString()).commit())
                throw new IllegalStateException("Не удалось запомнить объект");
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.setType("image/*");
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            host.startActivityForResult(pick, request);
        } catch (Exception failure) {
            Toast.makeText(host, "Не удалось открыть файл: " + failure.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void showApiKeyEditor(Activity host) {
        final EditText entry = new EditText(host);
        entry.setSingleLine(true);
        entry.setTextColor(WHITE);
        entry.setHint("sk-... (сохраняется только на устройстве)");
        entry.setHintTextColor(MUTED);
        entry.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout panel = container(host);
        panel.addView(label(host, "Используется тот же API-ключ, что и для текстового OpenAI API. "
            + "Он шифруется через Android Keystore и никогда не передаётся в ChatGPT Work.", 13, MUTED));
        panel.addView(entry);
        new AlertDialog.Builder(host).setTitle("OPENAI API-КЛЮЧ")
            .setView(panel).setPositiveButton("Сохранить", (dialog, which) -> {
                try {
                    AiKeyVault.save(host, entry.getText().toString());
                    Toast.makeText(host, "API-ключ сохранён", Toast.LENGTH_SHORT).show();
                } catch (Exception failure) {
                    Toast.makeText(host, "Ключ не сохранён: " + failure.getMessage(),
                        Toast.LENGTH_LONG).show();
                }
            }).setNegativeButton("Отмена", null).show();
    }

    private static JSONObject requestTarget(String kind, String id, String name, String prompt,
                                             String model, String quality, String size, int count) throws Exception {
        return new JSONObject().put("kind", kind).put("id", id)
            .put("name", name).put("prompt", prompt).put("model", model)
            .put("quality", quality).put("size", size).put("count", count);
    }

    private static void confirmApi(Activity host, JSONObject target, Uri reference,
                                   int accent, Runnable onUpdated) {
        if (!AiKeyVault.configured(host)) {
            Toast.makeText(host, "Сначала добавь OpenAI API-ключ", Toast.LENGTH_LONG).show();
            showApiKeyEditor(host);
            return;
        }
        if (ImageApiClient.busy()) {
            Toast.makeText(host, "Предыдущая генерация ещё выполняется", Toast.LENGTH_LONG).show();
            return;
        }
        String model = target.optString("model", "gpt-image-2.6-sunburst");
        String quality = target.optString("quality", "high");
        int count = target.optInt("count", 1);
        new AlertDialog.Builder(host).setTitle(reference == null
                ? "Создать изображения через API?" : "Изменить исходное изображение через API?")
            .setMessage("OpenAI Images API оплачивается отдельно от подписки ChatGPT."
                + "\nМодель: " + model + "\nКачество: " + quality + "\nКоличество: " + count
                + "\nЗапрос выполняется один раз, без платных автоматических повторов. "
                + "Изображения заменятся только после отдельного подтверждения.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Запустить платный запрос", (dialog, which) -> {
                Toast.makeText(host, "OpenAI API: выполняется запрос…", Toast.LENGTH_LONG).show();
                ImageApiClient.request(host, target.optString("prompt"), model, quality,
                    target.optString("size"), count, reference,
                    (images, error) -> {
                        if (host.isDestroyed() || host.isFinishing()) return;
                        if (error != null) {
                            new AlertDialog.Builder(host).setTitle("Ошибка OpenAI API")
                                .setMessage(error).setPositiveButton("Закрыть", null).show();
                            return;
                        }
                        if (images == null || images.isEmpty()) {
                            Toast.makeText(host, "Пустой ответ API", Toast.LENGTH_LONG).show();
                            return;
                        }
                        LinearLayout review = container(host);
                        review.addView(label(host, "Получено: " + images.size()
                            + ". Изображения ещё не установлены в игру.", 13, WHITE));
                        Bitmap picture = BitmapFactory.decodeByteArray(images.get(0), 0, images.get(0).length);
                        if (picture != null) {
                            ImageView preview = new ImageView(host);
                            preview.setImageBitmap(picture);
                            preview.setAdjustViewBounds(true);
                            review.addView(preview, new LinearLayout.LayoutParams(-1, -2));
                        }
                        new AlertDialog.Builder(host).setTitle("ПРЕДПРОСМОТР API")
                            .setView(review)
                            .setPositiveButton("Заменить основное", (a, b) ->
                                installApi(host, target, images, true, onUpdated))
                            .setNeutralButton("Добавить в галерею", (a, b) ->
                                installApi(host, target, images, false, onUpdated))
                            .setNegativeButton("Не сохранять", null).show();
                    });
            }).show();
    }

    /** Store API WebP output without a second lossy recompression. */
    private static void installApi(Activity host, JSONObject target, List<byte[]> pictures,
                                   boolean makePrimary, Runnable onUpdated) {
        new Thread(() -> {
            String kind = target.optString("kind"), id = target.optString("id");
            File folder = directory(host, kind, id);
            ArrayList<File> newFiles = new ArrayList<>();
            try {
                if (!folder.isDirectory() && !folder.mkdirs())
                    throw new IllegalStateException("Не удалось создать каталог изображений");
                JSONObject meta = metadata(host, kind, id);
                JSONArray images = meta.optJSONArray("images");
                if (images == null) images = new JSONArray();
                if (images.length() + pictures.size() > MAX_IMAGES)
                    throw new IllegalStateException("Превышен лимит " + MAX_IMAGES + " изображений");
                String primary = null;
                for (byte[] bytes : pictures) {
                    if (bytes == null || bytes.length < 16 || bytes.length > 24 * 1024 * 1024)
                        throw new IllegalStateException("Размер ответа превышает 24 МБ");
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                    if (bounds.outWidth < 32 || bounds.outHeight < 32
                            || (long) bounds.outWidth * bounds.outHeight > 120000000L)
                        throw new IllegalStateException("Повреждённое изображение");
                    String filename = UUID.randomUUID().toString() + ".webp";
                    File dest = new File(folder, filename);
                    newFiles.add(dest);
                    try (FileOutputStream output = new FileOutputStream(dest)) { output.write(bytes); }
                    if (primary == null) primary = filename;
                    images.put(new JSONObject().put("file", filename)
                        .put("prompt", target.optString("prompt"))
                        .put("source", "openai-images-api:" + target.optString("model"))
                        .put("created", System.currentTimeMillis())
                        .put("bytes", dest.length()).put("width", bounds.outWidth)
                        .put("height", bounds.outHeight));
                }
                meta.put("images", images);
                if (makePrimary && primary != null) meta.put("primary", primary);
                if (!writeMetadata(host, kind, id, meta))
                    throw new IllegalStateException("Не удалось сохранить галерею");
                host.runOnUiThread(() -> {
                    if (host.isFinishing() || host.isDestroyed()) return;
                    Toast.makeText(host, makePrimary ? "Основное изображение заменено"
                        : "Изображения добавлены в галерею", Toast.LENGTH_LONG).show();
                    if (onUpdated != null) onUpdated.run();
                });
            } catch (Exception | OutOfMemoryError failure) {
                for (File file : newFiles) file.delete();
                host.runOnUiThread(() -> Toast.makeText(host,
                    "Изображения не сохранены: " + failure.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "skyrim-api-images-save").start();
    }

    public static void open(Activity host, String kind, String id, String name, String facts,
                            int accent, Runnable onUpdated) {
        if (id == null || id.trim().isEmpty()) {
            Toast.makeText(host, "Не указан объект изображения", Toast.LENGTH_SHORT).show();
            return;
        }
        int count = prefs(host).getInt("count", 1);
        String key = promptKey(kind, id);
        LinearLayout panel = container(host);
        panel.addView(label(host, name, 18, accent));
        panel.addView(label(host,
            "Описание можно изменить полностью. ChatGPT Work откроется отдельно; после генерации сохрани файл и импортируй его.",
            12, MUTED));
        EditText editor = new EditText(host);
        editor.setTextColor(WHITE);
        editor.setHintTextColor(MUTED);
        editor.setTextSize(15);
        editor.setMinLines(5);
        editor.setMaxLines(12);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setSingleLine(false);
        editor.setHint("Описание изображения");
        editor.setText(prefs(host).getString(key, prompt(kind, name, facts, count)));
        panel.addView(editor, new LinearLayout.LayoutParams(-1, -2));
        editor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int after) {
                prefs(host).edit().putString(key, s.toString()).apply();
            }
            public void afterTextChanged(Editable s) {}
        });
        panel.addView(label(host, "Вариантов: 1 / 3 / 5", 12, MUTED));
        LinearLayout counts = new LinearLayout(host);
        counts.setOrientation(LinearLayout.HORIZONTAL);
        final int[] requested = {count};
        for (int n : new int[]{1, 3, 5}) {
            Button choice = button(host, String.valueOf(n), accent);
            choice.setAlpha(count == n ? 1f : 0.55f);
            counts.addView(choice, new LinearLayout.LayoutParams(0, -2, 1));
            choice.setOnClickListener(v -> {
                requested[0] = n;
                prefs(host).edit().putInt("count", n).apply();
                for (int j = 0; j < counts.getChildCount(); j++)
                    counts.getChildAt(j).setAlpha(j == (n == 1 ? 0 : n == 3 ? 1 : 2) ? 1f : 0.55f);
            });
        }
        panel.addView(counts);
        Button reset = button(host, "Восстановить автоматическое описание", accent);
        panel.addView(reset);
        reset.setOnClickListener(v -> editor.setText(prompt(kind, name, facts, requested[0])));
        Button copy = button(host, "Копировать описание", accent);
        panel.addView(copy);
        copy.setOnClickListener(v -> {
            String value = editor.getText().toString().trim();
            if (value.isEmpty()) { editor.setError("Напиши описание"); return; }
            copyPrompt(host, "Создай " + requested[0] + (requested[0] == 1 ? " изображение. " : requested[0] == 3 ? " изображения. " : " изображений. ") + value);
            Toast.makeText(host, "Описание скопировано", Toast.LENGTH_SHORT).show();
        });
        Button open = button(host, "Открыть ChatGPT / Work", accent);
        panel.addView(open);
        open.setOnClickListener(v -> {
            String value = editor.getText().toString().trim();
            if (value.isEmpty()) { editor.setError("Напиши описание"); return; }
            try {
                JSONObject pending = new JSONObject();
                pending.put("kind", kind).put("id", id).put("name", name)
                    .put("prompt", value).put("count", requested[0]);
                if (!prefs(host).edit().putString(PENDING, pending.toString()).commit())
                    throw new IllegalStateException("Не удалось сохранить цель передачи");
            } catch (Exception e) {
                Toast.makeText(host, "Ошибка подготовки Work: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
                return;
            }
            launchChatGpt(host, "Создай " + requested[0] + (requested[0] == 1 ? " отдельное изображение. " : requested[0] == 3 ? " отдельных изображения. " : " отдельных изображений. ") + value);
        });
        panel.addView(label(host, "CHATGPT WORK  |  ИСХОДНАЯ КАРТИНКА", 13, accent));
        Button shareReference = button(host, "Отправить картинку в ChatGPT / Work", accent);
        panel.addView(shareReference);
        shareReference.setOnClickListener(v -> {
            String description = editor.getText().toString().trim();
            if (description.isEmpty()) { editor.setError("Напиши описание"); return; }
            JSONObject meta = metadata(host, kind, id);
            File primary = safeFile(host, kind, id, meta.optString("primary", ""));
            String workText = "Отредактируй приложенное изображение для Skyrim. " + description;
            try {
                JSONObject pending = new JSONObject().put("kind", kind).put("id", id)
                    .put("name", name).put("prompt", workText).put("count", 1);
                if (!prefs(host).edit().putString(PENDING, pending.toString()).commit())
                    throw new IllegalStateException("Не удалось сохранить цель изображения");
            } catch (Exception error) {
                Toast.makeText(host, "Ошибка подготовки Work: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
                return;
            }
            if (primary != null && primary.isFile()) {
                new AlertDialog.Builder(host).setTitle("ИСХОДНОЕ ИЗОБРАЖЕНИЕ")
                    .setItems(new String[]{"Использовать текущее основное",
                        "Выбрать другое изображение"}, (d, choice) -> {
                        if (choice == 0)
                            shareWithChatGpt(host, ImageShareProvider.uriFor(host, primary), workText);
                        else selectReference(host, PENDING_WORK, REQUEST_WORK_SHARE,
                            kind, id, name, workText, "", "", "", 1);
                    }).show();
            } else selectReference(host, PENDING_WORK, REQUEST_WORK_SHARE,
                kind, id, name, workText, "", "", "", 1);
        });

        panel.addView(label(host, "OPENAI IMAGES API  |  ПРЯМАЯ ГЕНЕРАЦИЯ", 13, accent));
        panel.addView(label(host, "API оплачивается отдельно. Исходник передаётся в OpenAI "
            + "только при выборе редактирования. Результат добавляется после предпросмотра.", 12, MUTED));
        Button apiKeyButton = button(host, "Настроить API-ключ", accent);
        panel.addView(apiKeyButton);
        apiKeyButton.setOnClickListener(v -> showApiKeyEditor(host));
        String[] modelIds = {"gpt-image-2.6-sunburst", "gpt-image-2.6-flare",
            "gpt-image-2.5-sunburst", "gpt-image-2.5-flare", "gpt-image-1.5"};
        String[] modelNames = {"Sunburst 2.6 · максимум качества", "Flare 2.6 · быстрее",
            "Sunburst 2.5 · точное редактирование", "Flare 2.5 · быстрее",
            "GPT Image 1.5 · совместимость"};
        final int[] selectedModel = {0};
        String savedModel = prefs(host).getString("image_api_model", modelIds[0]);
        for (int i = 0; i < modelIds.length; i++)
            if (modelIds[i].equals(savedModel)) selectedModel[0] = i;
        Button modelButton = button(host, "Модель: " + modelNames[selectedModel[0]], accent);
        panel.addView(modelButton);
        modelButton.setOnClickListener(v -> new AlertDialog.Builder(host)
            .setTitle("Модель изображений").setSingleChoiceItems(modelNames, selectedModel[0],
                (dialog, index) -> {
                    selectedModel[0] = index;
                    modelButton.setText("Модель: " + modelNames[index]);
                    prefs(host).edit().putString("image_api_model", modelIds[index]).apply();
                    dialog.dismiss();
                }).setNegativeButton("Отмена", null).show());
        String[] qualities = {"high", "medium", "low"};
        String[] qualityNames = {"Максимальное", "Среднее", "Экономное"};
        final int[] selectedQuality = {0};
        Button qualityButton = button(host, "Качество: " + qualityNames[0], accent);
        panel.addView(qualityButton);
        qualityButton.setOnClickListener(v -> new AlertDialog.Builder(host)
            .setTitle("Качество API").setSingleChoiceItems(qualityNames, selectedQuality[0],
                (dialog, index) -> {
                    selectedQuality[0] = index;
                    qualityButton.setText("Качество: " + qualityNames[index]);
                    dialog.dismiss();
                }).setNegativeButton("Отмена", null).show());
        String resolution = "npc".equals(kind) ? "1024x1536"
            : "item".equals(kind) ? "1024x1024" : "1536x1024";
        panel.addView(label(host, "Разрешение: " + resolution
            + " · выход WebP, сжатие 90%", 12, MUTED));
        Button generate = button(host, "API · Создать новое", accent);
        panel.addView(generate);
        generate.setOnClickListener(v -> {
            String description = editor.getText().toString().trim();
            if (description.isEmpty()) { editor.setError("Напиши описание"); return; }
            try {
                confirmApi(host, requestTarget(kind, id, name, description,
                    modelIds[selectedModel[0]], qualities[selectedQuality[0]],
                    resolution, requested[0]), null, accent, onUpdated);
            } catch (Exception error) {
                Toast.makeText(host, "Ошибка подготовки запроса", Toast.LENGTH_LONG).show();
            }
        });
        Button editApi = button(host, "API · Изменить по исходнику", accent);
        panel.addView(editApi);
        editApi.setOnClickListener(v -> {
            String description = editor.getText().toString().trim();
            if (description.isEmpty()) { editor.setError("Опиши изменения"); return; }
            try {
                JSONObject target = requestTarget(kind, id, name, description,
                    modelIds[selectedModel[0]], qualities[selectedQuality[0]],
                    resolution, requested[0]);
                JSONObject meta = metadata(host, kind, id);
                File primary = safeFile(host, kind, id, meta.optString("primary", ""));
                if (primary != null && primary.isFile()) {
                    new AlertDialog.Builder(host).setTitle("ИЗМЕНИТЬ ИЗОБРАЖЕНИЕ")
                        .setItems(new String[]{"Текущее основное", "Выбрать исходник из файлов"},
                            (d, choice) -> {
                                if (choice == 0) confirmApi(host, target, Uri.fromFile(primary),
                                    accent, onUpdated);
                                else selectReference(host, PENDING_API, REQUEST_API_EDIT, kind, id,
                                    name, description, modelIds[selectedModel[0]],
                                    qualities[selectedQuality[0]], resolution, requested[0]);
                            }).show();
                } else selectReference(host, PENDING_API, REQUEST_API_EDIT, kind, id,
                    name, description, modelIds[selectedModel[0]],
                    qualities[selectedQuality[0]], resolution, requested[0]);
            } catch (Exception error) {
                Toast.makeText(host, "Ошибка подготовки редактирования", Toast.LENGTH_LONG).show();
            }
        });

        Button importButton = button(host, "Импортировать изображение", accent);
        panel.addView(importButton);
        importButton.setOnClickListener(v -> {
            try {
                JSONObject pending = new JSONObject();
                pending.put("kind", kind).put("id", id).put("name", name)
                    .put("prompt", editor.getText().toString()).put("count", requested[0]);
                if (!prefs(host).edit().putString(PENDING, pending.toString()).commit())
                    throw new IllegalStateException("Не удалось сохранить выбор");
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.setType("image/*");
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                host.startActivityForResult(pick, REQUEST_IMPORT);
            } catch (Exception e) {
                Toast.makeText(host, "Не удалось открыть импорт: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        Button gallery = button(host, "Мои изображения / выбрать основное", accent);
        panel.addView(gallery);
        gallery.setOnClickListener(v -> showGallery(host, kind, id, accent, onUpdated));
        ScrollView scroll = new ScrollView(host);
        scroll.addView(panel);
        new AlertDialog.Builder(host).setTitle("ИЗОБРАЖЕНИЯ  |  WORK + API")
            .setView(scroll).setNegativeButton("Закрыть", null).show();
    }
    /** User-initiated Android Sharesheet return path from ChatGPT Work. */
    public static boolean handleSharedIntent(Activity host, Intent intent, int accent, Runnable onUpdated) {
        if (intent == null) return false;
        String action = intent.getAction(), type = intent.getType();
        if (!(Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action))
            || type == null || !type.startsWith("image/")) return false;
        ArrayList<Uri> files = new ArrayList<>();
        ClipData clip = intent.getClipData();
        if (clip != null) for (int i = 0; i < Math.min(MAX_IMPORT, clip.getItemCount()); i++) {
            Uri uri = clip.getItemAt(i).getUri(); if (uri != null) files.add(uri);
        }
        if (files.isEmpty()) {
            try {
                if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    ArrayList<Uri> batch = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    if (batch != null) for (Uri uri : batch) {
                        if (uri != null && files.size() < MAX_IMPORT) files.add(uri);
                    }
                } else {
                    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (uri != null) files.add(uri);
                }
            } catch (Exception ignored) {}
        }
        if (files.isEmpty() && intent.getData() != null) files.add(intent.getData());
        if (files.isEmpty()) return true;
        String pending = prefs(host).getString(PENDING, "");
        if (pending.isEmpty()) {
            new AlertDialog.Builder(host).setTitle("Изображение из ChatGPT")
                .setMessage("Сначала открой редактор нужной локации, NPC или предмета"
                    + " и запусти ChatGPT / Work из редактора.")
                .setPositiveButton("Понятно", null).show();
            return true;
        }
        try {
            JSONObject target = new JSONObject(pending);
            if (target.optString("id").isEmpty()) return true;
            LinearLayout panel = container(host);
            panel.addView(label(host, "Цель: " + target.optString("name")
                + "\nИзображений: " + files.size() + "\nПроверь правильность выбранной цели.", 14, WHITE));
            try {
                Bitmap preview = readScaled(host, files.get(0), 1000);
                if (preview != null) {
                    ImageView art = new ImageView(host);
                    art.setImageBitmap(preview); art.setAdjustViewBounds(true);
                    panel.addView(art, new LinearLayout.LayoutParams(-1, -2));
                }
            } catch (Exception ignored) {}
            final ArrayList<Uri> selected = files;
            new AlertDialog.Builder(host).setTitle("ИМПОРТ ИЗ CHATGPT / WORK")
                .setView(panel)
                .setPositiveButton("Заменить основное", (d, w) ->
                    installAsync(host, target, selected, true, onUpdated, "chatgpt-work-share"))
                .setNeutralButton("Добавить в галерею", (d, w) ->
                    installAsync(host, target, selected, false, onUpdated, "chatgpt-work-share"))
                .setNegativeButton("Отмена", null).show();
        } catch (Exception error) {
            Toast.makeText(host, "Невозможно импортировать изображения из Work",
                Toast.LENGTH_LONG).show();
        }
        return true;
    }
    public static boolean onActivityResult(Activity host, int request, int result, Intent data,
                                           int accent, Runnable onUpdated) {
        if (request == REQUEST_WORK_SHARE || request == REQUEST_API_EDIT) {
            final String pendingKey = request == REQUEST_WORK_SHARE ? PENDING_WORK : PENDING_API;
            String saved = prefs(host).getString(pendingKey, "");
            prefs(host).edit().remove(pendingKey).apply();
            if (result == Activity.RESULT_OK && data != null && data.getData() != null && !saved.isEmpty()) {
                try {
                    JSONObject target = new JSONObject(saved);
                    if (request == REQUEST_WORK_SHARE)
                        shareWithChatGpt(host, data.getData(), target.optString("prompt"));
                    else confirmApi(host, target, data.getData(), accent, onUpdated);
                } catch (Exception error) {
                    Toast.makeText(host, "Не удалось обработать исходник", Toast.LENGTH_LONG).show();
                }
            }
            return true;
        }
        if (request != REQUEST_IMPORT) return false;
        String saved = prefs(host).getString(PENDING, "");
        if (result != Activity.RESULT_OK || data == null || saved.isEmpty()) return true;
        try {
            JSONObject target = new JSONObject(saved);
            ArrayList<Uri> files = new ArrayList<>();
            if (data.getClipData() != null) {
                ClipData list = data.getClipData();
                for (int i = 0; i < Math.min(MAX_IMPORT, list.getItemCount()); i++)
                    files.add(list.getItemAt(i).getUri());
            } else if (data.getData() != null) files.add(data.getData());
            if (files.isEmpty()) return true;
            LinearLayout panel = container(host);
            panel.addView(label(host, "Файлов: " + files.size() + ". Проверь результат до замены.", 14, WHITE));
            try {
                Bitmap preview = readScaled(host, files.get(0), 1000);
                if (preview != null) {
                    ImageView image = new ImageView(host);
                    image.setImageBitmap(preview);
                    image.setAdjustViewBounds(true);
                    panel.addView(image, new LinearLayout.LayoutParams(-1, -2));
                }
            } catch (Exception ignored) {}
            new AlertDialog.Builder(host)
                .setTitle("Импорт: " + target.optString("name"))
                .setView(panel)
                .setPositiveButton("Заменить основное", (d, w) ->
                    installAsync(host, target, files, true, onUpdated, "chatgpt-work-manual"))
                .setNeutralButton("Добавить в галерею", (d, w) ->
                    installAsync(host, target, files, false, onUpdated, "chatgpt-work-manual"))
                .setNegativeButton("Отмена", null).show();
        } catch (Exception e) {
            Toast.makeText(host, "Ошибка подготовки импорта", Toast.LENGTH_LONG).show();
        }
        return true;
    }
    private static Bitmap readScaled(Context c, Uri uri, int limit) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = c.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalArgumentException("Файл недоступен");
            BitmapFactory.decodeStream(input, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0)
            throw new IllegalArgumentException("Неподдерживаемое изображение");
        if ((long) options.outWidth * options.outHeight > 120000000L)
            throw new IllegalArgumentException("Изображение слишком большое");
        options.inSampleSize = 1;
        while (Math.max(options.outWidth / options.inSampleSize,
                        options.outHeight / options.inSampleSize) > limit * 2)
            options.inSampleSize *= 2;
        options.inJustDecodeBounds = false;
        Bitmap bitmap;
        try (InputStream input = c.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalArgumentException("Не удалось прочитать файл");
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) throw new IllegalArgumentException("Ошибка декодирования");
        int largest = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (largest > limit) {
            float scale = limit / (float) largest;
            Bitmap resized = Bitmap.createScaledBitmap(bitmap,
                Math.max(1, Math.round(bitmap.getWidth() * scale)),
                Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
            if (resized != bitmap) bitmap.recycle();
            return resized;
        }
        return bitmap;
    }
    private static void installAsync(Activity host, JSONObject target, ArrayList<Uri> inputs,
                                     boolean makePrimary, Runnable onUpdated, String origin) {
        new Thread(() -> {
            String kind = target.optString("kind"), id = target.optString("id");
            File folder = directory(host, kind, id);
            ArrayList<File> newFiles = new ArrayList<>();
            try {
                if (!folder.isDirectory() && !folder.mkdirs())
                    throw new IllegalStateException("Не удалось создать каталог");
                JSONObject meta = metadata(host, kind, id);
                JSONArray images = meta.optJSONArray("images");
                if (images == null) images = new JSONArray();
                if (images.length() + inputs.size() > MAX_IMAGES)
                    throw new IllegalStateException("Превышен лимит " + MAX_IMAGES + " файлов");
                String first = null;
                for (Uri uri : inputs) {
                    Bitmap bitmap = readScaled(host, uri, 2048);
                    int width = bitmap.getWidth(), height = bitmap.getHeight();
                    String filename = UUID.randomUUID().toString() + ".webp";
                    File dest = new File(folder, filename);
                    newFiles.add(dest);
                    try (FileOutputStream stream = new FileOutputStream(dest)) {
                        boolean saved = bitmap.compress(Bitmap.CompressFormat.WEBP, 88, stream);
                        if (!saved) throw new IllegalStateException("Не удалось записать WebP");
                    } finally { bitmap.recycle(); }
                    if (first == null) first = filename;
                    JSONObject record = new JSONObject()
                        .put("file", filename)
                        .put("prompt", target.optString("prompt"))
                        .put("source", origin).put("model", target.optString("model", ""))
                        .put("created", System.currentTimeMillis())
                        .put("bytes", dest.length())
                        .put("width", width)
                        .put("height", height);
                    images.put(record);
                }
                meta.put("images", images);
                if (makePrimary && first != null) meta.put("primary", first);
                if (!writeMetadata(host, kind, id, meta))
                    throw new IllegalStateException("Не удалось сохранить метаданные");
                final int count = newFiles.size();
                host.runOnUiThread(() -> {
                    if (host.isFinishing() || host.isDestroyed()) return;
                    prefs(host).edit().remove(PENDING).apply();
                    Toast.makeText(host, makePrimary ? "Основное изображение заменено" : "Добавлено: " + count,
                        Toast.LENGTH_LONG).show();
                    if (onUpdated != null) onUpdated.run();
                });
            } catch (Exception | OutOfMemoryError error) {
                for (File file : newFiles) file.delete();
                host.runOnUiThread(() -> Toast.makeText(host,
                    "Импорт не выполнен: " + error.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                if ("openai-api".equals(origin)) for (Uri uri : inputs) {
                    if ("file".equals(uri.getScheme())) {
                        File temporary = new File(uri.getPath());
                        if (temporary.getParentFile().equals(host.getCacheDir())
                            && temporary.getName().startsWith("skyrim_api_")) temporary.delete();
                    }
                }
            }
        }, "work-image-import").start();
    }
    public static void showGallery(Activity host, String kind, String id, int accent,
                                   Runnable onUpdated) {
        JSONObject meta = metadata(host, kind, id);
        JSONArray images = meta.optJSONArray("images");
        if (images == null || images.length() == 0) {
            new AlertDialog.Builder(host).setTitle("Галерея")
                .setMessage("Пока нет импортированных изображений")
                .setPositiveButton("Закрыть", null).show();
            return;
        }
        final JSONArray galleryImages = images;
        String[] titles = new String[galleryImages.length() + 1];
        titles[0] = "Вернуть встроенное изображение";
        for (int i = 0; i < galleryImages.length(); i++) {
            JSONObject record = galleryImages.optJSONObject(i);
            boolean selected = record != null && record.optString("file")
                .equals(meta.optString("primary"));
            long date = record == null ? 0 : record.optLong("created");
            titles[i + 1] = (selected ? "★ " : "") + "Изображение " + (i + 1) +
                (date > 0 ? " · " + DateFormat.getDateTimeInstance().format(new Date(date)) : "");
        }
        new AlertDialog.Builder(host).setTitle("МОЯ ГАЛЕРЕЯ")
            .setItems(titles, (d, which) -> {
                if (which == 0) {
                    try {
                        meta.remove("primary");
                        if (!writeMetadata(host, kind, id, meta)) throw new Exception();
                        if (onUpdated != null) onUpdated.run();
                    } catch (Exception e) {
                        Toast.makeText(host, "Не удалось восстановить оригинал", Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                int index = which - 1;
                JSONObject record = galleryImages.optJSONObject(index);
                if (record == null) return;
                String filename = record.optString("file", "");
                File imageFile = safeFile(host, kind, id, filename);
                if (imageFile == null || !imageFile.isFile()) {
                    Toast.makeText(host, "Файл изображения отсутствует", Toast.LENGTH_LONG).show();
                    return;
                }
                LinearLayout panel = container(host);
                Bitmap bitmap = sampledFile(imageFile, 1200);
                if (bitmap != null) {
                    ImageView preview = new ImageView(host);
                    preview.setImageBitmap(bitmap);
                    preview.setAdjustViewBounds(true);
                    panel.addView(preview, new LinearLayout.LayoutParams(-1, -2));
                }
                panel.addView(label(host, "Размер: " + Math.round(imageFile.length() / 1024f) +
                    " КБ · источник: " + (record.optString("source").startsWith("openai-images-api")
                    ? "OpenAI API" : "ChatGPT Work"), 12, MUTED));
                new AlertDialog.Builder(host).setTitle("Изображение " + (index + 1))
                    .setView(panel)
                    .setPositiveButton("Сделать основным", (x, y) -> {
                        try {
                            meta.put("primary", filename);
                            if (!writeMetadata(host, kind, id, meta)) throw new Exception();
                            if (onUpdated != null) onUpdated.run();
                        } catch (Exception e) {
                            Toast.makeText(host, "Не удалось обновить выбор", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNeutralButton("Удалить", (x, y) ->
                        new AlertDialog.Builder(host).setTitle("Удалить изображение?")
                            .setMessage("Файл будет удалён только из пользовательской галереи.")
                            .setNegativeButton("Отмена", null)
                            .setPositiveButton("Удалить", (confirm, z) -> {
                                try {
                                    JSONArray updated = new JSONArray();
                                    for (int j = 0; j < galleryImages.length(); j++)
                                        if (j != index) updated.put(galleryImages.opt(j));
                                    meta.put("images", updated);
                                    if (filename.equals(meta.optString("primary"))) meta.remove("primary");
                                    if (!writeMetadata(host, kind, id, meta)) throw new Exception();
                                    imageFile.delete();
                                    if (onUpdated != null) onUpdated.run();
                                } catch (Exception e) {
                                    Toast.makeText(host, "Не удалось удалить изображение", Toast.LENGTH_LONG).show();
                                }
                            }).show())
                    .setNegativeButton("Закрыть", null).show();
            }).setNegativeButton("Закрыть", null).show();
    }
}