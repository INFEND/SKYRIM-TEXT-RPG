package dev.infend.skyrimtext;
import java.net.URL;
/** Trusted and fixed public release channel. */
public final class OtaUrlPolicy {
    private OtaUrlPolicy(){}
    public static final String MANIFEST="https://github.com/INFEND/SKYRIM-TEXT-RPG/releases/latest/download/update.json";
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
