package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Low-cost local vector effects; no additional images or network resources. */
final class VisualEffectsView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String weather;private final boolean animate;
    VisualEffectsView(Context c,String weather,boolean animate){super(c);this.weather=weather;this.animate=animate;setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        float t=animate?(android.os.SystemClock.uptimeMillis()%12000L)/12000f:0;
        if("fog".equals(weather)){
            for(int i=0;i<4;i++){float y=(i*.25f+.12f)*h;float x=w*(.5f+.15f*(float)Math.sin(t*Math.PI*2+i));p.setColor(0x18d6e4e9);c.drawOval(x-w*.75f,y-h*.08f,x+w*.75f,y+h*.08f,p);}
        }else{
            boolean rain="rain".equals(weather)||"storm".equals(weather);
            int count="blizzard".equals(weather)?46:28;
            p.setStrokeWidth(Math.max(1f,getResources().getDisplayMetrics().density));
            for(int i=0;i<count;i++){
                float x=((i*.6180339f+t*(rain?.15f:.22f))%1f)*w;
                float y=((i*.381966f+t*(rain?4f:1f))%1f)*h;
                p.setColor(rain?0x66b8d4e3:0xaacfe2ef);
                if(rain)c.drawLine(x,y,x-4,y+13,p);else c.drawCircle(x,y,1.4f+(i%3)*.6f,p);
            }
        }
        if(animate&&isShown())postInvalidateDelayed(40);
    }
    static final class EdgeDrawable extends Drawable {
        private final Paint p=new Paint();private final int color;
        EdgeDrawable(int color){this.color=color;}
        @Override public void draw(Canvas c){
            Rect b=getBounds();float w=b.width(),h=b.height();if(w<=0||h<=0)return;
            p.setShader(new LinearGradient(0,0,w*.14f,0,color,Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawRect(0,0,w*.14f,h,p);
            p.setShader(new LinearGradient(w,0,w*.86f,0,color,Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawRect(w*.86f,0,w,h,p);p.setShader(null);
        }
        @Override public void setAlpha(int a){p.setAlpha(a);}
        @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    static final class HeroSilhouette extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int color;
        HeroSilhouette(Context c,int color){super(c);this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();c.save();c.scale(w/80f,h/172f);
            p.setColor(0xff273036);p.setStyle(Paint.Style.FILL);c.drawCircle(40,20,12,p);
            Path body=new Path();body.moveTo(22,39);body.lineTo(58,39);body.lineTo(73,87);body.lineTo(61,92);body.lineTo(51,65);body.lineTo(52,99);body.lineTo(62,160);body.lineTo(46,160);body.lineTo(40,111);body.lineTo(34,160);body.lineTo(18,160);body.lineTo(28,99);body.lineTo(29,65);body.lineTo(19,92);body.lineTo(7,87);body.close();c.drawPath(body,p);
            p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);c.drawPath(body,p);c.drawCircle(40,20,12,p);p.setStyle(Paint.Style.FILL);c.restore();
        }
    }
    static final class SkillStars extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int level,color;
        SkillStars(Context c,int level,int color){super(c);this.level=level;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();float[] xs={.15f,.31f,.5f,.69f,.85f},ys={.65f,.31f,.53f,.18f,.61f};
            p.setStrokeWidth(1.4f);p.setColor(0xff50565c);
            for(int i=1;i<5;i++)c.drawLine(xs[i-1]*w,ys[i-1]*h,xs[i]*w,ys[i]*h,p);
            for(int i=0;i<5;i++){p.setColor(level>=(i+1)*20?color:0xff58616a);float x=xs[i]*w,y=ys[i]*h;c.drawCircle(x,y,3.5f,p);}
        }
    }
}
