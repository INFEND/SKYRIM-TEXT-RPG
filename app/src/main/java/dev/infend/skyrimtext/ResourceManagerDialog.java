package dev.infend.skyrimtext;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Manager operates only on the optional catalog, never on saves or GGUF models. */
final class ResourceManagerDialog {
    private final Activity activity; private final int accent; private final Runnable refreshed;
    private AlertDialog dialog;
    private LinearLayout root, list, controls;
    private TextView summary,status;
    private ProgressBar bar;
    private Button stopButton;
    private final Set<String> selected=new LinkedHashSet<>();
    private final LinkedHashMap<String,List<ResourceStore.Entry>> groups=new LinkedHashMap<>();
    private final Map<String,ResourceStore.State> states=new HashMap<>();
    private final AtomicBoolean stop=new AtomicBoolean();
    private boolean busy;
    private long lastProgressAt;
    ResourceManagerDialog(Activity a,int color,Runnable refresh){activity=a;accent=color;refreshed=refresh;}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,int color){
        TextView v=new TextView(activity);v.setText(s);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(SovngardeTypeface.face(activity,false));v.setPadding(dp(3),dp(5),dp(3),dp(5));return v;
    }
    private LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private Button button(LinearLayout parent,String label,Runnable click){
        Button v=new Button(activity);v.setText(label);v.setAllCaps(false);v.setTextColor(accent);
        v.setTypeface(SovngardeTypeface.face(activity,false));v.setBackground(new RuneFrameDrawable(0xff161b1f,accent,false));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.bottomMargin=dp(5);parent.addView(v,p);
        v.setOnClickListener(w->click.run());return v;
    }
    void show(){
        root=column();root.setPadding(dp(14),dp(8),dp(14),dp(8));
        summary=text("Проверка каталога…",15,0xffe0e5e7);root.addView(summary);
        status=text("",12,0xffb9c3c7);root.addView(status);
        bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setVisibility(View.GONE);root.addView(bar);
        controls=column();root.addView(controls);
        Switch wifi=new Switch(activity);wifi.setText("Загрузка только через Wi-Fi");wifi.setTextColor(Color.WHITE);
        wifi.setChecked(ResourceStore.wifiOnly(activity));wifi.setOnCheckedChangeListener((v,on)->ResourceStore.wifiOnly(activity,on));controls.addView(wifi);
        button(controls,"Проверить целостность",()->run("check",false));
        button(controls,"Докачать отсутствующие / повреждённые",()->run("download",false));
        button(controls,"Выбрать всё / снять выбор",()->{if(selected.size()==groups.size())selected.clear();else selected.addAll(groups.keySet());draw();});
        button(controls,"Докачать выбранные",()->run("download",true));
        button(controls,"Перекачать выбранные",()->run("force",true));
        button(controls,"Удалить выбранные",this::confirmDelete);
        stopButton=button(root,"Остановить / продолжить позже",()->{stop.set(true);status.setText("Останавливаю операцию…");});
        stopButton.setVisibility(View.GONE);
        ScrollView scroll=new ScrollView(activity);list=column();scroll.addView(list);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(290)));
        dialog=new RuneDialogBuilder(activity,accent).setTitle("УПРАВЛЕНИЕ РЕСУРСАМИ")
            .setView(root).setNegativeButton("Закрыть",null).create();
        dialog.setOnDismissListener(d->{stop.set(true);if(!activity.isFinishing()&&!activity.isDestroyed())refreshed.run();});
        dialog.show();
        try{for(ResourceStore.Entry e:ResourceStore.entries(activity))groups.computeIfAbsent(e.group,k->new ArrayList<>()).add(e);}
        catch(Exception e){status.setText("Каталог ресурсов отсутствует: "+e.getMessage());return;}
        run("check",false);
    }
    private String size(long n){return String.format(Locale.ROOT,"%.1f МиБ",n/1048576.0);}
    private void draw(){
        list.removeAllViews();long local=0,total=0,ready=0,partial=0;int goodGroups=0;
        for(Map.Entry<String,List<ResourceStore.Entry>> g:groups.entrySet()){
            long bytes=0;int ok=0,damaged=0,embedded=0;boolean missing=false;
            for(ResourceStore.Entry e:g.getValue()){
                bytes+=e.size;local+=ResourceStore.storedBytes(activity,e);partial+=ResourceStore.partialBytes(activity,e);
                ResourceStore.State s=states.get(e.path);
                if(s==ResourceStore.State.READY||s==ResourceStore.State.BUNDLED){ok++;ready+=e.size;if(s==ResourceStore.State.BUNDLED)embedded++;}
                else if(s==ResourceStore.State.DAMAGED||s==ResourceStore.State.OLD)damaged++;else missing=true;
            }
            total+=bytes;boolean complete=ok==g.getValue().size();if(complete)goodGroups++;
            String label=complete?(embedded>0?"Встроено / доступно":"Загружено"):damaged>0?"Требуется восстановление":missing?"Отсутствует / не полностью":"Проверяется";
            CheckBox box=new CheckBox(activity);box.setText(g.getValue().get(0).label+"  |  "+size(bytes)+"\n"+label);
            box.setTextColor(0xffe0e5e7);box.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
            box.setChecked(selected.contains(g.getKey()));box.setEnabled(!busy);
            box.setOnCheckedChangeListener((v,on)->{if(on)selected.add(g.getKey());else selected.remove(g.getKey());});
            list.addView(box);
        }
        summary.setText("Пакеты: "+goodGroups+" / "+groups.size()+" | "+size(ready)+" / "+size(total)+
            "\nПостоянное хранилище: "+size(local)+" | незавершённые: "+size(partial));
    }
    private void setBusy(boolean on){
        busy=on;for(int i=0;i<controls.getChildCount();i++)controls.getChildAt(i).setEnabled(!on);
        bar.setVisibility(on?View.VISIBLE:View.GONE);stopButton.setVisibility(on?View.VISIBLE:View.GONE);
        if(dialog!=null)dialog.setCancelable(!on);
        if(dialog!=null&&dialog.getButton(AlertDialog.BUTTON_NEGATIVE)!=null)dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!on);
        draw();
    }
    private void confirmDelete(){
        if(selected.isEmpty()){status.setText("Выбери пакеты");return;}
        new RuneDialogBuilder(activity,accent).setTitle("Удалить выбранные ресурсы?")
            .setMessage("Сохранения и модели ИИ не затрагиваются. В переходной версии встроенные копии скрываются, а занимаемое APK место освободится после облегчённого обновления.")
            .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->run("delete",true)).show();
    }
    private void run(String operation,boolean onlySelected){
        if(busy)return;
        if(onlySelected&&selected.isEmpty()){status.setText("Выбери пакеты");return;}
        final ArrayList<ResourceStore.Entry> work=new ArrayList<>();
        for(Map.Entry<String,List<ResourceStore.Entry>> g:groups.entrySet())if(!onlySelected||selected.contains(g.getKey()))work.addAll(g.getValue());
        stop.set(false);setBusy(true);bar.setProgress(0);status.setText("Выполняется…");
        ResourceStore.IO.execute(()->{
            String message="Готово";int errors=0;String last="";int done=0;
            for(ResourceStore.Entry e:work){
                if(stop.get()){message="Остановлено. Незавершённые загрузки можно продолжить.";break;}
                try{
                    if("delete".equals(operation))ResourceStore.delete(activity,e);
                    else if("force".equals(operation))ResourceStore.download(activity,e,true,stop,this::progress);
                    else if("download".equals(operation)){
                        ResourceStore.State state=ResourceStore.state(activity,e,true);
                        if(state!=ResourceStore.State.READY&&state!=ResourceStore.State.BUNDLED)
                            ResourceStore.download(activity,e,false,stop,this::progress);
                    }
                }catch(Exception ex){if(stop.get()){message="Остановлено. Прогресс сохранён.";break;}errors++;last=ex.getMessage();}
                final int step=++done;ResourceStore.UI.post(()->{if(dialog.isShowing()){bar.setProgress(step*1000/Math.max(1,work.size()));status.setText(step+" / "+work.size()+" | "+e.label);}});
            }
            Map<String,ResourceStore.State> result=new HashMap<>();
            for(List<ResourceStore.Entry> group:groups.values())for(ResourceStore.Entry e:group){
                try{result.put(e.path,ResourceStore.state(activity,e,true));}
                catch(Exception ex){result.put(e.path,ResourceStore.State.DAMAGED);}
            }
            final String report=errors==0?message:message+" | ошибок: "+errors+" | "+last;
            ResourceStore.UI.post(()->{if(!dialog.isShowing())return;states.clear();states.putAll(result);setBusy(false);status.setText(report);});
        });
    }
    private void progress(String label,long done,long total){
        long now=android.os.SystemClock.elapsedRealtime();if(now-lastProgressAt<120&&done!=total)return;lastProgressAt=now;
        ResourceStore.UI.post(()->{if(dialog.isShowing()){bar.setProgress((int)Math.min(1000,done*1000/Math.max(1,total)));status.setText(label+" | "+size(done)+" / "+size(total));}});
    }
}
