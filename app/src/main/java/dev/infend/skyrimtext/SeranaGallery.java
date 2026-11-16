package dev.infend.skyrimtext;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.*;
import java.util.Locale;
import java.util.zip.*;

final class SeranaGallery {
    static final String REMOTE="media/serana/gallery.zip";
    static final int COUNT=13, MAX=2_000_000;
    private static final String[] SHA={"eddc19cb4572cfe2d5bc780c90bc21ffa1855c3a88cf8518b8a7a2d32844fc96","8e557525625087968a2c18ed939488bb11bb43af78cde4822d706d1b5434ef3d","d4a6fd6b2e98b7973ea2461c3bb61c2b5c73a2e5b0f8ed9f99d64b47f6448457","dad1ebf50ebe3e8acb52e8b76714a944178dc9d13094061671372c54ca7fbc28","cdff022d2e92bee6385cc1501295fcc5e0e0b60b26da750c0ac06632c064f294","000e781dbde67895be58d25e4730b3aadac2fdf591937f557c3f26804da6cea7","577259d10276e85a164332b4126030d625a725a4817bc45c15a0cd70d66286e3","9a62e5a21658223a8f59137307ff424fef5987ce96d390c8d66472b63af0ed64","cce3872e8e8ff238ada232d0c8b32143b6ebd13195f345b7896bec5fd02b35e7","73863d921de05a6e55816ec4b0984a77792a5011a4cf28ca231958dbb9361be2","1248fd1fc9e0efd07b892456e74f6593be8c4e6f14333161b3a53406c1a015ad","25cac0b8753ba77795dd82c10fd60a085ef905c2eac04bcf29ebd2e9b9376718","9b8f9b023c953dc8f2c8388a0c174a474d2ddde5d2b7443811a7f2321e7c3941"};
    static void verify(byte[] bytes,int index)throws IOException{
        if(index<1||index>COUNT)throw new IOException("Неверный номер фото");
        try{byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));if(!SHA[index-1].equals(s.toString()))throw new IOException("Фото не соответствует исходной галерее Сераны");}
        catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    static File local(Context c){return new File(c.getFilesDir(),"serana_gallery.zip");}
    static byte[] readEntry(InputStream raw,int index)throws IOException{
        String target=String.format(Locale.ROOT,"media/serana/%02d.webp",index);
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
        File dest=local(c),tmp=new File(c.getFilesDir(),"serana_gallery.tmp");
        try{
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
                if(in==null)throw new IOException("Файл недоступен");byte[] b=new byte[8192];int n,total=0;
                while((n=in.read(b))!=-1){total+=n;if(total>20_000_000)throw new IOException("Архив слишком большой");out.write(b,0,n);}
            }
            try(ZipFile z=new ZipFile(tmp)){if(z.size()!=COUNT)throw new IOException("Требуется 13 фотографий");
                for(int i=1;i<=COUNT;i++){ZipEntry e=z.getEntry(String.format(Locale.ROOT,"media/serana/%02d.webp",i));if(e==null||e.getSize()<1||e.getSize()>MAX)throw new IOException("Неверный состав архива");
                    try(InputStream in=z.getInputStream(e)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>MAX)throw new IOException("Фото слишком большое");out.write(buf,0,n);}byte[] bytes=out.toByteArray();verify(bytes,i);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<=0||o.outHeight<=0||o.outWidth>4096||o.outHeight>4096)throw new IOException("Неверное изображение");}
                }
            }
            if(!tmp.renameTo(dest))throw new IOException("Не удалось сохранить галерею");
        }finally{if(tmp.exists())tmp.delete();}
    }
}
