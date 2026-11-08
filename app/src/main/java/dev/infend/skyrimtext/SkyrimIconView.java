package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Native monochrome icons drawn entirely on transparent pixels.
 * Never cut square black backgrounds out of the reference screenshots.
 */
public final class SkyrimIconView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String glyph;
    private final int tint;
    public SkyrimIconView(Context context,String glyph,int tint){
        super(context);this.glyph=glyph==null?"star":glyph;this.tint=tint;
        setBackgroundColor(android.graphics.Color.TRANSPARENT);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        setContentDescription("Значок: "+this.glyph);
    }
    @Override protected void onMeasure(int width,int height){
        int size=(int)(30*getResources().getDisplayMetrics().density+.5f);
        setMeasuredDimension(resolveSize(size,width),resolveSize(size,height));
    }
    private void l(Canvas c,float x,float y,float a,float b){c.drawLine(x,y,a,b,paint);}
    private void path(Canvas c,float... xy){
        Path p=new Path();if(xy.length<4)return;p.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i],xy[i+1]);c.drawPath(p,paint);
    }
    private void diamond(Canvas c,float x,float y,float r){
        path(c,x,y-r,x+r,y,x,y+r,x-r,y,x,y-r);
    }
    private void swords(Canvas c){
        path(c,3,2,16,15,18,19,20,20,21,19,19,17,6,4,3,2);
        path(c,21,2,8,15,6,19,4,20,3,19,5,17,18,4,21,2);
        l(c,4,17,8,21);l(c,16,21,20,17);
        l(c,4,7,8,3);l(c,16,3,20,7);
    }
    private void shield(Canvas c){
        Path p=new Path();p.moveTo(12,2);p.cubicTo(7,4,4,4,3,4);
        p.lineTo(3,12);p.cubicTo(3,17,8,21,12,23);
        p.cubicTo(16,21,21,17,21,12);p.lineTo(21,4);
        p.cubicTo(17,4,15,4,12,2);p.close();c.drawPath(p,paint);
        path(c,12,5,12,19);diamond(c,12,11,4);
    }
    private void book(Canvas c){
        path(c,12,6,6,4,2,5,2,20,7,19,12,21,17,19,22,20,22,5,18,4,12,6);
        l(c,12,6,12,21);l(c,4,9,10,10);l(c,14,10,20,9);
    }
    private void scroll(Canvas c){
        path(c,7,3,17,3,20,6,20,19,17,21,7,21,4,18,4,6,7,3);
        c.drawCircle(7,6,2,paint);c.drawCircle(17,19,2,paint);
        l(c,9,9,17,9);l(c,9,12,17,12);l(c,9,15,15,15);
    }
    private void compass(Canvas c){
        c.drawCircle(12,12,9,paint);diamond(c,12,12,5);
        l(c,12,0,12,5);l(c,12,19,12,24);l(c,0,12,5,12);l(c,19,12,24,12);
        path(c,12,7,14,12,12,17,10,12,12,7);
    }
    private void satchel(Canvas c){
        path(c,5,8,19,8,21,21,3,21,5,8);
        path(c,7,8,7,5,10,3,14,3,17,5,17,8);
        path(c,5,11,12,15,19,11);diamond(c,12,14,1.8f);
    }
    private void potion(Canvas c){
        path(c,9,3,15,3,15,7,18,11,20,20,17,22,7,22,4,20,6,11,9,7,9,3);
        l(c,8,3,16,3);l(c,7,16,17,16);
        c.drawCircle(10,12,1,paint);c.drawCircle(14,18,1,paint);
    }
    private void hero(Canvas c){
        c.drawCircle(12,6,3,paint);
        path(c,7,21,6,16,8,11,12,12,16,11,18,16,17,21,7,21);
        l(c,8,16,16,16);diamond(c,12,16,2);
    }
    private void gear(Canvas c){
        c.drawCircle(12,12,8,paint);c.drawCircle(12,12,3,paint);
        for(int i=0;i<8;i++){double a=i*Math.PI/4;l(c,12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,
                12+(float)Math.cos(a)*11,12+(float)Math.sin(a)*11);}
    }
    private void dragon(Canvas c){
        path(c,2,17,6,11,11,8,14,3,19,4,22,8,18,9,21,16,16,13,13,17,9,13,4,19);
        path(c,9,8,5,4,3,5,3,9);l(c,16,7,18,10);
        diamond(c,17,8,1);
    }
    private void star(Canvas c){
        path(c,12,2,15,9,22,12,15,15,12,22,9,15,2,12,9,9,12,2);
    }
    @Override protected void onDraw(Canvas original){
        super.onDraw(original);
        if(getWidth()==0||getHeight()==0)return;
        Canvas c=original;c.save();
        float size=Math.min(getWidth(),getHeight());
        c.translate((getWidth()-size)/2f,(getHeight()-size)/2f);c.scale(size/24f,size/24f);
        paint.setColor(tint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.35f);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        switch(glyph){
            case "story":case "weapon":swords(c);break;
            case "map":compass(c);break;
            case "bag":satchel(c);break;
            case "codex":case "book":book(c);break;
            case "hero":hero(c);break;
            case "journal":scroll(c);break;
            case "settings":gear(c);break;
            case "armor":shield(c);break;
            case "potion":potion(c);break;
            case "dragon":dragon(c);break;
            default:star(c);break;
        }
        paint.setStyle(Paint.Style.FILL);c.restore();
    }
}
