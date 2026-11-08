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
    // Monochrome translucent menus, following the six provided Skyrim references.
    private static final int BG=0xff06090a, PANEL=0xe806090b, PANEL_2=0xf30b0e10;
    private static final int TEXT=0xfff0f1ef, MUTED=0xffaab0b3, BORDER=0xff666d72;
    private static final int[] ACCENTS={0xffe1e3e3,0xffa4b9bf,0xffb9c9dc,0xffceb48c,0xffc79a97,0xffeeece8};
    private static final String[] ACCENT_NAMES={"Серебро", "Север", "Лёд", "Древнее золото", "Рубин", "Снег"};
    private static final String PREFS="skyrim_text_v1";
    private GameEngine game;
    private SharedPreferences prefs;
    private LinearLayout root, content, header;
    private ScrollView scroll;
    private String renderedPage="";
    private static final class PageScroll {
        final int y; final java.util.ArrayList<Integer> horizontal;
        PageScroll(int y,java.util.ArrayList<Integer> horizontal){this.y=y;this.horizontal=horizontal;}
    }
    private final java.util.Map<String,PageScroll> scrollCache=new java.util.HashMap<>();
    private EditText nameField;
    private int tab=0, accentIndex=0, fontBonus=0;
    private String draftName="", selectedRace="Норд", selectedClass="Без класса";
    private int selectedStart=0,selectedMapRegion=-1;
    private final int[] initialPoints={0,0,0};
    private String selectedCategory="Все",selectedSort="Название";
    private String focusedInventoryId="";
    private static final String SKILL_HUD_OWNER="skill_hud_owner";
    private static final String SKILL_HUD_SNAPSHOT="skill_hud_snapshot";
    private boolean animations=true, titleOpen=true;
    private int codexCategory=0;
    private SkyrimOtaController updater;
    private ChatGptOAuth chatGptOAuth;
    private boolean awaitingAiToken;
    private AiStoryMemory aiMemory;
    private AiStoryClient aiClient;
    private String aiSession;
    private static final String AI_PENDING_ACTION="ai_pending_action";
    private static final String AI_PENDING_CHANCE="ai_pending_chance";
    private static final String AI_PENDING_DIE="ai_pending_die";
    private static final String AI_PENDING_SCENE="ai_pending_scene_hash";
    private static final String AI_PENDING_SESSION="ai_pending_session";
    private boolean aiTurnBusy;
    private String aiTurnPreview="",aiTurnStatus="";
    private TextView aiTurnText;


    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        aiSession=prefs.getString("ai_session","");
        if(aiSession.isEmpty()){
            aiSession=java.util.UUID.randomUUID().toString();
            prefs.edit().putString("ai_session",aiSession).apply();
        }
        aiMemory=new AiStoryMemory(this);
        aiClient=new AiStoryClient();
        chatGptOAuth=new ChatGptOAuth(this);
        ItemCatalog.initialize(this);
        SovngardeTypeface.preload(this);
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
    @Override protected void onDestroy(){
        if(aiClient!=null)aiClient.shutdown();
        if(chatGptOAuth!=null)chatGptOAuth.shutdown();
        if(aiMemory!=null)aiMemory.close();
        super.onDestroy();
    }
    private void newAiSession(){
        if(aiClient!=null)aiClient.cancel();
        aiSession=java.util.UUID.randomUUID().toString();
        prefs.edit().putString("ai_session",aiSession).apply();
    }
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
        t.setTypeface(SovngardeTypeface.face(this,bold));
        return t;
    }
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void space(LinearLayout parent,int h){View v=new View(this);parent.addView(v,lp(1,h));}
    private void addText(LinearLayout parent,String str,int size,int color,boolean bold,int bottom){TextView t=text(str,size,color,bold);parent.addView(t);if(bottom>0)space(parent,bottom);}
    private String viewportKey(){
        if(titleOpen)return "title";
        return (!game.started?"new-":"tab-")+tab;
    }
    private void findHorizontal(View node,java.util.List<HorizontalScrollView> found){
        if(node instanceof HorizontalScrollView)found.add((HorizontalScrollView)node);
        if(node instanceof ViewGroup){
            ViewGroup parent=(ViewGroup)node;
            for(int i=0;i<parent.getChildCount();i++)
                findHorizontal(parent.getChildAt(i),found);
        }
    }
    private void rememberViewport(){
        if(scroll==null||renderedPage.isEmpty())return;
        java.util.ArrayList<HorizontalScrollView> found=new java.util.ArrayList<>();
        findHorizontal(scroll,found);
        java.util.ArrayList<Integer> positions=new java.util.ArrayList<>();
        for(HorizontalScrollView h:found)positions.add(h.getScrollX());
        scrollCache.put(renderedPage,new PageScroll(scroll.getScrollY(),positions));
    }
    private void resumeViewport(PageScroll previous,ScrollView target,String key){
        if(previous==null)return;
        // Apply the restored offsets once layout has its final measured widths.
        // The user never sees the new screen at scroll=0 before restoration.
        target.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){
            @Override public boolean onPreDraw(){
                target.getViewTreeObserver().removeOnPreDrawListener(this);
                if(scroll!=target||!viewportKey().equals(key))return true;
                java.util.ArrayList<HorizontalScrollView> found=new java.util.ArrayList<>();
                findHorizontal(target,found);
                for(int i=0;i<Math.min(found.size(),previous.horizontal.size());i++){
                    HorizontalScrollView h=found.get(i);
                    View child=h.getChildAt(0);
                    int limit=child==null?0:Math.max(0,child.getWidth()-h.getWidth());
                    h.scrollTo(Math.min(Math.max(0,previous.horizontal.get(i)),limit),0);
                }
                View inner=target.getChildAt(0);
                int limit=inner==null?0:Math.max(0,inner.getHeight()-target.getHeight());
                target.scrollTo(0,Math.min(Math.max(0,previous.y),limit));
                return true;
            }
        });
    }
    private void render(){
        rememberViewport();
        String thisPage=viewportKey();
        PageScroll oldScroll=scrollCache.get(thisPage);
        root=column();root.setBackground(new SkyrimBackdropDrawable());root.setFitsSystemWindows(true);
        setContentView(root);
        if(!titleOpen){
            renderHeader();
            if(game.started)renderSkillLevelNotice();
            if(game.started && !(tab==0&&prefs.getBoolean("ai_enabled",false)))renderBars();
        }
        scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        // Avoid automatic descendant focus scrolling to an arbitrary new child.
        scroll.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        scroll.setFocusableInTouchMode(true);
        scroll.requestFocus();
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=column();content.setPadding(dp(13),dp(17),dp(13),dp(28));scroll.addView(content);
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
        SovngardeTypeface.applyTree(root);
        renderedPage=thisPage;
        resumeViewport(oldScroll,scroll,thisPage);
        if(animations){content.setAlpha(0.8f);content.animate().alpha(1f).setDuration(210).setInterpolator(new DecelerateInterpolator()).start();}
    }
    private void renderHeader(){
        header=row();header.setPadding(dp(13),dp(9),dp(13),dp(9));
        header.setBackground(new RuneFrameDrawable(PANEL,BORDER,false));
        root.addView(header,lp(-1,-2));
        SkyrimIconView crest=new SkyrimIconView(this,"dragon",accent());
        header.addView(crest,lp(42,42));
        crest.setOnClickListener(v->{titleOpen=true;render();});
        crest.setContentDescription("Открыть главное меню");
        LinearLayout label=column();label.setPadding(dp(12),0,0,0);
        header.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        TextView word=text("S K Y R I M",20,TEXT,true);word.setLetterSpacing(.08f);label.addView(word);
        addText(label,"ᛟ  ТЕКСТОВАЯ RPG",10,MUTED,false,0);
        if(game.started){
            LinearLayout state=column();state.setGravity(Gravity.END);
            addText(state,"ДЕНЬ "+game.day+"  ·  "+String.format(java.util.Locale.ROOT,"%02d:00",game.hour),11,TEXT,true,2);
            addText(state,"УРОВЕНЬ "+game.level,10,MUTED,false,0);
            header.addView(state,lp(-2,-2));
            if(!prefs.getBoolean("ai_enabled",false)){
                state.setContentDescription("Ожидать и изменить игровое время");
                state.setOnClickListener(v->showWaitDialog());
            }
        }
        root.addView(new SkyrimRuleView(this,accent()),lp(-1,15));
    }
    /**
     * Compact level-up HUD taken from the Skyrim skill-notification reference.
     * Only genuine skill-level increases trigger a notice. Previously loaded
     * saves establish their first snapshot silently, without fake level-ups.
     */
    private void renderSkillLevelNotice(){
        String owner=game.name+"|"+game.race+"|"+game.heroClass;
        StringBuilder saved=new StringBuilder();
        for(int i=0;i<RpgContent.SKILLS.length;i++){
            if(i>0)saved.append(',');
            saved.append(game.progress.skillLevel(i));
        }
        String snapshot=saved.toString();
        String prior=prefs.getString(SKILL_HUD_SNAPSHOT,"");
        String previousOwner=prefs.getString(SKILL_HUD_OWNER,"");
        prefs.edit().putString(SKILL_HUD_OWNER,owner)
            .putString(SKILL_HUD_SNAPSHOT,snapshot).apply();
        if(prior.isEmpty()||!owner.equals(previousOwner))return;
        String[] previous=prior.split(",",-1);
        if(previous.length!=RpgContent.SKILLS.length)return;
        int raised=-1;
        for(int i=0;i<previous.length;i++){
            int oldLevel;
            try{oldLevel=Integer.parseInt(previous[i]);}
            catch(NumberFormatException ex){return;}
            if(oldLevel>0&&game.progress.skillLevel(i)>oldLevel&&raised<0)raised=i;
        }
        if(raised<0)return;
        int i=raised;
        LinearLayout panel=column();
        panel.setPadding(dp(13),dp(11),dp(13),dp(11));
        panel.setBackground(new RuneFrameDrawable(0xf0090c0d,0xffbac5c9,true));
        LinearLayout.LayoutParams area=lp(-1,-2);
        area.setMargins(dp(10),dp(2),dp(10),dp(6));
        root.addView(panel,area);
        LinearLayout top=row();panel.addView(top,lp(-1,-2));
        top.addView(new SkyrimIconView(this,"armor",accent()),lp(37,40));
        LinearLayout words=column();
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);
        wp.leftMargin=dp(12);top.addView(words,wp);
        TextView label=text(RpgContent.SKILLS[i].toUpperCase(java.util.Locale.ROOT),14,TEXT,true);
        label.setLetterSpacing(.11f);words.addView(label);
        addText(words,"НАВЫК ПОВЫШЕН ДО "+game.progress.skillLevel(i),11,MUTED,false,0);
        TextView seal=text(String.valueOf(game.progress.skillLevel(i)),21,accent(),true);
        seal.setGravity(Gravity.CENTER);
        seal.setBackground(new RuneFrameDrawable(0x33000000,accent(),true));
        top.addView(seal,lp(39,39));
        ProgressBar line=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        line.setMax(Math.max(1,game.progress.skillNeed(i)));
        line.setProgress(Math.max(0,game.progress.skillXp[i]));
        line.setProgressTintList(ColorStateList.valueOf(accent()));
        line.setProgressBackgroundTintList(ColorStateList.valueOf(0xff30383c));
        LinearLayout.LayoutParams lineParams=lp(-1,3);
        lineParams.topMargin=dp(9);panel.addView(line,lineParams);
        panel.setContentDescription(RpgContent.SKILLS[i]+" повышен до уровня "+game.progress.skillLevel(i));
        if(animations){
            panel.setAlpha(0f);
            panel.animate().alpha(1f).setDuration(250).start();
        }
        panel.postDelayed(()->{
            panel.animate().alpha(0f).setDuration(420).withEndAction(()->{
                if(panel.getParent() instanceof android.view.ViewGroup)
                    ((android.view.ViewGroup)panel.getParent()).removeView(panel);
            }).start();
        },3400);
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
        TextView over=text(eyebrow.toUpperCase(java.util.Locale.ROOT),10,MUTED,true);
        over.setGravity(Gravity.CENTER);over.setLetterSpacing(.16f);
        content.addView(over,lp(-1,24));
        TextView title=text(heading.toUpperCase(java.util.Locale.ROOT),27,TEXT,true);
        title.setGravity(Gravity.CENTER);title.setLetterSpacing(.04f);content.addView(title,lp(-1,-2));
        content.addView(new SkyrimRuleView(this,accent()),lp(-1,19));
        if(subtitle!=null&&!subtitle.isEmpty()){
            TextView line=text(subtitle,14,MUTED,false);
            line.setGravity(Gravity.CENTER);line.setLineSpacing(dp(3),1.08f);
            content.addView(line,lp(-1,-2));
        }
        space(content,12);
    }
    private LinearLayout card(){
        LinearLayout c=column();c.setPadding(dp(16),dp(18),dp(16),dp(18));
        c.setBackground(new RuneFrameDrawable(PANEL,0xff929a9e,false));
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(11);content.addView(c,p);return c;
    }
    private static String iconOf(String title){
        String t=title.toLowerCase(java.util.Locale.ROOT);
        if(t.contains("карта")||t.contains("мест")||t.contains("локац")||t.contains("путь"))return "map";
        if(t.contains("оруж")||t.contains("меч")||t.contains("боев"))return "weapon";
        if(t.contains("доспех")||t.contains("брон")||t.contains("щит"))return "armor";
        if(t.contains("зель")||t.contains("лекар")||t.contains("алхим"))return "potion";
        if(t.contains("герой")||t.contains("характерист")||t.contains("навык"))return "hero";
        if(t.contains("квест")||t.contains("задан")||t.contains("журнал"))return "journal";
        if(t.contains("настр")||t.contains("тема")||t.contains("ии-")||t.contains("модель"))return "settings";
        if(t.contains("инвентар")||t.contains("сумк")||t.contains("предмет"))return "bag";
        if(t.contains("библиотек")||t.contains("кодекс")||t.contains("книга"))return "codex";
        if(t.contains("дракон")||t.contains("скайрим"))return "dragon";
        return "star";
    }
    private TextView actionButton(String title,String detail,boolean primary,Runnable onClick){
        LinearLayout row=row();row.setPadding(dp(12),dp(11),dp(10),dp(11));
        row.setBackground(new RuneFrameDrawable(primary?0xf0181a1c:PANEL,primary?accent():BORDER,primary));
        row.addView(new SkyrimIconView(this,iconOf(title),primary?accent():0xffbfc7cb),lp(28,29));
        LinearLayout info=column();LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1);
        ip.leftMargin=dp(11);row.addView(info,ip);
        addText(info,title,16,primary?accent():TEXT,true,detail.isEmpty()?0:4);
        if(!detail.isEmpty())addText(info,detail,12,MUTED,false,0);
        TextView mark=text("◇",19,primary?accent():MUTED,false);
        mark.setGravity(Gravity.CENTER);row.addView(mark,lp(24,28));
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(7);content.addView(row,p);
        row.setClickable(true);row.setFocusable(true);row.setContentDescription(title+". "+detail);
        row.setOnClickListener(v->onClick.run());return mark;
    }
    private void titleButton(String caption,boolean active,Runnable callback){
        TextView item=text(caption.toUpperCase(java.util.Locale.ROOT),19,active?accent():TEXT,true);
        item.setGravity(Gravity.CENTER);item.setLetterSpacing(.12f);
        item.setBackground(new RuneFrameDrawable(active?0xf0121516:PANEL,active?accent():BORDER,active));
        LinearLayout.LayoutParams p=lp(-1,50);p.setMargins(dp(28),0,dp(28),dp(9));
        content.addView(item,p);item.setClickable(true);item.setFocusable(true);
        item.setOnClickListener(v->callback.run());
    }
    private void titleRule(){content.addView(new SkyrimRuleView(this,accent()),lp(-1,29));}
    private void skyrimTopTabs(int current){
        final int[] screens={4,3,5};
        final String[] names={"ЗАДАНИЯ","СТАТИСТИКА","СИСТЕМА"};
        LinearLayout bar=row();bar.setPadding(dp(3),dp(5),dp(3),dp(5));
        bar.setBackground(new RuneFrameDrawable(PANEL,BORDER,false));
        content.addView(bar,lp(-1,-2));
        for(int i=0;i<screens.length;i++){
            final int index=screens[i];
            TextView title=text(names[i],12,current==index?accent():MUTED,current==index);
            title.setGravity(Gravity.CENTER);
            bar.addView(title,new LinearLayout.LayoutParams(0,dp(38),1));
            title.setOnClickListener(v->{tab=index;render();});
        }
        content.addView(new SkyrimRuleView(this,accent()),lp(-1,14));
    }
    private void renderTitle(){
        content.setPadding(0,dp(8),0,dp(25));
        FrameLayout cover=new FrameLayout(this);content.addView(cover,lp(-1,340));
        cover.addView(new NordicArtView(this,NordicArtView.TITLE),
            new FrameLayout.LayoutParams(-1,-1));
        LinearLayout letter=column();letter.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams overlay=new FrameLayout.LayoutParams(-1,-1);overlay.setMargins(dp(8),0,dp(8),dp(25));cover.addView(letter,overlay);
        SkyrimIconView rune=new SkyrimIconView(this,"dragon",accent());
        LinearLayout.LayoutParams logo=lp(82,82);logo.gravity=Gravity.CENTER;
        letter.addView(rune,logo);
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
            newAiSession();game.start(nameField.getText().toString(),selectedRace,selectedClass,selectedStart,initialPoints);
            tab=0;save();render();
        });
        addText(content,"Неофициальная самостоятельная история в знакомом северном мире.",12,MUTED,false,0);
    }
    private android.widget.Spinner spinner(String[] options,int position,java.util.function.IntConsumer onChange){
        android.widget.Spinner sp=new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> adapter=new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,options){
            @Override public View getView(int pos,View old,ViewGroup parent){
                TextView t=(TextView)super.getView(pos,old,parent);t.setTextColor(TEXT);t.setTextSize(15);t.setTypeface(SovngardeTypeface.face(MainActivity.this,false));t.setPadding(dp(12),dp(6),dp(9),dp(6));return t;
            }
            @Override public View getDropDownView(int pos,View old,ViewGroup parent){
                TextView t=(TextView)super.getDropDownView(pos,old,parent);t.setTextColor(TEXT);
                t.setTypeface(SovngardeTypeface.face(MainActivity.this,false));t.setBackgroundColor(PANEL);t.setPadding(dp(15),dp(15),dp(10),dp(15));return t;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        final int initial=Math.max(0,Math.min(position,options.length-1));
        sp.setSelection(initial);
        sp.setBackground(new RuneFrameDrawable(PANEL_2,0xff85745f,false));
        final int[] previous={initial};
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> v){}
            public void onItemSelected(android.widget.AdapterView<?> v,View view,int pos,long id){
                // Android calls onItemSelected once during initial layout.
                // That is not a user choice and must not rebuild the whole page.
                if(pos==previous[0])return;
                previous[0]=pos;
                onChange.accept(pos);
            }
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
        if(prefs.getBoolean("ai_enabled",false)){renderAiGame();return;}
        GameEngine.Scene s=game.scene();section(s.kicker,s.title,"");
        renderStorySnapshot();
        if(LocationGalleryView.hasGallery(game.progress.location)){
            addText(content,"ᛟ  ВИДЫ ЛОКАЦИИ",11,accent(),true,9);
            content.addView(new LocationGalleryView(this,game.progress.location,accent(),false),lp(-1,-2));
        }else{
            content.addView(new NordicArtView(this,NordicArtView.STORY),lp(-1,188));
        }
        space(content,16);
        LinearLayout narrative=card();
        addText(narrative,"ᚱ  ГЛАВА  ·  "+s.kicker.toUpperCase(java.util.Locale.ROOT),11,accent(),false,13);
        TextView body=text("",18,TEXT,false);
        body.setTypeface(SovngardeTypeface.face(this,false));
        body.setText(SceneTypography.render(s.body,accent()));
        body.setLineSpacing(dp(7),1.16f);
        makeItemsClickable(body,body.getText().toString());
        narrative.addView(body);
        renderStoryJournal();
        if(prefs.getBoolean("ai_enabled",false)) renderAiStoryPanel();
        space(content,12);
        addText(content,"Что ты предпримешь?",22,TEXT,true,7);
        addText(content,s.choices.size()+" доступных вариантов. Вероятность показана только для настоящих проверок.",12,MUTED,false,15);
        int number=0;
        for(GameEngine.Choice c:s.choices){
            storyChoice(++number,c,()->{
                if(aiClient!=null)aiClient.cancel();
                int old=game.questStage;
                game.choose(c.action);
                if(!c.action.startsWith("ACT:"))game.progress.observe(game,c.action,old);
                game.progress.sync(game);
                if(prefs.getBoolean("ai_enabled",false))
                    aiMemory.add(aiSession,"event","Игровое действие: "+c.title+". Текущее сообщение движка: "+game.message);
                save();tab=0;render();
            });
        }
        space(content,5);
        addText(content,"Каждое действие сохраняется автоматически",11,MUTED,false,0);
    }

    private void renderStorySnapshot(){
        addText(content,game.name+" · "+game.race+" · Уровень "+game.level,17,TEXT,true,7);
        addText(content,"⌖ "+game.progress.location+"  ·  День "+game.day+"  ·  Опыт "+game.xp,13,MUTED,false,7);
        addText(content,"Вес: "+
            String.format(java.util.Locale.forLanguageTag("ru"),"%.1f",game.progress.carryWeight())+
            "/50 кг  ·  "+game.gold+" септимов",13,accent(),false,12);
    }

    private void renderStoryJournal(){
        if(!game.started)return;
        if(!"QUEST".equals(game.mode)&&!"DRAGON".equals(game.mode)
            &&!"EVENT".equals(game.mode)&&!"RESULT".equals(game.mode))return;
        LinearLayout journal=card();
        addText(journal,"ᚱ  ЖУРНАЛ ПРИКЛЮЧЕНИЙ",11,accent(),true,10);
        if(game.questStage>0&&game.questStage<4)
            addText(journal,"◉  Пепельные печати · "+game.questStatus(),13,TEXT,false,7);
        if(game.progress.dragonStage>=0&&game.progress.dragonStage<DragonArc.FINAL_STAGE)
            addText(journal,"◉  Драконий след · "+DragonArc.status(game),13,TEXT,false,7);
        if(game.sideQuest==1)
            addText(journal,"◉  Опасные дороги · "+game.sideKills+"/3",13,MUTED,false,7);
        if("EVENT".equals(game.mode))
            addText(journal,"!  Случайная встреча · исход ещё неизвестен",13,0xfff18e60,true,1);
    }

    private void storyChoice(int position,GameEngine.Choice c,Runnable onClick){
        int chance=game.progress.chance(game,c.action);
        LinearLayout box=row();box.setGravity(Gravity.TOP);
        box.setPadding(dp(12),dp(13),dp(12),dp(14));
        box.setBackground(new RuneFrameDrawable(PANEL,c.special?accent():BORDER,c.special));
        TextView ordinal=text("◇ "+position,18,c.special?accent():MUTED,true);
        ordinal.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        box.addView(ordinal,lp(47,-2));
        LinearLayout prose=column();
        LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-2,1);
        size.leftMargin=dp(5);box.addView(prose,size);
        TextView name=text(c.title,17,TEXT,true);
        name.setTypeface(SovngardeTypeface.face(this,true));
        name.setLineSpacing(dp(3),1.1f);prose.addView(name);
        if(c.detail!=null&&!c.detail.isEmpty()){
            space(prose,5);
            TextView desc=text(c.detail,14,MUTED,false);
            desc.setLineSpacing(dp(3),1.12f);prose.addView(desc);
        }
        space(prose,6);
        if(chance>=0){
            addText(prose,SceneChoiceMeta.skill(c.action),12,MUTED,false,4);
            addText(prose,"ШАНС УСПЕХА  "+chance+"%",13,SceneChoiceMeta.chanceColor(chance),true,0);
        }else addText(prose,"БЕЗ ПРОВЕРКИ",11,0xffb8c1c4,false,0);
        LinearLayout.LayoutParams layout=lp(-1,-2);layout.bottomMargin=dp(5);
        content.addView(box,layout);
        box.setClickable(true);box.setFocusable(true);
        box.setOnClickListener(view->onClick.run());
    }

    /** A failed network continuation must not reroll an already evaluated action. */
    private String pendingAiAction(){
        if(game.aiTurn==null)return "";
        if(prefs.getInt(AI_PENDING_SCENE,0)!=game.aiTurn.scene.hashCode())return "";
        if(!aiSession.equals(prefs.getString(AI_PENDING_SESSION,"")))return "";
        return prefs.getString(AI_PENDING_ACTION,"");
    }
    private void clearAiPending(){
        prefs.edit().remove(AI_PENDING_ACTION).remove(AI_PENDING_CHANCE)
            .remove(AI_PENDING_DIE).remove(AI_PENDING_SCENE)
            .remove(AI_PENDING_SESSION).apply();
    }
    private void storeAiPending(String action,int chance,int die){
        prefs.edit().putString(AI_PENDING_ACTION,action)
            .putInt(AI_PENDING_CHANCE,chance).putInt(AI_PENDING_DIE,die)
            .putInt(AI_PENDING_SCENE,game.aiTurn==null?0:game.aiTurn.scene.hashCode())
            .putString(AI_PENDING_SESSION,aiSession).apply();
    }
    private String lockedFreeInstruction(String action,int chance,int die){
        String verdict=chance<0?"Проверка не требуется, действие гарантировано.":
            "ИИ ранее зафиксировал шанс "+chance+"%, Android уже бросил "+die+
            " из 100. Результат "+(AiGameTurn.success(chance,die)?"УСПЕХ":"НЕУДАЧА")+".";
        return "СВОБОДНОЕ ДЕЙСТВИЕ ИГРОКА: "+action+"\n"+verdict+
            " НЕ пересчитывай вероятность и НЕ меняй исход. Верни customChance ровно "+
            (chance<0?"null":String.valueOf(chance))+
            ", опиши только этот установленный результат, effects и 10 новых вариантов.";
    }

    /** AI mode replaces the entire offline story, not just its dialogue panel. */
    private void renderAiGame(){
        boolean chat=usingChatGpt();
        boolean ready=chat?chatGptOAuth.hasAuthorizedPlan():AiKeyVault.configured(this);
        aiTurnText=null;
        if(!ready){
            LinearLayout card=card();
            addText(card,"ИИ-ИГРА НЕ ПОДКЛЮЧЕНА",14,accent(),true,11);
            addText(card,"Подключи выбранный источник в настройках. Обычный текст игры скрыт, поскольку выбран режим ИИ.",14,TEXT,false,13);
            actionButton("НАСТРОИТЬ ИИ","Вход ChatGPT или ключ OpenAI API",true,()->{tab=5;render();});
            return;
        }
        if(aiTurnBusy) {
            addText(content,"ᛟ  ИИ СОЗДАЁТ ПРОДОЛЖЕНИЕ",12,accent(),true,10);
            if(!aiTurnStatus.isEmpty())addText(content,aiTurnStatus,12,MUTED,false,12);
            LinearLayout frame=card();
            aiTurnText=text("",17,TEXT,false);
            aiTurnText.setTypeface(SovngardeTypeface.face(this,false));
            aiTurnText.setLineSpacing(dp(7),1.15f);
            aiTurnText.setText(SceneTypography.render(aiTurnPreview.isEmpty()?
                    "…":aiTurnPreview,accent()));
            frame.addView(aiTurnText,lp(-1,-2));
            actionButton("ОСТАНОВИТЬ ГЕНЕРАЦИЮ","Текущая сцена и характеристики останутся прежними",false,()->{
                if(aiClient!=null)aiClient.cancel();
                awaitingAiToken=false;aiTurnBusy=false;aiTurnPreview="";
                aiTurnStatus="Генерация остановлена, изменения не сохранены";
                render();
            });
            return;
        }
        if(!aiTurnStatus.isEmpty())addText(content,aiTurnStatus,12,MUTED,false,11);
        if(game.aiTurn==null){
            addText(content,"ИИ-Мастер создаст первую сцену из истории твоего героя. Затем появятся десять вариантов действий и поле собственного ответа.",15,TEXT,false,15);
            actionButton("НАЧАТЬ ИИ-ПРИКЛЮЧЕНИЕ","Сгенерировать первую сцену и десять решений",true,()->
                requestAiTurn("Начни историю моего героя в текущем месте. Это первый ход: не меняй параметры, предоставь ten choices.",false));
            return;
        }
        LinearLayout narrative=card();
        aiTurnText=text("",18,TEXT,false);
        aiTurnText.setTypeface(SovngardeTypeface.face(this,false));
        aiTurnText.setLineSpacing(dp(7),1.16f);
        aiTurnText.setText(SceneTypography.render(game.aiTurn.scene,accent()));
        makeItemsClickable(aiTurnText,aiTurnText.getText().toString());
        narrative.addView(aiTurnText,lp(-1,-2));
        space(content,16);
        for(int i=0;i<game.aiTurn.choices.size();i++){
            final AiGameTurn.Option option=game.aiTurn.choices.get(i);
            renderAiChoice(i+1,option);
        }
        // The user's own response is always below choice number 10.
        space(content,12);
        addText(content,"СВОЙ ВАРИАНТ ОТВЕТА",12,accent(),true,11);
        EditText own=new EditText(this);
        own.setTextColor(TEXT);own.setHintTextColor(MUTED);own.setTextSize(sp(16));
        own.setHint("Опиши, что говоришь или делаешь...");
        String existingPending=pendingAiAction();
        if(!existingPending.isEmpty())own.setText(existingPending);
        own.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        own.setMinLines(3);own.setMaxLines(7);
        own.setPadding(dp(13),dp(11),dp(13),dp(11));
        own.setBackground(new RuneFrameDrawable(PANEL_2,BORDER,false));
        content.addView(own,lp(-1,-2));
        space(content,10);
        actionButton("ОТПРАВИТЬ СВОЙ ОТВЕТ","ИИ определит вероятность и последствия этого действия",true,()->{
            String value=own.getText().toString().trim();
            if(value.isEmpty()){toast("Напиши свой ответ");return;}
            if(value.length()>1500){toast("Не более 1500 символов");return;}
            if(value.equals(pendingAiAction())){
                // Retry the same recorded outcome after an API/validation error.
                int lockedChance=prefs.getInt(AI_PENDING_CHANCE,-1);
                int lockedDie=prefs.getInt(AI_PENDING_DIE,-1);
                requestAiTurn(lockedFreeInstruction(value,lockedChance,lockedDie),
                    true,false,lockedChance,null);
                return;
            }
            clearAiPending();
            requestAiTurn("ОЦЕНКА СВОБОДНОГО ДЕЙСТВИЯ: "+value+
                "\nНазови ТОЛЬКО customChance по состоянию персонажа и риску. "+
                "ЗАПРЕЩЕНО выполнять действие или выдавать эффект до броска.",false,true,-2,value);
        });
    }

    private void renderAiChoice(int number,AiGameTurn.Option option){
        LinearLayout row=row();row.setGravity(Gravity.TOP);
        row.setPadding(dp(12),dp(13),dp(12),dp(13));
        row.setBackground(new RuneFrameDrawable(PANEL,number==1?accent():BORDER,number==1));
        TextView ordinal=text("◇ "+number,17,number==1?accent():MUTED,true);
        ordinal.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        row.addView(ordinal,lp(48,-2));
        LinearLayout detail=column();
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
        p.leftMargin=dp(4);row.addView(detail,p);
        TextView title=text(option.title,17,TEXT,true);
        title.setTypeface(SovngardeTypeface.face(this,true));
        detail.addView(title);space(detail,6);
        addText(detail,option.detail,14,MUTED,false,7);
        if(option.chance>=0){
            if(!option.skill.isEmpty())
                addText(detail,"НАВЫК  "+option.skill,12,MUTED,false,3);
            addText(detail,"ШАНС УСПЕХА  "+option.chance+"%",13,
                SceneChoiceMeta.chanceColor(option.chance),true,0);
        }else addText(detail,"БЕЗ ПРОВЕРКИ",12,MUTED,false,0);
        row.setClickable(true);row.setFocusable(true);
        row.setOnClickListener(v->{
            clearAiPending();
            int die=AiGameTurn.roll();
            boolean success=AiGameTurn.success(option.chance,die);
            String result=option.chance<0?"Действие без проверки навыка.":
                "Вероятность, заданная ИИ: "+option.chance+"%. Реальный бросок: "+die+
                ". Результат: "+(success?"УСПЕХ":"НЕУДАЧА")+".";
            requestAiTurn("Игрок нажал вариант "+number+": "+option.title+
                ". Описание: "+option.detail+". "+result+
                " Продолжи именно этот исход и верни effects за совершённое действие.",true);
        });
        LinearLayout.LayoutParams wrap=lp(-1,-2);wrap.bottomMargin=dp(6);
        content.addView(row,wrap);
    }

    /**
     * All effects are committed only after a completed and parsed model response.
     * On interruption the previous scene, dice and inventory remain unchanged.
     */
    private void requestAiTurn(String instruction,boolean actionResolved){
        requestAiTurn(instruction,actionResolved,false,-2,null);
    }

    /** Two-stage custom action: proposal is never persisted, and the die is hidden
     * until the model has committed to a probability. -2 means no fixed chance. */
    private void requestAiTurn(String instruction,boolean actionResolved,
                               boolean proposalOnly,int expectedChance,String customAction){
        if(aiTurnBusy||aiClient.isRunning()||!game.started)return;
        final boolean chat=usingChatGpt();
        final String model=prefs.getString(chat?"ai_chatgpt_model":"ai_api_model",
                chat?"":AiModelCatalog.DEFAULT_API);
        if(model.isEmpty()){aiTurnStatus="Сначала выбери модель в настройках";render();return;}
        final String effort=selectedReasoningEffort(),history=aiMemory.context(aiSession);
        final String saved;
        try{saved=game.toJson().toString();}
        catch(Exception ex){aiTurnStatus="Не удалось прочитать состояние героя";render();return;}
        aiTurnBusy=true;aiTurnPreview="";
        aiTurnStatus=chat?"Подключение к ChatGPT…":"Создание игровой сцены…";
        render();
        final AiStoryClient.Listener listener=new AiStoryClient.Listener(){
            @Override public void onText(String accumulated){
                if(!aiTurnBusy)return;
                if(proposalOnly)return;
                String visible=AiGameTurn.previewScene(accumulated);
                if(visible.isEmpty()||visible.equals(aiTurnPreview))return;
                aiTurnPreview=visible;
                if(tab==0&&aiTurnText!=null&&prefs.getBoolean("ai_enabled",false)){
                    aiTurnText.setText(SceneTypography.render(visible,accent()));
                }
            }
            @Override public void onComplete(String answer){
                if(!aiTurnBusy)return;
                try{
                    // Never overwrite a save changed during the network request.
                    if(!game.toJson().toString().equals(saved))
                        throw new IllegalStateException("Состояние героя изменилось во время генерации");
                    if(proposalOnly){
                        // The short chance-only response is never written to RPG memory.
                        int fixedChance=AiGameTurn.parseChance(answer);
                        int die=fixedChance<0?-1:AiGameTurn.roll();
                        storeAiPending(customAction,fixedChance,die);
                        aiTurnBusy=false;aiTurnPreview="";
                        requestAiTurn(lockedFreeInstruction(customAction,fixedChance,die),
                            true,false,fixedChance,null);
                        return;
                    }
                    AiGameTurn completed=AiGameTurn.parse(answer);
                    if(expectedChance!=-2&&completed.customChance!=expectedChance)
                        throw new IllegalStateException("ИИ изменил шанс после броска");
                    GameEngine next=GameEngine.fromJson(saved);
                    completed.apply(next,actionResolved);
                    next.aiTurn=completed;
                    game=next;
                    save();
                    clearAiPending();
                    aiMemory.add(aiSession,"user",instruction);
                    aiMemory.add(aiSession,"master",completed.scene);
                    if(!completed.journal.isEmpty())aiMemory.add(aiSession,"event",completed.journal);
                    aiTurnStatus="";
                }catch(Exception ex){
                    aiTurnStatus="ИИ-сцена не принята: "+ex.getMessage()+". Повтори выбор, сохранение не изменилось.";
                }
                aiTurnBusy=false;aiTurnPreview="";
                if(tab==0)render();
            }
            @Override public void onError(String error){
                if(!aiTurnBusy)return;
                aiTurnBusy=false;aiTurnPreview="";
                aiTurnStatus="Ошибка генерации: "+error+". Сохранение не изменено.";
                if(tab==0)render();
            }
        };
        if(chat){
            awaitingAiToken=true;
            chatGptOAuth.getToken((token,error)->{
                if(!awaitingAiToken||!aiTurnBusy)return;
                awaitingAiToken=false;
                if(error!=null){aiTurnBusy=false;aiTurnStatus=error;render();return;}
                boolean started=proposalOnly?
                    aiClient.generateChance(token,model,true,effort,game,history,instruction,listener):
                    aiClient.generateTurn(token,model,true,effort,game,history,instruction,listener);
                if(!started){
                    aiTurnBusy=false;aiTurnStatus="Предыдущая генерация ещё завершается";render();
                }
            });
        }else{
            final String key;
            try{key=AiKeyVault.read(this);}
            catch(Exception e){aiTurnBusy=false;aiTurnStatus="API-ключ недоступен";render();return;}
            if(key==null||key.isEmpty()){aiTurnBusy=false;aiTurnStatus="Не настроен API-ключ";render();return;}
            boolean started=proposalOnly?
                aiClient.generateChance(key,model,false,effort,game,history,instruction,listener):
                aiClient.generateTurn(key,model,false,effort,game,history,instruction,listener);
            if(!started){
                aiTurnBusy=false;aiTurnStatus="Предыдущая генерация ещё завершается";render();
            }
        }
    }

    /** Story-only AI assistant: real game rules and quest transitions stay in GameEngine. */

    private String aiProviderMode(){
        String mode=prefs.getString("ai_provider","auto");
        return ("chatgpt".equals(mode)||"api".equals(mode))?mode:"auto";
    }
    private boolean usingChatGpt(){
        String mode=aiProviderMode();
        return "chatgpt".equals(mode)||("auto".equals(mode)&&chatGptOAuth.hasAuthorizedPlan());
    }
    private void aiProviderDialog(){
        String[] labels={"Автоматически · Работа/Codex → API → офлайн",
                "ChatGPT · расход лимитов Работа/Codex","OpenAI API · отдельная оплата"};
        String mode=aiProviderMode();
        int selected="chatgpt".equals(mode)?1:"api".equals(mode)?2:0;
        new RuneDialogBuilder(this,accent()).setTitle("Источник ИИ")
            .setSingleChoiceItems(labels,selected,(dialog,which)->{
                prefs.edit().putString("ai_provider",which==1?"chatgpt":which==2?"api":"auto").apply();
                if(aiClient!=null)aiClient.cancel();
                dialog.dismiss();render();
            }).setNegativeButton("Закрыть",null).show();
    }
    private String selectedReasoningEffort(){
        return AiReasoningEffort.normalize(prefs.getString("ai_reasoning_effort",AiReasoningEffort.DEFAULT));
    }
    private void aiReasoningEffortDialog(){
        String[] choices=AiReasoningEffort.LABELS;
        int selected=AiReasoningEffort.position(selectedReasoningEffort());
        new RuneDialogBuilder(this,accent()).setTitle("Уровень размышления · настройка игры")
            .setSingleChoiceItems(choices,selected,(dialog,which)->{
                String effort=AiReasoningEffort.VALUES[which];
                prefs.edit().putString("ai_reasoning_effort",effort).apply();
                if(aiClient!=null)aiClient.cancel();
                awaitingAiToken=false;
                dialog.dismiss();
                render();
            })
            .setNegativeButton("Закрыть",null).show();
    }
    private void apiModelDialog(){
        int current=AiModelCatalog.position(prefs.getString("ai_api_model",AiModelCatalog.DEFAULT_API));
        new RuneDialogBuilder(this,accent()).setTitle("Модель OpenAI API")
            .setSingleChoiceItems(AiModelCatalog.LABELS,current,(dialog,which)->{
                prefs.edit().putString("ai_api_model",AiModelCatalog.IDS[which]).apply();
                if(aiClient!=null)aiClient.cancel();
                dialog.dismiss();render();
            }).setNegativeButton("Закрыть",null).show();
    }
    private void loginChatGpt(){
        toast("Вход ChatGPT: для игры используются лимиты Работа/Codex");
        chatGptOAuth.signIn(this,(success,message)->{
            toast(message);
            if(tab==5)render();
            if(success)chatGptOAuth.loadModels((models,error)->{
                if(error!=null||models.isEmpty()){toast(error==null?"Нет доступных моделей ChatGPT":error);return;}
                String current=prefs.getString("ai_chatgpt_model","");
                boolean exists=false;
                for(ChatGptOAuth.ModelOption m:models)if(current.equals(m.slug))exists=true;
                if(!exists){
                    String best=models.get(0).slug;
                    for(ChatGptOAuth.ModelOption m:models)if("gpt-6.1-sol".equals(m.slug))best=m.slug;
                    prefs.edit().putString("ai_chatgpt_model",best).apply();
                }
                if(tab==5)render();
            });
        });
    }
    private void chatGptModelDialog(){
        toast("Получаю доступные модели ChatGPT для Работы/Codex…");
        chatGptOAuth.loadModels((models,error)->{
            if(error!=null){toast(error);return;}
            if(models.isEmpty()){toast("Нет доступных моделей ChatGPT");return;}
            String[] labels=new String[models.size()];
            int selected=0;String chosen=prefs.getString("ai_chatgpt_model","");
            for(int i=0;i<models.size();i++){
                labels[i]=models.get(i).title+" ("+models.get(i).slug+")";
                if(chosen.equals(models.get(i).slug))selected=i;
            }
            if(isFinishing()||isDestroyed())return;
            new RuneDialogBuilder(this,accent()).setTitle("Модели · Работа/Codex")
                .setSingleChoiceItems(labels,selected,(dialog,which)->{
                    prefs.edit().putString("ai_chatgpt_model",models.get(which).slug).apply();
                    if(aiClient!=null)aiClient.cancel();
                    dialog.dismiss();render();
                }).setNegativeButton("Закрыть",null).show();
        });
    }

    private void renderAiStoryPanel(){
        LinearLayout panel=card();
        addText(panel,"ᚱ  ИИ-МАСТЕР · "+(usingChatGpt()?"РАБОТА / CODEX":"OPENAI API"),11,accent(),true,8);
        addText(panel,"Свободный диалог и повествование. Инвентарь, бой, награды и перемещения изменяются только игровыми действиями.",12,MUTED,false,12);
        boolean chatgpt=usingChatGpt();
        boolean ready=chatgpt?chatGptOAuth.hasAuthorizedPlan():AiKeyVault.configured(this);
        if(!ready){
            addText(panel,chatgpt?"Войди через ChatGPT и разреши использование лимитов в настройках.":
                "Подключи ChatGPT или добавь OpenAI API-ключ в настройках. Обычная RPG работает офлайн.",13,TEXT,false,8);
            TextView setup=text("НАСТРОИТЬ ИИ",14,accent(),true);
            setup.setPadding(dp(10),dp(12),dp(10),dp(12));
            setup.setBackground(new RuneFrameDrawable(PANEL_2,accent(),false));
            panel.addView(setup);setup.setOnClickListener(v->{tab=5;render();});
            return;
        }
        String previous=aiMemory.latestAnswer(aiSession);
        TextView reply=text(previous.isEmpty()?"ИИ помнит историю беседы. Опиши действие или заговори с персонажем.":previous,17,TEXT,false);
        reply.setLineSpacing(dp(6),1.16f);
        reply.setText(SceneTypography.render(previous.isEmpty()?
            "ИИ помнит историю беседы. Опиши действие или заговори с персонажем.":previous,accent()));
        panel.addView(reply);
        space(panel,9);
        EditText prompt=new EditText(this);
        prompt.setTextColor(TEXT);prompt.setHintTextColor(MUTED);
        prompt.setHint("Что ты хочешь сказать или сделать?");
        prompt.setTextSize(sp(14));
        prompt.setMinLines(2);prompt.setMaxLines(4);
        prompt.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        prompt.setBackground(new RuneFrameDrawable(PANEL_2,BORDER,false));
        prompt.setPadding(dp(12),dp(10),dp(12),dp(10));
        panel.addView(prompt,lp(-1,-2));
        space(panel,9);
        String selectedModel=prefs.getString(chatgpt?"ai_chatgpt_model":"ai_api_model",chatgpt?"":AiModelCatalog.DEFAULT_API);
        String modelLabel=selectedModel.isEmpty()?"не выбрана":chatgpt?selectedModel:AiModelCatalog.label(selectedModel);
        String providerLabel=chatgpt?"Источник: ChatGPT · лимиты Работа/Codex":
                "Источник: OpenAI API · отдельная оплата";
        TextView status=text(providerLabel+"\nМодель (выбрана в игре): "+modelLabel+
                "\nРазмышление (настройка игры): "+AiReasoningEffort.label(selectedReasoningEffort()),11,MUTED,false);
        panel.addView(status);
        space(panel,8);
        TextView send=text("СОЗДАТЬ ПРОДОЛЖЕНИЕ",14,accent(),true);
        send.setGravity(Gravity.CENTER);
        send.setPadding(dp(8),dp(13),dp(8),dp(13));
        send.setBackground(new RuneFrameDrawable(PANEL_2,accent(),false));
        panel.addView(send);
        send.setOnClickListener(v->{
            if(aiClient.isRunning()||awaitingAiToken){
                awaitingAiToken=false;
                aiClient.cancel();
                status.setText("Генерация остановлена. Игровой прогресс не изменён.");
                send.setText("СОЗДАТЬ ПРОДОЛЖЕНИЕ");
                return;
            }
            final String request=prompt.getText().toString().trim();
            if(request.isEmpty()){toast("Напиши действие или реплику");return;}
            if(request.length()>800){toast("Максимум 800 символов");return;}
            String history=aiMemory.context(aiSession);
            String reasoningEffort=selectedReasoningEffort();
            if(chatgpt&&selectedModel.isEmpty()){
                status.setText("Сначала выбери модель ChatGPT в настройках.");return;
            }
            status.setText(chatgpt?"Проверка сессии ChatGPT…":"ИИ создаёт сцену…");
            reply.setText("");
            send.setText("ОСТАНОВИТЬ");
            AiStoryClient.Listener responseListener=new AiStoryClient.Listener(){
                @Override public void onText(String accumulated){
                    reply.setText(SceneTypography.render(accumulated,accent()));
                }
                @Override public void onComplete(String answer){
                    aiMemory.add(aiSession,"user",request);
                    aiMemory.add(aiSession,"master",answer);
                    status.setText("Ответ сохранён в памяти приключения");
                    prompt.setText("");
                    send.setText("СОЗДАТЬ ПРОДОЛЖЕНИЕ");
                }
                @Override public void onError(String error){
                    status.setText(error+"\nИгровой прогресс не изменён.");
                    send.setText("ПОВТОРИТЬ");
                }
            };
            if(chatgpt){
                awaitingAiToken=true;
                chatGptOAuth.getToken((token,error)->{
                    if(!awaitingAiToken)return;
                    awaitingAiToken=false;
                    if(error!=null){status.setText(error);send.setText("ПОВТОРИТЬ");return;}
                    status.setText("ChatGPT создаёт сцену…");
                    boolean started=aiClient.generate(token,selectedModel,true,reasoningEffort,game,history,request,responseListener);
                    if(!started){status.setText("Предыдущий запрос ещё завершается");send.setText("СОЗДАТЬ ПРОДОЛЖЕНИЕ");}
                });
            }else{
                final String key;
                try{key=AiKeyVault.read(this);}
                catch(Exception ex){status.setText("Не удалось прочитать ключ API");send.setText("ПОВТОРИТЬ");return;}
                if(key.isEmpty()){status.setText("API-ключ не настроен");send.setText("ПОВТОРИТЬ");return;}
                boolean started=aiClient.generate(key,selectedModel,false,reasoningEffort,game,history,request,responseListener);
                if(!started){status.setText("Предыдущий запрос ещё завершается");send.setText("СОЗДАТЬ ПРОДОЛЖЕНИЕ");}
            }
        });
    }

    private void aiKeyDialog(){
        EditText value=new EditText(this);
        value.setSingleLine(true);
        value.setTextColor(TEXT);value.setHintTextColor(MUTED);
        value.setHint("sk-...");
        value.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new RuneDialogBuilder(this,accent()).setTitle("OpenAI API-ключ")
            .setMessage("API оплачивается отдельно. Вход через ChatGPT доступен выше без API-ключа после разрешения использования подписки. Ключ шифруется в Android Keystore; публичным приложениям безопаснее использовать сервер.")
            .setView(value).setNegativeButton("Отмена",null)
            .setPositiveButton("Сохранить",(d,w)->{
                try{
                    AiKeyVault.save(this,value.getText().toString());
                    toast("Ключ сохранён в защищённом хранилище");
                    render();
                }catch(Exception e){toast("Не удалось сохранить ключ");}
            }).show();
    }

    private void makeItemsClickable(TextView view,String source){
        // Retain speaker/dialogue formatting when adding clickable item spans.
        android.text.SpannableStringBuilder span=new android.text.SpannableStringBuilder(view.getText());
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
        if(LocationGalleryView.hasGallery(game.progress.location)){
            addText(overview,"ᛟ  ГАЛЕРЕЯ ТЕКУЩЕГО МЕСТА",11,accent(),true,12);
            overview.addView(new LocationGalleryView(this,game.progress.location,accent(),false),lp(-1,-2));
        }
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
        AlertDialog.Builder dialog=new RuneDialogBuilder(this,accent()).setTitle(place.name)
            .setNegativeButton("Закрыть",null);
        if(LocationGalleryView.hasGallery(place.name)){
            android.widget.ScrollView scroller=new android.widget.ScrollView(this);
            scroller.setFillViewport(false);
            LinearLayout wrapper=column();
            wrapper.setPadding(dp(9),dp(9),dp(9),dp(12));
            scroller.addView(wrapper);
            wrapper.addView(new LocationGalleryView(this,place.name,accent(),false),lp(-1,-2));
            TextView description=text(body,14,TEXT,false);
            description.setLineSpacing(dp(4),1.05f);
            description.setPadding(dp(8),dp(13),dp(8),dp(5));
            wrapper.addView(description,lp(-1,-2));
            dialog.setView(scroller);
        }else{
            dialog.setMessage(body);
        }
        if(route.allowed&&route.days>0){
            dialog.setPositiveButton("Начать переход",(d,w)->{
                game.choose("LOC:"+place.name);game.progress.sync(game);save();tab=0;render();
            });
        }
        dialog.show();
    }
    private void renderInventory(){
        game.progress.sync(game);
        section("ПРЕДМЕТЫ И СНАРЯЖЕНИЕ","Инвентарь","Оружие, доспехи, книги и другие находки.");
        java.util.ArrayList<String> groups=ItemCatalog.groups();
        if(!groups.contains(selectedCategory))selectedCategory="Все";
        // Category icon ribbon, horizontally scrollable and fully transparent.
        HorizontalScrollView categoryScroll=new HorizontalScrollView(this);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        categoryScroll.setFillViewport(false);
        LinearLayout categoryStrip=row();
        categoryStrip.setPadding(dp(4),dp(7),dp(4),dp(9));
        categoryStrip.setBackground(new RuneFrameDrawable(PANEL,BORDER,false));
        categoryScroll.addView(categoryStrip);
        content.addView(categoryScroll,lp(-1,-2));
        for(String group:groups){
            boolean active=selectedCategory.equals(group);
            String image=iconOf(group);
            if(group.equals("Оружие"))image="weapon";
            else if(group.equals("Броня")||group.equals("Одежда"))image="armor";
            else if(group.equals("Зелья"))image="potion";
            else if(group.equals("Все"))image="bag";
            else if(group.equals("Книги"))image="book";
            LinearLayout tile=column();tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(7),dp(3),dp(7),dp(3));
            categoryStrip.addView(tile,lp(65,67));
            tile.addView(new SkyrimIconView(this,image,active?accent():MUTED),lp(28,30));
            TextView caption=text(group.toUpperCase(java.util.Locale.ROOT),9,active?TEXT:MUTED,active);
            caption.setGravity(Gravity.CENTER);caption.setMaxLines(2);
            tile.addView(caption,lp(-1,-2));
            tile.setOnClickListener(v->{selectedCategory=group;focusedInventoryId="";render();});
        }
        content.addView(new SkyrimRuleView(this,accent()),lp(-1,22));
        double carried=game.progress.carryWeight();
        LinearLayout summary=card();
        TextView weight=text(String.format(java.util.Locale.forLanguageTag("ru"),
                "ВЕС  %.2f / 50 кг     ◈  СЕПТИМЫ  %d",carried,game.gold),
                15,carried>50?0xffd9a5a5:TEXT,true);
        weight.setGravity(Gravity.CENTER);summary.addView(weight);
        space(summary,6);
        ProgressBar load=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        load.setMax(5000);load.setProgress((int)Math.min(5000,Math.round(carried*100)));
        load.setProgressTintList(ColorStateList.valueOf(0xffc4c9c9));
        load.setProgressBackgroundTintList(ColorStateList.valueOf(0xff303839));
        summary.addView(load,lp(-1,3));
        java.util.ArrayList<ItemCatalog.Item> owned=game.progress.owned(selectedCategory,selectedSort);
        if(owned.isEmpty())focusedInventoryId="";
        else if(focusedInventoryId.isEmpty()||game.progress.quantity(focusedInventoryId)==0
                ||owned.stream().noneMatch(item->item.id.equals(focusedInventoryId)))
            focusedInventoryId=owned.get(0).id;
        ItemCatalog.Item focused=focusedInventoryId.isEmpty()?null:ItemCatalog.find(focusedInventoryId);
        if(focused!=null){
            LinearLayout selected=card();
            LinearLayout top=row();selected.addView(top,lp(-1,-2));
            SkyrimIconView image=new SkyrimIconView(this,iconOf(focused.category),accent());
            top.addView(image,lp(70,78));
            LinearLayout details=column();details.setPadding(dp(9),0,0,0);
            top.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            addText(details,focused.name.toUpperCase(java.util.Locale.ROOT),18,TEXT,true,7);
            addText(details,focused.effect,12,MUTED,false,8);
            addText(details,"ВЕС "+String.format(java.util.Locale.forLanguageTag("ru"),"%.2f",focused.weight)
                    +"     СТОИМОСТЬ "+focused.value,12,accent(),true,0);
            selected.addView(new SkyrimRuleView(this,accent()),lp(-1,17));
            addText(selected,focused.description,14,TEXT,false,9);
            TextView detail=text("◇  ПОДРОБНЕЕ / ИСПОЛЬЗОВАТЬ",12,accent(),true);
            detail.setGravity(Gravity.END);selected.addView(detail,lp(-1,30));
            detail.setOnClickListener(v->itemDialog(focused));
        }
        // Skyrim's familiar name / weight / value table, without bulky cards.
        LinearLayout tableHead=row();tableHead.setPadding(dp(8),dp(7),dp(8),dp(8));
        tableHead.setBackgroundColor(0xb5000000);
        TextView title=text("НАЗВАНИЕ",12,MUTED,true);
        tableHead.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView w=text("ВЕС",11,MUTED,true);w.setGravity(Gravity.END);tableHead.addView(w,lp(63,-2));
        TextView v=text("ЦЕНА",11,MUTED,true);v.setGravity(Gravity.END);tableHead.addView(v,lp(58,-2));
        content.addView(tableHead,lp(-1,-2));
        content.addView(new SkyrimRuleView(this,0xff707577),lp(-1,10));
        if(owned.isEmpty())addText(content,"В выбранной категории нет предметов.",15,MUTED,false,14);
        for(ItemCatalog.Item item:owned){
            final boolean active=item.id.equals(focusedInventoryId);
            LinearLayout itemRow=row();itemRow.setPadding(dp(7),dp(10),dp(8),dp(10));
            itemRow.setBackgroundColor(active?0xa02a2e30:0x98050607);
            itemRow.addView(new SkyrimIconView(this,iconOf(item.category),active?accent():MUTED),lp(22,23));
            TextView name=text(item.name+" ×"+game.progress.quantity(item.id),14,active?accent():TEXT,active);
            name.setMaxLines(2);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);
            np.leftMargin=dp(8);itemRow.addView(name,np);
            TextView measure=text(String.format(java.util.Locale.forLanguageTag("ru"),"%.1f",item.weight),
                12,active?TEXT:MUTED,false);measure.setGravity(Gravity.END);
            itemRow.addView(measure,lp(63,-2));
            TextView price=text(String.valueOf(item.value),12,active?TEXT:MUTED,false);
            price.setGravity(Gravity.END);itemRow.addView(price,lp(58,-2));
            content.addView(itemRow,lp(-1,-2));
            View separator=new View(this);separator.setBackgroundColor(0xff262b2d);
            content.addView(separator,lp(-1,1));
            itemRow.setOnClickListener(view->{focusedInventoryId=item.id;render();});
            itemRow.setOnLongClickListener(view->{itemDialog(item);return true;});
        }
        space(content,9);
        LinearLayout sortBar=row();
        sortBar.setGravity(Gravity.CENTER);
        content.addView(sortBar,lp(-1,41));
        String[] sorts={"Название","Вес","Цена"};
        for(String field:sorts){
            TextView t=text(field.toUpperCase(java.util.Locale.ROOT),12,
                selectedSort.equals(field)?accent():MUTED,selectedSort.equals(field));
            t.setGravity(Gravity.CENTER);
            sortBar.addView(t,new LinearLayout.LayoutParams(0,-1,1));
            t.setOnClickListener(clicked->{selectedSort=field;render();});
        }
        content.addView(new SkyrimRuleView(this,0xff92999b),lp(-1,18));
        actionButton("Полный справочник предметов","Каталог, поиск и характеристики всех вещей",false,this::showItemCatalog);
        actionButton("Кодекс и бестиарий","Народы, существа, история и древние места",false,()->{tab=6;render();});
        actionButton("Личная библиотека","Твои документы, легенды и записи",false,this::showLibrary);
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
    private void showWaitDialog(){
        if(!game.started||prefs.getBoolean("ai_enabled",false)){
            toast("В режиме ИИ временем управляет ИИ-мастер");return;
        }
        if("COMBAT".equals(game.mode)){toast("Во время боя ожидать нельзя");return;}
        LinearLayout panel=column();panel.setPadding(dp(16),dp(10),dp(16),dp(8));
        TextView header=text("ОЖИДАТЬ СКОЛЬКО ЧАСОВ?",20,TEXT,true);
        header.setGravity(Gravity.CENTER);panel.addView(header,lp(-1,-2));
        panel.addView(new SkyrimRuleView(this,accent()),lp(-1,27));
        TextView amount=text("1",34,accent(),true);
        amount.setGravity(Gravity.CENTER);panel.addView(amount,lp(-1,54));
        android.widget.SeekBar slider=new android.widget.SeekBar(this);
        slider.setMax(23);slider.setProgress(0);
        slider.setProgressTintList(ColorStateList.valueOf(accent()));
        slider.setThumbTintList(ColorStateList.valueOf(accent()));
        panel.addView(slider,lp(-1,58));
        TextView clock=text("",14,MUTED,false);clock.setGravity(Gravity.CENTER);
        panel.addView(clock,lp(-1,54));
        java.util.function.IntConsumer update=h->{
            int total=game.hour+h;
            amount.setText(h+" Ч");
            clock.setText("ДЕНЬ "+(game.day+total/24)+"   ·   "+
                String.format(java.util.Locale.ROOT,"%02d:00",total%24));
        };
        update.accept(1);
        slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(android.widget.SeekBar b,int progress,boolean user){
                update.accept(progress+1);
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar b){}
            @Override public void onStopTrackingTouch(android.widget.SeekBar b){}
        });
        new RuneDialogBuilder(this,accent()).setTitle("ОЖИДАНИЕ")
            .setView(panel).setNegativeButton("Отмена",null)
            .setPositiveButton("Ждать",(dialog,which)->{
                if(game.waitHours(slider.getProgress()+1)){save();render();}
            }).show();
    }
    private void heroEntry(LinearLayout p,String label,String value){
        LinearLayout r=row();p.addView(r,lp(-1,39));
        r.addView(text(label,14,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));
        r.addView(text(value,15,TEXT,true));
    }
    private void renderHero(){
        skyrimTopTabs(3);
        section("ЛИЧНОЕ ДЕЛО",""+game.name,game.race+" • "+game.heroClass+" • уровень "+game.level);
        content.addView(new NordicArtView(this,NordicArtView.STORY),lp(-1,125));space(content,12);
        if(!prefs.getBoolean("ai_enabled",false))
            actionButton("Ожидать","Выбрать от 1 до 24 часов отдыха",false,this::showWaitDialog);
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
            LinearLayout skill=row();skill.setPadding(dp(12),dp(10),dp(12),dp(10));
            skill.setBackground(new RuneFrameDrawable(PANEL,BORDER,false));
            skill.addView(new SkyrimIconView(this,"armor",accent()),lp(34,37));
            LinearLayout body=column();LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);
            bp.leftMargin=dp(11);skill.addView(body,bp);
            addText(body,RpgContent.SKILLS[i].toUpperCase(java.util.Locale.ROOT),13,TEXT,true,4);
            ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
            progress.setMax(Math.max(1,game.progress.skillNeed(i)));
            progress.setProgress(Math.max(0,game.progress.skillXp[i]));
            progress.setProgressTintList(ColorStateList.valueOf(accent()));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xff353b3e));
            body.addView(progress,lp(-1,3));
            TextView value=text(String.valueOf(lvl),22,accent(),true);value.setGravity(Gravity.CENTER);
            skill.addView(value,lp(43,-2));
            content.addView(skill,lp(-1,-2));space(content,5);
            skill.setOnClickListener(v->showSkill(index));
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
    private String selectedJournalEntry="Драконий след";
    private void renderJournal(){
        skyrimTopTabs(4);
        section("ЛЕТОПИСЬ СЕВЕРА","Журнал","Выбирай задание, чтобы увидеть описание и текущую цель.");
        String[] entries=game.aiJournal.isEmpty()?
            new String[]{"Драконий след","Пепельные печати","Опасные дороги"}:
            new String[]{"Драконий след","Пепельные печати","Опасные дороги","Задание ИИ"};
        boolean present=false;for(String e:entries)if(e.equals(selectedJournalEntry))present=true;
        if(!present)selectedJournalEntry=entries[0];
        LinearLayout list=card();
        addText(list,"ЗАДАНИЯ",12,accent(),true,9);
        for(String entry:entries){
            boolean chosen=entry.equals(selectedJournalEntry);
            LinearLayout slot=row();slot.setPadding(dp(9),dp(11),dp(7),dp(11));
            slot.setBackgroundColor(chosen?0x99363b3d:0x22000000);
            TextView pointer=text(chosen?"◇":"◦",20,chosen?accent():MUTED,true);
            slot.addView(pointer,lp(27,26));
            TextView name=text(entry,17,chosen?TEXT:MUTED,chosen);
            slot.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            list.addView(slot,lp(-1,-2));
            slot.setOnClickListener(v->{selectedJournalEntry=entry;render();});
        }
        LinearLayout focus=card();
        String objective="",description="",title=selectedJournalEntry;
        if("Драконий след".equals(title)){
            description="Свидетельства о возвращении драконов ведут тебя от южных поселений к северным горам. Каждая глава открывает новые тайны.";
            objective=DragonArc.status(game);
            if(game.progress.dragonStage>=DragonArc.FINAL_STAGE)
                objective+="\n"+DragonContinuation.questObjective(game);
        }else if("Пепельные печати".equals(title)){
            description="История древних печатей и хранителей, которые пытаются не допустить возвращения давно забытой угрозы.";
            objective=game.questDetails();
        }else if("Опасные дороги".equals(title)){
            description="Заказ у доски объявлений. Северные тракты полны разбойников и опасных существ.";
            objective=game.sideQuest==0?"Поручение пока не принято.":"Победы над противниками: "+game.sideKills+"/3";
        }else{description="Приключение, созданное игровым ИИ-мастером.";objective=game.aiJournal;}
        TextView questTitle=text(title.toUpperCase(java.util.Locale.ROOT),22,TEXT,true);
        questTitle.setGravity(Gravity.CENTER);focus.addView(questTitle);
        focus.addView(new SkyrimRuleView(this,accent()),lp(-1,23));
        addText(focus,description,15,TEXT,false,13);
        TextView target=text("ЦЕЛИ",15,accent(),true);
        target.setGravity(Gravity.CENTER);focus.addView(target,lp(-1,35));
        addText(focus,"◇  "+objective,15,TEXT,false,11);
        if("Драконий след".equals(title)){
            actionButton("Продолжить историю","Следующее доступное сюжетное решение",true,()->{
                game.mode="DRAGON";save();tab=0;render();
            });
        }else if("Пепельные печати".equals(title)){
            actionButton("Открыть историю хранителей","Посмотреть текущую главу",true,()->{
                game.mode="QUEST";save();tab=0;render();
            });
        }else if("Опасные дороги".equals(title)){
            actionButton("Вернуться к объявлениям","Разговоры и заказы доступны в поселениях",false,()->{
                game.mode="WORLD";save();tab=0;render();
            });
        }else{
            actionButton("Продолжить ИИ-приключение","Вернуться к сцене ИИ",true,()->{tab=0;render();});
        }
        LinearLayout completed=card();
        addText(completed,"ЗАВЕРШЁННЫЕ ЗАДАНИЯ",12,accent(),true,9);
        if(game.progress.dragonStage>=DragonArc.FINAL_STAGE)
            addText(completed,"◇ Драконий след · первое испытание",14,TEXT,false,7);
        if(game.progress.dragonStage>=DragonArc.FINAL_STAGE){
            String[] investigations={"Письмо под пеплом","Совет за закрытыми дверями","След пепельного гонца"};
            for(int i=0;i<Math.min(3,game.progress.dragonNextStage);i++)
                addText(completed,"◇ "+investigations[i],14,TEXT,false,6);
        }
        if(game.questStage==4)addText(completed,"◇ Пепельные печати",14,TEXT,false,6);
        if(game.boardCompleted>0)addText(completed,"◇ Опасные дороги · заказов: "+game.boardCompleted,14,TEXT,false,6);
        if(game.progress.completed.isEmpty()&&game.questStage!=4
                &&game.progress.dragonStage<DragonArc.FINAL_STAGE&&game.boardCompleted==0)
            addText(completed,"Завершённых поручений пока нет.",14,MUTED,false,0);
        LinearLayout failures=card();
        addText(failures,"ПРОВАЛЕННЫЕ ЗАДАНИЯ",12,accent(),true,9);
        boolean any=false;
        for(java.util.Map.Entry<String,String> q:game.progress.questOutcomes.entrySet())
            if("FAILED".equals(q.getValue())){
                addText(failures,"✕ "+q.getKey(),13,MUTED,false,6);any=true;
            }
        if(!any)addText(failures,"Проваленных заданий нет.",14,MUTED,false,0);
        LinearLayout successRolls=card();
        addText(successRolls,"ИСТОРИЯ УСПЕШНЫХ ПРОВЕРОК",12,accent(),true,9);
        if(game.progress.completed.isEmpty())
            addText(successRolls,"Успешных проверок пока нет.",14,MUTED,false,0);
        for(String check:game.progress.completed)
            addText(successRolls,"◇ "+check,13,TEXT,false,6);
        LinearLayout failedRolls=card();
        addText(failedRolls,"ИСТОРИЯ НЕУДАЧНЫХ ПРОВЕРОК",12,accent(),true,9);
        if(game.progress.failed.isEmpty())
            addText(failedRolls,"Неудачных проверок пока нет.",14,MUTED,false,0);
        for(String check:game.progress.failed)
            addText(failedRolls,"✕ "+check,13,MUTED,false,6);
        actionButton("Вернуться к приключению","Продолжить текущую сцену",true,()->{tab=0;render();});
    }
    private void renderSettings(){
        skyrimTopTabs(5);
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
        space(content,6);addText(content,"ИИ-ПРИКЛЮЧЕНИЕ",11,accent(),true,11);
        LinearLayout aiCard=card();
        addText(aiCard,"ИСТОЧНИК МОДЕЛИ И РАЗМЫШЛЕНИЯ",11,accent(),true,8);
        addText(aiCard,"Подключение ChatGPT расходует доступные лимиты «Работа / Codex», НЕ лимиты обычного «Чата». Список моделей запрашивается из твоего аккаунта, но модель и уровень размышления задаются отдельно в этой игре. Через API действует своя оплата. Локальная модель пока не подключена.",12,MUTED,false,10);
        Switch aiToggle=new Switch(this);
        aiToggle.setText("ИИ управляет всей игрой, сценами и вариантами");
        aiToggle.setTextSize(sp(14));aiToggle.setTextColor(TEXT);
        aiToggle.setThumbTintList(ColorStateList.valueOf(accent()));
        aiToggle.setChecked(prefs.getBoolean("ai_enabled",false));
        aiToggle.setOnCheckedChangeListener((v,on)->{
            prefs.edit().putBoolean("ai_enabled",on).apply();
            if(!on && aiClient!=null){aiClient.cancel();aiTurnBusy=false;awaitingAiToken=false;}
        });
        aiCard.addView(aiToggle,lp(-1,48));
        addText(aiCard,"В «Авто» приоритет у разрешённой подписки ChatGPT (Работа/Codex), затем у настроенного API. При исчерпании лимита ChatGPT скрытого перехода на платный API нет. Обычный «Чат» и его личные настройки не считываются.",12,MUTED,false,0);
        String provider=aiProviderMode();
        actionButton("Источник ИИ","chatgpt".equals(provider)?"ChatGPT · Работа/Codex":
            "api".equals(provider)?"OpenAI API · отдельная оплата":
                "Автоматически · Работа/Codex → API",true,this::aiProviderDialog);
        actionButton("Вход ChatGPT · Работа/Codex",chatGptOAuth.accountLabel()+
            " · авторизация в системном браузере",false,this::loginChatGpt);
        if(chatGptOAuth.hasAuthorizedPlan()){
            actionButton("Модель · Работа/Codex",prefs.getString("ai_chatgpt_model","Не выбрана")+
                " · доступна по аккаунту, выбрана внутри игры",false,this::chatGptModelDialog);
            actionButton("Выйти из ChatGPT","Завершить авторизованную сессию",false,()->{
                new RuneDialogBuilder(this,accent()).setTitle("Выйти из ChatGPT?")
                    .setNegativeButton("Отмена",null).setPositiveButton("Выйти",(d,w)->{
                        if(aiClient!=null)aiClient.cancel();
                        chatGptOAuth.signOut((success,message)->{toast(message);render();});
                    }).show();
            });
        }
        actionButton("Модель · OpenAI API",AiModelCatalog.label(prefs.getString("ai_api_model",AiModelCatalog.DEFAULT_API))+
                " · отдельный API-ключ",false,this::apiModelDialog);
        actionButton("Уровень размышления · в игре",AiReasoningEffort.label(selectedReasoningEffort())+
            " · отправляется выбранной модели, не копируется из Чата/Работы",false,this::aiReasoningEffortDialog);
        addText(content,"«Автоматически» означает уровень по умолчанию у выбранной модели. Это не синхронизация ползунка из ChatGPT. Уровни доступны не для всех моделей; высокие значения могут отвечать медленнее и увеличивать расходы API.",12,MUTED,false,11);
        actionButton("Ввести / заменить OpenAI API-ключ",AiKeyVault.configured(this)?"Ключ уже сохранён (зашифрован)":"Ключ не настроен",false,this::aiKeyDialog);
        if(AiKeyVault.configured(this))actionButton("Удалить сохранённый API-ключ","Не затрагивает игру и историю",false,()->{
            new RuneDialogBuilder(this,accent()).setTitle("Удалить ключ API?")
                .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{
                    if(aiClient!=null)aiClient.cancel();
                    AiKeyVault.clear(this);render();
                }).show();
        });
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
            try{JSONObject j=game.toJson();j.put("ai_history",aiMemory.exportSession(aiSession));String s=j.toString();ClipboardManager cb=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
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
                    game=GameEngine.fromJson(json.toString());newAiSession();aiMemory.importSession(aiSession,json.optJSONArray("ai_history"));tab=0;save();render();toast("Сохранение восстановлено");
                }catch(Exception ex){toast("Неверный код сохранения");}
            }).show();
    }
    private void resetDialog(){
        new RuneDialogBuilder(this,accent()).setTitle("Начать заново?")
            .setMessage("Текущее сохранение будет заменено. При необходимости сначала скопируй код сохранения.")
            .setNegativeButton("Отмена",null).setPositiveButton("Новая игра",(d,w)->{
                newAiSession();game=new GameEngine();tab=0;draftName="";selectedStart=0;java.util.Arrays.fill(initialPoints,0);save();render();}).show();
    }
    private void renderNav(){
        root.addView(new SkyrimRuleView(this,0xff929b9d),lp(-1,12));
        LinearLayout nav=row();nav.setPadding(dp(2),dp(7),dp(2),dp(8));
        nav.setBackgroundColor(0xf8050809);root.addView(nav,lp(-1,66));
        String[] labels={"ИСТОРИЯ","КАРТА","СУМКА","КОДЕКС","ГЕРОЙ","ЖУРНАЛ","ТЕМА"};
        String[] glyphs={"story","map","bag","codex","hero","journal","settings"};
        int[] screens={0,1,2,6,3,4,5};
        for(int i=0;i<labels.length;i++){
            final int id=screens[i];
            LinearLayout area=column();area.setGravity(Gravity.CENTER);
            nav.addView(area,new LinearLayout.LayoutParams(0,-1,1));
            area.addView(new SkyrimIconView(this,glyphs[i],tab==id?accent():0xff969da0),lp(28,31));
            TextView label=text(labels[i],8,tab==id?accent():MUTED,tab==id);
            label.setGravity(Gravity.CENTER);area.addView(label);
            area.setContentDescription(labels[i]);
            area.setOnClickListener(v->{if(tab!=id){tab=id;render();}});
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
