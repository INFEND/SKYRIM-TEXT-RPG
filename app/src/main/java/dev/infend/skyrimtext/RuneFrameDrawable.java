package dev.infend.skyrimtext;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Classic Skyrim-inspired menu glass: inky, square, thin pewter rules,
 * transparent corner knots. Preserves all original call sites.
 */
public final class RuneFrameDrawable extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int fill,ink;
    private final boolean selected;
    public RuneFrameDrawable(int fillColor,int accentColor,boolean active){
        fill=fillColor;ink=accentColor;selected=active;
    }
    private void line(Canvas c,float x,float y,float x2,float y2){
        c.drawLine(x,y,x2,y2,p);
    }
    private void corner(Canvas c,float x,float y,float mirrorX,float mirrorY,float size){
        c.save();c.translate(x,y);c.scale(mirrorX,mirrorY);
        // Double meander and broken-angle terminals, isolated from the panel fill.
        line(c,0,size,0,0);line(c,0,0,size,0);
        line(c,3,size*.73f,3,3);line(c,3,3,size*.73f,3);
        line(c,6,size*.72f,6,6);line(c,6,6,size*.72f,6);
        line(c,0,size*.36f,size*.36f,size*.36f);
        line(c,size*.36f,0,size*.36f,size*.36f);
        line(c,size*.36f,size*.36f,size*.18f,size*.36f);
        line(c,size*.36f,size*.36f,size*.36f,size*.18f);
        line(c,size*.66f,0,size*.66f,size*.15f);
        line(c,0,size*.66f,size*.15f,size*.66f);
        c.restore();
    }
    @Override public void draw(Canvas c){
        Rect r=getBounds();if(r.width()<4||r.height()<4)return;
        float d=Resources.getSystem().getDisplayMetrics().density;
        float x=r.left+1*d,y=r.top+1*d,right=r.right-1*d,bottom=r.bottom-1*d;
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);
        c.drawRect(r,p);
        float corner=Math.min(18*d,Math.min(r.width(),r.height())*.32f);
        int outline=selected?0xffced7dc:0xff81878a;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(.8f,selected?1.1f*d:.75f*d));
        p.setColor(outline);
        // Very light perimeter and brighter selection rule.
        p.setAlpha(selected?180:70);c.drawRect(x,y,right,bottom,p);
        p.setColor(ink);p.setAlpha(selected?240:190);
        corner(c,x,y,1,1,corner);
        corner(c,right,y,-1,1,corner);
        corner(c,x,bottom,1,-1,corner);
        corner(c,right,bottom,-1,-1,corner);
        if(selected&&r.width()>95*d){
            p.setColor(0xffc3c9cc);p.setAlpha(152);
            line(c,x+corner+5*d,y+2*d,right-corner-5*d,y+2*d);
        }
        p.setStyle(Paint.Style.FILL);
    }
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}
    @SuppressWarnings("deprecation")@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
