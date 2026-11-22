package dev.infend.skyrimtext;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Work/Codex OAuth: text-only Responses API.
 * Images are deliberately NOT requested through ChatGPT-plan OAuth because
 * Sign in with ChatGPT preview excludes the hosted image-generation tool.
 * Artwork is created separately in ChatGPT Work and imported by the user.
 */
final class ItemWorkshopAi {
    interface Result { void finished(JSONObject item,String error); }
    interface Progress { void stage(int current,int total,String title,String details); }
    private static void report(Progress progress,int current,String title,String details){
        if(progress!=null)MAIN.post(()->progress.stage(current,4,title,details));
    }
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final String RESPONSES="https://api.openai.com/v1/responses";
    private ItemWorkshopAi(){}

    static void draft(String oauthToken,String model,String idea,Result callback){
        draft(oauthToken,model,idea,null,callback);
    }
    static void draft(String oauthToken,String model,String idea,Progress progress,Result callback){
        new Thread(()->{
            HttpURLConnection conn=null;
            try{
                report(progress,1,"Подготовка параметров",
                    "Проверяем модель и описание предмета");
                if(oauthToken==null||oauthToken.isEmpty()||model==null||
                    !model.matches("[a-zA-Z0-9._-]{2,100}"))
                    throw new IllegalArgumentException("Войди в ChatGPT и выбери модель Работа/Codex");
                String prompt=ItemWorkshopSpec.clip(idea,1600);
                if(prompt.isEmpty())throw new IllegalArgumentException("Опиши задумку предмета");
                JSONObject request=new JSONObject();
                request.put("model",model).put("store",false).put("stream",true)
                  .put("instructions","Ты создаёшь предметы для текстовой RPG в сеттинге Skyrim. "+
                     "Верни только один компактный JSON без markdown: "+
                     "{name:string,type:int,stat:int,weight:number,value:int,rarity:int,description:string,effect:string,art_prompt:string}. "+
                     "type: "+typeList()+". "+
                     "stat от 0 до 500, weight от 0.01 до 50, value от 0 до 999999, rarity от 0 до 5. "+
                     "description до 400 знаков, effect до 400 знаков, art_prompt до 900 знаков. "+
                     "Используй русские названия, не копируй название реального уникального предмета, если он новый. "+
                     "effect только описание и художественная идея, не обещай механических эффектов, которых нет в игре. "+
                     "art_prompt описывает ОДИН предмет, изолированный на тёмном фоне, без букв и рамки. "+
                     "Не выдумывай состоявшиеся игровые награды или изменения инвентаря.")
                  .put("input",new JSONArray().put(new JSONObject().put("role","user").put("content",prompt)));
                report(progress,2,"Отправка запроса",
                    "Передаём описание модели "+model);
                conn=open(RESPONSES,oauthToken,"text/event-stream");
                write(conn,request);
                report(progress,3,"Ожидание ответа",
                    "Запрос отправлен, ожидаем поток ответа ИИ");
                check(conn);
                StringBuilder result=new StringBuilder();boolean finished=false;
                int lastReported=0;
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream(),StandardCharsets.UTF_8))){
                    String line;
                    while((line=reader.readLine())!=null){
                        if(!line.startsWith("data:"))continue;
                        String raw=line.substring(5).trim();
                        if(raw.isEmpty()||raw.equals("[DONE]"))continue;
                        JSONObject event=new JSONObject(raw);
                        String type=event.optString("type","");
                        if("response.failed".equals(type)||"response.incomplete".equals(type)||
                           "error".equals(type))throw new IllegalStateException("ChatGPT не завершил создание предмета");
                        if("response.output_text.delta".equals(type)){
                            result.append(event.optString("delta",""));
                            if(result.length()>8000)throw new IllegalStateException("Слишком длинный ответ ИИ");
                            if(lastReported==0||result.length()-lastReported>=96){
                                lastReported=result.length();
                                report(progress,3,"Получение параметров",
                                    "Получено "+result.length()+" символов от ИИ");
                            }
                        }
                        if("response.completed".equals(type))finished=true;
                    }
                }
                if(!finished)throw new IllegalStateException("Ответ прерван до завершения");
                report(progress,4,"Проверка ответа",
                    "Принято "+result.length()+" символов. Проверяем JSON и ограничения.");
                String s=result.toString().trim();
                if(s.startsWith("```"))s=s.replaceFirst("(?s)^\\`\\`\\`(?:json)?\\s*","")
                    .replaceFirst("(?s)\\s*\\`\\`\\`$","");
                JSONObject item=new JSONObject(s);
                String name=ItemWorkshopSpec.clip(item.optString("name",""),60);
                if(name.isEmpty())throw new IllegalStateException("ИИ не предложил название");
                JSONObject safe=new JSONObject();
                int type=ItemWorkshopSpec.normalizeType(item.optInt("type",0));
                double weight=item.optDouble("weight",1);
                if(!Double.isFinite(weight))weight=1;
                safe.put("name",name).put("type",type)
                    .put("stat",ItemWorkshopSpec.normalizedStat(type,item.optInt("stat",0)))
                    .put("weight",Math.max(.01,Math.min(50,weight)))
                    .put("value",Math.max(0,Math.min(999999,item.optInt("value",0))))
                    .put("rarity",ItemWorkshopSpec.normalizeRarity(item.optInt("rarity",0)))
                    .put("description",ItemWorkshopSpec.clip(item.optString("description",""),400))
                    .put("effect",ItemWorkshopSpec.clip(item.optString("effect",""),400))
                    .put("art_prompt",ItemWorkshopSpec.clip(item.optString("art_prompt",""),900));
                MAIN.post(()->callback.finished(safe,null));
            }catch(Exception e){
                String error=brief(e);MAIN.post(()->callback.finished(null,error));
            }finally{if(conn!=null)conn.disconnect();}
        },"skyrim-item-workshop-text").start();
    }

    private static String typeList(){
        StringBuilder s=new StringBuilder();
        for(int i=0;i<ItemWorkshopSpec.TYPES.length;i++){
            if(i>0)s.append(", ");
            s.append(i).append("=").append(ItemWorkshopSpec.TYPES[i]);
        }
        return s.toString();
    }
    private static HttpURLConnection open(String url,String key,String accept)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setRequestMethod("POST");c.setConnectTimeout(16000);c.setReadTimeout(150000);
        c.setRequestProperty("Authorization","Bearer "+key);
        c.setRequestProperty("Content-Type","application/json; charset=utf-8");
        c.setRequestProperty("Accept",accept);c.setDoOutput(true);
        return c;
    }
    private static void write(HttpURLConnection c,JSONObject request)throws Exception{
        try(OutputStream out=c.getOutputStream()){
            out.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
    private static void check(HttpURLConnection c)throws Exception{
        int code=c.getResponseCode();
        if(code>=200&&code<300)return;
        String message="Ошибка сервиса: HTTP "+code;
        if(code==401)message="Не прошла авторизация: проверь подключение и разрешения";
        if(code==403)message="Нет разрешения для этой функции или модели";
        if(code==429)message="Достигнут лимит запросов или средств";
        throw new IllegalStateException(message);
    }
    private static String brief(Exception error){
        String s=error.getMessage();
        if(s==null||s.isEmpty())s="Не удалось завершить запрос";
        return s.length()>220?s.substring(0,220):s;
    }
}
