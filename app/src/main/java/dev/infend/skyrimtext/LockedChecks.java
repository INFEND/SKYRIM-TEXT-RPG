package dev.infend.skyrimtext;
import org.json.*;
/** Immutable results, created once before the storyteller is asked to resolve a turn. */
public final class LockedChecks {
 private final JSONArray checks;
 public LockedChecks(){checks=new JSONArray();}
 private LockedChecks(JSONArray value){checks=value;}
 public void add(int index,String title,int chance,int die){if(index<1||index>10||chance< -1||chance>100||(chance<0?die!=-1:die<1||die>100))throw new IllegalArgumentException("Invalid check");try{checks.put(new JSONObject().put("index",index).put("title",title).put("chance",chance).put("roll",die));}catch(JSONException ex){throw new IllegalStateException(ex);}}
 public JSONArray snapshot(){try{return new JSONArray(checks.toString());}catch(JSONException ex){return new JSONArray();}}
 public String toString(){return checks.toString();}
 public static LockedChecks restore(String raw){try{JSONArray a=new JSONArray(raw);LockedChecks result=new LockedChecks();java.util.HashSet<Integer> used=new java.util.HashSet<>();if(a.length()>10)return null;for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);int index=o.getInt("index");if(!used.add(index))return null;result.add(index,o.getString("title"),o.getInt("chance"),o.getInt("roll"));}return result;}catch(Exception ex){return null;}}
 public boolean hasSuccess(){for(int i=0;i<checks.length();i++){JSONObject o=checks.optJSONObject(i);if(o!=null&&AiGameTurn.success(o.optInt("chance"),o.optInt("roll")))return true;}return false;}
}
