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
    int rank=1, mana=80, health=100, victories=0, cooldown=0, lastGiftDay=-1, lastRemarkDay=-1, stance=0, weapon=0, armor=0;
    boolean savedHero=false;
    String lastRemark="";
    org.json.JSONArray memory=new org.json.JSONArray();
    int maxMana(){return 80+20*(rank-1);}
    int maxHealth(){return 100+25*(rank-1);}
    boolean active(){return enabled&&companion&&!terminalLoss();}
    boolean mortal(){return "Новая жизнь".equals(ending);}
    String stanceLabel(){return new String[]{"Сбалансированная","Защитная","Наступательная"}[stance];}
    String gearLabel(){return (weapon==0?"Алый кинжал":"Зачарованный кинжал")+" | "+(armor==0?"Кожаный доспех":"Усиленный доспех");}
    void remember(String s){if(s.isEmpty())return;while(memory.length()>=64)memory.remove(0);memory.put(s);lastRemark=s;}
    void beginBattle(){savedHero=false;cooldown=0;}
    void rest(int hours){if(!active())return;int h=Math.max(0,Math.min(24,hours));mana=Math.min(maxMana(),mana+h*8);health=Math.min(maxHealth(),health+h*10);}
    int assist(GameEngine g,int turn,int remaining){if(!active()||remaining<=0||health<=0)return 0;if(cooldown>0)cooldown--;if(turn%2!=0||mana<10)return 0;
        int damage=Math.min(remaining,5+rank*3+weapon*3+(stance==2?4:0)+(mortal()?0:(g.hour>=20||g.hour<6?3:0)));mana-=10;cooldown=1;
        return damage;}
    int protect(int incoming){if(!active()||health<=0||mana<5||incoming<2||stance==2)return 0;int blocked=Math.min(incoming-1,2+rank+(stance==1?3:0)+armor*2);mana-=5;health=Math.max(0,health-1);return blocked;}
    boolean rescue(GameEngine g){if(!active()||!romance||love<85||mana<25||savedHero||health<=0||mortal())return false;savedHero=true;mana-=25;health=0;g.hp=Math.max(1,g.maxHp/5);remember("Алая клятва: Альма спасла героя от гибели.");return true;}
    void victory(GameEngine g){if(!active())return;victories=GameEngine.nonnegativeAdd(victories,1);int old=rank;rank=Math.min(5,1+victories/5);if(rank>old){mana=maxMana();health=maxHealth();remember("Альма достигла магического ранга "+rank+".");}if(!mortal()&&health==0){health=Math.max(1,maxHealth()/3);remember("Альма вернулась после поражения, ослабленная проклятием.");}mana=Math.min(maxMana(),mana+8);}
    void observeTravel(GameEngine g){if(!active()||lastRemarkDay==g.day)return;lastRemarkDay=g.day;String loc=g.progress.location;String line=loc.toLowerCase(Locale.ROOT).contains("винтерхолд")?"Эти башни напоминают мне о запретных уроках.":loc.toLowerCase(Locale.ROOT).contains("рифтен")?"В Рифтене у каждого есть тайна.":"Путь в "+loc+" запомнится нам обоим.";remember("День "+g.day+" | "+line);}
    String aiContext(){if(!enabled||stage==0)return "";return "Альма: "+status()+"; отношения "+relationship()+"; доверие "+trust+"; любовь "+love+"; спутница "+companion+"; магический ранг "+rank+"; исход "+ending+". Не меняй эти подтверждённые значения. Последняя реплика: "+lastRemark;}

    static int clamp(int n){return Math.max(0,Math.min(100,n));}
    static boolean atRiften(GameEngine g){String l=g.progress.location.toLowerCase(Locale.ROOT).replace('ё','е');return l.contains("рифтен");}
    boolean available(GameEngine g){return enabled&&g.started&&"WORLD".equals(g.mode)&&!terminalLoss()&&(atRiften(g)||(stage>0&&companion));}
    String status(){return stage==0?"Не встречена":stage<TITLES.length?TITLES[stage]:ending;}
    String relationship(){if(stage==0)return "Незнакомка";if("Разбитая клятва".equals(ending))return "Противник";if(married)return "Супруга";if(romance&&love>=70)return "Возлюбленная";if(romance)return "Влюблённость";if(trust>=45)return "Друг";return "Знакомая";}
    boolean canRomance(){return stage>=8&&trust>=45&&affection>=35&&!terminalLoss();}
    boolean terminalLoss(){return "Последняя жертва".equals(ending)||"Разбитая клятва".equals(ending);}
    JSONObject save() throws JSONException{return new JSONObject().put("enabled",enabled).put("stage",stage).put("trust",trust).put("respect",respect).put("affection",affection).put("love",love).put("jealousy",jealousy).put("hurt",hurt).put("loyalty",loyalty).put("romance",romance).put("married",married).put("companion",companion).put("lastTalkDay",lastTalkDay).put("ending",ending).put("rank",rank).put("mana",mana).put("health",health).put("victories",victories).put("cooldown",cooldown).put("savedHero",savedHero).put("stance",stance).put("weapon",weapon).put("armor",armor).put("lastGiftDay",lastGiftDay).put("lastRemarkDay",lastRemarkDay).put("lastRemark",lastRemark).put("memory",memory);}
    static AlmaStory restore(JSONObject j){AlmaStory a=new AlmaStory();if(j==null)return a;
        a.enabled=j.optBoolean("enabled",true);a.stage=Math.max(0,Math.min(TITLES.length,j.optInt("stage")));
        a.trust=clamp(j.optInt("trust"));a.respect=clamp(j.optInt("respect"));a.affection=clamp(j.optInt("affection"));a.love=clamp(j.optInt("love"));a.jealousy=clamp(j.optInt("jealousy"));a.hurt=clamp(j.optInt("hurt"));a.loyalty=clamp(j.optInt("loyalty"));
        a.romance=j.optBoolean("romance")&&a.stage>=8;a.married=j.optBoolean("married")&&a.romance;a.companion=j.optBoolean("companion")&&a.stage>=3;a.lastTalkDay=j.optInt("lastTalkDay",-1);a.ending=a.stage==TITLES.length?j.optString("ending","Новая жизнь"):"";a.rank=Math.max(1,Math.min(5,j.optInt("rank",1)));a.mana=Math.max(0,Math.min(a.maxMana(),j.optInt("mana",a.maxMana())));a.health=Math.max(0,Math.min(a.maxHealth(),j.optInt("health",a.maxHealth())));a.victories=Math.max(0,j.optInt("victories"));a.cooldown=Math.max(0,Math.min(2,j.optInt("cooldown")));a.savedHero=j.optBoolean("savedHero");a.stance=Math.max(0,Math.min(2,j.optInt("stance")));a.weapon=Math.max(0,Math.min(1,j.optInt("weapon")));a.armor=Math.max(0,Math.min(1,j.optInt("armor")));a.lastGiftDay=j.optInt("lastGiftDay",-1);a.lastRemarkDay=j.optInt("lastRemarkDay",-1);a.lastRemark=j.optString("lastRemark","");org.json.JSONArray m=j.optJSONArray("memory");if(m!=null)for(int i=Math.max(0,m.length()-64);i<m.length();i++)a.memory.put(m.optString(i));return a;}
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
        if(stage>=3&&!terminalLoss()){
            s.add("ALMA:stance","Обсудить боевую тактику",stanceLabel());
            if(weapon==0&&g.gold>=150)s.add("ALMA:weapon","Улучшить кинжал","150 септимов");
            if(armor==0&&g.gold>=120)s.add("ALMA:armor","Укрепить доспех","120 септимов");
            if(lastGiftDay!=g.day&&g.gold>=30)s.add("ALMA:gift","Подарить серебряное украшение","30 септимов | один раз в день");
            if(g.katherine.enabled&&g.katherine.stage>=3&&g.katherine.following&&lastTalkDay!=g.day)s.add("ALMA:katherine","Поговорить с Кэтрин вместе","Обсудить доверие и магию");
        }
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
        else if("ALMA:stance".equals(action)&&stage>=3&&!terminalLoss()){stance=(stance+1)%3;remember("Тактика: "+stanceLabel());}
        else if("ALMA:weapon".equals(action)&&stage>=3&&!terminalLoss()&&weapon==0&&g.gold>=150){g.gold-=150;weapon=1;remember("Кинжал Альмы зачарован.");}
        else if("ALMA:armor".equals(action)&&stage>=3&&!terminalLoss()&&armor==0&&g.gold>=120){g.gold-=120;armor=1;remember("Доспех Альмы укреплён.");}
        else if("ALMA:gift".equals(action)&&stage>=3&&!terminalLoss()&&lastGiftDay!=g.day&&g.gold>=30){g.gold-=30;lastGiftDay=g.day;affection=clamp(affection+5);if(romance)love=clamp(love+4);remember("Альма приняла серебряное украшение.");}
        else if("ALMA:katherine".equals(action)&&stage>=3&&!terminalLoss()&&g.katherine.enabled&&g.katherine.stage>=3&&g.katherine.following&&lastTalkDay!=g.day){lastTalkDay=g.day;respect=clamp(respect+3);jealousy=clamp(jealousy-5);remember("Альма: Мы с Кэтрин знаем цену чужой власти. Не стоит мериться силами.");}
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
        if(advance||action.startsWith("ALMA:finish:")||"ALMA:romance".equals(action)||"ALMA:marry".equals(action)||"ALMA:breakup".equals(action)||"ALMA:date".equals(action))remember("День "+g.day+" | "+status()+" | "+relationship());
        g.message="Альма: "+status()+". "+relationship()+".";
    }
}
