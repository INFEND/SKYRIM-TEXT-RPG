package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/** Offline title art, cropped without stretching; cool Nordic grading and AMOLED fade. */
final class TitleArtView extends View {
    static final String[] NAMES={"Ветреный пик","Руины Бромьюнаара","Зал со скелетом дракона","Глотка Мира","Высокий Хротгар","Стена Слов"};
    private static final String[] SLUGS={"bleakfallsbarrow","labyrinthian","labyrinthian","throatoftheworld","highhrothgar","throatoftheworld"};
    private static final String[] FRAMES={"01.webp","01.webp","02.webp","01.webp","01.webp","02.webp"};
    static final String PREF_ART="title_art", PREF_RANDOM="title_art_random", PREF_SNOW="title_art_snow", PREF_MOTION="title_art_motion";
    private final Paint image=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint shade=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flakes=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap bitmap;
    private final boolean snow,motion;
    private final long start=SystemClock.uptimeMillis();
    private final RectF destination=new RectF();
    static int normalize(int n){return n>=0&&n<NAMES.length?n:0;}
    TitleArtView(Context context,int index,boolean snow,boolean motion){
        super(context);
        this.snow=snow;this.motion=motion;
        bitmap=load(context,normalize(index));
        ColorMatrix gray=new ColorMatrix();gray.setSaturation(.24f);
        ColorMatrix cool=new ColorMatrix(new float[]{
            .80f,0,0,0,0, 0,.91f,0,0,0, 0,0,1.04f,0,0, 0,0,0,1,0});
        gray.postConcat(cool);image.setColorFilter(new ColorMatrixColorFilter(gray));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private static Bitmap load(Context context,int index){
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inPreferredConfig=Bitmap.Config.ARGB_8888;
        options.inSampleSize=2;
        try(InputStream in=context.getAssets().open("title_art/"+index+".webp")){
            Bitmap result=BitmapFactory.decodeStream(in,null,options);
            if(result!=null)return result;
        }catch(Exception ignored){}
        // Full local builds still have the original gallery archives.
        try(InputStream in=ResourceStore.open(context,"gallery/"+SLUGS[index]+".zip");
            ZipInputStream zip=new ZipInputStream(in)){
            ZipEntry entry;while((entry=zip.getNextEntry())!=null){
                if(FRAMES[index].equals(entry.getName())){
                    java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
                    byte[] buffer=new byte[32768];int count;
                    while((count=zip.read(buffer))>0){
                        if(bytes.size()+count>2_000_000)return null;
                        bytes.write(buffer,0,count);
                    }
                    byte[] raw=bytes.toByteArray();
                    return BitmapFactory.decodeByteArray(raw,0,raw.length,options);
                }
            }
        }catch(Exception ignored){}
        return null;
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight();
        if(w<=0||h<=0)return;
        canvas.drawColor(Color.BLACK);
        double time=(SystemClock.uptimeMillis()-start)/1000.0;
        if(bitmap!=null&&!bitmap.isRecycled()){
            float scale=Math.max(w/bitmap.getWidth(),h/bitmap.getHeight())*(motion?1.045f:1f);
            float bw=bitmap.getWidth()*scale,bh=bitmap.getHeight()*scale;
            float dx=(w-bw)/2f,dy=(h-bh)/2f;
            if(motion){dx+=(float)Math.sin(time*.22)*Math.min((bw-w)/2,w*.014f);dy+=(float)Math.cos(time*.19)*Math.min((bh-h)/2,h*.012f);}
            destination.set(dx,dy,dx+bw,dy+bh);
            canvas.drawBitmap(bitmap,null,destination,image);
        }
        shade.setShader(new LinearGradient(0,0,0,h,
            new int[]{0x55000000,0x18000000,0x66000000,0xe8000000,0xff000000},
            new float[]{0f,.23f,.54f,.87f,1f},Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,shade);
        shade.setShader(null);
        if(snow){
            float density=getResources().getDisplayMetrics().density;
            for(int i=0;i<30;i++){
                float x=(float)(((i*.61803398875+time*(.003+(i%5)*.001))%1)*w);
                float y=(float)(((i*.38196601125+time*(.024+(i%4)*.007))%1)*h);
                float fade=1f-Math.min(1f,y/(h*.88f));
                flakes.setColor(Color.argb(Math.round((26+i%4*15)*fade),205,225,239));
                canvas.drawCircle(x,y,density*(.55f+(i%3)*.32f),flakes);
            }
        }
        if((snow||motion)&&isShown()&&isAttachedToWindow())postInvalidateDelayed(42);
    }
}
