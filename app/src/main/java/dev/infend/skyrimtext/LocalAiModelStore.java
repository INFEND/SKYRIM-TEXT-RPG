package dev.infend.skyrimtext;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.provider.OpenableColumns;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** SAF import, verified GGUF header, atomic replacement and app-owned model storage. */
final class LocalAiModelStore {
    static final String PREF_MODEL_PATH="ai_local_model_path";
    private static final ExecutorService COPY=Executors.newSingleThreadExecutor();
    interface Listener {
        void onProgress(long bytes,long total);
        void onImported(String path);
        void onFailure(String message);
    }
    private LocalAiModelStore(){}

    static String path(SharedPreferences prefs){return prefs.getString(PREF_MODEL_PATH,"");}
    static boolean ready(SharedPreferences prefs){
        String path=path(prefs);
        return !path.isEmpty()&&new File(path).isFile()&&new File(path).length()>64L*1024L*1024L;
    }
    static void importFromUri(Activity host,Uri uri,SharedPreferences prefs,AtomicBoolean cancelled,Listener listener){
        Handler main=new Handler(Looper.getMainLooper());
        COPY.execute(()->{
            File part=null;
            try{
                File directory=host.getExternalFilesDir("models");
                if(directory==null)throw new IllegalStateException("Хранилище модели недоступно");
                if(!directory.exists()&&!directory.mkdirs())throw new IOException("Нельзя создать папку модели");
                long expected=-1;
                try(Cursor cursor=host.getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){
                    if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))expected=cursor.getLong(0);
                }
                if(expected>0){
                    long available=new StatFs(directory.getAbsolutePath()).getAvailableBytes();
                    if(expected+134217728L>available)
                        throw new IOException("Недостаточно свободного места для копии GGUF");
                }
                part=new File(directory,".importing.gguf.part");
                byte[] buffer=new byte[1024*1024];
                long copied=0,lastNotice=0;
                try(InputStream input=new BufferedInputStream(host.getContentResolver().openInputStream(uri));
                    FileOutputStream stream=new FileOutputStream(part);
                    BufferedOutputStream output=new BufferedOutputStream(stream)){
                    if(input==null)throw new IOException("Нет доступа к выбранному файлу");
                    int n;boolean header=false;
                    while((n=input.read(buffer))!=-1){
                        if(cancelled.get())throw new InterruptedException("Импорт отменён");
                        if(!header){
                            if(n<4||buffer[0]!='G'||buffer[1]!='G'||buffer[2]!='U'||buffer[3]!='F')
                                throw new IOException("Файл не содержит заголовок GGUF");
                            header=true;
                        }
                        output.write(buffer,0,n);copied+=n;
                        long now=System.currentTimeMillis();
                        if(now-lastNotice>=400){
                            long current=copied,total=expected;
                            main.post(()->{if(!cancelled.get())listener.onProgress(current,total);});
                            lastNotice=now;
                        }
                    }
                    output.flush();stream.getFD().sync();
                }
                if(cancelled.get())throw new InterruptedException("Импорт отменён");
                if(copied<64L*1024L*1024L)throw new IOException("Неполный GGUF");
                File target=new File(directory,"skyrim-local.gguf");
                Files.move(part.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
                part=null;
                prefs.edit().putString(PREF_MODEL_PATH,target.getAbsolutePath()).apply();
                String path=target.getAbsolutePath();
                main.post(()->{if(!cancelled.get())listener.onImported(path);});
            }catch(Exception ex){
                String error=ex.getMessage()==null?"Ошибка импорта GGUF":ex.getMessage();
                main.post(()->{if(!cancelled.get())listener.onFailure(error);});
            }finally{if(part!=null&&part.exists())part.delete();}
        });
    }
}
