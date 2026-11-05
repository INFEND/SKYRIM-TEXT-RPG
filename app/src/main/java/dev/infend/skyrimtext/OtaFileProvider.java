package dev.infend.skyrimtext;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Read-only, narrowly scoped ContentProvider for Android's APK installer. */
public final class OtaFileProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    private File file(Uri uri)throws FileNotFoundException{
        if(!"/apk".equals(uri.getPath()))throw new FileNotFoundException("Invalid URI");
        File f=UpdateManager.apkFile(getContext());if(!f.isFile())throw new FileNotFoundException();return f;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(!"r".equals(mode))throw new FileNotFoundException("Read-only");
        return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public String getType(Uri uri){return "application/vnd.android.package-archive";}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder){
        try{
            File f=file(uri);
            String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
            MatrixCursor c=new MatrixCursor(cols);Object[] values=new Object[cols.length];
            for(int i=0;i<cols.length;i++){
                if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))values[i]="SKYRIM-TEXT-RPG.apk";
                else if(OpenableColumns.SIZE.equals(cols[i]))values[i]=f.length();
            }
            c.addRow(values);return c;
        }catch(Exception e){return null;}
    }
    @Override public Uri insert(Uri uri,ContentValues values){return null;}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){return 0;}
    @Override public int delete(Uri uri,String selection,String[] args){return 0;}
}
