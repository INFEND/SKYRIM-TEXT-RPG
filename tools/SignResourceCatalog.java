import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
/** Sign only the resource manifest using the existing release signing identity. */
class SignResourceCatalog {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[2]);
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(root.resolve("signing.properties"))){p.load(in);}
        KeyStore store=KeyStore.getInstance(root.resolve(p.getProperty("storeFile","release.jks")).toFile(),p.getProperty("storePassword").toCharArray());
        String alias=p.getProperty("keyAlias");
        PrivateKey key=(PrivateKey)store.getKey(alias,p.getProperty("keyPassword").toCharArray());
        java.security.cert.Certificate certificate=store.getCertificate(alias);
        String fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
        if(!fingerprint.equals("8a0d221b9c31007b55ac0baed5597a0b638881f7fef6650a6e7bd0e2b49793fe"))throw new GeneralSecurityException("Wrong release certificate");
        byte[] payload=Files.readAllBytes(Path.of(args[0]));
        if(payload.length>2_000_000)throw new IOException("Catalog too large");
        Signature s=Signature.getInstance("EC".equals(key.getAlgorithm())?"SHA256withECDSA":"SHA256withRSA");
        s.initSign(key);s.update(payload);byte[] signature=s.sign();
        s.initVerify(certificate);s.update(payload);if(!s.verify(signature))throw new GeneralSecurityException("Self-test failed");
        Base64.Encoder b=Base64.getEncoder();
        Files.writeString(Path.of(args[1]),"{\"schema\":1,\"payload\":\""+b.encodeToString(payload)+"\",\"signature\":\""+b.encodeToString(signature)+"\"}\n");
    }
}
