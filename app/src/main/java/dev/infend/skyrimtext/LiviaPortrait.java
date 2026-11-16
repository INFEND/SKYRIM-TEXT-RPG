package dev.infend.skyrimtext;

import android.content.Context;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;

/** User-supplied portrait, kept outside the lightweight APK. */
final class LiviaPortrait {
    static final String REMOTE="media/livia/portrait.webp";
    static final int MAX=8*1024*1024;
    static File local(Context c){return new File(c.getFilesDir(),"livia_portrait.webp");}
    static Bitmap photo(Context c)throws IOException{
        try(InputStream in=local(c).isFile()?new FileInputStream(local(c)):ResourceStore.open(c,REMOTE)){Bitmap b=BitmapFactory.decodeStream(in);if(b==null)throw new IOException("Некорректное изображение");return b;}
    }
    static void install(Context c,Uri uri)throws IOException{
        File tmp=new File(c.getFilesDir(),"livia_portrait.tmp");
        try {
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
                if(in==null)throw new IOException("Файл недоступен");
                byte[] buf=new byte[16384];int n,total=0;
                while((n=in.read(buf))!=-1){total+=n;if(total>MAX)throw new IOException("Файл слишком большой");out.write(buf,0,n);}
            }
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(tmp.getPath(),options);
            if(options.outWidth<=0||options.outHeight<=0||options.outWidth>4096||options.outHeight>4096||!"image/webp".equals(options.outMimeType))throw new IOException("Нужен корректный WebP до 4096 пикселей");
            if(!tmp.renameTo(local(c)))throw new IOException("Не удалось сохранить портрет");
        }finally{if(tmp.exists())tmp.delete();}
    }
}
