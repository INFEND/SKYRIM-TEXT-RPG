package dev.infend.skyrimtext;

import android.content.Context;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;

/** Exact supplied image bytes; manual override is independent of the signed optional resource. */
final class LiviaPortrait {
    static final String REMOTE="media/livia/portrait.webp";
    static final int MAX=8*1024*1024;
    static File local(Context c){return new File(c.getFilesDir(),"livia_portrait.webp");}
    static boolean overridden(Context c){return local(c).isFile();}
    static boolean reset(Context c){return !overridden(c)||local(c).delete();}
    static Bitmap photo(Context c)throws IOException{
        if(overridden(c)){
            Bitmap b=BitmapFactory.decodeFile(local(c).getPath());
            if(b!=null)return b;
        }
        try(InputStream in=ResourceStore.open(c,REMOTE)){
            Bitmap b=BitmapFactory.decodeStream(in);if(b==null)throw new IOException("Некорректное изображение");return b;
        }
    }
    static void install(Context c,Uri uri)throws IOException{
        File tmp=new File(c.getFilesDir(),"livia_portrait.tmp");
        try{
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
                if(in==null)throw new IOException("Файл недоступен");
                byte[] buf=new byte[16384];int n,total=0;
                while((n=in.read(buf))!=-1){total+=n;if(total>MAX)throw new IOException("Файл слишком большой");out.write(buf,0,n);}
            }
            BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeFile(tmp.getPath(),opt);
            String mime=opt.outMimeType;
            if(opt.outWidth<=0||opt.outHeight<=0||opt.outWidth>4096||opt.outHeight>4096||
                !("image/webp".equals(mime)||"image/jpeg".equals(mime)||"image/png".equals(mime)))
                throw new IOException("Нужен корректный JPEG, PNG или WebP до 4096 пикселей");
            // Validate actual decodability before replacing a working image; do not transcode.
            Bitmap probe=BitmapFactory.decodeFile(tmp.getPath());
            if(probe==null)throw new IOException("Повреждённое изображение");
            probe.recycle();
            if(!tmp.renameTo(local(c)))throw new IOException("Не удалось сохранить портрет");
        }finally{if(tmp.exists())tmp.delete();}
    }
}
