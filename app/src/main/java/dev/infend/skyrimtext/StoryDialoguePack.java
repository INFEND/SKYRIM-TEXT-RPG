package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

/** Read-only, locally imported story. No game-state mutations or network access. */
public final class StoryDialoguePack {
    public static final int MAX_BYTES=4*1024*1024;
    public static final int PAGE_SIZE=8;
    public static final class Line {
        public final int id,sourceLine;
        public final String speaker, text;
        public final boolean uncertain;
        Line(int id,int sourceLine,String speaker,String text,boolean uncertain){
            this.id=id;this.sourceLine=sourceLine;this.speaker=speaker;this.text=text;this.uncertain=uncertain;
        }
    }
    public static final class Chapter {
        public final int id;
        public final String title;
        private final ArrayList<Line> lines=new ArrayList<>();
        Chapter(int id,String title){this.id=id;this.title=title;}
        public List<Line> lines(){return Collections.unmodifiableList(lines);}
        public int pages(){return Math.max(1,(lines.size()+PAGE_SIZE-1)/PAGE_SIZE);}
        public int page(int requested){return Math.max(0,Math.min(requested,pages()-1));}
    }
    private final List<Chapter> chapters;
    public final int segments,uncertainSegments;
    private StoryDialoguePack(List<Chapter> chapters,int segments,int uncertain){
        this.chapters=Collections.unmodifiableList(chapters);
        this.segments=segments;this.uncertainSegments=uncertain;
    }
    public List<Chapter> chapters(){return chapters;}
    public Chapter chapter(int id){for(Chapter c:chapters)if(c.id==id)return c;return chapters.get(0);}
    private static String required(JSONObject obj,String key,int max)throws JSONException{
        Object v=obj.opt(key);
        if(!(v instanceof String))throw new JSONException("Нет поля "+key);
        String s=((String)v).trim();
        if(s.isEmpty()||s.length()>max)throw new JSONException("Некорректное поле "+key);
        return s;
    }
    public static StoryDialoguePack parse(String json)throws JSONException{
        if(json==null||json.length()>MAX_BYTES)throw new JSONException("Файл слишком большой");
        JSONObject root=new JSONObject(json);
        JSONArray scenes=root.getJSONArray("scenes"),segments=root.getJSONArray("segments");
        if(scenes.length()<1||scenes.length()>200||segments.length()<1||segments.length()>5000)
            throw new JSONException("Неверный размер сюжетной базы");
        LinkedHashMap<Integer,Chapter> byId=new LinkedHashMap<>();
        for(int i=0;i<scenes.length();i++){
            JSONObject s=scenes.getJSONObject(i);int id=s.getInt("id");
            if(id<=0||byId.containsKey(id))throw new JSONException("Повтор номера главы");
            byId.put(id,new Chapter(id,required(s,"title",250)));
        }
        HashSet<Integer> ids=new HashSet<>();
        int count=0,uncertain=0;
        for(int i=0;i<segments.length();i++){
            JSONObject s=segments.getJSONObject(i);int id=s.getInt("id");
            if(id<=0||!ids.add(id))throw new JSONException("Повтор номера реплики");
            Chapter c=byId.get(s.getInt("scene_id"));
            if(c==null)throw new JSONException("Неизвестная глава");
            if(s.optBoolean("exclude_from_reading",false))continue;
            boolean review=s.optBoolean("requires_manual_review",false);
            JSONArray turns=s.optJSONArray("utterances");
            if(turns!=null&&turns.length()>0){
                if(turns.length()>32)throw new JSONException("Слишком много реплик в записи");
                for(int j=0;j<turns.length();j++){
                    JSONObject u=turns.getJSONObject(j);
                    c.lines.add(new Line(id,s.optInt("line_start",0),required(u,"speaker",250),required(u,"text",8192),review));
                }
            }else{
                c.lines.add(new Line(id,s.optInt("line_start",0),required(s,"speaker",250),required(s,"reading_text",8192),review));
            }
            count++;if(review)uncertain++;
        }
        ArrayList<Chapter> chapters=new ArrayList<>();
        for(Chapter c:byId.values())if(!c.lines.isEmpty())chapters.add(c);
        if(chapters.isEmpty())throw new JSONException("Нет доступных реплик");
        return new StoryDialoguePack(chapters,count,uncertain);
    }
}
