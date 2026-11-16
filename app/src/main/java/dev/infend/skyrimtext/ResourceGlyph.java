package dev.infend.skyrimtext;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Crisp, uniform 24-unit outline icons, independent of Android theme assets. */
final class ResourceGlyph extends Drawable {
    private final String key;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color,alpha=255;
    ResourceGlyph(String key,int color){this.key=key;this.color=color;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.65f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
    @Override public int getIntrinsicWidth(){return 24;}
    @Override public int getIntrinsicHeight(){return 24;}
    @Override public void setAlpha(int a){alpha=a;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    private void line(Canvas c,float... a){Path path=new Path();path.moveTo(a[0],a[1]);for(int i=2;i<a.length;i+=2)path.lineTo(a[i],a[i+1]);c.drawPath(path,p);}
    private void rect(Canvas c,float x,float y,float r,float b,float radius){c.drawRoundRect(x,y,r,b,radius,radius,p);}
    private void circle(Canvas c,float x,float y,float r){c.drawCircle(x,y,r,p);}
    @Override public void draw(Canvas c){
        Rect b=getBounds();if(b.isEmpty())return;
        int save=c.save();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);p.setColor(color);p.setAlpha(alpha);
        switch(key){
            case "gallery":
                rect(c,3,6,18,21,2);line(c,7,3,19,3,21,5,21,17);circle(c,8,11,1.5f);line(c,4,18,9,14,12,17,15,13,18,16);break;
            case "portraits":
                circle(c,9,7,3);circle(c,17,8,2.5f);c.drawArc(3,13,15,23,180,180,false,p);c.drawArc(13,13,22,22,195,155,false,p);break;
            case "map":
                line(c,3,5,9,3,15,6,21,4,21,19,15,21,9,18,3,20,3,5);line(c,9,3,9,18);line(c,15,6,15,21);break;
            case "icons":
                line(c,12,3,16,9,8,9,12,3);rect(c,3,13,10,20,1.5f);circle(c,17.5f,16.5f,3.5f);break;
            case "font":
                line(c,4,7,4,4,20,4,20,7);line(c,12,4,12,20);line(c,8,20,16,20);break;
            case "engine":
                rect(c,6,6,18,18,2);rect(c,9,9,15,15,1);
                for(int k=8;k<=16;k+=4){line(c,k,3,k,6);line(c,k,18,k,21);line(c,3,k,6,k);line(c,18,k,21,k);}break;
            case "gguf":
                line(c,12,4,7,3,4,7,4,11,2.5f,14,5,19,9,21,12,19,15,21,19,19,21.5f,14,20,11,20,7,17,3,12,4,12,19);
                line(c,7,7,7,10,9,12);line(c,17,7,17,10,15,12);line(c,7,15,10,15);line(c,14,15,17,15);break;
            case "check":line(c,5,12,10,17,19,7);break;
            case "close":line(c,6,6,18,18);line(c,18,6,6,18);break;
            case "clock":circle(c,12,12,8);line(c,12,7,12,12,15,14);break;
            case "install":rect(c,4,5,20,20,2);line(c,12,8,12,16);line(c,8,12,12,16,16,12);break;
            case "download":line(c,12,3,12,15);line(c,7,10,12,15,17,10);line(c,4,16,4,20,20,20,20,16);break;
            default:circle(c,12,12,8);line(c,8,12,16,12);line(c,12,8,12,16);break;
        }
        c.restoreToCount(save);
    }
}
