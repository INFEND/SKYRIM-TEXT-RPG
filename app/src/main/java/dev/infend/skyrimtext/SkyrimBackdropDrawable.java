package dev.infend.skyrimtext;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Atmospheric, fully local background. Inky blurred-looking silhouettes
 * behind translucent menus, without embedding screenshot UI or large bitmaps.
 */
public final class SkyrimBackdropDrawable extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    @Override public void draw(Canvas original){
        if(getBounds().width()<10||getBounds().height()<10)return;
        Canvas c=original;c.save();
        c.translate(getBounds().left,getBounds().top);
        c.scale(getBounds().width()/400f,getBounds().height()/900f);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0,0,110,900,
                new int[]{0xff090c0e,0xff101718,0xff090c0c,0xff040606},
                new float[]{0,.36f,.65f,1},Shader.TileMode.CLAMP));
        c.drawRect(0,0,400,900,p);p.setShader(null);
        ridge(c,0xff13191b,420,125,38);
        ridge(c,0xff111618,570,96,27);
        ridge(c,0xff0a0e0f,750,80,19);
        for(int j=0;j<21;j++){
            float x=(j*83+29)%450-24;
            float base=500+(j*67)%370;
            float height=48+(j*31)%70;
            p.setColor(j%3==0?0x231b2728:0x301a2527);
            Path tree=new Path();
            tree.moveTo(x,base-height);tree.lineTo(x-13,base-height*.48f);
            tree.lineTo(x-6,base-height*.48f);tree.lineTo(x-21,base-5);
            tree.lineTo(x+21,base-5);tree.lineTo(x+6,base-height*.48f);
            tree.lineTo(x+13,base-height*.48f);tree.close();
            c.drawPath(tree,p);
        }
        p.setShader(new LinearGradient(0,0,0,900,new int[]{
            0x66020405,0x48020304,0x77020304,0xdd020304
        },null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,400,900,p);p.setShader(null);c.restore();
    }
    private void ridge(Canvas c,int color,float base,float peak,int seed){
        Path path=new Path();path.moveTo(0,900);path.lineTo(0,base);
        for(int i=0;i<=16;i++){
            float x=i*27;
            float y=base-((i*19+seed)%7)*peak/7f-(i%5==1?peak*.64f:0);
            path.lineTo(x,y);
        }
        path.lineTo(420,900);path.close();p.setColor(color);c.drawPath(path,p);
    }
    @Override public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}
    @SuppressWarnings("deprecation")@Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
