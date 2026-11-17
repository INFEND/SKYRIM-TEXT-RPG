package dev.infend.skyrimtext;
import org.json.JSONObject;

/** Optional purchased powers, owned and enabled independently in a single game save. */
final class FixShopGifts {
    static final int FATE=0,PHOENIX=1,WARD=2,MENTOR=3,INSURANCE=4,ANCHOR=5,POCKET=6,PURIFY=7,BLOOD=8;
    static final String[] NAME={"Благосклонность судьбы","Клятва Феникса","Щит вечной зимы","Эхо древних наставников","Золотой обет","Якорь пространства","Карман Обливиона","Омовение Этериуса","Кровавый обмен"};
    static final String[] DESC={"Один повтор неудачной проверки за игровой день. Второй бросок сохраняет исходный шанс.","Один раз в день пережить смертельный удар с 35% здоровья, не завершив бой.","Иммунитет к отравлению и обморожению в обычном бою.","Дополнительные 25% опыта изучаемого навыка при обычной практике, без лишнего опыта героя.","Один раз в день сохранить септимы при поражении. Само поражение остаётся.","Запомнить посещённую точку и вернуться к ней один раз за игровой день.","Доступ к общему хранилищу из любой безопасной локации, без необходимости владеть домом.","Один раз за день восстановить включённые потребности и снять яд и мороз вне боя.","В бою обменять 20% максимального здоровья на 40% максимальной магии один раз за сражение."};
    static final int[] PRICE={300,650,400,500,250,350,500,300,350};
    static final int ALL=(1<<NAME.length)-1;
    int owned,enabled,fateDay=-1,phoenixDay=-1,insuranceDay=-1,anchorDay=-1,purifyDay=-1;
    boolean bloodUsed=false;
    String anchor="";
    boolean owns(int i){return i>=0&&i<NAME.length&&(owned&(1<<i))!=0;}
    boolean on(int i){return owns(i)&&(enabled&(1<<i))!=0;}
    boolean buy(int i){if(i<0||i>=NAME.length||owns(i))return false;owned|=1<<i;enabled|=1<<i;return true;}
    boolean toggle(int i){if(!owns(i))return false;enabled^=1<<i;return true;}
    boolean retry(int day,int chance,int roll){if(!on(FATE)||fateDay==day||chance<=0||chance>=100||roll<=chance)return false;fateDay=day;return true;}
    boolean rescue(GameEngine g){if(!on(PHOENIX)||phoenixDay==g.day||g.hp>0)return false;phoenixDay=g.day;g.hp=Math.max(1,(int)((long)g.maxHp*35/100));g.poisonTurns=g.frostTurns=0;return true;}
    boolean insure(int day){if(!on(INSURANCE)||insuranceDay==day)return false;insuranceDay=day;return true;}
    int skillBonus(int amount){return on(MENTOR)?amount/4:0;}
    boolean mark(GameEngine g){if(!on(ANCHOR)||!FixShopWorld.safe(g)||WorldAtlas.find(g.progress.location)==null||!g.visitedVisual.contains(g.progress.location)||anchor.equals(g.progress.location))return false;anchor=g.progress.location;g.message="Якорь пространства установлен: "+anchor;return true;}
    boolean recall(GameEngine g){if(!on(ANCHOR)||anchorDay==g.day||!FixShopWorld.safe(g)||anchor.isEmpty()||!g.visitedVisual.contains(anchor)||!g.fixTravelToVisited(anchor))return false;anchorDay=g.day;return true;}
    boolean purify(GameEngine g){if(!on(PURIFY)||purifyDay==g.day||!FixShopWorld.safe(g))return false;g.needs.sync(g.day,g.hour);boolean needed=g.poisonTurns>0||g.frostTurns>0;for(int i=0;i<4;i++)needed|=g.needs.enabled(i)&&g.needs.value(i)<100;if(!needed)return false;purifyDay=g.day;g.needs.tavernRest();g.poisonTurns=g.frostTurns=0;g.message="Омовение Этериуса восстановило потребности и сняло яд и мороз.";return true;}
    void beginBattle(){bloodUsed=false;}
    boolean canBlood(GameEngine g){return on(BLOOD)&&!bloodUsed&&"COMBAT".equals(g.mode)&&g.hp>Math.max(1,g.maxHp/5)&&g.magicka<g.maxMagicka;}
    boolean blood(GameEngine g){if(!canBlood(g))return false;bloodUsed=true;g.hp-=Math.max(1,g.maxHp/5);g.magicka=(int)Math.min(g.maxMagicka,(long)g.magicka+Math.max(1,(long)g.maxMagicka*2/5));return true;}
    String status(int i,GameEngine g){if(!owns(i))return "Не приобретено";if(!on(i))return "Выключено";int used=i==FATE?fateDay:i==PHOENIX?phoenixDay:i==INSURANCE?insuranceDay:i==ANCHOR?anchorDay:i==PURIFY?purifyDay:-1;return used==g.day?"Использовано сегодня":i==ANCHOR&&!anchor.isEmpty()?"Якорь: "+anchor:"Активно";}
    static void choices(GameEngine g,GameEngine.Scene s){if(!FixShopWorld.safe(g))return;FixShopGifts f=g.fixShop.gifts;if(f.on(ANCHOR)){if(!f.anchor.equals(g.progress.location)&&g.visitedVisual.contains(g.progress.location))s.add("FIXGIFT:mark","Установить якорь пространства",g.progress.location);if(f.anchorDay!=g.day&&!f.anchor.isEmpty()&&!f.anchor.equals(g.progress.location))s.add("FIXGIFT:recall","Вернуться к якорю",f.anchor);}if(f.on(PURIFY)&&f.purifyDay!=g.day)s.add("FIXGIFT:purify","Омовение Этериуса","Восстановить потребности");}
    static boolean choose(GameEngine g,String a){FixShopGifts f=g.fixShop.gifts;boolean ok="FIXGIFT:mark".equals(a)?f.mark(g):"FIXGIFT:recall".equals(a)?f.recall(g):"FIXGIFT:purify".equals(a)&&f.purify(g);if(ok)g.recordVisualEvent(g.message);return ok;}
    JSONObject save(){JSONObject j=new JSONObject();try{j.put("owned",owned).put("enabled",enabled).put("fateDay",fateDay).put("phoenixDay",phoenixDay).put("insuranceDay",insuranceDay).put("anchorDay",anchorDay).put("purifyDay",purifyDay).put("anchor",anchor).put("bloodUsed",bloodUsed);}catch(Exception ignored){}return j;}
    static FixShopGifts restore(JSONObject j){FixShopGifts f=new FixShopGifts();if(j==null)return f;f.owned=j.optInt("owned")&ALL;f.enabled=j.optInt("enabled")&f.owned;f.fateDay=j.optInt("fateDay",-1);f.phoenixDay=j.optInt("phoenixDay",-1);f.insuranceDay=j.optInt("insuranceDay",-1);f.anchorDay=j.optInt("anchorDay",-1);f.purifyDay=j.optInt("purifyDay",-1);String a=j.optString("anchor");if(WorldAtlas.find(a)!=null)f.anchor=a;f.bloodUsed=j.optBoolean("bloodUsed");return f;}
}
