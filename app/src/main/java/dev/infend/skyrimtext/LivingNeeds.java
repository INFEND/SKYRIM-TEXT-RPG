package dev.infend.skyrimtext;

import org.json.JSONObject;

/** Needs follow committed world hours, not device time. Old saves start with full needs. */
public final class LivingNeeds {
    public int food=100, water=100, awake=100, rest=100;
    private long lastHour=-1;
    private static int clamp(int n){return Math.max(0,Math.min(100,n));}
    public void sync(int day,int hour){
        long now=(long)(Math.max(1,day)-1)*24+Math.max(0,Math.min(23,hour));
        if(lastHour<0){lastHour=now;return;}
        if(now<=lastHour)return;
        int elapsed=(int)Math.min(100,now-lastHour);lastHour=now;
        food=clamp(food-elapsed*2);water=clamp(water-elapsed*3);
        awake=clamp(awake-elapsed*3);rest=clamp(rest-elapsed*2);
    }
    public void eat(){food=clamp(food+25);}
    public void drink(){water=clamp(water+35);}
    public void recover(int hours,boolean sleep){
        rest=clamp(rest+hours*10);if(sleep)awake=clamp(awake+hours*15);
    }
    public int penalty(){return (food<25?5:0)+(water<25?8:0)+(awake<35?8:0)+(rest<25?5:0);}
    public JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("food",food).put("water",water).put("awake",awake).put("rest",rest).put("lastHour",lastHour);}catch(Exception ignored){}return o;}
    public static LivingNeeds fromJson(JSONObject o){LivingNeeds n=new LivingNeeds();if(o!=null){n.food=clamp(o.optInt("food",100));n.water=clamp(o.optInt("water",100));n.awake=clamp(o.optInt("awake",100));n.rest=clamp(o.optInt("rest",100));n.lastHour=o.optLong("lastHour",-1);}return n;}
}
