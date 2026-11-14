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
            case "stamina":path(c,14,2,5,13,11,13,9,22,19,10,13,10,14,2);break;
            case "magic":path(c,12,2,15,9,22,12,15,15,12,22,9,15,2,12,9,9,12,2);break;
            case "fire":{Path q=new Path();q.moveTo(12,2);q.cubicTo(19,10,22,14,18,20);q.cubicTo(11,26,3,20,5,13);q.cubicTo(6,10,9,10,9,7);q.cubicTo(11,11,13,10,12,2);q.close();c.drawPath(q,p);break;}
            default:c.drawCircle(12,12,9,p);path(c,12,11,12,17);c.drawCircle(12,7,.5f,p);
        }c.restore();
    }
}
