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
            case "stamina":path(c,2,7,19,7,19,17,2,17,2,7);path(c,22,10,22,14);path(c,5,10,5,14);break;
            case "soul":path(c,12,2,20,9,18,18,12,23,6,18,4,9,12,2);path(c,4,9,12,12,20,9);path(c,12,12,12,23);break;
            case "absorb_health":case "absorb_magic":case "absorb_stamina":
                c.drawArc(4,4,20,20,35,285,false,p);path(c,17,3,20,8,15,9);
                if("absorb_health".equals(kind)){Path q=new Path();q.moveTo(12,16);q.cubicTo(4,10,9,7,12,10);q.cubicTo(15,7,20,10,12,16);c.drawPath(q,p);}
                else if("absorb_magic".equals(kind))path(c,12,8,16,12,12,16,8,12,12,8);
                else path(c,13,7,9,13,13,13,11,17);break;
            case "mana":case "blood":{Path q=new Path();q.moveTo(12,2);q.cubicTo(8,8,4,12,4,16);q.cubicTo(4,26,20,26,20,16);q.cubicTo(20,12,16,8,12,2);c.drawPath(q,p);break;}
            case "chaos":c.drawCircle(12,12,3,p);c.drawArc(3,3,21,21,30,290,false,p);c.drawCircle(20,5,2,p);c.drawCircle(4,18,2,p);break;
            case "lock":path(c,4,10,20,10,20,22,4,22,4,10);c.drawArc(7,2,17,14,180,180,false,p);path(c,12,15,12,18);break;
            case "hourglass":path(c,5,2,19,2,17,7,7,17,5,22,19,22,17,17,7,7,5,2);break;
            case "ghost":{Path q=new Path();q.moveTo(4,21);q.lineTo(4,9);q.cubicTo(4,0,20,0,20,9);q.lineTo(20,21);q.lineTo(16,17);q.lineTo(12,21);q.lineTo(8,17);q.close();c.drawPath(q,p);c.drawCircle(9,10,1,p);c.drawCircle(15,10,1,p);break;}
            case "brain":c.drawOval(3,4,13,20,p);c.drawOval(11,4,21,20,p);path(c,12,4,12,20);break;
            case "hand":path(c,6,12,6,5,9,5,9,12,9,2,12,2,12,12,15,4,18,4,18,13,21,10,23,12,19,21,10,22,3,14,3,11,6,12);break;
            case "hammer":path(c,3,5,8,1,18,10,14,14,10,10,4,22,1,19,8,8,3,5);break;
            case "target":c.drawCircle(12,12,9,p);c.drawCircle(12,12,5,p);c.drawCircle(12,12,1,p);break;
            case "eye":case "eye_off":{Path q=new Path();q.moveTo(2,12);q.quadTo(12,-2,22,12);q.quadTo(12,26,2,12);c.drawPath(q,p);c.drawCircle(12,12,3,p);if(kind.equals("eye_off"))path(c,3,3,21,21);break;}
            case "footprints":c.drawOval(4,2,10,12,p);c.drawOval(14,10,20,20,p);c.drawCircle(7,15,2,p);c.drawCircle(17,23,1,p);break;
            case "speech":path(c,3,3,21,3,21,16,11,16,5,21,5,16,3,16,3,3);path(c,7,8,17,8);path(c,7,12,15,12);break;
            case "waves":case "wind":for(int i=0;i<3;i++){Path q=new Path();q.moveTo(2,5+i*6);q.quadTo(7,0+i*6,12,5+i*6);q.quadTo(17,10+i*6,22,5+i*6);c.drawPath(q,p);}break;
            case "moon":{Path q=new Path();q.moveTo(12,2);q.cubicTo(-1,2,-1,23,13,22);q.cubicTo(18,22,22,18,22,13);q.cubicTo(9,19,5,8,12,2);c.drawPath(q,p);break;}
            case "skull":c.drawCircle(12,10,8,p);path(c,8,17,8,22,16,22,16,17);c.drawCircle(9,10,1.5f,p);c.drawCircle(15,10,1.5f,p);break;
            case "ban":c.drawCircle(12,12,9,p);path(c,5,5,19,19);break;
            case "key":c.drawCircle(7,7,5,p);path(c,11,11,22,22,19,22,19,18,15,18);break;
            case "layers":path(c,2,8,12,2,22,8,12,14,2,8);path(c,2,13,12,19,22,13);path(c,2,17,12,23,22,17);break;
            case "shirt":path(c,8,3,3,6,1,12,6,15,7,12,7,22,17,22,17,12,18,15,23,12,21,6,16,3,12,7,8,3);break;
            case "star":{Path q=new Path();for(int i=0;i<10;i++){double a=i*Math.PI/5-Math.PI/2;float r=i%2==0?10:4.5f;float x=12+r*(float)Math.cos(a),y=12+r*(float)Math.sin(a);if(i==0)q.moveTo(x,y);else q.lineTo(x,y);}q.close();c.drawPath(q,p);break;}
            case "compare":path(c,3,7,21,7,17,3);path(c,21,17,3,17,7,21);break;
            case "more":for(int i=0;i<3;i++)c.drawCircle(5+i*7,12,1,p);break;
            default:c.drawCircle(12,12,9,p);path(c,12,11,12,17);c.drawCircle(12,7,.5f,p);
        }c.restore();
    }
}
