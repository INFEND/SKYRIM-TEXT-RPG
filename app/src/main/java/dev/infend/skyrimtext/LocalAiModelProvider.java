package dev.infend.skyrimtext;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;
/** Signature-protected access to the selected GGUF, without copying the model. */
public final class LocalAiModelProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    private static File selected(Context c)throws IOException{
        File dir=c.getExternalFilesDir("models");
        if(dir==null)throw new IOException("GGUF storage unavailable");
        File file=new File(dir,"skyrim-local.gguf").getCanonicalFile();
        String chosen=LocalAiModelStore.path(c.getSharedPreferences("skyrim_text_v1",0));
        if(chosen.isEmpty()||!file.equals(new File(chosen).getCanonicalFile())||!file.isFile()
            ||!dir.getCanonicalFile().equals(file.getParentFile()))throw new IOException("GGUF unavailable");
        return file;
    }
    static Uri uri(Context c,String path){
        try{File f=selected(c);
            if(!f.equals(new File(path).getCanonicalFile()))return null;
            return Uri.parse("content://"+c.getPackageName()+".models/"+f.lastModified()+"-"+f.length()+"/model.gguf");
        }catch(Exception e){return null;}
    }
    @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{
        if(!"r".equals(mode))throw new FileNotFoundException("Read only");
        try{File f=selected(getContext());
            if(!u.equals(uri(getContext(),f.getPath())))throw new FileNotFoundException("Invalid model URI");
            return ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);
        }catch(IOException e){throw new FileNotFoundException(e.getMessage());}
    }
    @Override public String getType(Uri uri){return "application/octet-stream";}
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String sort){return null;}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
