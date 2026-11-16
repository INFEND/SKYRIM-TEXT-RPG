package dev.infend.skyrimtext;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;

/** Signed APK catalog, persistent optional assets, and resumable verified transfers. */
final class ResourceStore {
    static final String MANIFEST="resource_manifest.json";
    private static final String PREF="skyrim_resource_store_v1";
    private static final String BASE="https://raw.githubusercontent.com/INFEND/SKYRIM-TEXT-RPG/";
    static final ExecutorService IO=Executors.newSingleThreadExecutor();
    static final Handler UI=new Handler(Looper.getMainLooper());
    private static final AtomicBoolean STARTED=new AtomicBoolean();
    private static volatile int generation;
    private static volatile Catalog catalog;
    private static final Set<String> AUTO_GALLERY=Collections.synchronizedSet(new HashSet<>());
    static final class Entry {
        final String path,group,label,sha,url; final long size;
        Entry(JSONObject j,String commit)throws Exception{
            path=j.getString("path");group=j.getString("group");label=j.getString("label");
            sha=j.getString("sha256");size=j.getLong("size");
            if(!path.matches("(gallery/[a-z0-9_-]+\\.zip|maps/[a-z0-9_-]+\\.webp|icons/skyrim_icons_358_exact\\.(webp|json)|portraits/[a-z]+_(male|female)_[1-3]\\.webp|fonts/choices/[a-z0-9_-]+/(regular|bold|italic|bolditalic)\\.ttf|fonts/choices/[a-z0-9_-]+/OFL\\.txt|media/[a-z0-9_-]+/[a-z0-9_-]+\\.(webp|png|jpg|jpeg|zip|ogg|mp3|mp4|json))")
                ||!sha.matches("[0-9a-f]{64}")||size<=0||size>100_000_000L)
                throw new IOException("Некорректный каталог ресурсов");
            if(path.startsWith("fonts/choices/")){
                String source=j.getString("url");
                if(!source.matches("https://raw\\.githubusercontent\\.com/google/fonts/[0-9a-f]{40}/ofl/[A-Za-z0-9_./%+,-]+")
                    ||source.contains(".."))throw new IOException("Недопустимый источник шрифта");
                url=source;
            }else url=BASE+commit+"/app/src/main/assets/"+path;
        }
    }
    static final class Catalog {
        final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();
        final boolean bundled;
        final String commit;
        final long revision;
        final JSONArray galleries;
        Catalog(Context c)throws Exception{this(readEmbedded(c));}
        private static JSONObject readEmbedded(Context c)throws Exception{
            try(InputStream in=c.getAssets().open(MANIFEST)){
                return new JSONObject(new String(SignedResourceCatalog.read(in,2_000_000),java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        Catalog(JSONObject j)throws Exception{
            commit=j.getString("commit");revision=j.optLong("revision",0L);
            if(j.getInt("schema")!=1||!commit.matches("[0-9a-f]{40}")||revision<0)throw new IOException("Неверная версия каталога");
            bundled=j.getBoolean("bundled");
            galleries=j.optJSONArray("galleries");
            JSONArray list=j.getJSONArray("files");
            if(list.length()>10000)throw new IOException("Каталог слишком велик");
            for(int i=0;i<list.length();i++){Entry e=new Entry(list.getJSONObject(i),commit);if(entries.put(e.path,e)!=null)throw new IOException("Дублирование ресурса");}
            if(entries.isEmpty())throw new IOException("Пустой каталог");
            if(galleries!=null)LocationGalleryView.validateCatalog(galleries,entries);
        }
    }
    private static synchronized Catalog catalog(Context c)throws Exception{
        if(catalog==null){
            Catalog embedded=new Catalog(c.getApplicationContext());
            catalog=embedded;
            if(!embedded.bundled)try{
                Catalog cached=SignedResourceCatalog.cached(c);
                if(cached!=null&&!cached.bundled&&cached.revision>=embedded.revision)catalog=cached;
            }catch(Exception ignored){/* signed APK remains the fallback */}
            LocationGalleryView.applyCatalog(catalog.galleries);
        }
        return catalog;
    }
    // Only invoked on the serial IO executor. Verify before publishing or replacing
    // any file; offline clients retain the last verified catalog.
    static boolean refreshRemote(Context c)throws Exception{
        Catalog before=catalog(c);
        if(before.bundled)return false;
        byte[] envelope=SignedResourceCatalog.fetch(c);
        Catalog remote=SignedResourceCatalog.verify(c,envelope);
        if(remote.bundled||remote.revision<=0||remote.galleries==null)throw new IOException("Неполный внешний каталог");
        if(remote.revision<before.revision)return false;
        if(remote.revision==before.revision&&!remote.commit.equals(before.commit))throw new IOException("Конфликт ревизий каталога");
        if(remote.commit.equals(before.commit))return false;
        SignedResourceCatalog.save(c,envelope);
        synchronized(ResourceStore.class){catalog=remote;LocationGalleryView.applyCatalog(remote.galleries);changed();}
        return true;
    }
    static String automaticError(Context c){return prefs(c).getString("auto_error","");}
    static String catalogError(Context c){return prefs(c).getString("catalog_error","");}
    private static void recordError(Context c,String key,String message){prefs(c).edit().putString(key,message).apply();}
    static boolean refreshCatalog(Context c){
        try{boolean changed=refreshRemote(c);recordError(c,"catalog_error","");return changed;}
        catch(Exception ex){recordError(c,"catalog_error","Каталог: "+ex.getMessage());return false;}
    }
    static List<Entry> entries(Context c)throws Exception{return new ArrayList<>(catalog(c).entries.values());}
    static int generation(){return generation;}
    static String portraitGroup(String path){
        String name=path.substring(path.lastIndexOf('/')+1);
        int split=name.indexOf('_');
        return split>0?"portraits:"+name.substring(0,split):"";
    }
    static boolean groupMissing(Context c,String group){
        try{for(Entry e:entries(c))if(group.equals(e.group)&&!has(c,e.path))return true;}
        catch(Exception ignored){}
        return false;
    }
    private static void changed(){generation++;}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    static boolean wifiOnly(Context c){return prefs(c).getBoolean("wifi_only",false);}
    static void wifiOnly(Context c,boolean value){prefs(c).edit().putBoolean("wifi_only",value).apply();}
    static boolean autoGallery(Context c){return prefs(c).getBoolean("auto_gallery",false);}
    static void autoGallery(Context c,boolean value){prefs(c).edit().putBoolean("auto_gallery",value).apply();}
    static boolean autoUpdate(Context c){return prefs(c).getBoolean("auto_update",false);}
    static void autoUpdate(Context c,boolean value){prefs(c).edit().putBoolean("auto_update",value).apply();}
    static boolean autoNew(Context c){return prefs(c).getBoolean("auto_new",autoUpdate(c));}
    static void autoNew(Context c,boolean value){prefs(c).edit().putBoolean("auto_new",value).apply();}
    static boolean automaticAllowed(Context c,String path){
        try{Entry e=catalog(c).entries.get(path);return e!=null&&!removed(c,e);}
        catch(Exception ignored){return false;}
    }
    static void requestGallery(Context c,String path,Runnable done){
        Context app=c.getApplicationContext();
        if(!autoGallery(app)||!automaticAllowed(app,path)||has(app,path)||!AUTO_GALLERY.add(path))return;
        IO.execute(()->{
            try{Entry e=catalog(app).entries.get(path);
                if(e!=null&&!removed(app,e))download(app,e,false,new AtomicBoolean(false),null);
            }catch(Exception ex){recordError(app,"auto_error",path+": "+ex.getMessage());UI.post(()->android.widget.Toast.makeText(app,"Не удалось загрузить фото. Подробнее в ресурсах.",android.widget.Toast.LENGTH_LONG).show());}finally{AUTO_GALLERY.remove(path);UI.post(done);}
        });
    }
    private static File file(Context c,Entry e){return new File(new File(c.getFilesDir(),"resource_store/v1"),e.path);}
    private static File part(Context c,Entry e){return new File(file(c,e).getPath()+".part");}
    private static boolean removed(Context c,Entry e){return prefs(c).getBoolean("removed:"+e.path,false);}
    private static boolean local(Context c,Entry e){
        File f=file(c,e);
        return f.isFile()&&f.length()==e.size&&e.sha.equals(prefs(c).getString("sha:"+e.path,""));
    }
    static boolean has(Context c,String path){
        try{Catalog a=catalog(c);Entry e=a.entries.get(path);
            return e!=null&&!removed(c,e)&&(local(c,e)||a.bundled);
        }catch(Exception ex){try(InputStream in=c.getAssets().open(path)){return true;}catch(IOException ignored){return false;}}
    }
    static File localFile(Context c,String path){
        try{Entry e=catalog(c).entries.get(path);return e!=null&&!removed(c,e)&&local(c,e)?file(c,e):null;}
        catch(Exception ignored){return null;}
    }
    static InputStream open(Context c,String path)throws IOException{
        Catalog a;try{a=catalog(c);}catch(Exception ex){return c.getAssets().open(path);}
        Entry e=a.entries.get(path);
        if(e==null)return c.getAssets().open(path);
        if(removed(c,e))throw new FileNotFoundException("Ресурс удалён");
        if(local(c,e))return new FileInputStream(file(c,e));
        if(a.bundled)return c.getAssets().open(path);
        throw new FileNotFoundException("Ресурс не загружен");
    }
    enum State { READY, BUNDLED, MISSING, NEW, DAMAGED, OLD }
    private static String digest(InputStream in)throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] buf=new byte[65536];int n;
        while((n=in.read(buf))!=-1)md.update(buf,0,n);
        StringBuilder out=new StringBuilder(64);for(byte x:md.digest())out.append(String.format(Locale.ROOT,"%02x",x&255));
        return out.toString();
    }
    private static boolean valid(File f,Entry e)throws Exception{
        if(!f.isFile()||f.length()!=e.size)return false;
        try(InputStream in=new FileInputStream(f)){return e.sha.equals(digest(in));}
    }
    static State state(Context c,Entry e,boolean verify)throws Exception{
        if(removed(c,e))return State.MISSING;
        File f=file(c,e);
        if(f.isFile()){
            if(verify){
                if(valid(f,e)){prefs(c).edit().putString("sha:"+e.path,e.sha).apply();return State.READY;}
                String previous=prefs(c).getString("sha:"+e.path,"");
                if(e.sha.equals(previous)){prefs(c).edit().remove("sha:"+e.path).apply();changed();}
                return !previous.isEmpty()&&!previous.equals(e.sha)?State.OLD:State.DAMAGED;
            }
            if(local(c,e))return State.READY;
            String previous=prefs(c).getString("sha:"+e.path,"");
            return !previous.isEmpty()&&!previous.equals(e.sha)?State.OLD:State.DAMAGED;
        }
        if(catalog(c).bundled){
            if(!verify)return State.BUNDLED;
            try(InputStream in=c.getAssets().open(e.path)){return e.sha.equals(digest(in))?State.BUNDLED:State.DAMAGED;}
            catch(IOException ex){return State.MISSING;}
        }
        return prefs(c).getString("sha:"+e.path,"").isEmpty()?State.NEW:State.MISSING;
    }
    static long storedBytes(Context c,Entry e){return file(c,e).length()+part(c,e).length();}
    static long partialBytes(Context c,Entry e){return part(c,e).length();}
    // Only partial bytes belonging to this exact catalog revision count toward progress.
    static long currentPartialBytes(Context c,Entry e){
        File f=part(c,e);
        return e.sha.equals(prefs(c).getString("partial:"+e.path,""))&&f.length()<=e.size?f.length():0;
    }
    interface Progress {void update(String label,long done,long total);}
    private static void publish(Context c,Entry e,File temp,boolean notify)throws Exception{
        if(!valid(temp,e)){temp.delete();throw new IOException("Контрольная сумма не совпадает: "+e.label);}
        File target=file(c,e);
        Files.move(temp.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        if(!prefs(c).edit().putString("sha:"+e.path,e.sha).remove("removed:"+e.path).remove("partial:"+e.path).commit())
            throw new IOException("Не удалось сохранить состояние ресурса");
        if(notify)changed();
    }
    private static void parent(File f)throws IOException{
        File p=f.getParentFile();if(!p.isDirectory()&&!p.mkdirs())throw new IOException("Не удалось создать хранилище");
    }
    private static void copyBundled(Context c,Entry e,AtomicBoolean stop,boolean notify)throws Exception{
        File tmp=new File(file(c,e).getPath()+".seed");parent(tmp);
        try{
            try(InputStream in=c.getAssets().open(e.path);FileOutputStream out=new FileOutputStream(tmp)){
                byte[] buf=new byte[65536];int n;long bytes=0;
                while((n=in.read(buf))!=-1){if(stop.get())throw new InterruptedIOException("Остановлено");bytes+=n;if(bytes>e.size)throw new IOException("Лишние данные");out.write(buf,0,n);}
                out.getFD().sync();
            }
            publish(c,e,tmp,notify);
            part(c,e).delete();
        }finally{tmp.delete();}
    }
    /** Bridge preserves old embedded assets before the installer replaces the APK. */
    static void migrate(Context c,Progress progress)throws Exception{
        Catalog a=catalog(c);if(!a.bundled)return;
        List<Entry> entries=new ArrayList<>(a.entries.values());int done=0;
        for(Entry e:entries){
            if(!removed(c,e)){
                if(!local(c,e)||!valid(file(c,e),e))
                    copyBundled(c,e,new AtomicBoolean(false),false);
            }
            if(progress!=null)progress.update(e.label,++done,entries.size());
        }
    }
    /** Called on an OTA worker before selecting a thin package. The same serial
     * executor owns migration, deletion and downloads so the check cannot race them. */
    static boolean canUseThin(Context c){
        Context app=c.getApplicationContext();
        try{return IO.submit(()->{
            Catalog a=catalog(app);
            if(!a.bundled)return true; // Already using persistent optional resources.
            migrate(app,null);
            for(Entry e:a.entries.values()){
                if(!removed(app,e)&&(!local(app,e)||!valid(file(app,e),e)))return false;
            }
            return true;
        }).get();}catch(Exception e){return false;}
    }
    static void start(Context c){
        Context app=c.getApplicationContext();
        if(!STARTED.compareAndSet(false,true))return;
        IO.execute(()->{
            try{migrate(app,null);}catch(Exception ex){recordError(app,"auto_error","Перенос: "+ex.getMessage());}
            refreshCatalog(app);
            autoSync(app);
        });
    }
    static void autoSync(Context app){
        int failures=0;String last="";
        try{
            for(Entry e:entries(app)){
                if(removed(app,e))continue;
                State state=state(app,e,true);
                boolean needs=(autoUpdate(app)&&(state==State.OLD||state==State.DAMAGED||state==State.MISSING))
                    ||(autoNew(app)&&state==State.NEW);
                if(!needs)continue;
                try{download(app,e,false,new AtomicBoolean(false),null);}
                catch(Exception ex){failures++;last=e.label+": "+ex.getMessage();}
            }
        }catch(Exception ex){failures++;last=String.valueOf(ex.getMessage());}
        if(failures>0){
            recordError(app,"auto_error","Ошибок автозагрузки: "+failures+" | "+last);
            UI.post(()->android.widget.Toast.makeText(app,"Не все ресурсы загружены. Подробности в управлении ресурсами.",android.widget.Toast.LENGTH_LONG).show());
        }else if(autoUpdate(app)||autoNew(app))recordError(app,"auto_error","");
    }
    static void checkNetwork(Context c)throws IOException{
        ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities caps=cm==null?null:cm.getNetworkCapabilities(cm.getActiveNetwork());
        if(caps==null||!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            throw new IOException("Нет подключения к интернету");
        if(wifiOnly(c)&&!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            throw new IOException("Разрешена загрузка только через Wi-Fi");
    }
    static void download(Context c,Entry e,boolean force,AtomicBoolean stop,Progress progress)throws Exception{
        if(stop.get())throw new InterruptedIOException("Загрузка остановлена");
        if(!force&&!removed(c,e)&&local(c,e))return;
        if(!force&&catalog(c).bundled){
            // A bridge APK can restore its own verified copy without network traffic.
            copyBundled(c,e,stop,true);return;
        }
        checkNetwork(c);
        File tmp=part(c,e);parent(tmp);
        // A partial file belongs to a particular signed catalog digest.
        String partialKey="partial:"+e.path;
        if(!e.sha.equals(prefs(c).getString(partialKey,""))||tmp.length()>e.size)tmp.delete();
        if(!prefs(c).edit().putString(partialKey,e.sha).commit())throw new IOException("Не удалось записать состояние загрузки");
        long offset=tmp.length();
        if(offset==e.size){publish(c,e,tmp,true);return;}
        HttpsURLConnection conn=(HttpsURLConnection)new URL(e.url).openConnection();
        conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(15000);conn.setReadTimeout(15000);
        conn.setRequestProperty("Accept-Encoding","identity");
        if(offset>0)conn.setRequestProperty("Range","bytes="+offset+"-");
        try{
            int code=conn.getResponseCode();
            if(code==200)offset=0;
            else if(code==206&&offset>0){
                String range=conn.getHeaderField("Content-Range");
                if(!("bytes "+offset+"-"+(e.size-1)+"/"+e.size).equals(range))throw new IOException("Неверный диапазон загрузки");
            }else throw new IOException("Сервер ресурсов: HTTP "+code);
            try(InputStream in=new BufferedInputStream(conn.getInputStream());FileOutputStream out=new FileOutputStream(tmp,offset>0)){
                byte[] buf=new byte[65536];long done=offset;int n;
                if(progress!=null)progress.update(e.label,done,e.size);
                while((n=in.read(buf))!=-1){
                    if(stop.get())throw new InterruptedIOException("Загрузка остановлена; прогресс сохранён");
                    checkNetwork(c);
                    done+=n;if(done>e.size){tmp.delete();throw new IOException("Размер не совпадает");}
                    out.write(buf,0,n);if(progress!=null)progress.update(e.label,done,e.size);
                }
                out.getFD().sync();if(done!=e.size)throw new EOFException("Загрузка не завершена; можно продолжить");
            }
            if(stop.get())throw new InterruptedIOException("Загрузка остановлена; прогресс сохранён");
            publish(c,e,tmp,true);
        }finally{conn.disconnect();}
    }
    static void delete(Context c,Entry e)throws IOException{
        if(!prefs(c).edit().putBoolean("removed:"+e.path,true).remove("sha:"+e.path).remove("partial:"+e.path).commit())
            throw new IOException("Не удалось сохранить выбор удаления");
        File target=file(c,e),partial=part(c,e);
        if((target.exists()&&!target.delete())||(partial.exists()&&!partial.delete()))throw new IOException("Не удалось удалить файл");
        changed();
    }
}
