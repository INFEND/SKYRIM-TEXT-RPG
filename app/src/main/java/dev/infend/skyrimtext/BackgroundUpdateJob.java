package dev.infend.skyrimtext;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
public final class BackgroundUpdateJob extends JobService {
    private volatile Thread worker;
    @Override public boolean onStartJob(JobParameters params){
        SharedPreferences prefs=getSharedPreferences("skyrim_text_v1",0);
        if(!prefs.getBoolean(OtaUpdateSchedule.PREF_BACKGROUND,false))return false;
        worker=new Thread(()->{
            prefs.edit().putLong("ota_last_background_attempt",System.currentTimeMillis()).apply();
            try{
                UpdateManager.Release release=UpdateManager.fetchLatest(getApplicationContext());
                long installed=getPackageManager().getPackageInfo(getPackageName(),0).getLongVersionCode();
                prefs.edit().putLong("ota_last_background_check",System.currentTimeMillis()).apply();
                if(release.versionCode>installed){
                    prefs.edit().putLong(OtaUpdateSchedule.PREF_AVAILABLE_CODE,release.versionCode)
                        .putString(OtaUpdateSchedule.PREF_AVAILABLE_NAME,release.versionName)
                        .putString("ota_available_notes",release.notesFor(installed))
                        .putLong("ota_available_notes_base_code",installed).apply();
                    long previous=prefs.getLong("ota_last_notified_code",0L);
                    if(release.versionCode>previous){
                        if(notifyUpdate(release.versionName))
                            prefs.edit().putLong("ota_last_notified_code",release.versionCode).apply();
                        // Do not persist a text status if Android notifications
                        // are disabled: updates remain available in the OTA dialog.
                    }
                }
            }catch(Exception error){
                // Background network failures are intentionally silent. The
                // user's manual update dialog owns error and retry messages.
                android.util.Log.w("SkyrimOta","Background update check failed",error);
            }finally{jobFinished(params,false);}
        },"skyrim-text-rpg-background-ota");
        worker.start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params){
        Thread active=worker;
        if(active!=null)active.interrupt();
        return true;
    }
    private boolean notifyUpdate(String version){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            !=PackageManager.PERMISSION_GRANTED)return false;
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(manager==null)return false;
        String channel="skyrim_text_rpg_updates";
        if(Build.VERSION.SDK_INT>=26)
            manager.createNotificationChannel(new NotificationChannel(channel,"Обновления SKYRIM TEXT RPG",
                NotificationManager.IMPORTANCE_DEFAULT));
        Intent open=new Intent(this,MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("ota_open_from_notification",true);
        PendingIntent pending=PendingIntent.getActivity(this,1501,open,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder n=Build.VERSION.SDK_INT>=26
            ?new android.app.Notification.Builder(this,channel)
            :new android.app.Notification.Builder(this);
        n.setSmallIcon(R.drawable.ic_app)
            .setContentTitle("Доступно обновление SKYRIM TEXT RPG")
            .setContentText("Версия "+version+" готова к установке")
            .setAutoCancel(true).setContentIntent(pending).setOnlyAlertOnce(true);
        manager.notify(1501,n.build());
        return true;
    }
}
