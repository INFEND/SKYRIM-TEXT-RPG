package dev.infend.skyrimtext;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.res.ColorStateList;

/** Activity UI is an observer of the durable transfer, never its owner. */
public final class SkyrimOtaController {
    private final MainActivity activity;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private OtaProgressDialog screen;
    private LinearLayout mini;
    private TextView miniLabel,miniBytes;
    private ProgressBar miniBar;
    private boolean resumed,hidden,checking,manualCheck,installing;
    private int checkGeneration;
    private String checkStatus="",lastRender="";
    private UpdateManager.Release offered;
    private final Runnable tick=new Runnable(){public void run(){if(!resumed)return;refresh();ui.postDelayed(this,300);}};
    SkyrimOtaController(MainActivity a){activity=a;}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private OtaProgressDialog display(){
        if(screen==null||!screen.isShowing()){screen=new OtaProgressDialog(activity);screen.show();lastRender="";}
        return screen;
    }
    private void hide(){hidden=true;if(screen!=null)screen.dismiss();refresh();}
    private void open(){
        hidden=false;lastRender="";
        OtaDownloadService.Snapshot s=OtaDownloadService.snapshot(activity);
        if(s!=null){display();refresh();}
        else if(checking){display().stage(checkStatus,"Подключение к серверу…");display().controls(this::hide,this::cancelCheck);}
        else if(offered!=null)showOffer(offered);
        else check(true);
    }
    void attachMini(LinearLayout root){
        mini=new LinearLayout(activity);mini.setOrientation(LinearLayout.VERTICAL);
        mini.setPadding(dp(13),dp(8),dp(13),dp(8));mini.setBackground(new RuneFrameDrawable(0xff101417,activity.currentAccent(),false));
        miniLabel=new TextView(activity);miniLabel.setTextColor(0xffeee9df);miniLabel.setTextSize(13);
        miniLabel.setTypeface(SovngardeTypeface.face(activity,true));mini.addView(miniLabel);
        miniBytes=new TextView(activity);miniBytes.setTextColor(0xffb8bfc3);miniBytes.setTextSize(11);
        miniBytes.setTypeface(SovngardeTypeface.face(activity,false));mini.addView(miniBytes);
        miniBar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);miniBar.setMax(1000);
        miniBar.setProgressTintList(ColorStateList.valueOf(activity.currentAccent()));
        mini.addView(miniBar,new LinearLayout.LayoutParams(-1,dp(5)));
        mini.setContentDescription("Открыть окно обновления");mini.setOnClickListener(v->open());
        root.addView(mini,new LinearLayout.LayoutParams(-1,-2));mini.setVisibility(View.GONE);refresh();
    }
    private void refresh(){
        if(activity.isFinishing()||activity.isDestroyed())return;
        OtaDownloadService.Snapshot s=OtaDownloadService.snapshot(activity);
        boolean showing=screen!=null&&screen.isShowing();
        if(mini!=null){
            boolean show=!installing&&!showing&&(s!=null||checking&&manualCheck&&hidden||offered!=null&&hidden);
            mini.setVisibility(show?View.VISIBLE:View.GONE);
            if(show){
                miniLabel.setText(s!=null?s.message:offered!=null?"Доступно обновление "+offered.versionName:checkStatus);
                long a=s==null?0:s.received,b=s==null?-1:s.total;
                int percent=b>0?(int)Math.min(100,a*100/b):0;
                miniBytes.setText(s==null?(offered!=null?OtaProgressDialog.size(offered.sizeBytes):"Проверка обновления"):s.release.versionName+" | "+OtaProgressDialog.size(a)+(b>0?" / "+OtaProgressDialog.size(b)+" | "+percent+"%":""));
                miniBar.setIndeterminate(s==null?checking:s.active()&&b<=0);
                miniBar.setProgress(b>0?(int)Math.min(1000,a*1000/b):0);
            }
        }
        if(s==null||!showing||hidden||installing)return;
        String key=s.phase+"|"+s.message+"|"+s.received+"|"+s.total;
        if(key.equals(lastRender))return;
        if(lastRender.isEmpty())screen.transferNotes(s.release.notesFor(BuildConfig.VERSION_CODE));
        lastRender=key;
        if(s.active())screen.activeTransfer(s.phase,s.message,s.received,s.total,this::hide,()->OtaDownloadService.cancel(activity));
        else if("ready".equals(s.phase)){
            screen.controls(this::hide,null);screen.ready(s.received,s.total,()->{
                installing=true;screen.dismiss();UpdateManager.install(activity);
            });
        }else{
            screen.controls(this::hide,null);
            UpdateManager.Release release=s.release;
            screen.paused(s.message,s.received,s.total,()->download(release),
                release.isThin()?()->new RuneDialogBuilder(activity,activity.currentAccent()).setTitle("Загрузить полный APK?")
                    .setMessage("Незавершённая облегчённая загрузка будет заменена.").setNegativeButton("Отмена",null)
                    .setPositiveButton("Загрузить",(d,w)->download(release.fullVersion())).show():null);
        }
    }
    private void showOffer(UpdateManager.Release info){
        offered=info;hidden=false;
        display().controls(this::hide,null);
        display().offer(info.versionName,info.notesFor(BuildConfig.VERSION_CODE),()->download(info));
        display().downloadSize(info.sizeBytes,info.isThin());refresh();
    }
    private void cancelCheck(){
        checkGeneration++;checking=false;manualCheck=false;hidden=true;
        if(screen!=null)screen.dismiss();refresh();
    }
    public void check(boolean manual){
        if(installing&&manual)installing=false;
        if(installing)return;
        OtaDownloadService.Snapshot pending=OtaDownloadService.snapshot(activity);
        if(pending!=null){if(manual)open();return;}
        if(checking){if(manual){manualCheck=true;open();}return;}
        if(offered!=null&&manual){showOffer(offered);return;}
        checking=true;manualCheck=manual;hidden=!manual;
        int token=++checkGeneration;
        checkStatus="Проверка соединения…";
        if(manual){display().stage(checkStatus,"Подключение к серверу…");display().controls(this::hide,this::cancelCheck);}
        UpdateManager.check(activity,UpdateManager.MANIFEST_URL,new UpdateManager.Callback(){
            private boolean valid(){return token==checkGeneration&&!activity.isFinishing()&&!activity.isDestroyed();}
            public void status(String message){
                if(!valid())return;checkStatus=message;
                if(manualCheck&&!hidden&&resumed){display().stage(message,"Подключение к серверу…");display().controls(SkyrimOtaController.this::hide,SkyrimOtaController.this::cancelCheck);}
                refresh();
            }
            public void release(UpdateManager.Release info,boolean newer){
                if(!valid())return;checking=false;
                if(newer){
                    activity.getSharedPreferences("skyrim_text_v1",0).edit()
                        .putLong(OtaUpdateSchedule.PREF_AVAILABLE_CODE,info.versionCode)
                        .putString(OtaUpdateSchedule.PREF_AVAILABLE_NAME,info.versionName).apply();
                    offered=info;
                    if(resumed&&(!manualCheck||!hidden))showOffer(info);
                    else hidden=true;
                }else if(manualCheck&&!hidden&&resumed){display().controls(null,null);display().current(BuildConfig.VERSION_NAME);}
                refresh();
            }
            public void downloaded(java.io.File f){}
            public void error(String message){
                if(!valid())return;checking=false;
                if(manualCheck&&!hidden&&resumed){display().controls(null,null);display().fail(message,()->check(true));}
                refresh();
            }
        });
    }
    private void download(UpdateManager.Release release){
        offered=null;hidden=false;installing=false;lastRender="";
        if(!OtaDownloadService.start(activity,release)){
            OtaDownloadService.Snapshot s=OtaDownloadService.snapshot(activity);
            if(s==null){display().fail("Не удалось запустить фоновую загрузку.",()->download(release));return;}
        }
        display();refresh();
    }
    public void onHostResume(){
        resumed=true;ui.removeCallbacks(tick);ui.post(tick);
        if(installing)installing=false;
        SharedPreferences p=activity.getSharedPreferences("skyrim_text_v1",0);
        if(p.getBoolean("ota_install_pending",false)&&
            (Build.VERSION.SDK_INT<26||activity.getPackageManager().canRequestPackageInstalls())){
            p.edit().putBoolean("ota_install_pending",false).apply();installing=true;UpdateManager.install(activity);
        }
        Intent i=activity.getIntent();
        if(i!=null&&i.getBooleanExtra("ota_open_from_notification",false)){
            i.removeExtra("ota_open_from_notification");open();
        }
    }
    void onHostPause(){resumed=false;ui.removeCallbacks(tick);}
    void onHostDestroy(){onHostPause();checkGeneration++;if(screen!=null)screen.dismiss();screen=null;mini=null;}
    void onNotificationIntent(){if(resumed){Intent i=activity.getIntent();if(i!=null)i.removeExtra("ota_open_from_notification");open();}}
}
