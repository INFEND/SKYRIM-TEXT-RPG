package dev.infend.skyrimtext;

import java.util.Locale;

/** A deterministic fictional regional climate. No network, random redraws or save migration. */
public final class WorldWeather {
    private static final int[] BASE={8,10,-4,5,6,11,-12,-8,2,0};
    public final int hour,temperature;
    public final String weather,glyph;
    private WorldWeather(int hour,int temperature,String weather,String glyph){
        this.hour=hour;this.temperature=temperature;this.weather=weather;this.glyph=glyph;
    }
    public static WorldWeather at(int region,String location,int day,int hour){
        int r=Math.max(0,Math.min(BASE.length-1,region));
        int h=Math.max(0,Math.min(23,hour));
        String place=location==null?"":location.toLowerCase(Locale.ROOT).replace('ё','е');
        int base=BASE[r];
        boolean peak=place.contains("хротгар")||place.contains("глотка мира");
        boolean underground=place.contains("черный предел");
        if(peak)base=-17;
        if(underground)return new WorldWeather(h,5,"Под землёй","cave");
        // Weather remains stable during each six-hour block and across rerenders.
        int seed=(int)Math.floorMod((long)day*1103515245L+(long)(h/6)*12345L+(long)r*2654435761L,101L);
        int t=base+(int)Math.round(5*Math.cos((h-14)*Math.PI/12.0))+(seed%5)-2;
        if(seed<23)return new WorldWeather(h,t,"Ясно",h<6||h>=21?"moon":"sun");
        if(seed<53)return new WorldWeather(h,t,"Облачно","cloud");
        int blockTemperature=base+(int)Math.round(5*Math.cos((h/6*6+3-14)*Math.PI/12.0))+(seed%5)-2;
        if(blockTemperature<=0)return new WorldWeather(h,t-2,seed>=88?"Метель":"Снег",seed>=88?"blizzard":"snow");
        if(seed<70)return new WorldWeather(h,t,"Туман","fog");
        if(seed<92)return new WorldWeather(h,t-2,"Дождь","rain");
        return new WorldWeather(h,t-3,"Гроза","storm");
    }
    public String time(){return String.format(Locale.ROOT,"%02d:00",hour);}
    public String timeGlyph(){return hour<5||hour>=21?"moon":hour<8?"sunrise":hour>=17?"sunset":"sun";}
    public String formattedTemperature(){return (temperature>0?"+":"")+temperature+" °C";}
}
