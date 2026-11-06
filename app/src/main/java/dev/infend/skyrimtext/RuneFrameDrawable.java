package dev.infend.skyrimtext;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Straight-sided Nordic frame: subtly heavier engraving, no hexagonal side cuts. */
public final class RuneFrameDrawable extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent,fill;
    private final boolean highlighted;
    public RuneFrameDrawable(int fillColor,int accentColor,boolean active){
        fill=fillColor;accent=accentColor;highlighted=active;
    }
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();
        float density=Resources.getSystem().getDisplayMetrics().density;
        float stroke=(highlighted?1.95f:1.55f)*density;
        float inset=stroke/2f+1f;
        float left=b.left+inset, top=b.top+inset, right=b.right-inset, bottom=b.bottom-inset;
        if(right<=left||bottom<=top)return;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setColor(fill);
        canvas.drawRect(left,top,right,bottom,paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setShader(new LinearGradient(left,top,right,bottom,new int[]{
            highlighted?0xfff0dda9:0xff98876d,accent,highlighted?0xfff0dda9:0xff98876d
        },null,Shader.TileMode.CLAMP));
        canvas.drawRect(left,top,right,bottom,paint);
        paint.setShader(null);
        if(highlighted && right-left>20*density){
            paint.setColor(0x66e4c994);paint.setStrokeWidth(.65f*density);
            float margin=8*density;
            canvas.drawLine(left+margin,top+3.5f*density,right-margin,top+3.5f*density,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @SuppressWarnings("deprecation") @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
