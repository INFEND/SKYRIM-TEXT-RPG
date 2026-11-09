package dev.infend.skyrimtext;
import java.net.URL;
/** Trusted and fixed public release channel. */
public final class OtaUrlPolicy {
    private OtaUrlPolicy(){}
    public static final String MANIFEST="https://github.com/INFEND/SKYRIM-TEXT-RPG/releases/latest/download/update.json";
    // A separate, maintained branch carries the exact verified release manifest.
    // Published only AFTER GitHub has accepted the signed release assets.
    public static final String MANIFEST_MIRROR=
        "https://raw.githubusercontent.com/INFEND/SKYRIM-TEXT-RPG/ota-manifest/update.json";
    public static final String RELEASE_API=
        "https://api.github.com/repos/INFEND/SKYRIM-TEXT-RPG/releases/latest";
    public static String manifestForTag(String tag){
        if(tag==null||!tag.matches("v[0-9]+(?:\\.[0-9]+){1,3}"))return null;
        return "https://github.com/INFEND/SKYRIM-TEXT-RPG/releases/download/"
            +tag+"/update.json";
    }
    public static boolean trustedApk(String raw,String version){
        if(raw==null||version==null||!version.matches("[A-Za-z0-9._-]{1,40}"))return false;
        try{
            URL url=new URL(raw);
            return "https".equalsIgnoreCase(url.getProtocol())&&
                "github.com".equalsIgnoreCase(url.getHost())&&
                url.getPort()==-1&&url.getUserInfo()==null&&url.getQuery()==null&&url.getRef()==null&&
                ("/INFEND/SKYRIM-TEXT-RPG/releases/download/v"+version+"/SKYRIM-TEXT-RPG.apk").equals(url.getPath());
        }catch(Exception error){return false;}
    }
    public static String canonicalApkFallback(String url,String version){return null;}
}
