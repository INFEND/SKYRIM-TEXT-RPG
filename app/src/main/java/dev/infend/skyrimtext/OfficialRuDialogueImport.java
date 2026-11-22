package dev.infend.skyrimtext;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Imports a locally supplied, validated 33-chapter Russian dialogue JSON pack.
 *  No game files are downloaded or distributed by the application.
 */
final class OfficialRuDialogueImport {
    private static final String FILE_NAME = "imported_story_v7.json";
    private static final String PROVENANCE = "user_supplied_official_russian_game_files";

    private OfficialRuDialogueImport() {}

    static int install(Context context, Uri uri) throws Exception {
        if (uri == null) throw new IllegalArgumentException("Файл не выбран");
        byte[] bytes;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new java.io.IOException("Не удалось открыть файл");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > StoryDialoguePack.MAX_BYTES)
                    throw new java.io.IOException("Пакет превышает 4 МиБ");
                out.write(buffer, 0, count);
            }
            bytes = out.toByteArray();
        }
        String raw = new String(bytes, StandardCharsets.UTF_8);
        if (raw.startsWith("\uFEFF")) raw = raw.substring(1);
        JSONObject metadata = new JSONObject(raw);
        if (!PROVENANCE.equals(metadata.optString("provenance")))
            throw new IllegalArgumentException(
                    "Это не пакет официальных строк, подготовленный импортёром");
        StoryDialoguePack pack = StoryDialoguePack.parse(raw);
        if (pack.chapters().size() != CanonicalCampaign.COUNT)
            throw new IllegalArgumentException("Должны присутствовать все 33 этапа");
        for (int i = 1; i <= CanonicalCampaign.COUNT; i++) {
            if (pack.chapter(i).id != i || pack.chapter(i).lines().isEmpty())
                throw new IllegalArgumentException("Нет реплик этапа " + i);
        }
        File target = new File(context.getFilesDir(), FILE_NAME);
        File temp = new File(context.getFilesDir(), FILE_NAME + ".tmp");
        try {
            try (FileOutputStream file = new FileOutputStream(temp)) {
                file.write(raw.getBytes(StandardCharsets.UTF_8));
                file.getFD().sync();
            }
            try {
                Files.move(temp.toPath(), target.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException atomic) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            CanonicalCampaign.installOriginal(pack);
            return pack.segments;
        } finally {
            if (temp.exists()) temp.delete();
        }
    }
}
