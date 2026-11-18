package dev.infend.skyrimtext;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Iterator;

/** Per-save companion life, family and equipment. */
final class LiviaLife {
    JSONObject pack=new JSONObject(),gear=new JSONObject();
    int lastDay=-1,independentDays=0,lastGroupDay=-1,lastGiftDay=-1,lastHomeDay=-1,engagementDay=-1;
    int love=0,groupTalks=0,homeEvenings=0,training=0;
    int katherineTrust=50,almaTrust=50,itachiTrust=50;
    String groupSummary(){return "Кэтрин "+katherineTrust+"/100 | Альма "+almaTrust+"/100 | Итачи "+itachiTrust+"/100";}
    String talk(GameEngine g,LiviaStory l){String[] topics={"Ливия: Долг не снимает ответственности. Я больше не хочу прятаться за чужими приказами.","Ливия рассказывает о первом карауле, страхе перед начальством и цене ошибки.","Ливия: Я не прошу забыть Хелген. Мне достаточно, что мы можем говорить о нём.","Вы обсуждаете границы власти, справедливость и то, каким должен быть настоящий защитник.","Ливия признаётся, что боится вновь не оправдать доверие, и спрашивает, чего ты ждёшь от неё."};return topics[Math.floorMod(g.day,topics.length)];}
    String date(GameEngine g,boolean walk){String[] routes={"Вы задержались у морской арки и слушали шум прибоя.","Вы прошли мимо Коллегии бардов. Ливия остановилась, чтобы послушать песню.","Вы гуляли у стен Синего дворца, обсуждая места, куда ещё хотите отправиться."};String[] evenings={"Ливия рассказывает о мечтах, которые оставила ради службы. Вы договариваетесь не торопить друг друга.","За ужином вы вспоминаете дороги и смеётесь над мелкими неприятностями путешествия.","Ливия берёт тебя за руку. Вы обсуждаете будущее, не давая обещаний за другого.","Вы делитесь историями из детства и долго сидите рядом, не нарушая тишины."};String[] pool=walk?routes:evenings;return pool[Math.floorMod(g.day,pool.length)];}
    boolean engaged=false,married=false,sharedHome=false;
    String activity="Помощь пострадавшим";
    static final String[] REDEEM={"Помогает семьям погибших","Разбирает списки выживших","Разносит припасы нуждающимся","Тренирует добровольцев"};
    static final String[] FORGIVE={"Тренируется на учебном дворе","Помогает городскому дозору","Навещает храм Богов","Читает письма сослуживцев"};
    boolean access(GameEngine g,LiviaStory l){return g.started&&l.enabled&&l.stage==4&&l.survivor()&&l.present(g)&&!g.road.pending()&&!g.solitude.custody()&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode)||LiviaStory.MODE.equals(g.mode)||"SOLITUDE:1".equals(g.mode)||SolitudeHome.MODE.equals(g.mode));}
    static boolean eligible(String id){ItemCatalog.Item i=id==null?null:ItemCatalog.find(id);if(i==null||CompensationPowers.itemIndex(id)>=0||i.baseStat<=0||i.twoHanded()||!("Оружие".equals(i.category)||"Броня".equals(i.category)))return false;String s=i.slot();return "weapon".equals(s)&&i.statSkill==0||"shield".equals(s)||"head".equals(s)||"body".equals(s)||"hands".equals(s)||"feet".equals(s);}
    boolean canGive(GameEngine g,String id){return eligible(id)&&!g.progress.equipped(id)&&g.progress.temperQuality(id)==0&&g.progress.quantity(id)>0&&!id.equals("Походный клинок")&&!id.equals("Стальной меч")&&!id.equals("Стальная кираса");}
    double weight(){double n=0;Iterator<String> it=pack.keys();while(it.hasNext()){String id=it.next();ItemCatalog.Item i=ItemCatalog.find(id);if(i!=null)n+=i.weight*pack.optInt(id);}return n;}
    private void count(String id,int n){try{if(n<=0)pack.remove(id);else pack.put(id,n);}catch(JSONException e){throw new IllegalStateException(e);}}
    boolean transfer(GameEngine g,LiviaStory l,String id,boolean give){if(!access(g,l)||!eligible(id))return false;int n=pack.optInt(id);ItemCatalog.Item i=ItemCatalog.find(id);
        if(give){if(!canGive(g,id)||n>=100||weight()+i.weight>35.00001||!g.progress.removeItem(id,1))return false;count(id,n+1);}
        else{if(n<1||!g.progress.addItem(id,1))return false;count(id,n-1);if(n==1&&id.equals(gear.optString(i.slot())))gear.remove(i.slot());}g.progress.sync(g);return true;}
    boolean equip(GameEngine g,LiviaStory l,String id){if(!access(g,l)||!eligible(id)||pack.optInt(id)<=0)return false;String slot=ItemCatalog.find(id).slot();if(id.equals(gear.optString(slot)))gear.remove(slot);else try{gear.put(slot,id);}catch(JSONException e){return false;}return true;}
    String gearName(String slot,String fallback){String id=gear.optString(slot);ItemCatalog.Item i=ItemCatalog.find(id);return i!=null&&pack.optInt(id)>0?i.name:fallback;}
    String gearLabel(){return gearName("weapon","Имперский меч")+" | "+gearName("shield","Имперский щит")+" | "+gearName("body","Имперские доспехи");}
    int bonus(boolean weapon){int n=0;Iterator<String> it=gear.keys();while(it.hasNext()){String slot=it.next();String id=gear.optString(slot);ItemCatalog.Item i=ItemCatalog.find(id);if(i!=null&&pack.optInt(id)>0&&weapon=="weapon".equals(slot))n+=Math.min(8,(int)(i.baseStat/5));}return Math.min(12,n);}
    void daily(GameEngine g,LiviaStory l){if(!l.enabled||l.stage!=4||!l.survivor()||g.solitude.custody()||g.road.pending()||lastDay==g.day)return;lastDay=g.day;
        if(l.active()){activity=l.companionStamina<30?"Просит сделать привал":"Следит за безопасностью отряда";if(l.companionStamina<30)l.remember(g,"Ливия предложила восстановить силы.");return;}
        independentDays=GameEngine.nonnegativeAdd(independentDays,1);l.companionStamina=Math.min(100,l.companionStamina+30);String[] jobs="Искупление".equals(l.ending)?REDEEM:FORGIVE;activity=jobs[Math.floorMod(g.day,jobs.length)];if(Math.floorMod(g.day,4)==0)training=Math.min(100,training+1);l.remember(g,activity+".");}
    String group(GameEngine g){java.util.ArrayList<String> names=new java.util.ArrayList<>();if(g.katherine.active())names.add("Кэтрин");if(g.alma.active())names.add("Альма");if(g.itachi.active())names.add("Итачи");return String.join(", ",names);}
    boolean groupReady(GameEngine g,LiviaStory l){return l.active()&&lastGroupDay!=g.day&&!group(g).isEmpty();}
    boolean otherMarriage(GameEngine g){return g.alma.married||g.katherine.companion.married||g.itachi.married;}
    boolean canEngage(GameEngine g,LiviaStory l){return l.romance&&!engaged&&!married&&!otherMarriage(g)&&l.epilogue==3&&l.trust>=85&&l.respect>=75&&love>=75&&l.dates>=3;}
    boolean canMarry(GameEngine g,LiviaStory l){return l.romance&&engaged&&!married&&!otherMarriage(g)&&g.day>engagementDay&&l.active()&&"Рифтен".equals(g.progress.location);}
    boolean canHome(GameEngine g,LiviaStory l){return married&&!sharedHome&&l.castle(g)&&SolitudeHome.owns(g);}
    boolean homeReady(GameEngine g,LiviaStory l){return married&&sharedHome&&SolitudeHome.owns(g)&&l.castle(g)&&lastHomeDay!=g.day;}
    void addChoices(GameEngine.Scene s,GameEngine g,LiviaStory l){
        if(lastGiftDay!=g.day&&g.gold>=25)s.add("LIVIA:gift","Подарить книгу","25 септимов");
        if(groupReady(g,l))s.add("LIVIA:group","Обсудить тактику со спутниками",group(g));
        if(canEngage(g,l))s.add("LIVIA:engage","Предложить помолвку","Взаимная клятва");
        if(canMarry(g,l))s.add("LIVIA:marry","Сыграть свадьбу в храме Мары","Рифтен");
        if(canHome(g,l))s.add("LIVIA:home","Предложить жить вместе","Приобретённый Высокий шпиль");
        if(homeReady(g,l))s.add("LIVIA:homeEvening","Провести вечер в Высоком шпиле","Совместная жизнь");
        if(engaged&&!married)s.add("LIVIA:cancelEngagement","Отменить помолвку","Роман сохраняется");
    }
    boolean choose(GameEngine g,LiviaStory l,String a){switch(a){
        case "LIVIA:gift":if(lastGiftDay!=g.day&&g.gold>=25){g.gold-=25;lastGiftDay=g.day;l.affection=LiviaStory.clamp(l.affection+3);if(l.romance)love=LiviaStory.clamp(love+3);l.remember(g,"Ливия приняла книгу о северных землях.");}return true;
        case "LIVIA:group":if(groupReady(g,l)){lastGroupDay=g.day;groupTalks=GameEngine.nonnegativeAdd(groupTalks,1);l.respect=LiviaStory.clamp(l.respect+2);StringBuilder exchange=new StringBuilder();if(g.katherine.active()){katherineTrust=LiviaStory.clamp(katherineTrust+3);exchange.append("Кэтрин согласовала с Ливией защиту тыла. ");}if(g.alma.active()){almaTrust=LiviaStory.clamp(almaTrust+3);exchange.append("Альма и Ливия договорились предупреждать друг друга перед атакой. ");}if(g.itachi.active()){itachiTrust=LiviaStory.clamp(itachiTrust+3);exchange.append("Итачи предложила порядок разведки и отхода. ");}l.remember(g,exchange.toString().trim());}return true;
        case "LIVIA:engage":if(canEngage(g,l)){engaged=true;engagementDay=g.day;l.remember(g,"Ливия приняла предложение. Свадьба возможна в Рифтене со следующего дня.");}return true;
        case "LIVIA:marry":if(canMarry(g,l)){engaged=false;married=true;l.remember(g,"В храме Мары вы обменялись клятвами. Ливия: Теперь я выбираю нашу дорогу.");}return true;
        case "LIVIA:home":if(canHome(g,l)){sharedHome=true;l.remember(g,"Вы решили жить в принадлежащем тебе Высоком шпиле.");}return true;
        case "LIVIA:homeEvening":if(homeReady(g,l)){lastHomeDay=g.day;homeEvenings=GameEngine.nonnegativeAdd(homeEvenings,1);love=LiviaStory.clamp(love+4);l.companionStamina=Math.min(100,l.companionStamina+20);l.remember(g,"Вы провели тихий вечер в Высоком шпиле.");}return true;
        case "LIVIA:cancelEngagement":if(engaged&&!married){engaged=false;engagementDay=-1;l.remember(g,"Вы отложили свадьбу, сохранив отношения.");}return true;
        default:return false;}}
    void endRomance(){engaged=false;married=false;sharedHome=false;engagementDay=-1;love=0;}
    JSONObject save() throws JSONException{return new JSONObject().put("pack",pack).put("gear",gear).put("lastDay",lastDay).put("independentDays",independentDays).put("lastGroupDay",lastGroupDay).put("lastGiftDay",lastGiftDay).put("lastHomeDay",lastHomeDay).put("engagementDay",engagementDay).put("love",love).put("groupTalks",groupTalks).put("homeEvenings",homeEvenings).put("training",training).put("engaged",engaged).put("married",married).put("sharedHome",sharedHome).put("activity",activity).put("katherineTrust",katherineTrust).put("almaTrust",almaTrust).put("itachiTrust",itachiTrust);}
    static LiviaLife restore(JSONObject j,LiviaStory l){LiviaLife x=new LiviaLife();if(j==null){if(l.romance)x.love=Math.max(35,l.affection/2);return x;}x.lastDay=j.optInt("lastDay",-1);x.independentDays=Math.max(0,j.optInt("independentDays"));x.lastGroupDay=j.optInt("lastGroupDay",-1);x.lastGiftDay=j.optInt("lastGiftDay",-1);x.lastHomeDay=j.optInt("lastHomeDay",-1);x.engagementDay=j.optInt("engagementDay",-1);x.love=l.romance?LiviaStory.clamp(j.optInt("love")):0;x.groupTalks=Math.max(0,j.optInt("groupTalks"));x.homeEvenings=Math.max(0,j.optInt("homeEvenings"));x.training=LiviaStory.clamp(j.optInt("training"));x.married=l.romance&&j.optBoolean("married");x.engaged=l.romance&&!x.married&&j.optBoolean("engaged");x.sharedHome=x.married&&j.optBoolean("sharedHome");x.activity=j.optString("activity",x.activity);x.katherineTrust=LiviaStory.clamp(j.optInt("katherineTrust",50));x.almaTrust=LiviaStory.clamp(j.optInt("almaTrust",50));x.itachiTrust=LiviaStory.clamp(j.optInt("itachiTrust",50));
        JSONObject raw=j.optJSONObject("pack");if(raw!=null){Iterator<String> it=raw.keys();while(it.hasNext()){String id=it.next();if(!eligible(id))continue;ItemCatalog.Item item=ItemCatalog.find(id);int n=Math.min(100,Math.max(0,raw.optInt(id)));while(n>0&&x.weight()+n*item.weight>35.00001)n--;x.count(id,n);}}
        JSONObject eq=j.optJSONObject("gear");if(eq!=null){Iterator<String> it=eq.keys();while(it.hasNext()){String slot=it.next(),id=eq.optString(slot);if(eligible(id)&&x.pack.optInt(id)>0&&slot.equals(ItemCatalog.find(id).slot()))try{x.gear.put(slot,id);}catch(JSONException ignored){}}}return x;}
}
