package dev.infend.skyrimtext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

/** Canonical Skyrim/Dawnguard/Dragonborn paid skill teachers. No paid Creation Club NPCs. */
public final class SkillTrainers {
    public static final class Trainer {
        public final String name,location;
        public final int skill,maximum;
        private Trainer(String name,int skill,int maximum,String location){
            this.name=name;this.skill=skill;this.maximum=maximum;this.location=location;
        }
        public String skillName(){return RpgContent.SKILLS[skill];}
    }
    private static final LinkedHashMap<String,Trainer> BY_NAME=new LinkedHashMap<>();
    private SkillTrainers(){}
    private static void add(String name,int skill,int maximum,String place){
        Trainer t=new Trainer(name,skill,maximum,place);
        BY_NAME.put(NpcNameMatcher.key(name),t);
    }
    static {
        add("Лами",7,50,"Морфал");
        add("Аркадия",7,75,"Вайтран");
        add("Милор Иент",7,75,"Воронья Скала");
        add("Бабетта",7,90,"Убежище Тёмного Братства");
        add("Дравиния Каменная Ткачиха",15,75,"Роща Кин");
        add("Толфдир",15,90,"Коллегия Винтерхолда");
        add("Фендал",2,50,"Ривервуд");
        add("Эйла Охотница",2,75,"Йоррваскр");
        add("Нируин",2,90,"Буйная фляга");
        add("Сорин Журар",2,90,"Форт Стражи Рассвета");
        add("Ньяда Каменная Рука",3,75,"Йоррваскр");
        add("Ларак",3,90,"Мор Казгур");
        add("Рунил",17,50,"Фолкрит");
        add("Финис Гестор",17,75,"Коллегия Винтерхолда");
        add("Фалион",17,90,"Морфал");
        add("Талвас Фатрион",17,90,"Тель-Митрин");
        add("Вунферт Неживой",13,50,"Виндхельм");
        add("Гаран Марети",13,75,"Замок Волкихар");
        add("Сибилла Стентор",13,75,"Солитьюд");
        add("Фаральда",13,90,"Коллегия Винтерхолда");
        add("Сергий Турриан",8,75,"Коллегия Винтерхолда");
        add("Хамал",8,90,"Маркарт");
        add("Нелот",8,90,"Тель-Митрин");
        add("Гарол",4,75,"Душник-Йал");
        add("Фаркас",4,90,"Йоррваскр");
        add("Исран",4,90,"Форт Стражи Рассвета");
        add("Кувар",4,90,"Солстейм");
        add("Атуб",16,75,"Ларгашбур");
        add("Древис Нелорен",16,90,"Коллегия Винтерхолда");
        add("Хорошо-Знает-Болота",5,50,"Доки Виндхельма");
        add("Грелка",5,75,"Рифтен");
        add("Назыр",5,90,"Убежище Тёмного Братства");
        add("Ма'джхад",10,75,"Караван каджитов");
        add("Векс",10,90,"Буйная фляга");
        add("Амрен",0,50,"Вайтран");
        add("Атис",0,75,"Йоррваскр");
        add("Бургук",0,90,"Душник-Йал");
        add("Сильда Невидимая",11,75,"Виндхельм");
        add("Випир Живчик",11,90,"Буйная фляга");
        add("Хранительница Карсетта",14,50,"Зал Дозора");
        add("Афия Велоти",14,50,"Воронья Скала");
        add("Колетта Маренс",14,75,"Коллегия Винтерхолда");
        add("Даника Свет Весны",14,90,"Вайтран");
        add("Флоренций Бений",14,90,"Форт Стражи Рассвета");
        add("Горза гра-Багол",6,50,"Маркарт");
        add("Балимунд",6,75,"Рифтен");
        add("Эорлунд Серая Грива",6,90,"Небесная кузница");
        add("Гунмар",6,90,"Форт Стражи Рассвета");
        add("Кайла",9,50,"Караван каджитов");
        add("Гарви",9,75,"Маркарт");
        add("Делвин Меллори",9,90,"Буйная фляга");
        add("Дро'мараш",12,50,"Караван каджитов");
        add("Ревин Садри",12,50,"Виндхельм");
        add("Ронтил",12,75,"Замок Волкихар");
        add("Огмунд",12,75,"Маркарт");
        add("Гирауд Жиман",12,90,"Коллегия бардов");
        add("Торбьорн Расколотый Щит",1,75,"Виндхельм");
        add("Вилкас",1,90,"Йоррваскр");
        add("Фура Кровавая Пасть",1,90,"Замок Волкихар");
        add("Вульф Дикая Кровь",1,90,"Деревня скаалов");
    }
    public static Trainer find(String name){
        if(name==null)return null;
        String resolved=NpcProfiles.canonicalName(name);
        Trainer trainer=BY_NAME.get(NpcNameMatcher.key(resolved));
        return trainer==null?BY_NAME.get(NpcNameMatcher.key(name)):trainer;
    }
    public static int count(){return BY_NAME.size();}
    public static ArrayList<Trainer> all(){return new ArrayList<>(BY_NAME.values());}
    public static int remaining(GameEngine g,Trainer t){
        if(g==null||t==null||!g.started)return 0;
        int used=g.progress.trainingLevel==g.level?g.progress.trainingsThisLevel:0;
        return Math.max(0,Math.min(5-used,t.maximum-g.progress.skillLevel(t.skill)));
    }
    /** Price for one skill level, using the unmodified underlying skill level. */
    public static int unitPrice(int skillLevel){
        int next=skillLevel+1;
        return (next<=50?10:next<=75?30:50)*next+50;
    }
    /** Entire transaction is checked before the first septim or level changes. */
    public static int quote(GameEngine g,Trainer t,int count){
        if(g==null||t==null||count<1||count>remaining(g,t))return -1;
        long total=0;
        int level=g.progress.skillLevel(t.skill);
        for(int i=0;i<count;i++)total+=unitPrice(level+i);
        return total>Integer.MAX_VALUE?-1:(int)total;
    }
    public static boolean train(GameEngine g,Trainer t,int count){
        if(g==null||!g.started||g.road.pending()||g.solitude.custody()||
                "COMBAT".equals(g.mode))return false;
        int price=quote(g,t,count);
        if(price<1||g.gold<price)return false;
        int currentLevel=g.level;
        if(g.progress.trainingLevel!=currentLevel){
            g.progress.trainingLevel=currentLevel;
            g.progress.trainingsThisLevel=0;
        }
        g.gold-=price;
        g.progress.trainingsThisLevel+=count;
        g.progress.skillLevels[t.skill]+=count;
        if(g.progress.skillLevels[t.skill]>=100)g.progress.skillXp[t.skill]=0;
        else g.progress.skillXp[t.skill]=Math.min(g.progress.skillXp[t.skill],g.progress.skillNeed(t.skill)-1);
        g.addXp(count*15);
        g.progress.sync(g);
        g.recordVisualEvent("Обучение у "+t.name+": "+t.skillName()+" +"+count);
        return true;
    }
}
