package dev.infend.skyrimtext;

import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Offline swipe gallery backed by one compact WebP archive per location. */
public final class LocationGalleryView extends LinearLayout {
    private static final String[] CITIES={
        "Вайтран","Виндхельм","Винтерхолд","Данстар","Маркарт",
        "Морфал","Ривервуд","Рифтен","Солитьюд","Фолкрит",
        "Хелген","Айварстед","Рорикстед","Драконий Мост","Картвастен","Шорский Камень","Кинсгроув","Чёрный Брод","Каменные Холмы","Воронья Скала","Тель-Митрин","Деревня скаалов","Высокий Хротгар","Ветреный пик","Чёрный Предел","Лабиринтиан","Коллегия Винтерхолда","Глотка Мира","Устенгрев"
    };
    private static final String[] SLUGS={
        "whiterun","windhelm","winterhold","dawnstar","markarth",
        "morthal","riverwood","riften","solitude","falkreath",
        "helgen","ivarstead","rorikstead","dragonbridge","karthwasten","shorsstone","kynesgrove","darkwater","stonehills","ravenrock","telmithryn","skaal","highhrothgar","bleakfallsbarrow","blackreach","labyrinthian","collegewinterhold","throatoftheworld","ustengrav"
    };
    private static final Map<String,String> ID_BY_CITY=new HashMap<>();
    private static final Map<String,Integer> PHOTO_INDEX_BY_CITY=new HashMap<>();
    /** Labels follow location/sub-location names, not gallery image numbers. */
    private static final String[][] PHOTO_LABELS={
        {"Главные ворота","Главная улица","Рынок","Ветреный район","Йоррваскр","Драконий Предел"},
        {"Главные ворота","Серый квартал","Рынок","Каменный квартал","Дворец королей","Набережная"},
        {"Подход к Винтерхолду","Заснеженная улица","Главная площадь","Врата Коллегии","Двор Коллегии","Зимний двор"},
        {"Подход к Данстару","Порт","Главная улица","Таверна","У причала","Заснеженная бухта"},
        {"Городские ворота","Каменные улицы","Рынок","Храм Дибеллы","Подкаменная крепость","Террасы Маркарта"},
        {"Подход к Морфалу","Деревянный мост","Главная улица","Таверна","Болотные окраины","Деревня у болота"},
        {"Южные ворота Ривервуда","Главная улица Ривервуда","Лесопилка Хода и Гердур","Таверна «Спящий великан»","Мост через Белую реку"},
        {"Городские ворота","Рынок","Городские каналы","Порт","Крепость Миствейл","Нижние улицы"},
        {"Главные ворота","Рынок","Синий дворец","Главная площадь","Городская арка","Набережная"},
        {"Подход к Фолкриту","Главная улица","Рынок","Зал мёртвых","Кладбище","Лесная окраина"},
        {"Подход к Хелгену","Крепостной двор","Улицы Хелгена","Большой зал крепости","Каменные переходы","Пещера под крепостью"},
        {"Подход к Айварстеду","Деревенская улица","Таверна «Вайлмир»","Мост через реку","Курган Погребальный Огонь","Начало Семи тысяч ступеней"},
        {"Подход к Рорикстеду","Главная улица","Таверна «Мороженый фрукт»","Поля Рорикстеда","Двор фермерского дома","Окраина у холмов"},
        {"Подход к Драконьему Мосту","На Драконьем Мосту","Деревенская улица","Таверна «Четыре щита»","Лесопилка у реки","Сторожевой пост"},
        {"Подход к Картвастену","Улица шахтёрского посёлка","Вход в шахту Сануарах","Шахтёрский штрек","Двор посёлка","Дом шахтёра"},
        {"Подход к Шорскому Камню","Шахтёрская деревня","Кузница Фильньяра","Вход в Красную шахту","Выработки Красной шахты","Вечер у общего очага"},
        {"Подход к Кинсгроуву","Таверна «Деревянное кружево»","Вход в шахту Паровой Ожог","Штреки шахты","Роща у посёлка","Зал деревенской таверны"},
        {"Подход к Чёрному Броду","Берег Чёрного Брода","Вход в шахту Золотая Скала","Выработки Золотой Скалы","Шахтёрский лагерь","Речная переправа"},
        {"Подход к Каменным Холмам","Шахтёрский двор","Вход в шахту Каменные Холмы","Горные выработки","Дом у шахты","Болотная окраина"},
        {"Прибытие в Воронью Скалу","Улицы Вороньей Скалы","Торговая площадь","Храм предков","Стена Бастиона","Таверна «Пьяный нетч»"},
        {"Подход к Тель-Митрину","Двор грибного поселения","Вход в башню","Зал Нелота","Алхимическая лаборатория","Жилище управляющего"},
        {"Подход к деревне скаалов","Заснеженная площадь","Большой дом","Кузница скаалов","Дом шамана","Лесная окраина деревни"},
        {"Подход к монастырю","Большой зал","Каменные переходы","Покои Седобородых","Внутренний двор","Врата к горной тропе","Высокий Хротгар на семитысячных ступенях","Своды Высокого Хротгара","Заснеженный тренировочный двор","Кельи Высокого Хротгара","Зал совета"},
        {"Заснеженный Ветреный пик","Разрушенный зал храма","Зал загадки","Дверь с замком Драконьего когтя","Саркофаг у Стены Слов","Сумрачные коридоры"},
        {"Город двемеров","Сияющая грибная роща","Город под янтарной сферой","Светящийся водопад","Лаборатория Синдериона"},
        {"Руины Бромьюнаара","Зал со скелетом дракона","Ледяной коридор","Затонувший зал","Лестницы в тумане","Лабиринт Шалидора"},
        {"Коллегия над морской бездной","Круглая галерея","Библиотека Арканеума","Кузница в Миддене"},
        {"Снежный путь к Глотке Мира","Заснеженная Стена Слов"},
        {"Устенгрев в туманных болотах"}
    };
    private static final ExecutorService IO=Executors.newFixedThreadPool(2);
    private static final Set<String> PREFETCHING=java.util.Collections.synchronizedSet(new HashSet<>());
    // Keep at most around three full-resolution 16:9 views decoded at once.
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(24*1024*1024){
        @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getByteCount();}
    };
    static{
        if(CITIES.length!=SLUGS.length||CITIES.length!=PHOTO_LABELS.length)
            throw new IllegalStateException("Несогласованный каталог галерей");
        for(int i=0;i<CITIES.length;i++){
            ID_BY_CITY.put(locationKey(CITIES[i]),SLUGS[i]);
            PHOTO_INDEX_BY_CITY.put(locationKey(CITIES[i]),i);
        }
        alias("Камень Шора","Шорский Камень");
        alias("Роща Кин","Кинсгроув");
        alias("High Hrothgar","Высокий Хротгар");
        alias("Пещера Чёрный Предел","Чёрный Предел");
        alias("Bleak Falls Barrow","Ветреный пик");
        alias("Blackreach","Чёрный Предел");
        alias("Labyrinthian","Лабиринтиан");
        alias("College of Winterhold","Коллегия Винтерхолда");
        alias("Throat of the World","Глотка Мира");
        alias("Ustengrav","Устенгрев");
        alias("Устенграв","Устенгрев");
    }
    private static final class GalleryDefinition {
        final String name,slug,parent;final String[] captions,archives;
        final Map<String,Integer> cityFrames=new HashMap<>();
        GalleryDefinition(org.json.JSONObject entry)throws Exception{
            name=entry.getString("name");slug=entry.getString("slug");parent=entry.optString("parent","");
            org.json.JSONArray c=entry.getJSONArray("captions"),a=entry.getJSONArray("archives");
            if(name.trim().isEmpty()||!slug.matches("[a-z0-9_-]+")||c.length()==0||c.length()>99||a.length()!=c.length())throw new IllegalArgumentException("Неверная галерея");
            captions=new String[c.length()];archives=new String[c.length()];
            for(int i=0;i<c.length();i++){
                captions[i]=c.getString(i);archives[i]=a.getString(i);
                boolean standardArchive=archives[i].equals(slug+".zip")||archives[i].equals(slug+"_extra.zip");
                boolean whiterunOverride="whiterun".equals(slug)&&i<2&&"media/whiterun/replacement.zip".equals(archives[i]);
                if(captions[i].trim().isEmpty()||!(standardArchive||whiterunOverride))throw new IllegalArgumentException("Неверный кадр галереи");
            }
            org.json.JSONArray places=entry.optJSONArray("cityPlaces");
            if(places==null)return;
            if(!parent.isEmpty()||places.length()!=captions.length)throw new IllegalArgumentException("Неверная привязка городских кадров");
            for(int i=0;i<places.length();i++){
                String place=locationKey(places.getString(i));
                if(!place.isEmpty()&&cityFrames.put(place,i)!=null)throw new IllegalArgumentException("Повтор городской локации");
            }
        }
    }
    private static final class GalleryRegistry {
        final Map<String,GalleryDefinition> names=new HashMap<>(),slugs=new HashMap<>();
        final java.util.ArrayList<String> locations=new java.util.ArrayList<>();
        GalleryRegistry(org.json.JSONArray array,Map<String,ResourceStore.Entry> files)throws Exception{
            for(int i=0;i<array.length();i++){
                org.json.JSONObject entry=array.getJSONObject(i);GalleryDefinition d=new GalleryDefinition(entry);
                if(slugs.put(d.slug,d)!=null||names.put(locationKey(d.name),d)!=null)throw new IllegalArgumentException("Повтор галереи");
                locations.add(d.name);
                org.json.JSONArray aliases=entry.optJSONArray("aliases");
                if(aliases!=null)for(int j=0;j<aliases.length();j++)if(names.put(locationKey(aliases.getString(j)),d)!=null)throw new IllegalArgumentException("Повтор названия");
                if(files!=null)for(String archive:d.archives)if(!files.containsKey(archive.startsWith("media/")?archive:"gallery/"+archive))throw new IllegalArgumentException("Нет архива "+archive);
            }
            if(locations.isEmpty())throw new IllegalArgumentException("Пустой реестр галерей");
            for(GalleryDefinition d:slugs.values()){
                if(d.parent.isEmpty())continue;
                GalleryDefinition owner=names.get(locationKey(d.parent));
                if(owner==null||owner==d||!owner.parent.isEmpty())throw new IllegalArgumentException("Неверная родительская галерея");
            }
        }
    }
    private static volatile GalleryRegistry registry;
    static void validateCatalog(org.json.JSONArray array,Map<String,ResourceStore.Entry> files)throws Exception{new GalleryRegistry(array,files);}
    static void applyCatalog(org.json.JSONArray array)throws Exception{registry=array==null?null:new GalleryRegistry(array,null);}
    private static GalleryDefinition definition(String location){GalleryRegistry r=registry;return r==null?null:r.names.get(locationKey(location));}
    private static String locationKey(String location){
        String key=location==null?"":location.trim().toLowerCase(new Locale("ru")).replace('ё','е');
        if(key.equals("тел-митрин"))return "тель-митрин";
        if(key.equals("аптека тел-митрина"))return "аптека тель-митрина";
        return key;
    }
    private static void alias(String name,String canonical){
        ID_BY_CITY.put(locationKey(name),ID_BY_CITY.get(locationKey(canonical)));
        PHOTO_INDEX_BY_CITY.put(locationKey(name),PHOTO_INDEX_BY_CITY.get(locationKey(canonical)));
    }
    public static boolean hasGallery(String location){return registry!=null?definition(location)!=null:ID_BY_CITY.containsKey(locationKey(location));}
    public static int imageCount(String location){
        GalleryDefinition d=definition(location);
        if(d!=null)return d.captions.length;
        if(!hasGallery(location))return 0;
        return PHOTO_LABELS[PHOTO_INDEX_BY_CITY.get(locationKey(location))].length;
    }
    public static String[] locations(){GalleryRegistry r=registry;return r==null?CITIES.clone():r.locations.toArray(new String[0]);}

    private final String location,slug,title;
    private final int cityFrame;

    static String displayName(String location){
        GalleryDefinition gallery=definition(location);
        return gallery==null?location:gallery.name;
    }

    private static GalleryDefinition cityInterior(String city,String place){
        GalleryDefinition gallery=definition(place);
        return gallery!=null&&!gallery.parent.isEmpty()&&definition(city)!=null&&definition(city)==definition(gallery.parent)?gallery:null;
    }

    /** All downloaded child galleries belong to the shared location directory. */
    static java.util.List<String> childPlaces(String city){
        GalleryRegistry r=registry;GalleryDefinition owner=definition(city);
        java.util.ArrayList<String> children=new java.util.ArrayList<>();
        if(r!=null&&owner!=null)for(GalleryDefinition d:r.slugs.values())
            if(!d.parent.isEmpty()&&r.names.get(locationKey(d.parent))==owner)children.add(d.name);
        children.sort(String.CASE_INSENSITIVE_ORDER);
        return children;
    }
    static boolean sameCityPlace(String city,String first,String second){
        GalleryDefinition a=cityInterior(city,first);
        return a!=null&&a==cityInterior(city,second);
    }

    static String cityPlaceTitle(String city,String place){
        GalleryDefinition interior=cityInterior(city,place);
        if(interior!=null)return interior.name;
        GalleryDefinition overview=definition(city);
        Integer frame=overview==null?null:overview.cityFrames.get(locationKey(place));
        return frame==null?place:overview.captions[frame];
    }

    static LocationGalleryView cityPlaceGallery(Context context,String city,String place,int accent){
        GalleryDefinition interior=cityInterior(city,place);
        if(interior!=null)return new LocationGalleryView(context,interior.name,accent,false);
        GalleryDefinition overview=definition(city);
        Integer frame=overview==null?null:overview.cityFrames.get(locationKey(place));
        if(frame!=null)return new LocationGalleryView(context,city,accent,false,frame,overview.captions[frame]);
        return hasGallery(place)?new LocationGalleryView(context,place,accent,false):null;
    }

    private int visibleImageCount(){return cityFrame<0?imageCount(location):1;}
    private int archiveFrame(int index){return cityFrame<0?index:cityFrame;}

    private final int accent;
    private final boolean fullscreen;
    private final ImageView photo;
    private final TextView page,missingPhoto;
    private final AccordionLayout galleryBody;
    private final LinearLayout indicators;
    private final View collapseArrow;
    private boolean collapsed=false;
    private int current=0,requested=0;

    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int sp,int color){
        TextView t=new TextView(getContext());
        t.setText(value);t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP,sp);
        t.setGravity(Gravity.CENTER_VERTICAL);t.setIncludeFontPadding(false);
        t.setTypeface(SovngardeTypeface.face(getContext(),false));
        return t;
    }
    private GradientDrawable panel(int color,float corners,int outline){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(corners));
        if(outline!=Color.TRANSPARENT)d.setStroke(dp(1),outline);
        return d;
    }
    private static String archive(String slug,int number){
        GalleryRegistry r=registry;GalleryDefinition d=r==null?null:r.slugs.get(slug);
        if(d!=null&&number>=1&&number<=d.archives.length)return d.archives[number-1];
        if("riften".equals(slug))return number<=3?"riften.zip":"riften_extra.zip";
        if("highhrothgar".equals(slug)&&number>6)return "highhrothgar_extra.zip";
        return slug+".zip";
    }
    private static String archivePath(String slug,int number){
        String archive=archive(slug,number);
        return archive.startsWith("media/")?archive:"gallery/"+archive;
    }
    private static String captionFor(String location,int index){
        GalleryDefinition d=definition(location);
        if(d!=null)return d.captions[Math.max(0,Math.min(index,d.captions.length-1))];
        Integer city=PHOTO_INDEX_BY_CITY.get(locationKey(location));
        if(city==null)return location;
        return PHOTO_LABELS[city][Math.max(0,Math.min(index,PHOTO_LABELS[city].length-1))];
    }
    private static String cacheKey(String slug,int number,boolean full){
        return ResourceStore.generation()+"/"+slug+"/"+number+"/"+(full?"full":"small");
    }
    /** Decode the city's compact previews once, ahead of swipes. No loading overlay. */
    private void prefetch(){
        if(cityFrame>=0)return;
        if(!PREFETCHING.add(slug))return;
        Context context=getContext().getApplicationContext();
        IO.execute(()->{
            try{
                for(int n=1;n<=imageCount(location);n++){
                    String key=cacheKey(slug,n,false);
                    if(CACHE.get(key)!=null)continue;
                    Bitmap result=decode(context,slug,n,false);
                    if(result!=null)CACHE.put(key,result);
                }
            }finally{PREFETCHING.remove(slug);}
        });
    }
    public LocationGalleryView(Context context,String location,int accent,boolean fullscreen){
        this(context,location,accent,fullscreen,-1,displayName(location));
    }

    private LocationGalleryView(Context context,String location,int accent,boolean fullscreen,int cityFrame,String title){
        super(context);
        if(!hasGallery(location))throw new IllegalArgumentException("Галерея не найдена: "+location);
        this.location=location;GalleryDefinition d=definition(location);this.slug=d==null?ID_BY_CITY.get(locationKey(location)):d.slug;
        this.cityFrame=cityFrame;this.title=title;
        this.accent=accent;this.fullscreen=fullscreen;
        setOrientation(VERTICAL);
        setPadding(dp(2),dp(4),dp(2),dp(8));
        setBackgroundColor(Color.BLACK);

        LinearLayout header=new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        addView(header,new LayoutParams(-1,dp(48)));
        View pin=galleryGlyph(0,0xffc5aa80);
        header.addView(pin,new LayoutParams(dp(26),dp(26)));
        TextView heading=text(title.toUpperCase(new Locale("ru")),18,Color.WHITE);
        heading.setTypeface(SovngardeTypeface.face(context,true));
        heading.setMaxLines(2);
        LayoutParams headingParams=new LayoutParams(0,-2,1);
        headingParams.leftMargin=dp(8);
        header.addView(heading,headingParams);
        collapseArrow=galleryGlyph(3,0xffa5a5a5);
        header.addView(collapseArrow,new LayoutParams(dp(44),dp(44)));

        galleryBody=new AccordionLayout(context);
        galleryBody.setOrientation(VERTICAL);
        addView(galleryBody,new LayoutParams(-1,-2));
        header.setContentDescription("Свернуть галерею "+title);
        header.setOnClickListener(v->{
            collapsed=!collapsed;
            boolean animate=getContext().getSharedPreferences("skyrim_text_v1",Context.MODE_PRIVATE).getBoolean("animations",true);
            galleryBody.setExpanded(!collapsed,animate);
            AccordionLayout.rotateArrow(collapseArrow,collapsed,animate);
            header.setContentDescription((collapsed?"Развернуть":"Свернуть")+" галерею "+title);
        });

        FrameLayout viewport=new FrameLayout(context){
            @Override protected void onMeasure(int widthSpec,int heightSpec){
                if(!fullscreen&&MeasureSpec.getMode(widthSpec)!=MeasureSpec.UNSPECIFIED){
                    int width=MeasureSpec.getSize(widthSpec);
                    if(width>0)heightSpec=MeasureSpec.makeMeasureSpec(Math.round(width*9f/16f),MeasureSpec.EXACTLY);
                }
                super.onMeasure(widthSpec,heightSpec);
            }
        };
        viewport.setBackground(panel(Color.BLACK,18,Color.TRANSPARENT));
        viewport.setClipToOutline(true);
        int height=fullscreen?Math.min(dp(470),(int)(getResources().getDisplayMetrics().heightPixels*0.61f)):-2;
        galleryBody.addView(viewport,new LayoutParams(-1,height));

        photo=new ImageView(context);
        photo.setScaleType(fullscreen?ImageView.ScaleType.FIT_CENTER:ImageView.ScaleType.CENTER_CROP);
        photo.setContentDescription("Изображение локации "+title);
        photo.setFocusable(true);
        viewport.addView(photo,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        missingPhoto=text("ЗАГРУЗИТЬ ИЗОБРАЖЕНИЯ",16,accent);
        missingPhoto.setGravity(Gravity.CENTER);
        missingPhoto.setVisibility(View.GONE);
        viewport.addView(missingPhoto,new FrameLayout.LayoutParams(-1,-1));
        missingPhoto.setOnClickListener(v->openResourceManager());
        final GestureDetector detector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onFling(MotionEvent e1,MotionEvent e2,float vx,float vy){
                if(e1==null||e2==null)return false;
                float dx=e2.getX()-e1.getX(),dy=e2.getY()-e1.getY();
                if(Math.abs(dx)>dp(45)&&Math.abs(dx)>Math.abs(dy)*1.1f){
                    move(dx<0?1:-1);return true;
                }
                return false;
            }
            @Override public boolean onSingleTapConfirmed(MotionEvent e){
                photo.performClick();
                return true;
            }
        });
        // A vertical gesture must be handled by the parent page ScrollView.
        // Only horizontal swipes belong to this picture carousel.
        photo.setOnTouchListener(new OnTouchListener(){
            private float downX,downY;
            private boolean vertical;
            @Override public boolean onTouch(View view,MotionEvent event){
                int action=event.getActionMasked();
                if(action==MotionEvent.ACTION_DOWN){
                    downX=event.getX();downY=event.getY();vertical=false;
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                }else if(action==MotionEvent.ACTION_MOVE&&!vertical){
                    float dx=Math.abs(event.getX()-downX);
                    float dy=Math.abs(event.getY()-downY);
                    if(dy>dp(12)&&dy>dx*1.15f){
                        vertical=true;
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
                if(vertical)return false;
                detector.onTouchEvent(event);
                return true;
            }
        });
        photo.setOnClickListener(v->{
            if(!ResourceStore.has(getContext(),archivePath(slug,archiveFrame(current)+1))){
                openResourceManager();
            }else if(!fullscreen)openFullscreen();
        });

        LinearLayout controls=new LinearLayout(context);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        galleryBody.addView(controls,new LayoutParams(-1,dp(56)));
        View previous=galleryGlyph(1,Color.WHITE);
        previous.setContentDescription("Предыдущий вид");
        previous.setOnClickListener(v->move(-1));
        controls.addView(previous,new LayoutParams(dp(48),dp(48)));
        page=text("",16,Color.WHITE);
        page.setGravity(Gravity.CENTER);
        page.setMaxLines(2);
        page.setTypeface(SovngardeTypeface.face(context,true));
        controls.addView(page,new LayoutParams(0,-1,1));
        View next=galleryGlyph(2,Color.WHITE);
        next.setContentDescription("Следующий вид");
        next.setOnClickListener(v->move(1));
        controls.addView(next,new LayoutParams(dp(48),dp(48)));
        if(visibleImageCount()==1){previous.setVisibility(View.INVISIBLE);next.setVisibility(View.INVISIBLE);}

        indicators=new LinearLayout(context);
        indicators.setGravity(Gravity.CENTER);
        LayoutParams dotsParams=new LayoutParams(-1,dp(28));
        galleryBody.addView(indicators,dotsParams);
        for(int i=0;i<visibleImageCount();i++){
            final int index=i;
            FrameLayout hit=new FrameLayout(context);
            View dot=new View(context);
            hit.addView(dot,new FrameLayout.LayoutParams(dp(7),dp(7),Gravity.CENTER));
            hit.setContentDescription(captionFor(location,archiveFrame(index)));
            hit.setOnClickListener(v->show(index));
            indicators.addView(hit,new LayoutParams(dp(16),dp(28)));
        }
        show(0);
        prefetch();
        // Child locations are rendered by MainActivity's categorized city directory.
    }
    /** Small borderless vector glyphs, independent of global icon tint and button style. */
    private View galleryGlyph(int kind,int color){
        return new View(getContext()){
            private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(android.graphics.Canvas canvas){
                super.onDraw(canvas);
                canvas.save();
                canvas.translate(getWidth()/2f,getHeight()/2f);
                canvas.scale(dp(20)/24f,dp(20)/24f);
                paint.setColor(color);
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(2.2f);
                paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
                android.graphics.Path path=new android.graphics.Path();
                if(kind==0){
                    path.moveTo(0,10);path.cubicTo(-4,6,-9,1,-9,-3);
                    path.cubicTo(-9,-15,9,-15,9,-3);
                    path.cubicTo(9,1,4,6,0,10);path.close();
                    canvas.drawPath(path,paint);canvas.drawCircle(0,-3,3,paint);
                }else{
                    if(kind==1){path.moveTo(3,-7);path.lineTo(-4,0);path.lineTo(3,7);}
                    else if(kind==2){path.moveTo(-3,-7);path.lineTo(4,0);path.lineTo(-3,7);}
                    else{path.moveTo(-7,3);path.lineTo(0,-4);path.lineTo(7,3);}
                    canvas.drawPath(path,paint);
                }
                canvas.restore();
            }
        };
    }
    private void updateIndicators(){
        for(int i=0;i<indicators.getChildCount();i++){
            FrameLayout hit=(FrameLayout)indicators.getChildAt(i);
            View dot=hit.getChildAt(0);
            boolean active=i==current;
            hit.setSelected(active);
            GradientDrawable marker=new GradientDrawable();
            marker.setShape(GradientDrawable.OVAL);
            marker.setColor(active?accent:0xff363636);
            dot.setBackground(marker);
        }
    }
    private void openResourceManager(){
        if(getContext() instanceof android.app.Activity){
            android.app.Activity a=(android.app.Activity)getContext();
            String initialGroup=archive(slug,archiveFrame(current)+1).startsWith("media/")?"media:whiterun":"gallery:"+slug;
            new ResourceManagerDialog(a,accent,()->{show(current);prefetch();},initialGroup).show();
        }
    }
    private void move(int delta){
        int count=visibleImageCount();
        if(count>1)show(Math.floorMod(current+delta,count));
    }
    private void show(int index){
        int count=visibleImageCount();if(count==0)return;
        if(index<0||index>=count){show(Math.max(0,Math.min(index,count-1)));return;}
        current=index;
        final int target=++requested;
        int frame=archiveFrame(index);
        page.setText(captionFor(location,frame));
        updateIndicators();
        String resourcePath=archivePath(slug,frame+1);
        if(!ResourceStore.has(getContext(),resourcePath)){
            photo.setImageDrawable(null);
            missingPhoto.setVisibility(View.VISIBLE);
            photo.setContentDescription("Изображения отсутствуют. Нажми для загрузки.");
            ResourceStore.requestGallery(getContext(),resourcePath,()->{
                if(isAttachedToWindow()&&target==requested&&ResourceStore.has(getContext(),resourcePath)){show(index);prefetch();}
            });
            return;
        }
        missingPhoto.setVisibility(View.GONE);
        photo.setContentDescription(title+": "+captionFor(location,frame));
        photo.animate().cancel();
        // Keep the previous picture visible while the next cached preview resolves.
        // Never show a "loading" banner during manual swiping.
        final String key=cacheKey(slug,frame+1,fullscreen);
        Bitmap cached=CACHE.get(key);
        if(cached!=null&&!cached.isRecycled()){
            present(target,cached);
            return;
        }
        IO.execute(()->{
            Bitmap bitmap=decode(getContext().getApplicationContext(),slug,frame+1,fullscreen);
            if(bitmap!=null)CACHE.put(key,bitmap);
            post(()->{
                if(target!=requested)return;
                if(bitmap!=null)present(target,bitmap);
                else{photo.setImageDrawable(null);missingPhoto.setVisibility(View.VISIBLE);}
            });
        });
    }
    private void present(int target,Bitmap image){
        if(target!=requested||image==null)return;
        photo.setImageBitmap(image);
        photo.setAlpha(1f);
    }
    @Override protected void onDetachedFromWindow(){
        requested++;
        photo.animate().cancel();
        super.onDetachedFromWindow();
    }
    private static Bitmap decode(Context context,String slug,int index,boolean full){
        String file=String.format(Locale.ROOT,"%02d.webp",index);
        try(InputStream raw=ResourceStore.open(context,archivePath(slug,index));
            ZipInputStream zip=new ZipInputStream(new BufferedInputStream(raw,65536))){
            ZipEntry entry;byte[] buf=new byte[32768];
            while((entry=zip.getNextEntry())!=null){
                if(!entry.getName().equals(file)){zip.closeEntry();continue;}
                ByteArrayOutputStream data=new ByteArrayOutputStream(600000);int n;
                while((n=zip.read(buf))>0){
                    if(data.size()+n>2_000_000)return null;
                    data.write(buf,0,n);
                }
                BitmapFactory.Options opt=new BitmapFactory.Options();
                opt.inPreferredConfig=Bitmap.Config.ARGB_8888;
                opt.inSampleSize=full?1:2;
                byte[] bytes=data.toByteArray();
                return BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);
            }
        }catch(Exception ignored){}
        return null;
    }
    /** A borderless photo lightbox: only an X button; no gallery chrome. */
    private void openFullscreen(){
        Dialog viewer=new Dialog(getContext());
        viewer.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout backdrop=new FrameLayout(getContext());
        backdrop.setBackgroundColor(0xf206090b);
        ImageView image=new ImageView(getContext());
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription("Полноэкранное фото локации "+title);
        backdrop.addView(image,new FrameLayout.LayoutParams(-1,-1));

        final int[] selected={current};
        final int[] generation={0};
        final Runnable[] render={null};
        final Context context=getContext().getApplicationContext();
        render[0]=()->{
            int number=archiveFrame(selected[0])+1;
            int ticket=++generation[0];
            if(!ResourceStore.has(context,archivePath(slug,number))){image.setImageDrawable(null);return;}
            String fullKey=cacheKey(slug,number,true);
            Bitmap full=CACHE.get(fullKey);
            if(full!=null&&!full.isRecycled()){
                image.setImageBitmap(full);
                return;
            }
            // Preview is already in memory, so there is never an empty loading view.
            Bitmap preview=CACHE.get(cacheKey(slug,number,false));
            if(preview!=null&&!preview.isRecycled())image.setImageBitmap(preview);
            IO.execute(()->{
                Bitmap bitmap=decode(context,slug,number,true);
                if(bitmap!=null)CACHE.put(fullKey,bitmap);
                image.post(()->{
                    if(ticket==generation[0]&&viewer.isShowing()&&bitmap!=null)
                        image.setImageBitmap(bitmap);
                });
            });
        };
        final GestureDetector detector=new GestureDetector(getContext(),
            new GestureDetector.SimpleOnGestureListener(){
                @Override public boolean onDown(MotionEvent event){return true;}
                @Override public boolean onFling(MotionEvent start,MotionEvent end,float vx,float vy){
                    if(start==null||end==null)return false;
                    float dx=end.getX()-start.getX(),dy=end.getY()-start.getY();
                    if(Math.abs(dx)<=dp(45)||Math.abs(dx)<=Math.abs(dy)*1.1f)return false;
                    int count=visibleImageCount();
                    if(count<2)return false;
                    selected[0]=(selected[0]+(dx<0?1:-1)+count)%count;
                    render[0].run();
                    return true;
                }
            });
        image.setOnTouchListener((view,event)->{
            detector.onTouchEvent(event);
            return true;
        });

        TextView close=text("×",33,Color.WHITE);
        close.setTypeface(SovngardeTypeface.face(getContext(),true));
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Закрыть полноэкранное фото");
        close.setOnClickListener(v->viewer.dismiss());
        FrameLayout.LayoutParams closeParams=new FrameLayout.LayoutParams(dp(54),dp(54),
            Gravity.TOP|Gravity.END);
        closeParams.topMargin=dp(19);
        closeParams.rightMargin=dp(14);
        backdrop.addView(close,closeParams);
        viewer.setContentView(backdrop);
        viewer.setOnDismissListener(d->generation[0]++);
        viewer.show();SovngardeTypeface.watchDialog(viewer);
        Window window=viewer.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params=window.getAttributes();
            params.dimAmount=.82f;
            params.gravity=Gravity.CENTER;
            params.width=WindowManager.LayoutParams.MATCH_PARENT;
            params.height=WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
        }
        render[0].run();
    }
}
