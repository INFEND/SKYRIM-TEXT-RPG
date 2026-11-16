package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashSet;

/** All Fixcoin shop state belongs to one GameEngine save, never to global preferences. */
public final class FixShopState {
    private final LinkedHashSet<String> favorites=new LinkedHashSet<>();
    private final JSONArray history=new JSONArray();
    public boolean favorite(String id){return favorites.contains(id);}
    public void toggle(String id){if(!favorites.remove(id))favorites.add(id);}
    public int count(String id){int n=0;for(int i=0;i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null&&id.equals(e.optString("id")))n++;}return n;}
    public static String key(String title,int price){return title+" | "+price;}
    public void record(String title,String detail,int price,int day){
        try{history.put(new JSONObject().put("id",key(title,price)).put("title",title).put("detail",detail).put("price",price).put("day",day));}catch(Exception ignored){}
    }
    public JSONArray history(){return history;}
    public JSONObject toJson(){JSONObject j=new JSONObject();try{j.put("favorites",new JSONArray(favorites)).put("history",history);}catch(Exception ignored){}return j;}
    public static FixShopState fromJson(JSONObject j){
        FixShopState s=new FixShopState();if(j==null)return s;
        JSONArray a=j.optJSONArray("favorites");if(a!=null)for(int i=0;i<a.length();i++){String id=a.optString(i);if(!id.isEmpty())s.favorites.add(id);}
        a=j.optJSONArray("history");if(a!=null)for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);if(e!=null&&e.optInt("price")>0)s.history.put(e);}
        return s;
    }
}
