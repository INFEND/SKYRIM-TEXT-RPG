package dev.infend.skyrimtext;

import org.json.JSONObject;

/** Needs follow committed world hours, not device time. Old saves start with full needs. */
public final class LivingNeeds {
    public int food=100, water=100, awake=100, rest=100;
    public static final int FOOD=0,WATER=1,AWAKE=2,REST=3,ALL=15;
    private int enabledMask=ALL;
    private long lastHour=-1;
    public int enabledMask(){return enabledMask;}
    public boolean anyEnabled(){return enabledMask!=0;}
    public boolean enabled(int need){return need>=0&&need<4&&(enabledMask&(1<<need))!=0;}
    public void setEnabledMask(int mask,int day,int hour){sync(day,hour);enabledMask=mask&ALL;}
    public int value(int need){switch(need){case FOOD:return food;case WATER:return water;case AWAKE:return awake;case REST:return rest;default:return 0;}}
    public static String label(int need){return new String[]{"Сытость","Запас воды","Бодрость","Отдых"}[need];}
    public String aiContext(){
        StringBuilder s=new StringBuilder("Потребности (100 — норма): ");
        for(int i=0;i<4;i++){if(i>0)s.append(", ");s.append(label(i)).append(": ").append(enabled(i)?String.valueOf(value(i)):"отключено");}
        return s.append(". Отключённые потребности не моделировать, не требовать их восстановления и не назначать за них штрафы.").toString();
    }
    private static int clamp(int n){return Math.max(0,Math.min(100,n));}
    public void sync(int day,int hour){
        long now=(long)(Math.max(1,day)-1)*24+Math.max(0,Math.min(23,hour));
        if(lastHour<0){lastHour=now;return;}
        if(now<=lastHour)return;
        int elapsed=(int)Math.min(100,now-lastHour);lastHour=now;
        if(enabled(FOOD))food=clamp(food-elapsed*2);if(enabled(WATER))water=clamp(water-elapsed*3);
        if(enabled(AWAKE))awake=clamp(awake-elapsed*3);if(enabled(REST))rest=clamp(rest-elapsed*2);
    }
    public void eat(){if(enabled(FOOD))food=clamp(food+25);}
    public void drink(){if(enabled(WATER))water=clamp(water+35);}
    public void recover(int hours,boolean sleep){
        if(enabled(REST))rest=clamp(rest+hours*10);if(sleep&&enabled(AWAKE))awake=clamp(awake+hours*15);
    }
    public void tavernRest(){if(enabled(FOOD))food=100;if(enabled(WATER))water=100;if(enabled(AWAKE))awake=100;if(enabled(REST))rest=100;}
    public String penaltyDetails(){return (enabled(FOOD)&&food<25?"Голод: −5 п.п.\n":"")+(enabled(WATER)&&water<25?"Жажда: −8 п.п.\n":"")+(enabled(AWAKE)&&awake<35?"Недосып: −8 п.п.\n":"")+(enabled(REST)&&rest<25?"Усталость: −5 п.п.\n":"");}
    public int penalty(){return (enabled(FOOD)&&food<25?5:0)+(enabled(WATER)&&water<25?8:0)+(enabled(AWAKE)&&awake<35?8:0)+(enabled(REST)&&rest<25?5:0);}
    public JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("food",food).put("water",water).put("awake",awake).put("rest",rest).put("lastHour",lastHour).put("enabledMask",enabledMask);}catch(Exception ignored){}return o;}
    public static LivingNeeds fromJson(JSONObject o){LivingNeeds n=new LivingNeeds();if(o!=null){n.food=clamp(o.optInt("food",100));n.water=clamp(o.optInt("water",100));n.awake=clamp(o.optInt("awake",100));n.rest=clamp(o.optInt("rest",100));n.lastHour=o.optLong("lastHour",-1);n.enabledMask=o.optInt("enabledMask",ALL)&ALL;}return n;}
}
