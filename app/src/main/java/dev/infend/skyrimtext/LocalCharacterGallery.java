package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.*;
import java.util.*;

/** Local user-provided character artwork. Original bytes are never re-encoded. */
final class LocalCharacterGallery {
    static final int LIMIT=32, MAX_BYTES=20*1024*1024;
    private static File folder(Context c){return new File(c.getNoBackupFilesDir(),"local_character_art_itachi");}
    static File[] files(Context c){File[] a=folder(c).listFiles((d,n)->n.matches("[0-9a-f]{64}\\.img"));if(a==null)return new File[0];Arrays.sort(a,Comparator.comparingLong(File::lastModified).thenComparing(File::getName));return a;}
    static synchronized int install(Context c,List<Uri> uris) throws IOException {
        File d=folder(c);if(!d.isDirectory()&&!d.mkdirs())throw new IOException("Не удалось создать папку");
        int added=0;
        for(Uri uri:uris){
            if(files(c).length>=LIMIT)break;
            File tmp=File.createTempFile("import_","tmp",d);
            try{
                java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");
                try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
                    if(in==null)throw new IOException("Файл недоступен");
                    byte[] b=new byte[8192];int n,total=0;
                    while((n=in.read(b))!=-1){total+=n;if(total>MAX_BYTES)throw new IOException("Файл слишком большой");hash.update(b,0,n);out.write(b,0,n);}
                }
                BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(tmp.getPath(),o);
                if(o.outWidth<1||o.outHeight<1||o.outWidth>12000||o.outHeight>12000)throw new IOException("Недопустимый формат или размер изображения");
                StringBuilder id=new StringBuilder();for(byte b:hash.digest())id.append(String.format(Locale.ROOT,"%02x",b&255));
                File dest=new File(d,id+".img");if(dest.exists())continue;
                if(!tmp.renameTo(dest))throw new IOException("Не удалось сохранить изображение");
                added++;
            }catch(IOException|java.security.NoSuchAlgorithmException ignored){}finally{tmp.delete();}
        }
        return added;
    }
    static Bitmap photo(File f)throws IOException{
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),o);
        if(o.outWidth<1||o.outHeight<1)throw new IOException("Не удалось прочитать изображение");
        o.inJustDecodeBounds=false;o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>2048)o.inSampleSize*=2;
        Bitmap b=BitmapFactory.decodeFile(f.getPath(),o);if(b==null)throw new IOException("Не удалось открыть изображение");return b;
    }
    private static android.content.SharedPreferences choices(Context c){return c.getSharedPreferences("local_character_art",Context.MODE_PRIVATE);}
    private static File find(Context c,String name){if(name==null||!name.matches("[0-9a-f]{64}\\.img"))return null;File f=new File(folder(c),name);return f.isFile()?f:null;}
    static synchronized File selected(Context c){return find(c,choices(c).getString("itachi_portrait",""));}
    static synchronized boolean select(Context c,File f){if(f==null)return choices(c).edit().remove("itachi_portrait").commit();File valid=find(c,f.getName());return valid!=null&&valid.equals(f)&&choices(c).edit().putString("itachi_portrait",f.getName()).commit();}
    static synchronized File replace(Context c,String target,Uri uri)throws IOException{
        File old=find(c,target);if(old==null)throw new IOException("Изображение больше не существует");
        File tmp=File.createTempFile("replace_","tmp",folder(c));
        try{
            java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
                if(in==null)throw new IOException("Файл недоступен");byte[] b=new byte[8192];int n,total=0;
                while((n=in.read(b))!=-1){total+=n;if(total>MAX_BYTES)throw new IOException("Файл слишком большой");hash.update(b,0,n);out.write(b,0,n);}
            }
            BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(tmp.getPath(),o);
            if(o.outWidth<1||o.outHeight<1||o.outWidth>12000||o.outHeight>12000)throw new IOException("Недопустимый формат или размер изображения");
            Bitmap preview=photo(tmp);preview.recycle();
            StringBuilder id=new StringBuilder();for(byte b:hash.digest())id.append(String.format(Locale.ROOT,"%02x",b&255));
            File dest=new File(folder(c),id+".img");if(dest.equals(old))return old;
            if(dest.exists())throw new IOException("Это изображение уже есть в галерее");
            boolean primary=old.equals(selected(c));long order=old.lastModified();
            if(!tmp.renameTo(dest))throw new IOException("Не удалось сохранить изображение");
            dest.setLastModified(order);
            if(primary&&!select(c,dest)){dest.delete();throw new IOException("Не удалось обновить портрет");}
            if(!old.delete()){if(primary)select(c,old);dest.delete();throw new IOException("Не удалось заменить прежнее изображение");}
            return dest;
        }catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}finally{tmp.delete();}
    }
    static synchronized boolean remove(Context c,File f){File valid=find(c,f.getName());if(valid==null||!valid.equals(f))return false;boolean primary=f.equals(selected(c));if(!f.delete())return false;if(primary)select(c,null);return true;}
    private LocalCharacterGallery(){}
}
