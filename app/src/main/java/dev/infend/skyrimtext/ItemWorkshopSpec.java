package dev.infend.skyrimtext;

/** Stable item type IDs: the first three preserve every historical save. */
final class ItemWorkshopSpec {
    static final String[] TYPES={
        "Одноручное оружие","Кираса","Кольцо","Двуручное оружие","Лук",
        "Посох","Щит","Шлем","Перчатки","Сапоги","Ожерелье",
        "Одежда","Зелье","Свиток","Книга","Разное","Боеприпасы",
        "Ингредиент","Инструмент"
    };
    static final String[] CATEGORY={
        "Оружие","Броня","Украшения","Оружие","Оружие",
        "Оружие","Броня","Броня","Броня","Броня","Украшения",
        "Одежда","Зелья","Свитки","Книги","Разное","Боеприпасы",
        "Ингредиенты","Инструменты"
    };
    static final String[] SLOT={
        "weapon","body","ring","weapon","weapon","weapon",
        "shield","head","hands","feet","neck","body","","","","",
        "ammo","",""
    };
    static final String[] ICON={
        "⚔","⬟","◈","⚔","🏹","✦","⬟","⬟","⬟","⬟",
        "◈","♟","◉","✧","▤","◈","➶","♣","⚒"
    };
    static final int[] SKILL={
        0,4,-1,1,2,2,4,4,4,4,-1,-1,-1,-1,-1,-1,2,-1,-1
    };
    static final double[] DEFAULT_WEIGHT={
        6,8,.2,14,8,8,8,3,2,3,.4,1,.5,.5,1,1,.1,.2,1
    };
    static final String[] RARITIES={
        "Обычный","Необычный","Редкий","Эпический","Легендарный","Мифический"
    };
    private ItemWorkshopSpec(){}
    static boolean validType(int t){return t>=0&&t<TYPES.length;}
    static int normalizeType(int type){return validType(type)?type:0;}
    static int normalizeRarity(int rarity){return Math.max(0,Math.min(RARITIES.length-1,rarity));}
    static boolean hasStat(int type){return validType(type)&&
            (CATEGORY[type].equals("Оружие")||CATEGORY[type].equals("Броня"));}
    static int normalizedStat(int type,int stat){return hasStat(type)?Math.max(0,Math.min(500,stat)):0;}
    static String clip(String value,int maximum){
        if(value==null)return "";
        String clean=value.trim().replace((char)0,' ');
        return clean.length()>maximum?clean.substring(0,maximum):clean;
    }
}
