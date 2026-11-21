package dev.infend.skyrimtext;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import java.util.concurrent.atomic.AtomicBoolean;

/** Android-managed resource refresh, independent of app APK OTA. */
public final class ResourceSyncJob extends JobService {
    private static final int ID=0x53595203;
    private static final long PERIOD=60L*60L*1000L;
    private AtomicBoolean cancellation;
    static void configure(Context context){
        Context app=context.getApplicationContext();
        JobScheduler scheduler=(JobScheduler)app.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler==null)return;
        int network=ResourceStore.wifiOnly(app)?JobInfo.NETWORK_TYPE_UNMETERED:JobInfo.NETWORK_TYPE_ANY;
        JobInfo old=scheduler.getPendingJob(ID);
        if(old!=null&&old.getIntervalMillis()==PERIOD&&old.getNetworkType()==network)return;
        try{
            scheduler.schedule(new JobInfo.Builder(ID,new ComponentName(app,ResourceSyncJob.class))
                .setRequiredNetworkType(network).setPersisted(true)
                .setPeriodic(PERIOD,15L*60L*1000L).build());
        }catch(RuntimeException ex){android.util.Log.w("SkyrimResources","Background schedule unavailable",ex);}
    }
    @Override public boolean onStartJob(JobParameters params){
        final AtomicBoolean stop=new AtomicBoolean(false);
        cancellation=stop;
        Context app=getApplicationContext();
        ResourceStore.IO.execute(()->{
            try{ResourceStore.automaticSync(app,stop);}
            finally{ResourceStore.UI.post(()->{
                if(cancellation==stop){cancellation=null;if(!stop.get())jobFinished(params,false);}
            });}
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params){
        if(cancellation!=null)cancellation.set(true);
        cancellation=null;
        return true;
    }
}
