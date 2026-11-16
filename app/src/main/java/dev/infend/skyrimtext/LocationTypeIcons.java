package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/** Six approved location markers, kept separate from the existing city atlas. */
final class LocationTypeIcons {
    private static final int CELL=96, COLUMNS=3, ROWS=2;
    private static final String PREFIX="location:";
    private static Bitmap atlas;
    private static boolean attempted;
    private static int resourceGeneration=-1;
    private static final Paint PAINT=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private LocationTypeIcons(){}

    static String keyForSite(SkyrimMapCatalog.Site site){
        switch(site.type){
            case "Форт":return PREFIX+"fort";
            case "Ферма":return PREFIX+"farm";
            case "Лагерь великанов":return PREFIX+"giantcamp";
            case "Лагерь":case "Имперский лагерь":case "Лагерь Братьев Бури":return PREFIX+"camp";
            case "Пещера":return PREFIX+"cave";
            case "Камень":return site.sheet==0?PREFIX+"stones":null;
            default:return null;
        }
    }
    private static int indexFor(String key){
        if(key==null)return -1;
        switch(key){
            case PREFIX+"fort":return 0;
            case PREFIX+"farm":return 1;
            case PREFIX+"camp":return 2;
            case PREFIX+"giantcamp":return 3;
            case PREFIX+"cave":return 4;
            case PREFIX+"stones":return 5;
            default:return -1;
        }
    }
    private static synchronized Bitmap load(Context context){
        int current=ResourceStore.generation();
        if(resourceGeneration!=current){resourceGeneration=current;attempted=false;atlas=null;}
        if(attempted)return atlas;
        attempted=true;
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inScaled=false;
        Bitmap loaded=null;
        try(java.io.InputStream in=ResourceStore.open(context,"media/mapicons/location_icons.webp")){
            loaded=BitmapFactory.decodeStream(in,null,options);
        }catch(Exception ignored){}
        if(loaded==null||loaded.getWidth()!=CELL*COLUMNS||loaded.getHeight()!=CELL*ROWS)
            loaded=BitmapFactory.decodeResource(context.getResources(),R.drawable.location_icons,options);
        if(loaded!=null&&loaded.getWidth()==CELL*COLUMNS&&loaded.getHeight()==CELL*ROWS)atlas=loaded;
        return atlas;
    }
    static boolean draw(Context context,Canvas canvas,String key,float width,float height){
        int index=indexFor(key);
        if(index<0||width<=0||height<=0)return false;
        Bitmap image=load(context);
        if(image==null)return false;
        int x=index%COLUMNS*CELL,y=index/COLUMNS*CELL;
        Rect src=new Rect(x,y,x+CELL,y+CELL);
        float size=Math.min(width,height)*.94f;
        RectF dst=new RectF((width-size)/2f,(height-size)/2f,(width+size)/2f,(height+size)/2f);
        canvas.drawBitmap(image,src,dst,PAINT);
        return true;
    }
}
