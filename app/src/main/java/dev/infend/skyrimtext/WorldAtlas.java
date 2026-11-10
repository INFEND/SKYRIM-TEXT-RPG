package dev.infend.skyrimtext;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public final class WorldAtlas {
    public static final class Place{
        public final String name,type,description;
        public final int region,x,y;
        Place(String n,String t,int r,int px,int py,String desc){
            name=n;type=t;region=r;x=px;y=py;description=desc;
        }
    }
    private static final ArrayList<Place> places=new ArrayList<>();
    private static void put(String n,String t,int region,int x,int y,String d){places.add(new Place(n,t,region,x,y,d));}
    static{
        put("Хелген","Поселение",0,560,1010,"Крепость на южном перевале. Здесь первая глава судьбы часто начинается с дыма и тревожных криков.");
        put("Вайтран","Столица",0,565,635,"Город над золотыми равнинами, в тени Драконьего Предела.");
        put("Ривервуд","Деревня",0,548,850,"Деревня лесопилки у стремительной реки, между Хелгеном и долиной.");
        put("Рорикстед","Деревня",0,425,690,"Небольшие поля на дороге у западных холмов.");
        put("Айварстед","Деревня",0,704,812,"Деревня у основания Глотки Мира и паломнической тропы.");
        put("Камень Хранитель","Камень",0,535,907,"Три древних камня над рекой, привлекающие искателей своего пути.");
        put("Ветреный пик","Руины",0,594,875,"Над долиной нависает древний нордский курган.");
        put("Западная сторожевая башня","Форт",0,510,641,"Дозорный пост на подступах к городу.");
        put("Лагерь Тихих Лун","Руины",0,554,536,"Забытые лунные камни и следы бандитского костра.");
        put("Высокий Хротгар","Святилище",0,688,743,"Седобородые хранят покой на снежной вершине.");
        put("Глотка Мира","Гора",0,683,708,"Высочайший пик, закрытый вечным снегом.");
        put("Форт Греймур","Форт",0,470,592,"Каменные укрепления к западу от Вайтрана.");
        put("Северный проход","Переход",0,637,545,"Каменистая тропа к дальним горным дорогам.");
        put("Рифтен","Столица",1,833,938,"Город на берегу озера, где часто торгуют не только товарами.");
        put("Шорский Камень","Деревня",1,830,752,"Шахтёрское селение среди каменистых полей.");
        put("Камень Шора","Руины",1,814,865,"Древние захоронения и нордские сказания.");
        put("Чёрный Брод","Поселение",1,867,820,"Небольшая долина у горных дорог.");
        put("Золотое поместье","Усадьба",1,795,973,"Земли у рифтенских дорог, богатые торговым урожаем.");
        put("Медоварня Хоннинга","Усадьба",1,818,978,"Торговая точка у южных трактов.");
        put("Форт Стражи Рассвета","Форт",1,949,852,"Скрытая среди скал крепость охотников на вампиров.");
        put("Пещера Чёрный Предел","Подземелье",1,758,850,"Мрачные подземные переходы под древними горами.");
        put("Глотка Мрака","Пещера",1,898,783,"Тёмный грот с подземными ручьями.");
        put("Виндхельм","Столица",2,803,496,"Каменная столица Истмарка над холодной рекой.");
        put("Кинсгроув","Деревня",2,827,635,"Тихая деревня на дороге к восточным землям.");
        put("Чёрная Гавань","Поселение",2,843,544,"Каменные склады и складские дворы над рекой.");
        put("Саретийская ферма","Ферма",2,752,571,"Уединённые поля, огороженные снежным лесом.");
        put("Форт Амол","Форт",2,737,686,"Военный форт среди лесных перевалов.");
        put("Святилище Боэтии","Святилище",2,894,505,"Место опасных клятв и слухов об испытании.");
        put("Святилище Азуры","Святилище",2,905,355,"Статуя богини обращена лицом к морозным далям.");
        put("Солитьюд","Столица",3,239,303,"Крупнейший северный порт под высокой каменной аркой.");
        put("Драконий Мост","Деревня",3,314,427,"Древняя переправа по дороге к столице.");
        put("Волчий Череп","Пещера",3,308,355,"Зловещая горная гряда со старыми костями.");
        put("Ферма Катлы","Ферма",3,267,365,"Поля и стойла на дальнем подъезде к Солитьюду.");
        put("Сломанное Весло","Грот",3,169,317,"Гавань, где не каждый корабль возвращается с грузом.");
        put("Маяк Солитьюда","Маяк",3,216,261,"Огни над скалами у входа в северную гавань.");
        put("Форт Храгстад","Форт",3,141,255,"Заброшенный сторожевой форт.");
        put("Маркарт","Столица",4,146,676,"Высеченный из скалы город с высокими водопадами.");
        put("Картвастен","Деревня",4,236,697,"Шахтёрское поселение у западных путей.");
        put("Старый Хролдан","Таверна",4,250,772,"Гостиница у древних укреплений.");
        put("Душник-Йал","Поселение орков",4,239,849,"Орочья крепость у каменных долин.");
        put("Ларгашбур","Поселение орков",4,308,866,"Крепость за стенами горных утёсов.");
        put("Бтардамз","Двемерские руины",4,97,615,"Скрытые механизмы под тоннами камня.");
        put("Пещера Утёсного Скитальца","Пещера",4,125,800,"Тёмные ходы и внезапные осыпи.");
        put("Фолкрит","Столица",5,416,967,"Город среди чёрных елей и древних кладбищ.");
        put("Озеро Илиналта","Озеро",5,471,870,"Широкое холодное озеро с островками и утопленными тайнами.");
        put("Полулунная лесопилка","Лесопилка",5,388,884,"Пахнет мокрой древесиной и смолой.");
        put("Святилище Тёмного Братства","Святилище",5,364,1014,"Скрывшийся в лесах вход, о котором не принято говорить вслух.");
        put("Форт Нойград","Форт",5,500,1018,"Каменная крепость рядом с южными тропами.");
        put("Дозор Стендарра","Форт",5,492,920,"Пограничная стоянка следопытов и храмовых рыцарей.");
        put("Винтерхолд","Столица",6,812,287,"Разрушенный приморский город у полуночного студёного моря.");
        put("Коллегия Винтерхолда","Коллегия",6,856,253,"Школа магии на скалистом острове, соединённом узким мостом.");
        put("Саартал","Руины",6,766,341,"Древние раскопки под толстым снегом.");
        put("Пещера Хоба","Пещера",6,859,324,"Прибрежный грот с опасными заклинателями.");
        put("Лабиринтиан","Руины",6,519,420,"Монументальные руины древних королей-магов.");
        put("Данстар","Столица",7,578,247,"Рыбацкие пристани и мрачные ночные сны.");
        put("Святилище Мерунеса Дагона","Святилище",7,631,298,"Храм на скалистом хребте.");
        put("Алтарь Призывателя","Святилище",7,601,374,"Каменный алтарь среди ветреных склонов.");
        put("Форт Данстад","Форт",7,546,345,"Старый форт между северными дорогами.");
        put("Руины Мзинчалефт","Двемерские руины",7,498,312,"Механические недра старого подземного города.");
        put("Морфал","Столица",8,373,405,"Поселение на границе туманных болот Хьялмарка.");
        put("Дом Алвы","Дом",8,390,414,"Снаружи обычный дом, но двери скрывают мрачные истории.");
        put("Устенгрев","Руины",8,459,456,"Нордские курганы, укрытые туманом.");
        put("Форт Сноухок","Форт",8,422,481,"Крепость между влажными низинами.");
        put("Болото Хьялмарка","Болото",8,344,473,"Тёмные воды, торфяной запах и низкие серые облака.");
        put("Воронья Скала","Поселение",9,975,399,"Порт Солстейма, где пепел вулкана покрывает крыши.");
        put("Тел-Митрин","Поселение",9,1041,457,"Гигантский грибной дом могущественного мага.");
        put("Деревня скаалов","Деревня",9,1012,277,"Поселение у замёрзших лесов острова.");
        put("Храм Мирака","Храм",9,1022,348,"Заброшенный храм с древними и опасными стенами.");
        put("Камень Ветра","Камень",9,1001,254,"Древняя магическая сила на снежном острове.");
        put("Камень Земли","Камень",9,962,341,"Камень силы среди лавовых полей.");
        put("Камень Солнца","Камень",9,1087,397,"Один из загадочных камней острова.");
        put("Нчардак","Двемерские руины",9,1065,339,"Двемерское сооружение над водой.");
        put("Кагрумез","Двемерские руины",9,1075,463,"Руины с древними испытаниями механизмов.");
        put("Колбьорн","Курган",9,959,328,"Раскопки под толстым слоем пепла.");
        put("Ущелье Фростмуна","Переход",9,1022,384,"Лесной проход среди снегов и камня.");
        put("Чёрный Предел","Подземелье",6,705,516,"Гигантская подземная область светящихся грибов и древних руин.");
        put("Совнгард","Иной мир",0,603,115,"Зал памяти нордских героев, недосягаемый обычной дорогой.");
        put("Каирн Душ","Иной мир",8,118,180,"Мир за гранью живых, связанный с могущественной некромантией.");
        put("Забытая долина","Долина",4,86,418,"Скрытая заснеженная долина, где свет отражается от ледяных стен.");
        put("Чёрный проход","Переход",2,749,598,"Старый тракт между горными лесами.");
        put("Перевал Бледной тропы","Переход",5,520,1076,"Южная дорога через горы.");
        put("Убежище Драконьего Моста","Форт",3,292,460,"Укреплённый участок важной дороги.");
    }
    private WorldAtlas(){}
    private static boolean geographyAdded;
    private static synchronized void ensureGeography(){
        if(geographyAdded)return;
        geographyAdded=true;
        for(SkyrimMapCatalog.Site site:SkyrimMapCatalog.all()){
            int existing=-1;
            for(int i=0;i<places.size();i++)if(places.get(i).name.equals(site.name)){existing=i;break;}
            Place old=existing<0?null:places.get(existing);
            String description=old==null?site.type+" · "+site.grid+". "+
                "Переход ведёт к этому месту; доступ к отдельным сюжетным областям зависит от событий истории.":old.description;
            Place location=new Place(site.name,site.type,site.region,
                old==null?Math.round(site.x):old.x,old==null?Math.round(site.y):old.y,description);
            if(existing<0)places.add(location);else places.set(existing,location);
        }
    }
    public static ArrayList<Place> all(){ensureGeography();return new ArrayList<>(places);}
    public static Place find(String name){
        ensureGeography();
        SkyrimMapCatalog.Site site=SkyrimMapCatalog.find(name);
        String canonical=site==null?name:site.name;
        for(Place p:places)if(p.name.equals(canonical))return p;
        return null;
    }
    public static ArrayList<Place> byRegion(int id){
        ensureGeography();
        ArrayList<Place> a=new ArrayList<>();for(Place p:places)if(p.region==id)a.add(p);
        return a;
    }
    public static String icon(String type){
        if(type.contains("Столица"))return "♜";
        if(type.contains("Деревня")||type.contains("Поселение"))return "⌂";
        if(type.contains("Руины")||type.contains("Курган"))return "◈";
        if(type.contains("Озеро")||type.contains("Болото"))return "≈";
        if(type.contains("Гора")||type.contains("Переход"))return "△";
        if(type.contains("Коллегия")||type.contains("Святилище"))return "✧";
        if(type.contains("Форт"))return "⬟";
        if(type.contains("Пещера")||type.contains("Подземелье"))return "◉";
        return "◇";
    }
    public static final class MapView extends View {
        private final GameEngine game;
        private final Set<String> hiddenTypes;
        private final Paint paint=new Paint(3);
        private OnPlace tap;
        private static final int INK=0xffeaeded,GOLD=0xffc7d0d4,WATER=0xff3b464b;
        public interface OnPlace{void selected(Place p);}
        public MapView(Context c,GameEngine g,OnPlace listener){
            this(c,g,listener,Collections.emptySet());
        }
        public MapView(Context c,GameEngine g,OnPlace listener,Set<String> hidden){
            super(c);game=g;tap=listener;
            hiddenTypes=new HashSet<>(hidden);
            setMinimumHeight(900);
        }
        private boolean visible(Place place){
            // Never hide the player's position even if its category is disabled.
            return place.name.equals(game.progress.location)||!hiddenTypes.contains(place.type);
        }
        @Override protected void onMeasure(int w,int h){
            int width=MeasureSpec.getSize(w);
            setMeasuredDimension(width,Math.round(width*1.25f));
        }
        private void color(int c,float stroke){paint.setColor(c);paint.setStrokeWidth(stroke);paint.setStyle(Paint.Style.STROKE);}
        private void line(Canvas c,float x1,float y1,float x2,float y2){
            c.drawLine(x1,y1,x2,y2,paint);
        }
        @Override protected void onDraw(Canvas original){
            super.onDraw(original);
            original.drawColor(0xff07090a);
            Canvas c=original;
            c.save();c.scale(getWidth()/1120f,getHeight()/1160f);
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xff0e1214);
            c.drawRoundRect(12,12,1108,1148,3,3,paint);
            paint.setColor(0xff171d1f);
            for(int i=0;i<13;i++){float y=i*95; c.drawRoundRect(25,y,1090,y+37,18,18,paint);}
            paint.setColor(0xff2f3536);
            Path land=new Path();land.moveTo(125,210);land.lineTo(242,160);land.lineTo(388,220);land.lineTo(540,130);
            land.lineTo(812,171);land.lineTo(910,259);land.lineTo(931,615);land.lineTo(865,1050);
            land.lineTo(640,1090);land.lineTo(400,1052);land.lineTo(145,852);land.lineTo(62,580);land.close();c.drawPath(land,paint);
            paint.setColor(0xff51595b);
            for(int i=0;i<90;i++){
                float x=86+(i*79)%775,y=239+(i*173)%762;
                Path mountain=new Path();mountain.moveTo(x-19,y+17);mountain.lineTo(x,y-23);mountain.lineTo(x+20,y+17);mountain.close();
                c.drawPath(mountain,paint);
            }
            color(WATER,16);
            Path river=new Path();river.moveTo(792,400);river.cubicTo(695,540,570,591,595,782);river.cubicTo(575,864,470,897,390,1030);
            c.drawPath(river,paint);
            Path lake=new Path();lake.addOval(378,830,548,885,Path.Direction.CW);
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xff353d41);c.drawPath(lake,paint);
            color(0xff9aa4a7,4);
            int[][] edges={{0,3},{0,1},{0,2},{0,5},{0,4},{0,8},{7,6},{7,8},{6,2},{2,1},{4,5},{3,8}};
            String[] capitals={"Вайтран","Рифтен","Виндхельм","Солитьюд","Маркарт","Фолкрит","Винтерхолд","Данстар","Морфал"};
            for(int[] e:edges){Place a=find(capitals[e[0]]),b=find(capitals[e[1]]);
                if(a!=null&&b!=null)line(c,a.x,a.y,b.x,b.y);}
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xff151a1b);c.drawOval(919,219,1107,524,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(0xff747e81);c.drawOval(919,219,1107,524,paint);
            Place player=find(game.progress.location);
            for(Place p:places){
                if(!visible(p))continue;
                float x=p.x,y=p.y;
                boolean city=p.type.contains("Столица")||p.name.equals("Воронья Скала");
                boolean isPlayer=p.name.equals(game.progress.location);
                paint.setStyle(Paint.Style.FILL);paint.setColor(isPlayer?0xffecf0f1:city?GOLD:0xff9da8ac);
                String landmark=isPlayer?"маркер игрока":
                    city?"столицы владений":
                    p.type.contains("Пещера")?"пещеры":
                    p.type.contains("Руины")?"нордские руины":
                    p.type.contains("Крепость")?"крепости":
                    p.type.contains("Деревня")?"деревни":
                    p.type.contains("Башня")?"башни":
                    p.type.contains("Святилище")?"даэдрические святилища":
                    p.type.contains("Лагерь")?"лагеря":"обнаруженные места";
                if(!SkyrimIconAtlas.drawMarker(getContext(),c,landmark,
                    isPlayer?0xffecf0f1:GOLD,x,y,isPlayer?31:city?22:14)){
                    c.drawCircle(x,y,isPlayer?13:city?8:4,paint);
                }
                if(city||isPlayer){
                    paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    paint.setTextSize(isPlayer?24:20);
                    paint.setColor(isPlayer?0xfff0f3f3:INK);
                    c.drawText(p.name,Math.max(15,Math.min(960,x-35)),y-15,paint);
                }
                if(isPlayer){
                    color(0xffccd6da,2.5f);c.drawCircle(x,y,24,paint);
                    paint.setStyle(Paint.Style.FILL);paint.setTextSize(24);paint.setColor(0xffecf1f1);
                    SkyrimIconAtlas.drawMarker(getContext(),c,"маркер игрока",
                        0xffecf1f1,x,y,26);
                }
            }
            paint.setStyle(Paint.Style.FILL);paint.setColor(INK);paint.setTextSize(26);
            c.drawText("ᛟ   С К А Й Р И М   ᛟ",52,70,paint);
            paint.setTextSize(16);paint.setColor(0xffb8c0c3);
            c.drawText("УСЛОВНАЯ ИГРОВАЯ СХЕМА | НАЖМИ НА МЕТКУ",52,98,paint);
            // Compass rose and engraved rim; the atlas remains an explicitly schematic map.
            color(GOLD,2);
            c.drawCircle(1023,83,30,paint);
            line(c,1023,39,1023,127);line(c,980,83,1066,83);
            paint.setStyle(Paint.Style.FILL);paint.setTextSize(20);paint.setColor(GOLD);
            c.drawText("N",1017,32,paint);
            color(0xff829095,2);c.drawRect(21,21,1099,1137,paint);
            c.drawText("● игрок   ● столица   · другие места",52,1115,paint);
            c.restore();
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float px=e.getX()*1120f/getWidth(),py=e.getY()*1160f/getHeight();
            Place best=null;double distance=Double.MAX_VALUE;
            for(Place p:places){
                if(!visible(p))continue;
                double d=Math.hypot(px-p.x,py-p.y);
                if(d<distance){best=p;distance=d;}
            }
            if(best!=null&&distance<45&&tap!=null)tap.selected(best);
            performClick();return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}

