package dev.infend.skyrimtext;
import org.json.JSONObject;
import java.util.Locale;

/** Turn-based adaptations; all persistent powers are owned by the current save. */
final class RoadArtifacts {
    private static final String[] NAMES={"","Воин","Маг","Вор","Любовник","Конь","Лорд","Леди","Ученик","Атронах","Ритуал","Змей","Тень","Башня"};
    private static final String[] DETAILS={"","Опыт боевых навыков +20%","Опыт магических навыков +20%","Опыт скрытных навыков +20%","Опыт всех навыков +15%","Переносимый вес +100 кг","Сопротивление урону +10%","Восстановление здоровья и сил","Усиление восстановления магии; уязвимость к магии","Сопротивление магии; замедление восстановления магии","Раз в день: призвать мертвеца на три хода","Раз в день: ядовитый удар и ослабление ответа","Раз в день: скрыться от атаки","Раз в день: облегчить следующую проверку взлома"};
    int stone=0,previous=0,usedDay=-1,towerDay=-1,towerConsumedDay=-1,trapTurns=0,whiteSoul=0,blackSoul=0;
    final int[] powerDays={-1,-1,-1};
    boolean powerReady(GameEngine g,int id){return id>=10&&id<=12&&has(g.progress,id)&&powerDays[id-10]!=g.day;}
    void usePower(GameEngine g,int id){powerDays[id-10]=g.day;}
    boolean has(RpgProgress p,int id){return stone==id||(previous==id&&p.equipped("Этериевая корона"));}
    String summary(RpgProgress p){return stone==0?"Камни-хранители: не активированы":"Камень "+NAMES[stone]+(previous>0&&p.equipped("Этериевая корона")?" | Корона: "+NAMES[previous]:"");}
    int skillBonus(RpgProgress p,int index,int amount){int percent=has(p,4)?15:0;if(index<=6&&has(p,1)||((index==8||index>=13)&&has(p,2))||((index==7||index>=9&&index<=12)&&has(p,3)))percent+=20;return (int)((long)amount*percent/100);}
    int carryBonus(RpgProgress p){return has(p,5)?100:0;}
    double reduction(RpgProgress p){return has(p,6)?.10:0;}
    int regen(RpgProgress p,int i){return (has(p,7)&&i!=1?1:0)+(i==1&&has(p,8)?2:0)-(i==1&&has(p,9)?1:0);}
    int magicAdjust(RpgProgress p,int incoming){int n=incoming;if(has(p,8))n+=incoming/2;if(has(p,9))n-=incoming/2;return Math.max(0,n);}
    private static final String[] MAP_NAMES={"","Камень Хранитель","Камень Хранитель","Камень Хранитель","Камень Любовника","Камень Коня","Камень Лорда","Камень Леди","Камень Ученика","Камень Атронаха","Камень Ритуала","Камень Змея","Камень Тени","Камень Башни"};
    private boolean at(GameEngine g,int id){if(id<1||id>=NAMES.length)return false;String place=g.progress.location;return MAP_NAMES[id].equalsIgnoreCase(place)||(id>=4&&("Камень "+NAMES[id]).equalsIgnoreCase(place))||(id<=3&&"Камни-хранители".equalsIgnoreCase(place));}
    void choices(GameEngine g,GameEngine.Scene s){for(int i=1;i<NAMES.length;i++)if(at(g,i)&&stone!=i)s.add("ROADSTONE:"+i,"Активировать: "+NAMES[i],DETAILS[i]);if(has(g.progress,13)&&towerDay!=g.day)s.add("ROADSTONE_TOWER","Сила Башни","Следующая проверка взлома сегодня получает +35%");
        if(whiteSoul>0&&g.progress.quantity("Звезда Азуры")>0)s.add("ROADSTAR:release:1","Высвободить душу из Звезды Азуры","Восстановление "+whiteSoul+" магии");
        if(blackSoul>0&&g.progress.quantity("Звезда Чёрной Азуры")>0)s.add("ROADSTAR:release:2","Высвободить душу из Чёрной звезды","Восстановление "+blackSoul+" магии");}
    void activate(GameEngine g,String action){if(g.road.pending()||!("WORLD".equals(g.mode)||"RESULT".equals(g.mode)))return;int id;try{id=Integer.parseInt(action.substring("ROADSTONE:".length()));}catch(Exception ex){return;}if(id<1||id>=NAMES.length||!at(g,id)||id==stone)return;previous=stone;stone=id;g.message="Камень "+NAMES[id]+": "+DETAILS[id]+". "+summary(g.progress);g.mode="RESULT";g.recordVisualEvent(g.message);}
    void combatChoices(GameEngine g,GameEngine.Scene s){{if(powerReady(g,10)&&g.roadSummonTurns==0)s.add("ROADSTONE_RITUAL","Сила Ритуала","Призвать мертвеца | 3 хода");if(powerReady(g,11))s.add("ROADSTONE_SERPENT","Сила Змея","Яд и ослабление ответа");if(powerReady(g,12))s.add("ROADSTONE_SHADOW","Сила Тени","Уклониться от ответной атаки");}
        if(trapTurns==0&&g.magicka>=10&&((whiteSoul==0&&g.progress.quantity("Звезда Азуры")>0)||(blackSoul==0&&g.progress.quantity("Звезда Чёрной Азуры")>0)))s.add("ROADSTAR_TRAP","Захват души","10 магии | 3 хода");
        if(whiteSoul>0&&g.progress.quantity("Звезда Азуры")>0)s.add("ROADSTAR_WHITE","Высвободить белую душу","Восстановить магию");
        if(blackSoul>0&&g.progress.quantity("Звезда Чёрной Азуры")>0)s.add("ROADSTAR_BLACK","Высвободить чёрную душу","Восстановить магию");}
    boolean useTower(GameEngine g){if(g.road.pending()||!"WORLD".equals(g.mode)||!has(g.progress,13)||towerDay==g.day)return false;towerDay=g.day;g.message="Камень Башни помогает при следующей проверке взлома сегодня.";return true;}
    int towerBonus(GameEngine g,int skill){return skill==10&&towerDay==g.day&&towerConsumedDay!=g.day&&has(g.progress,13)?35:0;}
    void consumeTower(GameEngine g,int skill){if(towerBonus(g,skill)>0)towerConsumedDay=g.day;}
    boolean white(String name){String n=name.toLowerCase(Locale.ROOT);for(String animal:new String[]{"волк","волки","медвед","саблезуб","злокрыс","паук","тролль","мамонт","нетч","хоркер","грязекраб","олень","коза","лошадь","пёс","собака","гигант","великан","спригган","ворожея","атронах","драугр"})if(n.contains(animal))return true;return false;}
    boolean human(String name){String n=name.toLowerCase(Locale.ROOT);for(String person:new String[]{"бандит","разбойник","марод","маг","некромант","колдун","ведьм","вампир","ассасин","убийца","наёмник","солдат","офицер","стражник","юстициар","культист","изгой","пират","путник","охотник","орк","импер","брат бури","братья бури","трон","вор ","головорез","малин","оркендор","халдин"})if(n.contains(person))return true;return false;}
    String capture(GameEngine g,String name){if(trapTurns<=0)return "";trapTurns=0;int value=Math.min(60,20+g.level);if(white(name)&&!human(name)&&whiteSoul==0&&g.progress.quantity("Звезда Азуры")>0){whiteSoul=value;return "\nЗвезда Азуры захватывает белую душу.";}if(human(name)&&blackSoul==0&&g.progress.quantity("Звезда Чёрной Азуры")>0){blackSoul=Math.min(100,value+20);return "\nЧёрная звезда захватывает чёрную душу.";}return "\nПодходящая пустая звезда отсутствует; душа не захвачена.";}
    boolean filled(String item){return "Звезда Азуры".equals(item)&&whiteSoul>0||"Звезда Чёрной Азуры".equals(item)&&blackSoul>0;}
    int release(GameEngine g,int kind){if(g.magicka>=g.maxMagicka)return 0;int value=kind==1?whiteSoul:blackSoul;String item=kind==1?"Звезда Азуры":"Звезда Чёрной Азуры";if(value<=0||g.progress.quantity(item)==0)return 0;int actual=Math.min(value,g.maxMagicka-g.magicka);g.magicka+=actual;if(kind==1)whiteSoul=0;else blackSoul=0;return actual;}
    void releaseOutside(GameEngine g,String action){if(g.road.pending()||!("WORLD".equals(g.mode)||"RESULT".equals(g.mode)))return;int k=action.equals("ROADSTAR:release:1")?1:action.equals("ROADSTAR:release:2")?2:0;if(k==0)return;int n=release(g,k);g.message=n>0?"Душа высвобождена. Магия +"+n+". Звезда снова пуста.":"Нет доступной души или магия уже полна.";g.mode="RESULT";}
    JSONObject save(){JSONObject j=new JSONObject();try{j.put("stone",stone).put("previous",previous).put("usedDay",usedDay).put("towerDay",towerDay).put("towerConsumedDay",towerConsumedDay).put("powerDays",new org.json.JSONArray(powerDays)).put("trapTurns",trapTurns).put("whiteSoul",whiteSoul).put("blackSoul",blackSoul);}catch(Exception ignored){}return j;}
    static RoadArtifacts restore(JSONObject j){RoadArtifacts a=new RoadArtifacts();if(j!=null){a.stone=Math.max(0,Math.min(13,j.optInt("stone")));a.previous=Math.max(0,Math.min(13,j.optInt("previous")));if(a.previous==a.stone)a.previous=0;a.usedDay=j.optInt("usedDay",-1);a.towerDay=j.optInt("towerDay",-1);a.towerConsumedDay=j.optInt("towerConsumedDay",-1);org.json.JSONArray powers=j.optJSONArray("powerDays");for(int i=0;i<3;i++)a.powerDays[i]=powers==null?a.usedDay:powers.optInt(i,-1);a.trapTurns=Math.max(0,Math.min(3,j.optInt("trapTurns")));a.whiteSoul=Math.max(0,Math.min(60,j.optInt("whiteSoul")));a.blackSoul=Math.max(0,Math.min(100,j.optInt("blackSoul")));}return a;}
}
