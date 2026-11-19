package dev.infend.skyrimtext;

import java.util.Locale;

/** Presentation only: never adds damage or converts a resistance into damage. */
final class ItemEffectStyle {
    static final ItemEffectStyle FIRE=new ItemEffectStyle("fire",0xffff7043);
    static final ItemEffectStyle FROST=new ItemEffectStyle("frost",0xff64d8ff);
    static final ItemEffectStyle SHOCK=new ItemEffectStyle("shock",0xffb794f6);
    static final ItemEffectStyle POISON=new ItemEffectStyle("poison",0xff8bc34a);
    static final ItemEffectStyle SUN=new ItemEffectStyle("sun",0xffffd99a);
    static final ItemEffectStyle MAGIC=new ItemEffectStyle("magic",0xffb0a2db);
    static final ItemEffectStyle HEALTH=new ItemEffectStyle("health",0xffff5252);
    static final ItemEffectStyle STAMINA=new ItemEffectStyle("stamina",0xff66bb6a);
    static final ItemEffectStyle SOUL=new ItemEffectStyle("soul",0xff9d8de1);
    static final ItemEffectStyle ABSORB_HEALTH=new ItemEffectStyle("absorb_health",0xffff5252);
    static final ItemEffectStyle ABSORB_MAGIC=new ItemEffectStyle("absorb_magic",0xff42a5f5);
    static final ItemEffectStyle ABSORB_STAMINA=new ItemEffectStyle("absorb_stamina",0xff66bb6a);
    static final ItemEffectStyle INFO=new ItemEffectStyle("info",0xffa3a3a3);
    static final ItemEffectStyle MANA=new ItemEffectStyle("mana",0xff42a5f5);
    static final ItemEffectStyle CHAOS=new ItemEffectStyle("chaos",0xffc084fc);
    static final ItemEffectStyle PARALYSIS=new ItemEffectStyle("lock",0xfffdd835);
    static final ItemEffectStyle SLOW=new ItemEffectStyle("hourglass",0xff81d4fa);
    static final ItemEffectStyle FEAR=new ItemEffectStyle("ghost",0xffb39ddb);
    static final ItemEffectStyle FRENZY=new ItemEffectStyle("brain",0xfff06292);
    static final ItemEffectStyle CALM=new ItemEffectStyle("hand",0xff90caf9);
    static final ItemEffectStyle BLEED=new ItemEffectStyle("blood",0xffe53935);
    static final ItemEffectStyle BLOCK=new ItemEffectStyle("shield",0xffc7a46b);
    static final ItemEffectStyle DISEASE=new ItemEffectStyle("shield",0xff80cbc4);
    static final ItemEffectStyle ARMOR=new ItemEffectStyle("shield",0xffb0bec5);
    static final ItemEffectStyle SPELL_ABSORB=new ItemEffectStyle("shield",0xffb794f6);
    static final ItemEffectStyle CRAFT=new ItemEffectStyle("hammer",0xffc7a46b);
    static final ItemEffectStyle ALCHEMY=new ItemEffectStyle("poison",0xff8bc34a);
    static final ItemEffectStyle SNEAK=new ItemEffectStyle("eye_off",0xff90a4ae);
    static final ItemEffectStyle SPEED=new ItemEffectStyle("footprints",0xff80cbc4);
    static final ItemEffectStyle SPEECH=new ItemEffectStyle("speech",0xff80cbc4);
    static final ItemEffectStyle GOLD=new ItemEffectStyle("septims",0xffffd54f);
    static final ItemEffectStyle WEIGHT=new ItemEffectStyle("bag",0xffc7a46b);
    static final ItemEffectStyle COMBAT=new ItemEffectStyle("weapon",0xffe0ae70);
    static final ItemEffectStyle CRITICAL=new ItemEffectStyle("target",0xffef5350);
    static final ItemEffectStyle ARCHERY=new ItemEffectStyle("target",0xffe0ae70);
    static final ItemEffectStyle WATER=new ItemEffectStyle("waves",0xff4fc3f7);
    static final ItemEffectStyle NIGHT=new ItemEffectStyle("moon",0xff90caf9);
    static final ItemEffectStyle DETECT=new ItemEffectStyle("eye",0xffffb74d);
    static final ItemEffectStyle UNDEAD=new ItemEffectStyle("skull",0xffb0bec5);
    static final ItemEffectStyle BANISH=new ItemEffectStyle("ban",0xffce93d8);
    static final ItemEffectStyle KEY=new ItemEffectStyle("key",0xffc7a46b);
    static final ItemEffectStyle SHOUT=new ItemEffectStyle("wind",0xff80cbc4);
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
        if(any(e,"поглощение заклин","поглощение магическ"))return SPELL_ABSORB;
        if(any(e,"хаос"))return CHAOS;
        if(any(e,"паралич","парализ"))return PARALYSIS;
        if(any(e,"замедлен")&&!any(e,"регенерац"))return SLOW;
        if(any(e,"страх","испуг","обращает в бегство"))return FEAR;
        if(any(e,"бешенств","ярост"))return FRENZY;
        if(any(e,"умиротвор","успокоен"))return CALM;
        if(any(e,"кровотеч"))return BLEED;
        if(any(e,"изгнание даэдр","изгоняет даэдр"))return BANISH;
        if(any(e,"изгнание нежити","обнаружение смерти","отпугивает нежить"))return UNDEAD;
        if(any(e,"огн","пламен","горени","разрушения"))return FIRE;
        if(any(e,"мороз","холод","ледян","льдом","льда"))return FROST;
        if(any(e,"электр","молни","грозов","разряд"))return SHOCK;
        if(any(e,"солнеч","солнцем","светом"))return SUN;
        if(any(e,"ядом","яду","ядов","ядам","отрав","яд ","яда","яды")||e.equals("яд"))return POISON;
        if(any(e,"болезн"))return DISEASE;
        if(any(e,"блокирован","эффективность блока","отражение урона"))return BLOCK;
        if(any(e,"брон","физическая защита"))return ARMOR;
        if(any(e,"кузнеч","улучшение оружия","улучшение доспех"))return CRAFT;
        if(any(e,"алхими"))return ALCHEMY;
        if(any(e,"взлом","отмыч"))return KEY;
        if(any(e,"скрытност","невидим"))return SNEAK;
        if(any(e,"приглушение шагов","бесшум","скорость передвиж"))return SPEED;
        if(any(e,"краснореч","карманные краж"))return SPEECH;
        if(any(e,"цены","торгов","септим"))return GOLD;
        if(any(e,"грузопод","переносимый вес","переносим"))return WEIGHT;
        if(any(e,"критическ"))return CRITICAL;
        if(any(e,"стрельб","урон лук"))return ARCHERY;
        if(any(e,"одноруч","двуруч","безоруж","скорость атаки"))return COMBAT;
        if(any(e,"подводное дыхание","хождение по воде"))return WATER;
        if(any(e,"ночное зрение","тихих лун"))return NIGHT;
        if(any(e,"обнаружение жизни"))return DETECT;
        if(any(e,"крик"))return SHOUT;
        if(any(e,"воскреш","колдовств","призыв"))return FEAR;
        if(any(e,"захват душ","ловушка душ","ловушку душ","пленение душ"))return SOUL;
        if(any(e,"сопротивление магии","уязвимость к магии","магическ","зачар","иллюзии","изменения","заклинан"))return MAGIC;
        if(any(e,"магии","магию","маны","мана"))return MANA;
        if(any(e,"запас сил","запаса сил","запасу сил","вынослив"))return STAMINA;
        if(any(e,"здоров","жизн"))return HEALTH;
        return INFO;
    }
    static boolean damageEnchantment(String effect){
        String e=norm(effect);
        if(!e.contains("урон")||any(e,"сопротивлен","уязвим","повышен"))return false;
        ItemEffectStyle style=classify(e,"");
        return style==FIRE||style==FROST||style==SHOCK||style==SUN||style==MAGIC||style==MANA||style==SOUL||style==CHAOS;
    }
}
