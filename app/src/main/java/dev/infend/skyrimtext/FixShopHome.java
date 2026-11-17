package dev.infend.skyrimtext;
import org.json.JSONObject;

/** Purchased furnishings and city residences are local to a game save. */
final class FixShopHome {
    static final String[] CITY={"Вайтран","Рифтен","Виндхельм","Маркарт","Солитьюд"};
    static final String[] NAME={"Дом тёплых ветров","Медовик","Хьерим","Влиндрел-холл","Высокий шпиль"};
    static final int[] PRICE={300,400,500,450,600};
    static final String[] ROOM={"Уютная спальня","Кухня","Алхимическая лаборатория","Личная кузница"};
    static final int[] ROOM_PRICE={120,150,250,250};
    int houses=0,rooms=0;
    int lastMeal=-1;
    boolean owns(int i){return i>=0&&i<NAME.length&&(houses&(1<<i))!=0;}
    boolean buy(int i){if(i<0||i>=NAME.length||owns(i))return false;houses|=1<<i;return true;}
    boolean room(int i){return i>=0&&i<ROOM.length&&(rooms&(1<<i))!=0;}
    boolean buyRoom(GameEngine g,int i){if(i<0||i>=ROOM.length||room(i)||!hasHome(g))return false;rooms|=1<<i;return true;}
    boolean hasHome(GameEngine g){if(houses!=0)return true;for(int r:new int[]{5,7,8})if(RoadEvents.ownsEstate(g,r))return true;return false;}
    String here(GameEngine g){for(int i=0;i<CITY.length;i++)if(owns(i)&&CITY[i].equals(g.progress.location))return NAME[i];for(int r:new int[]{5,7,8})if(RoadEvents.ownsEstate(g,r)&&RoadEvents.estate(r).equals(g.progress.location))return RoadEvents.estate(r);return "";}
    static void choices(GameEngine g,GameEngine.Scene s){if(!FixShopWorld.safe(g))return;FixShopHome h=g.fixShop.home;if(h.here(g).isEmpty())return;s.add("FIXRESIDENCE:rest","Отдохнуть: "+h.here(g),h.room(0)?"8 часов | улучшенный отдых":"8 часов | бесплатно");if(h.room(1)&&h.lastMeal!=g.day)s.add("FIXRESIDENCE:meal","Поесть дома","Бесплатно | один раз в день");if(h.room(2)&&h.lastAlchemy!=g.day)s.add("FIXRESIDENCE:alchemy","Алхимическая лаборатория","Практика алхимии | 1 час");if(h.room(2)&&canBrew(g))s.add("FIXRESIDENCE:brew","Сварить лечебное зелье","Пшеница + голубой горноцвет");if(h.room(3)&&h.lastForge!=g.day)s.add("FIXRESIDENCE:forge","Личная кузница","Практика кузнечного дела | 1 час");}
    int lastAlchemy=-1,lastForge=-1;
    static boolean canBrew(GameEngine g){return g.potions<Integer.MAX_VALUE&&g.progress.quantity("Пшеница")>0&&g.progress.quantity("Голубой горноцвет")>0&&g.progress.carryWeight()+0.3<=g.progress.carryLimit()+0.00001;}
    static boolean brew(GameEngine g){if(!canBrew(g))return false;if(!g.progress.removeItem("Пшеница",1))return false;if(!g.progress.removeItem("Голубой горноцвет",1)){g.progress.addItem("Пшеница",1);return false;}g.potions++;g.progress.skillExperience(g,7,25);g.progress.sync(g);g.message="Ты приготовил лечебное зелье.";g.mode="RESULT";return true;}
    static boolean choose(GameEngine g,String a){if(!FixShopWorld.safe(g))return false;FixShopHome h=g.fixShop.home;if(h.here(g).isEmpty())return false;
        if("FIXRESIDENCE:rest".equals(a)){if(!g.restHours(8,true))return false;if(h.room(0)){g.hp=g.maxHp;g.magicka=g.maxMagicka;g.stamina=g.maxStamina;}g.mode="RESULT";return true;}
        if("FIXRESIDENCE:meal".equals(a)&&h.room(1)&&h.lastMeal!=g.day){h.lastMeal=g.day;g.needs.eat();g.needs.drink();g.message="Ты поел и выпил воды в собственном доме.";g.mode="RESULT";return true;}
        if("FIXRESIDENCE:brew".equals(a))return h.room(2)&&brew(g);
        boolean alchemy="FIXRESIDENCE:alchemy".equals(a),forge="FIXRESIDENCE:forge".equals(a);if(!alchemy&&!forge)return false;
        int day=g.day;if(alchemy?(!h.room(2)||h.lastAlchemy==day):(!h.room(3)||h.lastForge==day))return false;
        if(!g.restHours(1,false))return false;int skill=-1;for(int i=0;i<RpgContent.SKILLS.length;i++)if(RpgContent.SKILLS[i].equalsIgnoreCase(alchemy?"Алхимия":"Кузнечное дело"))skill=i;
        if(alchemy)h.lastAlchemy=g.day;else h.lastForge=g.day;if(skill>=0)g.progress.skillExperience(g,skill,25);g.message="Час практики в собственном доме завершён.";g.mode="RESULT";return true;}
    JSONObject save(){JSONObject j=new JSONObject();try{j.put("houses",houses).put("rooms",rooms).put("lastMeal",lastMeal).put("lastAlchemy",lastAlchemy).put("lastForge",lastForge);}catch(Exception ignored){}return j;}
    static FixShopHome restore(JSONObject j){FixShopHome h=new FixShopHome();if(j==null)return h;h.houses=j.optInt("houses")&31;h.rooms=j.optInt("rooms")&15;h.lastMeal=j.optInt("lastMeal",-1);h.lastAlchemy=j.optInt("lastAlchemy",-1);h.lastForge=j.optInt("lastForge",-1);return h;}
}
