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
        Item(String n,String cat,String ic,double kg,int septims,String desc,String eff){
            id=n;name=n;category=cat;icon=ic;weight=kg;value=septims;description=desc;effect=eff;
        }
    }
    private static final LinkedHashMap<String,Item> items=new LinkedHashMap<>();
    private static boolean loaded=false;
    private static void put(String name,String category,String icon,double weight,int cost,String desc,String effect){
        items.put(name,new Item(name,category,icon,weight,cost,desc,effect));
    }
    static {
        put("Септимы","Валюта","◈",0,1,"Монеты имперской чеканки. Не занимают переносимый вес.","Расчётная валюта.");
        put("Походный клинок","Оружие","⚔",8,25,"Простое дорожное оружие с потемневшей гардой.","Ближний бой.");
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
        put("Кожаные полоски","Материалы","▤",0.1,3,"Узкие кожаные ленты.","Ремонт и изготовление вещей.");
        String[] metals={"Железный","Стальной","Орочий","Двемерский","Эльфийский","Стеклянный","Эбонитовый","Даэдрический","Драконий"};
        String[] feminine={"Железная","Стальная","Орочья","Двемерская","Эльфийская","Стеклянная","Эбонитовая","Даэдрическая","Драконья"};
        String[] plural={"Железные","Стальные","Орочьи","Двемерские","Эльфийские","Стеклянные","Эбонитовые","Даэдрические","Драконьи"};
        int[] prices={25,45,95,135,185,280,410,800,1000};
        for(int i=0;i<metals.length;i++){
            String metal=metals[i];int value=prices[i];
            put(metal+" меч","Оружие","⚔",8+i*1.1,value,
                "Одноручный меч, выкованный из материала «"+metal.toLowerCase(Locale.ROOT)+"».","Одноручное оружие.");
            put(metal+" боевой топор","Оружие","⚒",10+i*1.3,value+15,
                "Тяжёлое рубящее оружие для ближнего боя.","Одноручное оружие.");
            put(metal+" кинжал","Оружие","⚔",2.5+i*.45,value/2,
                "Короткий клинок для скрытного и быстрого удара.","Одноручное оружие.");
            put(metal+" двуручный меч","Оружие","⚔",16+i*1.7,value*2,
                "Большой клинок с тяжёлой рукоятью.","Двуручное оружие.");
            put(metal+" боевой молот","Оружие","⚒",18+i*1.8,value*2,
                "Оружие, способное пробить крепкую броню.","Двуручное оружие.");
            put(metal+" лук","Оружие","➶",7+i*.5,value,
                "Дальний бой и прицельная стрельба.","Стрельба.");
            put(metal+" шлем","Броня","◭",3+i*.8,value,
                "Защита головы; ковка материала "+metal.toLowerCase(Locale.ROOT)+".","Элемент доспеха.");
            put(feminine[i]+" кираса","Броня","⬟",17+i*2,value*3,
                "Нагрудная часть защитного комплекта.","Защита туловища.");
            put(plural[i]+" перчатки","Броня","✥",3+i*.5,value,
                "Защитные перчатки для боя.","Защита рук.");
            put(plural[i]+" сапоги","Броня","♟",4+i*.6,value,
                "Кованая обувь для сложных переходов.","Защита ног.");
            put(metal+" щит","Броня","⬡",10+i,value*2,
                "Круглый защитный щит для блокирования.","Блокирование.");
        }
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
        put("Клинок «Кай»","Пользовательское оружие","⚔",6.0,1000,"Редкий клинок из персонального архива приключений.","Уникальное снаряжение.");
        put("Арбалет «Рик»","Пользовательское оружие","➶",8.0,1200,"Арбалет из пользовательской коллекции предметов.","Стрельба.");
        put("Эликсир Жизни","Пользовательские предметы","✧",0.5,0,"Редкое средство из пользовательской библиотеки.","Мощное исцеление, подробности в Кодексе.");
    }
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
                put(name,o.optString("category","Еда"),"♨",
                    Math.max(0,o.optDouble("weight",0.5)),Math.max(0,o.optInt("cost",0)),
                    o.optString("description",""),o.optString("effect",""));
            }
        }catch(Exception ignored){}
    }
    public static ArrayList<Item> all(){return new ArrayList<>(items.values());}
    public static Item find(String id){return items.get(id);}
    public static int count(){return items.size();}
    public static ArrayList<Item> sorted(String category,String by){
        ArrayList<Item> list=new ArrayList<>();
        for(Item x:items.values())if(category==null||"Все".equals(category)||x.category.equals(category))list.add(x);
        if("Вес".equals(by))list.sort((a,b)->Double.compare(a.weight,b.weight));
        else if("Цена".equals(by))list.sort((a,b)->Integer.compare(b.value,a.value));
        else list.sort((a,b)->a.name.compareToIgnoreCase(b.name));
        return list;
    }
    public static ArrayList<String> groups(){
        LinkedHashSet<String> seen=new LinkedHashSet<>();seen.add("Все");
        for(Item i:items.values())seen.add(i.category);
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
