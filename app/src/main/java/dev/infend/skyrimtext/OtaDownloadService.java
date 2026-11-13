package dev.infend.skyrimtext;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import java.io.File;

/** User-initiated transfer survives the Activity and a locked screen. */
public final class OtaDownloadService extends Service {
    private static final String PREF="ota_transfer_v2", CHANNEL="skyrim_ota_transfer";
    private static final int ID=1502;
    private static final String START="dev.infend.skyrimtext.OTA_START", CANCEL="dev.infend.skyrimtext.OTA_CANCEL";
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static volatile Snapshot live;
    private static volatile boolean running;
    private UpdateManager.DownloadControl control;
    private PowerManager.WakeLock lock;
    private long lastNotification;
    public static final class Snapshot {
        final UpdateManager.Release release;
        final String phase,message;
        final long received,total;
        Snapshot(UpdateManager.Release r,String p,String m,long a,long b){release=r;phase=p;message=m;received=a;total=b;}
        boolean active(){return "prepare".equals(phase)||"download".equals(phase)||"verify".equals(phase)||"stopping".equals(phase);}
    }
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,0);}
    static UpdateManager.Release savedRelease(Context c){
        try{
            UpdateManager.Release r=UpdateManager.Release.restore(prefs(c).getString("manifest",""),prefs(c).getBoolean("thin",false));
            return r.versionCode>BuildConfig.VERSION_CODE?r:null;
        }catch(Exception e){return null;}
    }
    static Snapshot snapshot(Context c){
        Snapshot s=live;
        if(running&&s!=null)return s;
        UpdateManager.Release r=savedRelease(c);if(r==null)return null;
        String phase=prefs(c).getString("phase","paused");
        long bytes=UpdateManager.partialBytes(c,r);
        if("ready".equals(phase)&&UpdateManager.apkFile(c).isFile()&&
            (r.sizeBytes<0||UpdateManager.apkFile(c).length()==r.sizeBytes))
            return new Snapshot(r,"ready","Готово к установке",UpdateManager.apkFile(c).length(),r.sizeBytes);
        return new Snapshot(r,"paused",prefs(c).getString("message","Загрузка остановлена. Можно продолжить."),bytes,r.sizeBytes);
    }
    static boolean start(Context c,UpdateManager.Release r){
        if(running)return false;
        if(!prefs(c).edit().putString("manifest",r.manifestJson()).putBoolean("thin",r.isThin())
            .putString("phase","prepare").putString("message","Подготовка загрузки").commit())return false;
        running=true;live=new Snapshot(r,"prepare","Подготовка загрузки",UpdateManager.partialBytes(c,r),r.sizeBytes);
        try{c.startForegroundService(new Intent(c,OtaDownloadService.class).setAction(START));return true;}
        catch(Exception e){running=false;live=null;prefs(c).edit().putString("phase","paused").putString("message",UpdateManager.friendly(e)).apply();return false;}
    }
    static void cancel(Context c){
        if(!running)return;
        try{c.startService(new Intent(c,OtaDownloadService.class).setAction(CANCEL));}catch(Exception ignored){}
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i!=null&&CANCEL.equals(i.getAction())){
            if(control!=null){control.cancel();Snapshot s=live;
                if(s!=null)publish(new Snapshot(s.release,"stopping","Остановка загрузки",s.received,s.total));}
            return START_NOT_STICKY;
        }
        if(control!=null)return START_NOT_STICKY;
        UpdateManager.Release r=savedRelease(this);
        if(r==null){stopSelf();running=false;return START_NOT_STICKY;}
        running=true;control=new UpdateManager.DownloadControl();
        live=new Snapshot(r,"prepare","Подготовка загрузки",UpdateManager.partialBytes(this,r),r.sizeBytes);
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Загрузка обновлений Skyrim",NotificationManager.IMPORTANCE_LOW));
        if(Build.VERSION.SDK_INT>=29)startForeground(ID,notification(live),ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(ID,notification(live));
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        if(pm!=null){lock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Skyrim:OtaDownload");lock.setReferenceCounted(false);lock.acquire(2*60*60*1000L);}
        final UpdateManager.DownloadControl task=control;
        new Thread(()->{
            try{
                File apk=UpdateManager.downloadResumable(this,r,task,new UpdateManager.Callback(){
                    public void status(String msg){
                        Snapshot old=live;
                        if(old!=null&&!task.cancelled)publish(new Snapshot(r,msg.startsWith("Проверка")?"verify":"prepare",msg,old.received,old.total));
                    }
                    public void progress(long a,long b){if(!task.cancelled)publish(new Snapshot(r,"download","Загрузка обновления",a,b));}
                    public void release(UpdateManager.Release ignored,boolean newer){}
                    public void downloaded(File ignored){}
                    public void error(String ignored){}
                });
                finish(new Snapshot(r,"ready","Готово к установке",apk.length(),r.sizeBytes));
            }catch(Exception e){
                String msg=task.cancelled?"Загрузка отменена. Прогресс сохранён.":UpdateManager.friendly(e);
                finish(new Snapshot(r,"paused",msg,UpdateManager.partialBytes(this,r),r.sizeBytes));
            }
        },"skyrim-ota-transfer").start();
        return START_NOT_STICKY;
    }
    private void publish(Snapshot s){
        live=s;long now=SystemClock.elapsedRealtime();
        if(now-lastNotification<500&&"download".equals(s.phase))return;
        lastNotification=now;
        MAIN.post(()->{if(running&&live!=null){
            try{getSystemService(NotificationManager.class).notify(ID,notification(live));}catch(Exception ignored){}
        }});
    }
    private void finish(Snapshot s){
        MAIN.post(()->{
            prefs(this).edit().putString("phase",s.phase).putString("message",s.message).commit();
            live=s;control=null;running=false;
            releaseLock();
            stopForeground(STOP_FOREGROUND_REMOVE);
            try{getSystemService(NotificationManager.class).notify(ID,notification(s));}catch(Exception ignored){}
            stopSelf();
        });
    }
    private void releaseLock(){if(lock!=null&&lock.isHeld())lock.release();lock=null;}
    private Notification notification(Snapshot s){
        Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("ota_open_from_notification",true);
        PendingIntent go=PendingIntent.getActivity(this,1502,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_app)
            .setContentTitle("SKYRIM TEXT RPG "+s.release.versionName)
            .setContentText(s.message+" | "+OtaProgressDialog.size(s.received)+(s.total>0?" / "+OtaProgressDialog.size(s.total):""))
            .setContentIntent(go).setOnlyAlertOnce(true).setOngoing(s.active()).setAutoCancel(!s.active());
        if(s.active()){
            n.setProgress(1000,s.total>0?(int)Math.min(1000,s.received*1000/s.total):0,s.total<=0);
            Intent stop=new Intent(this,OtaDownloadService.class).setAction(CANCEL);
            PendingIntent cancel=PendingIntent.getService(this,1503,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            n.addAction(new Notification.Action.Builder(null,"Отменить",cancel).build());
        }
        return n.build();
    }
    @Override public void onDestroy(){
        if(control!=null){control.cancel();control=null;}
        releaseLock();super.onDestroy();
    }
    @Override public void onTimeout(int startId,int fgsType){
        if(control!=null)control.cancel();
        releaseLock();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
}
