package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.Locale;

/** Optional crossover story. All progression belongs to the save, not the UI. */
final class KatherineStory {
    static final String ID="katherine_zero_limit", MODE="KATHERINE", NAME="Кэтрин";
    int stage=0, trust=0;
    boolean enabled=true;
    static boolean atHut(GameEngine g){
        String s=g.progress.location.toLowerCase(Locale.ROOT).replace('ё','е');
        return s.contains("анис") && (s.contains("хижин")||s.contains("подвал"));
    }
    boolean available(GameEngine g){return enabled && g.started && "WORLD".equals(g.mode) && atHut(g);}
    String status(){return stage==0?"Не встречена":stage==1?"Знакомство":stage==2?"Доверие":stage==3?"Союзница":"Раскрыта тайна Круга";}
    JSONObject save() throws JSONException{return new JSONObject().put("enabled",enabled).put("stage",stage).put("trust",trust);}
    static KatherineStory restore(JSONObject j){
        KatherineStory k=new KatherineStory();
        if(j!=null){k.enabled=j.optBoolean("enabled",true);k.stage=Math.max(0,Math.min(4,j.optInt("stage")));k.trust=Math.max(-100,Math.min(100,j.optInt("trust")));}
        return k;
    }
    GameEngine.Scene scene(){
        GameEngine.Scene s;
        switch(stage){
            case 0:s=new GameEngine.Scene("НАСЛЕДИЕ ДВУХ МИРОВ","Хижина Анис","В подвале пахнет сырой древесиной. У лестницы сидит девушка с длинными серебристыми волосами и янтарными глазами. Рядом с ней лежит бессознательный Деймон. Она настороженно поднимается, не отходя от него.\n\nКэтрин:\n— Стой там. Сначала скажи, кто ты и зачем пришёл.");
                s.add("KATH:help","Предложить помощь Деймону","Показать, что ты не угроза");s.add("KATH:intro","Представиться","Не приближаться без разрешения");break;
            case 1:s=new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Разговор у лестницы","Кэтрин поправляет одеяло Деймона и внимательно следит за твоими движениями.\n\nКэтрин:\n— Я не собираюсь бросать его. Если хочешь помочь, говори прямо.");
                s.add("KATH:listen","Выслушать её","Не давить и не требовать объяснений");s.add("KATH:promise","Предложить охранять вход","Соблюдать её границы");break;
            case 2:s=new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Необычный отклик","На мгновение пламя свечи дрожит. Кэтрин с удивлением смотрит на собственную ладонь.\n\nКэтрин:\n— Ты тоже это заметил? Я не знаю, что со мной происходит.");
                s.add("KATH:study","Предложить исследовать аномалию","Не выдавать гипотезу за доказательство");s.add("KATH:wait","Не торопиться с выводами","Дать ей возможность решать самой");break;
            case 3:s=new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Тень Круга","Кэтрин вспоминает упоминание Нулевого Предела в документах Круга. Те, кто ищет её, хотят захватить её живой.\n\nКэтрин:\n— Я не вещь, которую можно забрать. И не собираюсь никому принадлежать.");
                s.add("KATH:resolve","Обсудить безопасный план","Сохранить её свободу выбора");break;
            default:s=new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Кэтрин","Серебристые волосы обрамляют её лицо. Кэтрин встречает тебя спокойным, внимательным взглядом.\n\nКэтрин:\n— Ты снова здесь. Что на этот раз?\n\nДоверие: "+trust+"/100. Природа драконьего отклика пока неизвестна.");break;
        }
        return s.add("KATH:exit","Вернуться","Продолжить путешествие");
    }
    void choose(GameEngine g,String action){
        if("KATH:open".equals(action)){if(available(g))g.mode=MODE;return;}
        if(!enabled||!MODE.equals(g.mode)||!atHut(g))return;
        if("KATH:exit".equals(action)){g.mode="WORLD";return;}
        int delta=0;
        switch(stage){
            case 0:if("KATH:help".equals(action))delta=12;else if("KATH:intro".equals(action))delta=6;break;
            case 1:if("KATH:listen".equals(action))delta=10;else if("KATH:promise".equals(action))delta=8;break;
            case 2:if("KATH:study".equals(action))delta=8;else if("KATH:wait".equals(action))delta=10;break;
            case 3:if("KATH:resolve".equals(action))delta=12;break;
        }
        if(delta==0)return;
        trust=Math.min(100,trust+delta);stage++;
        g.progress.setQuestOutcome(ID,stage==4?"COMPLETED":"ACTIVE");
        try { g.sceneState.setContext(g.day,g.hour,g.progress.location);
        g.sceneState.confirm("npc",ID,NAME,stage>=3?"HELPED":"MET"); } catch (JSONException ignored) {}
        g.message="Кэтрин: "+status()+". Доверие "+trust+"/100.";
    }
}
