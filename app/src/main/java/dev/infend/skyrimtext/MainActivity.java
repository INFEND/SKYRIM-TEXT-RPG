package dev.infend.skyrimtext;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final int BG=Color.rgb(5,7,9), PANEL=Color.rgb(12,16,20), PANEL_2=Color.rgb(17,23,28);
    private static final int TEXT=Color.rgb(241,238,229), MUTED=Color.rgb(151,160,163), BORDER=Color.rgb(41,49,54);
    private static final int[] ACCENTS={0xFFC8A86B,0xFF69CDA8,0xFF7FB5FF,0xFFAE8EF1,0xFFF18D86,0xFFE8E5DB};
    private static final String[] ACCENT_NAMES={"Золото", "Север", "Лёд", "Аметист", "Рубин", "Серебро"};
    private static final String PREFS="skyrim_text_v1";
    private GameEngine game;
    private SharedPreferences prefs;
    private LinearLayout root, content, header;
    private ScrollView scroll;
    private EditText nameField;
    private int tab=0, accentIndex=0, fontBonus=0;
    private String draftName="", selectedRace="Норд", selectedClass="Воин";
    private boolean animations=true;
    private UpdateManager updater;

    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        game=GameEngine.fromJson(prefs.getString("save",null));
        accentIndex=Math.max(0,Math.min(ACCENTS.length-1,prefs.getInt("accent",0)));
        fontBonus=Math.max(-2,Math.min(4,prefs.getInt("font_bonus",0)));
        animations=prefs.getBoolean("animations",true);
        updater=new UpdateManager(this);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        render();
    }
    @Override protected void onResume(){super.onResume();if(updater!=null && prefs!=null && game!=null && game.started){
        updater.onHostResume();
        long now=System.currentTimeMillis();long last=prefs.getLong("last_check",0);
        if(now-last>6L*60*60*1000){prefs.edit().putLong("last_check",now).apply();updater.check(false);}
    }}
    @Override protected void onPause(){save();super.onPause();}
    private void save(){try{prefs.edit().putString("save",game.toJson().toString()).apply();}catch(Exception ignored){}}
    private int dp(float dp){return (int)(dp*getResources().getDisplayMetrics().density+0.5f);}
    private int sp(int size){return Math.max(10,size+fontBonus);}
    private int accent(){return ACCENTS[accentIndex];}
    private GradientDrawable bg(int fill,int border,int radius){
        GradientDrawable gd=new GradientDrawable();gd.setColor(fill);gd.setCornerRadius(dp(radius));
        if(border!=Color.TRANSPARENT)gd.setStroke(dp(1),border);return gd;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String str,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(str);t.setTextSize(sp(size));t.setTextColor(color);
        t.setFontFeatureSettings("kern");t.setIncludeFontPadding(true);
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        return t;
    }
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void space(LinearLayout parent,int h){View v=new View(this);parent.addView(v,lp(1,h));}
    private void addText(LinearLayout parent,String str,int size,int color,boolean bold,int bottom){TextView t=text(str,size,color,bold);parent.addView(t);if(bottom>0)space(parent,bottom);}
    private void render(){
        root=column();root.setBackgroundColor(BG);root.setFitsSystemWindows(true);
        setContentView(root);
        renderHeader();
        if(game.started)renderBars();
        scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=column();content.setPadding(dp(19),dp(20),dp(19),dp(26));scroll.addView(content);
        if(!game.started)renderNewGame();
        else if(tab==0)renderStory();
        else if(tab==1)renderHero();
        else if(tab==2)renderJournal();
        else renderSettings();
        if(game.started)renderNav();
        if(animations){content.setAlpha(0.8f);content.animate().alpha(1f).setDuration(210).setInterpolator(new DecelerateInterpolator()).start();}
    }
    private void renderHeader(){
        header=row();header.setPadding(dp(19),dp(15),dp(19),dp(13));
        root.addView(header,lp(-1,-2));
        TextView sigil=text("ᛟ",29,accent(),true);sigil.setGravity(Gravity.CENTER);
        sigil.setBackground(bg(PANEL_2,accent(),13));header.addView(sigil,lp(47,47));
        LinearLayout copy=column();copy.setPadding(dp(12),0,0,0);
        header.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        addText(copy,"SKYRIM",18,TEXT,true,1);
        addText(copy,"TEXT ROLE-PLAYING GAME",9,accent(),true,0);
        if(game.started){LinearLayout meta=column();meta.setGravity(Gravity.END);
            addText(meta,"ДЕНЬ "+game.day,11,TEXT,true,1);
            addText(meta,"УР. "+game.level,10,MUTED,false,0);
            header.addView(meta,lp(-2,-2));}
        View line=new View(this);line.setBackgroundColor(BORDER);root.addView(line,lp(-1,1));
    }
    private void renderBars(){
        LinearLayout stats=row();stats.setPadding(dp(19),dp(11),dp(19),dp(11));
        stats.setBackgroundColor(PANEL);root.addView(stats,lp(-1,-2));
        stat(stats,"ЗДОРОВЬЕ",game.hp,game.maxHp,0xFFE47B7B);
        stat(stats,"ЗАПАС СИЛ",game.stamina,game.maxStamina,0xFF79CC9C);
        stat(stats,"МАГИЯ",game.magicka,game.maxMagicka,0xFF7DABF4);
        View line=new View(this);line.setBackgroundColor(BORDER);root.addView(line,lp(-1,1));
    }
    private void stat(LinearLayout host,String name,int value,int max,int color){
        LinearLayout col=column();LinearLayout.LayoutParams ww=new LinearLayout.LayoutParams(0,-2,1);ww.setMargins(dp(3),0,dp(6),0);host.addView(col,ww);
        TextView cap=text(name,9,MUTED,true);col.addView(cap);
        TextView num=text(value+"/"+max,11,TEXT,true);col.addView(num);
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1,max));bar.setProgress(Math.max(0,value));bar.setProgressTintList(ColorStateList.valueOf(color));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(BORDER));
        LinearLayout.LayoutParams b=lp(-1,3);b.topMargin=dp(6);col.addView(bar,b);
    }
    private void section(String eyebrow,String heading,String subtitle){
        addText(content,eyebrow.toUpperCase(),10,accent(),true,7);
        addText(content,heading,29,TEXT,true,9);
        if(subtitle!=null&&!subtitle.isEmpty()){TextView t=text(subtitle,15,MUTED,false);t.setLineSpacing(dp(3),1f);content.addView(t);}
        space(content,21);
    }
    private LinearLayout card(){
        LinearLayout c=column();c.setPadding(dp(17),dp(17),dp(17),dp(17));c.setBackground(bg(PANEL,BORDER,17));
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(13);content.addView(c,p);return c;
    }
    private TextView actionButton(String title,String detail,boolean primary,Runnable onClick){
        LinearLayout box=row();box.setPadding(dp(17),dp(15),dp(15),dp(15));
        box.setBackground(bg(primary?0xFF22251E:PANEL_2,primary?accent():BORDER,14));
        LinearLayout info=column();box.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        addText(info,title,16,primary?accent():TEXT,true,detail.isEmpty()?0:4);
        if(!detail.isEmpty())addText(info,detail,12,MUTED,false,0);
        TextView chevron=text("›",27,primary?accent():MUTED,false);chevron.setGravity(Gravity.CENTER);
        box.addView(chevron,lp(24,35));
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(10);content.addView(box,p);
        box.setClickable(true);box.setFocusable(true);box.setOnClickListener(v->onClick.run());
        return chevron;
    }
    private void renderNewGame(){
        section("СВОБОДНОЕ ПРИКЛЮЧЕНИЕ","Напиши свою легенду","Текстовая RPG на русском. Оригинальные приключения, тактические бои и последствия каждого решения.");
        LinearLayout c=card();addText(c,"ИМЯ ПЕРСОНАЖА",11,accent(),true,10);
        nameField=new EditText(this);nameField.setSingleLine(true);nameField.setTextColor(TEXT);
        nameField.setHintTextColor(MUTED);nameField.setHint("Как зовут героя?");nameField.setTextSize(sp(17));
        nameField.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(22)});
        nameField.setPadding(dp(12),dp(9),dp(12),dp(9));nameField.setBackground(bg(PANEL_2,BORDER,9));
        nameField.setText(draftName);c.addView(nameField,lp(-1,49));
        space(content,7);addText(content,"ПРОИСХОЖДЕНИЕ",11,accent(),true,11);
        chips(GameEngine.RACES,selectedRace,value->{draftName=nameField.getText().toString();selectedRace=value;render();});
        space(content,22);addText(content,"ПУТЬ ГЕРОЯ",11,accent(),true,11);
        chips(GameEngine.CLASSES,selectedClass,value->{draftName=nameField.getText().toString();selectedClass=value;render();});
        space(content,12);
        actionButton("НАЧАТЬ ПУТЕШЕСТВИЕ","Новая игра • автосохранение",true,()->{
            game.start(nameField.getText().toString(),selectedRace,selectedClass);tab=0;save();render();});
        space(content,13);addText(content,"Неофициальный фанатский проект. Оригинальные тексты и игровые события.",12,MUTED,false,0);
    }
    private interface Selection{void onSelect(String value);}
    private void chips(String[] options,String selected,Selection handler){
        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout layout=row();hsv.addView(layout);content.addView(hsv,lp(-1,48));
        for(String o:options){boolean active=o.equals(selected);
            TextView pill=text(o,14,active?BG:TEXT,active);pill.setGravity(Gravity.CENTER);
            pill.setPadding(dp(18),0,dp(18),0);pill.setBackground(bg(active?accent():PANEL_2,active?accent():BORDER,12));
            LinearLayout.LayoutParams p=lp(-2,43);p.rightMargin=dp(9);layout.addView(pill,p);
            pill.setOnClickListener(v->handler.onSelect(o));
        }
    }
    private void renderStory(){
        GameEngine.Scene s=game.scene();section(s.kicker,s.title,"");
        LinearLayout narrative=card();
        addText(narrative,"◈  ХРОНИКА ПУТИ",10,accent(),true,13);
        TextView body=text(s.body,16,TEXT,false);body.setLineSpacing(dp(5),1.09f);narrative.addView(body);
        space(content,8);addText(content,"ВЫБЕРИ ДЕЙСТВИЕ",11,MUTED,true,13);
        for(GameEngine.Choice c:s.choices){actionButton(c.title,c.detail,c.special,()->{
            game.choose(c.action);save();tab=0;render();scroll.scrollTo(0,0);
        });}
        space(content,5);
        addText(content,"Каждое действие сохраняется автоматически",11,MUTED,false,0);
    }
    private void heroEntry(LinearLayout p,String label,String value){
        LinearLayout r=row();p.addView(r,lp(-1,39));
        r.addView(text(label,14,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));
        r.addView(text(value,15,TEXT,true));
    }
    private void renderHero(){
        section("ЛИЧНОЕ ДЕЛО",""+game.name,game.race+" • "+game.heroClass+" • уровень "+game.level);
        LinearLayout c=card();addText(c,"ХАРАКТЕРИСТИКИ",11,accent(),true,9);
        heroEntry(c,"Опыт",game.xp+" / "+(game.level*80));
        heroEntry(c,"Атака",String.valueOf(game.attack));heroEntry(c,"Защита",String.valueOf(game.armor));
        heroEntry(c,"Золото",game.gold+" монет");heroEntry(c,"Репутация",String.valueOf(game.reputation));
        heroEntry(c,"Победы",String.valueOf(game.victories));heroEntry(c,"Исследований",String.valueOf(game.explored));
        LinearLayout items=card();addText(items,"СНАРЯЖЕНИЕ",11,accent(),true,12);
        addText(items,"Оружие: "+(game.swordUpgraded?"Закалённый клинок":"Походный клинок"),15,TEXT,false,10);
        addText(items,"Доспех: "+(game.armorUpgraded?"Стальной нагрудник":"Дорожная одежда"),15,TEXT,false,11);
        addText(items,"Лечебные зелья: "+game.potions,14,MUTED,false,6);
        addText(items,"Зелья магии: "+game.manaPotions,14,MUTED,false,0);
        actionButton("Выпить лечебное зелье","+45 здоровья • без боя",false,()->{
            toast(game.drinkHealing()?"Здоровье восстановлено":"Зелье не требуется или закончилось");save();render();});
        actionButton("Выпить зелье магии","+35 магии • без боя",false,()->{
            toast(game.drinkMana()?"Магия восстановлена":"Зелье не требуется или закончилось");save();render();});
    }
    private void renderJournal(){
        section("ЛЕТОПИСЬ","Задания","Твои решения формируют собственную историю Скайрима.");
        LinearLayout main=card();addText(main,"ОСНОВНАЯ ИСТОРИЯ",11,accent(),true,11);
        addText(main,"Пепельные печати",22,TEXT,true,8);
        addText(main,"Статус: "+game.questStatus(),13,accent(),false,13);
        addText(main,game.questDetails(),15,MUTED,false,0);
        LinearLayout side=card();addText(side,"ДОПОЛНИТЕЛЬНЫЕ ЗАДАНИЯ",11,accent(),true,11);
        addText(side,"Опасные дороги",20,TEXT,true,8);
        String sideStatus=game.sideQuest==0?"Нет активного заказа. Возьми его у доски объявлений в любом городе.":
                game.sideQuest==2?"Предложение доступно. Вернись к доске, чтобы принять его.":
                "Победы над противниками: "+game.sideKills+"/3"+(game.sideKills>=3?". Награда ждёт у доски.":".");
        addText(side,sideStatus,15,MUTED,false,0);
        actionButton("Вернуться к истории","Открыть текущую сцену",true,()->{tab=0;render();});
    }
    private void renderSettings(){
        section("ПАРАМЕТРЫ","Настройки","Твоя история, твои правила.");
        LinearLayout palette=card();addText(palette,"ЦВЕТ АКЦЕНТА",11,accent(),true,12);
        for(int i=0;i<ACCENTS.length;i++){
            final int value=i;LinearLayout item=row();LinearLayout.LayoutParams p=lp(-1,42);palette.addView(item,p);
            TextView mark=text(accentIndex==i?"●":"○",23,ACCENTS[i],true);item.addView(mark,lp(37,-2));
            item.addView(text(ACCENT_NAMES[i],15,TEXT,accentIndex==i));item.setOnClickListener(v->{accentIndex=value;prefs.edit().putInt("accent",value).apply();render();});
        }
        LinearLayout readability=card();addText(readability,"ЧИТАЕМОСТЬ",11,accent(),true,12);
        LinearLayout r=row();readability.addView(r,lp(-1,45));
        r.addView(text("Размер текста",15,TEXT,false),new LinearLayout.LayoutParams(0,-2,1));
        TextView smaller=text("−",24,accent(),true);smaller.setGravity(Gravity.CENTER);smaller.setBackground(bg(PANEL_2,BORDER,10));r.addView(smaller,lp(44,42));
        TextView amount=text(" "+(16+fontBonus)+" ",16,TEXT,true);r.addView(amount);
        TextView bigger=text("+",23,accent(),true);bigger.setGravity(Gravity.CENTER);bigger.setBackground(bg(PANEL_2,BORDER,10));r.addView(bigger,lp(44,42));
        smaller.setOnClickListener(v->{fontBonus=Math.max(-2,fontBonus-1);prefs.edit().putInt("font_bonus",fontBonus).apply();render();});
        bigger.setOnClickListener(v->{fontBonus=Math.min(4,fontBonus+1);prefs.edit().putInt("font_bonus",fontBonus).apply();render();});
        Switch sw=new Switch(this);sw.setText("Плавная анимация экранов");sw.setTextColor(TEXT);sw.setTextSize(sp(14));
        sw.setChecked(animations);sw.setThumbTintList(ColorStateList.valueOf(accent()));
        sw.setOnCheckedChangeListener((b,checked)->{animations=checked;prefs.edit().putBoolean("animations",checked).apply();});
        readability.addView(sw,lp(-1,47));
        space(content,6);addText(content,"ОБНОВЛЕНИЯ",11,accent(),true,11);
        actionButton("Проверить обновления","Установка APK внутри приложения • без браузера",true,()->updater.check(true));
        addText(content,"Текущая версия: "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")",12,MUTED,false,20);
        addText(content,"СОХРАНЕНИЯ",11,accent(),true,11);
        actionButton("Сохранить сейчас","Данные хранятся на устройстве",false,()->{save();toast("Прогресс сохранён");});
        actionButton("Копировать сохранение","Резервная копия в буфер обмена",false,()->{
            try{String s=game.toJson().toString();ClipboardManager cb=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("SKYRIM SAVE",s));toast("Код сохранения скопирован");}catch(Exception e){toast("Не удалось экспортировать");}
        });
        actionButton("Восстановить сохранение","Вставить код из резервной копии",false,this::restoreDialog);
        actionButton("Начать новую игру","Старое сохранение будет заменено",false,this::resetDialog);
        space(content,10);addText(content,"НЕОФИЦИАЛЬНЫЙ ФАНАТСКИЙ ПРОЕКТ",11,accent(),true,8);
        addText(content,"Оригинальные тексты и игровые механики. Не связан с Bethesda Softworks. Не содержит ресурсов официальной игры.",12,MUTED,false,0);
    }
    private void restoreDialog(){
        EditText e=new EditText(this);e.setTextColor(TEXT);e.setHint("Вставь JSON-код сохранения");e.setHintTextColor(MUTED);e.setMinLines(3);
        new AlertDialog.Builder(this).setTitle("Восстановление").setView(e).setNegativeButton("Отмена",null)
            .setPositiveButton("Загрузить",(d,w)->{
                try{JSONObject json=new JSONObject(e.getText().toString());if(json.optInt("schema")!=1 || !json.optBoolean("started"))throw new Exception();
                    game=GameEngine.fromJson(json.toString());tab=0;save();render();toast("Сохранение восстановлено");
                }catch(Exception ex){toast("Неверный код сохранения");}
            }).show();
    }
    private void resetDialog(){
        new AlertDialog.Builder(this).setTitle("Начать заново?")
            .setMessage("Текущее сохранение будет заменено. При необходимости сначала скопируй код сохранения.")
            .setNegativeButton("Отмена",null).setPositiveButton("Новая игра",(d,w)->{
                game=new GameEngine();tab=0;draftName="";save();render();}).show();
    }
    private void renderNav(){
        View sep=new View(this);sep.setBackgroundColor(BORDER);root.addView(sep,lp(-1,1));
        LinearLayout nav=row();nav.setPadding(dp(6),dp(7),dp(6),dp(7));nav.setBackgroundColor(PANEL);
        root.addView(nav,lp(-1,65));
        String[] labels={"ИСТОРИЯ","ГЕРОЙ","ЗАДАНИЯ","НАСТРОЙКИ"};
        String[] signs={"⌘","◇","☷","⚙"};
        for(int i=0;i<labels.length;i++){
            final int index=i;
            LinearLayout navitem=column();navitem.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-1,1);nav.addView(navitem,size);
            TextView icon=text(signs[i],23,tab==i?accent():MUTED,true);icon.setGravity(Gravity.CENTER);navitem.addView(icon);
            TextView t=text(labels[i],9,tab==i?accent():MUTED,tab==i);t.setGravity(Gravity.CENTER);navitem.addView(t);
            navitem.setOnClickListener(v->{if(tab!=index){tab=index;render();}});
        }
    }
    public void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    int currentAccent(){return accent();}
    @Override public void onBackPressed(){if(game.started&&tab!=0){tab=0;render();return;}
        if(game.started&&!game.mode.equals("WORLD")&&!game.mode.equals("COMBAT")){game.choose("GO_WORLD");save();render();return;}
        super.onBackPressed();}
}
