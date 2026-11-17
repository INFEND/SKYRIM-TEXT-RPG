package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.*;
import android.widget.FrameLayout;

/** Resolution-independent card vignette; no large bitmap assets required. */
final class FixShopArtwork extends FrameLayout {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int tint;
    private final String title;
    FixShopArtwork(Context context,String title,String glyph,int tint){
        super(context);this.title=title;this.tint=tint;setWillNotDraw(false);
        int size=(int)(70*getResources().getDisplayMetrics().density);
        LayoutParams p=new LayoutParams(size,size,android.view.Gravity.CENTER);
        addView(new SkyrimIconView(context,glyph,tint),p);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float w=getWidth(),h=getHeight(),x=w/2,y=h/2,r=Math.min(w,h)*.48f;
        if(r<=0)return;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(x,y,r*1.75f,new int[]{0xff000000|(tint&0x00ffffff),0xff16131c,0xff08080b},new float[]{0f,.35f,1f},Shader.TileMode.CLAMP));
        paint.setAlpha(80);canvas.drawRect(0,0,w,h,paint);paint.setShader(null);paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.3f);paint.setColor(tint);paint.setAlpha(100);canvas.drawCircle(x,y,r*.86f,paint);paint.setAlpha(45);canvas.drawCircle(x,y,r*.99f,paint);
        for(int i=0;i<12;i++){double a=i*Math.PI/6;float xx=x+(float)Math.cos(a)*r*.99f,yy=y+(float)Math.sin(a)*r*.99f;canvas.drawLine(xx,yy,x+(float)Math.cos(a)*r*1.08f,y+(float)Math.sin(a)*r*1.08f,paint);}
        paint.setStyle(Paint.Style.FILL);java.util.Random rand=new java.util.Random(title.hashCode());
        for(int i=0;i<32;i++){paint.setAlpha(30+rand.nextInt(120));canvas.drawCircle(rand.nextFloat()*w,rand.nextFloat()*h,0.6f+rand.nextFloat()*1.5f,paint);}paint.setAlpha(255);
    }
}
