package dev.infend.skyrimtext;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Rounded AMOLED frame; shared by cards, controls and themed dialog windows. */
public final class RuneFrameDrawable extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int fill,ink,style;
    private final boolean selected,window;
    private int opacity=255;
    public RuneFrameDrawable(int fillColor,int accentColor,boolean active){
        this(fillColor,accentColor,active,AmoledAppearance.buttons(),false);
    }
    public RuneFrameDrawable(int fillColor,int accentColor,boolean active,int variant,boolean isWindow){
        fill=fillColor;ink=accentColor;selected=active;style=AmoledAppearance.normalize(variant);window=isWindow;
    }
    private float radius(Rect r,float d){
        return Math.min(Math.min(r.width(),r.height())/2f,AmoledAppearance.radius(style,window,0)*d);
    }
    private static int blend(int a,int b,float ratio){
        return android.graphics.Color.rgb((int)(android.graphics.Color.red(a)*(1-ratio)+android.graphics.Color.red(b)*ratio),
            (int)(android.graphics.Color.green(a)*(1-ratio)+android.graphics.Color.green(b)*ratio),
            (int)(android.graphics.Color.blue(a)*(1-ratio)+android.graphics.Color.blue(b)*ratio));
    }
    @Override public void draw(Canvas c){
        Rect r=getBounds();if(r.width()<2||r.height()<2)return;
        float d=Resources.getSystem().getDisplayMetrics().density;
        float rad=radius(r,d);
        RectF bounds=new RectF(r.left+d*.65f,r.top+d*.65f,r.right-d*.65f,r.bottom-d*.65f);
        int surface=blend(0xff000000,fill,window?.6f:.75f);
        int edge=selected?ink:blend(0xff303030,ink,.35f);
        if(style==1){surface=blend(surface,0xff22190d,.55f);edge=selected?ink:blend(0xff5b492e,ink,.35f);}
        if(style==2){surface=blend(surface,0xff0d0d0d,.65f);}
        if(style==4&&selected){surface=blend(0xff17120b,ink,.22f);edge=ink;}
        if(style==5){edge=ink;surface=blend(surface,ink,.055f);}
        if(style==6){surface=selected?blend(surface,ink,.11f):blend(surface,0xff0b0b0b,.75f);}
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(surface);p.setAlpha(opacity);
        if(style==3)p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,
            blend(surface,ink,selected?.17f:.085f),surface,Shader.TileMode.CLAMP));
        c.drawRoundRect(bounds,rad,rad,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d*(selected?1.5f:1f));p.setColor(edge);p.setAlpha(opacity);
        c.drawRoundRect(bounds,rad,rad,p);
        if(style==2&&r.width()>72*d&&r.height()>28*d){
            p.setColor(ink);p.setAlpha(opacity*2/3);p.setStrokeWidth(d);
            float mid=bounds.centerX(),top=bounds.top+3*d;
            android.graphics.Path diamond=new android.graphics.Path();
            diamond.moveTo(mid-4*d,top);diamond.lineTo(mid,top+2*d);diamond.lineTo(mid+4*d,top);diamond.lineTo(mid,top-2*d);diamond.close();
            c.drawPath(diamond,p);
        }
        p.setStyle(Paint.Style.FILL);
    }
    @Override public void getOutline(Outline outline){
        Rect r=getBounds();outline.setRoundRect(r,radius(r,Resources.getSystem().getDisplayMetrics().density));
    }
    @Override public void setAlpha(int alpha){opacity=alpha;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}
    @SuppressWarnings("deprecation") @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
