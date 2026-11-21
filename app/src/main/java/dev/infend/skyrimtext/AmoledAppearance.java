package dev.infend.skyrimtext;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import java.util.WeakHashMap;

/** Shared, persisted AMOLED appearance. No game-state or click callback changes. */
public final class AmoledAppearance {
    public static final String BUTTON_PREF="amoled_button_style", WINDOW_PREF="amoled_window_style",
        MOTION_PREF="amoled_button_motion", HAPTIC_PREF="amoled_button_haptic";
    public static final String[] NAMES={"Обсидиан","Золотые капсулы","Северные руны",
        "Тёмное стекло","Имперский минимализм","Сюжетные действия","Плавающие кнопки"};
    public static final String[] MOTIONS={"Выключено","Мягкое уменьшение","Подсветка","Уменьшение и подсветка"};
    private static int buttons=0, windows=-1, motion=3;
    private static boolean haptic=false;
    private static final WeakHashMap<View,Integer> styled=new WeakHashMap<>();
    private AmoledAppearance(){}
    static int normalize(int value){return Math.max(0,Math.min(NAMES.length-1,value));}
    static int normalizeMotion(int value){return Math.max(0,Math.min(MOTIONS.length-1,value));}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("skyrim_text_v1",Context.MODE_PRIVATE);}
    public static void load(Context c){
        SharedPreferences p=prefs(c);
        buttons=normalize(p.getInt(BUTTON_PREF,0));
        int w=p.getInt(WINDOW_PREF,-1);windows=w<0?-1:normalize(w);
        motion=normalizeMotion(p.getInt(MOTION_PREF,3));haptic=p.getBoolean(HAPTIC_PREF,false);
    }
    public static void choose(Context c,String key,int value){
        prefs(c).edit().putInt(key,value).apply();load(c);
    }
    public static void haptic(Context c,boolean value){prefs(c).edit().putBoolean(HAPTIC_PREF,value).apply();load(c);}
    public static int buttons(){return buttons;}
    public static int windowChoice(){return windows;}
    public static int windows(){return windows<0?buttons:windows;}
    public static int motion(){return motion;}
    public static boolean haptic(){return haptic;}
    public static int radius(int style,boolean window,int requested){
        int[] radii={16,32,14,20,18,18,24};
        int base=radii[normalize(style)];
        return window?Math.max(24,base):Math.max(10,Math.max(base,requested));
    }
    public static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    public static GradientDrawable shape(Context c,int color,int border,int requested){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(c,radius(buttons,false,requested)));
        if(border!=Color.TRANSPARENT)d.setStroke(dp(c,1),border);
        return d;
    }
    public static RuneFrameDrawable window(int color,int accent){return new RuneFrameDrawable(color,accent,true,windows(),true);}
    public static int storyTint(String action,String title,int fallback){
        if(buttons!=5)return fallback;
        String key=(action+" "+title).toLowerCase(java.util.Locale.ROOT).replace('\n',' ');
        if(key.matches(".*(attack|heavy|fight|kill|атак|удар|убить|бой).*"))return 0xffe07171;
        if(key.matches(".*(heal|potion|леч|здоров|восстанов).*"))return 0xff79b98a;
        if(key.matches(".*(spell|magic|cast|заклин|магия|колдов).*"))return 0xff81a9e2;
        if(key.matches(".*(sneak|stealth|красть|скрыт|незамет).*"))return 0xffb399da;
        if(key.matches(".*(travel|region:|leave|покин|отправ|идти|перейти).*"))return 0xffaaaaaa;
        return 0xffd0b583;
    }
    public static void applyTree(View root){
        if(root==null)return;
        if(root.isClickable()&&root.hasOnClickListeners()&&!(root instanceof EditText)
            &&!(root instanceof CompoundButton)&&!(root instanceof SeekBar)
            &&!(root instanceof ScrollView)&&!(root instanceof HorizontalScrollView)){
            int stamp=buttons*100+motion*10+(haptic?1:0);
            Integer old=styled.get(root);
            if(old==null||old!=stamp){
                styled.put(root,stamp);
                if(root.getBackground()==null){
                    root.setBackground(shape(root.getContext(),Color.TRANSPARENT,Color.TRANSPARENT,12));
                }
                if(root.getBackground() instanceof android.graphics.drawable.ColorDrawable){
                    int color=((android.graphics.drawable.ColorDrawable)root.getBackground()).getColor();
                    root.setBackground(shape(root.getContext(),color,Color.TRANSPARENT,12));
                }
                boolean scale=motion==1||motion==3, glow=motion==2||motion==3;
                if(glow&&root.getForeground()==null){
                    GradientDrawable mask=shape(root.getContext(),Color.WHITE,Color.TRANSPARENT,12);
                    root.setForeground(new RippleDrawable(ColorStateList.valueOf(0x40d0b583),null,mask));
                }else if(!glow&&root.getForeground() instanceof RippleDrawable)root.setForeground(null);
                if(scale||haptic){
                    StateListAnimator states=new StateListAnimator();
                    float target=scale?.96f:1f;
                    android.animation.AnimatorSet pressed=new android.animation.AnimatorSet();
                    pressed.playTogether(ObjectAnimator.ofFloat(root,View.SCALE_X,target),ObjectAnimator.ofFloat(root,View.SCALE_Y,target));
                    pressed.setDuration(100);
                    if(haptic)pressed.addListener(new AnimatorListenerAdapter(){
                        @Override public void onAnimationStart(Animator animation){
                            if(root.isPressed())root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        }
                    });
                    states.addState(new int[]{android.R.attr.state_enabled,android.R.attr.state_pressed},pressed);
                    android.animation.AnimatorSet released=new android.animation.AnimatorSet();
                    released.playTogether(ObjectAnimator.ofFloat(root,View.SCALE_X,1f),ObjectAnimator.ofFloat(root,View.SCALE_Y,1f));
                    released.setDuration(scale?155:0);
                    states.addState(new int[]{},released);root.setStateListAnimator(states);
                }else{root.setStateListAnimator(null);root.setScaleX(1);root.setScaleY(1);}
            }
        }
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++)applyTree(group.getChildAt(i));
        }
    }
}
