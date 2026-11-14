package dev.infend.skyrimtext;

import java.util.Locale;

/** Presentation only: never adds damage or converts a resistance into damage. */
final class ItemEffectStyle {
    static final ItemEffectStyle FIRE=new ItemEffectStyle("fire",0xffe69b56);
    static final ItemEffectStyle FROST=new ItemEffectStyle("frost",0xff86cce9);
    static final ItemEffectStyle SHOCK=new ItemEffectStyle("shock",0xffe8ce65);
    static final ItemEffectStyle POISON=new ItemEffectStyle("poison",0xff79b966);
    static final ItemEffectStyle SUN=new ItemEffectStyle("sun",0xffffd99a);
    static final ItemEffectStyle MAGIC=new ItemEffectStyle("magic",0xffb0a2db);
    static final ItemEffectStyle HEALTH=new ItemEffectStyle("health",0xffe16c72);
    static final ItemEffectStyle STAMINA=new ItemEffectStyle("stamina",0xff79cc9c);
    static final ItemEffectStyle SOUL=new ItemEffectStyle("soul",0xff9d8de1);
    static final ItemEffectStyle ABSORB_HEALTH=new ItemEffectStyle("absorb_health",0xffe16c72);
    static final ItemEffectStyle ABSORB_MAGIC=new ItemEffectStyle("absorb_magic",0xffb0a2db);
    static final ItemEffectStyle ABSORB_STAMINA=new ItemEffectStyle("absorb_stamina",0xff79cc9c);
    static final ItemEffectStyle INFO=new ItemEffectStyle("info",0xffa3a3a3);
    final String glyph;final int color;
    private ItemEffectStyle(String glyph,int color){this.glyph=glyph;this.color=color;}
    private static String norm(String value){return value==null?"":value.toLowerCase(Locale.ROOT).replace('ё','е');}
    private static boolean any(String s,String... keys){for(String key:keys)if(s.contains(key))return true;return false;}
    static ItemEffectStyle classify(String effect,String name){
        String e=norm(effect);
        if(e.contains("соответствующему урону"))e+=" "+norm(name);
        if(any(e,"поглощ","высасыван")){
            if(any(e,"здоров","жизн"))return ABSORB_HEALTH;
            if(any(e,"запас сил","запаса сил","запасу сил","вынослив"))return ABSORB_STAMINA;
            if(any(e,"магии","магию","маны"))return ABSORB_MAGIC;
        }
        if(any(e,"огн","пламен","горени"))return FIRE;
        if(any(e,"мороз","холод","ледян","льдом","льда"))return FROST;
        if(any(e,"электр","молни","грозов","разряд"))return SHOCK;
        if(any(e,"солнеч","солнцем","светом"))return SUN;
        if(any(e,"ядом","яду","ядов","ядам","отрав"))return POISON;
        if(any(e,"захват душ","ловушка душ","ловушку душ","пленение душ"))return SOUL;
        if(any(e,"магии","магию","магическ","зачар","маны"))return MAGIC;
        if(any(e,"запас сил","запаса сил","запасу сил","вынослив"))return STAMINA;
        if(any(e,"здоров","жизн"))return HEALTH;
        return INFO;
    }
    static boolean damageEnchantment(String effect){
        String e=norm(effect);
        if(!e.contains("урон")||any(e,"сопротивлен","уязвим","повышен"))return false;
        ItemEffectStyle style=classify(e,"");
        return style==FIRE||style==FROST||style==SHOCK||style==SUN||style==MAGIC||style==SOUL;
    }
}
