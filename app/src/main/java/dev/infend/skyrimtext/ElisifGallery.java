package dev.infend.skyrimtext;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.*;
import java.util.Locale;
import java.util.zip.*;

final class ElisifGallery {
    static final String REMOTE="media/elisif/gallery.zip";
    static final int COUNT=10, MAX=2_000_000; // legacy archive remains byte-identical
    static final int DEFAULT=11;
    static final String PORTRAIT="media/elisif/portrait.png";
    static final String PORTRAIT_SHA="ebccdfe23435e7412d343ad885a4ab089f0250c20f6bdee08546221ef3b2b2aa";
    private static final String PREF="elisif_gallery", KEY="primary";
    static boolean legacy(Context c){return local(c).isFile()||ResourceStore.has(c,REMOTE);}
    static boolean newPortrait(Context c){return ResourceStore.has(c,PORTRAIT);}
    static int count(Context c){return (legacy(c)?COUNT:0)+(newPortrait(c)?1:0);}
    static boolean hasPhoto(Context c,int index){return index==DEFAULT?newPortrait(c):index>=1&&index<=COUNT&&legacy(c);}
    static int primary(Context c){
        int saved=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getInt(KEY,DEFAULT);
        if(hasPhoto(c,saved))return saved;
        return newPortrait(c)?DEFAULT:1;
    }
    static void select(Context c,int index){if(hasPhoto(c,index))c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putInt(KEY,index).apply();}
    static void reset(Context c){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().remove(KEY).apply();}
    static int position(Context c,int index){return legacy(c)?index:1;}
    static int navigate(Context c,int index,int direction){
        for(int i=1;i<=DEFAULT;i++){int candidate=1+Math.floorMod(index-1+direction*i,DEFAULT);if(hasPhoto(c,candidate))return candidate;}
        return primary(c);
    }
    private static byte[] readPortrait(Context c)throws IOException{
        try(InputStream in=ResourceStore.open(c,PORTRAIT);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1){if(out.size()+n>4_000_000)throw new IOException("Портрет слишком большой");out.write(buf,0,n);}
            byte[] bytes=out.toByteArray();
            try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder();for(byte v:digest)s.append(String.format(Locale.ROOT,"%02x",v&255));if(!PORTRAIT_SHA.equals(s.toString()))throw new IOException("Портрет повреждён");}
            catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
            return bytes;
        }
    }
    private static final String[] SHA={"9d46e7ce6028337d5f9cdb789a9b3d69dee9a958d3da7abc0e3541edc07605dd","ccca3bedaaa60b36fee56fa4fd6ee8f1ff8b926a60e5e80c525fb5cbee4ef453","1969d90c531e30aa7dd47a7ef547111b8074a55eb06b7756b697f1e92a63841d","a2c6ebbe8abf4438e5e4f80a74151c791e35da39ddc72577ba8b1dd787f52324","7b0bbadde2134f97daa0fde38b96ce01ac18003352b27cd39ced2245ccf0b809","b6d68977425d898c1fed9e510945ff63a71d1a2dfadb1271cee265b84ae1d535","d82e287bbcc7ad374cdeed4d09ee9ab34a8e222978d1d38b2ee17d9d6119ff8f","c834b0b525a0fdab72cc00de1bc28f4ffeae3e37cf86318cd30dd84791f731bf","a7c7e527f0db39daaeec41eb9fe4d40ddc607fb6a6cc26f7db0e2cb6c7fc2e28","c50c2f85cfbd16e24c4fac68cf34b00ec5ed7bb9537cfc8a5d11a3e0d9d13e82"};
    static boolean matches(String name){if(name==null)return false;String n=name.trim().toLowerCase(Locale.ROOT);return "elisif".equals(NpcAppearance.id(name))||"elisif".equals(NpcAppearance.id(n.replace("эллисиф","элисиф")));}
    static void verify(byte[] bytes,int index)throws IOException{
        if(index<1||index>COUNT)throw new IOException("Неверный номер фото");
        try{byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));if(!SHA[index-1].equals(s.toString()))throw new IOException("Фото не соответствует исходной галерее Элисиф");}
        catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    static File local(Context c){return new File(c.getFilesDir(),"elisif_gallery.zip");}
    static byte[] readEntry(InputStream raw,int index)throws IOException{
        String target=String.format(Locale.ROOT,"media/elisif/%02d.jpg",index);
        try(ZipInputStream z=new ZipInputStream(raw)){
            ZipEntry e;int count=0;
            while((e=z.getNextEntry())!=null&&++count<=COUNT){
                if(!e.getName().equals(target)){z.closeEntry();continue;}
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
                while((n=z.read(buf))!=-1){if(out.size()+n>MAX)throw new IOException("Фото слишком большое");out.write(buf,0,n);}return out.toByteArray();
            }
        }
        throw new IOException("Фото отсутствует");
    }
    static InputStream open(Context c)throws IOException{return local(c).isFile()?new FileInputStream(local(c)):ResourceStore.open(c,REMOTE);}
    static boolean available(Context c){return count(c)>0;}
    static Bitmap photo(Context c,int index)throws IOException{
        byte[] bytes;
        if(index==DEFAULT)bytes=readPortrait(c);
        else {
            try{bytes=readEntry(open(c),index);verify(bytes,index);}
            catch(IOException e){if(!local(c).isFile()||!ResourceStore.has(c,REMOTE))throw e;bytes=readEntry(ResourceStore.open(c,REMOTE),index);verify(bytes,index);}
        }
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
        if(o.outWidth<=0||o.outHeight<=0||o.outWidth>4096||o.outHeight>4096)throw new IOException("Неверные размеры изображения");
        o.inJustDecodeBounds=false;o.inSampleSize=Math.max(1,Math.max(o.outWidth,o.outHeight)/1400);
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(bitmap==null)throw new IOException("Не удалось прочитать фото");return bitmap;
    }
    static void install(Context c,Uri uri)throws IOException{
        File dest=local(c),tmp=new File(c.getFilesDir(),"elisif_gallery.tmp");
        try{
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
                if(in==null)throw new IOException("Файл недоступен");byte[] b=new byte[8192];int n,total=0;
                while((n=in.read(b))!=-1){total+=n;if(total>20_000_000)throw new IOException("Архив слишком большой");out.write(b,0,n);}
            }
            try(ZipFile z=new ZipFile(tmp)){if(z.size()!=COUNT)throw new IOException("Требуется 10 фотографий");
                for(int i=1;i<=COUNT;i++){ZipEntry e=z.getEntry(String.format(Locale.ROOT,"media/elisif/%02d.jpg",i));if(e==null||e.getSize()<1||e.getSize()>MAX)throw new IOException("Неверный состав архива");
                    try(InputStream in=z.getInputStream(e)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>MAX)throw new IOException("Фото слишком большое");out.write(buf,0,n);}byte[] bytes=out.toByteArray();verify(bytes,i);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<=0||o.outHeight<=0||o.outWidth>4096||o.outHeight>4096)throw new IOException("Неверное изображение");}
                }
            }
            if(!tmp.renameTo(dest))throw new IOException("Не удалось сохранить галерею");
        }finally{if(tmp.exists())tmp.delete();}
    }
}
