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
    int mana=60, lastAutonomyDay=-1, victories=0;
    KatherineCompanion companion=new KatherineCompanion();
    static final String FINALE="katherine_last_trail";
    int maxMana(){return 60+magicRank*20;}
    void rest(int hours){if(active()){mana=Math.min(maxMana(),mana+Math.max(0,Math.min(24,hours))*8);companion.rest(hours);if(hours>=6)companion.portalUsed=false;companion.updateMood(this);}}
    void autonomous(GameEngine g){if(!active()||lastAutonomyDay==g.day)return;lastAutonomyDay=g.day;companion.updateMood(this);if(companion.hp<companion.maxHp()/3){remember(g,"Кэтрин просит сделать привал, чтобы восстановить силы.");}else if(mana<maxMana()/3){remember(g,"Кэтрин просит сделать привал: её магия истощена.");}else if(trust<30){following=false;romance=false;companion.endRomance();remember(g,"Кэтрин решила вернуться в хижину Анис из-за недостатка доверия.");}else if(g.hp<g.maxHp/3){remember(g,"Кэтрин замечает раны героя и предлагает найти безопасное место для отдыха.");}else if(romance){remember(g,"Кэтрин первой начинает разговор о совместных планах.");}else{remember(g,"Кэтрин обсуждает дорогу и проверяет, не преследует ли вас Круг.");}}
    void victory(GameEngine g){if(!active())return;if(victories<Integer.MAX_VALUE)victories++;companion.gain(35+magicRank*10);if("KATHERINE".equals(g.battleKind)&&stage==9){stage=10;trust=clamp(trust+10);respect=clamp(respect+8);remember(g,"Засада Круга отбита. Кэтрин готова обсудить найденные сведения.");}else if(victories%3==0){respect=clamp(respect+2);remember(g,"Кэтрин отмечает согласованность действий в бою.");}}
    String stance="BALANCED",lastLocation="",lastRemark="";
    JSONArray memory=new JSONArray();
    static int clamp(int n){return Math.max(0,Math.min(100,n));}
    static boolean atHut(GameEngine g){String s=g.progress.location.toLowerCase(Locale.ROOT).replace('ё','е');return s.contains("анис")&&(s.contains("хижин")||s.contains("подвал"));}
    boolean active(){return enabled&&following&&stage>=5;}
    boolean present(GameEngine g){return active()||(stage>=11&&"Свободная дорога".equals(companion.ending)?g.progress.location.equals(companion.independentLocation(g.day,g.hour)):atHut(g));}
    boolean available(GameEngine g){return enabled&&g.started&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode))&&present(g);}
    boolean atCollege(GameEngine g){return g.progress.location.contains("Коллегия Винтерхолда");}
    boolean atPalace(GameEngine g){return g.progress.location.contains("Солитьюд")||g.progress.location.contains("Синий дворец");}
    String status(){return stage==0?"Не встречена":romance?"Романтические отношения":active()?"Спутница":stage>=11?"Расследование завершено":stage>=8?"Последний след":stage>=3?"Союзница":stage==2?"Доверие":"Знакомство";}
    String objective(){switch(stage){case 0:return "Найти хижину Анис.";case 1:case 2:case 3:return "Продолжить разговор с Кэтрин.";case 4:return "Обеспечить безопасность Деймона и обсудить расследование.";case 5:return "Исследовать отклик в Коллегии Винтерхолда.";case 6:return "Найти сведения о Круге в Солитьюде.";case 7:return "Вернуться в хижину Анис.";case 8:return "Обсудить последние следы Круга с Кэтрин.";case 9:return "Вернуться в хижину Анис и разобраться с преследователями.";case 10:return "Обсудить найденные сведения и дальнейшую судьбу Кэтрин.";default:return "Расследование завершено." ;}}
    String stanceLabel(){return "ATTACK".equals(stance)?"Атака":"DEFENSE".equals(stance)?"Защита":"Баланс";}
    void remember(GameEngine g,String event){lastRemark=event;JSONArray n=new JSONArray();for(int i=Math.max(0,memory.length()-1023);i<memory.length();i++)n.put(memory.opt(i));n.put("День "+g.day+" | "+g.progress.location+" | "+event);memory=n;companion.updateMood(this);}
    String recentMemory(){JSONArray n=new JSONArray();for(int i=Math.max(0,memory.length()-12);i<memory.length();i++)n.put(memory.opt(i));return n.toString();}
    String compactContext(GameEngine g){if(!enabled||stage==0)return "";return "Кэтрин: "+status()+", рядом="+present(g)+", доверие="+trust+", магия="+mana+"/"+maxMana()+", настроение="+companion.mood+", цель="+objective()+"; не менять её состояние через effects.";}
    private boolean presentForAi(){return active();}
    String aiContext(){if(!enabled||stage==0)return "";return "Кэтрин (18 лет): носитель Нулевого Предела; Круг Трёх Когтей хочет захватить живой; связь с драконами не доказана. Самостоятельная, гордая, ревнивая; не терпит лжи и манипуляций. Связь с Деймоном добровольная, без обещаний исключительности; необходима честность. Не изменяй её отношения, магию, местонахождение и статус без подтверждения движком. "+status()+", сопровождает="+active()+", доверие="+trust+", уважение="+respect+", симпатия="+affection+", ревность="+jealousy+", роман="+romance+", магический ранг="+magicRank+", запас магии="+mana+"/"+maxMana()+", цель="+objective()+", здоровье="+companion.hp+"/"+companion.maxHp()+", настроение="+companion.mood+", любовь="+companion.love+", снаряжение="+companion.gearName()+", итог="+companion.ending+", важные события="+companion.milestones+", память="+recentMemory();}
    void observeTravel(GameEngine g){if(active()&&romance&&g.alma.active()&&g.alma.romance&&companion.lastJoint!=g.day&&lastAutonomyDay!=g.day){jealousy=clamp(jealousy+5);remember(g,"Кэтрин хочет обсудить отношения с Альмой.");}if(active()&&!g.progress.location.equals(lastLocation)){lastLocation=g.progress.location;remember(g,"Мы прибыли в "+lastLocation+". Кэтрин осматривает окрестности и оценивает угрозы.");autonomous(g);}}
    int assist(GameEngine g,int turn,int remaining){if(!active()||companion.hp<=0||remaining<=0||turn%2!=1||"DEFENSE".equals(stance))return 0;int spell=Math.min(companion.spell,companion.limitSpell(magicRank));int cost=spell==4?16:spell==5?18:8;if(mana<cost)return 0;mana-=cost;assists=GameEngine.nonnegativeAdd(assists,1);companion.lastSpell=KatherineCompanion.SPELLS[spell];companion.castEffect(spell);int bonus=spell==4?9:spell==5?12:spell>0?3:0;return Math.min(remaining,Math.min(40,3+g.level/3+magicRank*3+companion.focus*2+bonus+(companion.dragonEcho?2:0)));}
    int protect(int incoming){if(!active()||companion.hp<=0||"ATTACK".equals(stance)||mana<5||incoming<=1)return 0;mana-=5;companion.hp=Math.max(0,companion.hp-1);return Math.min(incoming-1,2+magicRank+companion.gear*2+("DEFENSE".equals(stance)?2:0));}
    JSONObject save() throws JSONException{return new JSONObject().put("enabled",enabled).put("stage",stage).put("trust",trust).put("respect",respect).put("affection",affection).put("jealousy",jealousy).put("magicRank",magicRank).put("following",following).put("romance",romance).put("stance",stance).put("lastGiftDay",lastGiftDay).put("lastTalkDay",lastTalkDay).put("assists",assists).put("lastLocation",lastLocation).put("lastRemark",lastRemark).put("memory",memory).put("mana",mana).put("lastAutonomyDay",lastAutonomyDay).put("victories",victories).put("companion",companion.save());}
    static KatherineStory restore(JSONObject j){KatherineStory k=new KatherineStory();if(j==null)return k;
        k.enabled=j.optBoolean("enabled",true);k.stage=Math.max(0,Math.min(11,j.optInt("stage")));k.trust=clamp(j.optInt("trust"));k.respect=clamp(j.optInt("respect",k.trust/2));k.affection=clamp(j.optInt("affection"));k.jealousy=clamp(j.optInt("jealousy"));k.magicRank=Math.max(0,Math.min(3,j.optInt("magicRank")));k.following=j.optBoolean("following")&&k.stage>=5;k.romance=j.optBoolean("romance")&&k.stage>=4;k.stance=j.optString("stance","BALANCED");if(!"DEFENSE".equals(k.stance)&&!"ATTACK".equals(k.stance))k.stance="BALANCED";k.lastGiftDay=j.optInt("lastGiftDay",-1);k.lastTalkDay=j.optInt("lastTalkDay",-1);k.assists=Math.max(0,j.optInt("assists"));k.lastLocation=j.optString("lastLocation");k.lastRemark=j.optString("lastRemark");k.mana=Math.max(0,Math.min(k.maxMana(),j.optInt("mana",k.maxMana())));k.lastAutonomyDay=j.optInt("lastAutonomyDay",-1);k.victories=Math.max(0,j.optInt("victories"));k.companion=KatherineCompanion.restore(j.optJSONObject("companion"),k.magicRank,k.romance);JSONArray m=j.optJSONArray("memory");if(m!=null)for(int i=Math.max(0,m.length()-1024);i<m.length();i++)k.memory.put(m.optString(i));return k;}
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
        case 8:s=new GameEngine.Scene("ПОСЛЕДНИЙ СЛЕД","Письмо без подписи","Кэтрин показывает письмо с тремя когтями на печати. Кто-то знает, где находится Деймон.\n\nКэтрин:\n— Я хочу покончить с этим. Но я не собираюсь превращать его в заложника.");if(active())s.add("KATH:trail","Начать последнее расследование","Вернуться к хижине Анис");break;
        case 9:s=new GameEngine.Scene("ПОСЛЕДНИЙ СЛЕД","Люди Круга","У хижины появляются преследователи. Кэтрин держит магический барьер и ждёт решения.\n\nКэтрин:\n— Сначала попробуем выяснить, зачем им нужна я.");if(atHut(g)&&active()){s.add("KATH:fight","Отразить нападение","Бой с агентом Круга");if(respect>=55)s.add("KATH:negotiate","Потребовать объяснений","Переговоры | уважение 55");}break;
        case 10:s=new GameEngine.Scene("ПОСЛЕДНИЙ СЛЕД","Право на собственную судьбу","Кэтрин изучает бумаги Круга. Они подтверждают охоту за ней, но не объясняют происхождения её магии.\n\nКэтрин:\n— Теперь я сама решу, что делать дальше.");if(active())s.add("KATH:conclude","Обсудить дальнейший путь","Завершить расследование");break;
        default:s=new GameEngine.Scene("НУЛЕВОЙ ПРЕДЕЛ","Кэтрин","Кэтрин встречает тебя внимательным взглядом.\n\nКэтрин:\n— Ты снова здесь. Что на этот раз?\n\nПрирода драконьего отклика остаётся неизвестной.");break;
        }
        if(stage>=4){
            companion.appendChoices(s,g,this);
            if(!following&&present(g)&&stage>=5&&trust>=25)s.add("KATH:join","Пригласить в отряд","Кэтрин вправе согласиться");
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
            if(companion.choose(g,this,action))return;
            switch(action){
            case "KATH:trail":if(stage==8&&active()){stage=9;g.progress.setQuestOutcome(FINALE,"ACTIVE");remember(g,"Получено письмо Круга; нужно вернуться в хижину.");}return;
            case "KATH:fight":if(stage==9&&active()&&atHut(g)){g.beginKatherineBattle();}return;
            case "KATH:negotiate":if(stage==9&&active()&&atHut(g)&&respect>=55){stage=10;trust=clamp(trust+8);remember(g,"Переговоры позволили получить сведения без боя.");}return;
            case "KATH:conclude":if(stage==10&&active()){stage=11;companion.ending="Союз двух миров";companion.milestone(companion.ending);respect=clamp(respect+10);g.progress.setQuestOutcome(FINALE,"COMPLETED");remember(g,"Кэтрин завершила расследование и сама выбрала дальнейший путь.");}return;
            case "KATH:join":if(!following&&present(g)&&stage>=5&&trust>=25){following=true;remember(g,"Кэтрин согласилась сопровождать героя.");}return;
            case "KATH:leave":if(following){following=false;g.mode="WORLD";remember(g,"Кэтрин вернулась в хижину Анис.");}return;
            case "KATH:stance":if(following){stance="BALANCED".equals(stance)?"ATTACK":"ATTACK".equals(stance)?"DEFENSE":"BALANCED";remember(g,"Тактика: "+stanceLabel());}return;
            case "KATH:talk":if(lastTalkDay!=g.day){lastTalkDay=g.day;trust=clamp(trust+5);respect=clamp(respect+4);affection=clamp(affection+5);jealousy=clamp(jealousy-3);remember(g,"Откровенный разговор без обещаний исключительности.");}return;
            case "KATH:lie":if(lastTalkDay!=g.day){lastTalkDay=g.day;trust=clamp(trust-18);respect=clamp(respect-12);affection=clamp(affection-8);jealousy=clamp(jealousy+15);companion.hurt=clamp(companion.hurt+18);if(trust<25){following=false;romance=false;companion.endRomance();g.mode="WORLD";}remember(g,"Кэтрин заметила ложь и потребовала честности.");}return;
            case "KATH:gift":if(lastGiftDay!=g.day&&g.gold>=25){lastGiftDay=g.day;g.gold-=25;affection=clamp(affection+4);remember(g,"Кэтрин приняла книгу.");}return;
            case "KATH:romance":if(!romance&&trust>=65&&respect>=45&&affection>=35){romance=true;companion.love=Math.max(companion.love,35);companion.milestone("Начало романа");remember(g,"Оба согласились на отношения при полной честности. Связь с Деймоном не отменена.");}return;
            case "KATH:break":if(romance){romance=false;companion.endRomance();remember(g,"Роман завершён после открытого разговора.");}return;
            case "KATH:recruit":if(stage==4&&atHut(g)){following=true;stage=5;if(companion.profile>0){magicRank=Math.max(magicRank,companion.profile);mana=maxMana();}companion.damonSafe=true;companion.milestone("Кэтрин присоединилась");respect=clamp(respect+12);trust=clamp(trust+8);g.progress.setQuestOutcome(CONTINUATION,"ACTIVE");remember(g,"Деймон в безопасности; Кэтрин согласилась на расследование.");}return;
            case "KATH:college":if(stage==5&&active()&&atCollege(g)){stage=6;magicRank=Math.max(magicRank,1);mana=maxMana();respect=clamp(respect+8);remember(g,"Испытание в Коллегии. Отклик подтверждён, происхождение не установлено.");}return;
            case "KATH:palace":if(stage==6&&active()&&atPalace(g)){stage=7;magicRank=Math.max(magicRank,2);mana=maxMana();trust=clamp(trust+8);remember(g,"Получены сведения о Круге Трёх Когтей.");}return;
            case "KATH:final":if(stage==7&&active()&&atHut(g)){stage=8;magicRank=3;mana=maxMana();trust=clamp(trust+10);respect=clamp(respect+10);g.progress.setQuestOutcome(CONTINUATION,"COMPLETED");remember(g,"План защиты согласован; магическая природа остаётся тайной.");}return;
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
