package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.Locale;

/** Optional original questline. Decisions are authoritative, saved and replay-safe. */
final class AlmaStory {
    static final String ID="alma_crimson_immortal", MODE="ALMA", NAME="Альма";
    static final String[] TITLES={"Незнакомка под багровой луной","Тайна алых глаз","Кровь на руках","Запретные знания","Контракт Тёмного Братства","Призраки прошлого","По ту сторону Обливиона","Цена бессмертия","Сердце вампира","Между любовью и смертью","Последний договор","Вечность начинается сегодня"};
    static final String[] BODY={
        "У канала Рифтена стоит незнакомка с длинными багровыми волосами. Серебряный амулет ловит отблеск фонаря. Она замечает твой взгляд и отступает к стене.\n\nАльма:\n— Если ищешь неприятностей, сегодня не твой вечер. Если ищешь ответы, возможно, наш.",
        "Альма показывает клыки, не приближаясь. Она признаётся, что избегает Волкихар и не питается случайными прохожими.\n\nАльма:\n— Я вампир. Можешь уйти. Но не называй меня чудовищем, пока не узнаешь, что я сделала.",
        "Она достаёт печать с вырезанной чёрной ладонью. В прошлом Альма исполняла контракты Тёмного Братства, а затем отказалась убить невиновного.\n\nАльма:\n— Прошлое нельзя вырезать ножом. Я хочу отвечать за него сама.",
        "В старой книге Коллегии Винтерхолда описано смешение даэдрической и жизненной магии. Альма объясняет, как смертельная болезнь заставила её искать запретные знания.\n\nАльма:\n— Я думала, что лечу себя. На самом деле открыла дверь.",
        "Альма обнаруживает послание Братства. За её голову назначена награда. Она предлагает решить, стоит ли предупредить возможных жертв.\n\nАльма:\n— Я не прошу тебя убивать ради меня.",
        "В её дневнике перечислены имена людей, которых она не сумела спасти. Среди страниц спрятан ключ к договору.\n\nАльма:\n— Четыре века — слишком долгий срок, чтобы помнить только собственную боль.",
        "Серебряный амулет открывает видение Обливиона. В нём Альма видит своё отражение с иной, даэдрической природой.\n\nАльма:\n— Это часть меня, но не вся я. Не дай ей решать за нас.",
        "Из текста договора становится ясно: после гибели её душа возвращается, но каждое возвращение отдаёт часть памяти неизвестной сущности.\n\nАльма:\n— Бессмертие — не свобода. Я устала платить за него.",
        "Впервые за долгие годы Альма говорит о будущем. Она предлагает остаться рядом, не требуя обещаний.\n\nАльма:\n— Мне важно знать, чего хочешь ты. И я тоже хочу иметь право выбирать.",
        "Печать договора начинает трескаться. Альма предлагает готовиться к последнему выбору, а не бросаться навстречу опасности.\n\nАльма:\n— Не жертвуй собой ради красивой легенды. Мне нужен живой союзник.",
        "Условие договора найдено: отказаться от силы, разделить её по обоюдному согласию или принять даэдрическую природу. Ни один путь не отменяет последствий.\n\nАльма:\n— Я могу выбрать сама. Но хочу услышать тебя.",
        "Последние строки договора мерцают. Альма держит амулет на раскрытой ладони.\n\nАльма:\n— Какую бы дорогу мы ни выбрали, она должна быть нашей, а не навязанной."
    };
    static final String[] GOOD={"Представиться и выслушать","Принять её признание","Попросить рассказать о жертвах","Изучить записи вместе","Предупредить возможных жертв","Помочь разобрать дневник","Поддержать её выбор","Обсудить цену договора","Предложить союз без условий","Составить безопасный план","Обсудить свободный выбор"};
    static final String[] COOL={"Сохранить дистанцию","Признать, что нужно время","Не оправдывать прошлое","Попросить проверить выводы","Избежать столкновения","Не расспрашивать о прошлом","Остаться наблюдателем","Отложить решение","Остаться знакомыми","Предложить осторожность","Не вмешиваться в её решение"};
    boolean enabled=true, romance=false, married=false, companion=false;
    int stage=0, trust=0, respect=0, affection=0, love=0, jealousy=0, hurt=0, loyalty=0, lastTalkDay=-1;
    String ending="";
    static int clamp(int n){return Math.max(0,Math.min(100,n));}
    static boolean atRiften(GameEngine g){String l=g.progress.location.toLowerCase(Locale.ROOT).replace('ё','е');return l.contains("рифтен");}
    boolean available(GameEngine g){return enabled&&g.started&&"WORLD".equals(g.mode)&&!terminalLoss()&&(atRiften(g)||(stage>0&&companion));}
    String status(){return stage==0?"Не встречена":stage<TITLES.length?TITLES[stage]:ending;}
    String relationship(){if(stage==0)return "Незнакомка";if("Разбитая клятва".equals(ending))return "Противник";if(married)return "Супруга";if(romance&&love>=70)return "Возлюбленная";if(romance)return "Влюблённость";if(trust>=45)return "Друг";return "Знакомая";}
    boolean canRomance(){return stage>=8&&trust>=45&&affection>=35&&!terminalLoss();}
    boolean terminalLoss(){return "Последняя жертва".equals(ending)||"Разбитая клятва".equals(ending);}
    JSONObject save() throws JSONException{return new JSONObject().put("enabled",enabled).put("stage",stage).put("trust",trust).put("respect",respect).put("affection",affection).put("love",love).put("jealousy",jealousy).put("hurt",hurt).put("loyalty",loyalty).put("romance",romance).put("married",married).put("companion",companion).put("lastTalkDay",lastTalkDay).put("ending",ending);}
    static AlmaStory restore(JSONObject j){AlmaStory a=new AlmaStory();if(j==null)return a;
        a.enabled=j.optBoolean("enabled",true);a.stage=Math.max(0,Math.min(TITLES.length,j.optInt("stage")));
        a.trust=clamp(j.optInt("trust"));a.respect=clamp(j.optInt("respect"));a.affection=clamp(j.optInt("affection"));a.love=clamp(j.optInt("love"));a.jealousy=clamp(j.optInt("jealousy"));a.hurt=clamp(j.optInt("hurt"));a.loyalty=clamp(j.optInt("loyalty"));
        a.romance=j.optBoolean("romance")&&a.stage>=8;a.married=j.optBoolean("married")&&a.romance;a.companion=j.optBoolean("companion")&&a.stage>=3;a.lastTalkDay=j.optInt("lastTalkDay",-1);a.ending=a.stage==TITLES.length?j.optString("ending","Новая жизнь"):"";return a;}
    GameEngine.Scene scene(GameEngine g){
        String stats="\n\nОтношения: "+relationship()+" | Доверие "+trust+"/100 | Симпатия "+affection+"/100 | Любовь "+love+"/100";
        GameEngine.Scene s=new GameEngine.Scene("АЛАЯ БЕССМЕРТНАЯ",stage<TITLES.length?TITLES[stage]:ending,(stage<TITLES.length?BODY[stage]:"Альма завершила свою историю. "+(terminalLoss()?"Ваши пути разошлись.":"Её дальнейшие решения принадлежат ей."))+stats);
        if(stage<11){s.add("ALMA:good:"+stage,GOOD[stage],"Продолжить историю");s.add("ALMA:cool:"+stage,COOL[stage],"Осторожный выбор");}
        if(stage==11){s.add("ALMA:finish:mortal","Освободить Альму от проклятия","Новая жизнь");
            if(romance&&love>=60)s.add("ALMA:finish:eternal","Предложить разделить бессмертие","Вечность вдвоём");
            s.add("ALMA:finish:queen","Принять её даэдрическую природу","Алая королева");
            s.add("ALMA:finish:part","Разойтись и разрушить клятву","Разбитая клятва");
            if(trust>=65)s.add("ALMA:finish:sacrifice","Принять её решение о жертве","Последняя жертва");}
        if(stage>=3&&!terminalLoss())s.add(companion?"ALMA:dismiss":"ALMA:join",companion?"Попросить ждать в Рифтене":"Предложить путешествовать вместе","Союзница");
        if(canRomance()&&!romance&&hurt<15)s.add("ALMA:romance","Признаться в чувствах","Только при взаимной симпатии");
        if(!romance&&hurt>=15&&lastTalkDay!=g.day&&!terminalLoss())s.add("ALMA:reconcile","Поговорить о прошлом","Не обещает возобновления романа");
        if(romance&&!terminalLoss()){
            if(lastTalkDay!=g.day)s.add("ALMA:date","Провести время вместе","Один раз за игровой день");
            if(!married&&love>=85&&trust>=75&&stage>=10)s.add("ALMA:marry","Предложить брак","Взаимное согласие");
            s.add("ALMA:breakup","Поговорить о расставании","Завершить роман");
        }
        return s.add("ALMA:exit","Вернуться","Продолжить путешествие");
    }
    void choose(GameEngine g,String action){
        if("ALMA:open".equals(action)){if(available(g))g.mode=MODE;return;}
        if(!enabled||!MODE.equals(g.mode))return;
        if("ALMA:exit".equals(action)){g.mode="WORLD";return;}
        if(!atRiften(g)&&!companion)return;
        boolean advance=stage<11&&(action.equals("ALMA:good:"+stage)||action.equals("ALMA:cool:"+stage));
        if(advance){boolean good=action.startsWith("ALMA:good:");trust=clamp(trust+(good?9:4));respect=clamp(respect+(good?8:5));affection=clamp(affection+(good?7:2));loyalty=clamp(loyalty+(good?6:2));if(romance)love=clamp(love+(good?9:3));stage++;}
        else if("ALMA:join".equals(action)&&stage>=3&&!terminalLoss()&&!companion)companion=true;
        else if("ALMA:dismiss".equals(action)&&companion){companion=false;g.mode="WORLD";}
        else if("ALMA:romance".equals(action)&&canRomance()&&!romance&&hurt<15){romance=true;love=Math.max(love,45);}
        else if("ALMA:date".equals(action)&&romance&&!terminalLoss()&&lastTalkDay!=g.day){lastTalkDay=g.day;love=clamp(love+8);trust=clamp(trust+3);affection=clamp(affection+4);hurt=clamp(hurt-5);}
        else if("ALMA:marry".equals(action)&&romance&&!married&&love>=85&&trust>=75&&stage>=10&&!terminalLoss())married=true;
        else if("ALMA:breakup".equals(action)&&romance){romance=false;married=false;love=0;hurt=clamp(hurt+15);lastTalkDay=g.day;}
        else if("ALMA:reconcile".equals(action)&&!romance&&hurt>=15&&lastTalkDay!=g.day&&!terminalLoss()){hurt=clamp(hurt-5);lastTalkDay=g.day;}
        else if(stage==11&&action.startsWith("ALMA:finish:")){
            switch(action.substring("ALMA:finish:".length())){
                case "mortal":ending="Новая жизнь";break;
                case "eternal":if(!romance||love<60)return;ending="Вечность вдвоём";break;
                case "queen":ending="Алая королева";break;
                case "part":ending="Разбитая клятва";break;
                case "sacrifice":if(trust<65)return;ending="Последняя жертва";break;
                default:return;
            }stage=12;if(terminalLoss()){companion=false;romance=false;married=false;}
        }else return;
        g.progress.setQuestOutcome(ID,stage==12?"COMPLETED":"ACTIVE");
        try{g.sceneState.setContext(g.day,g.hour,g.progress.location);g.sceneState.confirm("npc",ID,NAME,terminalLoss()?"Разбитая клятва".equals(ending)?"HOSTILE":"DEAD":stage>=3?"HELPED":"MET");}catch(JSONException ignored){}
        g.message="Альма: "+status()+". "+relationship()+".";
    }
}
