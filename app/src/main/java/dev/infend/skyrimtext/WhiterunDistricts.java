package dev.infend.skyrimtext;

/** Canonical city districts. Local navigation never changes the regional atlas or quest location. */
public final class WhiterunDistricts {
    private WhiterunDistricts() {}
    public static final String ROOT = "CITY_MAP", MODE = "CITY";
    private static final String[][] DISTRICTS = {
        {"plains","Равнинный район","Нижняя терраса: ворота, ремесленники, рынок и таверны."},
        {"wind","Ветреный район","Средняя терраса: Златолист, храмы, Соратники и жилые дома."},
        {"cloud","Облачный район","Верхняя терраса: ступени и дворец ярла."},
        {"outside","За городскими стенами","Подступы к городу, конюшни, фермы и дороги. Это не четвёртый городской район."}
    };
    // Point ID, district ID, display name, original short description.
    private static final String[][] POINTS = {
        {"gate","plains","Главные ворота","Каменная арка и пост городской стражи."},
        {"barracks","plains","Казармы стражи","Помещения караула у городских укреплений."},
        {"street","plains","Главная улица","Дорога от ворот мимо мастерских к рынку."},
        {"market","plains","Рыночная площадь","Прилавки Карлотты, Анориата и Фралии Серой Гривы."},
        {"warmaidens","plains","Дева войны","Кузница Адрианны Авениччи и лавка оружейника."},
        {"huntsman","plains","Пьяный охотник","Луки, стрелы и охотничье снаряжение."},
        {"belethor","plains","Товары Белетора","Торговая лавка у площади."},
        {"arcadia","plains","Котелок Аркадии","Алхимическая лавка с полками ингредиентов и зелий."},
        {"mare","plains","Гарцующая кобыла","Таверна с очагом, комнатами и городскими слухами."},
        {"breezehome","plains","Дом тёплых ветров","Городской дом рядом с кузницей; покупка требует разрешения."},
        {"ysolda","plains","Дом Изольды","Жилище будущей торговки."},
        {"severio","plains","Дом Северия Пелагио","Городское жилище владельца фермы."},
        {"olava","plains","Дом Олавы Немощной","Жилище прорицательницы."},
        {"tree","wind","Площадь Златолиста","Священное дерево над каменными водоёмами."},
        {"kynareth","wind","Храм Кинарет","Святилище богини ветров и место служения Даники."},
        {"talos","wind","Святилище Талоса","Статуя у подъёма к дворцу, где проповедует Хеймскр."},
        {"jorrvaskr","wind","Йоррваскр","Зал Соратников с крышей в форме перевёрнутого корабля."},
        {"skyforge","wind","Небесная кузница","Древний горн на скале над Йоррваскром."},
        {"underforge","wind","Нижняя кузница","Скрытая кузница; доступ зависит от истории Соратников."},
        {"dead","wind","Зал мёртвых","Святилище Аркея и место погребения горожан."},
        {"catacombs","wind","Катакомбы Вайтрана","Подземные погребальные помещения Зала мёртвых."},
        {"graymane","wind","Дом Серых Грив","Жилище старинного нордского рода."},
        {"battleborn","wind","Дом Сынов Битвы","Жилище соперничающего с Серыми Гривами рода."},
        {"amren","wind","Дом Амрена","Жилище воина и его семьи."},
        {"uthgerd","wind","Дом Утгерд","Жилище Утгерд Несломленной."},
        {"heimskr","wind","Дом Хеймскра","Жилище проповедника Талоса."},
        {"carlotta","wind","Дом Карлотты Валентии","Жилище торговки и её дочери."},
        {"stairs","cloud","Лестница к Драконьему Пределу","Широкие ступени ведут к верхней террасе."},
        {"dragonsreach","cloud","Драконий Предел","Вход во дворец ярла Вайтрана."},
        {"throne","cloud","Тронный зал","Большой зал с очагом и троном ярла."},
        {"quarters","cloud","Покои ярла","Частные помещения дворца; свободный вход не гарантирован."},
        {"farengar","cloud","Помещения Фаренгара","Кабинет придворного мага."},
        {"porch","cloud","Большая галерея","Площадка дворца, связанная с историей драконов."},
        {"jail","cloud","Темница Драконьего Предела","Охраняемые тюремные помещения."},
        {"stables","outside","Конюшни Вайтрана","Стойла у дороги ниже городских ворот."},
        {"carriage","outside","Стоянка повозок","Место отправления извозчика."},
        {"caravan","outside","Стоянка каджитского каравана","Место у дороги; караван не всегда присутствует."},
        {"smelter","outside","Плавильня","Место для переплавки руды у городских подступов."},
        {"pelagia","outside","Ферма Пелагио","Поля к юго-западу от города."},
        {"battlefarm","outside","Ферма Сынов Битвы","Хозяйство к востоку от стен."},
        {"chillfurrow","outside","Ферма «Холодная Борозда»","Поля на восточных подступах."},
        {"meadery","outside","Медоварня Хоннинга","Медоварня возле дороги и реки."},
        {"watchtower","outside","Западная сторожевая башня","Дозорное укрепление на западной дороге; полноценный переход доступен через карту мира."}
    };
    static boolean here(GameEngine g){return g.started&&g.region==0&&"Вайтран".equals(g.progress.location);}
    public static boolean isMode(String mode){return ROOT.equals(mode)||MODE.equals(mode);}
    private static String[] find(String id,String[][] rows){for(String[] row:rows)if(row[0].equals(id))return row;return null;}
    static String normalize(String id){return find(id,DISTRICTS)!=null||find(id,POINTS)!=null?id:"gate";}
    static String district(String id){String[] point=find(id,POINTS);return point==null?normalize(id):point[1];}
    static String label(String id){String[] row=find(id,POINTS);if(row!=null)return row[2];row=find(id,DISTRICTS);return row==null?"Главные ворота":row[1];}
    static String description(String id){String[] point=find(id,POINTS);if(point!=null)return point[3];String[] row=find(id,DISTRICTS);return row==null?POINTS[0][3]:row[2];}
    static String location(GameEngine g){if(!here(g))return g.progress.location;String id=normalize(g.whiterunSection);String d=district(id);return "Вайтран | "+label(d)+(id.equals(d)?"":" | "+label(id));}
    static GameEngine.Scene scene(GameEngine g){
        String id=normalize(g.whiterunSection),d=district(id);
        if(ROOT.equals(g.mode)){
            GameEngine.Scene s=new GameEngine.Scene("ВАЙТРАН | РАЙОНЫ","Город на трёх террасах","Ты находишься: "+location(g)+". Выбери район. Подступы к городу выделены отдельно.");
            for(String[] row:DISTRICTS)s.add("CITY:"+row[0],row[1],row[2]+(row[0].equals(d)?" | текущий район":""));
            return s.add("GO_WORLD","Вернуться к истории","Остаться на текущем месте");
        }
        GameEngine.Scene s=new GameEngine.Scene("ВАЙТРАН | "+label(d),label(id),description(id)+"\n\nТекущее место: "+location(g)+". Перемещение между секциями показывает местность и подходы к зданиям; оно не открывает запертые двери и не завершает задания.");
        for(String[] row:POINTS)if(row[1].equals(d)&&!row[0].equals(id))s.add("CITY:"+row[0],row[2],row[3]);
        return s.add("CITY_OPEN","Другой район","Открыть список районов").add("GO_WORLD","Продолжить историю здесь","Вернуться к игровым действиям");
    }
    static void choose(GameEngine g,String action){
        if(!here(g)||g.road.pending()||"COMBAT".equals(g.mode))return;
        if("CITY_OPEN".equals(action)){
            if("WORLD".equals(g.mode)||isMode(g.mode))g.mode=ROOT;
            return;
        }
        if(!isMode(g.mode)||!action.startsWith("CITY:"))return;
        String id=action.substring(5);
        String[] row=find(id,DISTRICTS),point=find(id,POINTS);
        if(row==null&&point==null)return;
        if(point!=null&&!district(normalize(g.whiterunSection)).equals(point[1]))return;
        if(row!=null&&!ROOT.equals(g.mode))return;
        if(!id.equals(g.whiterunSection)){g.whiterunSection=id;g.aiTurn=null;}
        g.message="Ты направляешься к месту: "+label(id)+". "+description(id);
        g.mode=MODE;
    }
}
