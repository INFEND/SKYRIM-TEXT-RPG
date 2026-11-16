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
        byte[] bytes=readEntry(open(c),index);
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
                    try(InputStream in=z.getInputStream(e)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>MAX)throw new IOException("Фото слишком большое");out.write(buf,0,n);}byte[] bytes=out.toByteArray();BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<=0||o.outHeight<=0||o.outWidth>4096||o.outHeight>4096)throw new IOException("Неверное изображение");}
                }
            }
            if(!tmp.renameTo(dest))throw new IOException("Не удалось сохранить галерею");
        }finally{if(tmp.exists())tmp.delete();}
    }
}
