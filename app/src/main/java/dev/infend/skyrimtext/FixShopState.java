package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashSet;

/** All Fixcoin shop state belongs to one GameEngine save, never to global preferences. */
public final class FixShopState {
    private final LinkedHashSet<String> favorites=new LinkedHashSet<>();
    private final JSONArray history=new JSONArray();
    public boolean editorEnabled=false;
    final int[] adventureStage=new int[2];
    FixShopWorld world=new FixShopWorld();
    FixShopHome home=new FixShopHome();
    FixShopGifts gifts=new FixShopGifts();
    FixShopSeals seals=new FixShopSeals();
    FixShopCosmetics cosmetics=new FixShopCosmetics();
    final java.util.HashSet<Integer> collectionRewards=new java.util.HashSet<>();
    public String customCatalog="[]";
    public int pendingVouchers(){int n=0;for(int i=0;i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null&&"PENDING".equals(e.optString("state")))n++;}return n;}
    public boolean reserve(GameEngine g,FixShopCatalog.Offer o){return reserve(g,o,1);}
    public boolean reserve(GameEngine g,FixShopCatalog.Offer base,int packs){
        FixShopCatalog.Offer o=FixShopCatalog.multiply(base,packs);
        if(!g.started||"COMBAT".equals(g.mode)||g.road.pending()||!FixShopCatalog.valid(o)||g.fixSpent>Long.MAX_VALUE-o.price||pendingVouchers()>=200)return false;
        try{JSONObject e=new JSONObject().put("id",FixShopCatalog.productId(base)).put("title",o.title()).put("detail","Товар ожидает получения").put("packs",packs).put("unitQuantity",base.quantity).put("unitPrice",base.price).put("price",o.price).put("day",g.day).put("state","PENDING").put("item",o.item).put("quantity",o.quantity);history.put(e);g.fixSpent+=o.price;return true;}catch(Exception ex){return false;}
    }
    public boolean reserveBundle(GameEngine g,FixShopCatalog.Bundle b){
        if(!g.started||"COMBAT".equals(g.mode)||g.road.pending()||!FixShopCatalog.validBundle(b)||g.fixSpent>Long.MAX_VALUE-b.price||pendingVouchers()>=200)return false;
        try{history.put(new JSONObject().put("id",key(b.name,b.price)).put("title",b.name).put("detail",b.description()).put("price",b.price).put("day",g.day).put("state","PENDING").put("bundle",b.name));g.fixSpent+=b.price;return true;}catch(Exception ex){return false;}
    }
    public boolean redeem(GameEngine g,int index){
        JSONObject e=history.optJSONObject(index);if(!g.started||"COMBAT".equals(g.mode)||g.road.pending()||e==null||!"PENDING".equals(e.optString("state")))return false;
        String bundle=e.optString("bundle"),title;
        if(!bundle.isEmpty()){FixShopCatalog.Bundle b=FixShopCatalog.bundle(bundle);if(b==null||b.price!=e.optInt("price")||!FixShopCatalog.redeemBundle(g,b))return false;title=b.name;}
        else{FixShopCatalog.Offer o=new FixShopCatalog.Offer(e.optString("item"),e.optInt("quantity"),e.optInt("price"));if(!FixShopCatalog.redeem(g,o))return false;title=o.title();}
        try{e.put("state","APPLIED").put("detail","Получено: "+title);}catch(Exception ignored){}return true;
    }
    public boolean refund(GameEngine g,int index){
        JSONObject e=history.optJSONObject(index);if(!g.started||"COMBAT".equals(g.mode)||g.road.pending()||e==null||!"PENDING".equals(e.optString("state"))||g.fixSpent<e.optInt("price"))return false;
        try{e.put("state","REFUNDED").put("detail","Возврат до получения");g.fixSpent-=e.optInt("price");return true;}catch(Exception ex){return false;}
    }
    public int distinctApplied(){java.util.HashSet<String> ids=new java.util.HashSet<>();for(int i=0;i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null&&!"PENDING".equals(e.optString("state"))&&!"REFUNDED".equals(e.optString("state")))ids.add(e.optString("item").isEmpty()?e.optString("id"):"item:"+e.optString("item"));}return ids.size();}

    public boolean claimCollection(GameEngine g,int threshold){
        if(threshold!=5&&threshold!=15&&threshold!=30&&threshold!=50||distinctApplied()<threshold||!collectionRewards.add(threshold))return false;
        g.addXp(threshold*10);return true;
    }
    public long purchasedQuantity(String item){long n=0;for(int i=0;i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null&&item.equals(e.optString("item"))&&!"REFUNDED".equals(e.optString("state")))n+=Math.max(0,e.optInt("quantity"));}return n;}
    public boolean favorite(String id){return favorites.contains(id);}
    public void toggle(String id){if(!favorites.remove(id))favorites.add(id);}
    public int count(String id){int n=0;for(int i=0;i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null&&id.equals(e.optString("id"))&&!"REFUNDED".equals(e.optString("state")))n++;}return n;}
    public static String key(String title,int price){return title+" | "+price;}
    public void record(String title,String detail,int price,int day){
        try{history.put(new JSONObject().put("id",key(title,price)).put("title",title).put("detail",detail).put("price",price).put("day",day));}catch(Exception ignored){}
    }
    public JSONArray history(){return history;}
    /** Remove withdrawn products and subtract their recorded costs exactly once. */
    void migrateRemoved(GameEngine g){
        for(int i=history.length()-1;i>=0;i--){JSONObject e=history.optJSONObject(i);if(e!=null&&FixShopWorld.removedCharacter(e.optString("title"))){if(!"REFUNDED".equals(e.optString("state")))g.fixSpent=Math.max(0,g.fixSpent-e.optInt("price"));history.remove(i);}}
        favorites.removeIf(id->{int at=id.lastIndexOf(" | ");return FixShopWorld.removedCharacter(at>=0?id.substring(0,at):id);});
    }
    public JSONObject toJson(){JSONObject j=new JSONObject();try{j.put("seals",seals.save()).put("gifts",gifts.save()).put("home",home.save()).put("cosmetics",cosmetics.save()).put("world",world.save()).put("collectionRewards",new JSONArray(collectionRewards)).put("adventureStage",new JSONArray(adventureStage)).put("customCatalog",customCatalog).put("editorEnabled",editorEnabled).put("favorites",new JSONArray(favorites)).put("history",history);}catch(Exception ignored){}return j;}
    public static FixShopState fromJson(JSONObject j){
        FixShopState s=new FixShopState();if(j==null)return s;s.editorEnabled=j.optBoolean("editorEnabled",false);s.world=FixShopWorld.restore(j.optJSONObject("world"));s.home=FixShopHome.restore(j.optJSONObject("home"));s.gifts=FixShopGifts.restore(j.optJSONObject("gifts"));s.seals=FixShopSeals.restore(j.optJSONObject("seals"));s.cosmetics=FixShopCosmetics.restore(j.optJSONObject("cosmetics"));JSONArray rewards=j.optJSONArray("collectionRewards");if(rewards!=null)for(int i=0;i<Math.min(4,rewards.length());i++){int n=rewards.optInt(i);if(n==5||n==15||n==30||n==50)s.collectionRewards.add(n);}JSONArray stages=j.optJSONArray("adventureStage");if(stages!=null)for(int i=0;i<s.adventureStage.length;i++)s.adventureStage[i]=Math.max(0,Math.min(5,stages.optInt(i,0)));String custom=j.optString("customCatalog","[]");try{s.customCatalog=FixShopCatalog.encode(FixShopCatalog.parse(custom)).toString();}catch(Exception ignored){}
        JSONArray a=j.optJSONArray("favorites");if(a!=null)for(int i=0;i<a.length();i++){String id=a.optString(i);if(!id.isEmpty())s.favorites.add(id);}
        a=j.optJSONArray("history");if(a!=null)for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);if(e!=null&&e.optInt("price")>0)s.history.put(e);}
        return s;
    }
}
