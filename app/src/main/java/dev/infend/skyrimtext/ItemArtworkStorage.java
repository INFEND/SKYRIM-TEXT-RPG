package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

/** Local, size-bounded custom artwork; never stores remote URLs or API secrets. */
final class ItemArtworkStorage {
    private static final int MAX_BYTES=24*1024*1024;
    private static final int MAX_EDGE=1280;
    private ItemArtworkStorage(){}

    static String install(Context context,Uri uri)throws Exception{
        if(uri==null)throw new IllegalArgumentException("Не выбрано изображение");
        try(InputStream input=context.getContentResolver().openInputStream(uri)){
            return install(context,readLimited(input));
        }
    }
    static String install(Context context,byte[] image)throws Exception{
        if(image==null||image.length==0||image.length>MAX_BYTES)
            throw new IllegalArgumentException("Слишком большой файл изображения");
        BitmapFactory.Options bounds=new BitmapFactory.Options();
        bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(image,0,image.length,bounds);
        if(bounds.outWidth<32||bounds.outHeight<32||bounds.outWidth>12000||bounds.outHeight>12000)
            throw new IllegalArgumentException("Неверный размер изображения");
        BitmapFactory.Options opts=new BitmapFactory.Options();
        opts.inSampleSize=1;
        while(Math.max(bounds.outWidth, bounds.outHeight)/opts.inSampleSize>MAX_EDGE*2)
            opts.inSampleSize*=2;
        Bitmap source=BitmapFactory.decodeByteArray(image,0,image.length,opts);
        if(source==null)throw new IllegalArgumentException("Файл изображения повреждён");
        Bitmap resized=source;
        int largest=Math.max(source.getWidth(),source.getHeight());
        if(largest>MAX_EDGE){
            double k=MAX_EDGE/(double)largest;
            resized=Bitmap.createScaledBitmap(source,Math.max(1,(int)(source.getWidth()*k)),
                    Math.max(1,(int)(source.getHeight()*k)),true);
            source.recycle();
        }
        File directory=new File(context.getFilesDir(),"item_art");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IllegalStateException("Нет папки изображений");
        String filename=UUID.randomUUID().toString()+".webp";
        File imageFile=new File(directory,filename);
        boolean stored=false;
        try(FileOutputStream out=new FileOutputStream(imageFile)){
            stored=resized.compress(Bitmap.CompressFormat.WEBP,90,out);
            out.flush();
        }finally{resized.recycle();}
        if(!stored||imageFile.length()==0){
            imageFile.delete();throw new IllegalStateException("Не удалось сохранить WebP");
        }
        return filename;
    }
    static Bitmap preview(Context context,String filename,int maxEdge){
        if(filename==null||!filename.matches("[a-fA-F0-9-]{36}\\.webp"))return null;
        File file=new File(new File(context.getFilesDir(),"item_art"),filename);
        if(!file.isFile()||file.length()>MAX_BYTES)return null;
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(file.getAbsolutePath(),bounds);
        if(bounds.outWidth<1||bounds.outHeight<1)return null;
        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=1;
        while(Math.max(bounds.outWidth,bounds.outHeight)/opts.inSampleSize>Math.max(64,maxEdge)*2)
            opts.inSampleSize*=2;
        return BitmapFactory.decodeFile(file.getAbsolutePath(),opts);
    }
    private static byte[] readLimited(InputStream in)throws Exception{
        if(in==null)throw new IllegalArgumentException("Не удалось открыть изображение");
        try(InputStream src=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;
            while((n=src.read(buf))!=-1){
                if(out.size()+n>MAX_BYTES)throw new IllegalArgumentException("Размер изображения больше 24 МБ");
                out.write(buf,0,n);
            }
            return out.toByteArray();
        }
    }
}
