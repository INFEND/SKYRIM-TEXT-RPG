package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import java.util.Random;

/** Deterministic lightweight fantasy scenery, drawn offline with Canvas. No photo downloads. */
public final class NordicArtView extends View {
    public static final int TITLE=0, STORY=1, CODEX=2;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mode;
    private static final int GOLD=0xffd5bd89, INK=0xff090e12;
    public NordicArtView(Context context,int scene){super(context);mode=scene;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    @Override protected void onDraw(Canvas actual){
        super.onDraw(actual);
        int w=getWidth(),h=getHeight();if(w==0||h==0)return;
        Canvas c=actual;c.save();c.scale(w/1000f,h/550f);
        p.setShader(new LinearGradient(0,0,0,550,new int[]{0xff070b0e,0xff1a252d,0xff121b22,INK},null,Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);c.drawRect(0,0,1000,550,p);p.setShader(null);
        Random random=new Random(71+mode*41);
        for(int i=0;i<48;i++){
            float x=random.nextInt(1000),y=random.nextInt(200);
            p.setColor(i%4==0?0x557e8a91:0x2fbdc4c5);c.drawCircle(x,y,random.nextFloat()*1.7f+.5f,p);
        }
        p.setColor(0x3accd6d5);c.drawCircle(mode==TITLE?745:180,mode==TITLE?112:93,mode==TITLE?36:29,p);
        ridge(c,random,0xff34434d,239,95,9);
        ridge(c,random,0xff24333e,307,111,13);
        ridge(c,random,0xff17232c,387,82,18);
        mist(c,0x152c414c,302,87);
        if(mode==CODEX){dragon(c);castle(c,760,330,.65f);}else if(mode==STORY){
            castle(c,738,342,.98f);oldArch(c,136,406,0.65f);
        }else{oldArch(c,170,447,1.45f);castle(c,777,398,.69f);}
        trees(c,random,0xff0b161b,400,20,6);
        trees(c,random,0xff081014,475,33,9);
        p.setShader(new LinearGradient(0,310,0,550,new int[]{0x00080d10,0x76070b0e,0xff070b0e},null,Shader.TileMode.CLAMP));
        c.drawRect(0,310,1000,550,p);p.setShader(null);
        p.setColor(0x4e8c8780);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);
        c.drawLine(28,548,972,548,p);p.setStyle(Paint.Style.FILL);
        c.restore();
    }
    private void ridge(Canvas c,Random r,int color,int baseline,int peaks,int n){
        Path path=new Path();path.moveTo(-10,550);path.lineTo(-10,baseline);
        for(int i=0;i<=n;i++){
            float x=i*(1020f/n)-10;
            float y=baseline-(i%3==1?peaks:r.nextFloat()*peaks*.67f)+r.nextFloat()*27f;
            path.lineTo(x,y);
        }
        path.lineTo(1010,550);path.close();p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawPath(path,p);
        p.setColor(0x6baab2b5);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.3f);
        Path cap=new Path();cap.moveTo(-10,baseline);
        for(int i=0;i<=n;i++){
            float x=i*(1020f/n)-10;
            float y=baseline-(i%3==1?peaks:peaks*.28f);
            cap.lineTo(x,y);
        }
        c.drawPath(cap,p);p.setStyle(Paint.Style.FILL);
    }
    private void mist(Canvas c,int color,float cy,float radius){
        p.setColor(color);for(int i=0;i<7;i++)c.drawOval(-100+i*165,cy-radius/2,230+i*165,cy+radius/2,p);
    }
    private void trees(Canvas c,Random r,int color,int ground,int count,int offset){
        p.setColor(color);
        for(int i=0;i<count;i++){
            float x=(i*147+offset*23)%1050-25;
            float y=ground+r.nextInt(65);
            float size=32+r.nextInt(45);
            Path crown=new Path();crown.moveTo(x,y-size*1.8f);
            crown.lineTo(x-size*.35f,y-size*.5f);crown.lineTo(x-size*.14f,y-size*.5f);
            crown.lineTo(x-size*.53f,y);crown.lineTo(x+size*.53f,y);
            crown.lineTo(x+size*.14f,y-size*.5f);crown.lineTo(x+size*.35f,y-size*.5f);crown.close();
            c.drawPath(crown,p);c.drawRect(x-3,y-1,x+3,y+size*.35f,p);
        }
    }
    private void oldArch(Canvas c,float cx,float bottom,float scale){
        c.save();c.translate(cx,bottom);c.scale(scale,scale);
        p.setColor(0xff091117);
        c.drawRect(-100,-132,-58,30,p);c.drawRect(42,-156,84,30,p);
        Path top=new Path();top.moveTo(-100,-132);top.lineTo(-86,-184);top.lineTo(-26,-211);
        top.lineTo(34,-195);top.lineTo(84,-156);top.lineTo(60,-131);top.lineTo(22,-156);
        top.lineTo(-30,-165);top.lineTo(-70,-136);top.close();c.drawPath(top,p);
        p.setColor(0xff27343a);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);
        c.drawLine(-100,-132,-86,-184,p);c.drawLine(84,-156,62,-131,p);
        c.drawRect(-101,-130,-57,30,p);c.drawRect(42,-155,85,30,p);
        p.setStyle(Paint.Style.FILL);c.restore();
    }
    private void castle(Canvas c,float x,float y,float scale){
        c.save();c.translate(x,y);c.scale(scale,scale);
        p.setColor(0xff0c151c);
        Path land=new Path();land.moveTo(-133,56);land.lineTo(-108,19);land.lineTo(-46,-2);
        land.lineTo(13,8);land.lineTo(104,22);land.lineTo(145,54);land.close();c.drawPath(land,p);
        c.drawRect(-72,-57,93,16,p);c.drawRect(-40,-101,-8,-35,p);c.drawRect(45,-85,76,-35,p);
        c.drawRect(-75,-76,-50,-33,p);c.drawRect(0,-121,20,-31,p);
        Path spire=new Path();spire.moveTo(-49,-101);spire.lineTo(-23,-131);spire.lineTo(1,-101);spire.close();c.drawPath(spire,p);
        spire.reset();spire.moveTo(37,-85);spire.lineTo(60,-122);spire.lineTo(85,-85);spire.close();c.drawPath(spire,p);
        spire.reset();spire.moveTo(-8,-121);spire.lineTo(10,-151);spire.lineTo(29,-121);spire.close();c.drawPath(spire,p);
        p.setColor(GOLD);
        for(int j=0;j<3;j++){c.drawRect(-64+j*43,-34,-60+j*43,-23,p);c.drawRect(-37+j*43,-7,-32+j*43,7,p);}
        c.restore();
    }
    private void dragon(Canvas c){
        c.save();c.translate(520,278);c.scale(1.22f,1.22f);
        p.setColor(0xff9ba7ab);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);
        Path head=new Path();head.moveTo(-145,4);head.lineTo(-94,-35);head.lineTo(-42,-45);
        head.lineTo(6,-31);head.lineTo(40,-63);head.lineTo(57,-51);head.lineTo(34,-19);
        head.lineTo(80,8);head.lineTo(23,30);head.lineTo(-7,58);head.lineTo(-40,38);
        head.lineTo(-113,37);head.lineTo(-145,4);
        c.drawPath(head,p);
        for(int i=0;i<5;i++){
            float x=-33+i*24;
            c.drawLine(x,-32,x+11,-69-(i%2)*12,p);
        }
        for(int i=0;i<6;i++)c.drawLine(-125+i*15,20,-111+i*16,38,p);
        p.setStyle(Paint.Style.FILL);p.setColor(GOLD);c.drawCircle(-88,-4,5,p);
        c.restore();
    }
}
