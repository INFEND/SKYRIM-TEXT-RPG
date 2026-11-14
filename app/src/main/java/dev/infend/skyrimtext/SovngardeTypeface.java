package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * App-wide typography, including embedded dialogue, forms, dialogs and gallery.
 * Both weights are the exact Sovngarde Desktop Edition 2025 files from the
 * user-provided ZIP (the pinned CI mirror is verified by SHA-256).
 */
public final class SovngardeTypeface {
    public static final String NATIVE_ITEM_FONT="native_item_font";
    private static Typeface regular,bold;
    private SovngardeTypeface(){}

    public static synchronized void preload(Context context){
        if(regular!=null&&bold!=null)return;
        try{
            Typeface a=Typeface.createFromAsset(context.getAssets(),"fonts/Sovngarde Light.ttf");
            Typeface b=Typeface.createFromAsset(context.getAssets(),"fonts/Sovngarde Bold.ttf");
            regular=a;bold=b;
        }catch(Exception ignored){
            // Text stays legible in debug builds that lack the optional font assets.
            // Signed CI rejects missing assets before building an APK.
            regular=Typeface.SERIF;bold=Typeface.create(Typeface.SERIF,Typeface.BOLD);
        }
    }
    public static Typeface face(Context context,boolean heavy){
        preload(context);
        return heavy?bold:regular;
    }
    public static void applyTree(View root){
        if(root==null)return;
        if(root instanceof TextView && !NATIVE_ITEM_FONT.equals(root.getTag())){
            TextView text=(TextView)root;
            Typeface current=text.getTypeface();
            int style=current==null?Typeface.NORMAL:current.getStyle();
            text.setTypeface(face(root.getContext(),(style&Typeface.BOLD)!=0),style);
        }
        if(root instanceof ViewGroup){
            ViewGroup parent=(ViewGroup)root;
            for(int i=0;i<parent.getChildCount();i++)
                applyTree(parent.getChildAt(i));
        }
    }
}
