package dev.infend.skyrimtext;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.AtomicFile;
import android.util.LruCache;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;

/** Original optional artwork for five existing NPCs. */
final class NpcAppearance {
    static final String GROUP="media:npc";
    private static final String[] IDS={"astrid","vex","karliah","nocturnal","elisif"};
    private static final String[] SHA={
        "b4cabe15d2b6f84b03268d6c537e530f3cb9ccd5bb70a5120c1d97c9fb56ec64",
        "0654bb14364788a84e9fa2a71e147c1d2bfd3889a00e75c3ef3e5fffdacc3a03",
        "7cddc76b26dbaf9a8f50849e58b39963469c848719b4dfc5e43880779cadebc7",
        "8ee39da941e16282717152409f4fc0b2e9fafd14407d94e791ce797644db5104",
        "a65a8948644d84dd040814bbb6e26503b65761b3fb93bbf99b260c345da0bf80"
    };
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(6*1024){
        @Override protected int sizeOf(String k,Bitmap b){return Math.max(1,b.getByteCount()/1024);}
    };
    static String id(String name){
        if(name==null)return "";
        switch(name.trim().toLowerCase(Locale.ROOT).replace('ё','е')){
            case "астрид":return "astrid";
            case "векс":return "vex";
            case "карлия":return "karliah";
            case "ноктюрнал":case "ноктюрнал, госпожа ночи":return "nocturnal";
            case "элисиф":case "элисиф прекрасная":case "ярл элисиф":case "ярл элисиф прекрасная":return "elisif";
            default:return "";
        }
    }
    static String path(String id){return "media/npc/"+id+".webp";}
    private static File local(Context c,String id){return new File(c.getFilesDir(),"npc_appearance_"+id+".webp");}
    static boolean has(Context c,String name){String id=id(name);return !id.isEmpty()&&(ResourceStore.has(c,path(id))||local(c,id).isFile());}
    static Bitmap photo(Context c,String name){
        String id=id(name);if(id.isEmpty())return null;
        File file=local(c,id);boolean remote=ResourceStore.has(c,path(id));
        String key=id+":"+ResourceStore.generation()+":"+file.lastModified()+":"+remote;
        Bitmap cached=CACHE.get(key);if(cached!=null)return cached;
        if(remote){try(InputStream in=ResourceStore.open(c,path(id))){Bitmap b=decode(in);if(b!=null){CACHE.put(key,b);return b;}}catch(IOException ignored){}}
        if(file.isFile()){try(InputStream in=new AtomicFile(file).openRead()){Bitmap b=decode(in);if(b!=null){CACHE.put(key,b);return b;}}catch(IOException ignored){}}
        return null;
    }
    private static byte[] read(InputStream in)throws IOException{
        if(in==null)throw new IOException("Файл недоступен");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>2_000_000)throw new IOException("Файл слишком большой");out.write(b,0,n);}
        return out.toByteArray();
    }
    private static Bitmap decode(InputStream in)throws IOException{
        byte[] bytes=read(in);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
        if(o.outWidth<1||o.outHeight<1||o.outWidth>4096||o.outHeight>4096)return null;
        o.inJustDecodeBounds=false;o.inSampleSize=2;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
    }
    private static String digest(byte[] bytes)throws IOException{
        try{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
        catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    static String install(Context c,Uri uri)throws IOException{
        byte[] bytes;try(InputStream in=c.getContentResolver().openInputStream(uri)){bytes=read(in);}
        String hash=digest(bytes);int index=-1;for(int i=0;i<SHA.length;i++)if(SHA[i].equals(hash)){index=i;break;}
        if(index<0)throw new IOException("Выбери исходный WebP без повторного сжатия");
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
        if(o.outWidth<1||o.outHeight<1||o.outWidth>4096||o.outHeight>4096)throw new IOException("Неверное изображение");
        String id=IDS[index];AtomicFile file=new AtomicFile(local(c,id));FileOutputStream out=file.startWrite();
        try{out.write(bytes);file.finishWrite(out);}catch(IOException e){file.failWrite(out);throw e;}
        CACHE.evictAll();return id;
    }
}
