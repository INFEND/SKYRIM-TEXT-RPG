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
    private static final int BG=Color.rgb(7,11,15), PANEL=Color.rgb(12,17,21), PANEL_2=Color.rgb(16,22,27);
    private static final int TEXT=Color.rgb(237,231,218), MUTED=Color.rgb(169,170,165), BORDER=Color.rgb(85,79,68);
    private static final int[] ACCENTS={0xFFE1C58F,0xFF69CDA8,0xFF7FB5FF,0xFFAE8EF1,0xFFF18D86,0xFFE8E5DB};
    private static final String[] ACCENT_NAMES={"Золото", "Север", "Лёд", "Аметист", "Рубин", "Серебро"};
    private static final String PREFS="skyrim_text_v1";
    private GameEngine game;
    private SharedPreferences prefs;
    private LinearLayout root, content, header;
    private ScrollView scroll;
    private EditText nameField;
    private int tab=0, accentIndex=0, fontBonus=0;
    private String draftName="", selectedRace="Норд", selectedClass="Без класса";
    private int selectedStart=0,selectedMapRegion=-1;
    private final int[] initialPoints={0,0,0};
    private String selectedCategory="Все",selectedSort="Название";
    private boolean animations=true, titleOpen=true;
    private int codexCategory=0;
    private SkyrimOtaController updater;

    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        ItemCatalog.initialize(this);
        game=GameEngine.fromJson(prefs.getString("save",null));
        accentIndex=Math.max(0,Math.min(ACCENTS.length-1,prefs.getInt("accent",0)));
        fontBonus=Math.max(-2,Math.min(4,prefs.getInt("font_bonus",0)));
        animations=prefs.getBoolean("animations",true);
        updater=new SkyrimOtaController(this);
        OtaUpdateSchedule.configure(this);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        render();
    }
    @Override protected void onResume(){super.onResume();if(updater!=null && prefs!=null){
        updater.onHostResume();
        if(prefs.getBoolean(OtaUpdateSchedule.PREF_ON_LAUNCH,true)){
            long now=System.currentTimeMillis();long last=prefs.getLong("last_check",0);
            if(now-last>6L*60*60*1000){prefs.edit().putLong("last_check",now).apply();updater.check(false);}
        }
    }}
    @Override protected void onPause(){save();super.onPause();}
    private void save(){try{prefs.edit().putString("save",game.toJson().toString()).apply();}catch(Exception ignored){}}
    private int dp(float dp){return (int)(dp*getResources().getDisplayMetrics().density+0.5f);}
    private int sp(int size){return Math.max(10,size+fontBonus);}
    private int accent(){return ACCENTS[accentIndex];}
    private GradientDrawable bg(int fill,int border,int radius){
        GradientDrawable gd=new GradientDrawable();gd.setColor(fill);gd.setCornerRadius(dp(Math.min(radius,3)));
        if(border!=Color.TRANSPARENT)gd.setStroke(dp(1),border);return gd;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String str,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(str);t.setTextSize(sp(size));t.setTextColor(color);
        t.setFontFeatureSettings("kern");t.setIncludeFontPadding(true);
        t.setTypeface(Typeface.create(bold?"serif":"serif",bold?Typeface.BOLD:Typeface.NORMAL));
        return t;
    }
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void space(LinearLayout parent,int h){View v=new View(this);parent.addView(v,lp(1,h));}
    private void addText(LinearLayout parent,String str,int size,int color,boolean bold,int bottom){TextView t=text(str,size,color,bold);parent.addView(t);if(bottom>0)space(parent,bottom);}
    private void render(){
        root=column();root.setBackgroundColor(BG);root.setFitsSystemWindows(true);
        setContentView(root);
        if(!titleOpen){renderHeader();if(game.started)renderBars();}
        scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=column();content.setPadding(dp(21),dp(23),dp(21),dp(30));scroll.addView(content);
        if(titleOpen)renderTitle();
        else if(!game.started && tab!=5)renderNewGame();
        else if(tab==0)renderStory();
        else if(tab==1)renderMap();
        else if(tab==2)renderInventory();
        else if(tab==3)renderHero();
        else if(tab==4)renderJournal();
        else if(tab==6)renderCodex();
        else renderSettings();
        if(!titleOpen&&game.started)renderNav();
        if(animations){content.setAlpha(0.8f);content.animate().alpha(1f).setDuration(210).setInterpolator(new DecelerateInterpolator()).start();}
    }
    private void renderHeader(){
        header=row();header.setPadding(dp(19),dp(15),dp(19),dp(13));
        root.addView(header,lp(-1,-2));
        TextView sigil=text("ᛟ",30,accent(),false);sigil.setGravity(Gravity.CENTER);
        sigil.setBackground(new RuneFrameDrawable(BG,accent(),true));header.addView(sigil,lp(47,47));
        sigil.setContentDescription("Открыть главное меню");
        sigil.setOnClickListener(v->{titleOpen=true;render();});
        LinearLayout copy=column();copy.setPadding(dp(12),0,0,0);
        header.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        addText(copy,"SKYRIM",23,TEXT,false,1);
        addText(copy,"ᛟ  ТЕКСТОВАЯ RPG  ᛟ",10,accent(),false,0);
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
        addText(content,"ᛟ  "+eyebrow.toUpperCase()+"  ᛟ",11,accent(),false,10);
        addText(content,heading,30,TEXT,false,9);
        TextView runeRule=text("────────  ◈  ────────",14,accent(),false);runeRule.setGravity(Gravity.CENTER);content.addView(runeRule,lp(-1,32));
        if(subtitle!=null&&!subtitle.isEmpty()){TextView t=text(subtitle,15,MUTED,false);t.setLineSpacing(dp(3),1f);content.addView(t);}
        space(content,19);
    }
    private LinearLayout card(){
        LinearLayout c=column();c.setPadding(dp(17),dp(17),dp(17),dp(17));c.setBackground(new RuneFrameDrawable(PANEL,0xff82745d,false));
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(13);content.addView(c,p);return c;
    }
    private TextView actionButton(String title,String detail,boolean primary,Runnable onClick){
        LinearLayout box=row();box.setPadding(dp(17),dp(15),dp(15),dp(15));
        box.setBackground(new RuneFrameDrawable(primary?0xff22231e:0xff0e1519,primary?accent():0xff85745f,primary));
        LinearLayout info=column();box.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        addText(info,title,17,primary?accent():TEXT,false,detail.isEmpty()?0:4);
        if(!detail.isEmpty())addText(info,detail,12,MUTED,false,0);
        TextView chevron=text("ᛟ",19,primary?accent():MUTED,false);chevron.setGravity(Gravity.CENTER);
        box.addView(chevron,lp(24,35));
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(10);content.addView(box,p);
        box.setClickable(true);box.setFocusable(true);box.setOnClickListener(v->onClick.run());
        return chevron;
    }
    private void titleButton(String caption,boolean active,Runnable callback){
        TextView v=text(caption,19,active?accent():TEXT,false);v.setGravity(Gravity.CENTER);
        v.setBackground(new RuneFrameDrawable(active?0xff27261f:0xff0c1216,active?accent():0xff8c7b62,active));
        LinearLayout.LayoutParams params=lp(-1,49);params.setMargins(dp(39),0,dp(39),dp(10));
        content.addView(v,params);v.setClickable(true);v.setFocusable(true);
        v.setOnClickListener(view->callback.run());
    }
    private void titleRule(){
        TextView t=text("──────────  ◈  ──────────",15,accent(),false);
        t.setGravity(Gravity.CENTER);content.addView(t,lp(-1,39));
    }
    private void renderTitle(){
        content.setPadding(0,dp(8),0,dp(25));
        FrameLayout cover=new FrameLayout(this);content.addView(cover,lp(-1,340));
        cover.addView(new NordicArtView(this,NordicArtView.TITLE),
            new FrameLayout.LayoutParams(-1,-1));
        LinearLayout letter=column();letter.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams overlay=new FrameLayout.LayoutParams(-1,-1);overlay.setMargins(dp(8),0,dp(8),dp(25));cover.addView(letter,overlay);
        TextView rune=text("ᚱ   ᛟ   ᚦ",38,accent(),false);rune.setGravity(Gravity.CENTER);letter.addView(rune,lp(-1,75));
        TextView name=text("СКАЙРИМ",38,TEXT,false);name.setGravity(Gravity.CENTER);name.setLetterSpacing(.14f);
        letter.addView(name,lp(-1,60));
        TextView subtitle=text("Т Е К С Т О В А Я   R P G",11,accent(),false);
        subtitle.setGravity(Gravity.CENTER);letter.addView(subtitle,lp(-1,32));
        titleRule();
        titleButton("Новая игра",true,()->{
            if(game.started){
                new RuneDialogBuilder(this,accent()).setTitle("Новая история")
                    .setMessage("Существующее сохранение будет заменено. Продолжить?")
                    .setNegativeButton("Отмена",null)
                    .setPositiveButton("Новая игра",(d,w)->{
                        game=new GameEngine();save();titleOpen=false;tab=0;render();
                    }).show();
            }else{titleOpen=false;tab=0;render();}
        });
        titleButton("Продолжить",false,()->{
            if(!game.started){toast("Сохранённого героя пока нет");return;}
            titleOpen=false;tab=0;render();
        });
        titleButton("Настройки",false,()->{titleOpen=false;tab=5;render();});
        titleButton("О игре",false,()->new RuneDialogBuilder(this,accent())
            .setTitle("СКАЙРИМ · ТЕКСТОВАЯ RPG")
            .setMessage("Интерактивная история северных земель. Выборы, путешествия, дневник и кодекс. Самостоятельный неофициальный проект.")
            .setPositiveButton("Закрыть",null).show());
        titleRule();
        TextView foot=text("ᛟ  ТВОЯ ИСТОРИЯ НАЧИНАЕТСЯ НА СЕВЕРЕ  ᛟ",10,MUTED,false);
        foot.setGravity(Gravity.CENTER);content.addView(foot,lp(-1,36));
    }
    private void renderNewGame(){
        section("НОВАЯ ИСТОРИЯ","Твоя легенда начинается","Выбери место старта, расу, путь развития и распределение первых пяти очков. Каждый выбор влияет на историю героя.");
        content.addView(new NordicArtView(this,NordicArtView.STORY),lp(-1,165));
        space(content,15);
        LinearLayout details=card();addText(details,"ИМЯ ПЕРСОНАЖА",11,accent(),true,10);
        LinearLayout nameLine=row();details.addView(nameLine,lp(-1,-2));
        nameField=new EditText(this);nameField.setSingleLine(true);nameField.setTextColor(TEXT);nameField.setHintTextColor(MUTED);
        nameField.setHint("Имя героя");nameField.setTextSize(sp(16));
        nameField.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(28)});
        nameField.setPadding(dp(11),dp(8),dp(11),dp(8));nameField.setBackground(new RuneFrameDrawable(PANEL_2,0xff85745f,false));
        nameField.setText(draftName);nameLine.addView(nameField,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView nameDice=text("⟳",26,accent(),true);nameDice.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams diceLp=lp(48,48);diceLp.leftMargin=dp(7);nameLine.addView(nameDice,diceLp);
        nameDice.setBackground(bg(PANEL_2,accent(),10));
        nameDice.setContentDescription("Сгенерировать случайное имя");
        nameDice.setOnClickListener(v->{draftName=RpgContent.randomName();nameField.setText(draftName);nameField.setSelection(draftName.length());});
        addText(details,"Нажми ⟳, чтобы получить случайное имя.",12,MUTED,false,0);

        addText(content,"РАСА",11,accent(),true,9);
        chips(GameEngine.RACES,selectedRace,value->{draftName=nameField.getText().toString();selectedRace=value;render();});
        int race=RpgContent.raceIndex(selectedRace);
        LinearLayout racial=card();
        addText(racial,"ПАССИВНЫЙ ЭФФЕКТ",10,accent(),true,6);
        addText(racial,RpgContent.PASSIVES[race],14,TEXT,false,11);
        addText(racial,"РАСОВЫЙ ТАЛАНТ",10,accent(),true,6);
        addText(racial,RpgContent.POWERS[race],14,MUTED,false,0);

        addText(content,"КЛАСС • НЕОБЯЗАТЕЛЬНО",11,accent(),true,9);
        chips(GameEngine.CLASSES,selectedClass,value->{draftName=nameField.getText().toString();selectedClass=value;render();});
        int cls=0;for(int i=0;i<GameEngine.CLASSES.length;i++)if(GameEngine.CLASSES[i].equals(selectedClass))cls=i;
        addText(content,RpgContent.CLASS_DETAILS[cls],13,MUTED,false,15);

        LinearLayout stats=card();addText(stats,"СТАРТОВЫЕ ХАРАКТЕРИСТИКИ",11,accent(),true,7);
        int used=initialPoints[0]+initialPoints[1]+initialPoints[2];
        addText(stats,"Свободных очков: "+(5-used)+" / 5  •  +10 к выбранному ресурсу за очко",13,MUTED,false,12);
        for(int i=0;i<3;i++){
            final int index=i;LinearLayout line=row();stats.addView(line,lp(-1,50));
            line.addView(text(RpgContent.ATTRIBUTES[i],14,TEXT,false),new LinearLayout.LayoutParams(0,-2,1));
            TextView minus=text("−",24,accent(),true);minus.setGravity(Gravity.CENTER);minus.setBackground(bg(PANEL_2,BORDER,9));
            line.addView(minus,lp(38,38));
            TextView amount=text(" +"+initialPoints[i],16,TEXT,true);amount.setGravity(Gravity.CENTER);
            line.addView(amount,lp(63,43));
            TextView plus=text("+",23,accent(),true);plus.setGravity(Gravity.CENTER);plus.setBackground(bg(PANEL_2,BORDER,9));
            line.addView(plus,lp(38,38));
            minus.setOnClickListener(v->{if(initialPoints[index]>0){draftName=nameField.getText().toString();initialPoints[index]--;render();}});
            plus.setOnClickListener(v->{if(initialPoints[0]+initialPoints[1]+initialPoints[2]<5){draftName=nameField.getText().toString();initialPoints[index]++;render();}});
        }

        LinearLayout origin=card();
        addText(origin,"АЛЬТЕРНАТИВНОЕ НАЧАЛО",11,accent(),true,10);
        String[] origins=new String[RpgContent.STARTS.length];
        for(int i=0;i<origins.length;i++)origins[i]=RpgContent.startParts(i)[0];
        android.widget.Spinner selector=spinner(origins,selectedStart,i->{selectedStart=i;});
        origin.addView(selector,lp(-1,51));
        TextView originText=text(RpgContent.startParts(selectedStart)[3],14,MUTED,false);
        originText.setLineSpacing(dp(4),1.08f);
        origin.addView(originText);
        selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> p){}
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){
                selectedStart=position;originText.setText(RpgContent.startParts(position)[3]);
            }
        });
        space(content,8);
        actionButton("НАЧАТЬ ПУТЕШЕСТВИЕ","Игра с сохранением, выбором судьбы и последствиями решений",true,()->{
            int n=initialPoints[0]+initialPoints[1]+initialPoints[2];
            if(n!=5){toast("Распредели все 5 свободных очков");return;}
            game.start(nameField.getText().toString(),selectedRace,selectedClass,selectedStart,initialPoints);
            tab=0;save();render();
        });
        addText(content,"Неофициальная самостоятельная история в знакомом северном мире.",12,MUTED,false,0);
    }
    private android.widget.Spinner spinner(String[] options,int position,java.util.function.IntConsumer onChange){
        android.widget.Spinner sp=new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> adapter=new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,options){
            @Override public View getView(int pos,View old,ViewGroup parent){
                TextView t=(TextView)super.getView(pos,old,parent);t.setTextColor(TEXT);t.setTextSize(15);t.setPadding(dp(12),dp(6),dp(9),dp(6));return t;
            }
            @Override public View getDropDownView(int pos,View old,ViewGroup parent){
                TextView t=(TextView)super.getDropDownView(pos,old,parent);t.setTextColor(TEXT);
                t.setBackgroundColor(PANEL);t.setPadding(dp(15),dp(15),dp(10),dp(15));return t;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);sp.setSelection(Math.max(0,Math.min(position,options.length-1)));
        sp.setBackground(new RuneFrameDrawable(PANEL_2,0xff85745f,false));
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> v){}
            public void onItemSelected(android.widget.AdapterView<?> v,View view,int pos,long id){onChange.accept(pos);}
        });
        return sp;
    }
    private interface Selection{void onSelect(String value);}
    private void chips(String[] options,String selected,Selection handler){
        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout layout=row();hsv.addView(layout);content.addView(hsv,lp(-1,48));
        for(String o:options){boolean active=o.equals(selected);
            TextView pill=text(o,14,active?accent():TEXT,active);pill.setGravity(Gravity.CENTER);
            pill.setPadding(dp(18),0,dp(18),0);pill.setBackground(bg(active?0xFF2D2A22:PANEL_2,active?accent():BORDER,2));
            LinearLayout.LayoutParams p=lp(-2,43);p.rightMargin=dp(9);layout.addView(pill,p);
            pill.setOnClickListener(v->handler.onSelect(o));
        }
    }
    private void renderStory(){
        GameEngine.Scene s=game.scene();section(s.kicker,s.title,"");
        content.addView(new NordicArtView(this,NordicArtView.STORY),lp(-1,188));
        space(content,16);
        LinearLayout narrative=card();
        addText(narrative,"ᚱ  ГЛАВА  ·  "+s.kicker.toUpperCase(java.util.Locale.ROOT),11,accent(),false,13);
        TextView body=text(s.body,18,TEXT,false);body.setLineSpacing(dp(6),1.12f);makeItemsClickable(body,s.body);narrative.addView(body);
        space(content,8);addText(content,"ВЫБЕРИ ДЕЙСТВИЕ",11,MUTED,true,13);
        for(GameEngine.Choice c:s.choices){
            int chance=game.progress.chance(game,c.action);
            String caption=c.detail+(chance<0?"":"\n[Шанс успеха: "+chance+"%]");
            actionButton(c.title,caption,c.special,()->{
                int old=game.questStage;
                game.choose(c.action);
                if(!c.action.startsWith("ACT:"))game.progress.observe(game,c.action,old);
                game.progress.sync(game);
                save();tab=0;render();scroll.scrollTo(0,0);
            });
        }
        space(content,5);
        addText(content,"Каждое действие сохраняется автоматически",11,MUTED,false,0);
    }
    private void makeItemsClickable(TextView view,String source){
        android.text.SpannableString span=new android.text.SpannableString(source);
        java.util.ArrayList<ItemCatalog.Item> list=ItemCatalog.all();
        list.sort((a,b)->Integer.compare(b.name.length(),a.name.length()));
        boolean[] occupied=new boolean[source.length()];
        int created=0;
        for(ItemCatalog.Item item:list){
            int i=0;
            while((i=source.indexOf(item.name,i))>=0){
                final int start=i,end=i+item.name.length();i=end;
                boolean overlaps=false;
                for(int j=start;j<end;j++)if(occupied[j]){overlaps=true;break;}
                if(overlaps)continue;
                for(int j=start;j<end;j++)occupied[j]=true;
                span.setSpan(new android.text.style.ClickableSpan(){
                    @Override public void onClick(View widget){itemDialog(item);}
                    @Override public void updateDrawState(android.text.TextPaint p){
                        p.setColor(accent());p.setUnderlineText(true);
                    }
                },start,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(++created>60)break;
            }
            if(created>60)break;
        }
        if(created>0){
            view.setText(span);view.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            view.setHighlightColor(0x2255dfcb);
        }
    }
    private void renderMap(){
        section("АТЛАС СЕВЕРА","Карта Скайрима","Метка показывает твоё текущее местоположение. Нажми на любую точку или выбери место в списке.");
        LinearLayout overview=card();
        addText(overview,"⌖  ВЫ НАХОДИТЕСЬ: "+game.progress.location,15,accent(),true,10);
        addText(overview,"Регион: "+GameEngine.REGIONS[game.region]+"  •  день "+game.day,13,MUTED,false,9);
        overview.addView(new WorldAtlas.MapView(this,game,this::locationDialog),lp(-1,-2));
        addText(content,"МЕСТА И ПЕРЕХОДЫ",11,accent(),true,10);
        String[] regions=new String[GameEngine.REGIONS.length+1];regions[0]="Все регионы";
        System.arraycopy(GameEngine.REGIONS,0,regions,1,GameEngine.REGIONS.length);
        android.widget.Spinner filter=spinner(regions,selectedMapRegion+1,i->{
            if(selectedMapRegion!=i-1){selectedMapRegion=i-1;render();}
        });
        content.addView(filter,lp(-1,49));space(content,12);
        java.util.ArrayList<WorldAtlas.Place> places=selectedMapRegion<0?WorldAtlas.all():WorldAtlas.byRegion(selectedMapRegion);
        addText(content,"Обозначено мест: "+places.size(),12,MUTED,false,8);
        for(WorldAtlas.Place place:places){
            String status=place.name.equals(game.progress.location)?"  •  ВЫ ЗДЕСЬ":"";
            TravelPlanner.Route route=TravelPlanner.plan(game,place);
            String detail=place.type+"  •  "+GameEngine.REGIONS[place.region]+
                (route.allowed?(route.days>0?"  •  "+route.days+" дн.":""):"  •  Недоступно");
            actionButton(WorldAtlas.icon(place.type)+"  "+place.name+status,detail,false,()->locationDialog(place));
        }
    }
    private void locationDialog(WorldAtlas.Place place){
        TravelPlanner.Route route=TravelPlanner.plan(game,place);
        String body=place.description+"\n\nТип: "+place.type+
            "\nРегион: "+GameEngine.REGIONS[place.region]+"\n\n"+route.summary();
        AlertDialog.Builder dialog=new RuneDialogBuilder(this,accent()).setTitle(place.name).setMessage(body)
            .setNegativeButton("Закрыть",null);
        if(route.allowed&&route.days>0){
            dialog.setPositiveButton("Начать переход",(d,w)->{
                game.choose("LOC:"+place.name);game.progress.sync(game);save();tab=0;render();
            });
        }
        dialog.show();
    }
    private void renderInventory(){
        game.progress.sync(game);
        section("СУМКА СТРАННИКА","Инвентарь","Каждый предмет имеет вес, цену и собственное описание. Переносимый вес учитывается при путешествиях.");
        content.addView(new NordicArtView(this,NordicArtView.TITLE),lp(-1,120));space(content,12);
        LinearLayout capacity=card();
        double used=game.progress.carryWeight();
        addText(capacity,String.format(java.util.Locale.forLanguageTag("ru"),"ВЕС: %.2f / 50.00 КГ",used),19,used>50?0xffef8585:accent(),true,8);
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(5000);bar.setProgress(Math.min(5000,(int)Math.round(used*100)));
        bar.setProgressTintList(ColorStateList.valueOf(used>50?0xffef8585:accent()));
        capacity.addView(bar,lp(-1,8));
        addText(capacity,"Септимы: "+game.gold+"  •  Каталог: "+ItemCatalog.count()+" предметов",13,MUTED,false,0);
        java.util.ArrayList<String> categories=ItemCatalog.groups();
        if(!categories.contains(selectedCategory))selectedCategory="Все";
        int index=categories.indexOf(selectedCategory);
        android.widget.Spinner group=spinner(categories.toArray(new String[0]),index,i->{
            String choice=categories.get(i);if(!choice.equals(selectedCategory)){selectedCategory=choice;render();}
        });
        content.addView(group,lp(-1,50));space(content,8);
        String[] sorts={"Название","Вес","Цена"};
        int sortIndex=java.util.Arrays.asList(sorts).indexOf(selectedSort);
        android.widget.Spinner sort=spinner(sorts,Math.max(0,sortIndex),i->{
            if(!sorts[i].equals(selectedSort)){selectedSort=sorts[i];render();}
        });
        content.addView(sort,lp(-1,50));space(content,10);
        java.util.ArrayList<ItemCatalog.Item> owned=game.progress.owned(selectedCategory,selectedSort);
        if(owned.isEmpty())addText(content,"В этой категории пока ничего нет.",15,MUTED,false,15);
        for(ItemCatalog.Item item:owned){
            int count=game.progress.quantity(item.id);
            String desc=String.format(java.util.Locale.forLanguageTag("ru"),"%s · %.2f кг × %d · %d септимов",item.category,item.weight,count,item.value);
            actionButton(item.icon+"  "+item.name+" ×"+count,desc,false,()->itemDialog(item));
        }
        actionButton("Полный справочник предметов","Поиск, характеристики, вес и описание каждого предмета",true,this::showItemCatalog);
        actionButton("Кодекс и бестиарий","Народы, существа, история и места северных земель",false,()->{tab=6;render();});
        actionButton("Личная библиотека","Материалы, созданные для этой игры, включая способности и существ",false,this::showLibrary);
    }
    private void itemDialog(ItemCatalog.Item item){
        String text=item.icon+"  "+item.name+"\n\nКатегория: "+item.category+
            String.format(java.util.Locale.forLanguageTag("ru"),"\nВес: %.2f кг\nСтоимость: %d септимов",item.weight,item.value)+
            "\n\n"+item.description+"\n\nЭффект: "+item.effect;
        AlertDialog.Builder dialog=new RuneDialogBuilder(this,accent()).setTitle("Сведения о предмете")
            .setMessage(text).setNegativeButton("Закрыть",null);
        if(game.progress.quantity(item.id)>0){
            if("Лечебное зелье".equals(item.id)||"Зелье магии".equals(item.id))
                dialog.setPositiveButton("Использовать",(d,w)->{
                    boolean result="Лечебное зелье".equals(item.id)?game.drinkHealing():game.drinkMana();
                    game.progress.sync(game);save();render();
                    toast(result?"Зелье использовано":"Нет необходимости использовать зелье");
                });
        }
        dialog.show();
    }
    private void showItemCatalog(){
        java.util.ArrayList<ItemCatalog.Item> items=ItemCatalog.all();
        items.sort((a,b)->a.name.compareToIgnoreCase(b.name));
        LinearLayout root=column();root.setPadding(dp(12),dp(8),dp(12),dp(12));
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Найти предмет по названию");
        search.setTextColor(TEXT);search.setHintTextColor(MUTED);root.addView(search,lp(-1,55));
        android.widget.ListView list=new android.widget.ListView(this);
        root.addView(list,lp(-1,440));
        java.util.ArrayList<ItemCatalog.Item> visible=new java.util.ArrayList<>(items);
        android.widget.ArrayAdapter<String> adapter=new android.widget.ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new java.util.ArrayList<String>());
        Runnable refresh=()->{
            String term=search.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
            visible.clear();adapter.clear();
            for(ItemCatalog.Item item:items){
                if(!term.isEmpty()&&!item.name.toLowerCase(java.util.Locale.ROOT).contains(term))continue;
                visible.add(item);adapter.add(item.icon+"  "+item.name+" · "+item.weight+" кг");
            }
            adapter.notifyDataSetChanged();
        };
        list.setAdapter(adapter);refresh.run();
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence x,int start,int count,int after){}
            public void onTextChanged(CharSequence x,int a,int b,int c){refresh.run();}
            public void afterTextChanged(android.text.Editable x){}
        });
        AlertDialog dlg=new RuneDialogBuilder(this,accent()).setTitle("Каталог предметов").setView(root).setNegativeButton("Закрыть",null).create();
        list.setOnItemClickListener((parent,v,pos,id)->{if(pos>=0&&pos<visible.size())itemDialog(visible.get(pos));});
        dlg.show();
    }
    private void showLibrary(){
        java.util.ArrayList<String> docs=ItemCatalog.loreDocuments(this);
        if(docs.isEmpty()){toast("Библиотека не загружена");return;}
        new RuneDialogBuilder(this,accent()).setTitle("Личная библиотека • "+docs.size()+" документов")
            .setItems(docs.toArray(new String[0]),(d,position)->showDocument(docs.get(position)))
            .setNegativeButton("Закрыть",null).show();
    }
    private void showDocument(String file){
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);
        LinearLayout col=column();col.setPadding(dp(17),dp(15),dp(17),dp(18));scroll.addView(col);
        String picture=null;
        if(file.contains("Эликсир Жизни"))picture="Предметы/Эликсир Жизни.webp";
        android.graphics.Bitmap bmp=picture==null?null:ItemCatalog.picture(this,picture);
        if(bmp!=null){
            android.widget.ImageView image=new android.widget.ImageView(this);
            image.setImageBitmap(bmp);image.setAdjustViewBounds(true);
            image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            col.addView(image,lp(-1,260));
        }
        String data=ItemCatalog.document(this,file);
        TextView text=text(data,14,TEXT,false);text.setLineSpacing(dp(4),1.04f);col.addView(text);
        new RuneDialogBuilder(this,accent()).setTitle(file.replace(".txt","")).setView(scroll)
            .setNegativeButton("Закрыть",null).show();
    }
    private void heroEntry(LinearLayout p,String label,String value){
        LinearLayout r=row();p.addView(r,lp(-1,39));
        r.addView(text(label,14,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));
        r.addView(text(value,15,TEXT,true));
    }
    private void renderHero(){
        section("ЛИЧНОЕ ДЕЛО",""+game.name,game.race+" • "+game.heroClass+" • уровень "+game.level);
        content.addView(new NordicArtView(this,NordicArtView.STORY),lp(-1,125));space(content,12);
        LinearLayout c=card();addText(c,"ХАРАКТЕРИСТИКИ",11,accent(),true,9);
        heroEntry(c,"Опыт",game.xp+" / "+(game.level*80));
        heroEntry(c,"Атака",String.valueOf(game.attack));heroEntry(c,"Защита",String.valueOf(game.armor));
        heroEntry(c,"Септимы",game.gold+" септимов");heroEntry(c,"Репутация",String.valueOf(game.reputation));
        heroEntry(c,"Победы",String.valueOf(game.victories));heroEntry(c,"Исследований",String.valueOf(game.explored));
        LinearLayout items=card();addText(items,"СНАРЯЖЕНИЕ",11,accent(),true,12);
        addText(items,"Оружие: "+(game.swordUpgraded?"Закалённый клинок":"Походный клинок"),15,TEXT,false,10);
        addText(items,"Доспех: "+(game.armorUpgraded?"Стальной нагрудник":"Дорожная одежда"),15,TEXT,false,11);
        addText(items,"Лечебные зелья: "+game.potions,14,MUTED,false,6);
        addText(items,"Зелья магии: "+game.manaPotions,14,MUTED,false,0);
        actionButton("Выпить лечебное зелье","+45 здоровья • без боя",false,()->{
            toast(game.drinkHealing()?"Здоровье восстановлено":"Зелье не требуется или закончилось");save();render();});
        actionButton("Выпить зелье магии","+35 магии • без боя",false,()->{
            toast(game.drinkMana()?"Магия восстановлена":"Зелье не требуется или закончилось");game.progress.sync(game);save();render();});
        LinearLayout race=card();addText(race,"НАСЛЕДИЕ РАСЫ",11,accent(),true,12);
        addText(race,game.race+" • пассивно",16,TEXT,true,7);
        addText(race,game.progress.racePassive(game),14,MUTED,false,13);
        addText(race,"Талант",13,accent(),true,5);
        addText(race,game.progress.racePower(game),14,TEXT,false,7);
        actionButton("Применить расовый талант","Доступен один раз в день",false,()->{
            boolean ok=game.progress.useRacePower(game);
            if(ok)save();render();toast(ok?"Расовый талант активирован":"Талант уже применён сегодня");
        });
        LinearLayout stats=card();addText(stats,"БАЗОВЫЕ АТРИБУТЫ",11,accent(),true,10);
        heroEntry(stats,"Здоровье",String.valueOf(game.maxHp));
        heroEntry(stats,"Магия",String.valueOf(game.maxMagicka));
        heroEntry(stats,"Выносливость",String.valueOf(game.maxStamina));
        addText(stats,"Стартовые очки: +"+game.progress.spent[0]+" / +"+game.progress.spent[1]+" / +"+game.progress.spent[2],12,MUTED,false,0);
        addText(content,"НАВЫКИ И ДРЕВА РАЗВИТИЯ",11,accent(),true,12);
        addText(content,"Свободных очков талантов: "+game.progress.perkPoints,13,MUTED,false,13);
        for(int i=0;i<RpgContent.SKILLS.length;i++){
            final int index=i;int lvl=game.progress.skillLevel(i);
            String details=RpgContent.SKILL_DESCRIPTIONS[i]+" • опыт "+game.progress.skillXp[i]+"/"+game.progress.skillNeed(i);
            actionButton(RpgContent.SKILLS[i]+" • "+lvl+(lvl==100?" [ЛЕГЕНДАРНЫЙ]":""),details,false,()->showSkill(index));
        }
        addText(content,"ШКОЛЫ МАГИИ И КРИКИ",11,accent(),true,12);
        for(String record:RpgContent.SHOUTS){
            String[] parts=record.split("\\|",2);
            actionButton("ᚱ  "+parts[0],parts.length>1?parts[1]:"",false,()->new RuneDialogBuilder(this,accent())
                .setTitle(parts[0]).setMessage(parts.length>1?parts[1]:"").setPositiveButton("Закрыть",null).show());
        }
    }
    private void showSkill(int index){
        int lvl=game.progress.skillLevel(index);
        String details=RpgContent.SKILL_DESCRIPTIONS[index]+"\n\nУровень: "+lvl+
            "\nОпыт навыка: "+game.progress.skillXp[index]+" / "+game.progress.skillNeed(index)+
            "\n\nВетка талантов: ученик → адепт → мастер.";
        AlertDialog.Builder builder=new RuneDialogBuilder(this,accent()).setTitle(RpgContent.SKILLS[index])
            .setMessage(details).setNegativeButton("Закрыть",null);
        if(game.progress.perkPoints>0&&lvl>=20)
            builder.setPositiveButton("Развить талант",(d,w)->{
                game.progress.perkPoints--;game.progress.skillExperience(game,index,70);
                save();render();toast("Очко таланта применено");
            });
        builder.show();
    }

    private static final String[][] CODEX_CREATURES={
        {"Дракон","Древнее крылатое существо. Его появление меняет ход истории, а встреча требует подготовки, укрытий и холодной головы."},
        {"Волк","Стайный хищник лесов и дорог. Голодные стаи особенно опасны для одиноких путников."},
        {"Великан","Обитатель степей, пасущий мамонтов. Враждует с незваными гостями, но не всегда нападает первым."},
        {"Морозный тролль","Живёт в заснеженных ущельях. Быстро восстанавливает силы и боится пламени."},
        {"Драугр","Оживший страж древних нордских гробниц. Оружие и сила зависят от ранга погребённого воина."},
        {"Двемерский центурион","Механический исполин, охраняющий подземные залы двемеров. Крайне опасен в ближнем бою."},
        {"Ледяной призрак","Холодное существо на заснеженных перевалах. Трудно заметить во время метели."},
        {"Фалмер","Скрытный житель глубин и старых двемерских построек. Использует яды, ловушки и боевых существ."}
    };
    private void renderCodex(){
        section("ДРЕВНИЕ ЛЕТОПИСИ","Кодекс","Бестиарий, народы, места и истории, которые открывают мир северных земель.");
        content.addView(new NordicArtView(this,NordicArtView.CODEX),lp(-1,190));
        space(content,15);
        String[] groups={"Бестиарий","Народы","История","Места"};
        String selected=groups[Math.max(0,Math.min(groups.length-1,codexCategory))];
        chips(groups,selected,category->{
            for(int i=0;i<groups.length;i++)if(groups[i].equals(category)){codexCategory=i;break;}
            render();
        });
        space(content,14);
        if(codexCategory==0){
            addText(content,"ᛟ  СУЩЕСТВА И ОБИТАТЕЛИ",11,accent(),false,10);
            for(String[] record:CODEX_CREATURES){
                actionButton("◇  "+record[0],record[1],false,()->new RuneDialogBuilder(this,accent())
                    .setTitle(record[0]).setMessage(record[1])
                    .setPositiveButton("Закрыть",null).show());
            }
        }else if(codexCategory==1){
            addText(content,"ᛟ  ДЕСЯТЬ НАРОДОВ",11,accent(),false,10);
            for(int i=0;i<GameEngine.RACES.length;i++){
                final int idx=i;
                actionButton("ᚱ  "+GameEngine.RACES[i],RpgContent.PASSIVES[i],false,()->new RuneDialogBuilder(this,accent())
                    .setTitle(GameEngine.RACES[idx]).setMessage("Наследие: "+RpgContent.PASSIVES[idx]+
                    "\n\nРасовая способность: "+RpgContent.POWERS[idx])
                    .setPositiveButton("Закрыть",null).show());
            }
        }else if(codexCategory==2){
            java.util.ArrayList<String> docs=ItemCatalog.loreDocuments(this);
            addText(content,"ᛟ  ХРОНИКИ И РУКОПИСИ",11,accent(),false,10);
            if(docs.isEmpty())addText(content,"Справочные рукописи пока не загружены.",14,MUTED,false,8);
            for(String file:docs)actionButton("◇  "+file.replace(".txt",""),"Читать рукопись",false,()->showDocument(file));
        }else{
            addText(content,"ᛟ  МЕСТА И ДОРОГИ",11,accent(),false,10);
            java.util.ArrayList<WorldAtlas.Place> places=WorldAtlas.all();
            for(WorldAtlas.Place place:places){
                if(!place.type.contains("Столица")&&!place.type.contains("Поселение")&&
                   !place.type.contains("Деревня"))continue;
                actionButton(WorldAtlas.icon(place.type)+"  "+place.name,place.description,false,()->locationDialog(place));
            }
            actionButton("Открыть полную карту","Отметки, маршруты и расположение героя",true,()->{tab=1;render();});
        }
    }
    private void renderJournal(){
        section("ЛЕТОПИСЬ","Задания","Твои решения формируют собственную историю Скайрима.");
        content.addView(new NordicArtView(this,NordicArtView.TITLE),lp(-1,120));space(content,12);
        LinearLayout dragon=card();addText(dragon,"ОСНОВНАЯ ИСТОРИЯ",11,accent(),true,11);
        addText(dragon,"Драконий след",22,TEXT,true,8);
        addText(dragon,"Глава "+Math.min(6,game.progress.dragonStage+1)+" / 6",13,accent(),true,8);
        addText(dragon,DragonArc.status(game),15,MUTED,false,10);
        actionButton("Продолжить историю","Открыть следующую главу",true,()->{
            game.mode="DRAGON";save();tab=0;render();
        });
        LinearLayout main=card();addText(main,"ДОПОЛНИТЕЛЬНАЯ ИСТОРИЯ",11,accent(),true,11);
        addText(main,"Пепельные печати",22,TEXT,true,8);
        addText(main,"Статус: "+game.questStatus(),13,accent(),false,13);
        addText(main,game.questDetails(),15,MUTED,false,0);
        LinearLayout side=card();addText(side,"ДОПОЛНИТЕЛЬНЫЕ ЗАДАНИЯ",11,accent(),true,11);
        addText(side,"Опасные дороги",20,TEXT,true,8);
        String sideStatus=game.sideQuest==0?"Нет активного заказа. Возьми его у доски объявлений в любом городе.":
                game.sideQuest==2?"Предложение доступно. Вернись к доске, чтобы принять его.":
                "Победы над противниками: "+game.sideKills+"/3"+(game.sideKills>=3?". Награда ждёт у доски.":".");
        addText(side,sideStatus,15,MUTED,false,0);
        LinearLayout finishedQuests=card();addText(finishedQuests,"ЗАВЕРШЁННЫЕ ЗАДАНИЯ",11,accent(),true,9);
        if(game.progress.dragonStage==DragonArc.FINAL_STAGE)
            addText(finishedQuests,"✓  Драконий след • первое испытание",14,TEXT,false,8);
        if(game.questStage==4)addText(finishedQuests,"✓  Пепельные печати",14,TEXT,false,8);
        if(game.progress.dragonStage<DragonArc.FINAL_STAGE)
            addText(finishedQuests,"Драконий след: ещё не завершён.",13,MUTED,false,8);
        if(game.questStage!=4)addText(finishedQuests,"История хранителей ещё не завершена.",13,MUTED,false,8);
        addText(finishedQuests,"Заказы доски после сдачи не сохраняются в отдельном архиве.",12,MUTED,false,0);
        LinearLayout failedQuests=card();addText(failedQuests,"ПРОВАЛЕННЫЕ ЗАДАНИЯ",11,0xffeb8e8e,true,9);
        addText(failedQuests,"Пока нет проваленных сюжетных заданий.",13,MUTED,false,0);
        LinearLayout done=card();addText(done,"ИСТОРИЯ УСПЕШНЫХ ПРОВЕРОК",11,accent(),true,9);
        if(game.progress.completed.isEmpty())addText(done,"Пока нет успешных проверок.",13,MUTED,false,0);
        for(String entry:game.progress.completed)addText(done,"✓  "+entry,13,TEXT,false,8);
        LinearLayout failed=card();addText(failed,"ИСТОРИЯ НЕУДАЧНЫХ ПРОВЕРОК",11,0xffeb8e8e,true,9);
        if(game.progress.failed.isEmpty())addText(failed,"Пока нет неудачных проверок.",13,MUTED,false,0);
        for(String entry:game.progress.failed)addText(failed,"✕  "+entry,13,MUTED,false,8);
        actionButton("Вернуться к истории","Открыть текущую сцену",true,()->{tab=0;render();});
    }
    private void renderSettings(){
        section("ПАРАМЕТРЫ","Настройки","Твоя история, твои правила.");
        content.addView(new NordicArtView(this,NordicArtView.STORY),lp(-1,105));space(content,12);
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
        actionButton("Проверить обновления","Проверка подписи, история изменений и установка внутри приложения",true,()->updater.check(true));
        LinearLayout otaOptions=card();addText(otaOptions,"АВТОМАТИЧЕСКИЕ ПРОВЕРКИ",11,accent(),true,10);
        Switch onLaunch=new Switch(this);onLaunch.setText("Проверять при запуске");onLaunch.setTextSize(sp(14));
        onLaunch.setTextColor(TEXT);onLaunch.setChecked(prefs.getBoolean(OtaUpdateSchedule.PREF_ON_LAUNCH,true));
        onLaunch.setThumbTintList(ColorStateList.valueOf(accent()));
        onLaunch.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean(OtaUpdateSchedule.PREF_ON_LAUNCH,on).apply());
        otaOptions.addView(onLaunch,lp(-1,47));
        Switch background=new Switch(this);background.setText("Проверять в фоне");background.setTextSize(sp(14));
        background.setTextColor(TEXT);background.setThumbTintList(ColorStateList.valueOf(accent()));
        boolean bgEnabled=prefs.getBoolean(OtaUpdateSchedule.PREF_BACKGROUND,false);
        background.setChecked(bgEnabled);
        background.setOnCheckedChangeListener((v,on)->{
            prefs.edit().putBoolean(OtaUpdateSchedule.PREF_BACKGROUND,on).apply();
            OtaUpdateSchedule.configure(this);
            if(on&&android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},917);
            render();
        });
        otaOptions.addView(background,lp(-1,47));
        if(bgEnabled){
            long selected=prefs.getLong(OtaUpdateSchedule.PREF_FREQUENCY,24L);
            int index=4;
            for(int i=0;i<OtaUpdateSchedule.INTERVAL_HOURS.length;i++)
                if(OtaUpdateSchedule.INTERVAL_HOURS[i]==selected)index=i;
            TextView frequency=text("Интервал: "+OtaUpdateSchedule.INTERVAL_NAMES[index],13,MUTED,false);
            otaOptions.addView(frequency);
            android.widget.SeekBar seek=new android.widget.SeekBar(this);
            seek.setMax(OtaUpdateSchedule.INTERVAL_HOURS.length-1);seek.setProgress(index);
            seek.setProgressTintList(ColorStateList.valueOf(accent()));
            seek.setThumbTintList(ColorStateList.valueOf(accent()));
            seek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
                public void onStartTrackingTouch(android.widget.SeekBar bar){}
                public void onProgressChanged(android.widget.SeekBar bar,int p,boolean fromUser){
                    frequency.setText("Интервал: "+OtaUpdateSchedule.INTERVAL_NAMES[p]);
                }
                public void onStopTrackingTouch(android.widget.SeekBar bar){
                    long hours=OtaUpdateSchedule.INTERVAL_HOURS[bar.getProgress()];
                    prefs.edit().putLong(OtaUpdateSchedule.PREF_FREQUENCY,hours).apply();
                    OtaUpdateSchedule.configure(MainActivity.this);
                }
            });
            otaOptions.addView(seek,lp(-1,46));
            String result=prefs.getString("ota_background_result","");
            if(!result.isEmpty())addText(otaOptions,result,12,MUTED,false,0);
        }
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
        new RuneDialogBuilder(this,accent()).setTitle("Восстановление").setView(e).setNegativeButton("Отмена",null)
            .setPositiveButton("Загрузить",(d,w)->{
                try{JSONObject json=new JSONObject(e.getText().toString());if(json.optInt("schema")!=1 || !json.optBoolean("started"))throw new Exception();
                    game=GameEngine.fromJson(json.toString());tab=0;save();render();toast("Сохранение восстановлено");
                }catch(Exception ex){toast("Неверный код сохранения");}
            }).show();
    }
    private void resetDialog(){
        new RuneDialogBuilder(this,accent()).setTitle("Начать заново?")
            .setMessage("Текущее сохранение будет заменено. При необходимости сначала скопируй код сохранения.")
            .setNegativeButton("Отмена",null).setPositiveButton("Новая игра",(d,w)->{
                game=new GameEngine();tab=0;draftName="";selectedStart=0;java.util.Arrays.fill(initialPoints,0);save();render();}).show();
    }
    private void renderNav(){
        View sep=new View(this);sep.setBackgroundColor(BORDER);root.addView(sep,lp(-1,1));
        LinearLayout nav=row();nav.setPadding(dp(6),dp(7),dp(6),dp(7));nav.setBackgroundColor(PANEL);
        root.addView(nav,lp(-1,65));
        String[] labels={"ИСТОРИЯ","КАРТА","СУМКА","КОДЕКС","ГЕРОЙ","ЖУРНАЛ","ТЕМА"};
        String[] signs={"ᚱ","◈","▣","ᛟ","◇","☷","⚙"};
        int[] tabs={0,1,2,6,3,4,5};
        for(int i=0;i<labels.length;i++){
            final int index=tabs[i];
            LinearLayout navitem=column();navitem.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-1,1);nav.addView(navitem,size);
            TextView icon=text(signs[i],23,tab==index?accent():MUTED,true);icon.setGravity(Gravity.CENTER);navitem.addView(icon);
            TextView t=text(labels[i],8,tab==index?accent():MUTED,tab==index);t.setGravity(Gravity.CENTER);navitem.addView(t);
            navitem.setOnClickListener(v->{if(tab!=index){tab=index;render();}});
        }
    }
    public void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    int currentAccent(){return accent();}
    @Override public void onBackPressed(){if(titleOpen){super.onBackPressed();return;}
        if(!game.started||tab==5||tab==6){titleOpen=true;render();return;}
        if(game.started&&tab!=0){tab=0;render();return;}
        if(game.started&&!game.mode.equals("WORLD")&&!game.mode.equals("COMBAT")){game.choose("GO_WORLD");save();render();return;}
        super.onBackPressed();}
}
