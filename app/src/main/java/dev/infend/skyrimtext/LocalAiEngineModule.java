package dev.infend.skyrimtext;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.TextView;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
/** Separately installed, signature-checked APK; no downloaded executable is loaded in-process. */
final class LocalAiEngineModule {
    static final String PACKAGE="dev.infend.skyrimtext.engine";
    static final int VERSION=2;
    static final int REQUEST_ACTIVATE=7108;
    static Intent activationIntent(boolean returnToGame){return new Intent().setComponent(new ComponentName(PACKAGE,"dev.infend.skyrimtext.EngineActivationActivity")).putExtra("returnToGame",returnToGame);}
    static String diagnostic(Context c){try{PackageManager pm=c.getPackageManager();PackageInfo info=pm.getPackageInfo(PACKAGE,0);ApplicationInfo app=info.applicationInfo;ServiceInfo svc=pm.getServiceInfo(new ComponentName(PACKAGE,"dev.infend.skyrimtext.LocalAiInferenceService"),0);return "v"+info.versionCode+", приложение "+(app.enabled?"включено":"выключено")+", служба "+(svc.enabled?"включена":"выключена")+", разрешение "+(c.checkSelfPermission("dev.infend.skyrimtext.permission.LOCAL_ENGINE")==PackageManager.PERMISSION_GRANTED?"есть":"нет");}catch(Exception e){return e.getClass().getSimpleName();}}
    private static final AtomicBoolean running=new AtomicBoolean();
    private static final String BASE="https://github.com/INFEND/SKYRIM-TEXT-RPG/releases/download/";
    static File apk(Context c){return new File(c.getFilesDir(),"engine/SKYRIM-LOCAL-AI.apk");}
    static boolean installed(Context c){
        try{PackageManager pm=c.getPackageManager();PackageInfo info=pm.getPackageInfo(PACKAGE,0);
            return info.versionCode==VERSION&&pm.checkSignatures(c.getPackageName(),PACKAGE)==PackageManager.SIGNATURE_MATCH;
        }catch(Exception e){return false;}
    }
    private static JSONObject metadata(Context c)throws Exception{
        try(InputStream in=c.getAssets().open("engine_module.json")){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[1024];int n;
            while((n=in.read(buf))!=-1){if(out.size()+n>8192)throw new IOException("Invalid engine metadata");out.write(buf,0,n);}
            JSONObject j=new JSONObject(out.toString("UTF-8"));
            if(j.getInt("versionCode")!=VERSION||!j.getString("sha256").matches("[0-9a-f]{64}")
                ||j.getLong("size")<=0||j.getLong("size")>100_000_000L)throw new IOException("Invalid engine metadata");
            return j;
        }
    }
    static long totalBytes(Context c){
        try{return metadata(c).getLong("size");}catch(Exception e){return 0;}
    }
    static long downloadedBytes(Context c){
        try{
            JSONObject j=metadata(c);long size=j.getLong("size");
            if(installed(c))return size;
            File target=apk(c),part=new File(target.getPath()+".part");
            if(target.isFile()&&target.length()==size)return size;
            return j.getString("sha256").equals(c.getSharedPreferences("skyrim_engine",0).getString("partial",""))
                ?Math.min(size,part.length()):0;
        }catch(Exception e){return 0;}
    }
    static boolean downloading(){return running.get();}
    static String sizeLabel(Context c){
        try{return String.format(Locale.ROOT,"%.1f МБ",metadata(c).getLong("size")/1_000_000.0);}
        catch(Exception e){return "Отдельный APK";}
    }
    private static String sha(File f)throws Exception{
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(f)){byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)d.update(buf,0,n);}
        StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();
    }
    private static void verify(Context c,File f,JSONObject j)throws Exception{
        if(f.length()!=j.getLong("size")||!sha(f).equals(j.getString("sha256")))throw new IOException("Контрольная сумма движка не совпадает");
        PackageManager pm=c.getPackageManager();
        int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo a=pm.getPackageArchiveInfo(f.getPath(),flags),b=pm.getPackageInfo(c.getPackageName(),flags);
        if(a==null||!PACKAGE.equals(a.packageName)||a.versionCode!=VERSION)throw new IOException("Неверный пакет движка");
        Signature[] x=Build.VERSION.SDK_INT>=28?a.signingInfo.getApkContentsSigners():a.signatures;
        Signature[] y=Build.VERSION.SDK_INT>=28?b.signingInfo.getApkContentsSigners():b.signatures;
        if(x==null||y==null||x.length!=1||!Arrays.equals(x,y))throw new IOException("Неверная подпись движка");
    }
    private static HttpsURLConnection connect(String raw,long offset)throws Exception{
        for(int i=0;i<6;i++){
            URL url=new URL(raw);String host=url.getHost();
            if(!"https".equals(url.getProtocol())||url.getUserInfo()!=null||url.getPort()!=-1||
                !Arrays.asList("github.com","release-assets.githubusercontent.com","objects.githubusercontent.com","github-releases.githubusercontent.com").contains(host))
                throw new IOException("Недопустимый адрес движка");
            HttpsURLConnection con=(HttpsURLConnection)url.openConnection();
            con.setInstanceFollowRedirects(false);con.setConnectTimeout(15000);con.setReadTimeout(25000);
            con.setRequestProperty("Accept-Encoding","identity");
            if(offset>0)con.setRequestProperty("Range","bytes="+offset+"-");
            int code=con.getResponseCode();
            if(code>=300&&code<400){String next=con.getHeaderField("Location");con.disconnect();if(next==null)throw new IOException("Нет адреса перенаправления");raw=new URL(url,next).toString();continue;}
            return con;
        }
        throw new IOException("Слишком много перенаправлений");
    }
    private static void launch(Activity a)throws Exception{
        if(!a.getPackageManager().canRequestPackageInstalls()){
            a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName())));
            throw new IOException("Разреши установку из SKYRIM, затем нажми «Установить движок» ещё раз.");
        }
        Uri uri=Uri.parse("content://"+a.getPackageName()+".updates/engine");
        Intent view=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive");
        view.setClipData(ClipData.newRawUri("SKYRIM Local AI",uri));
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
        a.getApplicationContext().startActivity(view);
    }
    static void show(Activity a,int accent,Runnable refresh){
        if(installed(a)){
            apk(a).delete();
            new RuneDialogBuilder(a,accent).setTitle("ДВИЖОК ЛОКАЛЬНОГО ИИ")
                .setMessage("Установлен отдельно. Версия "+VERSION+". Активируй движок, если Android не запускает службу. GGUF и сохранения не удаляются.")
                .setNegativeButton("Закрыть",null).setNeutralButton("Активировать",(d,w)->{try{a.startActivity(activationIntent(false));}catch(Exception e){new RuneDialogBuilder(a,accent).setMessage("Не удалось открыть движок: "+e.getMessage()).setPositiveButton("Закрыть",null).show();}}).setPositiveButton("Удалить",(d,w)->a.startActivity(new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+PACKAGE)))).show();return;
        }
        new RuneDialogBuilder(a,accent).setTitle("ДВИЖОК ЛОКАЛЬНОГО ИИ")
            .setMessage(sizeLabel(a)+" | отдельный подписанный компонент. Android попросит подтвердить установку.")
            .setNegativeButton("Отмена",null).setPositiveButton("Скачать / установить",(d,w)->download(a,accent,refresh)).show();
    }
    private static void download(Activity a,int accent,Runnable refresh){
        if(!running.compareAndSet(false,true))return;
        AtomicBoolean stop=new AtomicBoolean();
        TextView status=new TextView(a);status.setPadding(24,24,24,24);status.setText("Проверка движка…");status.setTextColor(0xffeeeeee);
        AlertDialog dialog=new RuneDialogBuilder(a,accent).setTitle("ЛОКАЛЬНЫЙ ИИ").setView(status)
            .setNegativeButton("Остановить",(d,w)->stop.set(true)).create();
        dialog.setOnCancelListener(d->stop.set(true));dialog.show();
        Context c=a.getApplicationContext();
        ResourceStore.IO.execute(()->{
            try{
                JSONObject j=metadata(c);File target=apk(c),part=new File(target.getPath()+".part");
                File dir=target.getParentFile();if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Нет хранилища");
                if(target.isFile()){try{verify(c,target,j);}catch(Exception e){target.delete();}}
                if(!target.isFile()){
                    ResourceStore.checkNetwork(c);
                    String hash=j.getString("sha256");SharedPreferences prefs=c.getSharedPreferences("skyrim_engine",0);
                    long size=j.getLong("size");if(!hash.equals(prefs.getString("partial",""))||part.length()>size)part.delete();
                    prefs.edit().putString("partial",hash).commit();
                    long offset=part.length();
                    if(offset<size){
                        HttpsURLConnection con=connect(BASE+"v"+BuildConfig.VERSION_NAME+"/SKYRIM-LOCAL-AI.apk",offset);
                        try{
                            int code=con.getResponseCode();
                            if(code==200)offset=0;
                            else if(code!=206||offset==0||!("bytes "+offset+"-"+(size-1)+"/"+size).equals(con.getHeaderField("Content-Range")))throw new IOException("Ошибка загрузки: HTTP "+code);
                            try(InputStream in=con.getInputStream();FileOutputStream out=new FileOutputStream(part,offset>0)){
                                long done=offset,last=0;byte[] buf=new byte[65536];int n;
                                while((n=in.read(buf))!=-1){
                                    if(stop.get())throw new InterruptedIOException("Остановлено");
                                    done+=n;if(done>size)throw new IOException("Лишние данные");out.write(buf,0,n);
                                    long now=SystemClock.elapsedRealtime();if(now-last>200){last=now;long count=done;ResourceStore.UI.post(()->status.setText(String.format(Locale.ROOT,"%.1f / %.1f МБ",count/1e6,size/1e6)));}
                                }
                                out.getFD().sync();if(done!=size)throw new IOException("Загрузка не завершена");
                            }
                        }finally{con.disconnect();}
                    }
                    if(stop.get())throw new InterruptedIOException("Остановлено");
                    try{verify(c,part,j);}catch(Exception e){part.delete();throw e;}
                    Files.move(part.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
                }
                ResourceStore.UI.post(()->{if(!a.isFinishing()&&!a.isDestroyed()&&!stop.get()){
                    dialog.dismiss();try{launch(a);}catch(Exception e){new RuneDialogBuilder(a,accent).setTitle("Установка движка").setMessage(e.getMessage()).setPositiveButton("Закрыть",null).show();}
                    refresh.run();
                }});
            }catch(Exception e){ResourceStore.UI.post(()->{if(!a.isFinishing()&&!a.isDestroyed()&&!stop.get())status.setText("Ошибка: "+e.getMessage());});}
            finally{running.set(false);}
        });
    }
}
