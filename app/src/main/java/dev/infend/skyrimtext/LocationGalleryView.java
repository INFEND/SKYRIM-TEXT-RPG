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
        "Хелген","Айварстед","Рорикстед","Драконий Мост","Картвастен","Шорский Камень","Кинсгроув","Чёрный Брод","Каменные Холмы","Воронья Скала","Тел-Митрин","Деревня скаалов"
    };
    private static final String[] SLUGS={
        "whiterun","windhelm","winterhold","dawnstar","markarth",
        "morthal","riverwood","riften","solitude","falkreath",
        "helgen","ivarstead","rorikstead","dragonbridge","karthwasten","shorsstone","kynesgrove","darkwater","stonehills","ravenrock","telmithryn","skaal"
    };
    private static final Map<String,String> ID_BY_CITY=new HashMap<>();
    private static final Map<String,Integer> PHOTO_INDEX_BY_CITY=new HashMap<>();
    /** Labels follow location/sub-location names, not gallery image numbers. */
    private static final String[][] PHOTO_LABELS={
        {"Панорама Вайтрана","Главные ворота","Рынок","Ветреный район","Йоррваскр","Драконий Предел"},
        {"Главные ворота","Серый квартал","Рынок","Каменный квартал","Дворец королей","Набережная"},
        {"Подход к Винтерхолду","Заснеженная улица","Главная площадь","Врата Коллегии","Двор Коллегии","Зимний двор"},
        {"Подход к Данстару","Порт","Главная улица","Таверна","У причала","Заснеженная бухта"},
        {"Городские ворота","Каменные улицы","Рынок","Храм Дибеллы","Подкаменная крепость","Террасы Маркарта"},
        {"Подход к Морфалу","Деревянный мост","Главная улица","Таверна","Болотные окраины","Деревня у болота"},
        {"Подход к Ривервуду","Главная улица","Кузница Алвора","Лесопилка","Река у деревни","Таверна «Спящий великан»"},
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
        {"Подход к Тел-Митрину","Двор грибного поселения","Вход в башню","Зал Нелота","Алхимическая лаборатория","Жилище управляющего"},
        {"Подход к деревне скаалов","Заснеженная площадь","Большой дом","Кузница скаалов","Дом шамана","Лесная окраина деревни"}
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
    }
    private static String locationKey(String location){
        return location==null?"":location.trim().toLowerCase(new Locale("ru")).replace('ё','е');
    }
    private static void alias(String name,String canonical){
        ID_BY_CITY.put(locationKey(name),ID_BY_CITY.get(locationKey(canonical)));
        PHOTO_INDEX_BY_CITY.put(locationKey(name),PHOTO_INDEX_BY_CITY.get(locationKey(canonical)));
    }
    public static boolean hasGallery(String location){return ID_BY_CITY.containsKey(locationKey(location));}
    public static int imageCount(String location){
        if(!hasGallery(location))return 0;
        return PHOTO_LABELS[PHOTO_INDEX_BY_CITY.get(locationKey(location))].length;
    }
    public static String[] locations(){return CITIES.clone();}

    private final String location,slug;
    private final int accent;
    private final boolean fullscreen;
    private final ImageView photo;
    private final TextView page;
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
        d.setColor(color);d.setCornerRadius(dp(corners));d.setStroke(dp(1),outline);
        return d;
    }
    private static String archive(String slug,int number){
        if("riften".equals(slug))return number<=3?"riften.zip":"riften_extra.zip";
        if("riverwood".equals(slug)&&number==6)return "riverwood_extra.zip";
        return slug+".zip";
    }
    private static String captionFor(String location,int index){
        Integer city=PHOTO_INDEX_BY_CITY.get(locationKey(location));
        if(city==null)return location;
        return PHOTO_LABELS[city][Math.max(0,Math.min(index,PHOTO_LABELS[city].length-1))];
    }
    private static String cacheKey(String slug,int number,boolean full){
        return slug+"/"+number+"/"+(full?"full":"small");
    }
    /** Decode the city's compact previews once, ahead of swipes. No loading overlay. */
    private void prefetch(){
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
        super(context);
        if(!hasGallery(location))throw new IllegalArgumentException("Галерея не найдена: "+location);
        this.location=location;this.slug=ID_BY_CITY.get(locationKey(location));
        this.accent=accent;this.fullscreen=fullscreen;
        setOrientation(VERTICAL);
        setPadding(dp(10),dp(10),dp(10),dp(12));
        setBackground(new RuneFrameDrawable(0xff11181d,accent,true));

        LinearLayout header=new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(5),dp(2),dp(5),dp(10));
        addView(header,new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(31)));
        TextView heading=text("ᛟ  "+location.toUpperCase(new Locale("ru")),12,accent);
        heading.setTypeface(SovngardeTypeface.face(context,true));
        header.addView(heading,new LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        page=text("",12,0xffddd4c4);
        header.addView(page,new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(24)));

        FrameLayout viewport=new FrameLayout(context);
        viewport.setBackground(panel(0xff080d10,9,0xff3f3931));
        viewport.setClipToOutline(true);
        int height=fullscreen?Math.min(dp(470),(int)(getResources().getDisplayMetrics().heightPixels*0.61f)):dp(205);
        addView(viewport,new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height));

        photo=new ImageView(context);
        photo.setScaleType(fullscreen?ImageView.ScaleType.FIT_CENTER:ImageView.ScaleType.CENTER_CROP);
        photo.setContentDescription("Изображение локации "+location);
        photo.setFocusable(true);
        viewport.addView(photo,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
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
        photo.setOnClickListener(v->{if(!fullscreen)openFullscreen();});

        // No redundant instructions, page counter or arrow overlays.
        show(0);
        prefetch();
    }
    private void move(int delta){
        int count=imageCount(location);
        show((current+delta+count)%count);
    }
    private void show(int index){
        current=index;
        final int target=++requested;
        page.setText(captionFor(location,index));
        photo.animate().cancel();
        // Keep the previous picture visible while the next cached preview resolves.
        // Never show a "loading" banner during manual swiping.
        final String key=cacheKey(slug,index+1,fullscreen);
        Bitmap cached=CACHE.get(key);
        if(cached!=null&&!cached.isRecycled()){
            present(target,cached);
            return;
        }
        IO.execute(()->{
            Bitmap bitmap=decode(getContext().getApplicationContext(),slug,index+1,fullscreen);
            if(bitmap!=null)CACHE.put(key,bitmap);
            post(()->{
                if(target!=requested)return;
                if(bitmap!=null)present(target,bitmap);
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
        try(InputStream raw=context.getAssets().open("gallery/"+archive(slug,index));
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
        image.setContentDescription("Полноэкранное фото локации "+location);
        backdrop.addView(image,new FrameLayout.LayoutParams(-1,-1));

        final int[] selected={current};
        final int[] generation={0};
        final Runnable[] render={null};
        final Context context=getContext().getApplicationContext();
        render[0]=()->{
            int number=selected[0]+1;
            int ticket=++generation[0];
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
                    int count=imageCount(location);
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
        close.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
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
        viewer.show();
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
