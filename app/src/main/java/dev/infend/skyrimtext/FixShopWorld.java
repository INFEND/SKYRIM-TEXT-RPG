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
    int mount=0,pet=0,active=-1,mountOwned=0,petOwned=0;
    boolean stable=false;
    String queuedEncounter="";
    final ArrayList<JSONObject> characters=new ArrayList<>(),places=new ArrayList<>(),quests=new ArrayList<>();
    final JSONObject storage=new JSONObject();
    static String clean(String s,int max){return s==null?"":s.trim().length()>max?s.trim().substring(0,max):s.trim();}
    boolean ownMount(int i){return i>=1&&i<=MOUNTS.length&&(mountOwned&(1<<(i-1)))!=0;}
    boolean buyMount(int i){if(i<1||i>MOUNTS.length||ownMount(i))return false;mountOwned|=1<<(i-1);mount=i;return true;}
    boolean selectMount(int i){if(i!=0&&!ownMount(i))return false;mount=i;return true;}
    boolean ownPet(int i){return i>=1&&i<=PETS.length&&(petOwned&(1<<(i-1)))!=0;}
    boolean buyPet(int i){if(i<1||i>PETS.length||ownPet(i))return false;petOwned|=1<<(i-1);pet=i;return true;}
    boolean selectPet(int i){if(i!=0&&!ownPet(i))return false;pet=i;return true;}
    int travelBonus(){return mount==2?4:mount==1?3:0;}
    int petDamage(int turn,int remaining){return pet>0&&turn%3==0?Math.min(Math.max(0,remaining),pet==2?7:5):0;}
    String petName(){return pet>0?PETS[pet-1]:"";}
    JSONObject activeCharacter(){return active>=0&&active<characters.size()?characters.get(active):null;}
    String companionName(){JSONObject c=activeCharacter();return c==null?"":c.optString("name");}
    int companionDamage(int turn,int remaining){JSONObject c=activeCharacter();return c==null||turn%2!=0?0:Math.min(Math.max(0,remaining),c.optInt("power",8));}
    boolean addCharacter(String name,String description,int role,boolean companion){return addCharacter(name,description,role,companion,"Вайтран");}
    boolean addCharacter(String name,String description,int role,boolean companion,String home){
        name=clean(name,60);description=clean(description,400);
        if(name.isEmpty()||description.isEmpty()||role<0||role>=ROLES.length||characters.size()>=MAX_CHARACTERS||WorldAtlas.find(home)==null||"Ливия".equalsIgnoreCase(name)||"Кэтрин".equalsIgnoreCase(name)||"Итачи".equalsIgnoreCase(name)||"Альма".equalsIgnoreCase(name)||"Серана".equalsIgnoreCase(name))return false;
        for(JSONObject c:characters)if(name.equalsIgnoreCase(c.optString("name")))return false;
        try{characters.add(new JSONObject().put("name",name).put("description",description).put("role",role).put("companion",companion).put("home",home).put("power",role==1?12:role==2?10:8).put("relation",50).put("lastTalk",-1));return true;}catch(Exception e){return false;}
    }
    boolean present(GameEngine g,int i){return i>=0&&i<characters.size()&&(active==i||g.progress.location.equals(characters.get(i).optString("home")));}
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
    boolean addQuest(String title,String description,String start,String destination){
        title=clean(title,60);description=clean(description,400);
        if(title.isEmpty()||description.isEmpty()||quests.size()>=12||WorldAtlas.find(start)==null||WorldAtlas.find(destination)==null||start.equals(destination))return false;
        for(JSONObject q:quests)if(title.equalsIgnoreCase(q.optString("title")))return false;
        try{quests.add(new JSONObject().put("title",title).put("description",description).put("start",start).put("destination",destination).put("stage",0));return true;}catch(Exception e){return false;}
    }
    boolean questAction(GameEngine g,int i,boolean finish){
        if(!safe(g)||i<0||i>=quests.size())return false;JSONObject q=quests.get(i);int stage=q.optInt("stage");
        if(finish?(stage!=1||!g.progress.location.equals(q.optString("destination"))):(stage!=0||!g.progress.location.equals(q.optString("start"))))return false;
        try{q.put("stage",finish?2:1);}catch(Exception e){return false;}
        g.message=q.optString("title")+"\n\n"+(finish?"Поручение выполнено. +50 опыта.":q.optString("description")+"\nЦель: "+q.optString("destination"));
        if(finish)g.addXp(50);g.mode="RESULT";g.recordVisualEvent(g.message);return true;
    }
    static void choices(GameEngine g,GameEngine.Scene scene){
        if(!safe(g))return;FixShopWorld w=g.fixShop.world;
        for(int i=0;i<w.places.size();i++){JSONObject c=w.places.get(i);if(g.progress.location.equals(c.optString("anchor")))scene.add("FIXPLACE:"+i,"Посетить: "+c.optString("name"),"Авторская локация");}
        for(int i=0;i<w.characters.size();i++)if(w.present(g,i))scene.add("FIXNPC:"+i,"Поговорить: "+w.characters.get(i).optString("name"),"Авторский персонаж");
        for(int i=0;i<w.quests.size();i++){JSONObject q=w.quests.get(i);int stage=q.optInt("stage");if(stage==0&&g.progress.location.equals(q.optString("start")))scene.add("FIXCUSTOM:"+i+":start",q.optString("title"),"Принять поручение");else if(stage==1&&g.progress.location.equals(q.optString("destination")))scene.add("FIXCUSTOM:"+i+":finish",q.optString("title"),"Завершить поручение");}
        if(estateHere(g))scene.add("FIXHOME:rest","Отдохнуть в собственном поместье","8 часов | бесплатно");
    }
    static boolean choose(GameEngine g,String action){
        if(!safe(g))return false;
        if("FIXHOME:rest".equals(action)&&estateHere(g)){if(!g.restHours(8,true))return false;g.mode="RESULT";return true;}
        if(action.matches("FIXNPC:[0-9]{1,2}")){int i=Integer.parseInt(action.substring(7));FixShopWorld w=g.fixShop.world;if(!w.present(g,i))return false;JSONObject c=w.characters.get(i);boolean improved=w.talk(i,g.day);g.message=c.optString("name")+"\n\n"+c.optString("description")+"\n\n"+(improved?"Разговор укрепил доверие.":"Вы уже поговорили сегодня или достигли максимума доверия.");g.mode="RESULT";return true;}
        if(action.matches("FIXCUSTOM:[0-9]{1,2}:(start|finish)")){String[] a=action.split(":");return g.fixShop.world.questAction(g,Integer.parseInt(a[1]),"finish".equals(a[2]));}
        if(action.matches("FIXPLACE:[0-9]{1,2}"))return g.fixShop.world.visit(g,Integer.parseInt(action.substring(9)));
        return false;
    }
    String context(){JSONObject c=activeCharacter();return c==null?"":"Авторский спутник: "+c.optString("name")+"; "+ROLES[c.optInt("role",0)]+"; "+c.optString("description")+"; отношение "+c.optInt("relation",50)+"/100.";}
    JSONObject save(){JSONObject j=new JSONObject();try{j.put("queuedEncounter",queuedEncounter).put("mount",mount).put("pet",pet).put("mountOwned",mountOwned).put("petOwned",petOwned).put("quests",new JSONArray(quests)).put("active",active).put("stable",stable).put("storage",storage).put("characters",new JSONArray(characters)).put("places",new JSONArray(places));}catch(Exception ignored){}return j;}
    static FixShopWorld restore(JSONObject j){
        FixShopWorld w=new FixShopWorld();if(j==null)return w;
        w.mount=Math.max(0,Math.min(MOUNTS.length,j.optInt("mount")));w.pet=Math.max(0,Math.min(PETS.length,j.optInt("pet")));w.stable=j.optBoolean("stable");String encounter=j.optString("queuedEncounter","");if(RoadEvents.fixEventAllowed(encounter))w.queuedEncounter=encounter;w.mountOwned=(j.optInt("mountOwned")&3)|(w.mount>0?1<<(w.mount-1):0);w.petOwned=(j.optInt("petOwned")&3)|(w.pet>0?1<<(w.pet-1):0);
        JSONArray a=j.optJSONArray("characters");if(a!=null)for(int i=0;i<Math.min(a.length(),MAX_CHARACTERS);i++){JSONObject c=a.optJSONObject(i);if(c!=null&&w.addCharacter(c.optString("name"),c.optString("description"),c.optInt("role",-1),c.optBoolean("companion"),c.optString("home","Вайтран"))){JSONObject n=w.characters.get(w.characters.size()-1);try{n.put("relation",Math.max(0,Math.min(100,c.optInt("relation",50)))).put("lastTalk",c.optInt("lastTalk",-1));}catch(Exception ignored){}}}
        int active=j.optInt("active",-1);if(active>=0&&active<w.characters.size()&&w.characters.get(active).optBoolean("companion"))w.active=active;
        a=j.optJSONArray("places");if(a!=null)for(int i=0;i<Math.min(a.length(),MAX_PLACES);i++){JSONObject c=a.optJSONObject(i);if(c!=null&&w.addPlace(c.optString("name"),c.optString("description"),c.optString("anchor")))try{w.places.get(w.places.size()-1).put("visits",Math.max(0,c.optInt("visits")));}catch(Exception ignored){}}
        a=j.optJSONArray("quests");if(a!=null)for(int i=0;i<Math.min(a.length(),12);i++){JSONObject q=a.optJSONObject(i);if(q!=null&&w.addQuest(q.optString("title"),q.optString("description"),q.optString("start"),q.optString("destination")))try{w.quests.get(w.quests.size()-1).put("stage",Math.max(0,Math.min(2,q.optInt("stage"))));}catch(Exception ignored){}}
        JSONObject stored=j.optJSONObject("storage");if(stored!=null){Iterator<String> keys=stored.keys();while(keys.hasNext()){String id=keys.next();if(w.canStore(id)&&stored.optInt(id)>0)try{w.storage.put(id,stored.optInt(id));}catch(Exception ignored){}}}return w;
    }
}
