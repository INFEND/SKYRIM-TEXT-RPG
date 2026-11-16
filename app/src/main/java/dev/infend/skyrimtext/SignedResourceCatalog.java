package dev.infend.skyrimtext;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Base64;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/** Resource catalog authenticity is anchored in the installed release APK certificate. */
final class SignedResourceCatalog {
    private static final String URL_STRING="https://raw.githubusercontent.com/INFEND/SKYRIM-TEXT-RPG/ota-manifest/resource-catalog.json";
    private static final String CERT="8a0d221b9c31007b55ac0baed5597a0b638881f7fef6650a6e7bd0e2b49793fe";
    private static final int LIMIT=3_000_000;
    static byte[] read(InputStream in,int max)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
        while((n=in.read(buf))!=-1){if(out.size()+n>max)throw new IOException("Каталог слишком велик");out.write(buf,0,n);}
        return out.toByteArray();
    }
    private static File file(Context c){return new File(c.getFilesDir(),"resource_catalog_verified.json");}
    static byte[] fetch(Context c)throws Exception{
        ResourceStore.checkNetwork(c);
        HttpsURLConnection conn=(HttpsURLConnection)new URL(URL_STRING+"?check="+System.currentTimeMillis()).openConnection();
        conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(6000);conn.setReadTimeout(8000);
        conn.setUseCaches(false);
        conn.setRequestProperty("Cache-Control","no-cache, no-store, max-age=0");
        conn.setRequestProperty("Pragma","no-cache");
        try{
            int code=conn.getResponseCode();
            if(code!=200)throw new IOException("HTTP "+code);
            try(InputStream in=conn.getInputStream()){return read(in,LIMIT);}
        }finally{conn.disconnect();}
    }
    static ResourceStore.Catalog cached(Context c)throws Exception{
        File path=file(c);if(!path.isFile())return null;
        try(InputStream in=new FileInputStream(path)){return verify(c,read(in,LIMIT));}
    }
    static void save(Context c,byte[] envelope)throws IOException{
        File dest=file(c),tmp=new File(dest.getPath()+".tmp");
        try{
            try(FileOutputStream out=new FileOutputStream(tmp)){out.write(envelope);out.getFD().sync();}
            Files.move(tmp.toPath(),dest.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{tmp.delete();}
    }
    private static X509Certificate certificate(Context c)throws Exception{
        PackageManager pm=c.getPackageManager();
        PackageInfo info=pm.getPackageInfo(c.getPackageName(),Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES);
        android.content.pm.Signature[] signatures=Build.VERSION.SDK_INT>=28?info.signingInfo.getApkContentsSigners():info.signatures;
        if(signatures==null||signatures.length!=1)throw new GeneralSecurityException("Нет сертификата выпуска");
        byte[] der=signatures[0].toByteArray();
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(der);
        StringBuilder hex=new StringBuilder();for(byte x:hash)hex.append(String.format(java.util.Locale.ROOT,"%02x",x&255));
        if(!CERT.contentEquals(hex))throw new GeneralSecurityException("Неподходящая подпись приложения");
        return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der));
    }
    static ResourceStore.Catalog verify(Context c,byte[] envelope)throws Exception{
        JSONObject outer=new JSONObject(new String(envelope,StandardCharsets.UTF_8));
        if(outer.getInt("schema")!=1)throw new GeneralSecurityException("Версия подписи каталога");
        byte[] payload=Base64.decode(outer.getString("payload"),Base64.DEFAULT);
        byte[] signature=Base64.decode(outer.getString("signature"),Base64.DEFAULT);
        if(payload.length>2_000_000||signature.length>2048)throw new GeneralSecurityException("Размер подписи каталога");
        PublicKey key=certificate(c).getPublicKey();
        Signature verify=Signature.getInstance("EC".equals(key.getAlgorithm())?"SHA256withECDSA":"SHA256withRSA");
        verify.initVerify(key);verify.update(payload);
        if(!verify.verify(signature))throw new GeneralSecurityException("Подпись каталога не прошла проверку");
        return new ResourceStore.Catalog(new JSONObject(new String(payload,StandardCharsets.UTF_8)));
    }
}
