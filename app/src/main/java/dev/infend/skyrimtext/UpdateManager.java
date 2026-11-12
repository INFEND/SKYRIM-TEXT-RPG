package dev.infend.skyrimtext;

import android.app.Activity;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import javax.net.ssl.SSLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import android.os.Build;
import android.provider.Settings;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;

/** Secure user-initiated update checker for an HTTPS manifest and a single APK. */
public final class UpdateManager {
    private UpdateManager(){}
    /** This is the only official OTA channel. No editable source or alternate endpoint. */
    public static final String MANIFEST_URL=OtaUrlPolicy.MANIFEST;
    public static final String TRUSTED_CERT_SHA256="8A0D221B9C31007B55AC0BAED5597A0B638881F7FEF6650A6E7BD0E2B49793FE";
    private static final long MAX_APK=512L*1024L*1024L;
    private static final int MAX_MANIFEST_BYTES=4*1024*1024;
    private static final int MANIFEST_ATTEMPTS=3;
    private static final int APK_ATTEMPTS=3;
    public interface StatusReporter {void status(String text);}
    /** HTTP status remains machine-readable; do not decide retries by Russian text. */
    static final class HttpStatusException extends IOException {
        final int status;
        HttpStatusException(int code,String message){super(message);status=code;}
    }
    // Fixed update identity: downloaded APK must match this app and its pinned certificate.
    public static final String APK_FILE="skyrim_text_rpg_update.apk";
    public interface Callback {
        void status(String message);
        default void progress(long transferred,long total){}
        void release(Release info, boolean newer);
        void downloaded(File file);
        void error(String message);
    }
    public static final class Release {
        public final long versionCode;
        public final String versionName,apkUrl,sha256,notes,packageName;
        public final long sizeBytes;
        private final List<OtaHistory.Entry> history;
        public String notesFor(long installedVersionCode){
            return OtaHistory.merge(history,installedVersionCode,versionCode,notes);
        }
        Release(JSONObject j)throws Exception {
            if(j.has("schema") && j.getInt("schema")!=1)throw new IOException("Неподдерживаемый формат OTA");
            // Read the real application identity separately from a legacy metadata alias.
            // Older installed clients have an incorrect hard-coded value. Preserve compatibility
            // in update.json without ever accepting an APK with a different package or signer.
            String metadataId=j.optString("packageName","");
            packageName=j.optString("apkPackageName",metadataId);
            if(!BuildConfig.APPLICATION_ID.equals(packageName))
                throw new IOException("Обновление предназначено для другого приложения");
            boolean legacyMetadata=j.optBoolean("legacyPackageField",false)
                && "dev.xarkon.link".equals(metadataId);
            if(!BuildConfig.APPLICATION_ID.equals(metadataId)&&!legacyMetadata)
                throw new IOException("Ошибка идентификатора приложения");
            versionCode=j.getLong("versionCode");
            versionName=j.optString("versionName","Новая версия");
            apkUrl=j.getString("apkUrl");
            sha256=j.getString("sha256").toLowerCase(Locale.US);
            notes=j.optString("notes","");
            sizeBytes=j.optLong("sizeBytes",-1L);
            if(versionCode<1||!sha256.matches("[0-9a-f]{64}")||
                !versionName.matches("[A-Za-z0-9._-]{1,40}")||
                sizeBytes==0||sizeBytes < -1||sizeBytes>MAX_APK||notes.length()>100_000)
                throw new IOException("Недействительные метаданные OTA");
            if(!OtaUrlPolicy.trustedApk(apkUrl,versionName))
                throw new IOException("APK размещён вне официального канала");
            List<OtaHistory.Entry> entries=new ArrayList<>();
            JSONArray versions=j.optJSONArray("history");
            // Do not discard the full missed-release history after 500 entries.
            // Input bytes are bounded separately when the manifest is downloaded.
            if(versions!=null){
                for(int i=0;i<versions.length();i++){
                    JSONObject item=versions.optJSONObject(i);
                    if(item==null){entries.clear();break;}
                    entries.add(new OtaHistory.Entry(item.optLong("versionCode",0),
                        item.optString("versionName",""),item.optString("notes","")));
                }
            }
            history=entries;
        }
    }
    private static void checkHttps(String url)throws IOException{
        URL u=new URL(url);
        if(!"https".equalsIgnoreCase(u.getProtocol())||u.getHost()==null||u.getHost().isEmpty())
            throw new IOException("Нужна прямая HTTPS-ссылка");
    }
    public static File apkFile(Context c){return new File(new File(c.getFilesDir(),"ota"),APK_FILE);}
    private static void internetReady(Context c)throws IOException{
        ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network=cm==null?null:cm.getActiveNetwork();
        NetworkCapabilities caps=network==null?null:cm.getNetworkCapabilities(network);
        if(caps==null||!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            throw new IOException("Нет подключения к интернету. Включите Wi-Fi или мобильные данные.");
        // Do not DNS-probe github.com here: a VPN, DNS filter or temporary
        // GitHub outage must not block the trusted raw.githubusercontent.com mirror.
        // The HTTPS request below provides the authoritative connectivity result.
    }
    private static String friendly(Exception e){
        if(e instanceof UnknownHostException)return "Не удаётся определить адрес сервера обновлений. Проверьте интернет, DNS и VPN.";
        if(e instanceof SocketTimeoutException)return "Сервер обновлений не ответил вовремя. Попробуйте ещё раз.";
        if(e instanceof SSLException)return "Ошибка защищённого соединения. Проверьте дату, время и настройки VPN.";
        if(e instanceof ConnectException)return "Не удалось подключиться к серверу обновлений.";
        String s=e.getMessage();
        return s==null||s.trim().isEmpty()?"Ошибка обновления":s;
    }
    private static void requireOk(HttpsURLConnection c,String purpose)throws IOException{
        int code=c.getResponseCode();
        if(code==200)return;
        if(code==404)throw new HttpStatusException(code,"Файл обновления не найден на сервере (HTTP 404).");
        if(code==403||code==429)throw new HttpStatusException(code,"Сервер временно ограничил загрузку (HTTP "+code+"). Попробуйте позже.");
        if(code>=500)throw new HttpStatusException(code,"Сервер обновлений временно недоступен (HTTP "+code+").");
        throw new HttpStatusException(code,purpose+": сервер ответил HTTP "+code);
    }
    private static HttpsURLConnection connection(String url)throws IOException{
        checkHttps(url);
        URLConnection c=new URL(url).openConnection();
        if(!(c instanceof HttpsURLConnection))throw new IOException("Не HTTPS");
        HttpsURLConnection h=(HttpsURLConnection)c;
        h.setConnectTimeout(12000);h.setReadTimeout(25000);
        h.setRequestProperty("User-Agent","SKYRIM-TEXT-RPG-Updater/1.0");
        return h;
    }
    /** Retry only transient failures; never retry invalid metadata, signatures or TLS errors. */
    private static boolean transientNetworkFailure(Exception e){
        if(e instanceof HttpStatusException){
            int status=((HttpStatusException)e).status;
            return status==408||status==429||status>=500;
        }
        return e instanceof SocketTimeoutException||e instanceof ConnectException
            ||e instanceof java.net.SocketException||e instanceof UnknownHostException;
    }
    private static void retryPause(int attempt)throws IOException{
        try{Thread.sleep(350L*attempt);}
        catch(InterruptedException interrupted){
            Thread.currentThread().interrupt();
            throw new IOException("Проверка обновлений прервана.",interrupted);
        }
    }
    private static void report(StatusReporter reporter,String message){
        if(reporter!=null)reporter.status(message);
    }
    private static HttpsURLConnection openApk(String url,String version)throws IOException{
        IOException last=null;
        for(int attempt=1;attempt<=APK_ATTEMPTS;attempt++){
            HttpsURLConnection connection=null;
            try{
                connection=connection(url);
                requireOk(connection,"Загрузка APK");
                return connection;
            }catch(IOException unavailable){
                if(connection!=null)connection.disconnect();
                last=unavailable;
                if(!transientNetworkFailure(unavailable)||attempt>=APK_ATTEMPTS)break;
                retryPause(attempt);
            }
        }
        throw last==null?new IOException("Не удалось подключиться к серверу APK."):last;
    }

    /**
     * Official latest redirect first, then pinned release URL via GitHub API,
     * then a verified raw mirror containing the SAME published update.json.
     * Only fixed repository endpoints are allowed; Release still pins package,
     * APK URL, expected APK SHA-256 and installed signing certificate.
     */
    public static Release fetchLatest(Context context)throws Exception{
        return fetchLatest(context,null);
    }
    public static Release fetchLatest(Context context,StatusReporter reporter)throws Exception{
        internetReady(context);
        Exception primaryFailure;
        try{return readManifestRetried(MANIFEST_URL,reporter);}
        catch(Exception e){
            if(!transientNetworkFailure(e)&&!(e instanceof HttpStatusException))throw e;
            primaryFailure=e;
        }

        String expectedTag=null;
        Exception pinnedFailure=null;
        try{
            report(reporter,"Основной сервер недоступен. Проверяем опубликованную версию...");
            expectedTag=readLatestPublishedTag();
            String pinned=OtaUrlPolicy.manifestForTag(expectedTag);
            if(pinned==null)throw new IOException("Некорректный номер официального выпуска.");
            report(reporter,"Переключение на прямую ссылку версии "+expectedTag.substring(1)+"...");
            Release fromTag=readManifestRetried(pinned,reporter);
            if(!expectedTag.equals("v"+fromTag.versionName))
                throw new IOException("Версия официального манифеста не совпадает с опубликованной.");
            return fromTag;
        }catch(Exception e){pinnedFailure=e;}
        try{
            report(reporter,"Проверяем резервный канал обновлений...");
            Release mirror=readManifestRetried(OtaUrlPolicy.MANIFEST_MIRROR,reporter);
            if(expectedTag!=null&&!expectedTag.equals("v"+mirror.versionName))
                throw new IOException("Резервный канал ещё не получил последнюю версию "+expectedTag.substring(1)+".");
            return mirror;
        }catch(Exception backupError){
            backupError.addSuppressed(primaryFailure);
            if(pinnedFailure!=null)backupError.addSuppressed(pinnedFailure);
            throw backupError;
        }
    }
    private static Release readManifestRetried(String url,StatusReporter reporter)throws Exception{
        for(int attempt=1;attempt<=MANIFEST_ATTEMPTS;attempt++){
            try{return readManifest(url);}
            catch(IOException problem){
                if(!transientNetworkFailure(problem)||attempt==MANIFEST_ATTEMPTS)throw problem;
                report(reporter,"Повтор подключения к серверу: "+(attempt+1)+"/"+MANIFEST_ATTEMPTS);
                retryPause(attempt);
            }
        }
        throw new IOException("Сервер обновлений не ответил.");
    }
    private static String readLatestPublishedTag()throws Exception{
        HttpsURLConnection connection=connection(OtaUrlPolicy.RELEASE_API);
        connection.setRequestProperty("Accept","application/vnd.github+json");
        try{
            requireOk(connection,"Сведения о выпуске");
            JSONObject latest=new JSONObject(readBounded(connection,MAX_MANIFEST_BYTES));
            if(latest.optBoolean("draft",true)||latest.optBoolean("prerelease",true))
                throw new IOException("Версия ещё не опубликована.");
            String tag=latest.optString("tag_name","");
            String pinned=OtaUrlPolicy.manifestForTag(tag);
            if(pinned==null)throw new IOException("Неподдерживаемое имя версии.");
            JSONArray assets=latest.optJSONArray("assets");
            boolean exists=false;
            if(assets!=null)for(int i=0;i<assets.length();i++){
                JSONObject a=assets.optJSONObject(i);
                if(a!=null&&"update.json".equals(a.optString("name"))
                    &&pinned.equals(a.optString("browser_download_url")))exists=true;
            }
            if(!exists)throw new IOException("Выпуск ещё не содержит OTA-манифест.");
            return tag;
        }catch(org.json.JSONException invalid){
            throw new IOException("Некорректные сведения о выпуске.",invalid);
        }finally{connection.disconnect();}
    }
    private static String readBounded(HttpsURLConnection connection,int limit)throws IOException{
        ByteArrayOutputStream memory=new ByteArrayOutputStream();
        try(InputStream in=connection.getInputStream()){
            byte[] data=new byte[8192];int count;
            while((count=in.read(data))!=-1){
                if(memory.size()+count>limit)
                    throw new IOException("Ответ сервера обновлений слишком большой.");
                memory.write(data,0,count);
            }
        }
        return memory.toString("UTF-8");
    }
    private static Release readManifest(String url)throws Exception{
        HttpsURLConnection c=connection(url);
        try{
            requireOk(c,"Проверка обновления");
            try{return new Release(new JSONObject(readBounded(c,MAX_MANIFEST_BYTES)));}
            catch(org.json.JSONException bad){
                throw new IOException("Некорректные данные обновления.",bad);
            }
        }finally{c.disconnect();}
    }
    public static void check(Activity activity,String manifestUrl,Callback cb){
        new Thread(()->{
            try{
                if(!MANIFEST_URL.equals(manifestUrl))throw new IOException("Недоверенный источник обновления.");
                activity.runOnUiThread(()->cb.status("Проверка соединения..."));
                Release info=fetchLatest(activity.getApplicationContext(),
                    message->activity.runOnUiThread(()->cb.status(message)));
                long installed=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).getLongVersionCode();
                activity.runOnUiThread(()->cb.release(info,info.versionCode>installed));
            }catch(Exception e){
                String error=friendly(e);
                activity.runOnUiThread(()->cb.error(error));
            }
        },"skyrim-text-rpg-update-check").start();
    }
    public static void download(Activity activity,Release r,Callback cb){
        new Thread(()->{
            File folder=new File(activity.getFilesDir(),"ota");
            File temp=new File(folder,APK_FILE+".part");
            File apk=new File(folder,APK_FILE);
            try{
                internetReady(activity);
                if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Не удалось создать папку обновлений во внутренней памяти.");
                if(r.sizeBytes>MAX_APK)throw new IOException("Размер обновления превышает 512 МБ.");
                if(r.sizeBytes>0&&folder.getUsableSpace()<r.sizeBytes+4L*1024*1024)
                    throw new IOException("Недостаточно свободного места для обновления. Освободите память устройства.");
                HttpsURLConnection c=openApk(r.apkUrl,r.versionName);
                long received=0L;
                long total;
                MessageDigest digest=MessageDigest.getInstance("SHA-256");
                try{
                    long serverLength=c.getContentLengthLong();
                    if(serverLength>MAX_APK)throw new IOException("Слишком большой APK.");
                    if(r.sizeBytes>0&&serverLength>0&&serverLength!=r.sizeBytes)
                        throw new IOException("Сервер вернул файл другого размера.");
                    total=r.sizeBytes>0?r.sizeBytes:serverLength;
                    final long visibleTotal=total;
                    activity.runOnUiThread(()->cb.progress(0L,visibleTotal));
                    try(InputStream in=new BufferedInputStream(c.getInputStream(),128*1024);
                        FileOutputStream out=new FileOutputStream(temp,false)){
                        byte[] buf=new byte[128*1024];
                        int n;long lastReport=0L;
                        while((n=in.read(buf))!=-1){
                            received+=n;
                            if(received>MAX_APK)throw new IOException("Превышен допустимый размер обновления.");
                            out.write(buf,0,n);digest.update(buf,0,n);
                            long now=System.currentTimeMillis();
                            if(now-lastReport>120L){
                                lastReport=now;
                                final long current=received;
                                activity.runOnUiThread(()->cb.progress(current,visibleTotal));
                            }
                        }
                        out.getFD().sync();
                    }catch(FileNotFoundException e){
                        throw new IOException("Не удалось создать файл обновления. Проверьте свободное место.",e);
                    }
                }finally{c.disconnect();}
                if(r.sizeBytes>0&&received!=r.sizeBytes)
                    throw new IOException("Файл загрузился не полностью. Повторите загрузку.");
                final long complete=received;
                final long full=r.sizeBytes>0?r.sizeBytes:received;
                activity.runOnUiThread(()->{
                    cb.progress(complete,full);
                    cb.status("Проверяем целостность и подпись…");
                });
                StringBuilder hash=new StringBuilder(64);
                for(byte b:digest.digest())hash.append(String.format(Locale.US,"%02x",b&255));
                if(!hash.toString().equals(r.sha256))throw new IOException("Контрольная сумма SHA-256 не совпала. Файл отклонён.");
                try{Files.move(temp.toPath(),apk.toPath(),StandardCopyOption.REPLACE_EXISTING);}
                catch(IOException move){throw new IOException("Не удалось сохранить APK во внутренней памяти. Проверьте свободное место.",move);}
                try{verifyApk(activity,apk,r.versionCode);}
                catch(Exception invalid){apk.delete();throw invalid;}
                activity.runOnUiThread(()->cb.downloaded(apk));
            }catch(Exception e){
                temp.delete();
                final String error=friendly(e);
                activity.runOnUiThread(()->cb.error(error));
            }
        },"skyrim-text-rpg-update-download").start();
    }
    /** An archive is accepted only if Android will recognize it as THIS installed app. */
    public static void verifyApk(Context c,File file,long expectedCode)throws Exception{
        PackageManager pm=c.getPackageManager();
        PackageInfo archive=pm.getPackageArchiveInfo(file.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
        if(archive==null)throw new IOException("Не удалось прочитать пакет APK");
        if(!c.getPackageName().equals(archive.packageName))throw new IOException("APK принадлежит другому приложению");
        if(archive.getLongVersionCode()!=expectedCode)throw new IOException("Версия APK не соответствует манифесту");
        long current=pm.getPackageInfo(c.getPackageName(),0).getLongVersionCode();
        if(archive.getLongVersionCode()<=current)throw new IOException("Версия уже установлена или устарела");
        PackageInfo installed=pm.getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
        if(installed.signingInfo==null||archive.signingInfo==null)throw new IOException("Не удалось проверить подпись APK");
        Signature[] oldSigners=installed.signingInfo.getApkContentsSigners();
        Signature[] newSigners=archive.signingInfo.getApkContentsSigners();
        if(oldSigners==null||newSigners==null||oldSigners.length!=1||newSigners.length!=1)
            throw new IOException("Неподдерживаемое количество сертификатов подписи");
        if(!TRUSTED_CERT_SHA256.equals(sha256OfSignature(oldSigners[0]))
            || !TRUSTED_CERT_SHA256.equals(sha256OfSignature(newSigners[0])))
            throw new IOException("Неверная подпись APK. Установите официальную версию SKYRIM TEXT RPG");
        if(!Arrays.equals(oldSigners,newSigners))
            throw new IOException("Подписи установленного приложения и обновления различаются");
    }
    private static String sha256OfSignature(Signature signer)throws Exception{
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(signer.toByteArray());
        StringBuilder hex=new StringBuilder(64);
        for(byte b:digest)hex.append(String.format(Locale.US,"%02X",b&255));
        return hex.toString();
    }
    public static void install(Activity a){
        try{
            File apk=apkFile(a);
            if(!apk.isFile())throw new IOException("Сначала загрузите обновление");
            PackageManager pm=a.getPackageManager();
            if(Build.VERSION.SDK_INT>=26&&!pm.canRequestPackageInstalls()){
                Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:"+a.getPackageName()));
                a.getSharedPreferences("skyrim_text_v1",0).edit().putBoolean("ota_install_pending",true).apply();
                OtaProgressDialog notice=new OtaProgressDialog(a);
                notice.show();
                notice.stage("Разрешение на установку","Разрешите установку обновлений для SKYRIM TEXT RPG в настройках Android.");
                a.getWindow().getDecorView().postDelayed(()->{
                    if(a.isFinishing()||a.isDestroyed())return;
                    try{a.startActivity(settings);notice.dismiss();}
                    catch(Exception error){notice.fail(friendly(error),null);}
                },500L);
                return;
            }
            // Both APK identity and signing certificate are checked again before installation.
            PackageInfo archive=pm.getPackageArchiveInfo(apk.getAbsolutePath(),0);
            if(archive==null)throw new IOException("Не удалось открыть APK");
            verifyApk(a,apk,archive.getLongVersionCode());
            OtaProgressDialog migration=new OtaProgressDialog(a);migration.show();
            migration.stage("Сохранение ресурсов","Подготовка локальных файлов перед обновлением…");
            ResourceStore.IO.execute(()->{
                try{
                    ResourceStore.migrate(a,(label,done,total)->a.runOnUiThread(()->{
                        if(!a.isFinishing()&&!a.isDestroyed())migration.stage("Сохранение ресурсов",done+" / "+total+" | "+label);
                    }));
                    a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;migration.dismiss();launchVerifiedInstaller(a);});
                }catch(Exception e){a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())migration.fail("Не удалось сохранить ресурсы: "+friendly(e),()->install(a));});}
            });
        }catch(Exception e){
            OtaProgressDialog screen=new OtaProgressDialog(a);
            screen.show();screen.fail(friendly(e),null);
        }
    }
    private static void launchVerifiedInstaller(Activity a){
        try{
            Uri content=Uri.parse("content://"+a.getPackageName()+".updates/apk");
            // A self-update kills the old SKYRIM TEXT RPG process. Launching the
            // system installer as a child of our Activity can leave its
            // post-install "Open" action attached to the dying task.
            // Start the installer from the application context with NEW_TASK
            // so Android keeps the installer and SKYRIM TEXT RPG in separate tasks.
            Intent view=new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(content,"application/vnd.android.package-archive");
            view.setClipData(ClipData.newRawUri("SKYRIM TEXT RPG",content));
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_NEW_TASK);
            a.getApplicationContext().startActivity(view);
        }catch(Exception e){
            OtaProgressDialog screen=new OtaProgressDialog(a);
            screen.show();screen.fail(friendly(e),null);
        }
    }
}
