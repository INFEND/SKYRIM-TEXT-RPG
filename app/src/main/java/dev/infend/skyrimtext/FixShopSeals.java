package dev.infend.skyrimtext;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;

/** Extra optional mechanics. Separate mask preserves the 24 original gift IDs. */
final class FixShopSeals {
 static final int RIPOSTE=0,MANA_WARD=1,SIPHON=2,HUNTER=3,ELIXIR=4,TRADE=5,SMITH=6,QUIET=7,HOME=8,ARSENAL=9,MEDITATION=10,SLEEP=11;
 static final String[] NAME={"Огненный ответ","Зеркало чар","Пожиратель заклинаний","Око охотника","Эликсир мастера","Договор купеческой гильдии","Кузнечный обет","Незримые тропы","Зов родного очага","Память арсенала","Медитация мудрецов","Лунный покой"};
 static final String[] DESC={"После защиты следующий успешный удар получает до 20 дополнительного урона. Один раз за бой.","Один раз за бой расходовать до 20 магии, поглощая столько же входящего урона.","При попадании магической вспышкой вернуть до 3 потраченных единиц магии.","Обычного одиночного противника, оставшегося с 10% здоровья, добить успешным оружейным ударом. Не действует на боссов, драконов и сюжетные бои.","Лечебные зелья и зелья выносливости восстанавливают на 25% больше.","Скидка до 15% на дополнительные припасы обычных торговцев. Цена не опускается ниже цены продажи.","Один раз в день улучшить предмет, не израсходовав имеющийся материал. Требования к кузнице и навыку сохраняются.","Вдвое снизить вероятность случайных дорожных встреч. Уникальные и заказанные встречи сохраняются.","Один раз в день вернуться в выбранный посещённый собственный дом или поместье.","Сохранить до трёх комплектов экипировки и быстро менять их вне боя. Недостающие предметы не создаются.","Один раз в день выбрать навык и получить 100 опыта этого навыка без затрат времени.","Сон от шести часов дополнительно восстанавливает до четверти максимального здоровья, магии и сил."};
 static final int[] PRICE={450,550,400,650,300,350,450,400,500,600,350,300};
 private static final int MASK=(1<<NAME.length)-1;
 static final String[] SLOTS={"weapon","shield","head","body","hands","feet","ring","neck","ammo"};
 int owned,enabled,smithDay=-1,homeDay=-1,meditationDay=-1;
 boolean riposteReady,riposteUsed,wardUsed;
 final JSONObject[] sets=new JSONObject[3];
 boolean owns(int id){return id>=0&&id<NAME.length&&(owned&(1<<id))!=0;}
 boolean on(int id){return owns(id)&&(enabled&(1<<id))!=0;}
 boolean buy(int id){if(id<0||id>=NAME.length||owns(id))return false;owned|=1<<id;enabled|=1<<id;return true;}
 boolean toggle(int id){if(!owns(id))return false;enabled^=1<<id;if(id==RIPOSTE&&!on(id))riposteReady=false;return true;}
 static boolean safe(GameEngine g){return FixShopWorld.safe(g)&&!g.solitude.custody();}
 void beginBattle(){riposteReady=riposteUsed=wardUsed=false;}
 void defend(){if(on(RIPOSTE)&&!riposteUsed)riposteReady=true;}
 int riposte(int damage){if(!on(RIPOSTE)||!riposteReady||riposteUsed||damage<=0)return 0;riposteReady=false;riposteUsed=true;return Math.min(20,Math.max(1,damage/3));}
 int ward(GameEngine g,int incoming){if(!on(MANA_WARD)||wardUsed||incoming<=0||g.magicka<=0)return 0;int n=Math.min(20,Math.min(incoming,g.magicka));g.magicka-=n;wardUsed=true;return n;}
 int siphon(GameEngine g,int damage){if(!on(SIPHON)||damage<=0)return 0;int n=Math.min(3,Math.min(g.magicCost(),g.maxMagicka-g.magicka));g.magicka+=n;return n;}
 boolean execute(GameEngine g,int damage){if(!on(HUNTER)||damage<=0||damage>=g.enemyHp||!"WILD".equals(g.battleKind)||g.road.pending()||g.road.groupActive(g)||g.enemyMaxHp<=0)return false;String n=g.enemyName.toLowerCase(java.util.Locale.ROOT);return !n.contains("дракон")&&!n.contains("карстааг")&&!n.contains("главарь")&&!n.contains("лорд")&&!n.contains("алдуин")&&g.enemyHp-damage<=Math.max(1,g.enemyMaxHp/10);}
 int potion(int base){return on(ELIXIR)?GameEngine.nonnegativeAdd(base,base/4):base;}
 int price(int base,int floor){return !on(TRADE)||base<=0?base:Math.max(Math.min(base,floor),Math.max(1,(int)(((long)base*85+99)/100)));}
 int encounterChance(int base){return on(QUIET)?base/2:base;}
 boolean saveMaterial(GameEngine g){if(!on(SMITH)||smithDay==g.day||!safe(g))return false;smithDay=g.day;return true;}
 void sleep(GameEngine g,int hours,boolean sleep){if(!on(SLEEP)||!sleep||hours<6)return;g.hp=Math.min(g.maxHp,GameEngine.nonnegativeAdd(g.hp,g.maxHp/4));g.magicka=Math.min(g.maxMagicka,GameEngine.nonnegativeAdd(g.magicka,g.maxMagicka/4));g.stamina=Math.min(g.maxStamina,GameEngine.nonnegativeAdd(g.stamina,g.maxStamina/4));}
 ArrayList<String> homes(GameEngine g){ArrayList<String> a=new ArrayList<>();for(int i=0;i<FixShopHome.CITY.length;i++)if(g.fixShop.home.owns(i))a.add(FixShopHome.CITY[i]);for(int i:new int[]{5,7,8})if(RoadEvents.ownsEstate(g,i))a.add(RoadEvents.estate(i));a.removeIf(n->!g.visitedVisual.contains(n)||WorldAtlas.find(n)==null||n.equals(g.progress.location));return a;}
 boolean returnHome(GameEngine g,String n){if(!on(HOME)||homeDay==g.day||!safe(g)||!homes(g).contains(n)||!g.fixTravelToVisited(n))return false;homeDay=g.day;return true;}
 boolean meditate(GameEngine g,int index){if(!on(MEDITATION)||meditationDay==g.day||!safe(g)||index<0||index>=RpgContent.SKILLS.length||g.progress.skillLevel(index)>=100)return false;meditationDay=g.day;g.progress.skillExperience(g,index,100);g.message="Медитация: "+RpgContent.SKILLS[index]+".";return true;}
 boolean saveSet(GameEngine g,int index){if(!on(ARSENAL)||!safe(g)||index<0||index>=sets.length)return false;JSONObject o=new JSONObject();try{for(String slot:SLOTS){String id=g.progress.equippedName(slot);if(!"Не надето".equals(id))o.put(slot,id);}}catch(Exception ex){return false;}sets[index]=o;return true;}
 boolean loadSet(GameEngine g,int index){if(!on(ARSENAL)||!safe(g)||index<0||index>=sets.length||sets[index]==null)return false;JSONObject o=sets[index];for(String slot:SLOTS){String id=o.optString(slot);if(id.isEmpty())continue;ItemCatalog.Item item=ItemCatalog.find(id);if(item==null||g.progress.quantity(id)<1||!slot.equals(item.slot()))return false;}ItemCatalog.Item weapon=ItemCatalog.find(o.optString("weapon"));if(weapon!=null&&weapon.twoHanded()&&!o.optString("shield").isEmpty())return false;for(String slot:SLOTS)g.progress.unequip(g.progress.equippedName(slot));for(String slot:SLOTS){String id=o.optString(slot);if(!id.isEmpty())g.progress.equip(id);}return true;}
 String setLabel(int i){if(sets[i]==null)return "Пустой комплект";String n=sets[i].optString("weapon","Без оружия");int count=sets[i].length();return n+" | "+count+" предметов";}
 String status(int i,GameEngine g){if(!owns(i))return "Не приобретено";if(!on(i))return "Выключено";int day=i==SMITH?smithDay:i==HOME?homeDay:i==MEDITATION?meditationDay:-1;return day==g.day?"Использовано сегодня":"Активно";}
 JSONObject save(){JSONObject j=new JSONObject();try{JSONArray a=new JSONArray();for(JSONObject o:sets)a.put(o==null?JSONObject.NULL:o);j.put("owned",owned).put("enabled",enabled).put("smithDay",smithDay).put("homeDay",homeDay).put("meditationDay",meditationDay).put("riposteReady",riposteReady).put("riposteUsed",riposteUsed).put("wardUsed",wardUsed).put("sets",a);}catch(Exception ignored){}return j;}
 static FixShopSeals restore(JSONObject j){FixShopSeals s=new FixShopSeals();if(j==null)return s;s.owned=j.optInt("owned")&MASK;s.enabled=j.optInt("enabled")&s.owned;s.smithDay=j.optInt("smithDay",-1);s.homeDay=j.optInt("homeDay",-1);s.meditationDay=j.optInt("meditationDay",-1);s.riposteReady=j.optBoolean("riposteReady");s.riposteUsed=j.optBoolean("riposteUsed");s.wardUsed=j.optBoolean("wardUsed");JSONArray a=j.optJSONArray("sets");if(a!=null)for(int i=0;i<s.sets.length;i++){JSONObject src=a.optJSONObject(i);if(src==null)continue;JSONObject dst=new JSONObject();try{for(String slot:SLOTS){String id=src.optString(slot);if(!id.isEmpty()&&id.length()<=150)dst.put(slot,id);}s.sets[i]=dst;}catch(Exception ignored){}}return s;}
}
