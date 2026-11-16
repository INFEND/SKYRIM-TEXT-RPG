package dev.infend.skyrimtext;

import android.app.Activity;
import android.app.AlertDialog;
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
    private AlertDialog dialog;
    private LinearLayout root, list, controls;
    private TextView summary,status;
    private Button allButton, stopButton;
    private boolean busy;
    private String activePath="", operation="";
    private static final int MUTED=0xffaaaaaa;
    private static final String[] KEYS={"gallery","portraits","map","icons","font","media"};
    private static final String[] LABELS={"Галереи локаций","Портреты персонажей","Карты мира","Расширенные иконки","Дополнительные шрифты","Дополнительные ресурсы"};
    private static final int[] ICONS={android.R.drawable.ic_menu_gallery,android.R.drawable.ic_menu_myplaces,android.R.drawable.ic_menu_mapmode,android.R.drawable.ic_menu_sort_by_size,android.R.drawable.ic_menu_edit,android.R.drawable.ic_menu_add};
    private static final class Category {
        final String key,label;final int icon;final List<String> packages=new ArrayList<>();
        TextView amount;ProgressBar progress;ImageButton action;View body;
        Category(String k,String l,int i){key=k;label=l;icon=i;}
    }
    private static final class Count {long done,total;boolean ready=true;}
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
        v.setTextColor(filled?Color.BLACK:accent);v.setTypeface(SovngardeTypeface.face(activity,false));
        v.setBackground(shape(filled?accent:0xff000000,0,16));v.setOnClickListener(w->click.run());return v;
    }
    private ProgressBar progress(){
        ProgressBar p=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
        p.setMax(1000);p.setProgressTintList(ColorStateList.valueOf(accent));
        p.setProgressBackgroundTintList(ColorStateList.valueOf(0xff292929));
        p.setIndeterminateTintList(ColorStateList.valueOf(accent));return p;
    }
    private void divider(LinearLayout p){
        View line=new View(activity);line.setBackgroundColor(0xff202020);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(10);lp.bottomMargin=dp(10);p.addView(line,lp);
    }
    private void addSwitch(String label,boolean checked,android.widget.CompoundButton.OnCheckedChangeListener change){
        Switch s=new Switch(activity);s.setText(label);s.setTextColor(Color.WHITE);s.setTextSize(13);
        s.setTypeface(SovngardeTypeface.face(activity,false));s.setChecked(checked);s.setOnCheckedChangeListener(change);controls.addView(s);
    }
    void show(){
        root=column();root.setPadding(dp(14),dp(8),dp(14),dp(8));root.setBackgroundColor(Color.BLACK);
        summary=text("Проверка каталога…",13,MUTED);root.addView(summary);
        ScrollView scroll=new ScrollView(activity);LinearLayout scrolling=column();scroll.addView(scrolling);
        int maxHeight=Math.min(dp(590),Math.round(activity.getResources().getDisplayMetrics().heightPixels*.65f));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,maxHeight));
        list=column();scrolling.addView(list);
        controls=column();scrolling.addView(controls);
        divider(controls);
        allButton=button("Скачать все ресурсы",true,()->run("download",new LinkedHashSet<>(groups.keySet())));
        controls.addView(allButton,new LinearLayout.LayoutParams(-1,dp(52)));
        LinearLayout tools=new LinearLayout(activity);
        tools.addView(button("Проверить",false,()->run("check",new LinkedHashSet<>(groups.keySet()))),new LinearLayout.LayoutParams(0,dp(48),1));
        tools.addView(button("Перекачать",false,()->choose("force",new LinkedHashSet<>(groups.keySet()))),new LinearLayout.LayoutParams(0,dp(48),1));
        tools.addView(button("Удалить",false,()->choose("delete",new LinkedHashSet<>(groups.keySet()))),new LinearLayout.LayoutParams(0,dp(48),1));
        controls.addView(tools);divider(controls);
        addSwitch("Загрузка только через Wi-Fi",ResourceStore.wifiOnly(activity),(v,on)->ResourceStore.wifiOnly(activity,on));
        addSwitch("Докачивать фото при посещении локации",ResourceStore.autoGallery(activity),(v,on)->ResourceStore.autoGallery(activity,on));
        addSwitch("Автообновлять установленные ресурсы",ResourceStore.autoUpdate(activity),(v,on)->ResourceStore.autoUpdate(activity,on));
        status=text("",12,MUTED);root.addView(status);
        stopButton=button("Остановить / продолжить позже",false,()->{stop.set(true);status.setText("Останавливаю операцию…");});
        stopButton.setVisibility(View.GONE);root.addView(stopButton,new LinearLayout.LayoutParams(-1,dp(46)));
        dialog=new RuneDialogBuilder(activity,accent).setTitle("УПРАВЛЕНИЕ РЕСУРСАМИ").setView(root).setNegativeButton("Закрыть",null).create();
        dialog.setOnDismissListener(d->{stop.set(true);ResourceStore.UI.removeCallbacks(tick);if(!activity.isFinishing()&&!activity.isDestroyed())refreshed.run();});
        dialog.show();
        try{for(ResourceStore.Entry e:ResourceStore.entries(activity))groups.computeIfAbsent(e.group,k->new ArrayList<>()).add(e);}
        catch(Exception e){status.setText("Каталог ресурсов отсутствует: "+e.getMessage());}
        for(int i=0;i<KEYS.length;i++){
            Category c=new Category(KEYS[i],LABELS[i],ICONS[i]);
            for(String g:groups.keySet())if(g.startsWith(c.key+":"))c.packages.add(g);
            if(!c.packages.isEmpty())categories.put(c.key,c);
        }
        Category other=new Category("other","Прочие ресурсы",android.R.drawable.ic_menu_add);
        for(String g:groups.keySet()){
            boolean found=false;for(Category c:categories.values())if(c.packages.contains(g)){found=true;break;}
            if(!found)other.packages.add(g);
        }
        if(!other.packages.isEmpty())categories.put(other.key,other);
        categories.put("engine",new Category("engine","Движок локального ИИ",android.R.drawable.ic_menu_manage));
        categories.put("gguf",new Category("gguf","Локальные модели GGUF",android.R.drawable.ic_menu_save));
        for(Category c:categories.values())addRow(c);
        updateRows();ResourceStore.UI.post(tick);
        if(!groups.isEmpty())run("check",new LinkedHashSet<>(groups.keySet()));
        if(groups.containsKey(initialGroup)){
            for(Category c:categories.values())if(c.packages.contains(initialGroup)){
                final Category target=c;scroll.post(()->scroll.smoothScrollTo(0,target.body.getTop()));break;
            }
        }
    }
    private void addRow(Category c){
        LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0,dp(9),0,dp(9));c.body=row;
        ImageView icon=new ImageView(activity);icon.setImageResource(c.icon);icon.setColorFilter(accent);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(24),dp(24));ip.rightMargin=dp(12);row.addView(icon,ip);
        LinearLayout body=column();body.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text(c.label,16,Color.WHITE);body.addView(title);
        c.amount=text("",13,MUTED);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(4);body.addView(c.amount,ap);
        c.progress=progress();LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(5));pp.topMargin=dp(7);body.addView(c.progress,pp);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.rightMargin=dp(12);row.addView(body,bp);
        c.action=new ImageButton(activity);c.action.setImageResource(android.R.drawable.stat_sys_download);
        c.action.setColorFilter(accent);c.action.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        c.action.setPadding(dp(12),dp(12),dp(12),dp(12));c.action.setBackground(shape(Color.BLACK,0xff655b49,22));
        c.action.setContentDescription("Скачать: "+c.label);row.addView(c.action,new LinearLayout.LayoutParams(dp(44),dp(52)));
        c.action.setOnClickListener(v->categoryAction(c));
        body.setOnClickListener(v->{if(!busy){if(c.packages.isEmpty())categoryAction(c);else details(c);}});
        list.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }
    private void categoryAction(Category c){
        if(busy)return;
        if("engine".equals(c.key))LocalAiEngineModule.show(activity,accent,()->{updateRows();refreshed.run();});
        else if("gguf".equals(c.key)){if(activity instanceof MainActivity)((MainActivity)activity).selectLocalModel();}
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
                if(s==ResourceStore.State.BUNDLED&&verified.get(e.path)==ResourceStore.State.DAMAGED)ready=false;
            }catch(Exception ignored){}
            if("force".equals(operation)&&e.path.equals(activePath))ready=false;
            count.ready&=ready;
            count.done+=ready?e.size:Math.min(e.size,ResourceStore.currentPartialBytes(activity,e));
        }
        return count;
    }
    private void setRow(Category c,long done,long total,boolean ready,boolean active,String suffix){
        c.amount.setText(sizePair(done,total)+(suffix.isEmpty()?"":"  ·  "+suffix));
        boolean unknown=total<=0&&active;
        if(c.progress.isIndeterminate()!=unknown)c.progress.setIndeterminate(unknown);
        if(!unknown)c.progress.setProgress(total>0?(int)Math.min(1000,done*1000.0/total):0,true);
        c.action.setImageResource(ready?android.R.drawable.checkbox_on_background:android.R.drawable.stat_sys_download);
        c.action.setEnabled(!busy&&!active&&(!ready||c.packages.isEmpty()));
        c.action.setAlpha(c.action.isEnabled()||ready?1f:.45f);
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
            setRow(c,n.done,n.total,n.ready,false,n.ready?"Готово":"");
        }
        summary.setText("Ресурсы игры: "+sizePair(allDone,allTotal));
        allButton.setEnabled(!busy&&!groups.isEmpty()&&!allReady);
        allButton.setText(allReady&&!groups.isEmpty()?"Все ресурсы загружены":"Скачать все ресурсы");
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
            box.setText(groups.get(key).get(0).label+"\n"+sizePair(n.done,n.total));
            box.setTextColor(Color.WHITE);box.setButtonTintList(ColorStateList.valueOf(accent));box.setChecked(picked.contains(key));
            box.setOnCheckedChangeListener((v,on)->{if(on)picked.add(key);else picked.remove(key);});
            items.addView(box);boxes.add(box);
        }
        all.setOnCheckedChangeListener((v,on)->{for(CheckBox box:boxes)box.setChecked(on);});
        LinearLayout panel=column();panel.addView(scroll,new LinearLayout.LayoutParams(-1,dp(280)));
        AlertDialog chooser=new RuneDialogBuilder(activity,accent).setTitle(title).setView(panel).setNegativeButton("Закрыть",null).create();
        if(action==null){
            panel.addView(button("Докачать выбранные",true,()->{if(!picked.isEmpty()){chooser.dismiss();run("download",picked);}}),new LinearLayout.LayoutParams(-1,dp(48)));
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
        dialog.setCancelable(!on);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!on);updateRows();
    }
    private void run(String op,Set<String> picked){
        if(busy||picked.isEmpty())return;
        final ArrayList<ResourceStore.Entry> work=new ArrayList<>();
        for(String key:picked)work.addAll(groups.get(key));
        operation=op;stop.set(false);setBusy(true);status.setText("Выполняется…");
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
                ResourceStore.UI.post(()->{if(!dialog.isShowing())return;activePath="";if(state!=null)verified.put(e.path,state);status.setText(step+" / "+work.size()+" | "+e.label);updateRows();});
            }
            final String report=errors==0?message:message+" | ошибок: "+errors+" | "+last;
            ResourceStore.UI.post(()->{if(!dialog.isShowing())return;verified.putAll(result);activePath="";operation="";setBusy(false);status.setText(report);});
        });
    }
}
