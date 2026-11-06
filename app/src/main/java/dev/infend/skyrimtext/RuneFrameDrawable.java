package dev.infend.skyrimtext;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Chamfered fantasy frame inspired by engraved metalwork, scales at all screen widths. */
public final class RuneFrameDrawable extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent,fill;
    private final boolean highlighted;
    public RuneFrameDrawable(int fillColor,int accentColor,boolean active){
        fill=fillColor;accent=accentColor;highlighted=active;
    }
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();float l=b.left+2,t=b.top+2,r=b.right-2,bt=b.bottom-2;
        float notch=Math.min(15f,(bt-t)*.29f);
        Path shape=new Path();shape.moveTo(l+notch,t);shape.lineTo(r-notch,t);shape.lineTo(r,t+(bt-t)/2);
        shape.lineTo(r-notch,bt);shape.lineTo(l+notch,bt);shape.lineTo(l,t+(bt-t)/2);shape.close();
        paint.setStyle(Paint.Style.FILL);paint.setColor(fill);canvas.drawPath(shape,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(highlighted?2.2f:1.35f);
        paint.setShader(new LinearGradient(l,t,r,bt,new int[]{
            highlighted?0xfff0dda9:0xff706553,accent,highlighted?0xfff0dda9:0xff706553
        },null,Shader.TileMode.CLAMP));
        canvas.drawPath(shape,paint);paint.setShader(null);
        if(highlighted){
            paint.setColor(0x66e4c994);paint.setStrokeWidth(.6f);
            canvas.drawLine(l+notch+7,t+4,r-notch-7,t+4,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @SuppressWarnings("deprecation") @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
