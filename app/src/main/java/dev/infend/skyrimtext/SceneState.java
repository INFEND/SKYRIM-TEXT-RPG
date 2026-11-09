package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

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
    public JSONObject toJson() throws JSONException {
        JSONObject out=new JSONObject();
        for(String kind:new String[]{"npc","clue","sublocation"}){
            JSONArray array=new JSONArray();
            for(JSONObject entry:collection(this,kind).values())array.put(new JSONObject(entry.toString()));
            out.put(kind,array);
        }
        return out;
    }
    public static SceneState fromJson(JSONObject data) {
        SceneState state=new SceneState();
        if(data==null)return state;
        try {
            for(String kind:new String[]{"npc","clue","sublocation"}){
                JSONArray arr=data.optJSONArray(kind);
                if(arr==null)continue;
                if(arr.length()>MAX_ENTRIES)throw new JSONException("Слишком много фактов сцены");
                for(int i=0;i<arr.length();i++){
                    JSONObject entry=arr.getJSONObject(i);
                    state.confirm(kind,entry.getString("id"),entry.getString("title"),entry.getString("status"));
                }
            }
            return state;
        }catch(JSONException invalid){
            // Malformed optional scene metadata cannot invalidate an older character.
            return new SceneState();
        }
    }
}
