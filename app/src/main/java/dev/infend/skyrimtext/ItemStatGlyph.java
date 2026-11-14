package dev.infend.skyrimtext;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
/** Reference item-card line symbols. Colors are applied to the actual strokes. */
final class ItemStatGlyph extends View {
    private final String kind;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    ItemStatGlyph(Context c,String kind,int color){
        super(c);this.kind=kind;p.setColor(color);p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.65f);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private void path(Canvas c,float... xy){Path q=new Path();q.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)q.lineTo(xy[i],xy[i+1]);c.drawPath(q,p);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float scale=Math.min(getWidth(),getHeight())/24f;c.save();c.translate((getWidth()-24*scale)/2,(getHeight()-24*scale)/2);c.scale(scale,scale);
        switch(kind){
            case "weapon":path(c,3,3,3,8,15,20,19,16,7,4,3,3);path(c,12,20,20,12);path(c,17,18,22,23);break;
            case "shield":{Path q=new Path();q.moveTo(12,2);q.lineTo(21,6);q.lineTo(21,12);q.cubicTo(21,17,17,20,12,23);q.cubicTo(7,20,3,17,3,12);q.lineTo(3,6);q.close();c.drawPath(q,p);break;}
            case "bag":path(c,7,8,17,8,21,21,3,21,7,8);c.drawCircle(12,5,3,p);break;
            case "septims":c.drawCircle(9,9,6,p);c.drawArc(10,10,23,23,-92,270,false,p);path(c,9,6,9,12);break;
            case "health":{Path q=new Path();q.moveTo(12,21);q.cubicTo(9,18,2,13,2,8);q.cubicTo(2,2,9,1,12,7);q.cubicTo(15,1,22,2,22,8);q.cubicTo(22,13,15,18,12,21);q.close();c.drawPath(q,p);break;}
            case "magic":path(c,12,2,15,9,22,12,15,15,12,22,9,15,2,12,9,9,12,2);break;
            case "fire":{Path q=new Path();q.moveTo(12,2);q.cubicTo(19,10,22,14,18,20);q.cubicTo(11,26,3,20,5,13);q.cubicTo(6,10,9,10,9,7);q.cubicTo(11,11,13,10,12,2);q.close();c.drawPath(q,p);break;}
            case "frost":{
                for(int i=0;i<6;i++){double a=i*Math.PI/3;float x=(float)Math.cos(a),y=(float)Math.sin(a);
                    path(c,12,12,12+10*x,12+10*y);
                    float bx=12+6*x,by=12+6*y;
                    path(c,bx-3*x+3*y,by-3*y-3*x,bx,by,bx-3*x-3*y,by-3*y+3*x);}
                break;}
            case "shock":path(c,13,2,4,14,11,14,10,23,20,10,13,10,13,2);break;
            case "poison":{path(c,9,3,15,3,15,7,19,16,19,20,5,20,5,16,9,7,9,3);path(c,7,15,17,15);c.drawCircle(12,17,1,p);break;}
            case "sun":{c.drawCircle(12,12,5,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;float x=(float)Math.cos(a),y=(float)Math.sin(a);path(c,12+8*x,12+8*y,12+11*x,12+11*y);}break;}
            case "stamina":path(c,4,16,10,6,10,12,20,12,14,22,14,16,4,16);break;
            case "soul":path(c,12,2,20,9,18,18,12,23,6,18,4,9,12,2);path(c,4,9,12,12,20,9);path(c,12,12,12,23);break;
            case "absorb_health":case "absorb_magic":case "absorb_stamina":
                c.drawArc(4,4,20,20,35,285,false,p);path(c,17,3,20,8,15,9);
                if("absorb_health".equals(kind)){Path q=new Path();q.moveTo(12,16);q.cubicTo(4,10,9,7,12,10);q.cubicTo(15,7,20,10,12,16);c.drawPath(q,p);}
                else if("absorb_magic".equals(kind))path(c,12,8,16,12,12,16,8,12,12,8);
                else path(c,13,7,9,13,13,13,11,17);break;
            default:c.drawCircle(12,12,9,p);path(c,12,11,12,17);c.drawCircle(12,7,.5f,p);
        }c.restore();
    }
}
