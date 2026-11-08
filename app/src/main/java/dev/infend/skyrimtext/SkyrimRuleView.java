package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** The thin woven horizontal rule shown throughout classic Skyrim menus. */
public final class SkyrimRuleView extends View {
    private final Paint p=new Paint(3);
    private final int tint;
    public SkyrimRuleView(Context context,int color){super(context);tint=color;setBackgroundColor(0);}
    @Override protected void onMeasure(int width,int height){
        int h=(int)(19*getResources().getDisplayMetrics().density+.5f);
        setMeasuredDimension(View.MeasureSpec.getSize(width),resolveSize(h,height));
    }
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight(),m=h/2f;if(w<24)return;
        p.setColor(tint);p.setStrokeWidth(Math.max(1f,getResources().getDisplayMetrics().density*.75f));
        p.setStyle(Paint.Style.STROKE);p.setAlpha(160);
        float left=16,right=w-16;
        c.drawLine(left,m,right,m,p);
        float middle=w/2f,r=Math.min(6,h*.28f);
        c.drawLine(middle-r,m,middle,m-r,p);
        c.drawLine(middle,m-r,middle+r,m,p);
        c.drawLine(middle+r,m,middle,m+r,p);
        c.drawLine(middle,m+r,middle-r,m,p);
        p.setAlpha(95);
        for(int i=0;i<6;i++){
            float offset=middle+15+i*11;
            if(offset+7<right){c.drawLine(offset,m-2,offset+4,m+2,p);c.drawLine(offset+4,m+2,offset+8,m-2,p);}
            float minus=middle-15-i*11;
            if(minus-7>left){c.drawLine(minus,m-2,minus-4,m+2,p);c.drawLine(minus-4,m+2,minus-8,m-2,p);}
        }
        p.setStyle(Paint.Style.FILL);
    }
}
