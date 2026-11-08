package dev.infend.skyrimtext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonToken;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Official Sign in with ChatGPT open-source OAuth flow:
 * browser + loopback listener + PKCE + signed OIDC identity verification.
 * Never collect the ChatGPT password, cookies, or a ChatGPT browser session.
 */
final class ChatGptOAuth {
    interface StatusListener { void finished(boolean success,String message); }
    interface TokenListener { void token(String bearer,String error); }
    interface ModelListener { void models(List<ModelOption> models,String error); }
    static final class ModelOption {
        final String slug;
        final String title;
        ModelOption(String slug,String title){this.slug=slug;this.title=title;}
    }

    private static final String AUTHORIZE="https://auth.openai.com/api/accounts/authorize";
    private static final String TOKEN="https://auth.openai.com/api/accounts/oauth/token";
    private static final String DISCOVERY="https://auth.openai.com/.well-known/openid-configuration";
    private static final String RESOURCE="https://api.openai.com/v1";
    private static final String SCOPES="openid profile email offline_access resource.invoke chatgpt.tokens.use.direct";
    private final Context context;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private volatile ServerSocket listener;
    private volatile boolean signingIn;
    private final SecureRandom random=new SecureRandom();

    ChatGptOAuth(Context context){this.context=context.getApplicationContext();}

    boolean isSigningIn(){return signingIn;}

    boolean hasAuthorizedPlan(){
        try {
            JSONObject session=AiOAuthVault.load(context);
            return !session.optString("refresh_token","").isEmpty()
                    && hasScope(session.optString("scope",""),"chatgpt.tokens.use.direct");
        } catch(Exception ignored){return false;}
    }

    String accountLabel(){
        try{
            JSONObject s=AiOAuthVault.load(context);
            if(!hasAuthorizedPlan())return "Не подключён";
            String email=s.optString("email","");
            return email.isEmpty()?"ChatGPT подключён":email;
        }catch(Exception e){return "Не подключён";}
    }

    void signIn(Activity activity,StatusListener callback){
        if(signingIn){callback.finished(false,"Авторизация уже выполняется");return;}
        signingIn=true;
        worker.execute(()->{
            try(ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){
                listener=server;
                server.setSoTimeout(180000);
                String redirect="http://127.0.0.1:"+server.getLocalPort()+"/auth/callback";
                JSONObject old=AiOAuthVault.load(context);
                String savedClient=old.optString("client_id","");
                boolean newClient=savedClient.isEmpty();
                String client=newClient?"dynamic_agent_client":savedClient;
                String state=randomUrl(32);
                String nonce=randomUrl(32);
                String verifier=randomUrl(64);
                String challenge=urlSafe(MessageDigest.getInstance("SHA-256")
                        .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                Uri.Builder uri=Uri.parse(AUTHORIZE).buildUpon()
                        .appendQueryParameter("client_id",client)
                        .appendQueryParameter("ext_agent_host_id",AiOAuthVault.hostId(context))
                        .appendQueryParameter("response_type","code")
                        .appendQueryParameter("redirect_uri",redirect)
                        .appendQueryParameter("scope",SCOPES)
                        .appendQueryParameter("resource",RESOURCE)
                        .appendQueryParameter("state",state)
                        .appendQueryParameter("nonce",nonce)
                        .appendQueryParameter("code_challenge_method","S256")
                        .appendQueryParameter("code_challenge",challenge);
                if(newClient)uri.appendQueryParameter("agent_name_hint","SKYRIM TEXT RPG");
                else {
                    String hint=old.optString("id_token","");
                    if(!hint.isEmpty())uri.appendQueryParameter("id_token_hint",hint);
                }
                final Uri authUri=uri.build();
                activity.runOnUiThread(()->{
                    try{
                        Intent intent=new Intent(Intent.ACTION_VIEW,authUri);
                        intent.addCategory(Intent.CATEGORY_BROWSABLE);
                        activity.startActivity(intent);
                    }catch(Exception e){
                        try{server.close();}catch(Exception ignored){}
                    }
                });

                Uri returned=null;
                for(int attempts=0;attempts<5 && returned==null;attempts++){
                    try(Socket socket=server.accept()){
                        socket.setSoTimeout(12000);
                        BufferedReader input=new BufferedReader(new InputStreamReader(
                                socket.getInputStream(),StandardCharsets.US_ASCII));
                        String line=input.readLine();
                        if(line==null || !line.startsWith("GET /auth/callback?")) {
                            respond(socket,false);
                            continue;
                        }
                        String[] fields=line.split(" ",3);
                        if(fields.length<2 || fields[1].length()>10000){respond(socket,false);continue;}
                        Uri candidate=Uri.parse("http://127.0.0.1"+fields[1]);
                        if(!"/auth/callback".equals(candidate.getPath())){respond(socket,false);continue;}
                        if(!state.equals(candidate.getQueryParameter("state"))){
                            respond(socket,false);continue;
                        }
                        respond(socket,true);
                        returned=candidate;
                    }
                }
                if(returned==null)throw new IllegalStateException("Не получен ответ авторизации");
                String denied=returned.getQueryParameter("error");
                if(denied!=null)throw new IllegalStateException("Авторизация отклонена: "+denied);
                String code=returned.getQueryParameter("code");
                if(code==null || code.isEmpty())throw new IllegalStateException("Не получен OAuth-код");
                String issued=returned.getQueryParameter("client_id");
                if(newClient){
                    if(issued==null || issued.isEmpty() || "dynamic_agent_client".equals(issued))
                        throw new IllegalStateException("Сервер не выдал идентификатор приложения");
                    client=issued;
                }else if(issued!=null && !issued.equals(savedClient)){
                    throw new SecurityException("OAuth-клиент не соответствует сохранённому");
                }
                JSONObject tokens=postForm(TOKEN,new String[][]{
                        {"grant_type","authorization_code"},{"client_id",client},
                        {"code",code},{"code_verifier",verifier},
                        {"redirect_uri",redirect},{"resource",RESOURCE}
                });
                String idToken=tokens.optString("id_token","");
                JSONObject identity=verifyIdToken(idToken,client,nonce);
                String subject=identity.optString("sub","");
                if(subject.isEmpty())throw new SecurityException("ID-токен без идентификатора пользователя");
                String previously=old.optString("subject","");
                if(!previously.isEmpty() && !previously.equals(subject))
                    throw new SecurityException("Выполнен вход в другой аккаунт. Сначала отключите предыдущий.");
                String scope=tokens.optString("scope","");
                JSONObject session=new JSONObject();
                session.put("client_id",client);
                session.put("subject",subject);
                session.put("email",identity.optString("email",""));
                session.put("id_token",idToken);
                session.put("scope",scope);
                session.put("access_token",tokens.optString("access_token",""));
                session.put("refresh_token",tokens.optString("refresh_token",""));
                session.put("expires_at",System.currentTimeMillis()+Math.max(60,tokens.optLong("expires_in",3600))*1000L);
                // Save identity and any separately granted scopes without claiming plan access.
                AiOAuthVault.save(context,session);
                boolean authorized=hasScope(scope,"chatgpt.tokens.use.direct");
                notify(callback,authorized,authorized
                        ? "ChatGPT подключён. Разрешено использование лимита подписки."
                        : "Вход выполнен, но использование подписки не разрешено. Повтори вход с разрешением.");
            }catch(Exception e){notify(callback,false,"Вход через ChatGPT: "+safeError(e));}
            finally{listener=null;signingIn=false;}
        });
    }

    private static void respond(Socket socket,boolean accepted)throws Exception{
        String page=accepted ?
                "<!doctype html><html lang='ru'><meta charset='utf-8'><body style='font-family:sans-serif;background:#10151a;color:#eee;padding:30px'><h2>Вход передан в SKYRIM TEXT RPG</h2><p>Вернись в приложение, чтобы завершить подключение.</p></body></html>" :
                "<!doctype html><html lang='ru'><meta charset='utf-8'><body>Запрос не принят.</body></html>";
        byte[] body=page.getBytes(StandardCharsets.UTF_8);
        String headers="HTTP/1.1 "+(accepted?"200 OK":"400 Bad Request")+"\r\n"+
                "Content-Type: text/html; charset=utf-8\r\n"+
                "Content-Length: "+body.length+"\r\n"+
                "Cache-Control: no-store\r\nConnection: close\r\n\r\n";
        OutputStream out=socket.getOutputStream();
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(body);out.flush();
    }

    private void notify(StatusListener listener,boolean good,String message){
        ui.post(()->listener.finished(good,message));
    }

    void getToken(TokenListener listener){
        worker.execute(()->{
            try{
                JSONObject session=AiOAuthVault.load(context);
                if(!hasScope(session.optString("scope",""),"chatgpt.tokens.use.direct"))
                    throw new IllegalStateException("Подписка ChatGPT не разрешена для приложения");
                String token=session.optString("access_token","");
                if(token.isEmpty())throw new IllegalStateException("Не найден токен ChatGPT");
                if(session.optLong("expires_at",0)<System.currentTimeMillis()+90000L){
                    String refresh=session.optString("refresh_token","");
                    if(refresh.isEmpty())throw new IllegalStateException("Нужен повторный вход через ChatGPT");
                    JSONObject data=postForm(TOKEN,new String[][]{
                            {"grant_type","refresh_token"},{"client_id",session.getString("client_id")},
                            {"refresh_token",refresh},{"resource",RESOURCE}
                    });
                    String scope=data.optString("scope",session.optString("scope",""));
                    if(!hasScope(scope,"chatgpt.tokens.use.direct"))
                        throw new IllegalStateException("Отозвано разрешение использования ChatGPT");
                    token=data.getString("access_token");
                    session.put("scope",scope);
                    session.put("access_token",token);
                    if(data.has("refresh_token"))session.put("refresh_token",data.getString("refresh_token"));
                    if(data.has("id_token"))session.put("id_token",data.getString("id_token"));
                    session.put("expires_at",System.currentTimeMillis()+Math.max(60,data.optLong("expires_in",3600))*1000L);
                    AiOAuthVault.save(context,session);
                }
                final String ready=token;
                ui.post(()->listener.token(ready,null));
            }catch(Exception e){String err=safeError(e);ui.post(()->listener.token(null,err));}
        });
    }

    void loadModels(ModelListener listener){
        getToken((token,error)->{
            if(error!=null){listener.models(Collections.emptyList(),error);return;}
            worker.execute(()->{
                try{
                    HttpURLConnection conn=(HttpURLConnection)new URL(RESOURCE+"/models").openConnection();
                    conn.setRequestProperty("Authorization","Bearer "+token);
                    conn.setConnectTimeout(12000);conn.setReadTimeout(18000);
                    try{
                        if(conn.getResponseCode()!=200)throw new IllegalStateException("Модели ChatGPT недоступны (HTTP "+conn.getResponseCode()+")");
                        // The account-specific /models catalog can exceed 500 KB.
                        // Parse it incrementally instead of materializing the whole JSON.
                        List<ModelOption> options=readModelCatalog(conn.getInputStream());
                        ui.post(()->listener.models(options,null));
                    }finally{conn.disconnect();}
                }catch(Exception ex){String err=safeError(ex);ui.post(()->listener.models(Collections.emptyList(),err));}
            });
        });
    }

    void signOut(StatusListener callback){
        worker.execute(()->{
            boolean remote=false;
            try{
                JSONObject old=AiOAuthVault.load(context);
                String refresh=old.optString("refresh_token","");
                if(!refresh.isEmpty()){
                    JSONObject config=getJson(DISCOVERY);
                    String endpoint=config.optString("revocation_endpoint","");
                    if(endpoint.startsWith("https://auth.openai.com/")){
                        postForm(endpoint,new String[][]{
                                {"token",refresh},{"token_type_hint","refresh_token"},
                                {"client_id",old.optString("client_id","")}
                        });
                        remote=true;
                    }
                }else remote=true;
            }catch(Exception ignored){}
            finally{AiOAuthVault.clear(context);}
            notify(callback,true,remote?"Выход из ChatGPT выполнен.":"Локальный выход выполнен; отзыв удалённой сессии не подтверждён.");
        });
    }

    private JSONObject verifyIdToken(String jwt,String client,String expectedNonce)throws Exception{
        String[] parts=jwt.split("\\.");
        if(parts.length!=3)throw new SecurityException("Неполный ID-токен");
        JSONObject header=new JSONObject(new String(Base64.decode(parts[0],Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING),StandardCharsets.UTF_8));
        JSONObject body=new JSONObject(new String(Base64.decode(parts[1],Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING),StandardCharsets.UTF_8));
        if(!"RS256".equals(header.optString("alg","")))throw new SecurityException("Неподдерживаемая подпись ID-токена");
        JSONObject discovery=getJson(DISCOVERY);
        if(!"https://auth.openai.com".equals(discovery.optString("issuer","")))
            throw new SecurityException("Неизвестный издатель токена");
        String jwks=discovery.optString("jwks_uri","");
        if(!jwks.startsWith("https://auth.openai.com/"))
            throw new SecurityException("Неизвестный источник подписей");
        JSONArray keys=getJson(jwks).getJSONArray("keys");
        boolean verified=false;
        for(int i=0;i<keys.length();i++){
            JSONObject jwk=keys.optJSONObject(i);
            if(jwk==null || !header.optString("kid","").equals(jwk.optString("kid","")))continue;
            if(!"RSA".equals(jwk.optString("kty","")))continue;
            BigInteger n=new BigInteger(1,Base64.decode(jwk.getString("n"),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING));
            BigInteger e=new BigInteger(1,Base64.decode(jwk.getString("e"),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING));
            Signature verifier=Signature.getInstance("SHA256withRSA");
            verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n,e)));
            verifier.update((parts[0]+"."+parts[1]).getBytes(StandardCharsets.US_ASCII));
            verified=verifier.verify(Base64.decode(parts[2],Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING));
            if(verified)break;
        }
        if(!verified)throw new SecurityException("Подпись ID-токена не подтверждена");
        if(!"https://auth.openai.com".equals(body.optString("iss","")))
            throw new SecurityException("Неправильный издатель ID-токена");
        if(!expectedNonce.equals(body.optString("nonce","")))
            throw new SecurityException("Nonce ID-токена не совпадает");
        if(body.optLong("exp",0)*1000L < System.currentTimeMillis())
            throw new SecurityException("ID-токен истёк");
        Object audience=body.opt("aud");
        boolean matches=client.equals(audience);
        if(audience instanceof JSONArray){
            JSONArray arr=(JSONArray)audience;
            for(int i=0;i<arr.length();i++)if(client.equals(arr.optString(i)))matches=true;
        }
        if(!matches)throw new SecurityException("ID-токен предназначен другому приложению");
        return body;
    }

    private String randomUrl(int bytes){
        byte[] raw=new byte[bytes];random.nextBytes(raw);return urlSafe(raw);
    }
    private static String urlSafe(byte[] bytes){
        return Base64.encodeToString(bytes,Base64.URL_SAFE|Base64.NO_PADDING|Base64.NO_WRAP);
    }
    private static String safeError(Exception error){
        String msg=error.getMessage();
        if(msg==null || msg.isEmpty())return "ошибка соединения или проверки токена";
        if(msg.length()>250)msg=msg.substring(0,250);
        return msg;
    }
    private static boolean hasScope(String list,String scope){
        for(String part:list.split("\\s+"))if(scope.equals(part))return true;
        return false;
    }
    /** Keeps memory bounded when the account-specific models catalog is large. */
    private static List<ModelOption> readModelCatalog(InputStream input) throws Exception {
        List<ModelOption> result=new ArrayList<>();
        boolean modelsPresent=false;
        try (JsonReader reader=new JsonReader(new InputStreamReader(
                new LimitedInputStream(input,64L*1024*1024),StandardCharsets.UTF_8))) {
            reader.beginObject();
            while(reader.hasNext()){
                String field=reader.nextName();
                if(!"models".equals(field)){
                    reader.skipValue();
                    continue;
                }
                if(reader.peek()!=JsonToken.BEGIN_ARRAY){
                    reader.skipValue();
                    continue;
                }
                modelsPresent=true;
                reader.beginArray();
                while(reader.hasNext()){
                    // The screen displays at most 100 authorized models.
                    // Stop downloading the rest once we have enough valid entries.
                    if(result.size()>=100)return result;
                    if(reader.peek()!=JsonToken.BEGIN_OBJECT){
                        reader.skipValue();
                        continue;
                    }
                    reader.beginObject();
                    String slug="", display="", visibility="";
                    while(reader.hasNext()){
                        String name=reader.nextName();
                        if("slug".equals(name)){
                            if(reader.peek()==JsonToken.STRING)slug=reader.nextString();
                            else reader.skipValue();
                        }else if("display_name".equals(name)){
                            if(reader.peek()==JsonToken.STRING)display=reader.nextString();
                            else reader.skipValue();
                        }else if("visibility".equals(name)){
                            if(reader.peek()==JsonToken.STRING)visibility=reader.nextString();
                            else reader.skipValue();
                        }else reader.skipValue();
                    }
                    reader.endObject();
                    if(!"list".equals(visibility)||!slug.matches("[a-zA-Z0-9._-]{2,100}"))
                        continue;
                    if(display.isEmpty()||display.length()>150)display=slug;
                    result.add(new ModelOption(slug,display));
                }
                reader.endArray();
            }
            reader.endObject();
        }
        if(!modelsPresent)throw new IOException("Сервер не вернул каталог моделей ChatGPT");
        return result;
    }

    /** Hard network safety limit, separate from the OAuth metadata limit of 500 KB. */
    private static final class LimitedInputStream extends FilterInputStream {
        private final long limit;
        private long consumed;
        LimitedInputStream(InputStream input,long limit){super(input);this.limit=limit;}
        @Override public int read() throws IOException {
            int value=in.read();
            if(value!=-1&&++consumed>limit)throw new IOException("Каталог моделей превысил 64 МБ");
            return value;
        }
        @Override public int read(byte[] data,int offset,int length) throws IOException {
            if(consumed>=limit){
                int probe=in.read();
                if(probe==-1)return -1;
                throw new IOException("Каталог моделей превысил 64 МБ");
            }
            int allowed=(int)Math.min((long)length,limit-consumed);
            int count=in.read(data,offset,allowed);
            if(count>0)consumed+=count;
            return count;
        }
    }

    private static String read(InputStream stream)throws Exception{
        if(stream==null)throw new IllegalStateException("Пустой ответ сервера");
        try(InputStream in=stream; ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int count;
            while((count=in.read(buf))!=-1){
                if(out.size()+count>512000)throw new IllegalStateException("Ответ сервера слишком большой");
                out.write(buf,0,count);
            }
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private static JSONObject getJson(String url)throws Exception{
        HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();
        conn.setConnectTimeout(12000);conn.setReadTimeout(15000);
        try{
            if(conn.getResponseCode()!=200)throw new IllegalStateException("Ошибка конфигурации OpenAI");
            return new JSONObject(read(conn.getInputStream()));
        }finally{conn.disconnect();}
    }
    private static JSONObject postForm(String endpoint,String[][] pairs)throws Exception{
        StringBuilder form=new StringBuilder();
        for(String[] pair:pairs){
            if(form.length()>0)form.append('&');
            form.append(URLEncoder.encode(pair[0],"UTF-8"))
                    .append('=').append(URLEncoder.encode(pair[1],"UTF-8"));
        }
        HttpURLConnection conn=(HttpURLConnection)new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000);conn.setReadTimeout(20000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        byte[] data=form.toString().getBytes(StandardCharsets.UTF_8);
        try(OutputStream out=conn.getOutputStream()){out.write(data);}
        try{
            int status=conn.getResponseCode();
            if(status<200 || status>299)throw new IllegalStateException("OAuth сервер вернул HTTP "+status);
            String response=read(conn.getInputStream());
            return response.trim().isEmpty()?new JSONObject():new JSONObject(response);
        }finally{conn.disconnect();}
    }

    void shutdown(){
        ServerSocket s=listener;
        if(s!=null)try{s.close();}catch(Exception ignored){}
        worker.shutdownNow();
    }
}
