package dev.infend.skyrimtext;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.*;
import java.util.Locale;
import java.util.zip.*;

final class AlmaGallery {
    static final String REMOTE="media/alma/gallery.zip";
    static final int COUNT=7, MAX=2_000_000;
    private static final String[] SHA={"b4c319c16ba2a18b05138c5c877329225aefcca05f6141be5f3144ef586af077","bb31fbb9f36c5830f170ed6dc79d7113e273118f3b6c653b39e3dc9d7cdf073e","b8389be0291cc1ea67f3d15d6f66543858c549e7ec0a58cbfd8a811c3d395436","1274692b57370583996e5201e1fa8a2fbb207068a5e516b9315273c43bcb87b3","1503866260fee59ceffedef3cead65491c15061dadaa5f81becdc691a7a4b713","cfcf2636bf8a563356f05b034bf871880bf6dfbd0d39bdda5d85a601003b749e","92ac8c7295bf90dc7bad4e29c35eee5e758bc629f9f3312c5f8c7a0fb2a09196"};
    static void verify(byte[] bytes,int index)throws IOException{
        if(index<1||index>COUNT)throw new IOException("Неверный номер фото");
        try{byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));if(!SHA[index-1].equals(s.toString()))throw new IOException("Фото не соответствует исходной галерее Альмы");}
        catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    static File local(Context c){return new File(c.getFilesDir(),"alma_gallery.zip");}
    static byte[] readEntry(InputStream raw,int index)throws IOException{
        String target=String.format(Locale.ROOT,"media/alma/%02d%s",index,index==4?".jpg":".webp");
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
    static boolean available(Context c){return local(c).isFile()||ResourceStore.has(c,REMOTE);}
    static Bitmap photo(Context c,int index)throws IOException{
        byte[] bytes;
        try{bytes=readEntry(open(c),index);verify(bytes,index);}
        catch(IOException e){if(!local(c).isFile()||!ResourceStore.has(c,REMOTE))throw e;bytes=readEntry(ResourceStore.open(c,REMOTE),index);verify(bytes,index);}
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
        if(o.outWidth<=0||o.outHeight<=0||o.outWidth>4096||o.outHeight>4096)throw new IOException("Неверные размеры изображения");
        o.inJustDecodeBounds=false;o.inSampleSize=Math.max(1,Math.max(o.outWidth,o.outHeight)/1400);
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(bitmap==null)throw new IOException("Не удалось прочитать фото");return bitmap;
    }
    static void install(Context c,Uri uri)throws IOException{
        File dest=local(c),tmp=new File(c.getFilesDir(),"alma_gallery.tmp");
        try{
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
                if(in==null)throw new IOException("Файл недоступен");byte[] b=new byte[8192];int n,total=0;
                while((n=in.read(b))!=-1){total+=n;if(total>20_000_000)throw new IOException("Архив слишком большой");out.write(b,0,n);}
            }
            try(ZipFile z=new ZipFile(tmp)){if(z.size()!=COUNT)throw new IOException("Требуется 7 фотографий");
                for(int i=1;i<=COUNT;i++){ZipEntry e=z.getEntry(String.format(Locale.ROOT,"media/alma/%02d%s",i,i==4?".jpg":".webp"));if(e==null||e.getSize()<1||e.getSize()>MAX)throw new IOException("Неверный состав архива");
                    try(InputStream in=z.getInputStream(e)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>MAX)throw new IOException("Фото слишком большое");out.write(buf,0,n);}byte[] bytes=out.toByteArray();verify(bytes,i);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<=0||o.outHeight<=0||o.outWidth>4096||o.outHeight>4096)throw new IOException("Неверное изображение");}
                }
            }
            if(!tmp.renameTo(dest))throw new IOException("Не удалось сохранить галерею");
        }finally{if(tmp.exists())tmp.delete();}
    }
}
