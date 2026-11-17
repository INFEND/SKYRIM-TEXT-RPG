package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashSet;

/** All Fixcoin shop state belongs to one GameEngine save, never to global preferences. */
public final class FixShopState {
    private final LinkedHashSet<String> favorites=new LinkedHashSet<>();
    private final JSONArray history=new JSONArray();
    public boolean editorEnabled=false;
    public String customCatalog="[]";
    public int pendingVouchers(){int n=0;for(int i=0;i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null&&"PENDING".equals(e.optString("state")))n++;}return n;}
    public boolean reserve(GameEngine g,FixShopCatalog.Offer o){
        if(!g.started||"COMBAT".equals(g.mode)||g.road.pending()||!FixShopCatalog.valid(o)||g.fixSpent>Long.MAX_VALUE-o.price||pendingVouchers()>=200)return false;
        try{JSONObject e=new JSONObject().put("id",key(o.title(),o.price)).put("title",o.title()).put("detail","Товар ожидает получения").put("price",o.price).put("day",g.day).put("state","PENDING").put("item",o.item).put("quantity",o.quantity);history.put(e);g.fixSpent+=o.price;return true;}catch(Exception ex){return false;}
    }
    public boolean redeem(GameEngine g,int index){
        JSONObject e=history.optJSONObject(index);if(!g.started||"COMBAT".equals(g.mode)||g.road.pending()||e==null||!"PENDING".equals(e.optString("state")))return false;
        FixShopCatalog.Offer o=new FixShopCatalog.Offer(e.optString("item"),e.optInt("quantity"),e.optInt("price"));
        if(!FixShopCatalog.redeem(g,o))return false;
        try{e.put("state","APPLIED").put("detail","Получено: "+o.title());}catch(Exception ignored){}return true;
    }
    public boolean refund(GameEngine g,int index){
        JSONObject e=history.optJSONObject(index);if(!g.started||"COMBAT".equals(g.mode)||g.road.pending()||e==null||!"PENDING".equals(e.optString("state"))||g.fixSpent<e.optInt("price"))return false;
        try{e.put("state","REFUNDED").put("detail","Возврат до получения");g.fixSpent-=e.optInt("price");return true;}catch(Exception ex){return false;}
    }
    public int distinctApplied(){java.util.HashSet<String> ids=new java.util.HashSet<>();for(int i=0;i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null&&!"PENDING".equals(e.optString("state"))&&!"REFUNDED".equals(e.optString("state")))ids.add(e.optString("id"));}return ids.size();}

    public boolean favorite(String id){return favorites.contains(id);}
    public void toggle(String id){if(!favorites.remove(id))favorites.add(id);}
    public int count(String id){int n=0;for(int i=0;i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null&&id.equals(e.optString("id"))&&!"REFUNDED".equals(e.optString("state")))n++;}return n;}
    public static String key(String title,int price){return title+" | "+price;}
    public void record(String title,String detail,int price,int day){
        try{history.put(new JSONObject().put("id",key(title,price)).put("title",title).put("detail",detail).put("price",price).put("day",day));}catch(Exception ignored){}
    }
    public JSONArray history(){return history;}
    public JSONObject toJson(){JSONObject j=new JSONObject();try{j.put("customCatalog",customCatalog).put("editorEnabled",editorEnabled).put("favorites",new JSONArray(favorites)).put("history",history);}catch(Exception ignored){}return j;}
    public static FixShopState fromJson(JSONObject j){
        FixShopState s=new FixShopState();if(j==null)return s;s.editorEnabled=j.optBoolean("editorEnabled",false);String custom=j.optString("customCatalog","[]");try{s.customCatalog=FixShopCatalog.encode(FixShopCatalog.parse(custom)).toString();}catch(Exception ignored){}
        JSONArray a=j.optJSONArray("favorites");if(a!=null)for(int i=0;i<a.length();i++){String id=a.optString(i);if(!id.isEmpty())s.favorites.add(id);}
        a=j.optJSONArray("history");if(a!=null)for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);if(e!=null&&e.optInt("price")>0)s.history.put(e);}
        return s;
    }
}
