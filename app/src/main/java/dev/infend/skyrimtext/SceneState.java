package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.LinkedHashMap;

/**
 * Authoritative, saveable ledger of confirmed AI-scene facts.
 * Narrative generation must never create entries here by itself.
 * The ledger is optional in schema-1 saves and never changes the world atlas.
 */
public final class SceneState {
    private static final int MAX_ENTRIES = 32;
    private final LinkedHashMap<String,JSONObject> npcs = new LinkedHashMap<>();
    private final LinkedHashMap<String,JSONObject> clues = new LinkedHashMap<>();
    private final LinkedHashMap<String,JSONObject> sublocations = new LinkedHashMap<>();

    private static String id(String raw) throws JSONException {
        if(raw==null || !raw.matches("[a-zA-Z0-9_\\-]{1,48}"))
            throw new JSONException("Недопустимый идентификатор сцены");
        return raw;
    }
    private static String label(String raw) throws JSONException {
        if(raw==null || raw.trim().isEmpty() || raw.length()>120)
            throw new JSONException("Недопустимое название сцены");
        return raw.trim();
    }
    private static LinkedHashMap<String,JSONObject> collection(SceneState s,String kind) throws JSONException {
        switch(kind){
            case "npc": return s.npcs;
            case "clue": return s.clues;
            case "sublocation": return s.sublocations;
            default: throw new JSONException("Неизвестный тип факта сцены");
        }
    }
    /** Only explicit, validated engine-side confirmations may add a fact. */
    public void confirm(String kind,String key,String title,String status) throws JSONException {
        LinkedHashMap<String,JSONObject> entries=collection(this,kind);
        String safeId=id(key), safeTitle=label(title);
        if(status==null || !status.matches("[A-Z_]{2,24}"))
            throw new JSONException("Недопустимое состояние факта");
        if(!entries.containsKey(safeId) && entries.size()>=MAX_ENTRIES)
            throw new JSONException("Переполнен журнал сцены");
        entries.put(safeId,new JSONObject().put("id",safeId).put("title",safeTitle).put("status",status));
    }
    public boolean contains(String kind,String key) throws JSONException {
        return collection(this,kind).containsKey(id(key));
    }
    /** Bounded, read-only context of facts confirmed by the engine. */
    public String confirmedContext() {
        StringBuilder out = new StringBuilder();
        for(String kind : new String[]{"npc","clue","sublocation"}) {
            LinkedHashMap<String,JSONObject> entries;
            try { entries = collection(this,kind); }
            catch(JSONException impossible) { continue; }
            for(JSONObject entry : entries.values()) {
                if(out.length()>0)out.append('\n');
                out.append(kind).append(" [").append(entry.optString("id")).append("]: ")
                   .append(entry.optString("title")).append(" (")
                   .append(entry.optString("status")).append(')');
            }
        }
        return out.toString();
    }
    public JSONObject toJson() throws JSONException {
        JSONObject out=new JSONObject();
        for(String kind:new String[]{"npc","clue","sublocation"}){
            JSONArray array=new JSONArray();
            for(JSONObject entry:collection(this,kind).values())array.put(new JSONObject(entry.toString()));
            out.put(kind,array);
        }
        return out;
    }
    private static int collectionSize(SceneState state,String kind) {
        try { return collection(state,kind).size(); }
        catch(JSONException impossible) { return MAX_ENTRIES; }
    }
    public static SceneState fromJson(JSONObject data) {
        SceneState state=new SceneState();
        if(data==null)return state;
        // Corruption in one optional record must not erase valid NPCs, clues or places.
        // Restore up to MAX_ENTRIES valid unique facts, not merely the first 32 array slots.
        // Bound inspection too, so a maliciously huge save cannot stall loading.
        for(String kind:new String[]{"npc","clue","sublocation"}){
            JSONArray arr=data.optJSONArray(kind);
            if(arr==null)continue;
            int limit=Math.min(arr.length(),4096);
            for(int i=0;i<limit && collectionSize(state,kind)<MAX_ENTRIES;i++){
                JSONObject entry=arr.optJSONObject(i);
                if(entry==null)continue;
                try {
                    String key=entry.getString("id");
                    // Duplicate IDs in corrupted saves cannot overwrite the first valid fact.
                    if(state.contains(kind,key))continue;
                    state.confirm(kind,key,entry.getString("title"),entry.getString("status"));
                }catch(JSONException invalid){
                    // Skip only this invalid optional record; preserve the character.
                }
            }
        }
        return state;
    }
}
