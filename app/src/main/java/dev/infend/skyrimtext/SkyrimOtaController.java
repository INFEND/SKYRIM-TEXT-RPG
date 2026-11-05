package dev.infend.skyrimtext;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
/** Coordinates in-app checking, verification, download, and installation. */
public final class SkyrimOtaController {
    private final MainActivity activity;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private OtaProgressDialog screen;
    private boolean busy;
    SkyrimOtaController(MainActivity a){activity=a;}
    private OtaProgressDialog display(){
        if(screen==null||!screen.isShowing()){screen=new OtaProgressDialog(activity);screen.show();}
        return screen;
    }
    public void check(boolean manual){
        if(busy){if(manual)activity.toast("Проверка уже выполняется");return;}
        busy=true;
        if(manual)display().stage("Проверка соединения...","Подключение к серверу...");
        UpdateManager.check(activity,UpdateManager.MANIFEST_URL,new UpdateManager.Callback(){
            public void status(String message){if(manual)display().stage(message,"Подключение к серверу...");}
            public void release(UpdateManager.Release info,boolean newer){
                busy=false;
                if(newer){
                    SharedPreferences p=activity.getSharedPreferences("skyrim_text_v1",0);
                    p.edit().putLong(OtaUpdateSchedule.PREF_AVAILABLE_CODE,info.versionCode)
                        .putString(OtaUpdateSchedule.PREF_AVAILABLE_NAME,info.versionName).apply();
                    long installed=BuildConfig.VERSION_CODE;
                    display().offer(info.versionName,info.notesFor(installed),()->download(info));
                }else if(manual)display().current(BuildConfig.VERSION_NAME);
            }
            public void downloaded(java.io.File f){}
            public void error(String message){busy=false;if(manual)display().fail(message,()->check(true));}
        });
    }
    private void download(UpdateManager.Release release){
        if(busy)return;
        busy=true;
        display().stage("Подготовка обновления","Проверка свободного места и соединения...");
        UpdateManager.download(activity,release,new UpdateManager.Callback(){
            public void status(String message){display().stage(message,"Проверка пакета обновления...");}
            public void progress(long amount,long total){display().downloading(amount,total);}
            public void release(UpdateManager.Release i,boolean n){}
            public void downloaded(java.io.File file){
                busy=false;
                display().stage("Проверка завершена","Открываем системный установщик...");
                ui.postDelayed(()->{if(screen!=null)screen.dismiss();UpdateManager.install(activity);},240L);
            }
            public void error(String message){busy=false;display().fail(message,()->download(release));}
        });
    }
    public void onHostResume(){
        SharedPreferences p=activity.getSharedPreferences("skyrim_text_v1",0);
        if(!p.getBoolean("ota_install_pending",false))return;
        if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls())return;
        p.edit().putBoolean("ota_install_pending",false).apply();
        UpdateManager.install(activity);
    }
}
