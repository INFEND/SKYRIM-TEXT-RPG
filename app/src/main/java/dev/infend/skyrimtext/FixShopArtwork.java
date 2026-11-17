package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Locale;

/** Original procedural miniature illustrations, rendered at device resolution. */
final class FixShopArtwork extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int tint;private final String title,category,glyph;
    FixShopArtwork(Context context,String title,String glyph,int tint,String category){super(context);this.title=title;this.glyph=glyph;this.tint=tint;this.category=category;setContentDescription(title);}
    private int shade(int c,float f){return Color.rgb(Math.min(255,(int)(Color.red(c)*f)),Math.min(255,(int)(Color.green(c)*f)),Math.min(255,(int)(Color.blue(c)*f)));}
    private void fill(int c){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(c);p.setAlpha(255);}
    private void stroke(int c,float width){fill(c);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);}
    private Path path(float... a){Path q=new Path();q.moveTo(a[0],a[1]);for(int i=2;i<a.length;i+=2)q.lineTo(a[i],a[i+1]);q.close();return q;}
    private void poly(Canvas c,int color,float... a){fill(color);c.drawPath(path(a),p);}
    private void line(Canvas c,int color,float width,float x,float y,float xx,float yy){stroke(color,width);c.drawLine(x,y,xx,yy,p);}
    private void circle(Canvas c,int color,float x,float y,float r){fill(color);c.drawCircle(x,y,r,p);}
    private void box(Canvas c,int color,float l,float t,float r,float b,float round){fill(color);c.drawRoundRect(l,t,r,b,round,round,p);}
    private void gem(Canvas c,int col,float x,float y,float size){c.save();c.translate(x,y);c.scale(size/45,size/45);poly(c,shade(col,.48f),0,-45,29,-12,22,21,0,42,-22,21,-29,-12);poly(c,col,0,-45,0,0,-29,-12);poly(c,shade(col,1.5f),0,-45,29,-12,0,0);poly(c,shade(col,.8f),0,0,29,-12,22,21,0,42);poly(c,shade(col,.65f),0,0,0,42,-22,21,-29,-12);line(c,shade(col,1.6f),1,0,-45,0,42);c.restore();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();if(w<1||h<1)return;float scale=Math.min(w/180f,h/108f);float x=w/2,y=h/2;
        fill(0xff08090e);c.drawRect(0,0,w,h,p);
        p.setShader(new RadialGradient(x,y,Math.max(w,h)*.7f,new int[]{shade(tint,.55f),0xff13111c,0xff08090c},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        c.save();c.translate(x,y);c.scale(scale,scale);
        java.util.Random random=new java.util.Random(title.hashCode());for(int i=0;i<38;i++){int alpha=35+random.nextInt(100);circle(c,(alpha<<24)|(tint&0xffffff),random.nextFloat()*180-90,random.nextFloat()*108-54,.4f+random.nextFloat());}
        stroke((0x65<<24)|(tint&0xffffff),.8f);c.drawCircle(0,0,48,p);c.drawCircle(0,0,45,p);
        String n=title.toLowerCase(Locale.ROOT);
        if(n.contains("сердце")){heart(c);}else if(n.contains("ноша")||n.contains("хранилищ")){bag(c);}else if(n.contains("второй шанс")||n.contains("воскрес")||n.contains("исцелен")){for(int i=0;i<7;i++){c.save();c.rotate(i*360f/7);gem(c,tint,0,-22,25);c.restore();}gem(c,0xff9fe8ff,0,0,30);}
        else if(n.contains("судьб")||n.contains("предзнамен")){hourglass(c);}
        else if(category.equals("Снаряжение")){equipment(c,n);}
        else if(category.equals("Недвижимость")||n.contains("локаци")){castle(c);}
        else if(category.equals("Транспорт")){horse(c);}
        else if(category.equals("Питомцы")){paw(c);}
        else if(category.equals("Септимы")){for(int i=0;i<7;i++){float xx=(i%3-1)*22,yy=26-i*5;circle(c,0xff70441c,xx,yy,13);circle(c,0xffdeb765,xx,yy-2,12);stroke(0xffffe5a0,1);c.drawCircle(xx,yy-2,9,p);}}
        else if(category.equals("Персонажи")||category.equals("Внешность")){portrait(c);}
        else if(category.equals("Дополнения")||category.equals("Редактор мира")||category.equals("Коллекции")||category.equals("Навыки")){book(c);}
        else{gem(c,tint,0,0,36);for(int i=0;i<5;i++){c.save();c.rotate(i*72);line(c,shade(tint,1.4f),1.5f,0,-37,0,-45);c.restore();}}
        c.restore();
    }
    private void heart(Canvas c){Path h=new Path();h.moveTo(0,33);h.cubicTo(-12,17,-43,-5,-30,-24);h.cubicTo(-21,-39,-6,-30,0,-20);h.cubicTo(6,-30,21,-39,30,-24);h.cubicTo(43,-5,12,17,0,33);h.close();fill(0xffe0a342);p.setShader(new LinearGradient(-25,-30,22,35,0xfffff0b4,0xff9a4812,Shader.TileMode.CLAMP));c.drawPath(h,p);p.setShader(null);stroke(0xffffedaa,1.6f);c.drawPath(h,p);for(int i=0;i<2;i++){stroke(0xffdeb46a,3);c.drawArc(-47+i*5,-45+i*5,47-i*5,45-i*5,35,270,false,p);}poly(c,0xffffefb1,0,-42,-9,-17,0,-22,9,-17);}
    private void bag(Canvas c){box(c,0xff4b2d28,-31,-32,31,39,8);box(c,0xff855840,-28,-29,28,33,6);box(c,0xff593b33,-30,-33,30,-3,8);for(int i:new int[]{-19,19}){box(c,0xff302329,i-4,-31,i+4,33,3);box(c,0xffd8b268,i-6,2,i+6,13,2);box(c,0xff402826,i-3,5,i+3,10,1);}gem(c,0xffc182ff,0,-13,12);gem(c,0xffc182ff,0,23,8);}
    private void hourglass(Canvas c){box(c,0xffd4a55a,-25,-40,25,-32,3);box(c,0xffd4a55a,-25,32,25,40,3);poly(c,0xff36505a,-19,-31,19,-31,13,-12,0,0,13,12,19,31,-19,31,-13,12,0,0,-13,-12);poly(c,0xfff8c976,-15,-25,15,-25,0,-3);poly(c,0xffe3a447,0,8,16,29,-16,29);line(c,0xffffe1a0,1,0,0,0,16);for(int i=0;i<3;i++){stroke(0xffc49a5b,1);c.drawArc(-43-i*3,-30-i*3,43+i*3,30+i*3,i*45,180,false,p);}}
    private void equipment(Canvas c,String n){if(n.contains("брон")||n.contains("кирас")||n.contains("доспех")||n.contains("шлем")||n.contains("сапог")||n.contains("перчат")){poly(c,shade(tint,.65f),-35,-24,-14,-38,0,-27,14,-38,35,-24,25,1,20,-6,20,37,-20,37,-20,-6,-25,1);poly(c,tint,-17,-15,0,-25,17,-15,13,21,0,31,-13,21);line(c,0xffddd5c8,1,0,-25,0,30);}
        else if(n.contains("лук")){stroke(tint,4);c.drawArc(-29,-42,29,42,-90,180,false,p);line(c,0xffe8d8b4,1.3f,0,-42,0,42);line(c,tint,2,-32,0,37,0);poly(c,tint,37,0,27,-5,27,5);}
        else if(n.contains("кольц")||n.contains("амулет")||n.contains("ожерел")){stroke(0xffd8b46b,6);c.drawCircle(0,7,23,p);gem(c,tint,0,-22,17);}
        else {c.save();c.rotate(34);poly(c,0xffe8e5e1,0,-47,8,-31,7,21,-7,21,-8,-31);poly(c,shade(tint,.6f),0,-47,0,21,7,21,8,-31);box(c,0xffb68a4e,-19,20,19,26,2);box(c,0xff523a2c,-4,26,4,40,2);gem(c,tint,0,43,6);c.restore();}}
    private void castle(Canvas c){poly(c,0xff39424b,-81,36,-52,-5,-27,28,9,-17,44,29,72,-5,90,36);box(c,0xff7c7370,-32,-12,32,37,1);for(int i:new int[]{-32,32}){box(c,0xff9b9388,i-10,-31,i+10,37,1);poly(c,0xff45434a,i-14,-29,i,-45,i+14,-29);box(c,0xffe8b66c,i-2,-15,i+2,-5,1);}poly(c,0xff514b4e,-34,-10,0,-33,34,-10);box(c,0xff302e33,-7,12,7,37,5);box(c,0xffe5a952,-20,1,-15,9,1);box(c,0xffe5a952,15,1,20,9,1);}
    private void horse(Canvas c){Path h=new Path();h.moveTo(-39,35);h.lineTo(-29,5);h.lineTo(-8,-17);h.lineTo(3,-38);h.lineTo(11,-26);h.lineTo(26,-39);h.lineTo(27,-14);h.lineTo(41,2);h.lineTo(29,15);h.lineTo(8,9);h.lineTo(-2,39);h.close();fill(shade(tint,.75f));c.drawPath(h,p);stroke(shade(tint,1.5f),1.5f);c.drawPath(h,p);circle(c,0xfffce4a8,20,-12,2.8f);for(int i=0;i<4;i++)line(c,shade(tint,.5f),2,-22+i*3,12-i*5,-35+i*3,32-i*5);}
    private void paw(Canvas c){for(int i=0;i<4;i++){float xx=-27+i*18,yy=-20+(i==0||i==3?9:0);fill(shade(tint,1.2f));c.drawOval(xx-8,yy-13,xx+8,yy+13,p);}Path q=new Path();q.moveTo(0,-4);q.cubicTo(-12,-4,-35,22,-20,31);q.cubicTo(-12,38,-6,28,0,28);q.cubicTo(6,28,12,38,20,31);q.cubicTo(35,22,12,-4,0,-4);fill(tint);c.drawPath(q,p);}
    private void portrait(Canvas c){poly(c,0xff323343,-42,42,-24,16,0,6,24,16,42,42);Path hood=new Path();hood.moveTo(-32,11);hood.quadTo(-29,-44,0,-42);hood.quadTo(29,-44,32,11);hood.lineTo(18,28);hood.lineTo(-18,28);hood.close();fill(shade(tint,.6f));c.drawPath(hood,p);fill(0xffae8e73);c.drawOval(-14,-25,14,11,p);poly(c,0xff423a39,-15,-10,0,4,15,-10,10,22,-10,22);line(c,0xffe6d2a0,1,-8,-10,-3,-10);line(c,0xffe6d2a0,1,3,-10,8,-10);gem(c,tint,0,33,8);}
    private void book(Canvas c){poly(c,0xff553a2d,-42,-30,-3,-22,0,-19,3,-22,42,-30,42,30,0,38,-42,30);poly(c,0xffd2bd8f,-39,-26,-4,-18,-4,32,-39,24);poly(c,0xffebd6a5,4,-18,39,-26,39,24,4,32);for(int i=0;i<5;i++){line(c,0xff9a805b,1,-34,-14+i*7,-9,-9+i*7);line(c,0xff9a805b,1,9,-9+i*7,34,-14+i*7);}gem(c,tint,0,0,12);}
}
