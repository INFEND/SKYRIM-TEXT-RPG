package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;

/** Authored daily itineraries for existing Skyrim residents, not exact original AI packages. */
final class SolitudeResidents {
    private SolitudeResidents(){}
    private static final String[] NAMES={"Элисиф Прекрасная","Сорекс Виний","Аддвар","Пантея Атейя","Сибилла Стентор"};
    private static final String[] IDS={"elisif","sorex","addvar","pantea","sybille"};
    private static final String[] LINES={
        "Хаафингар нуждается в спокойствии. В Синем дворце я выслушиваю тех, кто приходит с прошениями.",
        "В трактире всегда найдётся работа. Путники приносят вести из всех владений.",
        "Свежую рыбу лучше выбирать утром. Море не ждёт, пока торговец выспится.",
        "Музыка требует ежедневной практики. Даже знакомую мелодию можно сыграть лучше.",
        "При дворе ценят осторожность. Магия требует не только силы, но и знаний."
    };
    // Half-open time windows; -1 means off duty and unavailable for public dialogue.
    static int district(int i,int hour){
        if(hour<0||hour>=24)return -1;
        switch(i){
            case 0:return hour>=9&&hour<17?2:-1;
            case 1:return hour>=8&&hour<10?0:hour>=10&&hour<23?6:-1;
            case 2:return hour>=6&&hour<8?7:hour>=8&&hour<17?0:-1;
            case 3:return hour>=9&&hour<17?3:hour>=17&&hour<20?6:-1;
            case 4:return hour>=10&&hour<18?2:-1;
            default:return -1;
        }
    }
    private static boolean present(GameEngine g,int i,int d){
        if(d<0||i<0||i>=NAMES.length||district(i,g.hour)!=d)return false;
        String status=g.sceneState.npcStatusByName(NAMES[i]);
        return !"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
    }
    private static JSONObject fact(GameEngine g,int i){
        JSONArray a=g.sceneState.npcFacts();
        for(int k=0;k<a.length();k++){JSONObject f=a.optJSONObject(k);if(f!=null&&NAMES[i].equalsIgnoreCase(f.optString("title")))return f;}
        return null;
    }
    static void choices(GameEngine g,GameEngine.Scene s,int d){
        for(int i=0;i<NAMES.length;i++)if(present(g,i,d)){
            JSONObject f=fact(g,i);
            s.add("SOLITUDE:resident:talk:"+i,"Поговорить: "+NAMES[i],f==null?"Авторский диалог | Знакомство":"Отношение: "+f.optInt("relation",50)+"/100");
            if(f!=null)s.add("SOLITUDE:resident:memory:"+i,"Вспомнить встречу: "+NAMES[i],"Общая память NPC");
        }
    }
    static boolean choose(GameEngine g,int d,String a){
        if(!a.startsWith("SOLITUDE:resident:"))return false;
        for(int i=0;i<NAMES.length;i++)if(present(g,i,d)){
            boolean memory=a.equals("SOLITUDE:resident:memory:"+i);
            if(!memory&&!a.equals("SOLITUDE:resident:talk:"+i))continue;
            JSONObject f=fact(g,i);if(memory&&f==null)return true;
            try{
                g.sceneState.setContext(g.day,g.hour,"Солитьюд");
                if(f==null)g.sceneState.confirm("npc","solitude_"+IDS[i],NAMES[i],"MET");
                else g.sceneState.revisitNpc(f.optString("id"));
            }catch(org.json.JSONException ignored){}
            g.message=NAMES[i]+": "+(memory?"Отношение: "+f.optInt("relation",50)+"/100 | Последняя встреча: день "+f.optInt("lastDay"):(f==null?"":"Снова встретились. ")+LINES[i]);
            g.progress.recent=g.message;g.recordVisualEvent(g.message);g.progress.sync(g);return true;
        }
        return true;
    }
}
