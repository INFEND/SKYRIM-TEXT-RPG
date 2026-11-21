package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

/** Smoothly rounded, compact byte progress; never uses the platform square track. */
final class ResourceMeter extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent;
    private float fraction;
    private boolean unknown;
    ResourceMeter(Context c,int accent){super(c);this.accent=accent;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void update(float value,boolean indeterminate){
        float clamped=Math.max(0f,Math.min(1f,value));
        if(fraction!=clamped||unknown!=indeterminate){fraction=clamped;unknown=indeterminate;invalidate();}
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight(),r=h/2f;if(w<=0||h<=0)return;
        paint.setColor(0xff292929);c.drawRoundRect(0,0,w,h,r,r,paint);
        paint.setColor(accent);
        if(unknown){
            float span=w*.25f, x=(SystemClock.uptimeMillis()%1300)/1300f*(w+span)-span;
            float left=Math.max(0,x),right=Math.min(w,x+span);
            if(right>left)c.drawRoundRect(left,0,right,h,r,r,paint);
            postInvalidateOnAnimation();
        }else if(fraction>0)c.drawRoundRect(0,0,Math.min(w,Math.max(h,w*fraction)),h,r,r,paint);
    }
}
