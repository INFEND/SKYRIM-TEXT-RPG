package dev.infend.skyrimtext;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ItemCatalog {
    public static final class Item {
        public final String id, name, category, icon, description, effect;
        public final double weight;
        public final int value;
        // -1 means not verified / not applicable, not a fabricated zero.
        public final double baseStat;
        public final int statSkill;
        Item(String n,String cat,String ic,double kg,int septims,String desc,String eff,double stat,int skill){
            id=n;name=n;category=cat;icon=ic;weight=kg;value=septims;description=desc;effect=eff;
            baseStat=stat;statSkill=skill;
        }
        public boolean weapon(){return "Оружие".equals(category)||"Боеприпасы".equals(category)||statSkill>=0&&statSkill<=2;}
        public boolean wearable(){return "Броня".equals(category)||"Одежда".equals(category)||"Украшения".equals(category)||statSkill>=4;}
        public double currentStat(RpgProgress progress){
            if(baseStat<0||statSkill<0||progress==null)return baseStat;
            double rank=(slot().equals("shield")?0:progress.combatPerkRank(statSkill))*0.2;
            return (baseStat+progress.temperBonus(this))*(1+progress.skillLevel(statSkill)*(weapon()?0.005:0.004))*(1+rank);
        }
        // Only known, non-enchanted standard equipment can be tempered.
        public String temperMaterial(){
            if(baseStat<=0||statSkill<0||"Артефакты".equals(category)||"Боеприпасы".equals(category))return "";
            String n=name.toLowerCase(Locale.ROOT).replace('ё','е');
            if(n.contains("чешуйчат"))return "Драконья чешуя";
            if(n.startsWith("дракон"))return "Драконья кость";
            if(n.startsWith("даэдр")||n.startsWith("эбонит"))return "Эбонитовый слиток";
            if(n.startsWith("двемер"))return "Двемерский металлический слиток";
            if(n.startsWith("ороч"))return "Орихалковый слиток";
            if(n.startsWith("эльфий"))return "Очищенный лунный камень";
            if(n.startsWith("стеклян"))return "Очищенный малахит";
            if(n.startsWith("сталгрим"))return "Сталгрим";
            if(n.startsWith("нордск"))return "Ртутный слиток";
            if(n.contains("пластинчат"))return "Корундовый слиток";
            if(n.startsWith("костян"))return "Костная мука";
            if(n.contains("хитинов"))return "Хитиновая пластина";
            if(n.startsWith("кожан")||n.contains("из шкур"))return "Кожа";
            if(n.startsWith("стальн"))return "Стальной слиток";
            if(n.startsWith("железн")||n.equals("походный клинок"))return "Железный слиток";
            if(n.equals("арбалет")||n.equals("улучшенный арбалет"))return "Стальной слиток";
            if(n.equals("улучшенный двемерский арбалет"))return "Двемерский металлический слиток";
            if(n.equals("длинный лук")||n.equals("охотничий лук"))return "Кожа";
            return "";
        }
        public String slot(){
            if(weapon())return "Боеприпасы".equals(category)?"ammo":"weapon";
            if(!wearable())return "";
            String n=name.toLowerCase(Locale.ROOT).replace('ё','е');
            if(n.contains("щит"))return "shield";
            if(n.contains("кольц"))return "ring";
            if(n.contains("ожерел")||n.contains("амулет")||n.contains("медальон"))return "neck";
            if(n.contains("шлем")||n.contains("маск")||n.contains("диадем")||n.contains("корона")||n.contains("венец")||n.contains("головной убор")||n.contains("колпак")||n.contains("шапка")||n.contains("шляп")||n.startsWith("капюшон")||n.contains("капюшон вампира"))return "head";
            if(n.contains("перчат")||n.contains("наруч")||n.contains("рукавиц"))return "hands";
            if(n.contains("сапог")||n.contains("ботин")||n.contains("обув")||n.contains("сандал"))return "feet";
            return "body";
        }
        public boolean twoHanded(){return weapon()&&(statSkill==1||statSkill==2)&&!"Боеприпасы".equals(category);}
        public String statLabel(RpgProgress progress){
            if(!weapon()&&!wearable())return "";
            String label=weapon()?"УРОН":"БРОНЯ";
            if(baseStat<0)return label+": не определён";
            String base=formatStat(baseStat),current=formatStat(currentStat(progress));
            return label+": "+current+(statSkill>=0?" | базовый: "+base:"");
        }
        private static String formatStat(double n){
            return String.format(Locale.forLanguageTag("ru"),n==Math.rint(n)?"%.0f":"%.1f",n);
        }
    }
    private static final LinkedHashMap<String,Item> items=new LinkedHashMap<>();
    private static boolean loaded=false;
    private static void put(String name,String category,String icon,double weight,int cost,String desc,String effect){
        double stat=("Одежда".equals(category)||"Украшения".equals(category))?0:-1;
        put(name,category,icon,weight,cost,desc,effect,stat,-1);
    }
    private static void put(String name,String category,String icon,double weight,int cost,String desc,String effect,double stat,int skill){
        items.put(name,new Item(name,category,icon,weight,cost,desc,effect,stat,skill));
    }
    private static void weapon(String name,double[] row,int skill){
        put(name,"Оружие","⚔",row[1],(int)row[2],"Оружие Скайрима.",
            skill==0?"Одноручное оружие.":skill==1?"Двуручное оружие.":"Стрельба.",row[0],skill);
    }
    private static void armor(String name,double[] row,int skill,String effect){
        put(name,"Броня","⬟",row[1],(int)row[2],"Снаряжение Скайрима.",effect,row[0],skill);
    }
    static {
        put("Септимы","","◈",0,1,"Монеты имперской чеканки. Не занимают переносимый вес.","Средство расчёта.");
        put("Походный клинок","Оружие","⚔",8,25,"Простое дорожное оружие с потемневшей гардой.","Ближний бой.",7,0);
        put("Дорожная одежда","Одежда","♟",1.5,10,"Обычная одежда странника. Защищает от пыли, но не от стали.","Базовое снаряжение.");
        put("Лечебное зелье","Зелья","◉",0.5,35,"Красноватая жидкость из алхимической лавки.","Восстанавливает здоровье.");
        put("Зелье магии","Зелья","◉",0.5,30,"Тягучий синий состав для мага.","Восстанавливает магию.");
        put("Зелье запаса сил","Зелья","◉",0.5,30,"Алхимический раствор из северных трав.","Восстанавливает выносливость.");
        put("Отмычка","Инструменты","⚒",0.02,2,"Тонкий стержень из пружинной стали.","Для взлома замков.");
        put("Факел","Инструменты","♧",0.5,3,"Смолистый факел, освещающий пещеры.","Источник света.");
        put("Железный слиток","Материалы","▣",1,7,"Железо для кузнечных работ.","Ковка.");
        put("Стальной слиток","Материалы","▣",1,20,"Закалённая кузнечная заготовка.","Ковка.");
        put("Серебряный слиток","Материалы","▣",1,50,"Мягкий серебристый металл.","Ювелирное дело.");
        put("Золотой слиток","Материалы","▣",1,100,"Тяжёлый жёлтый металл высокой стоимости.","Ювелирное дело.");
        put("Эбонитовый слиток","Материалы","▣",1,150,"Редкий чёрный металл высокой прочности.","Ковка мощного снаряжения.");
        put("Ртутный слиток","Материалы","▣",1,60,"Необычный обработанный металл.","Ковка.");
        put("Очищенный малахит","Материалы","▣",1,100,"Зелёный камень для дорогих доспехов.","Ковка.");
        put("Очищенный лунный камень","Материалы","▣",1,75,"Светящийся металл эльфийских мастеров.","Ковка.");
        put("Двемерский металлический слиток","Материалы","▣",1,30,"Сплав древних подземных строителей.","Ковка.");
        put("Драконья кость","Материалы","⬟",15,500,"Останки древнего крылатого существа.","Кузнечное дело.");
        put("Драконья чешуя","Материалы","⬟",10,250,"Твёрдая защитная пластина.","Кузнечное дело.");
        put("Орихалковый слиток","Материалы","▣",1,45,"Металл орочьих кузнецов.","Ковка.");
        put("Корундовый слиток","Материалы","▣",1,40,"Сплав для изготовления доспехов.","Ковка.");
        put("Кожа","Материалы","▤",2,10,"Выделанная кожа.","Ковка.");
        put("Сталгрим","Материалы","▣",1,200,"Древний зачарованный лёд Солстейма.","Ковка.");
        put("Хитиновая пластина","Материалы","▤",1,15,"Пластина панциря.","Ковка.");
        put("Костная мука","Ингредиенты","♣",0.5,5,"Измельчённая кость.","Алхимия и костяные доспехи.");
        put("Кожаные полоски","Материалы","▤",0.1,3,"Узкие кожаные ленты.","Ремонт и изготовление вещей.");
        // Base-game/Dawnguard/Dragonborn data: damage or armor, weight, value.
        // Keep existing Russian IDs for save compatibility.
        String[] metals={"Железный","Стальной","Орочий","Двемерский","Эльфийский","Стеклянный","Эбонитовый","Даэдрический","Драконий","Нордский","Сталгримовый"};
        String[] feminine={"Железная","Стальная","Орочья","Двемерская","Эльфийская","Стеклянная","Эбонитовая","Даэдрическая","Драконья","Нордская резная","Сталгримовая"};
        String[] plural={"Железные","Стальные","Орочьи","Двемерские","Эльфийские","Стеклянные","Эбонитовые","Даэдрические","Драконьи","Нордские резные","Сталгримовые"};
        // sword, war axe, dagger, mace, greatsword, battleaxe, warhammer, bow.
        double[][][] weapons={
            {{7,9,25},{8,11,30},{4,2,10},{9,13,35},{15,16,50},{16,20,55},{18,24,60},{6,5,30}},
            {{8,10,45},{9,12,55},{5,2.5,18},{10,14,65},{17,17,90},{18,21,100},{20,25,110},{7,7,50}},
            {{9,11,75},{10,13,90},{6,3,30},{11,15,105},{18,18,75},{19,25,165},{21,26,180},{10,9,150}},
            {{10,12,135},{11,14,165},{7,3.5,55},{12,16,190},{19,19,270},{20,23,300},{22,27,325},{12,10,270}},
            {{11,13,235},{12,15,280},{8,4,95},{13,17,330},{20,20,470},{21,24,520},{23,28,565},{13,12,470}},
            {{12,14,410},{13,16,490},{9,4.5,165},{14,18,575},{21,22,820},{22,25,900},{24,29,985},{15,14,820}},
            {{13,15,720},{15,17,865},{10,5,290},{16,19,1000},{22,22,1440},{23,26,1585},{25,30,1725},{17,16,1440}},
            {{14,16,1250},{15,18,1500},{11,6,500},{16,20,1750},{24,23,2500},{25,27,2750},{27,31,4000},{19,18,2500}},
            {{15,19,1500},{16,21,1700},{12,6.5,600},{17,22,2000},{25,27,2725},{26,30,3000},{28,33,4275},{20,20,2725}},
            {{11,12,290},{12,14,350},{8,3.5,115},{13,16,410},{20,19,585},{21,23,650},{23,27,700},{13,11,580}},
            {{13,14,985},{15,16,1180},{10,4.5,395},{16,18,1375},{23,21,1970},{24,25,2150},{26,29,2850},{17,15,1800}}
        };
        String[] kinds={"меч","боевой топор","кинжал","булава","двуручный меч","секира","боевой молот","лук"};
        for(int i=0;i<metals.length;i++)for(int k=0;k<kinds.length;k++){
            if(k==7&&i<2)continue;
            String name=metals[i]+" "+kinds[k];
            // Russian feminine inflection for newly introduced maces and battleaxes.
            if(k==3||k==5)name=feminine[i].replace(" резная","")+" "+kinds[k];
            weapon(name,weapons[i][k],k<4?0:k<7?1:2);
        }
        weapon("Длинный лук",weapons[0][7],2);
        weapon("Охотничий лук",weapons[1][7],2);
        // Legacy IDs retained for saved games, not additional vanilla bow forms.
        weapon("Железный лук",weapons[0][7],2);weapon("Стальной лук",weapons[1][7],2);
        weapon("Арбалет",new double[]{19,14,120},2);
        weapon("Улучшенный арбалет",new double[]{19,15,200},2);
        weapon("Двемерский арбалет",new double[]{22,20,350},2);
        weapon("Улучшенный двемерский арбалет",new double[]{22,21,550},2);
        // helmet, cuirass, gauntlets, boots, shield
        double[][][] armors={
            {{15,5,60},{25,30,125},{10,5,25},{10,6,25},{20,12,60}},
            {{17,5,125},{31,35,275},{12,4,55},{12,8,55},{24,12,150}},
            {{20,8,500},{40,35,1000},{15,7,200},{15,7,200},{30,14,500}},
            {{18,12,200},{34,45,400},{13,8,85},{13,10,85},{26,12,225}},
            {{13,1,110},{29,4,225},{8,1,45},{8,1,45},{21,4,115}},
            {{16,2,450},{38,7,900},{11,2,190},{11,2,190},{27,6,450}},
            {{21,10,750},{43,38,1500},{16,7,275},{16,7,275},{32,14,750}},
            {{23,15,1600},{49,50,3200},{18,6,625},{18,10,625},{36,15,1600}},
            {{22,8,1050},{46,40,2125},{17,8,425},{17,8,425},{34,15,1050}},
            {{20,7,550},{43,37,1600},{15,6,220},{15,6,220},{26.5,10,335}},
            {{22,7,1135},{46,38,2200},{17,7,450},{17,7,450}}
        };
        for(int i=0;i<armors.length;i++){
            int skill=i==4||i==5?5:4;double[][] a=armors[i];
            String prefix=i==9?"Нордский резной":metals[i];
            armor(prefix+" шлем",a[0],skill,"Защита головы.");
            // Existing save IDs kept exactly, including Стальная кираса.
            put(feminine[i]+" кираса","Броня","⬟",a[1][1],(int)a[1][2],"Нагрудная часть защитного комплекта.","Защита туловища.",a[1][0],skill);
            put(plural[i]+" перчатки","Броня","✥",a[2][1],(int)a[2][2],"Защитные перчатки.","Защита рук.",a[2][0],skill);
            armor(plural[i]+" сапоги",a[3],skill,"Защита ног.");
            if(a.length>4)armor(metals[i]+" щит",a[4],skill,"Блокирование.");
        }
        armor("Кожаная кираса",new double[]{26,6,125},5,"Защита туловища.");
        armor("Кожаные сапоги",new double[]{7,2,25},5,"Защита ног.");
        armor("Кожаные наручи",new double[]{7,2,25},5,"Защита рук.");
        armor("Кожаный шлем",new double[]{12,2,60},5,"Защита головы.");
        armor("Кираса из шкур",new double[]{20,5,50},5,"Защита туловища.");
        armor("Сапоги из шкур",new double[]{5,1,10},5,"Защита ног.");
        armor("Наручи из шкур",new double[]{5,1,10},5,"Защита рук.");
        armor("Шлем из шкур",new double[]{10,2,25},5,"Защита головы.");
        armor("Щит из шкур",new double[]{15,4,25},5,"Блокирование.");
        armor("Драконья чешуйчатая кираса",new double[]{41,10,1500},5,"Защита туловища.");
        armor("Драконьи чешуйчатые сапоги",new double[]{12,3,300},5,"Защита ног.");
        armor("Драконьи чешуйчатые перчатки",new double[]{12,3,300},5,"Защита рук.");
        armor("Драконий чешуйчатый шлем",new double[]{17,4,750},5,"Защита головы.");
        armor("Драконий чешуйчатый щит",new double[]{29,6,750},5,"Блокирование.");
        armor("Стальной пластинчатый шлем",new double[]{19,6,300},4,"Защита головы.");
        armor("Стальная пластинчатая кираса",new double[]{40,38,625},4,"Защита туловища.");
        armor("Стальные пластинчатые перчатки",new double[]{14,6,125},4,"Защита рук.");
        armor("Стальные пластинчатые сапоги",new double[]{14,9,125},4,"Защита ног.");
        armor("Костяная кираса",new double[]{32,34,290},4,"Защита туловища.");
        armor("Костяные сапоги",new double[]{12,7,60},4,"Защита ног.");
        armor("Костяные перчатки",new double[]{12,3.5,60},4,"Защита рук.");
        armor("Костяной шлем",new double[]{17,4.5,135},4,"Защита головы.");
        armor("Костяной щит",new double[]{21.5,8,95},4,"Блокирование.");
        armor("Тяжёлая хитиновая кираса",new double[]{40,35,650},4,"Защита туловища.");
        armor("Тяжёлые хитиновые сапоги",new double[]{14,6,135},4,"Защита ног.");
        armor("Тяжёлые хитиновые перчатки",new double[]{14,5,135},4,"Защита рук.");
        armor("Тяжёлый хитиновый шлем",new double[]{19,5,135},4,"Защита головы.");
        String[] arrows={"Железная","Стальная","Орочья","Двемерская","Эльфийская","Стеклянная","Эбонитовая","Даэдрическая","Драконья","Нордская","Сталгримовая"};
        int[][] ammo={{8,1},{10,2},{12,3},{14,4},{16,5},{18,6},{20,7},{24,8},{25,9},{12,3},{20,7}};
        for(int i=0;i<arrows.length;i++)put(arrows[i]+" стрела","Боеприпасы","➶",0,ammo[i][1],"Стрела для лука.","Боеприпас.",ammo[i][0],-1);
        put("Стальной болт","Боеприпасы","➶",0,1,"Болт для арбалета.","Боеприпас.",10,-1);
        put("Двемерский болт","Боеприпасы","➶",0,3,"Болт для арбалета.","Боеприпас.",14,-1);
        String[][] attire={
            {"Капюшон мага","0.5","12"},{"Одеяние ученика","1","30"},{"Одеяние архимага","1","3265"},
            {"Шляпа путника","0.5","5"},{"Сапоги странника","1","8"},{"Кожаные перчатки","0.5","6"},
            {"Кольцо серебряное","0.3","70"},{"Кольцо золотое","0.3","120"},
            {"Золотое ожерелье","0.5","210"},{"Серебряное ожерелье","0.5","125"},
            {"Диадема из камня","2","300"},{"Плащ паломника","1.5","15"}
        };
        for(String[] a:attire)put(a[0],a[0].contains("Кольцо")||a[0].contains("ожерелье")?"Украшения":"Одежда",
            "✧",Double.parseDouble(a[1]),Integer.parseInt(a[2]),"Личный предмет снаряжения путешественника.","Носимый предмет.");
        String[] gems={"Гранат","Аметист","Рубин","Сапфир","Изумруд","Алмаз","Безупречный гранат","Безупречный аметист","Безупречный рубин","Безупречный сапфир","Безупречный изумруд","Безупречный алмаз"};
        for(int i=0;i<gems.length;i++)put(gems[i],"Драгоценности","◆",0.1,70+i*65,"Огранённый драгоценный камень. Используется в украшениях.","Торговля, кузнечное дело.");
        String[] ingredients={"Пшеница","Соль","Лунный сахар","Голубой горноцвет","Красный горноцвет","Пурпурный горноцвет",
            "Лаванда","Чеснок","Снежные ягоды","Крыло голубой бабочки","Клык саблезуба",
            "Грибы имп","Корень Нирна","Жир тролля","Вересковое сердце","Глаз саблезуба",
            "Кровавый венец","Чешуйчатка","Пушица","Палец великана"};
        for(String n:ingredients)put(n,"Ингредиенты","♣",n.contains("Палец")?1.0:0.1,14,
            "Алхимический ингредиент. Свойства открываются при опытах и дегустации.","Алхимия.");
        String[] artifacts={"Звезда Азуры","Звезда Чёрной Азуры","Ваббаджек","Меч Мерунеса","Сияние Рассвета",
            "Эбонитовый клинок","Молот Волендранг","Булава Молаг Бала","Клинок Горя","Охладитель",
            "Маска Мирака","Древний свиток","Амулет Голдура","Соловьиный клинок",
            "Щит Ауриэля","Лук Ауриэля","Кольцо Хирсина","Бритва Мерунеса"};
        for(String n:artifacts)put(n,"Артефакты","✦",n.contains("Молот")?25:n.contains("свиток")?1:8,
            1800,"Редкий артефакт, связанный с легендами и опасными историями северных земель.","Уникальный предмет.");
        // Fixed-stat artifacts. Enchantment descriptions are not yet executable effects.
        put("Сияние Рассвета","Артефакты","⚔",10,740,"Меч Меридии.","Огонь и изгнание нежити (описание оригинала).",12,0);
        put("Бритва Мерунеса","Артефакты","⚔",3,860,"Кинжал Мерунеса Дагона.","Шанс мгновенного убийства (описание оригинала).",11,0);
        put("Эбонитовый клинок","Артефакты","⚔",10,2000,"Двуручный клинок Мефалы.","Поглощение здоровья (описание оригинала).",11,1);
        put("Молот Волендранг","Артефакты","⚒",26,1843,"Молот Малаката.","Поглощение запаса сил (описание оригинала).",25,1);
        put("Булава Молаг Бала","Артефакты","⚔",18,1257,"Булава даэдрического князя.","Урон магии и запасу сил, захват душ (описание оригинала).",16,0);
        put("Клинок Горя","Артефакты","⚔",7,880,"Кинжал Тёмного Братства.","Поглощение здоровья (описание оригинала).",12,0);
        put("Лук Ауриэля","Артефакты","➶",11,1000,"Древний лук Ауриэля.","Солнечный урон (описание оригинала).",13,2);
        put("Вутрад","Артефакты","⚒",25,2000,"Секира Исграмора.","Дополнительный урон эльфам (описание оригинала).",25,1);
        put("Разрушитель Заклинаний","Артефакты","⬡",12,277,"Щит Периайта.","Магический оберег (описание оригинала).",38,4);
        put("Шкура Спасителя","Артефакты","⬟",6,2679,"Доспех Хирсина.","Сопротивление магии и ядам (описание оригинала).",26,5);
        put("Клинок «Кай»","Оружие","⚔",6.0,1000,"Редкий клинок из персонального архива приключений.","Одноручное оружие.");
        put("Арбалет «Рик»","Оружие","➶",8.0,1200,"Редкий арбалет из персонального архива.","Стрельба.");
        put("Эликсир Жизни","Зелья","✧",0.5,0,"Редкое средство из личной библиотеки.","Мощное исцеление, подробности в Кодексе.");
    }
    // Supplement uses existing name as save key, so replacements preserve quantities.
    static void registerSupplement(String name,String category,String icon,double weight,int cost,String desc,String effect,double stat,int skill){
        put(name,category,icon,weight,cost,desc,effect,stat,skill);
    }
    static void registerMissing(String name,String category,String icon,double weight,int cost,String desc,String effect,double stat,int skill){
        if(!items.containsKey(name))put(name,category,icon,weight,cost,desc,effect,stat,skill);
    }
    static { ApparelAndPotionCatalog.register(); WorldItemCatalog.register(); LibraryItemCatalog.register(); CommonItemCatalog.register(); }
    private ItemCatalog(){}
    public static synchronized void initialize(Context c){
        if(loaded)return;
        loaded=true;
        byte[] raw=asset(c,"catalog/user_foods.json");
        if(raw==null)return;
        try{
            JSONArray list=new JSONArray(new String(raw,StandardCharsets.UTF_8));
            for(int i=0;i<list.length();i++){
                JSONObject o=list.getJSONObject(i);
                String name=o.optString("name","");
                if(name.length()<2)continue;
                if(items.containsKey(name))continue; // Do not overwrite verified built-in stats.
                put(name,o.optString("category","Еда"),"♨",
                    Math.max(0,o.optDouble("weight",0.5)),Math.max(0,o.optInt("cost",0)),
                    o.optString("description",""),o.optString("effect",""));
            }
        }catch(Exception ignored){}
    }
    public static ArrayList<Item> all(){
        ArrayList<Item> list=new ArrayList<>();
        for(Item item:items.values())if(!item.category.isEmpty())list.add(item);
        return list;
    }
    public static Item find(String id){return items.get(id);}
    public static int count(){return items.size();}
    public static int countCategory(String category){return sorted(category,"Название").size();}
    public static ArrayList<Item> sorted(String category,String by){
        ArrayList<Item> list=new ArrayList<>();
        for(Item x:all())if(category==null||"Все".equals(category)||x.category.equals(category))list.add(x);
        if("Вес".equals(by))list.sort((a,b)->Double.compare(a.weight,b.weight));
        else if("Цена".equals(by))list.sort((a,b)->Integer.compare(b.value,a.value));
        else list.sort((a,b)->a.name.compareToIgnoreCase(b.name));
        return list;
    }
    public static ArrayList<String> groups(){
        LinkedHashSet<String> seen=new LinkedHashSet<>();seen.add("Все");
        for(Item i:all())seen.add(i.category);
        return new ArrayList<>(seen);
    }
    public static byte[] asset(Context context,String requested){
        try(InputStream input=context.getAssets().open("lore_bundle.zip");
            ZipInputStream zip=new ZipInputStream(input)){
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null){
                if(entry.getName().equals(requested)){
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                    byte[] block=new byte[8192];int n;
                    while((n=zip.read(block))>0){
                        if(bytes.size()+n>750000)break;
                        bytes.write(block,0,n);
                    }
                    return bytes.toByteArray();
                }
                zip.closeEntry();
            }
        }catch(Exception ignored){}
        return null;
    }
    public static ArrayList<String> loreDocuments(Context c){
        ArrayList<String> names=new ArrayList<>();
        try(InputStream in=c.getAssets().open("lore_bundle.zip");
            ZipInputStream z=new ZipInputStream(in)){
            ZipEntry e;
            while((e=z.getNextEntry())!=null){
                if(e.getName().startsWith("lore/")&&e.getName().endsWith(".txt"))
                    names.add(e.getName().substring(5));
                z.closeEntry();
            }
        }catch(Exception ignored){}
        Collections.sort(names,String.CASE_INSENSITIVE_ORDER);
        return names;
    }
    public static String document(Context c,String name){
        byte[] b=asset(c,"lore/"+name);
        return b==null?"Документ не найден":new String(b,StandardCharsets.UTF_8);
    }
    public static Bitmap picture(Context c,String fileName){
        byte[] b=asset(c,"images/"+fileName);
        return b==null?null:BitmapFactory.decodeByteArray(b,0,b.length);
    }
}
