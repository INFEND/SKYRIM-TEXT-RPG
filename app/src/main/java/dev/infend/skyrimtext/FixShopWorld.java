package dev.infend.skyrimtext;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;

/** Save-local optional world additions; never rewrites the canonical atlas. */
public final class FixShopWorld {
    static final String[] MOUNTS={"Призрачный скакун","Драконий скакун"};
    static final String[] PETS={"Северный волк","Огненный фамильяр"};
    static final String[] ROLES={"Воин","Маг","Лучник"};
    static final int MAX_CHARACTERS=12, MAX_PLACES=12;
    int mount=0,pet=0,active=-1;
    boolean stable=false;
    final ArrayList<JSONObject> characters=new ArrayList<>(),places=new ArrayList<>();
    final JSONObject storage=new JSONObject();
    static String clean(String s,int max){return s==null?"":s.trim().length()>max?s.trim().substring(0,max):s.trim();}
    boolean ownMount(int i){return i>=1&&i<=MOUNTS.length&&mount==i;}
    boolean buyMount(int i){if(i<1||i>MOUNTS.length||i==mount)return false;mount=i;return true;}
    boolean buyPet(int i){if(i<1||i>PETS.length||i==pet)return false;pet=i;return true;}
    int travelBonus(){return mount==2?4:mount==1?3:0;}
    int petDamage(int turn,int remaining){return pet>0&&turn%3==0?Math.min(Math.max(0,remaining),pet==2?7:5):0;}
    String petName(){return pet>0?PETS[pet-1]:"";}
    JSONObject activeCharacter(){return active>=0&&active<characters.size()?characters.get(active):null;}
    String companionName(){JSONObject c=activeCharacter();return c==null?"":c.optString("name");}
    int companionDamage(int turn,int remaining){JSONObject c=activeCharacter();return c==null||turn%2!=0?0:Math.min(Math.max(0,remaining),c.optInt("power",8));}
    boolean addCharacter(String name,String description,int role,boolean companion){
        name=clean(name,60);description=clean(description,400);
        if(name.isEmpty()||description.isEmpty()||role<0||role>=ROLES.length||characters.size()>=MAX_CHARACTERS)return false;
        for(JSONObject c:characters)if(name.equalsIgnoreCase(c.optString("name")))return false;
        try{characters.add(new JSONObject().put("name",name).put("description",description).put("role",role).put("companion",companion).put("power",role==1?12:role==2?10:8).put("relation",50).put("lastTalk",-1));return true;}catch(Exception e){return false;}
    }
    boolean recruit(int i){if(i<0||i>=characters.size()||!characters.get(i).optBoolean("companion")||active==i)return false;active=i;return true;}
    boolean dismiss(){if(active<0)return false;active=-1;return true;}
    boolean talk(int i,int day){if(i<0||i>=characters.size())return false;JSONObject c=characters.get(i);if(c.optInt("lastTalk",-1)==day||c.optInt("relation")>=100)return false;try{c.put("lastTalk",day).put("relation",Math.min(100,c.optInt("relation",50)+5));return true;}catch(Exception e){return false;}}
    boolean addPlace(String name,String description,String anchor){
        name=clean(name,60);description=clean(description,400);
        if(name.isEmpty()||description.isEmpty()||places.size()>=MAX_PLACES||WorldAtlas.find(anchor)==null||WorldAtlas.find(name)!=null)return false;
        for(JSONObject c:places)if(name.equalsIgnoreCase(c.optString("name")))return false;
        try{places.add(new JSONObject().put("name",name).put("description",description).put("anchor",anchor).put("visits",0));return true;}catch(Exception e){return false;}
    }
    boolean visit(GameEngine g,int i){if(!safe(g)||i<0||i>=places.size())return false;JSONObject c=places.get(i);if(!g.progress.location.equals(c.optString("anchor")))return false;try{c.put("visits",Math.min(1000000,c.optInt("visits")+1));}catch(Exception e){return false;}g.message=c.optString("name")+"\n\n"+c.optString("description");g.mode="RESULT";g.recordVisualEvent("Посещение: "+c.optString("name"));return true;}
    static boolean safe(GameEngine g){return g.started&&!g.road.pending()&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode));}
    static boolean estateHere(GameEngine g){for(int i:new int[]{5,7,8})if(RoadEvents.ownsEstate(g,i)&&RoadEvents.estate(i).equals(g.progress.location))return true;return false;}
    boolean canStore(String id){ItemCatalog.Item item=ItemCatalog.find(id);return item!=null&&CompensationPowers.itemIndex(id)<0&&("Оружие".equals(item.category)||"Броня".equals(item.category)||"Украшения".equals(item.category)||"Одежда".equals(item.category)||"Боеприпасы".equals(item.category))&&!"Походный клинок".equals(id)&&!"Дорожная одежда".equals(id)&&!"Стальной меч".equals(id)&&!"Стальная кираса".equals(id);}
    boolean transfer(GameEngine g,String id,int n,boolean deposit){
        if(!safe(g)||!stable||!estateHere(g)||n<1||!canStore(id))return false;
        int old=storage.optInt(id);
        if(deposit){if(g.progress.equipped(id)||g.progress.temperQuality(id)>0||old>Integer.MAX_VALUE-n||!g.progress.removeItem(id,n))return false;}
        else if(old<n||!g.progress.addItem(id,n))return false;
        try{storage.put(id,deposit?old+n:old-n);return true;}catch(Exception e){return false;}
    }
    static void choices(GameEngine g,GameEngine.Scene scene){
        if(!safe(g))return;FixShopWorld w=g.fixShop.world;
        for(int i=0;i<w.places.size();i++){JSONObject c=w.places.get(i);if(g.progress.location.equals(c.optString("anchor")))scene.add("FIXPLACE:"+i,"Посетить: "+c.optString("name"),"Авторская локация");}
        if(estateHere(g))scene.add("FIXHOME:rest","Отдохнуть в собственном поместье","8 часов | бесплатно");
    }
    static boolean choose(GameEngine g,String action){
        if(!safe(g))return false;
        if("FIXHOME:rest".equals(action)&&estateHere(g)){if(!g.restHours(8,true))return false;g.mode="RESULT";return true;}
        if(action.matches("FIXPLACE:[0-9]{1,2}"))return g.fixShop.world.visit(g,Integer.parseInt(action.substring(9)));
        return false;
    }
    String context(){JSONObject c=activeCharacter();return c==null?"":"Авторский спутник: "+c.optString("name")+"; "+ROLES[c.optInt("role",0)]+"; "+c.optString("description")+"; отношение "+c.optInt("relation",50)+"/100.";}
    JSONObject save(){JSONObject j=new JSONObject();try{j.put("mount",mount).put("pet",pet).put("active",active).put("stable",stable).put("storage",storage).put("characters",new JSONArray(characters)).put("places",new JSONArray(places));}catch(Exception ignored){}return j;}
    static FixShopWorld restore(JSONObject j){
        FixShopWorld w=new FixShopWorld();if(j==null)return w;
        w.mount=Math.max(0,Math.min(MOUNTS.length,j.optInt("mount")));w.pet=Math.max(0,Math.min(PETS.length,j.optInt("pet")));w.stable=j.optBoolean("stable");
        JSONArray a=j.optJSONArray("characters");if(a!=null)for(int i=0;i<Math.min(a.length(),MAX_CHARACTERS);i++){JSONObject c=a.optJSONObject(i);if(c!=null&&w.addCharacter(c.optString("name"),c.optString("description"),c.optInt("role",-1),c.optBoolean("companion"))){JSONObject n=w.characters.get(w.characters.size()-1);try{n.put("relation",Math.max(0,Math.min(100,c.optInt("relation",50)))).put("lastTalk",c.optInt("lastTalk",-1));}catch(Exception ignored){}}}
        int active=j.optInt("active",-1);if(active>=0&&active<w.characters.size()&&w.characters.get(active).optBoolean("companion"))w.active=active;
        a=j.optJSONArray("places");if(a!=null)for(int i=0;i<Math.min(a.length(),MAX_PLACES);i++){JSONObject c=a.optJSONObject(i);if(c!=null&&w.addPlace(c.optString("name"),c.optString("description"),c.optString("anchor")))try{w.places.get(w.places.size()-1).put("visits",Math.max(0,c.optInt("visits")));}catch(Exception ignored){}}
        JSONObject stored=j.optJSONObject("storage");if(stored!=null){Iterator<String> keys=stored.keys();while(keys.hasNext()){String id=keys.next();if(w.canStore(id)&&stored.optInt(id)>0)try{w.storage.put(id,stored.optInt(id));}catch(Exception ignored){}}}return w;
    }
}
