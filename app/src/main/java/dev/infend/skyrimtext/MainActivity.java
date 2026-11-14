package dev.infend.skyrimtext;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
    private static final int[] HERO_NAME_COLORS={
        0xffdeb66d,0xfff1f2ef,0xffa8c8e5,0xffde8789,0xff8ccdab,0xffc5a8e2
    };
    private static final String[] HERO_NAME_COLOR_NAMES={
        "Золото","Серебро","Лёд","Рубин","Изумруд","Аметист"
    };
    private static final String PREFS="skyrim_text_v1";
    private GameEngine game;
    private SharedPreferences prefs;
    private LinearLayout root, content, header, topChrome;
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
    private String selectedGender=CharacterIdentity.MALE;
    private int selectedPortrait=1;
    private int selectedStart=0,selectedMapRegion=-1;
    private int mapListLimit=60;
    private SkyrimTerrainMapView activeTerrainMap;
    private final int[] initialPoints={0,0,0};
    private String selectedCategory="Все",selectedSort="Название";
    private String focusedInventoryId="";
    private final java.util.LinkedHashMap<String,Integer> recentItemGains=new java.util.LinkedHashMap<>();
    private static final int ITEM_DAMAGE=0xffcb7770, ITEM_ARMOR=0xff8baecc, ITEM_GOLD=0xffc6b17d, ITEM_UP=0xff79cc9c, ITEM_DOWN=0xffe47b7b, ITEM_MAGIC=0xffb0a2db;
    private static final String SKILL_HUD_OWNER="skill_hud_owner";
    private static final String SKILL_HUD_SNAPSHOT="skill_hud_snapshot";
    private boolean animations=true, titleOpen=true;
    private int codexCategory=0;
    private SkyrimOtaController updater;
    private ChatGptOAuth chatGptOAuth;
    private boolean awaitingAiToken;
    private boolean accountActionBusy;
    private AiStoryMemory aiMemory;
    private AiStoryClient aiClient;
    private LocalAiClient localAiClient;
    private static final int REQUEST_LOCAL_GGUF=7107;
    private String aiSession;
    private static final String AI_PENDING_ACTION="ai_pending_action";
    private static final String AI_PENDING_CHANCE="ai_pending_chance";
    private static final String AI_PENDING_DIE="ai_pending_die";
    private static final String AI_PENDING_SCENE="ai_pending_scene_hash";
    private static final String AI_PENDING_SESSION="ai_pending_session";
    private boolean aiTurnBusy;
    private String aiTurnPreview="",aiTurnStatus="";
    private String aiLocalPhase="Локальная генерация…";
    private long aiLocalStartedAt=0L;
    private TextView aiTurnText,aiProgressInfo,aiReasoningText;
    private Runnable aiDotsTicker;
    private String aiReasoningPreview="";
    private final java.util.LinkedHashSet<Integer> selectedAiChoices=new java.util.LinkedHashSet<>();
    private final java.util.HashSet<String> hiddenMapTypes=new java.util.HashSet<>();
    private String aiOwnDraft="";


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
        localAiClient=new LocalAiClient(this);
        chatGptOAuth=new ChatGptOAuth(this);
        ResourceStore.start(this);
        ItemCatalog.initialize(this);
        SovngardeTypeface.preload(this);
        game=GameEngine.fromJson(prefs.getString("save",null));
        if(bundle!=null){
            draftName=bundle.getString("hero_name","");
            selectedRace=bundle.getString("hero_race","Норд");
            selectedClass=bundle.getString("hero_class","Без класса");
            selectedGender=CharacterIdentity.normalizeGender(bundle.getString("hero_gender"));
            selectedPortrait=CharacterIdentity.normalizePortrait(bundle.getInt("hero_portrait",1));
            selectedStart=Math.max(0,Math.min(RpgContent.STARTS.length-1,bundle.getInt("hero_start",0)));
            int[] points=bundle.getIntArray("hero_points");
            if(points!=null&&points.length==3)System.arraycopy(points,0,initialPoints,0,3);
        }
        restoreAiDraft();
        accentIndex=Math.max(0,Math.min(ACCENTS.length-1,prefs.getInt("accent",0)));
        fontBonus=Math.max(-2,Math.min(4,prefs.getInt("font_bonus",0)));
        animations=prefs.getBoolean("animations",true);
        hiddenMapTypes.addAll(prefs.getStringSet("map_hidden_types",java.util.Collections.emptySet()));
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
    @Override protected void onSaveInstanceState(Bundle state){
        if(!game.started&&nameField!=null)draftName=nameField.getText().toString();
        state.putString("hero_name",draftName);state.putString("hero_race",selectedRace);
        state.putString("hero_class",selectedClass);state.putString("hero_gender",selectedGender);
        state.putInt("hero_portrait",selectedPortrait);state.putInt("hero_start",selectedStart);
        state.putIntArray("hero_points",initialPoints.clone());
        super.onSaveInstanceState(state);
    }
    @Override protected void onPause(){if(updater!=null)updater.onHostPause();save();super.onPause();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(updater!=null&&intent.getBooleanExtra("ota_open_from_notification",false))updater.onNotificationIntent();}
    @Override protected void onDestroy(){
        if(updater!=null)updater.onHostDestroy();
        if(aiClient!=null)aiClient.shutdown();
        if(localAiClient!=null)localAiClient.shutdown();
        if(chatGptOAuth!=null)chatGptOAuth.shutdown();
        if(aiMemory!=null)aiMemory.close();
        super.onDestroy();
    }
    private void newAiSession(){
        recentItemGains.clear();
        selectedAiChoices.clear();aiOwnDraft="";clearAiDraft();
        if(aiClient!=null)aiClient.cancel();if(localAiClient!=null)localAiClient.cancel();
        aiSession=java.util.UUID.randomUUID().toString();
        prefs.edit().putString("ai_session",aiSession).apply();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQUEST_LOCAL_GGUF&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null)
            importLocalModel(data.getData());
    }
    private void selectLocalModel(){
        if(localAiClient!=null&&localAiClient.isRunning()){toast("Заверши текущую генерацию");return;}
        Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        try{startActivityForResult(picker,REQUEST_LOCAL_GGUF);}
        catch(Exception ex){toast("Не удалось открыть выбор файлов");}
    }
    private void testLocalModel(){
        if(!localModelReady()){toast("Сначала импортируй файл GGUF");return;}
        if(localAiClient.isRunning()){toast("Модель уже выполняет запрос");return;}
        final TextView result=text("Проверка запуска GGUF…",14,TEXT,false);
        result.setPadding(dp(16),dp(16),dp(16),dp(16));
        AlertDialog dialog=new RuneDialogBuilder(this,accent())
            .setTitle("ТЕСТ ЛОКАЛЬНОЙ МОДЕЛИ")
            .setView(result)
            .setNegativeButton("Остановить",(d,w)->localAiClient.cancel()).create();
        dialog.setCancelable(false);
        dialog.show();
        final long start=android.os.SystemClock.elapsedRealtime();
        final float[] speed={0f};
        final int[] tokens={0};
        boolean started=localAiClient.testModel(LocalAiModelStore.path(prefs),
            new AiStoryClient.Listener(){
                @Override public void onLocalStatus(String state){
                    if(dialog.isShowing())result.setText(state+
                        "\nМодель работает отдельно от процесса игры. Первый запуск может быть медленным.");
                }
                @Override public void onLocalMetrics(int count,float tps,long elapsed){
                    tokens[0]=count;speed[0]=tps;
                    prefs.edit().putFloat("ai_local_last_tps",tps)
                        .putInt("ai_local_last_tokens",count).apply();
                }
                @Override public void onText(String value){}
                @Override public void onComplete(String answer){
                    if(!dialog.isShowing())return;
                    long elapsed=(android.os.SystemClock.elapsedRealtime()-start)/1000L;
                    result.setText("ТЕСТ УСПЕШЕН\n\nОтвет: "+answer.trim()+
                        "\nОбщее время: "+elapsed+" с"+
                        "\nСгенерировано: "+tokens[0]+" токенов"+
                        "\nСкорость: "+String.format(java.util.Locale.ROOT,"%.2f",speed[0])+" ток/с"+
                        "\n\nДля длинных сцен Skyrim потребуется значительно больше времени.");
                }
                @Override public void onError(String message){
                    if(dialog.isShowing())result.setText("ТЕСТ НЕ ПРОЙДЕН\n\n"+message+
                        "\n\nИгровые сохранения не изменены.");
                }
            });
        if(!started)result.setText("Модель занята либо импортированный GGUF не найден.");
    }
    private void importLocalModel(Uri uri){
        java.util.concurrent.atomic.AtomicBoolean cancelled=new java.util.concurrent.atomic.AtomicBoolean();
        TextView progress=text("Проверка GGUF и свободного места…",14,TEXT,false);
        progress.setPadding(dp(16),dp(16),dp(16),dp(16));
        AlertDialog dialog=new RuneDialogBuilder(this,accent())
            .setTitle("ИМПОРТ ЛОКАЛЬНОЙ МОДЕЛИ")
            .setView(progress)
            .setNegativeButton("Отмена",(d,w)->cancelled.set(true))
            .create();
        dialog.setCancelable(false);
        dialog.show();
        LocalAiModelStore.importFromUri(this,uri,prefs,cancelled,new LocalAiModelStore.Listener(){
            @Override public void onProgress(long bytes,long total){
                String done=String.format(java.util.Locale.ROOT,"%.1f ГБ",bytes/1073741824.0);
                progress.setText(total>0
                    ?"Импорт "+done+" / "+String.format(java.util.Locale.ROOT,"%.1f ГБ",total/1073741824.0)
                     +" ("+(bytes*100/total)+"%)"
                    :"Импорт "+done+"…");
            }
            @Override public void onImported(String path){
                dialog.dismiss();
                prefs.edit().putString("ai_provider","local").apply();
                toast("GGUF импортирован. Локальная модель выбрана");
                render();
            }
            @Override public void onFailure(String message){
                dialog.dismiss();
                new RuneDialogBuilder(MainActivity.this,accent())
                    .setTitle("Не удалось импортировать GGUF")
                    .setMessage(message)
                    .setPositiveButton("Закрыть",null).show();
            }
        });
    }
    private boolean localModelReady(){return LocalAiModelStore.ready(prefs);}
    private String resolvedAiProvider(){
        String mode=aiProviderMode();
        if(!"auto".equals(mode))return mode;
        if(chatGptOAuth.hasAuthorizedPlan())return "chatgpt";
        if(AiKeyVault.configured(this))return "api";
        return localModelReady()?"local":"none";
    }
    private void save(){try{prefs.edit().putString("save",game.toJson().toString()).apply();}catch(Exception ignored){}}
    private int dp(float dp){return (int)(dp*getResources().getDisplayMetrics().density+0.5f);}
    private int sp(int size){return Math.max(10,size+fontBonus);}
    private int accent(){return ACCENTS[accentIndex];}
    private int heroColor(){
        int index=Math.max(0,Math.min(HERO_NAME_COLORS.length-1,prefs.getInt("hero_name_color",0)));
        return HERO_NAME_COLORS[index];
    }
    private boolean showChoiceNumbers(){return prefs.getBoolean("show_choice_numbers",true);}
    private String skillGlyph(String skill){
        if(skill==null||skill.trim().isEmpty())return "skills";
        String clean=skill.split(" [·|] ",2)[0].trim();
        String exact=SkyrimIconAtlas.exactLabel(this,clean);
        return exact==null?"skills":exact;
    }
    private GradientDrawable bg(int fill,int border,int radius){
        GradientDrawable gd=new GradientDrawable();gd.setColor(fill);gd.setCornerRadius(dp(Math.min(radius,3)));
        if(border!=Color.TRANSPARENT)gd.setStroke(dp(1),border);return gd;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String str,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(SceneTypography.displaySeparators(str));t.setTextSize(sp(size));t.setTextColor(color);
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

    private boolean visual(String key){return prefs.getBoolean("visual_"+key,true);}
    private void visualSettings(){
        int start=content.getChildCount();
        LinearLayout panel=card();
        addText(panel,"Каждый эффект можно отключить отдельно. Игровые правила не меняются.",13,MUTED,false,12);
        String[][] options={{"weather","Живая погода"},{"status","Эффекты состояния"},{"portraits","Собеседники и аватары"},{"narrative","Оформление повествования"},{"choices","Расширенные карточки действий"},{"reveal","Плавное раскрытие текста"},{"stars","Созвездия навыков"},{"gear","Силуэт и экипировка"},{"damage","Показатели урона"},{"victory","Итоги победы"},{"fog","Туман войны"},{"travel","Маршрут путешествия"},{"chronicle","Хроника приключений"},{"reputation","Репутация и отношения"},{"loot","Карточки добычи"},{"trade","Панель торговли"},{"defeat","Экран поражения"},{"notice","Тематические уведомления"}};
        for(String[] option:options){
            Switch toggle=new Switch(this);
            toggle.setText(option[1]);toggle.setTextColor(TEXT);toggle.setTextSize(sp(14));
            toggle.setThumbTintList(ColorStateList.valueOf(accent()));toggle.setChecked(visual(option[0]));
            toggle.setMinHeight(dp(46));panel.addView(toggle,lp(-1,-2));
            toggle.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("visual_"+option[0],on).apply());
        }
        foldSettingsSection("visual","ВИЗУАЛЬНЫЕ ЭФФЕКТЫ",start);
    }
    private View sceneGallery(String location){
        LocationGalleryView gallery=new LocationGalleryView(this,location,accent(),false);
        if(!visual("weather"))return gallery;
        WorldWeather weather=WorldWeather.at(game.region,location,game.day,game.hour);
        if(!location.equalsIgnoreCase(game.progress.location))return gallery;
        if(!java.util.Arrays.asList("snow","blizzard","rain","storm","fog").contains(weather.glyph))return gallery;
        FrameLayout frame=new FrameLayout(this);
        frame.addView(gallery,new FrameLayout.LayoutParams(-1,-2));
        VisualEffectsView particles=new VisualEffectsView(this,weather.glyph,animations);
        frame.addView(particles,new FrameLayout.LayoutParams(-1,-1));
        return frame;
    }
    private void stateEffects(){
        if(!visual("status")||!game.started||titleOpen)return;
        float fraction=game.hp/(float)Math.max(1,game.maxHp);
        if(fraction>=.65f)return;
        root.setForeground(new VisualEffectsView.EdgeDrawable(fraction<.25f?0x99b51c27:0x55b51c27));
    }
    private String lastNarrative="";
    private void decorateNarrative(LinearLayout host,TextView body,String raw){
        if(!visual("narrative"))body.setText(body.getText().toString());
        if(visual("portraits")){
            java.util.LinkedHashSet<String> speakers=new java.util.LinkedHashSet<>();
            java.util.regex.Matcher names=java.util.regex.Pattern.compile("(?m)^\\s*(?:\\*\\*)?([^:\\n*]{2,48}):(?:\\*\\*)?\\s*$").matcher(raw);
            while(names.find()&&speakers.size()<4){
                String name=names.group(1).trim();
                if(!name.isEmpty()&&!name.equalsIgnoreCase("ты")&&!name.equalsIgnoreCase(game.name))speakers.add(name);
            }
            if(!speakers.isEmpty()){
                LinearLayout avatars=row();
                for(String name:speakers){
                    LinearLayout person=column();person.setGravity(Gravity.CENTER);
                    person.addView(new SkyrimIconView(this,"hero",accent()),lp(34,37));
                    TextView label=text(name,10,MUTED,false);label.setGravity(Gravity.CENTER);label.setMaxLines(2);
                    person.addView(label,lp(-1,-2));avatars.addView(person,new LinearLayout.LayoutParams(0,-2,1));
                }
                host.addView(avatars,0,lp(-1,-2));
            }
        }
        if(visual("reveal")&&animations&&!raw.equals(lastNarrative)){
            lastNarrative=raw;body.setAlpha(.15f);
            body.animate().alpha(1f).setDuration(360).start();
            body.setOnLongClickListener(v->{body.animate().cancel();body.setAlpha(1f);return false;});
        }
    }
    private void choiceExtras(LinearLayout host,String action){
        if(!visual("choices"))return;
        String cost="";
        if("HEAVY".equals(action))cost="−10 выносливости";
        else if("MAGIC".equals(action))cost="−12 магии";
        else if("POTION".equals(action))cost="−1 лечебное зелье";
        else if("DEFEND".equals(action))cost="Восстановление сил | снижение урона";
        else if("FLEE".equals(action))cost="Риск неудачного отступления";
        if(!cost.isEmpty())addText(host,cost,12,ITEM_GOLD,true,3);
    }
    private static final class VisualSnapshot {
        final String location,mode,enemy;final int hp,enemyHp,victories,gold,xp,level,day;
        VisualSnapshot(GameEngine g){location=g.progress.location;mode=g.mode;enemy=g.enemyName;hp=g.hp;enemyHp=g.enemyHp;victories=g.victories;gold=g.gold;xp=g.xp;level=g.level;day=g.day;}
    }
    private String combatVisual="",victoryVisual="",travelVisual="";
    private void recordVisual(VisualSnapshot before,GameEngine after,String action){
        combatVisual="";victoryVisual="";travelVisual="";
        after.visitedVisual.add(after.progress.location);
        boolean moved=!before.location.equals(after.progress.location);
        if(moved)travelVisual=before.location+" → "+after.progress.location+" | "+Math.max(0,after.day-before.day)+" дн.";
        if("COMBAT".equals(before.mode)){
            int hit=Math.max(0,before.enemyHp-after.enemyHp);
            if(!before.enemy.equals(after.enemyName)&&after.victories==before.victories)hit=0;
            int received=Math.max(0,before.hp-after.hp);
            if(!"DEFEAT".equals(after.mode)&&(hit>0||received>0))
                combatVisual=(hit>0?"−"+hit+" врагу":"")+(received>0?(hit>0?"  |  ":"")+"−"+received+" герою":"");
            if(after.victories>before.victories){
                long earned=after.xp-before.xp;
                for(int i=before.level;i<after.level;i++)earned+=i*80L;
                victoryVisual=before.enemy+"\\n+"+Math.max(0,after.gold-before.gold)+" септимов  |  +"+Math.max(0,earned)+" опыта";
            }
        }
        String event=moved?travelVisual:!victoryVisual.isEmpty()?("Победа: "+before.enemy):
            "DEFEAT".equals(after.mode)?"Поражение":after.mode.equals("COMBAT")?after.battleLog:after.message;
        if(event==null||event.trim().isEmpty())event=action;
        after.recordVisualEvent(event);
    }
    private void renderVisualResults(){
        if(visual("damage")&&!combatVisual.isEmpty()){
            LinearLayout box=card();TextView t=text(combatVisual,21,ITEM_DOWN,true);t.setGravity(Gravity.CENTER);box.addView(t);
            if(animations){t.setTranslationY(dp(8));t.setAlpha(.2f);t.animate().translationY(0).alpha(1).setDuration(300).start();}
        }
        if(visual("victory")&&!victoryVisual.isEmpty()){
            LinearLayout box=card();addText(box,"◇  ПОБЕДА",23,ITEM_GOLD,true,10);addText(box,victoryVisual,16,TEXT,false,4);
        }
        if(visual("travel")&&!travelVisual.isEmpty()){
            LinearLayout box=card();addText(box,"ПУТЕШЕСТВИЕ ЗАВЕРШЕНО",12,accent(),true,8);addText(box,travelVisual,15,TEXT,false,0);
        }
        if(visual("defeat")&&"DEFEAT".equals(game.mode)){
            LinearLayout box=card();box.setBackground(new RuneFrameDrawable(0xff190c0d,ITEM_DOWN,false));
            TextView t=text("ПОРАЖЕНИЕ",26,ITEM_DOWN,true);t.setGravity(Gravity.CENTER);box.addView(t);
            addText(box,game.message,15,TEXT,false,0);
            if(animations){box.setAlpha(.2f);box.animate().alpha(1).setDuration(600).start();}
        }
    }
    private void routePreview(LinearLayout host,TravelPlanner.Route route){
        if(!visual("travel")||!route.allowed||route.days==0)return;
        addText(host,"МАРШРУТ | "+route.days+" дн. | "+route.fare+" септимов",12,ITEM_GOLD,true,9);
        for(int i=0;i<route.stops.size();i++){
            LinearLayout line=row();line.addView(new SkyrimIconView(this,"map",accent()),lp(23,25));
            addText(line,(i==0?"Отправление: ":i==route.stops.size()-1?"Прибытие: ":"Переход: ")+route.stops.get(i),13,TEXT,false,0);
            host.addView(line,lp(-1,-2));
        }
    }
    private void renderChronicle(){
        if(!visual("chronicle"))return;
        LinearLayout c=card();addText(c,"ХРОНИКА ПРИКЛЮЧЕНИЙ",12,accent(),true,12);
        for(int i=game.visualChronicle.length()-1;i>=0;i--){
            JSONObject entry=game.visualChronicle.optJSONObject(i);if(entry==null)continue;
            addText(c,"◇  День "+entry.optInt("day")+" | "+entry.optString("location"),12,ITEM_GOLD,true,4);
            addText(c,entry.optString("event"),14,TEXT,false,11);
        }
        if(game.visualChronicle.length()==0)addText(c,"Новые события появятся после совершённых действий.",13,MUTED,false,0);
    }
    private void reputationPanel(){
        if(!visual("reputation"))return;
        LinearLayout c=card();addText(c,"РЕПУТАЦИЯ И ОТНОШЕНИЯ",12,accent(),true,10);
        int rep=game.reputation;
        addText(c,"Общая репутация: "+(rep>0?"+":"")+rep,17,rep>=0?ITEM_UP:ITEM_DOWN,true,7);
        android.widget.ProgressBar bar=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(200);bar.setProgress(100+Math.max(-100,Math.min(100,rep)));
        bar.setProgressTintList(ColorStateList.valueOf(rep>=0?ITEM_UP:ITEM_DOWN));c.addView(bar,lp(-1,5));
        String facts=game.sceneState.confirmedContext();
        if(!facts.isEmpty())addText(c,facts,12,MUTED,false,0);
    }
    private void gearVisual(LinearLayout items){
        if(!visual("gear"))return;
        LinearLayout layout=row();layout.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout slots=column();layout.addView(slots,new LinearLayout.LayoutParams(0,-2,1));
        for(String[] slot:new String[][]{{"head","Голова"},{"body","Доспех"},{"hands","Руки"},{"feet","Ноги"}}){
            addText(slots,slot[1],10,MUTED,true,2);addText(slots,game.progress.equippedName(slot[0]),12,TEXT,false,7);
        }
        layout.addView(new VisualEffectsView.HeroSilhouette(this,accent()),lp(80,172));
        LinearLayout right=column();right.setPadding(dp(6),0,0,0);layout.addView(right,new LinearLayout.LayoutParams(0,-2,1));
        for(String[] slot:new String[][]{{"weapon","Оружие"},{"shield","Щит"},{"ring","Кольцо"},{"neck","Амулет"}}){
            addText(right,slot[1],10,MUTED,true,2);addText(right,game.progress.equippedName(slot[0]),12,TEXT,false,7);
        }
        items.addView(layout,lp(-1,-2));
    }
    private void starSkills(){
        if(!visual("stars"))return;
        LinearLayout c=card();addText(c,"СОЗВЕЗДИЯ НАВЫКОВ",12,ITEM_GOLD,true,10);
        for(int i=0;i<RpgContent.SKILLS.length;i+=3){
            LinearLayout r=row();c.addView(r,lp(-1,-2));
            for(int k=i;k<Math.min(i+3,RpgContent.SKILLS.length);k++){
                final int index=k;LinearLayout node=column();node.setGravity(Gravity.CENTER);
                node.addView(new VisualEffectsView.SkillStars(this,game.progress.skillLevel(k),accent()),lp(-1,49));
                TextView label=text(RpgContent.SKILLS[k]+"\\n"+game.progress.skillLevel(k),11,TEXT,true);
                label.setGravity(Gravity.CENTER);node.addView(label,lp(-1,-2));
                r.addView(node,new LinearLayout.LayoutParams(0,-2,1));node.setOnClickListener(v->showSkill(index));
            }
        }
    }
    private void tradeVisual(){
        if(!visual("trade")||!"SHOP".equals(game.mode))return;
        LinearLayout c=card();addText(c,"ТОРГОВЕЦ  ◇  ПОКУПАТЕЛЬ",12,ITEM_GOLD,true,10);
        LinearLayout r=row();c.addView(r,lp(-1,-2));
        LinearLayout a=column(),b=column();r.addView(a,new LinearLayout.LayoutParams(0,-2,1));r.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        addText(a,"Ассортимент",13,accent(),true,5);addText(a,"Зелья | оружие | броня",12,TEXT,false,0);
        addText(b,"Твой кошелёк",13,accent(),true,5);addText(b,game.gold+" септимов",16,ITEM_GOLD,true,0);
        addText(b,itemNumber(game.progress.carryWeight())+" / 50 кг",12,MUTED,false,0);
    }
    private void themedNotice(String message){
        if(!visual("notice")||root==null||!root.isAttachedToWindow()){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();return;}
        android.view.ViewGroup decor=findViewById(android.R.id.content);
        if(!(decor instanceof FrameLayout)){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();return;}
        TextView notice=text(message,14,TEXT,true);notice.setGravity(Gravity.CENTER);notice.setPadding(dp(16),dp(12),dp(16),dp(12));
        notice.setBackground(new RuneFrameDrawable(0xff101315,accent(),false));
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);
        p.setMargins(dp(15),dp(12),dp(15),0);decor.addView(notice,p);
        if(animations){notice.setAlpha(0f);notice.animate().alpha(1f).setDuration(160).start();}
        notice.postDelayed(()->{if(notice.getParent()==decor)decor.removeView(notice);},2400);
    }

    private void render(){
        rememberViewport();
        if(game.started&&game.visitedVisual.add(game.progress.location))save();
        String thisPage=viewportKey();
        PageScroll oldScroll=scrollCache.get(thisPage);
        root=column();root.setBackground(new SkyrimBackdropDrawable());root.setFitsSystemWindows(true);
        setContentView(root);
        activeTerrainMap=null;
        // Header and health belong to the scroll document, not a floating overlay.
        // One stable vertical hierarchy: no direction-based translations or clipping.
        scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        scroll.setFocusableInTouchMode(true);
        if(updater!=null)updater.attachMini(root);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout page=column();scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
        topChrome=column();page.addView(topChrome,lp(-1,-2));
        if(!titleOpen){
            renderHeader();
            if(game.started)renderSkillLevelNotice();
            if(game.started)renderBars();
        }
        content=column();content.setPadding(dp(13),dp(17),dp(13),dp(28));
        page.addView(content,lp(-1,-2));
        scroll.requestFocus();
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
        stateEffects();
        SovngardeTypeface.applyTree(root);
        renderedPage=thisPage;
        resumeViewport(oldScroll,scroll,thisPage);
        if(animations){content.setAlpha(0.8f);content.animate().alpha(1f).setDuration(210).setInterpolator(new DecelerateInterpolator()).start();}
    }
    private void renderHeader(){
        header=row();header.setPadding(dp(13),dp(9),dp(13),dp(9));
        header.setBackground(new RuneFrameDrawable(PANEL,BORDER,false));
        topChrome.addView(header,lp(-1,-2));
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
            addText(state,"ДЕНЬ "+game.day+"  |  "+String.format(java.util.Locale.ROOT,"%02d:00",game.hour),11,TEXT,true,2);
            addText(state,"УРОВЕНЬ "+game.level,10,MUTED,false,0);
            header.addView(state,lp(-2,-2));
            if(!prefs.getBoolean("ai_enabled",false)){
                state.setContentDescription("Ожидать и изменить игровое время");
                state.setOnClickListener(v->showWaitDialog());
            }
        }
        topChrome.addView(new SkyrimRuleView(this,accent()),lp(-1,15));
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
        topChrome.addView(panel,area);
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

    // One compact HUD, shared by every started-game tab and both story modes.
    private String hudOwner="";
    private final java.util.Map<String,Integer> hudPrevious=new java.util.HashMap<>();
    private void renderBars(){
        String owner=aiSession+"|"+game.name+"|"+game.race;
        if(!owner.equals(hudOwner)){hudOwner=owner;hudPrevious.clear();}
        LinearLayout stats=column();stats.setPadding(dp(12),dp(7),dp(12),dp(7));
        stats.setBackgroundColor(0xff050505);topChrome.addView(stats,lp(-1,-2));
        LinearLayout resources=row();stats.addView(resources,lp(-1,-2));
        stat(resources,"Здоровье","health",game.hp,game.maxHp,0xffd76060);
        stat(resources,"Выносливость","stamina",game.stamina,game.maxStamina,0xff83b779);
        stat(resources,"Магия","magic",game.magicka,game.maxMagicka,0xff75a2dd);
        View separator=new View(this);separator.setBackgroundColor(0xff303030);
        LinearLayout.LayoutParams rule=lp(-1,1);rule.topMargin=dp(5);rule.bottomMargin=dp(4);
        stats.addView(separator,rule);
        int next=game.level>=100?0:Math.max(0,game.level*80-game.xp);
        double weight=game.progress.carryWeight();
        LinearLayout details=row();stats.addView(details,lp(-1,-2));
        hudMetric(details,"Уровень","level",String.valueOf(game.level),ITEM_GOLD,1f,()->{tab=3;render();});
        hudMetric(details,"Опыт / осталось","experience",game.xp+" / "+(game.level>=100?"MAX":String.valueOf(next)),ITEM_GOLD,1.65f,()->{tab=3;render();});
        hudMetric(details,"Вес, кг","bag",itemNumber(weight)+"/50",weight>50?ITEM_DOWN:weight>=45?0xffe1ac70:TEXT,1.2f,()->{tab=2;render();});
        hudMetric(details,"Септимы","septims",String.valueOf(game.gold),ITEM_GOLD,1f,()->{tab=2;render();});
        hudBar(stats,"xp",game.level>=100?1:game.xp,game.level>=100?1:game.level*80,ITEM_GOLD,3);
    }
    private void stat(LinearLayout host,String name,String glyph,int value,int max,int color){
        LinearLayout col=column();col.setPadding(dp(3),0,dp(3),0);
        host.addView(col,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout line=row();line.setGravity(Gravity.CENTER);
        line.addView(new HudGlyphView(this,glyph,color),lp(16,16));
        TextView num=hudText(value+"/"+max,13,color,true);
        LinearLayout.LayoutParams np=lp(-2,-2);np.leftMargin=dp(3);line.addView(num,np);
        col.addView(line,lp(-1,-2));
        hudBar(col,glyph,value,max,color,6);
        col.addView(hudText(name,10,MUTED,false),lp(-1,-2));
        col.setContentDescription(name+": "+value+" из "+max);
        col.setOnClickListener(v->{tab=3;render();});
    }
    private TextView hudText(String value,int size,int color,boolean bold){
        TextView t=text(value,size,color,bold);
        t.setGravity(Gravity.CENTER);t.setSingleLine(true);
        t.setAutoSizeTextTypeUniformWithConfiguration(9,Math.max(10,sp(size)),1,android.util.TypedValue.COMPLEX_UNIT_SP);
        t.setIncludeFontPadding(false);
        return t;
    }
    private void hudMetric(LinearLayout host,String label,String glyph,String value,int color,float weight,Runnable action){
        LinearLayout col=column();col.setPadding(dp(1),dp(3),dp(1),dp(3));
        host.addView(col,new LinearLayout.LayoutParams(0,-2,weight));
        LinearLayout line=row();line.setGravity(Gravity.CENTER);
        line.addView(new HudGlyphView(this,glyph,color),lp(14,14));
        TextView number=hudText(value,12,color,true);
        LinearLayout.LayoutParams np=lp(-2,-2);np.leftMargin=dp(2);
        number.setMaxWidth(dp(glyph.equals("experience")?104:72));
        line.addView(number,np);col.addView(line,lp(-1,-2));
        col.addView(hudText(label,10,MUTED,false),lp(-1,-2));
        col.setContentDescription(label+": "+value);col.setOnClickListener(v->action.run());
    }
    private void hudBar(LinearLayout host,String key,int value,int max,int color,int height){
        int limit=Math.max(1,max),target=Math.max(0,Math.min(limit,value));
        int previous=hudPrevious.getOrDefault(key,target);hudPrevious.put(key,target);
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        GradientDrawable track=new GradientDrawable();track.setColor(0xff242424);
        GradientDrawable fill=new GradientDrawable();fill.setColor(color);
        android.graphics.drawable.ClipDrawable clipped=new android.graphics.drawable.ClipDrawable(fill,Gravity.LEFT,android.graphics.drawable.ClipDrawable.HORIZONTAL);
        android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{track,clipped});
        layers.setId(0,android.R.id.background);layers.setId(1,android.R.id.progress);
        bar.setProgressDrawable(layers);bar.setMax(limit);bar.setPadding(0,0,0,0);
        bar.setProgress(Math.max(0,Math.min(limit,previous)));
        LinearLayout.LayoutParams bp=lp(-1,height);bp.topMargin=dp(4);bp.bottomMargin=dp(3);host.addView(bar,bp);
        if(animations&&previous!=target)bar.setProgress(target,true);else bar.setProgress(target);
        bar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private void renderLocationWeather(LinearLayout host){
        WorldWeather w=WorldWeather.at(game.region,game.progress.location,game.day,game.hour);
        LinearLayout strip=row();strip.setPadding(dp(4),dp(7),dp(4),dp(6));
        strip.setBackgroundColor(0xff050505);host.addView(strip,lp(-1,-2));
        int tempColor=w.temperature<=0?0xff82b9dc:w.temperature>=25?0xffe7a470:0xffd4c19f;
        weatherMetric(strip,w.timeGlyph(),w.time(),"Время",ITEM_GOLD);
        weatherMetric(strip,w.glyph,w.weather,"Погода",0xffb8c2ce);
        weatherMetric(strip,"temperature",w.formattedTemperature(),"Температура",tempColor);
        strip.setContentDescription("Игровая погода и температура: "+w.weather+", "+w.formattedTemperature()+", "+w.time());
    }
    private void weatherMetric(LinearLayout host,String glyph,String value,String label,int color){
        LinearLayout col=column();col.setGravity(Gravity.CENTER);col.setPadding(dp(2),0,dp(2),0);
        host.addView(col,new LinearLayout.LayoutParams(0,-2,1));
        col.addView(new HudGlyphView(this,glyph,color),lp(25,25));
        TextView number=hudText(value,15,color,true);
        LinearLayout.LayoutParams np=lp(-1,-2);np.topMargin=dp(3);col.addView(number,np);
        TextView caption=hudText(label,10,MUTED,false);
        LinearLayout.LayoutParams cp=lp(-1,-2);cp.topMargin=dp(3);col.addView(caption,cp);
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
    private String iconOf(String title){
        String exact=SkyrimIconAtlas.exactLabel(this,title);
        return exact!=null?exact:SkyrimIconView.iconForLabel(title);
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
        row.setClickable(true);row.setFocusable(true);row.setContentDescription(SceneTypography.displaySeparators(title+". "+detail));
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
            .setTitle("СКАЙРИМ | ТЕКСТОВАЯ RPG")
            .setMessage("Интерактивная история северных земель. Выборы, путешествия, дневник и кодекс. Самостоятельный неофициальный проект.")
            .setPositiveButton("Закрыть",null).show());
        titleRule();
        TextView foot=text("ᛟ  ТВОЯ ИСТОРИЯ НАЧИНАЕТСЯ НА СЕВЕРЕ  ᛟ",10,MUTED,false);
        foot.setGravity(Gravity.CENTER);content.addView(foot,lp(-1,36));
    }
    private void renderNewGame(){
        section("НОВАЯ ИСТОРИЯ","Твоя легенда начинается","Выбери пол, расу и портрет персонажа. Затем определи имя, начало истории и распредели первые пять очков.");
        addText(content,"ПОЛ",11,accent(),true,9);
        chips(CharacterIdentity.GENDERS,selectedGender,value->{
            draftName=nameField.getText().toString();selectedGender=value;selectedPortrait=1;render();
        });
        space(content,9);
        LinearLayout details=card();addText(details,"ИМЯ ПЕРСОНАЖА",11,accent(),true,10);
        LinearLayout nameLine=row();details.addView(nameLine,lp(-1,-2));
        nameField=new EditText(this);nameField.setSingleLine(true);nameField.setTextColor(TEXT);nameField.setHintTextColor(MUTED);
        nameField.setHint("Имя персонажа");nameField.setTextSize(sp(16));
        nameField.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(28)});
        nameField.setPadding(dp(11),dp(8),dp(11),dp(8));nameField.setBackground(new RuneFrameDrawable(PANEL_2,0xff85745f,false));
        nameField.setText(draftName);nameLine.addView(nameField,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView nameDice=text("⟳",26,accent(),true);nameDice.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams diceLp=lp(48,48);diceLp.leftMargin=dp(7);nameLine.addView(nameDice,diceLp);
        nameDice.setBackground(bg(PANEL_2,accent(),10));
        nameDice.setContentDescription("Сгенерировать случайное имя");
        nameDice.setOnClickListener(v->{draftName=CharacterIdentity.FEMALE.equals(selectedGender)?
            RpgContent.randomName(selectedGender):RpgContent.randomName();
            nameField.setText(draftName);nameField.setSelection(draftName.length());});
        addText(details,"Нажми ⟳, чтобы получить случайное имя.",12,MUTED,false,0);

        addText(content,"РАСА",11,accent(),true,9);
        chips(GameEngine.RACES,selectedRace,value->{
            draftName=nameField.getText().toString();selectedRace=value;selectedPortrait=1;render();
        });
        renderPortraitChoices();
        int race=RpgContent.raceIndex(selectedRace);
        LinearLayout racial=card();
        addText(racial,"ПАССИВНЫЙ ЭФФЕКТ",10,accent(),true,6);
        addText(racial,RpgContent.PASSIVES[race],14,TEXT,false,11);
        addText(racial,"РАСОВЫЙ ТАЛАНТ",10,accent(),true,6);
        addText(racial,RpgContent.POWERS[race],14,MUTED,false,0);

        addText(content,"КЛАСС | НЕОБЯЗАТЕЛЬНО",11,accent(),true,9);
        chips(GameEngine.CLASSES,selectedClass,value->{draftName=nameField.getText().toString();selectedClass=value;render();});
        int cls=0;for(int i=0;i<GameEngine.CLASSES.length;i++)if(GameEngine.CLASSES[i].equals(selectedClass))cls=i;
        addText(content,RpgContent.CLASS_DETAILS[cls],13,MUTED,false,15);

        LinearLayout stats=card();addText(stats,"СТАРТОВЫЕ ХАРАКТЕРИСТИКИ",11,accent(),true,7);
        int used=initialPoints[0]+initialPoints[1]+initialPoints[2];
        addText(stats,"Свободных очков: "+(5-used)+" / 5  |  +10 к выбранному ресурсу за очко",13,MUTED,false,12);
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
            newAiSession();game.start(nameField.getText().toString(),selectedRace,selectedClass,selectedStart,
                initialPoints,selectedGender,selectedPortrait);
            tab=0;save();render();
        });
        addText(content,"Неофициальная самостоятельная история в знакомом северном мире.",12,MUTED,false,0);
    }
    private void renderPortraitChoices(){
        addText(content,"ПОРТРЕТ",11,accent(),true,9);
        LinearLayout choices=row();content.addView(choices,lp(-1,-2));
        for(int i=1;i<=CharacterIdentity.PORTRAITS_PER_GENDER;i++){
            final int portrait=i;
            LinearLayout tile=column();tile.setTag(i);tile.setPadding(dp(3),dp(3),dp(3),dp(3));
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);
            if(i>1)params.leftMargin=dp(7);
            choices.addView(tile,params);
            tile.addView(new CharacterPortraitView(this,selectedRace,selectedGender,i),lp(-1,-2));
            TextView label=text("",10,TEXT,true);label.setGravity(Gravity.CENTER);
            tile.addView(label,lp(-1,32));
            tile.setFocusable(true);
            tile.setOnClickListener(v->{selectedPortrait=portrait;updatePortraitChoices(choices);});
        }
        updatePortraitChoices(choices);
        addText(content,"Выбери один из трёх образов. Он будет отображаться в личном деле.",12,MUTED,false,15);
        String portraitGroup=ResourceStore.portraitGroup(CharacterIdentity.portraitPath(selectedRace,selectedGender,1));
        if(ResourceStore.groupMissing(this,portraitGroup))
            actionButton("Загрузить портреты","Отдельный пакет выбранной расы | выбор образа доступен и без фото",false,
                ()->new ResourceManagerDialog(this,accent(),this::render,portraitGroup).show());
    }
    private void updatePortraitChoices(LinearLayout choices){
        for(int i=0;i<choices.getChildCount();i++){
            LinearLayout tile=(LinearLayout)choices.getChildAt(i);
            int portrait=(Integer)tile.getTag();boolean active=portrait==selectedPortrait;
            tile.setSelected(active);
            tile.setBackground(new RuneFrameDrawable(PANEL_2,active?accent():BORDER,active));
            tile.setContentDescription(selectedRace+", "+selectedGender+", образ "+portrait+
                (active?", выбран":""));
            TextView label=(TextView)tile.getChildAt(1);
            label.setText(active?"ВЫБРАН":"ОБРАЗ "+portrait);label.setTextColor(active?accent():MUTED);
        }
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
            content.addView(sceneGallery(game.progress.location),lp(-1,-2));
            renderLocationWeather(content);
        }else{
            content.addView(new NordicArtView(this,NordicArtView.STORY),lp(-1,188));
        }
        space(content,16);
        LinearLayout narrative=card();
        addText(narrative,"ᚱ  ГЛАВА  |  "+s.kicker.toUpperCase(java.util.Locale.ROOT),11,accent(),false,13);
        TextView body=text("",18,TEXT,false);
        body.setTypeface(SovngardeTypeface.face(this,false));
        body.setText(SceneTypography.render(s.body,accent(),game.name,heroColor()));
        body.setLineSpacing(dp(7),1.16f);
        makeItemsClickable(body,body.getText().toString());
        narrative.addView(body);
        decorateNarrative(narrative,body,s.body);
        renderVisualResults();tradeVisual();
        renderItemGains();
        renderStoryJournal();
        if(prefs.getBoolean("ai_enabled",false)) renderAiStoryPanel();
        space(content,12);
        addText(content,"Что ты предпримешь?",22,TEXT,true,7);
        addText(content,s.choices.size()+" доступных вариантов. Вероятность показана только для настоящих проверок.",12,MUTED,false,15);
        int number=0;
        for(GameEngine.Choice c:s.choices){
            if("SHOP".equals(game.mode))renderShopItem(c.action);
            storyChoice(++number,c,()->{
                if(aiClient!=null)aiClient.cancel();if(localAiClient!=null)localAiClient.cancel();
                int old=game.questStage;
                java.util.Map<String,Integer> before=itemSnapshot(game);
                VisualSnapshot visualBefore=new VisualSnapshot(game);
                game.choose(c.action);
                if(!c.action.startsWith("ACT:"))game.progress.observe(game,c.action,old);
                game.progress.sync(game);
                captureItemGains(before,game,false);
                recordVisual(visualBefore,game,c.title);
                if(prefs.getBoolean("ai_enabled",false))
                    aiMemory.add(aiSession,"event","Игровое действие: "+c.title+". Текущее сообщение движка: "+game.message);
                save();tab=0;render();
            });
        }
        space(content,5);
        addText(content,"Каждое действие сохраняется автоматически",11,MUTED,false,0);
    }

    private void renderStorySnapshot(){
        addText(content,game.name+" | "+game.race+" | Уровень "+game.level,17,TEXT,true,7);
        addText(content,"⌖ "+game.progress.location+"  |  День "+game.day+"  |  Опыт "+game.xp,13,MUTED,false,7);
        addText(content,"Вес: "+
            String.format(java.util.Locale.forLanguageTag("ru"),"%.1f",game.progress.carryWeight())+
            "/50 кг  |  "+game.gold+" септимов",13,accent(),false,12);
    }

    private void renderStoryJournal(){
        if(!game.started)return;
        if(!"QUEST".equals(game.mode)&&!"DRAGON".equals(game.mode)
            &&!"EVENT".equals(game.mode)&&!"RESULT".equals(game.mode))return;
        LinearLayout journal=card();
        addText(journal,"ᚱ  ЖУРНАЛ ПРИКЛЮЧЕНИЙ",11,accent(),true,10);
        if(game.questStage>0&&game.questStage<4)
            addText(journal,"◉  Пепельные печати | "+game.questStatus(),13,TEXT,false,7);
        if(game.progress.dragonStage>=0&&game.progress.dragonStage<DragonArc.FINAL_STAGE)
            addText(journal,"◉  Драконий след | "+DragonArc.status(game),13,TEXT,false,7);
        if(game.sideQuest==1)
            addText(journal,"◉  Опасные дороги | "+game.sideKills+"/3",13,MUTED,false,7);
        if("EVENT".equals(game.mode))
            addText(journal,"!  Случайная встреча | исход ещё неизвестен",13,0xfff18e60,true,1);
    }

    /** Only the value is bold; the attribute label remains regular. */
    private void addChoiceStat(LinearLayout target,String label,String value,int valueColor,int bottom){
        String display=label+": "+value;
        TextView line=text(display,13,MUTED,false);
        android.text.SpannableString styled=new android.text.SpannableString(display);
        int start=display.length()-value.length();
        styled.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),start,display.length(),
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new android.text.style.ForegroundColorSpan(valueColor),start,display.length(),
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        line.setText(styled);
        target.addView(line,lp(-1,-2));
        if(bottom>0)space(target,bottom);
    }

    private void storyChoice(int position,GameEngine.Choice c,Runnable onClick){
        int chance=game.progress.chance(game,c.action);
        LinearLayout box=row();box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(14),dp(14),dp(14),dp(14));
        box.setBackground(new RuneFrameDrawable(PANEL,c.special?accent():BORDER,c.special));
        if(showChoiceNumbers()){
            TextView ordinal=text(String.valueOf(position),17,c.special?accent():MUTED,true);
            ordinal.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ordinalSize=lp(32,36);
            ordinalSize.rightMargin=dp(8);
            box.addView(ordinal,ordinalSize);
        }
        SkyrimIconView glyph=new SkyrimIconView(this,
            chance>=0?skillGlyph(SceneChoiceMeta.skillName(c.action)):"skills",accent());
        if(visual("choices"))box.addView(glyph,lp(31,34));
        LinearLayout prose=column();
        LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-2,1);
        size.leftMargin=dp(13);box.addView(prose,size);
        TextView name=text(c.title,17,TEXT,true);
        name.setTypeface(SovngardeTypeface.face(this,true));
        name.setLineSpacing(dp(3),1.1f);prose.addView(name);
        if(c.detail!=null&&!c.detail.isEmpty()){
            space(prose,5);
            TextView desc=text(c.detail,14,MUTED,false);
            desc.setLineSpacing(dp(3),1.12f);prose.addView(desc);
        }
        space(prose,6);
        choiceExtras(prose,c.action);
        if(visual("travel")&&c.action.startsWith("REGION:")){
            int target=Integer.parseInt(c.action.substring(7));
            if(target>=0&&target<TravelPlanner.HUBS.length)routePreview(prose,TravelPlanner.plan(game,WorldAtlas.find(TravelPlanner.HUBS[target])));
        }
        if(chance>=0){
            addChoiceStat(prose,"НАВЫК",SceneChoiceMeta.skillName(c.action),accent(),4);
            addChoiceStat(prose,"ШАНС УСПЕХА",chance+"%",SceneChoiceMeta.chanceColor(chance),0);
        }else addText(prose,"БЕЗ ПРОВЕРКИ",11,0xffb8c1c4,false,0);
        LinearLayout.LayoutParams layout=lp(-1,-2);layout.bottomMargin=dp(5);
        content.addView(box,layout);
        box.setClickable(true);box.setFocusable(true);
        box.setOnClickListener(view->onClick.run());
    }

    /** A failed network continuation must not reroll an already evaluated action. */
    private void persistAiDraft(){
        if(prefs==null||game==null||game.aiTurn==null)return;
        StringBuilder ids=new StringBuilder();
        for(Integer i:selectedAiChoices){
            if(ids.length()>0)ids.append(',');
            ids.append(i);
        }
        prefs.edit().putInt("ai_draft_scene",game.aiTurn.scene.hashCode())
            .putString("ai_draft_selected",ids.toString())
            .putString("ai_draft_text",aiOwnDraft).apply();
    }
    private void restoreAiDraft(){
        if(game==null||game.aiTurn==null
                ||prefs.getInt("ai_draft_scene",0)!=game.aiTurn.scene.hashCode())return;
        aiOwnDraft=prefs.getString("ai_draft_text","");
        for(String piece:prefs.getString("ai_draft_selected","").split(",")){
            try{
                int i=Integer.parseInt(piece.trim());
                if(i>=1&&i<=game.aiTurn.choices.size())selectedAiChoices.add(i);
            }catch(NumberFormatException ignored){}
        }
    }
    private void clearAiDraft(){
        prefs.edit().remove("ai_draft_scene").remove("ai_draft_selected")
            .remove("ai_draft_text").apply();
    }

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
        String source=resolvedAiProvider();
        boolean ready="local".equals(source)?localModelReady():
            "chatgpt".equals(source)?chatGptOAuth.hasAuthorizedPlan():
            "api".equals(source)&&AiKeyVault.configured(this);
        if(aiProgressInfo!=null&&aiDotsTicker!=null)
            aiProgressInfo.removeCallbacks(aiDotsTicker);
        aiDotsTicker=null;
        aiTurnText=null;aiProgressInfo=null;aiReasoningText=null;
        if(LocationGalleryView.hasGallery(game.progress.location)){
            addText(content,"ᛟ  "+game.progress.location.toUpperCase(java.util.Locale.ROOT),12,accent(),true,10);
            content.addView(sceneGallery(game.progress.location),lp(-1,-2));
            renderLocationWeather(content);
            space(content,13);
        }
        if(!ready){
            LinearLayout card=card();
            addText(card,"ИИ-ИГРА НЕ ПОДКЛЮЧЕНА",14,accent(),true,11);
            addText(card,"Подключи выбранный источник в настройках. Обычный текст игры скрыт, поскольку выбран режим ИИ.",14,TEXT,false,13);
            actionButton("НАСТРОИТЬ ИИ","Выбор GGUF, ChatGPT или OpenAI API",true,()->{tab=5;render();});
            return;
        }
        if(aiTurnBusy) {
            LinearLayout activity=row();
            activity.setGravity(Gravity.CENTER_VERTICAL);
            activity.setPadding(dp(11),dp(16),dp(11),dp(16));
            ProgressBar spinner=new ProgressBar(this);
            spinner.setIndeterminate(true);
            spinner.setIndeterminateTintList(ColorStateList.valueOf(accent()));
            activity.addView(spinner,lp(35,35));
            aiProgressInfo=text("Генерация ответа",16,TEXT,true);
            LinearLayout.LayoutParams statusLayout=new LinearLayout.LayoutParams(0,-2,1);
            statusLayout.leftMargin=dp(13);
            activity.addView(aiProgressInfo,statusLayout);
            content.addView(activity,lp(-1,-2));
            final TextView progressLabel=aiProgressInfo;
            aiDotsTicker=new Runnable(){
                private int phase=0;
                private final String[] frames={"",".","..","..."};
                @Override public void run(){
                    if(!aiTurnBusy||tab!=0||aiProgressInfo!=progressLabel)return;
                    String label="local".equals(resolvedAiProvider())?aiLocalPhase:"Генерация ответа";
                    if("local".equals(resolvedAiProvider())&&aiLocalStartedAt>0){
                        long elapsed=(android.os.SystemClock.elapsedRealtime()-aiLocalStartedAt)/1000L;
                        aiProgressInfo.setText(label+"\nПрошло "+elapsed+" с"+frames[phase++%frames.length]);
                    }else aiProgressInfo.setText("Генерация ответа"+frames[phase++%frames.length]);
                    aiProgressInfo.postDelayed(this,430L);
                }
            };
            aiProgressInfo.post(aiDotsTicker);
            if("local".equals(source)){
                LinearLayout live=card();
                addText(live,"ТЕКСТ ЛОКАЛЬНОЙ МОДЕЛИ | ПРЕДПРОСМОТР",11,accent(),true,9);
                aiTurnText=text(aiTurnPreview.isEmpty()?"Ожидание первых токенов…" : aiTurnPreview,17,TEXT,false);
                aiTurnText.setTypeface(SovngardeTypeface.face(this,false));
                aiTurnText.setLineSpacing(dp(5),1.12f);
                live.addView(aiTurnText,lp(-1,-2));
                addText(live,"Пока сцена генерируется, сохранение и игровые характеристики не изменяются.",12,MUTED,false,4);
            }
            space(content,7);
            actionButton("ОСТАНОВИТЬ","Прервать генерацию без изменения сохранения",false,()->{
                if(aiClient!=null)aiClient.cancel();if(localAiClient!=null)localAiClient.cancel();
                awaitingAiToken=false;aiTurnBusy=false;aiTurnPreview="";
                aiTurnStatus="Генерация остановлена, изменения не сохранены";
                render();
            });
            return;
        }
        if(!aiTurnStatus.isEmpty())addText(content,aiTurnStatus,12,MUTED,false,11);
        if("COMBAT".equals(game.mode)&&game.enemyHp>0){
            addText(content,"БОЙ | "+game.enemyName+" | "+game.enemyHp+"/"+game.enemyMaxHp,14,accent(),true,8);
            actionButton("УДАР","Обычная атака",true,()->resolveNativeAiCombat("ATTACK"));
            actionButton("СИЛЬНЫЙ УДАР","10 единиц сил | "+game.progress.chance(game,"HEAVY")+"%",game.stamina>=10,()->resolveNativeAiCombat("HEAVY"));
            actionButton("МАГИЯ","12 единиц магии",game.magicka>=12,()->resolveNativeAiCombat("MAGIC"));
            actionButton("ЗАЩИТА","Восстановить силы и уменьшить входящий урон",true,()->resolveNativeAiCombat("DEFEND"));
            actionButton("ЛЕЧЕБНОЕ ЗЕЛЬЕ","В наличии: "+game.potions,game.potions>0,()->resolveNativeAiCombat("POTION"));
            if("WILD".equals(game.battleKind))
                actionButton("ОТСТУПИТЬ","Шанс: "+game.progress.chance(game,"FLEE")+"%",true,()->resolveNativeAiCombat("FLEE"));
        }
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
        aiTurnText.setText(SceneTypography.render(game.aiTurn.scene,accent(),game.name,heroColor()));
        makeItemsClickable(aiTurnText,aiTurnText.getText().toString());
        narrative.addView(aiTurnText,lp(-1,-2));
        decorateNarrative(narrative,aiTurnText,game.aiTurn.scene);
        renderVisualResults();
        renderItemGains();
        space(content,16);
        addText(content,"ВЫБЕРИ ОДИН ИЛИ НЕСКОЛЬКО ВАРИАНТОВ. ОТПРАВКА ТОЛЬКО ПО КНОПКЕ НИЖЕ.",12,MUTED,false,11);
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
        if(!aiOwnDraft.isEmpty())own.setText(aiOwnDraft);
        else if(selectedAiChoices.isEmpty()){
            String existingPending=pendingAiAction();
            if(!existingPending.isEmpty()&&!existingPending.startsWith("ВЫБРАННЫЕ ВАРИАНТЫ")
                    &&!existingPending.startsWith("СОБСТВЕННЫЙ ТЕКСТ"))
                own.setText(existingPending);
        }
        own.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                aiOwnDraft=s.toString();persistAiDraft();
            }
            public void afterTextChanged(android.text.Editable e){}
        });
        own.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        own.setMinLines(3);own.setMaxLines(7);
        own.setPadding(dp(13),dp(11),dp(13),dp(11));
        own.setBackground(new RuneFrameDrawable(PANEL_2,BORDER,false));
        content.addView(own,lp(-1,-2));
        space(content,10);
        actionButton("ОТПРАВИТЬ","Отправить выбранные ответы и свой текст одним действием",true,()->{
            String value=own.getText().toString().trim();
            if(value.length()>1500){toast("Не более 1500 символов своего текста");return;}
            java.util.ArrayList<Integer> indexes=new java.util.ArrayList<>(selectedAiChoices);
            java.util.Collections.sort(indexes);
            if(indexes.isEmpty()&&value.isEmpty()){toast("Выбери вариант или напиши свой ответ");return;}
            // Combat is authoritative: never submit a generic AI effects patch during a live encounter.
            if("COMBAT".equals(game.mode)&&game.enemyHp>0){
                String action;
                if(indexes.isEmpty()){
                    action=typedNativeCombatAction(value);
                    if(action.isEmpty()){
                        toast("Укажи одну команду: удар, сильный удар, магия, защита, зелье или отступление");return;
                    }
                }else{
                    if(indexes.size()!=1||!value.isEmpty()){
                        toast("В бою выбери одну команду или введи её текстом");return;
                    }
                    int index=indexes.get(0)-1;
                    if(index<0||index>=game.aiTurn.choices.size())return;
                    action=game.aiTurn.choices.get(index).combatAction;
                }
                if(!nativeAiCombatAvailable(action)){
                    toast("Это боевое действие сейчас недоступно");return;
                }
                resolveNativeAiCombat(action);
                return;
            }
            StringBuilder combined=new StringBuilder();
            if(!indexes.isEmpty()){
                combined.append("ВЫБРАННЫЕ ВАРИАНТЫ ИГРОКА (обработай по порядку):\n");
                for(Integer index:indexes){
                    if(index<1||index>game.aiTurn.choices.size())continue;
                    AiGameTurn.Option option=game.aiTurn.choices.get(index-1);
                    combined.append(index).append(". ").append(option.title).append(". ")
                        .append(option.detail).append("\n");
                }
            }
            if(!value.isEmpty())combined.append("СОБСТВЕННЫЙ ТЕКСТ ИГРОКА:\n")
                .append(value).append("\n");
            String compound=combined.toString();
            persistAiDraft();
            if(!value.isEmpty()){
                if(compound.equals(pendingAiAction())){
                    int chance=prefs.getInt(AI_PENDING_CHANCE,-1);
                    int die=prefs.getInt(AI_PENDING_DIE,-1);
                    requestAiTurn(lockedFreeInstruction(compound,chance,die),true,false,chance,null);
                    return;
                }
                clearAiPending();
                requestAiTurn("ОЦЕНКА СВОБОДНОГО ДЕЙСТВИЯ: "+compound+
                    "\nОцени единый шанс составного действия. Не исполняй до броска Android.",false,true,-2,compound);
                return;
            }
            clearAiPending();
            StringBuilder instruction=new StringBuilder(compound);
            instruction.append("\nУчитывай конфликты действий: невозможное не исполняй. ");
            instruction.append("Исходы выполнимых действий уже установлены Android:\n");
            for(Integer index:indexes){
                if(index<1||index>game.aiTurn.choices.size())continue;
                AiGameTurn.Option option=game.aiTurn.choices.get(index-1);
                int die=option.chance<0?-1:AiGameTurn.roll();
                instruction.append("Вариант ").append(index).append(": ");
                if(option.chance<0)instruction.append("без проверки.");
                else instruction.append("шанс ").append(option.chance).append("%, бросок ")
                    .append(die).append("/100, ")
                    .append(AiGameTurn.success(option.chance,die)?"УСПЕХ.":"НЕУДАЧА.");
                instruction.append("\n");
            }
            instruction.append("Продолжи историю, применив effects только за совершённые действия.");
            requestAiTurn(instruction.toString(),true);
        });
    }

    /** Native combat is committed once before requesting prose; a failed model
     * request cannot reroll the attack or duplicate its rewards. */
    // Exact aliases only: never infer an attack from negation or compound prose.
    static String typedNativeCombatAction(String value){
        if(value==null)return "";
        String command=value.trim().toLowerCase(java.util.Locale.ROOT)
            .replace('ё','е').replaceAll("\\s+"," ").replaceAll("[.!]+$","").trim();
        switch(command){
            case "удар":case "атаковать":case "атакую":case "обычная атака":return "ATTACK";
            case "сильный удар":case "мощный удар":case "силовая атака":return "HEAVY";
            case "магия":case "магическая атака":case "атакую магией":return "MAGIC";
            case "защита":case "защищаюсь":case "блок":case "блокировать":return "DEFEND";
            case "зелье":case "лечебное зелье":case "выпить лечебное зелье":return "POTION";
            case "отступление":case "отступить":case "отступаю":case "бежать":return "FLEE";
            default:return "";
        }
    }

    private boolean nativeAiCombatAvailable(String action){
        if(!"COMBAT".equals(game.mode)||game.enemyHp<=0)return false;
        switch(action){
            case "ATTACK":case "DEFEND":return true;
            case "HEAVY":return game.stamina>=10;
            case "MAGIC":return game.magicka>=12;
            case "POTION":return game.potions>0;
            case "FLEE":return "WILD".equals(game.battleKind);
            default:return false;
        }
    }

    private void resolveNativeAiCombat(String action){
        if(aiTurnBusy||!"COMBAT".equals(game.mode)||game.enemyHp<=0)return;
        if("HEAVY".equals(action)&&game.stamina<10)return;
        if("MAGIC".equals(action)&&game.magicka<12)return;
        if("POTION".equals(action)&&game.potions<=0)return;
        if("FLEE".equals(action)&&!"WILD".equals(game.battleKind))return;
        if(!nativeAiCombatAvailable(action))return;
        java.util.Map<String,Integer> before=itemSnapshot(game);
        VisualSnapshot visualBefore=new VisualSnapshot(game);
        game.choose(action);
        game.progress.sync(game);
        captureItemGains(before,game,false);
        recordVisual(visualBefore,game,action);
        String outcome="COMBAT".equals(game.mode)?game.battleLog:game.message;
        // Native retreat and defeat leave the old enemy HP in the engine.
        // Clear the finished encounter before sending the next AI context.
        if(!"COMBAT".equals(game.mode)){
            game.enemyHp=0;game.enemyMaxHp=0;game.enemyDamage=0;
            game.enemyName="";game.battleKind="";
        }
        game.aiTurn=null;
        selectedAiChoices.clear();aiOwnDraft="";clearAiDraft();clearAiPending();
        save();
        aiMemory.add(aiSession,"event",outcome);
        requestAiTurn("Движок уже выполнил боевое действие. Итог: "+outcome+
            "\nОпиши только подтверждённый исход и предложи десять следующих вариантов. Не применяй effects.",false);
        if(!aiTurnBusy)render();
    }

    private void renderAiChoice(int number,AiGameTurn.Option option){
        LinearLayout row=row();row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14),dp(14),dp(14),dp(14));
        boolean selected=selectedAiChoices.contains(number);
        row.setBackground(new RuneFrameDrawable(PANEL,selected?accent():BORDER,selected));
        TextView ordinal=showChoiceNumbers()?text(String.valueOf(number),17,
            selected?accent():MUTED,true):null;
        if(ordinal!=null){
            ordinal.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ordinalSize=lp(32,36);
            ordinalSize.rightMargin=dp(8);
            row.addView(ordinal,ordinalSize);
        }
        SkyrimIconView glyph=new SkyrimIconView(this,skillGlyph(option.skill),accent());
        if(visual("choices"))row.addView(glyph,lp(31,34));
        LinearLayout detail=column();
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
        p.leftMargin=dp(13);row.addView(detail,p);
        TextView title=text(option.title,17,TEXT,true);
        title.setTypeface(SovngardeTypeface.face(this,true));
        detail.addView(title);space(detail,6);
        addText(detail,option.detail,14,MUTED,false,7);
        choiceExtras(detail,option.combatAction);
        boolean combat="COMBAT".equals(game.mode)&&game.enemyHp>0;
        int displayedChance=combat?("HEAVY".equals(option.combatAction)||"FLEE".equals(option.combatAction)
            ?game.progress.chance(game,option.combatAction):-1):option.chance;
        if(displayedChance>=0){
            addChoiceStat(detail,"НАВЫК",option.skill.isEmpty()?"Способности":option.skill,accent(),3);
            addChoiceStat(detail,"ШАНС УСПЕХА",displayedChance+"%",
                SceneChoiceMeta.chanceColor(displayedChance),0);
        }else addText(detail,"БЕЗ ПРОВЕРКИ",12,MUTED,false,0);
        boolean available=!combat||nativeAiCombatAvailable(option.combatAction);
        if(!available){
            addText(detail,"ДЕЙСТВИЕ НЕДОСТУПНО",12,MUTED,true,0);
            row.setAlpha(0.55f);
        }
        row.setClickable(available);row.setFocusable(available);
        row.setContentDescription("Вариант "+number+". "+option.title+". "+(selected?"Выбран":"Не выбран"));
        row.setOnClickListener(v->{
            if(!available)return;
            if(combat){
                boolean wasSelected=selectedAiChoices.contains(number);
                selectedAiChoices.clear();
                if(!wasSelected)selectedAiChoices.add(number);
                persistAiDraft();render();return;
            }
            if(!selectedAiChoices.add(number))selectedAiChoices.remove(number);
            persistAiDraft();
            boolean active=selectedAiChoices.contains(number);
            row.setBackground(new RuneFrameDrawable(PANEL,active?accent():BORDER,active));
            if(ordinal!=null)ordinal.setTextColor(active?accent():MUTED);
            row.setContentDescription("Вариант "+number+". "+option.title+". "+(active?"Выбран":"Не выбран"));
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
        if(aiTurnBusy||aiClient.isRunning()||localAiClient.isRunning()||!game.started)return;
        final String source=resolvedAiProvider();
        final boolean local="local".equals(source),chat="chatgpt".equals(source);
        final String model=local?"Qwen3.5-9B":prefs.getString(chat?"ai_chatgpt_model":"ai_api_model",
                chat?"":AiModelCatalog.DEFAULT_API);
        if(model.isEmpty()){aiTurnStatus="Сначала выбери модель в настройках";render();return;}
        final String effort=selectedReasoningEffort(),history=aiMemory.context(aiSession);
        final String saved;
        try{saved=game.toJson().toString();}
        catch(Exception ex){aiTurnStatus="Не удалось прочитать состояние героя";render();return;}
        aiTurnBusy=true;aiTurnPreview="";aiReasoningPreview="";
        if(local){
            aiLocalStartedAt=android.os.SystemClock.elapsedRealtime();
            aiLocalPhase="Запуск модели…";
        }
        aiTurnStatus=proposalOnly?"Определение вероятности действия…":
            local?"Локальная генерация без интернета…":chat?"Подключение к ChatGPT…":"Создание игровой сцены…";
        render();
        final AiStoryClient.Listener listener=new AiStoryClient.Listener(){
            @Override public void onLocalStatus(String status){
                if(!aiTurnBusy)return;
                aiLocalPhase=status;
                if(aiProgressInfo!=null)aiProgressInfo.setText(status);
            }
            @Override public void onLocalMetrics(int tokens,float speed,long elapsed){
                prefs.edit().putFloat("ai_local_last_tps",speed).putInt("ai_local_last_tokens",tokens).apply();
            }
            @Override public void onText(String accumulated){
                if(!aiTurnBusy)return;
                if(proposalOnly)return;
                String visible=AiGameTurn.previewScene(accumulated);
                if(visible.isEmpty()||visible.equals(aiTurnPreview))return;
                aiTurnPreview=visible;
                // Keep streaming text buffered; the waiting screen shows only the
                // animated generation label, without distracting counters.
                if(tab==0&&aiTurnText!=null&&prefs.getBoolean("ai_enabled",false)){
                    aiTurnText.setText(SceneTypography.render(visible,accent(),game.name,heroColor()));
                }
            }
            @Override public void onReasoningSummary(String summary){
                if(!aiTurnBusy)return;
                aiReasoningPreview=summary;
                // Reasoning summaries remain internal to the live request UI.
                // Do not replace the compact generation status with debug text.
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
                    java.util.Map<String,Integer> before=itemSnapshot(next);
                    VisualSnapshot visualBefore=new VisualSnapshot(next);
                    completed.apply(next,actionResolved);
                    next.progress.sync(next);
                    captureItemGains(before,next,!actionResolved);
                    if(!actionResolved)recordVisual(visualBefore,next,completed.journal.isEmpty()?"Продолжение истории":completed.journal);
                    else next.recordVisualEvent(completed.journal.isEmpty()?"Продолжение истории":completed.journal);
                    next.aiTurn=completed;
                    game=next;
                    selectedAiChoices.clear();aiOwnDraft="";clearAiDraft();
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
        if(local){
            if(!localModelReady()){
                aiTurnBusy=false;aiTurnStatus="GGUF не импортирован. Выбери модель в настройках";render();return;
            }
            boolean started=localAiClient.generate(LocalAiModelStore.path(prefs),game,history,
                instruction,true,proposalOnly,listener);
            if(!started){
                aiTurnBusy=false;aiTurnStatus="Локальная модель занята или недоступна";render();
            }
        }else if(chat){
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
        return ("chatgpt".equals(mode)||"api".equals(mode)||"local".equals(mode))?mode:"auto";
    }
    private boolean usingChatGpt(){
        return "chatgpt".equals(resolvedAiProvider());
    }
    private void aiProviderDialog(){
        String[] labels={"Автоматически | Работа/Codex → API → офлайн",
                "ChatGPT | расход лимитов Работа/Codex","OpenAI API | отдельная оплата",
                "Локальная модель | GGUF | без интернета"};
        String mode=aiProviderMode();
        int selected="chatgpt".equals(mode)?1:"api".equals(mode)?2:"local".equals(mode)?3:0;
        new RuneDialogBuilder(this,accent()).setTitle("Источник ИИ")
            .setSingleChoiceItems(labels,selected,(dialog,which)->{
                prefs.edit().putString("ai_provider",which==1?"chatgpt":which==2?"api":which==3?"local":"auto").apply();
                cancelAiForSourceChange();
                dialog.dismiss();render();
            }).setNegativeButton("Закрыть",null).show();
    }
    private void cancelAiForSourceChange(){
        awaitingAiToken=false;
        aiTurnBusy=false;
        aiTurnPreview="";
        aiReasoningPreview="";
        if(aiClient!=null)aiClient.cancel();
        if(localAiClient!=null)localAiClient.cancel();
    }
    private String selectedReasoningEffort(){
        return AiReasoningEffort.normalize(prefs.getString("ai_reasoning_effort",AiReasoningEffort.DEFAULT));
    }
    private void aiReasoningEffortDialog(){
        String[] choices=AiReasoningEffort.LABELS;
        int selected=AiReasoningEffort.position(selectedReasoningEffort());
        new RuneDialogBuilder(this,accent()).setTitle("Уровень размышления | настройка игры")
            .setSingleChoiceItems(choices,selected,(dialog,which)->{
                String effort=AiReasoningEffort.VALUES[which];
                prefs.edit().putString("ai_reasoning_effort",effort).apply();
                if(aiClient!=null)aiClient.cancel();if(localAiClient!=null)localAiClient.cancel();
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
                if(aiClient!=null)aiClient.cancel();if(localAiClient!=null)localAiClient.cancel();
                dialog.dismiss();render();
            }).setNegativeButton("Закрыть",null).show();
    }
    private void loginChatGpt(){
        if(accountActionBusy||chatGptOAuth.hasSignedInAccount())return;
        accountActionBusy=true;
        toast("Вход ChatGPT: для игры используются лимиты Работа/Codex");
        chatGptOAuth.signIn(this,(success,message)->{
            accountActionBusy=false;
            if(isFinishing()||isDestroyed())return;
            toast(message);
            if(tab==5)render();
            if(success)chatGptOAuth.loadModels((models,error)->{
                if(isFinishing()||isDestroyed()||!chatGptOAuth.hasAuthorizedPlan()||accountActionBusy)return;
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
        if(tab==5)render();
    }
    private void logoutChatGpt(){
        if(accountActionBusy||!chatGptOAuth.hasSignedInAccount())return;
        new RuneDialogBuilder(this,accent()).setTitle("Выйти из ChatGPT?")
            .setNegativeButton("Отмена",null).setPositiveButton("Выйти",(d,w)->{
                if(accountActionBusy)return;
                accountActionBusy=true;
                cancelAiForSourceChange();
                chatGptOAuth.signOut((success,message)->{
                    accountActionBusy=false;
                    if(isFinishing()||isDestroyed())return;
                    toast(message);
                    if(tab==5)render();
                });
                if(tab==5)render();
            }).show();
    }
    private void chatGptModelDialog(){
        toast("Получаю доступные модели ChatGPT для Работы/Codex…");
        chatGptOAuth.loadModels((models,error)->{
            if(isFinishing()||isDestroyed()||tab!=5||!usingChatGpt()
                ||!chatGptOAuth.hasAuthorizedPlan()||accountActionBusy)return;
            if(error!=null){toast(error);return;}
            if(models.isEmpty()){toast("Нет доступных моделей ChatGPT");return;}
            String[] labels=new String[models.size()];
            int selected=0;String chosen=prefs.getString("ai_chatgpt_model","");
            for(int i=0;i<models.size();i++){
                labels[i]=models.get(i).title+" ("+models.get(i).slug+")";
                if(chosen.equals(models.get(i).slug))selected=i;
            }
            if(isFinishing()||isDestroyed())return;
            new RuneDialogBuilder(this,accent()).setTitle("Модели | Работа/Codex")
                .setSingleChoiceItems(labels,selected,(dialog,which)->{
                    prefs.edit().putString("ai_chatgpt_model",models.get(which).slug).apply();
                    if(aiClient!=null)aiClient.cancel();if(localAiClient!=null)localAiClient.cancel();
                    dialog.dismiss();render();
                }).setNegativeButton("Закрыть",null).show();
        });
    }

    private void renderAiStoryPanel(){
        LinearLayout panel=card();
        String source=resolvedAiProvider();
        addText(panel,"ᚱ  ИИ-МАСТЕР | "+("local".equals(source)?"ОФЛАЙН GGUF":usingChatGpt()?"РАБОТА / CODEX":"OPENAI API"),11,accent(),true,8);
        addText(panel,"Свободный диалог и повествование. Инвентарь, бой, награды и перемещения изменяются только игровыми действиями.",12,MUTED,false,12);
        boolean chatgpt=usingChatGpt();
        boolean local="local".equals(source);
        boolean ready=local?localModelReady():chatgpt?chatGptOAuth.hasAuthorizedPlan():AiKeyVault.configured(this);
        if(!ready){
            addText(panel,chatgpt?"Войди через ChatGPT и разреши использование лимитов в настройках.":
                "Выбери GGUF из памяти или добавь OpenAI API-ключ в настройках.",13,TEXT,false,8);
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
            "ИИ помнит историю беседы. Опиши действие или заговори с персонажем.":previous,
            accent(),game.name,heroColor()));
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
        String selectedModel=local?"Qwen3.5-9B":prefs.getString(chatgpt?"ai_chatgpt_model":"ai_api_model",chatgpt?"":AiModelCatalog.DEFAULT_API);
        String modelLabel=selectedModel.isEmpty()?"не выбрана":local?"Qwen3.5-9B | GGUF":chatgpt?selectedModel:AiModelCatalog.label(selectedModel);
        String providerLabel=local?"Источник: локальная модель | без интернета":chatgpt?"Источник: ChatGPT | лимиты Работа/Codex":
                "Источник: OpenAI API | отдельная оплата";
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
            if(aiClient.isRunning()||localAiClient.isRunning()||awaitingAiToken){
                awaitingAiToken=false;
                aiClient.cancel();if(localAiClient!=null)localAiClient.cancel();
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
            status.setText(local?"Генерация локальной моделью…":chatgpt?"Проверка сессии ChatGPT…":"ИИ создаёт сцену…");
            reply.setText("");
            send.setText("ОСТАНОВИТЬ");
            AiStoryClient.Listener responseListener=new AiStoryClient.Listener(){
                @Override public void onLocalStatus(String message){
                    status.setText(message);
                }
                @Override public void onLocalMetrics(int tokens,float speed,long elapsed){
                    prefs.edit().putFloat("ai_local_last_tps",speed).putInt("ai_local_last_tokens",tokens).apply();
                }
                @Override public void onText(String accumulated){
                    reply.setText(SceneTypography.render(accumulated,accent(),game.name,heroColor()));
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
            if(local){
                boolean started=localAiClient.generate(LocalAiModelStore.path(prefs),game,history,
                    request,false,false,responseListener);
                if(!started){status.setText("Локальная модель занята");send.setText("ПОВТОРИТЬ");}
            }else if(chatgpt){
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
        section("АТЛАС СЕВЕРА","Карта Скайрима","Выбери материк или Солстейм. Нажми на метку, чтобы открыть место и маршрут.");
        LinearLayout overview=card();
        addText(overview,"⌖  ВЫ НАХОДИТЕСЬ: "+game.progress.location,15,accent(),true,10);
        addText(overview,"День "+game.day+"  |  "+SkyrimMapCatalog.size()+" локаций",13,MUTED,false,9);
        final SkyrimTerrainMapView terrainMap=new SkyrimTerrainMapView(this,game,this::locationDialog,hiddenMapTypes);
        activeTerrainMap=terrainMap;
        terrainMap.setMapRegion(selectedMapRegion);
        terrainMap.setOnCatalog(this::atlasSitesDialog);
        LinearLayout sheets=row();overview.addView(sheets,lp(-1,47));
        mapControl(sheets,"Скайрим",()->{terrainMap.setMapRegion(-1);terrainMap.setSheet(0);});
        mapControl(sheets,"Солстейм",()->{terrainMap.setMapRegion(-1);terrainMap.setSheet(1);});
        overview.addView(terrainMap,lp(-1,410));
        LinearLayout mapActions=row();mapActions.setGravity(Gravity.CENTER);
        mapActions.setPadding(0,dp(10),0,dp(8));overview.addView(mapActions,lp(-1,-2));
        mapControl(mapActions,"−",()->terrainMap.zoomBy(1f/1.45f));
        mapControl(mapActions,"+",()->terrainMap.zoomBy(1.45f));
        mapControl(mapActions,"Вся карта",terrainMap::showWholeProvince);
        mapControl(mapActions,"Мой герой",terrainMap::centerOnPlayer);
        TextView fullMap=text("⛶  ОТКРЫТЬ КАРТУ НА ВЕСЬ ЭКРАН",13,accent(),true);
        fullMap.setGravity(Gravity.CENTER);fullMap.setPadding(dp(8),dp(12),dp(8),dp(12));
        fullMap.setBackground(new RuneFrameDrawable(PANEL_2,BORDER,false));overview.addView(fullMap,lp(-1,-2));
        fullMap.setOnClickListener(v->openFullscreenWorldMap());
        addText(overview,"Масштаб — двумя пальцами или кнопками. Группы меток раскрываются при увеличении. Поиск работает по русским и оригинальным названиям.",12,MUTED,false,6);
        addText(overview,"Два полных листа рельефа работают без интернета. Масштаб и положение каждого листа сохраняются отдельно.",11,MUTED,false,7);
        if(LocationGalleryView.hasGallery(game.progress.location)){
            addText(overview,"ᛟ  ГАЛЕРЕЯ ТЕКУЩЕГО МЕСТА",11,accent(),true,12);
            overview.addView(sceneGallery(game.progress.location),lp(-1,-2));
            renderLocationWeather(overview);
        }
        addText(content,"МЕСТА И ПЕРЕХОДЫ",11,accent(),true,10);
        actionButton("ПОИСК ПО ВСЕМ ТОЧКАМ АТЛАСА",SkyrimMapCatalog.size()+" локаций материка и Солстейма",false,()->searchSkyrimAtlas(terrainMap));
        String[] regions=new String[GameEngine.REGIONS.length+1];regions[0]="Все области маршрутов";
        System.arraycopy(GameEngine.REGIONS,0,regions,1,GameEngine.REGIONS.length);
        android.widget.Spinner filter=spinner(regions,selectedMapRegion+1,i->{
            if(selectedMapRegion!=i-1){selectedMapRegion=i-1;mapListLimit=60;render();}
        });
        content.addView(filter,lp(-1,49));space(content,12);renderMapTypeFilters();
        java.util.ArrayList<SkyrimMapCatalog.Site> locations=SkyrimMapCatalog.all();
        locations.removeIf(site->hiddenMapTypes.contains(site.type)||(selectedMapRegion>=0&&site.region!=selectedMapRegion));
        final java.text.Collator names=java.text.Collator.getInstance(new java.util.Locale("ru"));
        locations.sort((a,b)->names.compare(a.name,b.name));
        addText(content,"Найдено локаций: "+locations.size()+"  |  показано "+Math.min(mapListLimit,locations.size()),12,MUTED,false,8);
        int shown=0;
        for(SkyrimMapCatalog.Site site:locations){
            if(shown++>=mapListLimit)break;
            mapLocationRow(content,site,()->{
                terrainMap.centerOn(site);
                atlasSiteDialog(site);
            });
        }
        if(locations.size()>mapListLimit)actionButton("ПОКАЗАТЬ ЕЩЁ 60","Осталось "+(locations.size()-mapListLimit),false,()->{mapListLimit+=60;render();});
        addText(content,"Время пути рассчитывается по игровой сети переходов. Области маршрутов привязаны к городам; это не границы владений.",11,MUTED,false,9);
        addText(content,"Координаты: UESP.net / Itemaps. Источники рельефа и лицензии — в сведениях о карте.",11,MUTED,false,8);
        actionButton("СВЕДЕНИЯ О КАРТЕ","Покрытие, источники и оформление",false,()->new RuneDialogBuilder(this,accent())
            .setTitle("Атлас Севера").setMessage(SkyrimMapCatalog.size()+" локаций. Координаты материка основаны на игровых данных. "+
            "Рельеф Солстейма привязан к опубликованным ориентирам; отдельные метки имеют приблизительную привязку. "+
            "Подземные миры и отдельные внутренние помещения не являются географической поверхностью этой карты.\n\n"+
            "Координаты: Unofficial Elder Scrolls Pages (UESP.net), опубликованный каталог Itemaps. "+
            "Полный рельеф материка: игровые LOD-текстуры, сборка S1lveric. "+
            "Рельеф Солстейма и его ориентиры: Tamriel Maps (tamrielma.ps). "+
            "The Elder Scrolls V: Skyrim — Bethesda Softworks. Неофициальный атлас.")
            .setNegativeButton("Закрыть",null).show());
    }
    /**
     * One location = one precise map icon + name. Do not reuse actionButton:
     * its generic left icon, diamond at the right and detail caption are
     * deliberately absent from the location list.
     */
    private void mapLocationRow(LinearLayout host,SkyrimMapCatalog.Site site,Runnable onClick){
        LinearLayout tile=row();
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setMinimumHeight(dp(55));
        tile.setPadding(dp(13),dp(9),dp(13),dp(9));
        tile.setBackground(new RuneFrameDrawable(PANEL,BORDER,false));
        String glyph=SkyrimTerrainMapView.iconForSite(site);
        tile.addView(new SkyrimIconView(this,glyph,0xffd5dfe1),lp(32,32));
        TextView name=text(site.name,16,TEXT,true);
        name.setGravity(Gravity.CENTER_VERTICAL);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams=new LinearLayout.LayoutParams(0,-2,1);
        nameParams.leftMargin=dp(13);
        tile.addView(name,nameParams);
        LinearLayout.LayoutParams params=lp(-1,-2);
        params.bottomMargin=dp(7);
        host.addView(tile,params);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription(site.name);
        tile.setOnClickListener(v->onClick.run());
    }
    private void mapControl(LinearLayout host,String label,Runnable command){
        TextView button=text(label,13,TEXT,true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8),dp(9),dp(8),dp(9));
        button.setBackground(new RuneFrameDrawable(PANEL_2,BORDER,false));
        LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,dp(43),1);
        size.setMargins(dp(2),0,dp(2),0);
        host.addView(button,size);
        button.setContentDescription(label);
        button.setOnClickListener(v->command.run());
    }
    /** Separate immersive map viewer; shares the same persistent camera and filters. */
    private void openFullscreenWorldMap(){
        android.app.Dialog popup=new android.app.Dialog(this);
        popup.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        FrameLayout scene=new FrameLayout(this);
        scene.setBackgroundColor(0xff06090a);
        SkyrimTerrainMapView map=new SkyrimTerrainMapView(
            this,game,this::locationDialog,hiddenMapTypes);
        map.setOnCatalog(this::atlasSitesDialog);
        map.setMapRegion(selectedMapRegion);
        scene.addView(map,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout sheets=row();
        sheets.setPadding(dp(5),dp(5),dp(58),dp(5));
        sheets.setBackgroundColor(0xd806090b);
        FrameLayout.LayoutParams sheetsSize=new FrameLayout.LayoutParams(-1,dp(57),Gravity.TOP);
        scene.addView(sheets,sheetsSize);
        mapControl(sheets,"Скайрим",()->{map.setMapRegion(-1);map.setSheet(0);});
        mapControl(sheets,"Солстейм",()->{map.setMapRegion(-1);map.setSheet(1);});
        LinearLayout toolbar=row();
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8),dp(6),dp(8),dp(6));
        toolbar.setBackgroundColor(0xe506090b);
        FrameLayout.LayoutParams toolbarSize=new FrameLayout.LayoutParams(
            -1,dp(57),Gravity.BOTTOM);
        scene.addView(toolbar,toolbarSize);
        mapControl(toolbar,"−",()->map.zoomBy(1f/1.5f));
        mapControl(toolbar,"+",()->map.zoomBy(1.5f));
        mapControl(toolbar,"Вся карта",map::showWholeProvince);
        mapControl(toolbar,"К герою",map::centerOnPlayer);
        mapControl(toolbar,"Поиск",()->searchSkyrimAtlas(map));
        TextView close=text("×",33,TEXT,true);
        close.setGravity(Gravity.CENTER);
        close.setBackgroundColor(0xc506090b);
        FrameLayout.LayoutParams closeSize=new FrameLayout.LayoutParams(
            dp(52),dp(52),Gravity.TOP|Gravity.END);
        closeSize.setMargins(0,dp(14),dp(14),0);
        scene.addView(close,closeSize);
        close.setContentDescription("Закрыть полноэкранную карту");
        close.setOnClickListener(v->popup.dismiss());
        popup.setOnDismissListener(d->{
            render();map.saveCamera();
            if(activeTerrainMap!=null)activeTerrainMap.restoreCamera();
        });
        popup.setContentView(scene);
        popup.show();
        android.view.Window window=popup.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(-1,-1);
        }
    }
    /** A reference grid cell may include several genuine places. */
    private void atlasSitesDialog(java.util.ArrayList<SkyrimMapCatalog.Site> sites){
        if(sites==null||sites.isEmpty())return;
        if(sites.size()==1){atlasSiteDialog(sites.get(0));return;}
        String[] names=new String[sites.size()];
        for(int i=0;i<sites.size();i++){
            SkyrimMapCatalog.Site site=sites.get(i);
            names[i]=site.name+"  |  "+site.type;
        }
        new RuneDialogBuilder(this,accent())
            .setTitle("Локации рядом")
            .setItems(names,(dialog,which)->atlasSiteDialog(sites.get(which)))
            .setNegativeButton("Закрыть",null).show();
    }
    private void atlasSiteDialog(SkyrimMapCatalog.Site site){
        if(site==null)return;
        WorldAtlas.Place place=WorldAtlas.find(SkyrimMapCatalog.travelDestination(site));
        if(place!=null){locationDialog(place);return;}
        new RuneDialogBuilder(this,accent()).setTitle(site.name)
            .setMessage(site.type+" | "+site.grid).setNegativeButton("Закрыть",null).show();
    }
    private void searchSkyrimAtlas(){
        searchSkyrimAtlas(null);
    }
    /** Search results focus the map camera, not merely display a detached card. */
    private void searchSkyrimAtlas(SkyrimTerrainMapView activeMap){
        LinearLayout wrapper=column();
        wrapper.setPadding(dp(14),dp(6),dp(14),dp(7));
        EditText query=new EditText(this);
        query.setSingleLine(true);
        query.setTextColor(TEXT);
        query.setHintTextColor(MUTED);
        query.setTextSize(sp(15));
        query.setHint("Название места, например, Мзулфт");
        wrapper.addView(query,lp(-1,49));
        TextView count=text("",12,MUTED,false);
        wrapper.addView(count,lp(-1,31));
        ScrollView scroller=new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        LinearLayout matches=column();
        scroller.addView(matches);
        wrapper.addView(scroller,lp(-1,420));
        final Runnable[] refresh={null};
        final AlertDialog[] searchWindow={null};
        final int[] limit={60};
        refresh[0]=()->{
            String needle=query.getText().toString().trim();
            SkyrimMapCatalog.Site exact=SkyrimMapCatalog.find(needle);
            int previousY=scroller.getScrollY();
            matches.removeAllViews();
            java.util.ArrayList<SkyrimMapCatalog.Site> found=new java.util.ArrayList<>();
            for(SkyrimMapCatalog.Site site:SkyrimMapCatalog.all()){
                if(hiddenMapTypes.contains(site.type))continue;
                if(!needle.isEmpty()&&site!=exact&&!site.matches(needle))continue;
                found.add(site);
            }
            java.text.Collator order=java.text.Collator.getInstance(new java.util.Locale("ru"));
            found.sort((a,b)->order.compare(a.name,b.name));
            int shown=0;
            for(SkyrimMapCatalog.Site site:found){
                if(shown++>=limit[0])break;
                mapLocationRow(matches,site,()->{
                    if(activeMap!=null)activeMap.centerOn(site);
                    if(searchWindow[0]!=null)searchWindow[0].dismiss();
                    atlasSiteDialog(site);
                });
            }
            count.setText("Найдено: "+found.size()+"  |  показано "+Math.min(limit[0],found.size()));
            if(found.isEmpty())matches.addView(text("Подходящих локаций нет. Проверь название и фильтры.",14,MUTED,false),lp(-1,-2));
            if(found.size()>limit[0]){
                TextView more=text("ПОКАЗАТЬ ЕЩЁ 60",14,accent(),true);
                more.setPadding(dp(7),dp(14),dp(7),dp(14));matches.addView(more,lp(-1,-2));
                more.setOnClickListener(v->{limit[0]+=60;refresh[0].run();});
            }
            scroller.post(()->scroller.scrollTo(0,previousY));
        };
        refresh[0].run();
        query.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                limit[0]=60;scroller.scrollTo(0,0);
                query.removeCallbacks(refresh[0]);query.postDelayed(refresh[0],120);
            }
            public void afterTextChanged(android.text.Editable e){}
        });
        searchWindow[0]=new RuneDialogBuilder(this,accent())
            .setTitle("Поиск по карте Скайрима")
            .setView(wrapper).setNegativeButton("Закрыть",null).show();
    }
    /** Full atlas type filter; reflects both the map markers and location list. */
    private void renderMapTypeFilters(){
        LinearLayout panel=card();
        boolean expanded=prefs.getBoolean("map_types_expanded",false);
        TextView toggle=text((expanded?"⌃":"⌄")+"  ТИПЫ ЛОКАЦИЙ",14,accent(),true);
        toggle.setPadding(dp(7),dp(10),dp(7),dp(10));
        panel.addView(toggle,lp(-1,-2));
        addText(panel,"✓  отображается     ✕  скрыто",12,MUTED,false,7);
        toggle.setOnClickListener(v->{
            prefs.edit().putBoolean("map_types_expanded",!expanded).apply();render();
        });
        if(!expanded)return;
        java.util.LinkedHashSet<String> types=new java.util.LinkedHashSet<>();
        types.add("Столица");types.add("Деревня");
        for(WorldAtlas.Place place:WorldAtlas.all())types.add(place.type);
        for(SkyrimMapCatalog.Site site:SkyrimMapCatalog.all())types.add(site.type);
        LinearLayout commands=row();panel.addView(commands,lp(-1,47));
        TextView all=text("✓  Включить все",13,accent(),true);
        TextView none=text("✕  Скрыть все",13,MUTED,true);
        commands.addView(all,new LinearLayout.LayoutParams(0,-2,1));
        commands.addView(none,new LinearLayout.LayoutParams(0,-2,1));
        all.setOnClickListener(v->{hiddenMapTypes.clear();persistMapFilters();render();});
        none.setOnClickListener(v->{hiddenMapTypes.clear();hiddenMapTypes.addAll(types);persistMapFilters();render();});
        LinearLayout options=column();panel.addView(options,lp(-1,-2));
        LinearLayout current=null;int index=0;
        for(String type:types){
            if(index++%2==0){current=row();options.addView(current,lp(-1,-2));}
            final String category=type;
            boolean enabled=!hiddenMapTypes.contains(type);
            String label=type.equals("Столица")?"Города":type.equals("Деревня")?"Деревни":type;
            TextView button=text((enabled?"✓ ":"✕ ")+label,12,enabled?accent():MUTED,enabled);
            button.setPadding(dp(7),dp(11),dp(5),dp(11));
            button.setBackground(new RuneFrameDrawable(PANEL_2,enabled?BORDER:0xff3a3e40,false));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
            p.setMargins(dp(2),dp(2),dp(2),dp(2));
            current.addView(button,p);
            button.setOnClickListener(v->{
                if(!hiddenMapTypes.add(category))hiddenMapTypes.remove(category);
                persistMapFilters();render();
            });
        }
        if(types.size()%2==1)current.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
    }
    private void persistMapFilters(){
        prefs.edit().putStringSet("map_hidden_types",new java.util.HashSet<>(hiddenMapTypes)).apply();
    }

    private void locationDialog(WorldAtlas.Place place){
        TravelPlanner.Route route=TravelPlanner.plan(game,place);
        String body=place.description+"\n\nТип: "+place.type+
            "\nРегион: "+GameEngine.REGIONS[place.region]+"\n\n"+route.summary();
        SkyrimMapCatalog.Site geographic=SkyrimMapCatalog.find(place.name);
        if(geographic!=null&&!geographic.registered)body+="\n\nПоложение метки на рельефе ориентировочное.";
        AlertDialog.Builder dialog=new RuneDialogBuilder(this,accent()).setTitle(place.name)
            .setNegativeButton("Закрыть",null);
        if(LocationGalleryView.hasGallery(place.name)){
            android.widget.ScrollView scroller=new android.widget.ScrollView(this);
            scroller.setFillViewport(false);
            LinearLayout wrapper=column();
            wrapper.setPadding(dp(9),dp(9),dp(9),dp(12));
            scroller.addView(wrapper);
            wrapper.addView(new LocationGalleryView(this,place.name,accent(),false),lp(-1,-2));
            if(place.name.equalsIgnoreCase(game.progress.location))renderLocationWeather(wrapper);
            TextView description=text(body,14,TEXT,false);
            description.setLineSpacing(dp(4),1.05f);
            description.setPadding(dp(8),dp(13),dp(8),dp(5));
            wrapper.addView(description,lp(-1,-2));
            routePreview(wrapper,route);
            dialog.setView(scroller);
        }else{
            dialog.setMessage(body);
        }
        if(route.allowed&&route.days>0){
            dialog.setPositiveButton("Начать переход",(d,w)->{
                java.util.Map<String,Integer> before=itemSnapshot(game);VisualSnapshot visualBefore=new VisualSnapshot(game);game.choose("LOC:"+place.name);game.progress.sync(game);captureItemGains(before,game,false);recordVisual(visualBefore,game,place.name);save();tab=0;render();
            });
        }
        dialog.show();
    }

    // Shared item presentation: combat values and equipped slot come from the engine.
    private String itemNumber(double n){
        return String.format(java.util.Locale.forLanguageTag("ru"),n==Math.rint(n)?"%.0f":"%.1f",n);
    }
    private boolean hasItemStat(ItemCatalog.Item item){
        return item.baseStat>0&&(item.weapon()||item.wearable());
    }
    private String itemType(ItemCatalog.Item item){
        if(item.weapon()&&!"Боеприпасы".equals(item.category)){
            if(item.statSkill==0)return "Одноручное оружие";
            if(item.statSkill==1)return "Двуручное оружие";
            if(item.statSkill==2)return "Оружие дальнего боя";
        }
        if(item.wearable()&&item.statSkill==4)return "Тяжёлая броня";
        if(item.wearable()&&item.statSkill==5)return "Лёгкая броня";
        return item.category.isEmpty()?"Валюта":item.category;
    }
    private double itemDelta(ItemCatalog.Item item){
        if(!hasItemStat(item)||item.slot().isEmpty()||game.progress.equipped(item.id))return Double.NaN;
        ItemCatalog.Item worn=ItemCatalog.find(game.progress.equippedName(item.slot()));
        if(worn==null||worn.baseStat<0)return Double.NaN;
        return Math.round(item.currentStat(game.progress)*10)/10.0-
            Math.round(worn.currentStat(game.progress)*10)/10.0;
    }
    private LinearLayout itemMetric(String label,String glyph,String value,int color,double delta,boolean large){
        LinearLayout metric=column();metric.setGravity(Gravity.CENTER);
        TextView caption=text(label,10,MUTED,true);caption.setGravity(Gravity.CENTER);
        metric.addView(caption,lp(-1,-2));
        LinearLayout line=row();line.setGravity(Gravity.CENTER);
        line.addView(new SkyrimIconView(this,glyph,color),lp(large?23:17,large?23:17));
        TextView number=text(value,large?23:17,color,true);
        LinearLayout.LayoutParams n=lp(-2,-2);n.leftMargin=dp(3);line.addView(number,n);
        metric.addView(line,lp(-1,-2));
        if(!Double.isNaN(delta)&&Math.abs(delta)>=0.05){
            TextView change=text((delta>0?"▲ +":"▼ −")+itemNumber(Math.abs(delta)),11,delta>0?ITEM_UP:ITEM_DOWN,true);
            change.setGravity(Gravity.CENTER);metric.addView(change,lp(-1,-2));
            metric.setContentDescription(label+" "+value+", изменение "+(delta>0?"+":"−")+itemNumber(Math.abs(delta)));
        }else metric.setContentDescription(label+" "+value);
        return metric;
    }
    private void itemSmallMetric(LinearLayout host,String glyph,String value,int color){
        LinearLayout metric=row();
        metric.addView(new SkyrimIconView(this,glyph,color),lp(15,15));
        TextView v=text(value,12,color,false);
        LinearLayout.LayoutParams p=lp(-2,-2);p.leftMargin=dp(3);metric.addView(v,p);
        LinearLayout.LayoutParams m=lp(-2,-2);m.rightMargin=dp(8);host.addView(metric,m);
    }
    private LinearLayout itemSummary(ItemCatalog.Item item,int quantity,boolean active,boolean showWeight,int price){
        LinearLayout itemRow=row();itemRow.setMinimumHeight(dp(62));
        itemRow.setPadding(dp(9),dp(9),dp(9),dp(9));
        itemRow.setBackground(new RuneFrameDrawable(active?0xff17191a:0xff080808,active?accent():0xff44494b,false));
        itemRow.addView(new SkyrimIconView(this,SkyrimIconView.iconForItem(item.name,item.category),active?accent():MUTED),lp(39,43));
        LinearLayout words=column();LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);
        wp.leftMargin=dp(9);itemRow.addView(words,wp);
        TextView name=text((game.progress.equipped(item.id)?"◆ ":"")+item.name+(quantity>1?" ×"+quantity:""),14,TEXT,true);
        name.setMaxLines(3);words.addView(name,lp(-1,-2));
        addText(words,itemType(item),11,MUTED,false,3);
        LinearLayout small=row();words.addView(small,lp(-1,-2));
        if(showWeight)itemSmallMetric(small,"bag",itemNumber(item.weight)+" кг",MUTED);
        itemSmallMetric(small,"septims",String.valueOf(price),ITEM_GOLD);
        if(hasItemStat(item)){
            boolean weapon=item.weapon();
            itemRow.addView(itemMetric(weapon?"УРОН":"БРОНЯ",weapon?"weapon":"shield",
                itemNumber(item.currentStat(game.progress)),weapon?ITEM_DAMAGE:ITEM_ARMOR,itemDelta(item),false),lp(79,-2));
        }
        return itemRow;
    }
    private void itemDetailMetrics(LinearLayout host,ItemCatalog.Item item){
        LinearLayout metrics=row();metrics.setGravity(Gravity.CENTER);
        if(hasItemStat(item)){
            boolean weapon=item.weapon();
            metrics.addView(itemMetric(weapon?"УРОН":"БРОНЯ",weapon?"weapon":"shield",
                itemNumber(item.currentStat(game.progress)),weapon?ITEM_DAMAGE:ITEM_ARMOR,itemDelta(item),true),new LinearLayout.LayoutParams(0,-2,1));
        }
        metrics.addView(itemMetric("ВЕС","bag",itemNumber(item.weight),MUTED,Double.NaN,true),new LinearLayout.LayoutParams(0,-2,1));
        metrics.addView(itemMetric("ЦЕНА","septims",String.valueOf(item.value),ITEM_GOLD,Double.NaN,true),new LinearLayout.LayoutParams(0,-2,1));
        host.addView(metrics,lp(-1,-2));space(host,9);
        if(hasItemStat(item)){
            addText(host,"Базовое значение: "+itemNumber(item.baseStat),12,MUTED,false,5);
            if(!Double.isNaN(itemDelta(item)))addText(host,"Сравнение: "+game.progress.equippedName(item.slot()),11,MUTED,false,5);
            if(item.twoHanded()&&!"Не надето".equals(game.progress.equippedName("shield")))
                addText(host,"Двуручное оружие снимает экипированный щит.",12,ITEM_DOWN,false,5);
        }
    }
    private void itemEffect(LinearLayout host,ItemCatalog.Item item){
        if(item.effect==null||item.effect.trim().isEmpty())return;
        String e=item.effect.toLowerCase(java.util.Locale.ROOT);
        boolean magic=e.contains("зачар")||e.contains("поглощ")||e.contains("урон огн")||e.contains("урон холод")||e.contains("урон мороз");
        LinearLayout line=row();line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(new SkyrimIconView(this,e.contains("огн")?"огонь":e.contains("мороз")?"мороз":magic?"зачарование":"book",magic?ITEM_MAGIC:MUTED),lp(22,22));
        TextView value=text(item.effect,13,magic?ITEM_MAGIC:MUTED,false);
        LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(0,-2,1);vp.leftMargin=dp(8);
        line.addView(value,vp);host.addView(line,lp(-1,-2));space(host,5);
    }
    private java.util.Map<String,Integer> itemSnapshot(GameEngine state){
        java.util.Map<String,Integer> result=new java.util.HashMap<>();
        java.util.Iterator<String> keys=state.progress.inventory.keys();
        while(keys.hasNext()){String id=keys.next();result.put(id,state.progress.quantity(id));}
        return result;
    }
    private void captureItemGains(java.util.Map<String,Integer> before,GameEngine state,boolean keepIfEmpty){
        java.util.Map<String,Integer> after=itemSnapshot(state);
        java.util.LinkedHashMap<String,Integer> gains=new java.util.LinkedHashMap<>();
        for(String id:after.keySet()){
            int delta=after.get(id)-before.getOrDefault(id,0);
            if(delta>0&&!"Септимы".equals(id)&&ItemCatalog.find(id)!=null)gains.put(id,delta);
        }
        if(!keepIfEmpty||!gains.isEmpty()){recentItemGains.clear();recentItemGains.putAll(gains);}
    }
    private void renderItemGains(){
        if(recentItemGains.isEmpty()||!visual("loot"))return;
        LinearLayout rewards=card();
        addText(rewards,"ПОЛУЧЕНЫ ПРЕДМЕТЫ",11,ITEM_GOLD,true,8);
        for(java.util.Map.Entry<String,Integer> gain:recentItemGains.entrySet()){
            ItemCatalog.Item item=ItemCatalog.find(gain.getKey());if(item==null)continue;
            LinearLayout entry=itemSummary(item,gain.getValue(),false,true,item.value);
            rewards.addView(entry,lp(-1,-2));entry.setOnClickListener(v->itemDialog(item));
            if(animations){entry.setAlpha(.25f);entry.animate().alpha(1).setDuration(260).start();}
            itemEffect(rewards,item);
        }
        addText(rewards,"Добавлено в инвентарь",12,ITEM_UP,false,0);
    }
    private void renderShopItem(String action){
        String id;int price;
        switch(action){
            case "BUY_SWORD":if(game.swordUpgraded)return;id="Стальной меч";price=180;break;
            case "BUY_ARMOR":if(game.armorUpgraded)return;id="Стальная кираса";price=160;break;
            case "BUY_HP":id="Лечебное зелье";price=35;break;
            case "BUY_MP":id="Зелье магии";price=30;break;
            default:return;
        }
        ItemCatalog.Item item=ItemCatalog.find(id);if(item==null)return;
        LinearLayout entry=itemSummary(item,0,false,true,price);
        content.addView(entry,lp(-1,-2));entry.setOnClickListener(v->itemDialog(item));
    }
    private boolean canDiscardItem(ItemCatalog.Item item){
        return !"COMBAT".equals(game.mode)&&game.progress.quantity(item.id)>0
            &&!"Квестовые предметы".equals(item.category)&&!"Ключи".equals(item.category)
            &&!"Септимы".equals(item.id)&&!"Лечебное зелье".equals(item.id)&&!"Зелье магии".equals(item.id)
            &&!"Походный клинок".equals(item.id)&&!"Дорожная одежда".equals(item.id)
            &&!(game.swordUpgraded&&"Стальной меч".equals(item.id))
            &&!(game.armorUpgraded&&"Стальная кираса".equals(item.id));
    }

    private void renderInventory(){
        game.progress.sync(game);
        section("СУМКА ПУТНИКА","Инвентарь","Предметы и снаряжение");
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
            String image=SkyrimIconView.iconForCategory(group);
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
            selected.addView(itemSummary(focused,game.progress.quantity(focused.id),true,false,focused.value),lp(-1,-2));
            selected.addView(new SkyrimRuleView(this,accent()),lp(-1,17));
            itemDetailMetrics(selected,focused);
            itemEffect(selected,focused);
            addText(selected,focused.description,14,TEXT,false,9);
            TextView detail=text("◇  ПОДРОБНЕЕ / ИСПОЛЬЗОВАТЬ",12,accent(),true);
            detail.setGravity(Gravity.END);selected.addView(detail,lp(-1,30));
            detail.setOnClickListener(v->itemDialog(focused));
        }
        if(owned.isEmpty())addText(content,"В выбранной категории нет предметов.",15,MUTED,false,14);
        for(ItemCatalog.Item item:owned){
            final boolean active=item.id.equals(focusedInventoryId);
            LinearLayout itemRow=itemSummary(item,game.progress.quantity(item.id),active,true,item.value);
            LinearLayout.LayoutParams entry=lp(-1,-2);entry.bottomMargin=dp(4);
            content.addView(itemRow,entry);
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
        LinearLayout view=column();
        view.setPadding(dp(14),dp(10),dp(14),dp(8));
        LinearLayout heading=row();
        view.addView(heading,lp(-1,-2));
        // Use the actual atlas sprite rather than Item.icon's old Unicode glyph.
        heading.addView(new SkyrimIconView(this,
            SkyrimIconView.iconForItem(item.name,item.category),accent()),lp(44,44));
        LinearLayout words=column();
        LinearLayout.LayoutParams wordsParams=new LinearLayout.LayoutParams(0,-2,1);
        wordsParams.leftMargin=dp(12);
        heading.addView(words,wordsParams);
        addText(words,item.name,17,TEXT,true,4);
        addText(words,item.category,12,MUTED,false,0);
        view.addView(new SkyrimRuleView(this,accent()),lp(-1,22));
        itemDetailMetrics(view,item);
        view.addView(new SkyrimRuleView(this,accent()),lp(-1,14));
        addText(view,item.description,14,TEXT,false,12);
        itemEffect(view,item);
        if(game.progress.quantity(item.id)>0&&!item.temperMaterial().isEmpty()){
            addText(view,"КУЗНЕЧНОЕ УЛУЧШЕНИЕ",11,accent(),true,5);
            addText(view,"Качество: "+game.progress.temperQualityName(item.id)+
                (game.progress.quantity(item.id)>1&&game.progress.temperQuality(item.id)>0?" (1 экземпляр)":"")+
                "\nМатериал: "+item.temperMaterial()+" | "+game.progress.quantity(item.temperMaterial())+
                "\nДоступное качество: "+game.progress.smithingQualityName(),13,TEXT,false,5);
            String blocked=game.progress.temperBlock(item,game);
            if(!blocked.isEmpty())addText(view,blocked,12,MUTED,false,4);
        }
        AlertDialog.Builder dialog=new RuneDialogBuilder(this,accent()).setTitle("Сведения о предмете")
            .setView(view).setNegativeButton("Закрыть",null);
        if(game.progress.quantity(item.id)>0){
            if(!item.temperMaterial().isEmpty()&&game.progress.temperBlock(item,game).isEmpty())
                dialog.setNeutralButton("Улучшить",(d,w)->{
                    new RuneDialogBuilder(this,accent()).setTitle("Улучшить "+item.name+"?")
                        .setMessage("Расход: 1 × "+item.temperMaterial()+"\nКачество: "+game.progress.smithingQualityName())
                        .setNegativeButton("Отмена",null).setPositiveButton("Улучшить",(a,b)->{
                            if(game.progress.temper(item,game)){save();render();toast("Предмет улучшен");}
                            else toast(game.progress.temperBlock(item,game));
                        }).show();
                });
            if(!item.slot().isEmpty()){
                boolean worn=game.progress.equipped(item.id);
                addText(view,worn?"ЭКИПИРОВАНО":"Не экипировано",12,accent(),true,6);
                if(!"COMBAT".equals(game.mode))dialog.setPositiveButton(worn?"Снять":"Экипировать",(d,w)->{
                    if(worn){game.progress.unequip(item.id);save();render();}
                    else if(game.progress.equip(item.id)){save();render();}
                    else toast("Сначала сними двуручное оружие");
                });
            }else if("Лечебное зелье".equals(item.id)||"Зелье магии".equals(item.id))
                dialog.setPositiveButton("Использовать",(d,w)->{
                    boolean result="Лечебное зелье".equals(item.id)?game.drinkHealing():game.drinkMana();
                    game.progress.sync(game);save();render();
                    toast(result?"Зелье использовано":"Нет необходимости использовать зелье");
                });
        }
        if(canDiscardItem(item)){
            TextView discard=text("ВЫБРОСИТЬ  ×1",13,ITEM_DOWN,true);
            discard.setGravity(Gravity.CENTER);view.addView(discard,lp(-1,43));
            discard.setOnClickListener(v->new RuneDialogBuilder(this,accent()).setTitle("Выбросить предмет?")
                .setMessage(item.name+" ×1").setNegativeButton("Отмена",null)
                .setPositiveButton("Выбросить",(d,w)->{
                    if(canDiscardItem(item)&&game.progress.removeItem(item.id,1)){
                        save();render();toast("Предмет выброшен");
                        discard.setEnabled(false);discard.setText("ПРЕДМЕТ ВЫБРОШЕН");
                    }
                }).show());
        }
        ScrollView itemScroll=new ScrollView(this);itemScroll.setFillViewport(false);
        itemScroll.addView(view);
        dialog.setView(itemScroll).show();
    }

    /** Compact expandable catalog, with dedicated atlas icons and no list weights. */
    private void showItemCatalog(){
        java.util.ArrayList<ItemCatalog.Item> allItems=ItemCatalog.all();
        allItems.sort((a,b)->a.name.compareToIgnoreCase(b.name));
        LinearLayout panel=column();
        panel.setPadding(dp(12),dp(5),dp(12),dp(8));
        EditText search=new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(TEXT);search.setHintTextColor(MUTED);
        search.setTextSize(sp(14));
        search.setHint("Поиск предметов");
        panel.addView(search,lp(-1,48));
        TextView resultCount=text("",12,MUTED,false);
        panel.addView(resultCount,lp(-1,28));
        ScrollView scrollCatalog=new ScrollView(this);
        scrollCatalog.setVerticalScrollBarEnabled(false);
        scrollCatalog.setFillViewport(false);
        LinearLayout sections=column();
        scrollCatalog.addView(sections);
        int maxHeight=Math.min(dp(530),
            (int)(getResources().getDisplayMetrics().heightPixels*.58f));
        panel.addView(scrollCatalog,new LinearLayout.LayoutParams(-1,maxHeight));
        java.util.HashSet<String> opened=new java.util.HashSet<>();
        java.util.HashSet<String> manuallyClosed=new java.util.HashSet<>();
        final Runnable[] refresh=new Runnable[1];
        refresh[0]=()->{
            String query=search.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
            boolean searching=!query.isEmpty();
            java.util.LinkedHashMap<String,java.util.ArrayList<ItemCatalog.Item>> groups=
                new java.util.LinkedHashMap<>();
            int matches=0;
            for(ItemCatalog.Item item:allItems){
                if(searching&&!item.name.toLowerCase(java.util.Locale.ROOT).contains(query)
                    &&!item.category.toLowerCase(java.util.Locale.ROOT).contains(query))continue;
                java.util.ArrayList<ItemCatalog.Item> group=groups.get(item.category);
                if(group==null){group=new java.util.ArrayList<>();groups.put(item.category,group);}
                group.add(item);matches++;
            }
            resultCount.setText("НАЙДЕНО: "+matches+" / "+allItems.size()+
                "     КАТЕГОРИЙ: "+groups.size());
            sections.removeAllViews();
            if(matches==0){
                TextView empty=text("Совпадений нет",14,MUTED,false);
                empty.setGravity(Gravity.CENTER);
                sections.addView(empty,lp(-1,90));
                return;
            }
            for(java.util.Map.Entry<String,java.util.ArrayList<ItemCatalog.Item>> entry:groups.entrySet()){
                String category=entry.getKey();
                java.util.ArrayList<ItemCatalog.Item> categoryItems=entry.getValue();
                boolean expanded=opened.contains(category)||(searching&&!manuallyClosed.contains(category));
                LinearLayout header=row();
                header.setMinimumHeight(dp(49));
                header.setPadding(dp(8),dp(7),dp(10),dp(7));
                header.setBackground(new RuneFrameDrawable(PANEL,BORDER,false));
                header.addView(new SkyrimIconView(this,
                    SkyrimIconView.iconForCategory(category),accent()),lp(28,29));
                TextView title=text(category.toUpperCase(java.util.Locale.ROOT)+
                    "  ("+categoryItems.size()+")",13,TEXT,true);
                title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams nameParams=new LinearLayout.LayoutParams(0,-2,1);
                nameParams.leftMargin=dp(10);header.addView(title,nameParams);
                TextView arrow=text(expanded?"⌃":"⌄",21,accent(),true);
                arrow.setGravity(Gravity.CENTER);header.addView(arrow,lp(28,28));
                LinearLayout.LayoutParams hp=lp(-1,-2);
                hp.bottomMargin=dp(3);
                sections.addView(header,hp);
                LinearLayout entries=column();
                entries.setVisibility(expanded?View.VISIBLE:View.GONE);
                sections.addView(entries,lp(-1,-2));
                header.setContentDescription(category+", "+(expanded?"развёрнуто":"свёрнуто"));
                header.setOnClickListener(v->{
                    boolean next=entries.getVisibility()!=View.VISIBLE;
                    if(animations)android.transition.TransitionManager.beginDelayedTransition(
                        sections,new android.transition.AutoTransition().setDuration(200));
                    entries.setVisibility(next?View.VISIBLE:View.GONE);
                    arrow.setText(next?"⌃":"⌄");
                    if(next){opened.add(category);manuallyClosed.remove(category);}
                    else{opened.remove(category);manuallyClosed.add(category);}
                    header.setContentDescription(category+", "+(next?"развёрнуто":"свёрнуто"));
                });
                // Build rows for collapsed categories too: the first tap must
                // reveal items immediately, without resetting search or scroll.
                for(ItemCatalog.Item item:categoryItems){
                    LinearLayout itemRow=itemSummary(item,0,false,false,item.value);
                    entries.addView(itemRow,lp(-1,-2));
                    View sep=new View(this);sep.setBackgroundColor(0xff252b2e);
                    entries.addView(sep,lp(-1,1));
                    itemRow.setOnClickListener(v->itemDialog(item));
                }
                space(sections,5);
            }
        };
        // Start compact; typing a query reveals matching categories automatically.
        refresh[0].run();
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence x,int start,int count,int after){}
            public void onTextChanged(CharSequence x,int a,int b,int c){refresh[0].run();}
            public void afterTextChanged(android.text.Editable x){}
        });
        new RuneDialogBuilder(this,accent()).setTitle("Каталог предметов")
            .setView(panel).setNegativeButton("Закрыть",null).show();
    }
    private void showLibrary(){
        java.util.ArrayList<String> docs=ItemCatalog.loreDocuments(this);
        if(docs.isEmpty()){toast("Библиотека не загружена");return;}
        new RuneDialogBuilder(this,accent()).setTitle("Личная библиотека | "+docs.size()+" документов")
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
            clock.setText("ДЕНЬ "+(game.day+total/24)+"   |   "+
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
        section("ЛИЧНОЕ ДЕЛО",""+game.name,game.race+" | "+game.heroClass+" | уровень "+game.level);
        CharacterPortraitView portrait=new CharacterPortraitView(this,game.race,game.gender,game.portrait);
        LinearLayout.LayoutParams portraitParams=lp(150,-2);portraitParams.gravity=Gravity.CENTER_HORIZONTAL;
        content.addView(portrait,portraitParams);space(content,12);
        String portraitPath=CharacterIdentity.portraitPath(game.race,game.gender,game.portrait);
        if(!ResourceStore.has(this,portraitPath))
            actionButton("Загрузить портрет героя","Изображения хранятся отдельно от приложения",false,
                ()->new ResourceManagerDialog(this,accent(),this::render,ResourceStore.portraitGroup(portraitPath)).show());
        if(!prefs.getBoolean("ai_enabled",false))
            actionButton("Ожидать","Выбрать от 1 до 24 часов отдыха",false,this::showWaitDialog);
        reputationPanel();
        LinearLayout c=card();addText(c,"ХАРАКТЕРИСТИКИ",11,accent(),true,9);
        heroEntry(c,"Пол",game.gender);
        heroEntry(c,"Опыт",game.xp+" / "+(game.level*80));
        heroEntry(c,"Атака",String.valueOf(game.effectiveAttack()));heroEntry(c,"Защита",String.valueOf(game.effectiveArmor()));
        heroEntry(c,"Септимы",game.gold+" септимов");heroEntry(c,"Репутация",String.valueOf(game.reputation));
        heroEntry(c,"Победы",String.valueOf(game.victories));heroEntry(c,"Исследований",String.valueOf(game.explored));
        LinearLayout items=card();addText(items,"СНАРЯЖЕНИЕ",11,accent(),true,12);
        gearVisual(items);
        addText(items,"Оружие: "+game.progress.equippedName("weapon"),15,TEXT,false,10);
        String[] gearSlots={"body","head","hands","feet","shield","ring","neck","ammo"};
        String[] gearLabels={"Доспех","Голова","Руки","Ноги","Щит","Кольцо","Амулет","Боеприпасы"};
        for(int i=0;i<gearSlots.length;i++)addText(items,gearLabels[i]+": "+game.progress.equippedName(gearSlots[i]),15,TEXT,false,6);
        addText(items,"Снижение физического урона: "+String.format(java.util.Locale.ROOT,"%.1f%%",100*game.progress.damageReduction()),12,accent(),true,8);
        addText(items,"Лечебные зелья: "+game.potions,14,MUTED,false,6);
        addText(items,"Зелья магии: "+game.manaPotions,14,MUTED,false,0);
        actionButton("Выпить лечебное зелье","+45 здоровья | без боя",false,()->{
            toast(game.drinkHealing()?"Здоровье восстановлено":"Зелье не требуется или закончилось");save();render();});
        actionButton("Выпить зелье магии","+35 магии | без боя",false,()->{
            toast(game.drinkMana()?"Магия восстановлена":"Зелье не требуется или закончилось");game.progress.sync(game);save();render();});
        LinearLayout race=card();addText(race,"НАСЛЕДИЕ РАСЫ",11,accent(),true,12);
        addText(race,game.race+" | пассивно",16,TEXT,true,7);
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
        starSkills();
        for(int i=0;i<RpgContent.SKILLS.length;i++){
            final int index=i;int lvl=game.progress.skillLevel(i);
            LinearLayout skill=row();skill.setPadding(dp(12),dp(10),dp(12),dp(10));
            skill.setBackground(new RuneFrameDrawable(PANEL,BORDER,false));
            skill.addView(new SkyrimIconView(this,RpgContent.SKILLS[i],accent()),lp(34,37));
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
        boolean combat=RpgProgress.isCombatPerk(index);
        int rank=game.progress.combatPerkRank(index);
        if(combat)details+="\n\n"+game.progress.combatPerkName(index)+": "+rank+" / 5"
            +"\nБонус: +"+(rank*20)+"%"+(rank<5?"\nСледующий ранг: навык "+game.progress.nextCombatPerkLevel(index):"");
        AlertDialog.Builder builder=new RuneDialogBuilder(this,accent()).setTitle(RpgContent.SKILLS[index])
            .setMessage(details).setNegativeButton("Закрыть",null);
        if(combat){
            if(game.progress.perkPoints>0&&rank<5&&lvl>=game.progress.nextCombatPerkLevel(index))
                builder.setPositiveButton("Повысить ранг",(d,w)->{
                    if(game.progress.buyCombatPerk(index)){save();render();toast("Талант развит");}
                });
        }else if(game.progress.perkPoints>0&&lvl>=20)
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
        renderChronicle();
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
            addText(completed,"◇ Драконий след | первое испытание",14,TEXT,false,7);
        if(game.progress.dragonStage>=DragonArc.FINAL_STAGE){
            String[] investigations={"Письмо под пеплом","Совет за закрытыми дверями","След пепельного гонца"};
            for(int i=0;i<Math.min(3,game.progress.dragonNextStage);i++)
                addText(completed,"◇ "+investigations[i],14,TEXT,false,6);
        }
        if(game.questStage==4)addText(completed,"◇ Пепельные печати",14,TEXT,false,6);
        if(game.boardCompleted>0)addText(completed,"◇ Опасные дороги | заказов: "+game.boardCompleted,14,TEXT,false,6);
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
    private void foldSettingsSection(String key,String name,int from){
        LinearLayout body=column();
        while(content.getChildCount()>from){
            View child=content.getChildAt(from);
            ViewGroup.LayoutParams params=child.getLayoutParams();
            content.removeViewAt(from);
            body.addView(child,params);
        }
        LinearLayout panel=column();
        panel.setBackground(new RuneFrameDrawable(PANEL,BORDER,false));
        LinearLayout heading=row();
        heading.setPadding(dp(12),dp(12),dp(12),dp(12));
        boolean open=prefs.getBoolean("settings_group_"+key,"ai".equals(key));
        TextView title=text(name,15,accent(),true);
        heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=text(open?"⌃":"⌄",23,accent(),true);
        heading.addView(arrow,lp(30,30));
        panel.addView(heading,lp(-1,-2));
        body.setPadding(dp(6),dp(4),dp(6),dp(10));
        body.setVisibility(open?View.VISIBLE:View.GONE);
        panel.addView(body,lp(-1,-2));
        heading.setClickable(true);
        heading.setContentDescription(name+", "+(open?"раскрыто":"свёрнуто"));
        heading.setOnClickListener(v->{
            boolean expanded=body.getVisibility()!=View.VISIBLE;
            if(animations)android.transition.TransitionManager.beginDelayedTransition(
                panel,new android.transition.AutoTransition().setDuration(230));
            body.setVisibility(expanded?View.VISIBLE:View.GONE);
            arrow.setText(expanded?"⌃":"⌄");
            heading.setContentDescription(name+", "+(expanded?"раскрыто":"свёрнуто"));
            prefs.edit().putBoolean("settings_group_"+key,expanded).apply();
        });
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(9);
        content.addView(panel,from,p);
    }

    private void renderSettings(){
        skyrimTopTabs(5);
        section("ПАРАМЕТРЫ","Настройки","Твоя история, твои правила.");
        content.addView(new NordicArtView(this,NordicArtView.STORY),lp(-1,105));space(content,12);
        int appearanceStart=content.getChildCount();
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
        Switch numberToggle=new Switch(this);
        numberToggle.setText("Показывать номера вариантов ответа");
        numberToggle.setTextColor(TEXT);
        numberToggle.setTextSize(sp(14));
        numberToggle.setThumbTintList(ColorStateList.valueOf(accent()));
        numberToggle.setChecked(showChoiceNumbers());
        numberToggle.setOnCheckedChangeListener((v,on)->{
            prefs.edit().putBoolean("show_choice_numbers",on).apply();
            render();
        });
        readability.addView(numberToggle,lp(-1,47));
        LinearLayout nameColors=card();
        addText(nameColors,"ЦВЕТ ИМЕНИ ГЕРОЯ В ДИАЛОГАХ",11,accent(),true,10);
        int currentHeroColor=Math.max(0,Math.min(HERO_NAME_COLORS.length-1,
            prefs.getInt("hero_name_color",0)));
        for(int i=0;i<HERO_NAME_COLORS.length;i++){
            final int selectedColor=i;
            LinearLayout line=row();
            line.setMinimumHeight(dp(42));
            TextView marker=text(currentHeroColor==i?"●":"○",23,HERO_NAME_COLORS[i],true);
            marker.setGravity(Gravity.CENTER);
            line.addView(marker,lp(38,38));
            line.addView(text(HERO_NAME_COLOR_NAMES[i],15,TEXT,currentHeroColor==i));
            line.setOnClickListener(v->{
                prefs.edit().putInt("hero_name_color",selectedColor).apply();
                render();
            });
            nameColors.addView(line,lp(-1,44));
        }
        foldSettingsSection("appearance","ОФОРМЛЕНИЕ И ЧИТАЕМОСТЬ",appearanceStart);
        visualSettings();
        int aiStart=content.getChildCount();
        space(content,6);addText(content,"ИИ-ПРИКЛЮЧЕНИЕ",11,accent(),true,11);
        LinearLayout aiCard=card();
        addText(aiCard,"ИСТОЧНИК ИИ",11,accent(),true,8);
        Switch aiToggle=new Switch(this);
        aiToggle.setText("ИИ управляет всей игрой, сценами и вариантами");
        aiToggle.setTextSize(sp(14));aiToggle.setTextColor(TEXT);
        aiToggle.setThumbTintList(ColorStateList.valueOf(accent()));
        aiToggle.setChecked(prefs.getBoolean("ai_enabled",false));
        aiToggle.setOnCheckedChangeListener((v,on)->{
            prefs.edit().putBoolean("ai_enabled",on).apply();
            if(!on){
                if(aiClient!=null)aiClient.cancel();
                if(localAiClient!=null)localAiClient.cancel();
                aiTurnBusy=false;awaitingAiToken=false;
            }
        });
        aiCard.addView(aiToggle,lp(-1,48));
        String provider=aiProviderMode();
        actionButton("Источник ИИ",
            "chatgpt".equals(provider)?"ChatGPT | Работа/Codex":
            "api".equals(provider)?"OpenAI API | отдельная оплата":
            "local".equals(provider)?"Локальная GGUF | офлайн":
                "Автоматически | Работа/Codex → API → GGUF",
            true,this::aiProviderDialog);
        // AUTO shows only the effectively selected engine, never every engine.
        String effectiveProvider=resolvedAiProvider();
        if("auto".equals(provider)){
            String chosen="chatgpt".equals(effectiveProvider)?"ChatGPT | Работа/Codex":
                "api".equals(effectiveProvider)?"OpenAI API":
                "local".equals(effectiveProvider)?"Локальная GGUF":"не найден";
            addText(content,"Автоматический выбор: "+chosen+
                ". Приоритет: ChatGPT → API → GGUF. Платный API "+
                "не запускается скрыто после исчерпания лимитов ChatGPT.",
                12,MUTED,false,10);
        }
        if("local".equals(effectiveProvider)){
            actionButton("Локальная модель | GGUF",
                localModelReady()?"Готово | "+new java.io.File(LocalAiModelStore.path(prefs)).getName()+
                    " | без интернета":"Не выбрана | импорт из Download",
                true,this::selectLocalModel);
            actionButton("ПРОВЕРИТЬ ЛОКАЛЬНУЮ МОДЕЛЬ",
                localModelReady()?"Тест запуска и скорости | сохранение не меняется":
                    "Сначала импортируй GGUF",false,this::testLocalModel);
            float lastTps=prefs.getFloat("ai_local_last_tps",0f);
            if(lastTps>0f)addText(content,"Последний замер: "+
                String.format(java.util.Locale.ROOT,"%.2f ток/с",lastTps)+
                " (скорость генерации).",12,MUTED,false,7);
            addText(content,"GGUF хранится на устройстве. Импорт модели требует "+
                "свободного места. Локальная CPU-генерация может занимать "+
                "несколько минут; стадия и время отображаются в игровом режиме.",
                12,MUTED,false,10);
        }else if("chatgpt".equals(effectiveProvider)){
            boolean signedIn=chatGptOAuth.hasSignedInAccount();
            boolean authorized=chatGptOAuth.hasAuthorizedPlan();
            // A single account control is rebuilt with the current session state.
            String accountTitle=signedIn?"Выйти из ChatGPT":"Войти в ChatGPT";
            String accountDetail=accountActionBusy?"Выполняется "+(signedIn?"выход…":"вход…"):
                signedIn?chatGptOAuth.accountLabel()+" | завершить сессию":
                "Авторизация в системном браузере | лимиты Работа/Codex";
            Runnable accountAction=signedIn?this::logoutChatGpt:this::loginChatGpt;
            TextView accountMark=actionButton(accountTitle,accountDetail,false,accountAction);
            View accountRow=(View)accountMark.getParent();
            accountRow.setEnabled(!accountActionBusy);
            accountRow.setClickable(!accountActionBusy);
            if(signedIn){
                if(authorized&&!accountActionBusy){
                    actionButton("Модель | Работа/Codex",
                        prefs.getString("ai_chatgpt_model","Не выбрана")+
                        " | выбрана внутри игры",false,this::chatGptModelDialog);
                }else if(!authorized){
                    addText(content,"Вход выполнен, но разрешение использовать лимиты ChatGPT не предоставлено. "
                        +"Для повторной авторизации сначала выйди из аккаунта.",12,MUTED,false,10);
                }
            }
            actionButton("Уровень размышления",
                AiReasoningEffort.label(selectedReasoningEffort())+
                " | настройка игры",false,this::aiReasoningEffortDialog);
            addText(content,"ChatGPT использует разрешённые лимиты Работа/Codex, "+
                "не обычного Чата. Параметры модели выбираются внутри игры.",
                12,MUTED,false,10);
        }else if("api".equals(effectiveProvider)){
            actionButton("Модель | OpenAI API",
                AiModelCatalog.label(prefs.getString("ai_api_model",AiModelCatalog.DEFAULT_API))+
                " | отдельный API-ключ",false,this::apiModelDialog);
            actionButton("Уровень размышления",
                AiReasoningEffort.label(selectedReasoningEffort())+
                " | отправляется модели",false,this::aiReasoningEffortDialog);
            actionButton("Ввести / заменить OpenAI API-ключ",
                AiKeyVault.configured(this)?"Ключ сохранён (зашифрован)":"Ключ не настроен",
                false,this::aiKeyDialog);
            if(AiKeyVault.configured(this)){
                actionButton("Удалить сохранённый API-ключ",
                    "Не затрагивает игру и историю",false,()->{
                        new RuneDialogBuilder(this,accent()).setTitle("Удалить ключ API?")
                            .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{
                                if(aiClient!=null)aiClient.cancel();
                                if(localAiClient!=null)localAiClient.cancel();
                                AiKeyVault.clear(this);render();
                            }).show();
                    });
            }
            addText(content,"API оплачивается отдельно. Уровни размышления "+
                "доступны не у всех моделей и могут увеличивать задержку и расходы.",
                12,MUTED,false,10);
        }else{
            addText(content,"Ни один источник ИИ не настроен. Выбери ChatGPT, "+
                "OpenAI API или локальную GGUF в меню «Источник ИИ».",
                13,MUTED,false,11);
        }
        foldSettingsSection("ai","ИИ-ПРИКЛЮЧЕНИЕ И МОДЕЛИ",aiStart);
        int updatesStart=content.getChildCount();
        space(content,6);addText(content,"ОБНОВЛЕНИЯ",11,accent(),true,11);
        actionButton("Проверить обновления","Проверка подписи, история изменений и установка внутри приложения",true,()->updater.check(true));
        actionButton("Управление ресурсами","Проверка, докачка, восстановление и выборочное удаление",false,()->new ResourceManagerDialog(this,accent(),this::render).show());
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
            // Results and errors of update checks belong exclusively in the OTA
            // dialog. Never append background network text to the settings UI.
        }
        addText(content,"Текущая версия: "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")",12,MUTED,false,20);
        foldSettingsSection("updates","ОБНОВЛЕНИЯ",updatesStart);
        int savesStart=content.getChildCount();
        addText(content,"СОХРАНЕНИЯ",11,accent(),true,11);
        actionButton("Сохранить сейчас","Данные хранятся на устройстве",false,()->{save();toast("Прогресс сохранён");});
        actionButton("Копировать сохранение","Резервная копия в буфер обмена",false,()->{
            try{JSONObject j=game.toJson();j.put("ai_history",aiMemory.exportSession(aiSession));String s=j.toString();ClipboardManager cb=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("SKYRIM SAVE",s));toast("Код сохранения скопирован");}catch(Exception e){toast("Не удалось экспортировать");}
        });
        actionButton("Восстановить сохранение","Вставить код из резервной копии",false,this::restoreDialog);
        actionButton("Начать новую игру","Старое сохранение будет заменено",false,this::resetDialog);
        foldSettingsSection("saves","СОХРАНЕНИЯ И НОВАЯ ИГРА",savesStart);
        space(content,10);addText(content,"НЕОФИЦИАЛЬНЫЙ ФАНАТСКИЙ ПРОЕКТ",11,accent(),true,8);
        addText(content,"Авторские тексты и игровые механики, пользовательские материалы и фанатский атлас. Не связан с Bethesda Softworks.",12,MUTED,false,0);
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
    public void toast(String message){themedNotice(message);}
    int currentAccent(){return accent();}
    @Override public void onBackPressed(){if(titleOpen){super.onBackPressed();return;}
        if(!game.started||tab==5||tab==6){titleOpen=true;render();return;}
        if(game.started&&tab!=0){tab=0;render();return;}
        if(game.started&&!game.mode.equals("WORLD")&&!game.mode.equals("COMBAT")){recentItemGains.clear();game.choose("GO_WORLD");save();render();return;}
        super.onBackPressed();}
}

