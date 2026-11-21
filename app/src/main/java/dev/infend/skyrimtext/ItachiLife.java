package dev.infend.skyrimtext;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Iterator;

/** Persistent equipment, independent activities and companion relationships. */
final class ItachiLife {
    final JSONObject pack=new JSONObject(), equipped=new JSONObject();
    int independentDays=0, lastGroup=-1, groupTalks=0,lastHome=-1,homeEvenings=0,lastStudy=-1,studies=0,engagementDay=-1,lastInitiative=-1;
    int almaTrust=50,katherineTrust=50,liviaTrust=50;
    boolean sharedHome=false,homeReturn=false;
    String goal="Исследовать границу миров",activity="Наблюдает за дорогой";
    static final String[] TASKS={"Исследует руны межмировой печати","Тренирует точность броска","Разведывает безопасные дороги","Изучает защитные заклинания"};
    static final String[] RESULTS={"Итачи сравнила руны печати с записями Коллегии.","Итачи тренировала точные удары на безопасной площадке.","Итачи отметила на карте укрытия и безопасные тропы.","Итачи сопоставила духовную защиту с магией Тамриэля."};
    static int clamp(int v){return Math.max(0,Math.min(100,v));}
    boolean access(GameEngine g,ItachiStory s){return g.started&&s.enabled&&s.stage==ItachiStory.FINAL&&s.present(g)&&!g.road.pending()&&!g.solitude.custody()&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode)||ItachiStory.MODE.equals(g.mode)||SolitudeHome.MODE.equals(g.mode));}
    static boolean eligible(String id){return LiviaLife.eligible(id);}
    boolean canGive(GameEngine g,String id){return eligible(id)&&!g.progress.equipped(id)&&g.progress.temperQuality(id)==0&&g.progress.quantity(id)>0&&!"Походный клинок".equals(id)&&!"Стальной меч".equals(id)&&!"Стальная кираса".equals(id);}
    double weight(){double sum=0;Iterator<String> it=pack.keys();while(it.hasNext()){String id=it.next();ItemCatalog.Item item=ItemCatalog.find(id);if(item!=null)sum+=item.weight*pack.optInt(id);}return sum;}
    private void count(String id,int n){try{if(n<=0)pack.remove(id);else pack.put(id,n);}catch(JSONException e){throw new IllegalStateException(e);}}
    boolean transfer(GameEngine g,ItachiStory s,String id,boolean give){if(!access(g,s)||!eligible(id))return false;ItemCatalog.Item item=ItemCatalog.find(id);int n=pack.optInt(id);
        if(give){if(!canGive(g,id)||n>=100||weight()+item.weight>35.00001||!g.progress.removeItem(id,1))return false;count(id,n+1);}
        else{if(n<=0||!g.progress.addItem(id,1))return false;count(id,n-1);if(n==1&&id.equals(equipped.optString(item.slot())))equipped.remove(item.slot());}g.progress.sync(g);return true;}
    boolean equip(GameEngine g,ItachiStory s,String id){if(!access(g,s)||!eligible(id)||pack.optInt(id)<1)return false;String slot=ItemCatalog.find(id).slot();if(id.equals(equipped.optString(slot)))equipped.remove(slot);else try{equipped.put(slot,id);}catch(JSONException e){return false;}return true;}
    String gearName(String slot,String fallback){String id=equipped.optString(slot);ItemCatalog.Item item=ItemCatalog.find(id);return item!=null&&pack.optInt(id)>0?item.name:fallback;}
    String gearLabel(){return gearName("weapon","Кунаи")+" | "+gearName("body","Чёрное одеяние")+" | "+gearName("shield","Духовная защита");}
    int bonus(boolean weapon){int n=0;Iterator<String> it=equipped.keys();while(it.hasNext()){String slot=it.next(),id=equipped.optString(slot);ItemCatalog.Item item=ItemCatalog.find(id);if(eligible(id)&&pack.optInt(id)>0&&weapon=="weapon".equals(slot))n+=Math.min(8,(int)(item.baseStat/5));}return Math.min(12,n);}
    void daily(GameEngine g,ItachiStory s){int i=Math.floorMod(g.day,TASKS.length);if(s.active()){activity=s.eyeStrain>=60?"Просит привал":"Следит за безопасностью отряда";goal=s.eyeStrain>=60?"Восстановить зрение":"Поддерживать союзников";if(lastInitiative!=g.day&&s.eyeStrain>=60){lastInitiative=g.day;s.remember("Итачи: Не стоит тратить силы впустую. Сделаем привал.");}return;}
        independentDays=GameEngine.nonnegativeAdd(independentDays,1);activity=sharedHome&&SolitudeHome.owns(g)?"Занимается в Высоком шпиле":TASKS[i];goal=TASKS[i];if(i==1)s.blade=clamp(s.blade+1);if(i==3)s.spirit=clamp(s.spirit+1);s.remember(RESULTS[i]);}
    String group(GameEngine g){java.util.ArrayList<String> names=new java.util.ArrayList<>();if(g.alma.active())names.add("Альма");if(g.katherine.active())names.add("Кэтрин");if(g.livia.active())names.add("Ливия");return String.join(", ",names);}
    boolean groupReady(GameEngine g,ItachiStory s){return access(g,s)&&s.active()&&lastGroup!=g.day&&!group(g).isEmpty();}
    String groupLabel(){return "Альма "+almaTrust+"/100 | Кэтрин "+katherineTrust+"/100 | Ливия "+liviaTrust+"/100";}
    boolean otherMarriage(GameEngine g){return g.alma.married||g.katherine.companion.married||g.livia.life.married;}
    boolean atHome(GameEngine g){return "Солитьюд".equalsIgnoreCase(g.progress.location)||g.progress.location.contains("Высокий шпиль");}
    boolean canHome(GameEngine g,ItachiStory s){return s.married&&!sharedHome&&atHome(g)&&SolitudeHome.owns(g);}
    void choices(GameEngine.Scene scene,GameEngine g,ItachiStory s){
        if(lastStudy!=g.day)scene.add("ITACHI:study","Разобрать записи о духовных техниках","Один раз в день | развитие навыков");
        if(groupReady(g,s))scene.add("ITACHI:group","Обсудить тактику с отрядом",group(g));
        if(s.engaged&&!s.married)scene.add("ITACHI:cancelEngagement","Отложить свадьбу","Отношения сохраняются");
        if(canHome(g,s))scene.add("ITACHI:home","Предложить совместный дом","Высокий шпиль");
        if(sharedHome&&s.married&&atHome(g)&&SolitudeHome.owns(g)&&lastHome!=g.day)scene.add("ITACHI:homeEvening","Провести вечер дома","Совместная жизнь");
    }
    boolean choose(GameEngine g,ItachiStory s,String a){if(!access(g,s))return false;switch(a){
        case "ITACHI:study":if(lastStudy!=g.day){lastStudy=g.day;studies=GameEngine.nonnegativeAdd(studies,1);s.spirit=clamp(s.spirit+3);s.illusion=clamp(s.illusion+2);s.gain(20);s.remember("Вы сопоставили записи Коллегии и духовные техники Итачи.");}return true;
        case "ITACHI:group":if(groupReady(g,s)){lastGroup=g.day;groupTalks=GameEngine.nonnegativeAdd(groupTalks,1);StringBuilder b=new StringBuilder();if(g.alma.active()){almaTrust=clamp(almaTrust+3);b.append("Альма и Итачи согласовали применение магии и пути отхода. ");}if(g.katherine.active()){katherineTrust=clamp(katherineTrust+3);b.append("Кэтрин и Итачи распределили разведку и защиту. ");}if(g.livia.active()){liviaTrust=clamp(liviaTrust+3);b.append("Ливия и Итачи обсудили прикрытие отряда. ");}s.respect=clamp(s.respect+2);s.remember(b.toString().trim());}return true;
        case "ITACHI:cancelEngagement":if(s.engaged&&!s.married){s.engaged=false;engagementDay=-1;s.remember("Вы отложили свадьбу по взаимному согласию.");}return true;
        case "ITACHI:home":if(canHome(g,s)){sharedHome=true;s.remember("Вы решили жить в Высоком шпиле.");}return true;
        case "ITACHI:homeEvening":if(s.married&&sharedHome&&atHome(g)&&SolitudeHome.owns(g)&&lastHome!=g.day){lastHome=g.day;homeEvenings=GameEngine.nonnegativeAdd(homeEvenings,1);s.love=clamp(s.love+4);s.attachment=clamp(s.attachment+3);s.recover(2);s.remember("Вы провели вечер дома: Итачи приготовила чай и поделилась планами на завтра.");}return true;
        default:return false;}}
    void endRomance(){sharedHome=false;homeReturn=false;engagementDay=-1;}
    JSONObject save()throws JSONException{return new JSONObject().put("pack",pack).put("equipped",equipped).put("independentDays",independentDays).put("lastGroup",lastGroup).put("groupTalks",groupTalks).put("lastHome",lastHome).put("homeEvenings",homeEvenings).put("lastStudy",lastStudy).put("studies",studies).put("engagementDay",engagementDay).put("lastInitiative",lastInitiative).put("almaTrust",almaTrust).put("katherineTrust",katherineTrust).put("liviaTrust",liviaTrust).put("sharedHome",sharedHome).put("homeReturn",homeReturn).put("goal",goal).put("activity",activity);}
    static ItachiLife restore(JSONObject j,ItachiStory s){ItachiLife x=new ItachiLife();if(j==null)return x;
        x.independentDays=Math.max(0,j.optInt("independentDays"));x.lastGroup=j.optInt("lastGroup",-1);x.groupTalks=Math.max(0,j.optInt("groupTalks"));x.lastHome=j.optInt("lastHome",-1);x.homeEvenings=Math.max(0,j.optInt("homeEvenings"));x.lastStudy=j.optInt("lastStudy",-1);x.studies=Math.max(0,j.optInt("studies"));x.engagementDay=j.optInt("engagementDay",-1);x.lastInitiative=j.optInt("lastInitiative",-1);
        x.almaTrust=clamp(j.optInt("almaTrust",50));x.katherineTrust=clamp(j.optInt("katherineTrust",50));x.liviaTrust=clamp(j.optInt("liviaTrust",50));x.sharedHome=s.married&&j.optBoolean("sharedHome");x.homeReturn=x.sharedHome&&j.optBoolean("homeReturn");x.goal=j.optString("goal",x.goal);x.activity=j.optString("activity",x.activity);
        JSONObject raw=j.optJSONObject("pack");if(raw!=null){Iterator<String> it=raw.keys();while(it.hasNext()){String id=it.next();if(!eligible(id))continue;ItemCatalog.Item item=ItemCatalog.find(id);int n=Math.max(0,Math.min(100,raw.optInt(id)));while(n>0&&x.weight()+n*item.weight>35.00001)n--;x.count(id,n);}}
        JSONObject eq=j.optJSONObject("equipped");if(eq!=null){Iterator<String> it=eq.keys();while(it.hasNext()){String slot=it.next(),id=eq.optString(slot);if(eligible(id)&&x.pack.optInt(id)>0&&slot.equals(ItemCatalog.find(id).slot()))try{x.equipped.put(slot,id);}catch(JSONException ignored){}}}return x;}
}
