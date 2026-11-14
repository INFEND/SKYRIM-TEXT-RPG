package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Optional verified portraits, decoded off the UI thread with generation-aware cache. */
public final class CharacterPortraitView extends ImageView {
    private static final ExecutorService DECODER=Executors.newFixedThreadPool(2);
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(8*1024){
        @Override protected int sizeOf(String key,Bitmap value){return Math.max(1,value.getByteCount()/1024);}
    };
    private final String assetPath;
    private int generation;
    public CharacterPortraitView(Context context,String race,String gender,int portrait){
        super(context);
        assetPath=CharacterIdentity.portraitPath(race,gender,portrait);
        setScaleType(ScaleType.CENTER_CROP);
        setBackgroundColor(0xff141516);
        setContentDescription(race+", "+CharacterIdentity.normalizeGender(gender)+", образ "+
            CharacterIdentity.normalizePortrait(portrait));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec);
        if(width==0)width=Math.round(144*getResources().getDisplayMetrics().density);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(Math.round(width*4f/3f),MeasureSpec.EXACTLY));
    }
    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        if(!ResourceStore.has(getContext(),assetPath)){setImageDrawable(null);setContentDescription("Портрет не загружен");return;}
        final String key=assetPath+":"+ResourceStore.generation();
        Bitmap cached=CACHE.get(key);
        if(cached!=null){setImageBitmap(cached);return;}
        final int request=++generation;
        final Context app=getContext().getApplicationContext();
        DECODER.execute(()->{
            Bitmap bitmap=CACHE.get(key);
            if(bitmap==null){
                try(InputStream input=ResourceStore.open(app,assetPath)){
                    bitmap=BitmapFactory.decodeStream(input);
                    if(bitmap!=null)CACHE.put(key,bitmap);
                }catch(java.io.IOException ignored){}
            }
            final Bitmap result=bitmap;
            if(result!=null)post(()->{
                if(request==generation&&isAttachedToWindow()&&key.equals(assetPath+":"+ResourceStore.generation())&&ResourceStore.has(app,assetPath))setImageBitmap(result);
            });
        });
    }
    @Override protected void onDetachedFromWindow(){generation++;super.onDetachedFromWindow();}
}
