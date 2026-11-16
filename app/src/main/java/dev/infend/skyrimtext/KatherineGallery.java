package dev.infend.skyrimtext;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.*;
import java.util.Locale;
import java.util.zip.*;

final class KatherineGallery {
    static final String REMOTE="media/katherine/gallery.zip";
    static final int COUNT=10, MAX=2_000_000;
    private static final String[] SHA={"6911c5d07840442ec1e085b282cc78a3f9654f4b5134c972410bd8ef9ae45068","a74384c9dc4710203515dc830169ad243286325a94391d4319391e48cc7ef9ac","9e8c745f9449a2848041c93b97fcb3b7650037ad82cb5955bd7a1fe178d720fe","c7333b8f56c30f825a7154f55235d3e831541e8201deab830401fee9952533ca","c34e41329a280b94e98809d5f5ff74f650aef5a369134218a6160290de9030bb","71380e1518f1ef3e56288373135769bbf5214705cb217c3baf6949b1f1b55a0a","e63400c2f935f9451b1edb29cc5a162b84ff929b14c43362bed41de8e7a4062c","3075ae22eda3d19aaa97156070a75ac9fca53e605256851704552f1738e0e8a4","f156af36bf53b1698e94cff0cebecfa19df0629548534868c114915d8e4c2617","febd0e3142663c53450b6e2a63c1560a3fe7ddb071105e47dcf9e4e7cda0cd91"};
    static void verify(byte[] bytes,int index)throws IOException{
        if(index<1||index>COUNT)throw new IOException("Неверный номер фото");
        try{byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));if(!SHA[index-1].equals(s.toString()))throw new IOException("Фото не соответствует исходной галерее Кэтрин");}
        catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    static File local(Context c){return new File(c.getFilesDir(),"katherine_gallery.zip");}
    static byte[] readEntry(InputStream raw,int index)throws IOException{
        String target=String.format(Locale.ROOT,"media/katherine/%02d.webp",index);
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
        File dest=local(c),tmp=new File(c.getFilesDir(),"katherine_gallery.tmp");
        try{
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
                if(in==null)throw new IOException("Файл недоступен");byte[] b=new byte[8192];int n,total=0;
                while((n=in.read(b))!=-1){total+=n;if(total>20_000_000)throw new IOException("Архив слишком большой");out.write(b,0,n);}
            }
            try(ZipFile z=new ZipFile(tmp)){if(z.size()!=COUNT)throw new IOException("Требуется 10 фотографий");
                for(int i=1;i<=COUNT;i++){ZipEntry e=z.getEntry(String.format(Locale.ROOT,"media/katherine/%02d.webp",i));if(e==null||e.getSize()<1||e.getSize()>MAX)throw new IOException("Неверный состав архива");
                    try(InputStream in=z.getInputStream(e)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>MAX)throw new IOException("Фото слишком большое");out.write(buf,0,n);}byte[] bytes=out.toByteArray();verify(bytes,i);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<=0||o.outHeight<=0||o.outWidth>4096||o.outHeight>4096)throw new IOException("Неверное изображение");}
                }
            }
            if(!tmp.renameTo(dest))throw new IOException("Не удалось сохранить галерею");
        }finally{if(tmp.exists())tmp.delete();}
    }
}
