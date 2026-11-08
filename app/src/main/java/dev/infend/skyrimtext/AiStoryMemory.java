package dev.infend.skyrimtext;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Permanent per-character narrative log, separate from the authoritative game save. */
final class AiStoryMemory extends SQLiteOpenHelper {
    AiStoryMemory(Context context) {
        super(context, "skyrim_ai_memory.db", null, 1);
        setWriteAheadLoggingEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE turns(id INTEGER PRIMARY KEY AUTOINCREMENT, session TEXT NOT NULL, role TEXT NOT NULL, body TEXT NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_ai_session ON turns(session,id)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future schema migrations must be non-destructive.
    }

    // Preserve complete long-form AI scenes rather than cutting them at 6000 characters.
    private static final int STORED_TURN_LIMIT=32000;
    synchronized void add(String session, String role, String body) {
        if (session == null || session.isEmpty() || body == null || body.trim().isEmpty()) return;
        if (!("user".equals(role) || "master".equals(role) || "event".equals(role))) return;
        ContentValues values = new ContentValues();
        values.put("session", session);
        values.put("role", role);
        values.put("body", body.substring(0, Math.min(STORED_TURN_LIMIT, body.length())));
        values.put("created", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("turns", null, values);
    }

    /** Latest events have priority. Earlier long passages cannot evict the newest turn. */
    synchronized String context(String session) {
        List<String> newest=new ArrayList<>();
        final int limit=18000;
        int used=0;
        try (Cursor c=getReadableDatabase().query("turns",new String[]{"role","body"},
                "session=?",new String[]{session},null,null,"id DESC","26")) {
            while(c.moveToNext()) {
                String role=c.getString(0);
                String body=c.getString(1);
                if(body==null)continue;
                int max="master".equals(role)?3200:900;
                if(body.length()>max){
                    // Keep the scene's opening and its latest dramatic outcome.
                    int prefix="master".equals(role)?500:0;
                    body=prefix>0?body.substring(0,prefix)+"\n…\n"+
                            body.substring(body.length()-(max-prefix-3)):
                            body.substring(body.length()-max);
                }
                String line=("user".equals(role)?"Игрок: ":
                        "event".equals(role)?"Факт движка: ":"Рассказчик: ")+body;
                if(used+line.length()+1>limit)break;
                newest.add(line);
                used+=line.length()+1;
            }
        }
        Collections.reverse(newest);
        StringBuilder output=new StringBuilder();
        for(String line:newest)output.append(line).append('\n');
        return output.toString();
    }

    synchronized String latestAnswer(String session) {
        try (Cursor c = getReadableDatabase().query("turns", new String[]{"body"},
                "session=? AND role='master'", new String[]{session},
                null, null, "id DESC", "1")) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }

    /** JSON export for the latest 500 AI turns, preserving legacy schema-1 saves. */
    synchronized JSONArray exportSession(String session) {
        ArrayList<JSONObject> turns = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("turns", new String[]{"role","body"},
                "session=?", new String[]{session}, null, null, "id DESC", "500")) {
            while (c.moveToNext()) {
                JSONObject row = new JSONObject();
                try { row.put("role", c.getString(0)).put("body", c.getString(1)); }
                catch (Exception ignored) { continue; }
                turns.add(row);
            }
        }
        Collections.reverse(turns);
        JSONArray out = new JSONArray();
        for (JSONObject row : turns) out.put(row);
        return out;
    }

    synchronized void importSession(String session, JSONArray turns) {
        if (turns == null) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int start = Math.max(0, turns.length() - 500);
            for (int i = start; i < turns.length(); i++) {
                JSONObject row = turns.optJSONObject(i);
                if (row == null) continue;
                String role = row.optString("role", "");
                if (!("user".equals(role) || "master".equals(role) || "event".equals(role))) continue;
                ContentValues v = new ContentValues();
                v.put("session", session);
                v.put("role", role);
                String body = row.optString("body", "");
                v.put("body", body.substring(0, Math.min(body.length(), STORED_TURN_LIMIT)));
                v.put("created", System.currentTimeMillis());
                db.insert("turns", null, v);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
}
