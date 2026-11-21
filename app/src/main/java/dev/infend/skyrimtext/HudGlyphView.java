package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Lightweight vector HUD icons, independent of optional downloaded artwork and fonts. */
final class HudGlyphView extends View {
    private final String glyph;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    HudGlyphView(Context context,String glyph,int color){
        super(context);this.glyph=glyph;p.setColor(color);p.setStrokeWidth(1.7f);
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,p);}
    private void path(Canvas c,float... pts){
        Path a=new Path();a.moveTo(pts[0],pts[1]);
        for(int i=2;i<pts.length;i+=2)a.lineTo(pts[i],pts[i+1]);c.drawPath(a,p);
    }
    private void sun(Canvas c,float x,float y,float radius){
        c.drawCircle(x,y,radius,p);
        for(int i=0;i<8;i++){double a=i*Math.PI/4;float dx=(float)Math.cos(a),dy=(float)Math.sin(a);
            line(c,x+dx*(radius+2),y+dy*(radius+2),x+dx*(radius+4),y+dy*(radius+4));}
    }
    private void cloud(Canvas c){
        Path a=new Path();a.moveTo(5,15);a.cubicTo(0,15,0,9,6,9);a.cubicTo(6,2,17,2,18,9);
        a.cubicTo(24,8,25,15,19,15);a.close();c.drawPath(a,p);
    }
    private void snow(Canvas c,float x,float y,float r){
        for(int i=0;i<3;i++){double a=i*Math.PI/3;float dx=(float)Math.cos(a)*r,dy=(float)Math.sin(a)*r;
            line(c,x-dx,y-dy,x+dx,y+dy);}
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float s=Math.min(getWidth(),getHeight())/24f;c.save();
        c.translate((getWidth()-24*s)/2,(getHeight()-24*s)/2);c.scale(s,s);
        switch(glyph){
            case "health":{Path h=new Path();h.moveTo(12,21);h.cubicTo(9,18,1,13,2,7);h.cubicTo(3,2,9,2,12,7);h.cubicTo(15,2,21,2,22,7);h.cubicTo(23,13,15,19,12,21);h.close();c.drawPath(h,p);break;}
            case "stamina":path(c,14,2,5,13,11,13,10,22,20,10,13,10,14,2);break;
            case "magic":case "experience":path(c,12,2,15,9,22,12,15,15,12,22,9,15,2,12,9,9,12,2);break;
            case "level":path(c,4,11,12,3,20,11);path(c,4,20,12,12,20,20);break;
            case "bag":c.drawRoundRect(4,7,20,22,3,3,p);c.drawArc(8,1,16,13,180,180,false,p);line(c,5,13,19,13);break;
            case "septims":c.drawCircle(12,12,9,p);c.drawCircle(12,12,5,p);line(c,12,8,12,16);break;
            case "temperature":c.drawCircle(12,18,4,p);path(c,9,15,9,5,10,2,14,2,15,5,15,15);line(c,12,8,12,18);break;
            case "moon":{Path m=new Path();m.moveTo(15,2);m.cubicTo(3,1,0,14,8,20);m.cubicTo(13,24,20,20,22,15);m.cubicTo(14,17,8,9,15,2);c.drawPath(m,p);break;}
            case "sun":sun(c,12,12,5);break;
            case "sunrise":case "sunset":c.drawArc(6,7,18,19,180,180,false,p);line(c,2,14,22,14);line(c,5,19,19,19);line(c,12,2,12,5);line(c,3,5,5,7);line(c,21,5,19,7);break;
            case "cloud":cloud(c);break;
            case "rain":cloud(c);for(int x=6;x<=18;x+=6)line(c,x,18,x-2,22);break;
            case "storm":cloud(c);path(c,13,15,9,20,13,20,10,23);break;
            case "snow":case "blizzard":cloud(c);snow(c,7,20,2.5f);snow(c,16,20,2.5f);break;
            case "fog":cloud(c);line(c,3,18,21,18);line(c,5,21,19,21);break;
            case "cave":path(c,2,21,2,15,8,4,16,4,22,15,22,21,2,21);path(c,8,21,8,15,12,11,16,15,16,21);break;
            default:c.drawCircle(12,12,8,p);
        }
        c.restore();
    }
}
