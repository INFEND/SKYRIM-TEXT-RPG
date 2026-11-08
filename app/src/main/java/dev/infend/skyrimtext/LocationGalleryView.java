package dev.infend.skyrimtext;

import android.app.AlertDialog;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Offline swipe gallery backed by one compact WebP archive per location. */
public final class LocationGalleryView extends LinearLayout {
    private static final String[] CITIES={
        "Вайтран","Виндхельм","Винтерхолд","Данстар","Маркарт",
        "Морфал","Ривервуд","Рифтен","Солитьюд","Фолкрит"
    };
    private static final String[] SLUGS={
        "whiterun","windhelm","winterhold","dawnstar","markarth",
        "morthal","riverwood","riften","solitude","falkreath"
    };
    private static final Map<String,String> ID_BY_CITY=new HashMap<>();
    private static final ExecutorService IO=Executors.newFixedThreadPool(2);
    // Keep at most around three full-resolution 16:9 views decoded at once.
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(24*1024*1024){
        @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getByteCount();}
    };
    static{
        for(int i=0;i<CITIES.length;i++)ID_BY_CITY.put(CITIES[i],SLUGS[i]);
    }
    public static boolean hasGallery(String location){return ID_BY_CITY.containsKey(location);}
    public static int imageCount(String location){
        if(!hasGallery(location))return 0;
        return "Ривервуд".equals(location)?5:6;
    }
    public static String[] locations(){return CITIES.clone();}

    private final String location,slug;
    private final int accent;
    private final boolean fullscreen;
    private final ImageView photo;
    private final TextView page,caption,loading;
    private final TextView leftArrow,rightArrow;
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
        return slug+".zip";
    }
    private static String captionFor(String location,int index){
        if("Винтерхолд".equals(location)){
            String[] titles={
                "Заснеженный посёлок и крепость","Улица к крепости",
                "Внутренняя площадь","Главные врата академии",
                "Магический двор","Статуя и источник"
            };
            return titles[index];
        }
        return index==0?"Главный вид · "+location:location+" · ракурс "+(index+1);
    }
    public LocationGalleryView(Context context,String location,int accent,boolean fullscreen){
        super(context);
        if(!hasGallery(location))throw new IllegalArgumentException("Галерея не найдена: "+location);
        this.location=location;this.slug=ID_BY_CITY.get(location);
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
        loading=text("Загрузка изображения…",13,0xffb6ada0);
        loading.setGravity(Gravity.CENTER);
        viewport.addView(loading,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        leftArrow=arrow(context,"❮",true);
        rightArrow=arrow(context,"❯",false);
        FrameLayout.LayoutParams lpLeft=new FrameLayout.LayoutParams(dp(42),dp(52),Gravity.START|Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams lpRight=new FrameLayout.LayoutParams(dp(42),dp(52),Gravity.END|Gravity.CENTER_VERTICAL);
        lpLeft.leftMargin=dp(6);lpRight.rightMargin=dp(6);
        viewport.addView(leftArrow,lpLeft);viewport.addView(rightArrow,lpRight);
        leftArrow.setOnClickListener(v->move(-1));
        rightArrow.setOnClickListener(v->move(1));
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
        photo.setOnTouchListener((v,event)->{
            detector.onTouchEvent(event);
            return true;
        });
        photo.setOnClickListener(v->{if(!fullscreen)openFullscreen();});

        caption=text("",13,0xffddd4c4);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(dp(5),dp(12),dp(5),dp(3));
        addView(caption,new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(42)));
        TextView hint=text(fullscreen?"Листай изображения влево или вправо":"Проведи вбок или нажми на фото для увеличения",11,0xffa79c8c);
        hint.setGravity(Gravity.CENTER);
        addView(hint,new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(24)));
        show(0);
    }
    private TextView arrow(Context c,String text,boolean left){
        TextView v=text(text,26,accent);
        v.setGravity(Gravity.CENTER);
        v.setBackground(panel(0xdf0b1116,8,0xff635139));
        v.setContentDescription(left?"Предыдущее изображение":"Следующее изображение");
        v.setFocusable(true);
        return v;
    }
    private void move(int delta){
        int count=imageCount(location);
        show((current+delta+count)%count);
    }
    private void show(int index){
        current=index;
        final int target=++requested;
        page.setText((index+1)+" / "+imageCount(location));
        caption.setText(captionFor(location,index));
        photo.animate().cancel();
        photo.setAlpha(0.45f);
        loading.setVisibility(View.VISIBLE);
        final String key=slug+"/"+(index+1)+"/"+(fullscreen?"full":"small");
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
                else{loading.setText("Изображение временно недоступно");photo.setAlpha(1f);}
            });
        });
    }
    private void present(int target,Bitmap image){
        if(target!=requested||image==null)return;
        photo.setImageBitmap(image);
        loading.setVisibility(View.GONE);
        photo.animate().alpha(1f).setDuration(180).start();
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
    private void openFullscreen(){
        LinearLayout root=new LinearLayout(getContext());
        root.setOrientation(VERTICAL);
        root.setPadding(dp(8),dp(8),dp(8),dp(8));
        LocationGalleryView large=new LocationGalleryView(getContext(),location,accent,true);
        large.show(current);
        root.addView(large,new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        new RuneDialogBuilder(getContext(),accent)
            .setTitle("Галерея · "+location)
            .setView(root)
            .setNegativeButton("Закрыть",null)
            .show();
    }
}
