package dev.infend.skyrimtext;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.Gravity;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/** Checks public GitHub Releases, verifies SHA-256, installs through Android Package Installer. */
public final class UpdateManager {
    private static final String MANIFEST_URL="https://github.com/INFEND/SKYRIM-TEXT-RPG/releases/latest/download/update.json";
    private static final String ALLOWED_APK_PREFIX="https://github.com/INFEND/SKYRIM-TEXT-RPG/releases/download/";
    private final MainActivity activity;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private volatile boolean busy=false;
    private boolean installPending=false;
    UpdateManager(MainActivity activity){this.activity=activity;}

    public void check(boolean manual){
        if(busy){if(manual)activity.toast("Проверка уже выполняется");return;}
        busy=true;if(manual)activity.toast("Проверка обновлений…");
        new Thread(()->{
            try{
                JSONObject info=new JSONObject(readString(MANIFEST_URL));
                int code=info.getInt("versionCode");String version=info.getString("versionName");
                String apk=info.getString("apkUrl");String sha=info.getString("sha256").toLowerCase(Locale.ROOT);
                if(!apk.startsWith(ALLOWED_APK_PREFIX)||!sha.matches("[0-9a-f]{64}"))throw new Exception("Некорректный манифест обновления");
                StringBuilder notes=new StringBuilder();JSONArray arr=info.optJSONArray("changelog");
                if(arr!=null)for(int i=0;i<arr.length();i++)notes.append("• ").append(arr.optString(i)).append("\n");
                ui.post(()->{
                    busy=false;
                    if(code>BuildConfig.VERSION_CODE)offer(version,apk,sha,notes.toString());
                    else if(manual)activity.toast("Установлена актуальная версия: "+BuildConfig.VERSION_NAME);
                });
            }catch(Exception e){ui.post(()->{busy=false;if(manual)activity.toast("Нет доступного релиза или ошибка сети");});}
        },"skyrim-ota-check").start();
    }
    private String readString(String link)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(link).openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(16000);c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent","SKYRIM-TEXT-RPG/"+BuildConfig.VERSION_NAME);
        try{
            if(c.getResponseCode()!=200)throw new Exception("HTTP "+c.getResponseCode());
            if(c.getContentLengthLong()>128000)throw new Exception("Manifest too large");
            try(InputStream in=c.getInputStream()){
                byte[] bytes=readLimited(in,128000);return new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
            }
        }finally{c.disconnect();}
    }
    private byte[] readLimited(InputStream in,int max)throws Exception{
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
        while((n=in.read(buf))!=-1){if(out.size()+n>max)throw new Exception("Too large");out.write(buf,0,n);}return out.toByteArray();
    }
    private void offer(String version,String apk,String sha,String notes){
        String msg="Новая версия: "+version+"\nТекущая: "+BuildConfig.VERSION_NAME+"\n\n"+(notes.isEmpty()?"Исправления и улучшения.":notes);
        new AlertDialog.Builder(activity).setTitle("Обновление SKYRIM TEXT RPG")
            .setMessage(msg).setNegativeButton("Позже",null)
            .setPositiveButton("Скачать и установить",(d,w)->download(apk,sha)).show();
    }
    private void download(String url,String expectedHash){
        if(busy)return;busy=true;
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(24*activity.getResources().getDisplayMetrics().density);panel.setPadding(pad,pad,pad,pad);
        TextView label=new TextView(activity);label.setText("Загрузка обновления… 0%");label.setTextSize(15);
        label.setTextColor(Color.WHITE);panel.addView(label);
        ProgressBar progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);progress.setProgressTintList(ColorStateList.valueOf(activity.currentAccent()));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,pad/2);lp.topMargin=pad/2;panel.addView(progress,lp);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Обновление приложения")
            .setView(panel).setCancelable(false).create();dialog.show();
        new Thread(()->{
            HttpURLConnection c=null;File temp=new File(activity.getCacheDir(),"update.part");
            try{
                URL source=new URL(url);if(!url.startsWith(ALLOWED_APK_PREFIX))throw new Exception("Invalid update URL");
                c=(HttpURLConnection)source.openConnection();c.setConnectTimeout(18000);c.setReadTimeout(25000);
                c.setRequestProperty("User-Agent","SKYRIM-TEXT-RPG/OTA");
                if(c.getResponseCode()!=200)throw new Exception("HTTP "+c.getResponseCode());
                long size=c.getContentLengthLong();if(size>200L*1024*1024)throw new Exception("APK too large");
                MessageDigest hash=MessageDigest.getInstance("SHA-256");
                try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                    byte[] buffer=new byte[16384];int count;long downloaded=0;int last=-1;
                    while((count=in.read(buffer))!=-1){downloaded+=count;
                        if(downloaded>200L*1024*1024)throw new Exception("APK size limit");
                        out.write(buffer,0,count);hash.update(buffer,0,count);
                        int percent=size>0?(int)Math.min(100,downloaded*100/size):0;
                        if(percent!=last){last=percent;final int p=percent;
                            ui.post(()->{progress.setProgress(p);label.setText("Загрузка обновления… "+p+"%");});}
                    }
                }
                byte[] digest=hash.digest();StringBuilder sb=new StringBuilder();for(byte b:digest)sb.append(String.format(Locale.ROOT,"%02x",b&0xff));
                if(!expectedHash.equals(sb.toString()))throw new Exception("SHA-256 mismatch");
                File target=new File(activity.getCacheDir(),"update.apk");
                if(target.exists()&&!target.delete())throw new Exception("Cannot replace old update");
                if(!temp.renameTo(target))throw new Exception("Cannot move verified APK");
                ui.post(()->{busy=false;dialog.dismiss();installVerifiedApk();});
            }catch(Exception error){temp.delete();ui.post(()->{
                busy=false;dialog.dismiss();new AlertDialog.Builder(activity)
                    .setTitle("Ошибка обновления").setMessage("APK не установлен: "+error.getMessage())
                    .setPositiveButton("Закрыть",null).show();});
            }finally{if(c!=null)c.disconnect();}
        },"skyrim-ota-download").start();
    }
    private void installVerifiedApk(){
        File apk=new File(activity.getCacheDir(),"update.apk");if(!apk.isFile()){activity.toast("Файл обновления отсутствует");return;}
        if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
            installPending=true;
            new AlertDialog.Builder(activity).setTitle("Разрешение на установку")
                .setMessage("Android должен разрешить этому приложению устанавливать загруженные обновления. После разрешения установка продолжится автоматически.")
                .setNegativeButton("Позже",(d,w)->installPending=false)
                .setPositiveButton("Разрешить",(d,w)->{
                    Intent i=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName()));
                    try{activity.startActivity(i);}catch(Exception ex){activity.toast("Открой разрешение установки APK в настройках Android");}
                }).show();return;
        }
        installPending=false;
        Uri uri=Uri.parse("content://"+activity.getPackageName()+".ota/update.apk");
        Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(uri,"application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setClipData(android.content.ClipData.newUri(activity.getContentResolver(),"SKYRIM Update",uri));
        try{activity.startActivity(i);}catch(Exception e){activity.toast("Системный установщик Android недоступен");}
    }
    public void onHostResume(){if(installPending&&activity.getPackageManager().canRequestPackageInstalls())installVerifiedApk();}
}
