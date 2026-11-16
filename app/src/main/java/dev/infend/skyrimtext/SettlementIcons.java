package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/** Eleven approved monochrome settlement glyphs, cropped from the user's chosen sheet.
 * Small core resource: available immediately in the lightweight APK, offline. */
final class SettlementIcons {
    private static final int CELL=96, COLUMNS=4, ROWS=3;
    private static final String PREFIX="settlement:";
    private static Bitmap atlas;
    private static boolean attempted;
    private static int resourceGeneration=-1;
    private SettlementIcons(){}

    static String keyForSite(SkyrimMapCatalog.Site site){
        if(site.capital()){
            switch(site.english){
                case "Solitude": return PREFIX+"solitude";
                case "Dawnstar": return PREFIX+"dawnstar";
                case "Winterhold": return PREFIX+"winterhold";
                case "Windhelm": return PREFIX+"windhelm";
                case "Markarth": return PREFIX+"markarth";
                case "Morthal": return PREFIX+"morthal";
                case "Whiterun": return PREFIX+"whiterun";
                case "Riften": return PREFIX+"riften";
                case "Falkreath": return PREFIX+"falkreath";
                default: return PREFIX+"town"; // Raven Rock has no approved distinct glyph.
            }
        }
        if(site.type.contains("Деревня")||site.type.equals("Город"))return PREFIX+"town";
        if(site.type.contains("Поселение"))return PREFIX+"settlement";
        return LocationTypeIcons.keyForSite(site);
    }
    private static int indexFor(String key){
        if(key==null)return -1;
        switch(key){
            case PREFIX+"solitude":return 0;
            case PREFIX+"dawnstar":return 1;
            case PREFIX+"winterhold":return 2;
            case PREFIX+"windhelm":return 3;
            case PREFIX+"markarth":return 4;
            case PREFIX+"morthal":return 5;
            case PREFIX+"whiterun":return 6;
            case PREFIX+"riften":return 7;
            case PREFIX+"falkreath":return 8;
            case PREFIX+"town":return 9;
            case PREFIX+"settlement":return 10;
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
        try(java.io.InputStream in=ResourceStore.open(context,"media/mapicons/settlement_icons.webp")){
            loaded=BitmapFactory.decodeStream(in,null,options);
        }catch(Exception ignored){}
        if(loaded==null||loaded.getWidth()!=CELL*COLUMNS||loaded.getHeight()!=CELL*ROWS)
            loaded=BitmapFactory.decodeResource(context.getResources(),R.drawable.settlement_icons,options);
        if(loaded!=null&&loaded.getWidth()==CELL*COLUMNS&&loaded.getHeight()==CELL*ROWS)
            atlas=loaded;
        return atlas;
    }
    static boolean draw(Context context,Canvas canvas,String key,float width,float height){
        if(LocationTypeIcons.draw(context,canvas,key,width,height))return true;
        int index=indexFor(key);
        if(index<0||width<=0||height<=0)return false;
        Bitmap image=load(context);
        if(image==null)return false;
        int x=index%COLUMNS*CELL,y=index/COLUMNS*CELL;
        Rect src=new Rect(x,y,x+CELL,y+CELL);
        float size=Math.min(width,height)*.94f;
        RectF dst=new RectF((width-size)/2f,(height-size)/2f,
            (width+size)/2f,(height+size)/2f);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        // Preserve the approved grey artwork; never recolor it with the theme accent.
        canvas.drawBitmap(image,src,dst,paint);
        return true;
    }
}
