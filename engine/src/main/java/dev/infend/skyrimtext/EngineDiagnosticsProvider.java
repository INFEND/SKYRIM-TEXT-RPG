package dev.infend.skyrimtext;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

/** Exit history is read from the engine UID, outside the native worker. */
public final class EngineDiagnosticsProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){
        if(getContext().getPackageManager().checkSignatures(Binder.getCallingUid(),Process.myUid())
                !=PackageManager.SIGNATURE_MATCH)throw new SecurityException("Invalid caller");
        Bundle out=new Bundle();
        if(!"exit".equals(method)||extras==null||Build.VERSION.SDK_INT<30)return out;
        int pid=extras.getInt("pid",0);
        long since=extras.getLong("since",Long.MAX_VALUE);
        if(pid<=0||since<=0)return out;
        try{
            ActivityManager am=getContext().getSystemService(ActivityManager.class);
            if(am==null)return out;
            for(ApplicationExitInfo e:am.getHistoricalProcessExitReasons(getContext().getPackageName(),pid,8)){
                if(e.getPid()!=pid||e.getTimestamp()<since||
                    !(getContext().getPackageName()+":llm").equals(e.getProcessName()))continue;
                int reason=e.getReason();
                String detail;
                switch(reason){
                    case ApplicationExitInfo.REASON_LOW_MEMORY:
                        detail="Android подтвердил завершение из-за нехватки доступной памяти (LMK)";break;
                    case ApplicationExitInfo.REASON_CRASH_NATIVE:
                        detail="Android подтвердил сбой нативного движка (код "+e.getStatus()+")";break;
                    case ApplicationExitInfo.REASON_SIGNALED:
                        detail="Процесс завершён сигналом "+e.getStatus();break;
                    case ApplicationExitInfo.REASON_CRASH:
                        detail="Android подтвердил ошибку Java/Kotlin";break;
                    case ApplicationExitInfo.REASON_ANR:
                        detail="Android зафиксировал отсутствие ответа процесса (ANR)";break;
                    case ApplicationExitInfo.REASON_USER_REQUESTED:
                        detail="Процесс остановлен системой по запросу пользователя";break;
                    case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:
                        detail="Android остановил процесс за превышение ограничения ресурсов";break;
                    default:
                        detail="Причина Android: код "+reason+", статус "+e.getStatus();break;
                }
                out.putBoolean("found",true);
                out.putBoolean("lowMemory",reason==ApplicationExitInfo.REASON_LOW_MEMORY);
                out.putString("detail",detail);
                return out;
            }
        }catch(Exception ignored){}
        return out;
    }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String order){return null;}
    @Override public String getType(Uri u){return null;}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
