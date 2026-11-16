package dev.infend.skyrimtext;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Compact category dashboard; selection still works at the signed catalog package level. */
final class ResourceManagerDialog {
    private final Activity activity;
    private final int accent;
    private final Runnable refreshed;
    private final String initialGroup;
    private final LinkedHashMap<String,List<ResourceStore.Entry>> groups=new LinkedHashMap<>();
    private final LinkedHashMap<String,Category> categories=new LinkedHashMap<>();
    private final Map<String,ResourceStore.State> verified=new HashMap<>();
    private final AtomicBoolean stop=new AtomicBoolean();
    private Dialog dialog;
    private LinearLayout root, list, controls, settings;
    private TextView summary,status;
    private Button allButton, stopButton;
    private ImageButton closeButton;
    private boolean lastAllReady;
    private boolean busy;
    private String activePath="", operation="", catalogReport="";
    private static final int MUTED=0xffaaaaaa;
    private static final String[] KEYS={"gallery","portraits","map","icons","font","media"};
    private static final String[] LABELS={"Галереи локаций","Портреты персонажей","Карты мира","Расширенные иконки","Дополнительные шрифты","Дополнительные ресурсы"};
    private static final class Category {
        final String key,label;final List<String> packages=new ArrayList<>();
        TextView amount, percent;ResourceMeter progress;ImageButton action;View body;
        String actionIcon="";
        Category(String k,String l){key=k;label=l;}
    }
    private static final class Count {
        long done,total;boolean ready=true;int fresh,old,damaged,missing;
        String status(){
            ArrayList<String> parts=new ArrayList<>();
            if(fresh>0)parts.add("Новые: "+fresh);
            if(old>0)parts.add("Обновить: "+old);
            if(damaged>0)parts.add("Повреждены: "+damaged);
            if(missing>0)parts.add("Не скачаны: "+missing);
            return ready?"Установлено":String.join(" · ",parts);
        }
    }
    ResourceManagerDialog(Activity a,int color,Runnable refresh){this(a,color,refresh,"");}
    ResourceManagerDialog(Activity a,int color,Runnable refresh,String group){activity=a;accent=color;refreshed=refresh;initialGroup=group;}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private TextView text(String s,int size,int color){
        TextView v=new TextView(activity);v.setText(s);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(SovngardeTypeface.face(activity,false));return v;
    }
    private GradientDrawable shape(int color,int stroke,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    private Button button(String label,boolean filled,Runnable click){
        Button v=new Button(activity);v.setText(label);v.setTextSize(14);v.setAllCaps(false);
        v.setTextColor(filled?onAccent():0xffeeeeee);v.setTypeface(SovngardeTypeface.face(activity,false));
        v.setMinHeight(0);v.setMinimumHeight(0);v.setMinWidth(0);v.setMinimumWidth(0);
        v.setPadding(dp(5),0,dp(5),0);v.setBackgroundTintList(null);
        v.setBackground(shape(filled?accent:0xff151515,0,14));v.setOnClickListener(w->click.run());return v;
    }
    private int onAccent(){return Color.luminance(accent)>.42?Color.BLACK:Color.WHITE;}
    private int accentSurface(){return android.graphics.Color.rgb(
        16+Color.red(accent)/12,16+Color.green(accent)/12,16+Color.blue(accent)/12);}
    private ImageButton iconButton(String key,int color,int background){
        ImageButton b=new ImageButton(activity);
        b.setBackgroundTintList(null);b.setBackground(shape(background,0,14));
        b.setImageDrawable(new ResourceGlyph(key,color));b.setScaleType(ImageView.ScaleType.FIT_CENTER);
        b.setPadding(dp(10),dp(10),dp(10),dp(10));return b;
    }
    private void addTool(LinearLayout line,String label,Runnable action){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),1);
        if(line.getChildCount()>0)p.leftMargin=dp(7);line.addView(button(label,false,action),p);
    }
    private void showStatus(String value){
        status.setText(value);status.setVisibility(value.isEmpty()||"Готово".equals(value)?View.GONE:View.VISIBLE);
    }
    private void divider(LinearLayout p){
        View line=new View(activity);line.setBackgroundColor(0xff202020);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(10);lp.bottomMargin=dp(10);p.addView(line,lp);
    }
    private void addSwitch(String label,boolean checked,android.widget.CompoundButton.OnCheckedChangeListener change){
        Switch s=new Switch(activity);s.setText(label);s.setTextColor(Color.WHITE);s.setTextSize(13);
        s.setTypeface(SovngardeTypeface.face(activity,false));
        int[][] states={{android.R.attr.state_checked},{}};
        s.setThumbTintList(new ColorStateList(states,new int[]{accent,0xff888888}));
        s.setTrackTintList(new ColorStateList(states,new int[]{accentSurface(),0xff292929}));
        s.setMinHeight(dp(42));s.setChecked(checked);s.setOnCheckedChangeListener(change);settings.addView(s);
    }
    void show(){
        root=column();root.setPadding(dp(16),dp(13),dp(16),dp(14));
        root.setBackground(shape(Color.BLACK,0,24));root.setClipToOutline(true);
        LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("Управление ресурсами",20,Color.WHITE);
        title.setTypeface(SovngardeTypeface.face(activity,true));
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        closeButton=iconButton("close",0xffc4c4c4,0xff161616);closeButton.setContentDescription("Закрыть");
        closeButton.setOnClickListener(v->{if(!busy)dialog.dismiss();});
        header.addView(closeButton,new LinearLayout.LayoutParams(dp(42),dp(42)));root.addView(header);
        summary=text("Проверка каталога…",13,MUTED);summary.setPadding(dp(2),dp(3),0,dp(8));root.addView(summary);
        ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout scrolling=column();scroll.addView(scrolling);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        list=column();scrolling.addView(list);
        settings=column();scrolling.addView(settings);
        divider(settings);
        TextView section=text("Настройки загрузки",14,0xffd0d0d0);settings.addView(section);
        addSwitch("Загрузка только через Wi-Fi",ResourceStore.wifiOnly(activity),(v,on)->ResourceStore.wifiOnly(activity,on));
        addSwitch("Докачивать фото при посещении локации",ResourceStore.autoGallery(activity),(v,on)->ResourceStore.autoGallery(activity,on));
        addSwitch("Автообновлять установленные ресурсы",ResourceStore.autoUpdate(activity),(v,on)->ResourceStore.autoUpdate(activity,on));
        addSwitch("Автоматически скачивать новые ресурсы",ResourceStore.autoNew(activity),(v,on)->ResourceStore.autoNew(activity,on));
        controls=column();root.addView(controls);divider(controls);
        allButton=button("Скачать все ресурсы",true,()->run("download",new LinkedHashSet<>(groups.keySet())));
        controls.addView(allButton,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout tools=new LinearLayout(activity);
        addTool(tools,"Проверить",this::refreshCatalog);
        addTool(tools,"Перекачать",()->choose("force",new LinkedHashSet<>(groups.keySet())));
        addTool(tools,"Удалить",()->choose("delete",new LinkedHashSet<>(groups.keySet())));
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(42));tp.topMargin=dp(8);controls.addView(tools,tp);
        status=text("",12,MUTED);status.setPadding(0,dp(6),0,0);root.addView(status);status.setVisibility(View.GONE);
        stopButton=button("Остановить / продолжить позже",false,()->{stop.set(true);showStatus("Останавливаю операцию…");});
        stopButton.setVisibility(View.GONE);root.addView(stopButton,new LinearLayout.LayoutParams(-1,dp(44)));
        dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);dialog.setContentView(root);
        dialog.setOnDismissListener(d->{stop.set(true);ResourceStore.UI.removeCallbacks(tick);if(!activity.isFinishing()&&!activity.isDestroyed())refreshed.run();});
        Window window=dialog.getWindow();
        if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(.74f);}
        dialog.show();
        if(window!=null){
            android.util.DisplayMetrics metrics=activity.getResources().getDisplayMetrics();
            window.setLayout(Math.min(dp(480),metrics.widthPixels-dp(24)),Math.min(dp(820),Math.round(metrics.heightPixels*.90f)));
        }
        SovngardeTypeface.watchDialog(dialog);
        rebuildGroups();
        ResourceStore.UI.post(tick);
        refreshCatalog();
        if(groups.containsKey(initialGroup)){
            for(Category c:categories.values())if(c.packages.contains(initialGroup)){
                final Category target=c;scroll.post(()->scroll.smoothScrollTo(0,target.body.getTop()));break;
            }
        }
    }
    private void rebuildGroups(){
        groups.clear();categories.clear();verified.clear();list.removeAllViews();
        try{for(ResourceStore.Entry e:ResourceStore.entries(activity))groups.computeIfAbsent(e.group,k->new ArrayList<>()).add(e);}
        catch(Exception e){showStatus("Каталог ресурсов отсутствует: "+e.getMessage());}
        for(int i=0;i<KEYS.length;i++){
            Category c=new Category(KEYS[i],LABELS[i]);
            for(String g:groups.keySet())if(g.startsWith(c.key+":"))c.packages.add(g);
            if(!c.packages.isEmpty())categories.put(c.key,c);
        }
        Category other=new Category("other","Прочие ресурсы");
        for(String g:groups.keySet()){
            boolean found=false;for(Category c:categories.values())if(c.packages.contains(g)){found=true;break;}
            if(!found)other.packages.add(g);
        }
        if(!other.packages.isEmpty())categories.put(other.key,other);
        categories.put("engine",new Category("engine","Движок локального ИИ"));
        categories.put("gguf",new Category("gguf","Локальные модели GGUF"));
        for(Category c:categories.values())addRow(c);
        updateRows();
    }
    private void refreshCatalog(){
        if(busy)return;
        stop.set(false);setBusy(true);showStatus("Проверка каталога в сети…");
        ResourceStore.IO.execute(()->{
            boolean changed=ResourceStore.refreshCatalog(activity);
            String error=ResourceStore.catalogError(activity);
            if(!stop.get()&&error.isEmpty())ResourceStore.autoSync(activity.getApplicationContext(),stop);
            final String outcome=!error.isEmpty()?error:(changed?"Каталог обновлён":"Каталог актуален");
            ResourceStore.UI.post(()->{
                if(!dialog.isShowing())return;
                catalogReport=outcome;rebuildGroups();setBusy(false);
                if(!stop.get()&&!groups.isEmpty())run("check",new LinkedHashSet<>(groups.keySet()));
                else showStatus("Проверка остановлена");
            });
        });
    }
    private void addRow(Category c){
        LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(9),dp(9),dp(9),dp(9));row.setBackground(shape(0xff101010,0,16));c.body=row;
        ImageView icon=new ImageView(activity);icon.setImageDrawable(new ResourceGlyph(c.key,accent));
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(24),dp(24));ip.rightMargin=dp(10);row.addView(icon,ip);
        LinearLayout body=column();body.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text(c.label,15,Color.WHITE);body.addView(title);
        LinearLayout info=new LinearLayout(activity);info.setGravity(Gravity.CENTER_VERTICAL);
        c.amount=text("",12,MUTED);info.addView(c.amount,new LinearLayout.LayoutParams(0,-2,1));
        c.percent=text("",11,accent);c.percent.setGravity(Gravity.END);info.addView(c.percent,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(3);body.addView(info,ap);
        c.progress=new ResourceMeter(activity,accent);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(4));pp.topMargin=dp(6);body.addView(c.progress,pp);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.rightMargin=dp(10);row.addView(body,bp);
        c.action=iconButton("download",accent,accentSurface());c.action.setContentDescription("Скачать: "+c.label);
        row.addView(c.action,new LinearLayout.LayoutParams(dp(42),dp(42)));
        c.action.setOnClickListener(v->categoryAction(c));
        body.setOnClickListener(v->{if(!busy){if(c.packages.isEmpty())categoryAction(c);else details(c);}});
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(6);list.addView(row,rp);
    }
    private void categoryAction(Category c){
        if(busy)return;
        if("engine".equals(c.key))LocalAiEngineModule.show(activity,accent,()->{updateRows();refreshed.run();});
        else if("gguf".equals(c.key)){if(activity instanceof MainActivity)((MainActivity)activity).selectLocalModel();}
        else if(count(c.packages).ready)details(c);
        else run("download",new LinkedHashSet<>(c.packages));
    }
    private String number(long n,long unit){
        if(n==0)return "0";
        if(n<unit/100)return "<0,01";
        String value=String.format(Locale.ROOT,"%.2f",n/(double)unit);
        return value.replaceAll("0+$","").replaceAll("\\.$","").replace('.',',');
    }
    private String sizePair(long done,long total){
        long basis=total>0?total:done;long unit=basis>=1000000L?1000000L:1000L;
        return number(done,unit)+"/"+(total>0?number(total,unit):"?")+(unit==1000000L?" МБ":" КБ");
    }
    private Count count(Collection<String> packages){
        Count count=new Count();
        for(String key:packages)for(ResourceStore.Entry e:groups.get(key)){
            count.total+=e.size;boolean ready=false;
            try{
                ResourceStore.State s=ResourceStore.state(activity,e,false);
                ready=s==ResourceStore.State.READY||s==ResourceStore.State.BUNDLED;
                if(s==ResourceStore.State.BUNDLED&&verified.get(e.path)==ResourceStore.State.DAMAGED){ready=false;s=ResourceStore.State.DAMAGED;}
                switch(s){case NEW:count.fresh++;break;case OLD:count.old++;break;case DAMAGED:count.damaged++;break;case MISSING:count.missing++;break;default:break;}
            }catch(Exception ignored){}
            if("force".equals(operation)&&e.path.equals(activePath))ready=false;
            count.ready&=ready;
            count.done+=ready?e.size:Math.min(e.size,ResourceStore.currentPartialBytes(activity,e));
        }
        return count;
    }
    private void setRow(Category c,long done,long total,boolean ready,boolean active,String suffix){
        String amount=total<=0&&!active?("engine".equals(c.key)?"Размер неизвестен":"По выбору"):sizePair(done,total);
        if("engine".equals(c.key)&&total>0&&!ready&&done==total)amount+=" · Установить";
        if(!suffix.isEmpty())amount+="\n"+suffix;
        c.amount.setText(amount);
        c.amount.setContentDescription(sizePair(done,total)+(suffix.isEmpty()?"":" · "+suffix));
        int percent=total>0?(int)Math.min(100,done*100.0/total):0;
        c.percent.setText(total>0?percent+"%":active?"…":"");
        c.percent.setContentDescription(suffix.isEmpty()?c.percent.getText():c.percent.getText()+" · "+suffix);
        c.progress.update(total>0?Math.min(1f,done/(float)total):0f,total<=0&&active);
        String glyph=ready?"check":active?"clock":"download";
        if(!glyph.equals(c.actionIcon)){
            c.actionIcon=glyph;c.action.setImageDrawable(new ResourceGlyph(glyph,ready?0xffa9b8ac:accent));
            c.action.setBackground(shape(ready?0xff1a211c:accentSurface(),0,14));
        }
        c.action.setEnabled(!busy&&!active);
        c.action.setAlpha(c.action.isEnabled()||ready?1f:.48f);
        c.action.setContentDescription((ready?"Управление: ":"Скачать: ")+c.label);
    }
    private void updateRows(){
        if(dialog==null||!dialog.isShowing())return;
        long allDone=0,allTotal=0;boolean allReady=true;
        for(Category c:categories.values()){
            if("engine".equals(c.key)){
                boolean ready=LocalAiEngineModule.installed(activity);long total=LocalAiEngineModule.totalBytes(activity);
                long done=LocalAiEngineModule.downloadedBytes(activity);
                setRow(c,done,total,ready,LocalAiEngineModule.downloading(),ready?"Установлен":done>0&&done==total?"Установить":"");continue;
            }
            if("gguf".equals(c.key)){
                java.io.File dir=activity.getExternalFilesDir("models");
                java.io.File file=dir==null?null:new java.io.File(dir,"skyrim-local.gguf");
                long bytes=file!=null&&file.isFile()?file.length():0;
                boolean importing=LocalAiModelStore.importing;
                setRow(c,importing?LocalAiModelStore.importingBytes:bytes,importing?LocalAiModelStore.importingTotal:bytes,
                    !importing&&bytes>0,importing,importing?"Импорт":bytes>0?"Импортирована":"По выбору");continue;
            }
            Count n=count(c.packages);allDone+=n.done;allTotal+=n.total;allReady&=n.ready;
            setRow(c,n.done,n.total,n.ready,false,n.status());
        }
        String error=ResourceStore.catalogError(activity),auto=ResourceStore.automaticError(activity);
        int fileCount=0;for(List<ResourceStore.Entry> entries:groups.values())fileCount+=entries.size();
        summary.setText("Ресурсы игры: "+sizePair(allDone,allTotal)+" · "+fileCount+" файлов"+(error.isEmpty()?"":"\n"+error)+(auto.isEmpty()?"":"\n"+auto));
        allButton.setEnabled(!busy&&!groups.isEmpty()&&!allReady);
        boolean complete=allReady&&!groups.isEmpty();
        allButton.setText(complete?"✓  Все ресурсы загружены":"Скачать / обновить все ресурсы");
        allButton.setTextColor(complete?0xffb9c6bb:onAccent());
        if(lastAllReady!=complete){allButton.setBackground(shape(complete?0xff151d17:accent,0,14));lastAllReady=complete;}
        allButton.setAlpha(busy&&!complete?.55f:1f);
    }
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(dialog!=null&&dialog.isShowing()){updateRows();ResourceStore.UI.postDelayed(this,500);}
    }};
    private void details(Category c){
        LinkedHashSet<String> picked=new LinkedHashSet<>(c.packages);
        if(c.packages.contains(initialGroup)){picked.clear();picked.add(initialGroup);}
        showSelection(c.label,c.packages,picked,null);
    }
    private void choose(String action,Set<String> candidates){
        if(busy)return;
        showSelection("force".equals(action)?"Перекачать ресурсы":"Удалить ресурсы",new ArrayList<>(candidates),new LinkedHashSet<>(),action);
    }
    private void showSelection(String title,List<String> keys,Set<String> picked,String action){
        ScrollView scroll=new ScrollView(activity);LinearLayout items=column();scroll.addView(items);
        items.setPadding(dp(12),dp(8),dp(12),dp(8));
        List<CheckBox> boxes=new ArrayList<>();
        CheckBox all=new CheckBox(activity);all.setText("Выбрать всё");all.setTextColor(Color.WHITE);all.setButtonTintList(ColorStateList.valueOf(accent));
        items.addView(all);all.setChecked(picked.size()==keys.size());
        for(String key:keys){
            Count n=count(Collections.singleton(key));CheckBox box=new CheckBox(activity);
            box.setText(groups.get(key).get(0).label+"\n"+sizePair(n.done,n.total)+" · "+n.status());
            box.setTextColor(Color.WHITE);box.setButtonTintList(ColorStateList.valueOf(accent));box.setChecked(picked.contains(key));
            box.setOnCheckedChangeListener((v,on)->{if(on)picked.add(key);else picked.remove(key);});
            items.addView(box);boxes.add(box);
        }
        all.setOnCheckedChangeListener((v,on)->{for(CheckBox box:boxes)box.setChecked(on);});
        LinearLayout panel=column();panel.addView(scroll,new LinearLayout.LayoutParams(-1,dp(280)));
        AlertDialog chooser=new RuneDialogBuilder(activity,accent).setTitle(title).setView(panel).setNegativeButton("Закрыть",null).create();
        if(action==null){
            panel.addView(button("Докачать / обновить выбранные",true,()->{if(!picked.isEmpty()){chooser.dismiss();run("download",picked);}}),new LinearLayout.LayoutParams(-1,dp(48)));
            panel.addView(button("Перекачать выбранные",false,()->{if(!picked.isEmpty()){chooser.dismiss();run("force",picked);}}),new LinearLayout.LayoutParams(-1,dp(48)));
            panel.addView(button("Удалить выбранные",false,()->{if(!picked.isEmpty()){chooser.dismiss();confirmDelete(picked);}}),new LinearLayout.LayoutParams(-1,dp(48)));
        }else panel.addView(button("force".equals(action)?"Перекачать выбранные":"Удалить выбранные",true,()->{
            if(!picked.isEmpty()){chooser.dismiss();if("delete".equals(action))confirmDelete(picked);else run(action,picked);}
        }),new LinearLayout.LayoutParams(-1,dp(48)));
        chooser.show();
    }
    private void confirmDelete(Set<String> picked){
        new RuneDialogBuilder(activity,accent).setTitle("Удалить выбранные ресурсы?")
            .setMessage("Сохранения, движок ИИ и модели GGUF не затрагиваются. Встроенный шрифт Sovngarde останется доступен.")
            .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->run("delete",picked)).show();
    }
    private void setBusy(boolean on){
        busy=on;
        for(int i=0;i<controls.getChildCount();i++){
            View child=controls.getChildAt(i);child.setEnabled(!on);
            if(child instanceof LinearLayout){LinearLayout line=(LinearLayout)child;for(int j=0;j<line.getChildCount();j++)line.getChildAt(j).setEnabled(!on);}
        }
        stopButton.setVisibility(on?View.VISIBLE:View.GONE);
        for(int i=0;i<settings.getChildCount();i++)settings.getChildAt(i).setEnabled(!on);
        dialog.setCancelable(!on);dialog.setCanceledOnTouchOutside(!on);closeButton.setEnabled(!on);closeButton.setAlpha(on?.4f:1f);updateRows();
    }
    private void run(String op,Set<String> picked){
        if(busy||picked.isEmpty())return;
        final ArrayList<ResourceStore.Entry> work=new ArrayList<>();
        for(String key:picked)work.addAll(groups.get(key));
        operation=op;stop.set(false);setBusy(true);showStatus("Выполняется…");
        ResourceStore.IO.execute(()->{
            String message="Готово",last="";int errors=0,done=0;
            Map<String,ResourceStore.State> result=new HashMap<>();
            for(ResourceStore.Entry e:work){
                if(stop.get()){message="Остановлено. Незавершённые загрузки можно продолжить.";break;}
                ResourceStore.UI.post(()->{activePath=e.path;});
                try{
                    if("delete".equals(op))ResourceStore.delete(activity,e);
                    else if("force".equals(op))ResourceStore.download(activity,e,true,stop,(label,n,total)->{});
                    else if("download".equals(op)){
                        ResourceStore.State s=ResourceStore.state(activity,e,true);
                        if(s!=ResourceStore.State.READY&&s!=ResourceStore.State.BUNDLED)ResourceStore.download(activity,e,false,stop,null);
                    }
                    result.put(e.path,ResourceStore.state(activity,e,true));
                }catch(Exception ex){if(stop.get()){message="Остановлено. Прогресс сохранён.";break;}errors++;last=ex.getMessage();}
                final int step=++done;final ResourceStore.State state=result.get(e.path);
                ResourceStore.UI.post(()->{if(!dialog.isShowing())return;activePath="";if(state!=null)verified.put(e.path,state);showStatus(step+" / "+work.size()+" | "+e.label);updateRows();});
            }
            if("check".equals(op)&&!stop.get()){
                int fresh=0,old=0,missing=0,damaged=0;
                for(ResourceStore.State state:result.values())switch(state){case NEW:fresh++;break;case OLD:old++;break;case MISSING:missing++;break;case DAMAGED:damaged++;break;default:break;}
                message=catalogReport+"\nНовые: "+fresh+" · Обновить: "+old+" · Не скачаны: "+missing+" · Повреждены: "+damaged;
            }
            final String report=errors==0?message:message+" | ошибок: "+errors+" | "+last;
            ResourceStore.UI.post(()->{if(!dialog.isShowing())return;verified.putAll(result);activePath="";operation="";setBusy(false);showStatus(report);});
        });
    }
}
