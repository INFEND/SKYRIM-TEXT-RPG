package dev.infend.skyrimtext;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Locale;

/** Independent optional crossover story, with authoritative per-save state. */
final class KatherineStory {
    static final String ID="katherine_zero_limit", CONTINUATION="katherine_three_claws", MODE="KATHERINE", NAME="Кэтрин";
    int stage=0,trust=0,respect=0,affection=0,jealousy=0,magicRank=0,lastGiftDay=-1,lastTalkDay=-1,assists=0;
    boolean enabled=true,following=false,romance=false;
    String stance="BALANCED",lastLocation="",lastRemark="";
    JSONArray memory=new JSONArray();
    static int clamp(int n){return Math.max(0,Math.min(100,n));}
    static boolean atHut(GameEngine g){String s=g.progress.location.toLowerCase(Locale.ROOT).replace('ё','е');return s.contains("анис")&&(s.contains("хижин")||s.contains("подвал"));}
    boolean active(){return enabled&&following&&stage>=5;}
    boolean present(GameEngine g){return active()||atHut(g);}
    boolean available(GameEngine g){return enabled&&g.started&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode))&&present(g);}
    boolean atCollege(GameEngine g){return g.progress.location.contains("Коллегия Винтерхолда");}
    boolean atPalace(GameEngine g){return g.progress.location.contains("Солитьюд")||g.progress.location.contains("Синий дворец");}
    String status(){return stage==0?"Не встречена":romance?"Романтические отношения":active()?"Спутница":stage>=8?"Расследование завершено":stage>=3?"Союзница":stage==2?"Доверие":"Знакомство";}
    String objective(){switch(stage){case 0:return "Найти хижину Анис.";case 1:case 2:case 3:return "Продолжить разговор с Кэтрин.";case 4:return "Обеспечить безопасность Деймона и обсудить расследование.";case 5:return "Исследовать отклик в Коллегии Винтерхолда.";case 6:return "Найти сведения о Круге в Солитьюде.";case 7:return "Вернуться в хижину Анис.";default:return "Расследование завершено." ;}}
    String stanceLabel(){return "ATTACK".equals(stance)?"Атака":"DEFENSE".equals(stance)?"Защита":"Баланс";}
    void remember(GameEngine g,String event){lastRemark=event;JSONArray n=new JSONArray();for(int i=Math.max(0,memory.length()-39);i<memory.length();i++)n.put(memory.opt(i));n.put("День "+g.day+" | "+g.progress.location+" | "+event);memory=n;}
    String aiContext(){if(!enabled||stage==0)return "";return "Кэтрин (18 лет): носитель Нулевого Предела; Круг Трёх Когтей хочет захватить живой; связь с драконами не доказана. Самостоятельная, гордая, ревнивая; не терпит лжи и манипуляций. Связь с Деймоном добровольная, без обещаний исключительности; необходима честность. Не изменяй её отношения, магию, местонахождение и статус без подтверждения движком. "+status()+", сопровождает="+active()+", доверие="+trust+", уважение="+respect+", симпатия="+affection+", ревность="+jealousy+", роман="+romance+", магический ранг="+magicRank+", цель="+objective()+", память="+memory.toString();}
    void observeTravel(GameEngine g){if(active()&&!g.progress.location.equals(lastLocation)){lastLocation=g.progress.location;remember(g,"Мы прибыли в "+lastLocation+". Кэтрин осматривает окрестности и оценивает угрозы.");}}
    int assist(GameEngine g,int turn,int remaining){if(!active()||remaining<=0||turn%2!=1||"DEFENSE".equals(stance))return 0;assists++;return Math.min(remaining,Math.min(24,3+g.level/3+magicRank*3));}
    int protect(int incoming){return active()&&!"ATTACK".equals(stance)?Math.min(Math.max(0,incoming-1),2+magicRank):0;}
    JSONObject save() throws JSONException{return new JSONObject().put("enabled",enabled).put("stage",stage).put("trust",trust).put("respect",respect).put("affection",affection).put("jealousy",jealousy).put("magicRank",magicRank).put("following",following).put("romance",romance).put("stance",stance).put("lastGiftDay",lastGiftDay).put("lastTalkDay",lastTalkDay).put("assists",assists).put("lastLocation",lastLocation).put("lastRemark",lastRemark).put("memory",memory);}
    static KatherineStory restore(JSONObject j){KatherineStory k=new KatherineStory();if(j==null)return k;
        k.enabled=j.optBoolean("enabled",true);k.stage=Math.max(0,Math.min(8,j.optInt("stage")));k.trust=clamp(j.optInt("trust"));k.respect=clamp(j.optInt("respect",k.trust/2));k.affection=clamp(j.optInt("affection"));k.jealousy=clamp(j.optInt("jealousy"));k.magicRank=Math.max(0,Math.min(3,j.optInt("magicRank")));k.following=j.optBoolean("following")&&k.stage>=5;k.romance=j.optBoolean("romance")&&k.stage>=4;k.stance=j.optString("stance","BALANCED");if(!"DEFENSE".equals(k.stance)&&!"ATTACK".equals(k.stance))k.stance="BALANCED";k.lastGiftDay=j.optInt("lastGiftDay",-1);k.lastTalkDay=j.optInt("lastTalkDay",-1);k.assists=Math.max(0,j.optInt("assists"));k.lastLocation=j.optString("lastLocation");k.lastRemark=j.optString("lastRemark");JSONArray m=j.optJSONArray("memory");if(m!=null)for(int i=Math.max(0,m.length()-40);i<m.length();i++)k.memory.put(m.optString(i));return k;}
    GameEngine.Scene scene(GameEngine g){
        GameEngine.Scene s;
        if(!enabled||!present(g))return new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Разговор недоступен","Кэтрин сейчас не может продолжить разговор. Вернись к путешествию.").add("KATH:exit","Вернуться","Продолжить путешествие");
        switch(stage){
        case 0:s=new GameEngine.Scene("НАСЛЕДИЕ ДВУХ МИРОВ","Хижина Анис","В подвале пахнет сырой древесиной. У лестницы стоит девушка с серебристыми волосами и янтарными глазами. Рядом лежит бессознательный Деймон.\n\nКэтрин:\n— Стой там. Сначала скажи, кто ты и зачем пришёл.");
            s.add("KATH:help","Предложить помощь Деймону","Показать, что ты не угроза").add("KATH:intro","Представиться","Не приближаться без разрешения");break;
        case 1:s=new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Разговор у лестницы","Кэтрин поправляет одеяло Деймона.\n\nКэтрин:\n— Я не собираюсь бросать его. Если хочешь помочь, говори прямо.");
            s.add("KATH:listen","Выслушать её","Не требовать объяснений").add("KATH:promise","Охранять вход","Соблюдать её границы");break;
        case 2:s=new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Необычный отклик","Пламя свечи дрожит, и Кэтрин смотрит на свою ладонь.\n\nКэтрин:\n— Ты тоже это заметил? Я не знаю, что со мной происходит.");
            s.add("KATH:study","Исследовать аномалию","Не выдавать гипотезу за доказательство").add("KATH:wait","Не торопиться","Дать ей решать самой");break;
        case 3:s=new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Тень Круга","Кэтрин вспоминает документы Круга Трёх Когтей. Её хотят захватить живой.\n\nКэтрин:\n— Я не вещь, которую можно забрать.");
            s.add("KATH:resolve","Обсудить план","Сохранить свободу выбора");break;
        case 4:s=new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Наследие двух миров","Деймон приходит в себя. Кэтрин убеждается, что он в безопасности, прежде чем предложить расследование.\n\nКэтрин:\n— Я согласна искать ответы, но только на своих условиях.");
            if(atHut(g))s.add("KATH:recruit","Предложить расследование","Кэтрин сама решает, согласиться ли");break;
        case 5:s=new GameEngine.Scene("КРУГ ТРЁХ КОГТЕЙ","Запечатанный источник","Кэтрин хочет выяснить причину преследования. Доказательств драконьего происхождения нет.\n\nКэтрин:\n— Проверим факты. Не собираюсь верить каждой красивой легенде.\n\nЦель: "+objective());
            if(atCollege(g)&&active())s.add("KATH:college","Исследовать отклик","Контролируемое испытание | ранг I");break;
        case 6:s=new GameEngine.Scene("КРУГ ТРЁХ КОГТЕЙ","Имя в документах","Следы ведут к Солитьюду. Приказ Круга требует взять Кэтрин живой.\n\nКэтрин:\n— Значит, кому-то я нужна. Хотелось бы узнать, зачем.\n\nЦель: "+objective());
            if(atPalace(g)&&active())s.add("KATH:palace","Проверить сведения","Собрать свидетельства | ранг II");break;
        case 7:s=new GameEngine.Scene("КРУГ ТРЁХ КОГТЕЙ","Свобода выбора","Сведения позволяют запутать следы преследователей. Кэтрин хочет обсудить с Деймоном дальнейший путь.\n\nКэтрин:\n— Мою судьбу не решит ни Круг, ни чародеи, ни пророчества.\n\nЦель: "+objective());
            if(atHut(g)&&active())s.add("KATH:final","Согласовать план защиты","Завершить расследование | ранг III");break;
        default:s=new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Кэтрин","Кэтрин встречает тебя внимательным взглядом.\n\nКэтрин:\n— Ты снова здесь. Что на этот раз?\n\nПрирода драконьего отклика остаётся неизвестной.");break;
        }
        if(stage>=4){
            if(!following&&atHut(g)&&stage>=5&&trust>=25)s.add("KATH:join","Пригласить в отряд","Кэтрин вправе согласиться");
            if(following){s.add("KATH:leave","Отпустить Кэтрин","Она вернётся в хижину Анис");s.add("KATH:stance","Обсудить тактику","Текущая: "+stanceLabel());}
            if(lastTalkDay!=g.day){s.add("KATH:talk","Поговорить откровенно","Обсудить чувства и границы");s.add("KATH:lie","Попытаться скрыть правду","Риск потерять доверие");}
            if(lastGiftDay!=g.day&&g.gold>=25)s.add("KATH:gift","Подарить книгу","25 септимов");
            if(!romance&&trust>=65&&respect>=45&&affection>=35)s.add("KATH:romance","Обсудить взаимные чувства","Только взаимное согласие и честность");
            if(romance)s.add("KATH:break","Завершить роман","Открытый разговор без принуждения");
        }
        return s.add("KATH:exit","Вернуться","Продолжить путешествие");
    }
    void choose(GameEngine g,String action){
        if("KATH:open".equals(action)){if(available(g))g.mode=MODE;return;}
        if(!MODE.equals(g.mode))return;
        // Exit must remain available even if a restored scene lost its companion or was disabled.
        if("KATH:exit".equals(action)){g.mode="WORLD";return;}
        if(!enabled||!present(g))return;
        if(stage>=4){
            switch(action){
            case "KATH:join":if(!following&&atHut(g)&&stage>=5&&trust>=25){following=true;remember(g,"Кэтрин согласилась сопровождать героя.");}return;
            case "KATH:leave":if(following){following=false;g.mode="WORLD";remember(g,"Кэтрин вернулась в хижину Анис.");}return;
            case "KATH:stance":if(following){stance="BALANCED".equals(stance)?"ATTACK":"ATTACK".equals(stance)?"DEFENSE":"BALANCED";remember(g,"Тактика: "+stanceLabel());}return;
            case "KATH:talk":if(lastTalkDay!=g.day){lastTalkDay=g.day;trust=clamp(trust+5);respect=clamp(respect+4);affection=clamp(affection+5);jealousy=clamp(jealousy-3);remember(g,"Откровенный разговор без обещаний исключительности.");}return;
            case "KATH:lie":if(lastTalkDay!=g.day){lastTalkDay=g.day;trust=clamp(trust-18);respect=clamp(respect-12);affection=clamp(affection-8);jealousy=clamp(jealousy+15);if(trust<25){following=false;romance=false;g.mode="WORLD";}remember(g,"Кэтрин заметила ложь и потребовала честности.");}return;
            case "KATH:gift":if(lastGiftDay!=g.day&&g.gold>=25){lastGiftDay=g.day;g.gold-=25;affection=clamp(affection+4);remember(g,"Кэтрин приняла книгу.");}return;
            case "KATH:romance":if(!romance&&trust>=65&&respect>=45&&affection>=35){romance=true;remember(g,"Оба согласились на отношения при полной честности. Связь с Деймоном не отменена.");}return;
            case "KATH:break":if(romance){romance=false;remember(g,"Роман завершён после открытого разговора.");}return;
            case "KATH:recruit":if(stage==4&&atHut(g)){following=true;stage=5;respect=clamp(respect+12);trust=clamp(trust+8);g.progress.setQuestOutcome(CONTINUATION,"ACTIVE");remember(g,"Деймон в безопасности; Кэтрин согласилась на расследование.");}return;
            case "KATH:college":if(stage==5&&active()&&atCollege(g)){stage=6;magicRank=1;respect=clamp(respect+8);remember(g,"Испытание в Коллегии. Отклик подтверждён, происхождение не установлено.");}return;
            case "KATH:palace":if(stage==6&&active()&&atPalace(g)){stage=7;magicRank=2;trust=clamp(trust+8);remember(g,"Получены сведения о Круге Трёх Когтей.");}return;
            case "KATH:final":if(stage==7&&active()&&atHut(g)){stage=8;magicRank=3;trust=clamp(trust+10);respect=clamp(respect+10);g.progress.setQuestOutcome(CONTINUATION,"COMPLETED");remember(g,"План защиты согласован; магическая природа остаётся тайной.");}return;
            default:return;
            }
        }
        int delta=0;
        switch(stage){case 0:if("KATH:help".equals(action))delta=12;else if("KATH:intro".equals(action))delta=6;break;case 1:if("KATH:listen".equals(action))delta=10;else if("KATH:promise".equals(action))delta=8;break;case 2:if("KATH:study".equals(action))delta=8;else if("KATH:wait".equals(action))delta=10;break;case 3:if("KATH:resolve".equals(action))delta=12;break;}
        if(delta==0)return;
        trust=clamp(trust+delta);respect=clamp(respect+delta/2);stage++;remember(g,"Решение в разговоре: "+action);
        g.progress.setQuestOutcome(ID,stage==4?"COMPLETED":"ACTIVE");
        try{g.sceneState.setContext(g.day,g.hour,g.progress.location);g.sceneState.confirm("npc",ID,NAME,stage>=3?"HELPED":"MET");}catch(JSONException ignored){}
        g.message="Кэтрин: "+status()+". Доверие "+trust+"/100.";
    }
}
