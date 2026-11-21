package dev.infend.skyrimtext;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Read-only, grant-scoped Android sharing of user-imported artwork.
 * The provider is not exported; ChatGPT receives access only to an image
 * explicitly shared with an ACTION_SEND intent.
 */
public final class ImageShareProvider extends ContentProvider {
    static Uri uriFor(Context context, File file) {
        File directory = file.getParentFile();
        if (directory == null || !directory.getName().matches("[0-9a-f]{64}")
                || !file.getName().matches("[a-f0-9-]{36}\\.webp")
                || !directory.getParentFile().equals(new File(context.getFilesDir(), "work_images"))
                || !file.isFile()) throw new IllegalArgumentException("Некорректное изображение");
        return new Uri.Builder().scheme("content")
            .authority(context.getPackageName() + ".workimages")
            .appendPath(directory.getName()).appendPath(file.getName()).build();
    }

    private File resolve(Uri uri) throws FileNotFoundException {
        if (uri == null || !getContext().getPackageName().concat(".workimages").equals(uri.getAuthority())
                || uri.getPathSegments().size() != 2)
            throw new FileNotFoundException("Недействительный URI");
        String folder = uri.getPathSegments().get(0), name = uri.getPathSegments().get(1);
        if (!folder.matches("[0-9a-f]{64}") || !name.matches("[a-f0-9-]{36}\\.webp"))
            throw new FileNotFoundException("Недействительное имя изображения");
        File file = new File(new File(new File(getContext().getFilesDir(), "work_images"), folder), name);
        if (!file.isFile()) throw new FileNotFoundException("Изображение отсутствует");
        return file;
    }

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "image/webp"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Доступ только для чтения");
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) {
        try {
            File file = resolve(uri);
            String[] columns = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
            MatrixCursor cursor = new MatrixCursor(columns, 1);
            Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) row[i] = file.getName();
                else if (OpenableColumns.SIZE.equals(columns[i])) row[i] = file.length();
            }
            cursor.addRow(row);
            return cursor;
        } catch (FileNotFoundException e) { return null; }
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
