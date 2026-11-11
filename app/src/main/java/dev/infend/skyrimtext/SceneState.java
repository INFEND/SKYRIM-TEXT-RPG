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
        if(!allowedStatus(kind,status))
            throw new JSONException("Недопустимое состояние факта");
        if(!entries.containsKey(safeId) && entries.size()>=MAX_ENTRIES)
            throw new JSONException("Переполнен журнал сцены");
        if(entries.containsKey(safeId)) {
            String previous=entries.get(safeId).optString("status");
            // A confirmed death is terminal; evidence and discovered places cannot
            // revert to an earlier stage after loading or another AI turn.
            if("npc".equals(kind)) {
                if("DEAD".equals(previous) && !"DEAD".equals(status))
                    throw new JSONException("Погибший NPC не может вернуться");
                int oldRank=npcRelationshipRank(previous), newRank=npcRelationshipRank(status);
                if(oldRank>0 && newRank>0 && newRank<oldRank)
                    throw new JSONException("Откат отношений NPC запрещён");
            } else if(factRank(kind,status)<factRank(kind,previous)) {
                throw new JSONException("Откат подтверждённого факта запрещён");
            }
        }
        entries.put(safeId,new JSONObject().put("id",safeId).put("title",safeTitle).put("status",status));
    }
    private static boolean allowedStatus(String kind,String status) {
        if(status==null)return false;
        switch(kind) {
            case "npc": return "MET".equals(status)||"INFORMED".equals(status)||
                "HELPED".equals(status)||"HOSTILE".equals(status)||
                "MISSING".equals(status)||"DEAD".equals(status);
            case "clue": return "FOUND".equals(status)||"EXAMINED".equals(status)||"USED".equals(status);
            case "sublocation": return "DISCOVERED".equals(status)||"VISITED".equals(status)||"CLEARED".equals(status);
            default: return false;
        }
    }
    private static int factRank(String kind,String status) {
        if("clue".equals(kind)) {
            switch(status) {case "FOUND":return 1;case "EXAMINED":return 2;case "USED":return 3;default:return 0;}
        }
        if("sublocation".equals(kind)) {
            switch(status) {case "DISCOVERED":return 1;case "VISITED":return 2;case "CLEARED":return 3;default:return 0;}
        }
        return 0;
    }
    private static int npcRelationshipRank(String status) {
        switch(status) {
            case "MET": return 1;
            case "INFORMED": return 2;
            case "HELPED": return 3;
            default: return 0; // Other statuses are not ordered relationships.
        }
    }
    public boolean contains(String kind,String key) throws JSONException {
        return collection(this,kind).containsKey(id(key));
    }
    /** Return a confirmed status without exposing mutable JSON to callers. */
    public String status(String kind,String key) throws JSONException {
        JSONObject entry=collection(this,kind).get(id(key));
        return entry==null ? "" : entry.optString("status");
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
