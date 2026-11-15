package dev.infend.skyrimtext;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import java.util.HashMap;
import java.util.WeakHashMap;

/** One persisted font family for all game-owned text, preserving emphasis. */
public final class SovngardeTypeface {
    public static final String PREF="font_family";
    public static final String[] IDS={"sovngarde","cormorantgaramond","forum","philosopher","oranienbaum","literata","lora","alegreya","ptserif","oswald","manrope","sourcesans3","system"};
    public static final String[] NAMES={"Sovngarde","Cormorant Garamond","Forum","Philosopher","Oranienbaum","Literata","Lora","Alegreya","PT Serif","Oswald","Manrope","Source Sans 3","Системный"};
    private static final Object PREVIEW=new Object();
    private static final HashMap<String,Typeface> cache=new HashMap<>();
    private static final WeakHashMap<View,Boolean> watched=new WeakHashMap<>();
    private static String selected="sovngarde";
    private SovngardeTypeface(){}

    public static String normalize(String id){
        for(String known:IDS)if(known.equals(id))return known;
        return IDS[0];
    }
    public static synchronized void preload(Context context){
        selected=normalize(context.getSharedPreferences("skyrim_text_v1",Context.MODE_PRIVATE).getString(PREF,IDS[0]));
        face(context,false);
        face(context,true);
    }
    public static String selected(){return selected;}
    public static String label(){
        for(int i=0;i<IDS.length;i++)if(IDS[i].equals(selected))return NAMES[i];
        return NAMES[0];
    }
    public static void select(Context context,String id){
        selected=normalize(id);
        context.getSharedPreferences("skyrim_text_v1",Context.MODE_PRIVATE).edit().putString(PREF,selected).apply();
    }
    public static Typeface face(Context context,boolean heavy){
        return forFamily(context,selected,heavy?Typeface.BOLD:Typeface.NORMAL);
    }
    public static Typeface styled(Context context,int style){
        return forFamily(context,selected,style);
    }
    private static boolean hasAsset(Context context,String path){
        try(java.io.InputStream ignored=context.getAssets().open(path)){return true;}
        catch(java.io.IOException ignored){return false;}
    }
    public static synchronized Typeface forFamily(Context context,String family,int style){
        family=normalize(family);
        style=style&Typeface.BOLD_ITALIC;
        String key=family+":"+style;
        Typeface cached=cache.get(key);
        if(cached!=null)return cached;
        boolean bold=(style&Typeface.BOLD)!=0, italic=(style&Typeface.ITALIC)!=0;
        Typeface value;
        try{
            if("system".equals(family))value=Typeface.create("sans-serif",style);
            else if("sovngarde".equals(family)){
                String path=bold?"fonts/Sovngarde Bold.ttf":"fonts/Sovngarde Light.ttf";
                value=new Typeface.Builder(context.getAssets(),path).setWeight(bold?700:400).setItalic(false).build();
                if(value==null)value=Typeface.createFromAsset(context.getAssets(),path);
                if(italic)value=Typeface.create(value,style);
            }else{
                String dir="fonts/choices/"+family+"/";
                boolean variable=java.util.Arrays.asList("cormorantgaramond","literata","lora","alegreya","oswald","manrope","sourcesans3").contains(family);
                String specific=dir+(bold&&italic?"bolditalic":bold?"bold":italic?"italic":"regular")+".ttf";
                boolean hasSpecific=hasAsset(context,specific);
                boolean realItalic=italic&&hasAsset(context,dir+"italic.ttf");
                String path=hasSpecific?specific:realItalic?dir+"italic.ttf":dir+"regular.ttf";
                boolean realBold=variable||(bold&&hasSpecific);
                Typeface.Builder builder=new Typeface.Builder(context.getAssets(),path)
                    .setWeight(bold&&realBold?700:400).setItalic(realItalic);
                if(variable)builder.setFontVariationSettings("'wght' "+(bold?700:400));
                value=builder.build();
                if(value==null)value=Typeface.createFromAsset(context.getAssets(),path);
                if((bold&&!realBold)||(italic&&!realItalic))value=Typeface.create(value,style);
            }
        }catch(Exception ignored){
            value=Typeface.create(Typeface.SERIF,style);
        }
        cache.put(key,value);
        return value;
    }
    public static void preview(TextView view,String family,int style){
        view.setTag(PREVIEW);
        view.setTypeface(forFamily(view.getContext(),family,style));
    }
    public static void applyTree(View root){
        if(root==null)return;
        if(root instanceof TextView && root.getTag()!=PREVIEW){
            TextView text=(TextView)root;
            Typeface current=text.getTypeface();
            int style=current==null?Typeface.NORMAL:current.getStyle();
            Typeface desired=styled(root.getContext(),style);
            if(current!=desired&&!desired.equals(current))text.setTypeface(desired);
        }
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++)applyTree(group.getChildAt(i));
        }
    }
    /** Handles recycled adapter rows and controls inserted after a dialog is shown. */
    public static void watchTree(View root){
        if(root==null)return;
        applyTree(root);
        if(watched.containsKey(root))return;
        watched.put(root,true);
        final ViewTreeObserver observer=root.getViewTreeObserver();
        final ViewTreeObserver.OnGlobalLayoutListener listener=()->applyTree(root);
        observer.addOnGlobalLayoutListener(listener);
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            @Override public void onViewAttachedToWindow(View v){}
            @Override public void onViewDetachedFromWindow(View v){
                if(observer.isAlive())observer.removeOnGlobalLayoutListener(listener);
                ViewTreeObserver now=v.getViewTreeObserver();
                if(now!=observer&&now.isAlive())now.removeOnGlobalLayoutListener(listener);
                watched.remove(v);
                v.removeOnAttachStateChangeListener(this);
            }
        });
    }
    public static void watchDialog(Dialog dialog){
        if(dialog.getWindow()!=null)watchTree(dialog.getWindow().getDecorView());
    }
}
