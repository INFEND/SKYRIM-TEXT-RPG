package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.Locale;

/** Optional Helgen survivor arc. The engine, not generated dialogue, owns the outcome. */
final class LiviaStory {
    static final String NAME="Ливия", MODE="LIVIA", TITLE="Пепел Хелгена";
    boolean enabled=true, following=false, romance=false;
    int stage=0, trust=0, respect=0, affection=0, lastTalkDay=-1, lastDateDay=-1, victories=0;
    String ending="", memory="";
    static int clamp(int n){return Math.max(0,Math.min(100,n));}
    boolean survivor(){return !"Возмездие".equals(ending)&&!"Последний поединок".equals(ending);}
    boolean active(){return enabled&&following&&stage==4&&survivor();}
    boolean castle(GameEngine g){String loc=g.progress.location.toLowerCase(Locale.ROOT).replace('ё','е');return loc.equals("солитьюд")||loc.contains("мрачный замок");}
    boolean witnessed(GameEngine g){return "Хелген".equalsIgnoreCase(g.progress.startId);}
    boolean present(GameEngine g){return enabled&&survivor()&&(active()||castle(g));}
    boolean available(GameEngine g){return enabled&&g.started&&!g.road.pending()&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode))&&present(g)&&(!witnessed(g)||g.canonicalStage>=3);}
    boolean setEnabled(GameEngine g,boolean on){if("COMBAT".equals(g.mode))return false;enabled=on;if(!on&&MODE.equals(g.mode))g.mode="WORLD";return true;}
    String status(){return stage==0?"Мрачный замок, Солитьюд":stage==1?"Допрос капитана":stage==2?"Свидетельства Хелгена":stage==3?"Цена приговора":ending+(following?" | Спутница":"")+(romance?" | Возлюбленная":"");}
    String relation(){return romance?"Возлюбленная":active()?"Спутница":trust>=55?"Доверяет":trust>=25?"Уважает":"Держит дистанцию";}
    String objective(){return TITLE+" | "+status();}
    String aiContext(GameEngine g){if(!enabled)return "Ливия: дополнение отключено; не вводить её в сцену.";return "Ливия, имперский капитан, пережила нападение Алдуина. "+(witnessed(g)?"Приказала казнить героя в Хелгене.":"Герой не обязательно присутствовал на казни.")+" Этап "+stage+"/4, исход="+ending+", рядом="+active()+", роман="+romance+", доверие="+trust+", место="+(active()?g.progress.location:"Мрачный замок, Солитьюд")+". ИИ не вправе менять эти флаги.";}
    GameEngine.Scene scene(GameEngine g){
        String body; GameEngine.Scene s;
        if(stage==0){
            body="В Мрачном замке, за дверью караульного помещения, пахнет воском и мокрой сталью. Женщина в имперских доспехах поднимается из-за стола. На запястье виден след ожога. "+(witnessed(g)?"Ты узнаёшь капитана, отправившую тебя на плаху. Она бледнеет, встретив твой взгляд.":"Ты узнаёшь, что она командовала казнью в Хелгене и выжила после нападения дракона.")+"\n\nЛивия:\n— Я думала, что Хелген останется позади. Но приказы не исчезают вместе с пеплом.";
            s=new GameEngine.Scene(TITLE,"Встреча в Мрачном замке",body).primary("LIVIA:confront","Потребовать объяснений","Не позволить ей уклониться").add("LIVIA:listen","Выслушать Ливию","Без обещания простить");
        }else if(stage==1){
            s=new GameEngine.Scene(TITLE,"Приказ без имени","Ливия кладёт перед тобой копию списка пленников. Огонь уничтожил часть документов, но её подпись осталась.\n\nЛивия:\n— Когда Алдуин ударил по башне, меня отбросило под обвал. Я выбралась через служебный ход. Я не стану винить ни дракона, ни Туллия за собственное решение.").primary("LIVIA:records","Изучить список казнённых","Собрать свидетельства").add("LIVIA:accuse","Напомнить о невиновных","Потребовать признания вины");
        }else if(stage==2){
            s=new GameEngine.Scene(TITLE,"Голоса выживших","В караульной появляются два уцелевших солдата. Один помнит приказ капитана, другой видел, как она выводила раненых из огня. Ливия не пытается выдать спасённые жизни за оправдание.\n\nЛивия:\n— Я готова дать показания. Решай, чего ты хочешь от меня.").primary("LIVIA:hearing","Провести открытое разбирательство","Установить ответственность").add("LIVIA:witnesses","Выслушать обоих свидетелей","Дать ей возможность исправить содеянное");
        }else if(stage==3){
            s=new GameEngine.Scene(TITLE,"Цена приговора","За дверью ждёт военный писарь. На столе лежат показания и подписанное признание капитана.\n\nЛивия:\n— Я не имею права просить тебя о прощении. Но могу ответить за содеянное.").primary("LIVIA:redeem","Потребовать искупления","Она оставит строевую службу и поможет пострадавшим").add("LIVIA:forgive","Простить её","Не отменяя ответственности за приказ").add("LIVIA:revenge","Добиться возмездия","Передать дело военному суду; Ливия покинет историю").add("LIVIA:duel","Вызвать на смертельный поединок","Необратимый исход при победе");
        }else{
            s=new GameEngine.Scene(TITLE,"Ливия | "+relation(),"Ливия поправляет перевязь меча. "+(romance?"Она берёт тебя за руку.":"Она оставляет тебе пространство для решения.")+"\n\nЛивия:\n— "+(romance?"Я хочу быть рядом не из долга, а по собственному выбору.":"Моё прошлое не изменится. Но я могу выбирать, что делать дальше.")+"\n\n"+status()+"\nДоверие "+trust+"/100 | Уважение "+respect+"/100 | Симпатия "+affection+"/100"+(memory.isEmpty()?"":"\n\n"+memory));
            if(!following&&trust>=35)s.add("LIVIA:join","Пригласить в отряд","Меч и щит");
            if(following)s.add("LIVIA:leave","Отпустить спутницу","Вернётся в Мрачный замок");
            if(lastTalkDay!=g.day)s.add("LIVIA:talk","Поговорить откровенно","Доверие и симпатия").add("LIVIA:aid","Помочь семьям пострадавших","25 септимов; один раз за день");
            if(!romance&&trust>=65&&respect>=50&&affection>=45)s.add("LIVIA:romance","Признаться в чувствах","Взаимное согласие");
            if(romance){if(lastDateDay!=g.day)s.add("LIVIA:date","Провести вечер вдвоём","Спокойный разговор о будущем");s.add("LIVIA:break","Прекратить роман","Сохранить уважение и возможность дружбы");}
        }
        return s.add("LIVIA:exit","Продолжить путь","Вернуться к приключению");
    }
    void choose(GameEngine g,String action){
        if("LIVIA:open".equals(action)){if(available(g))g.mode=MODE;return;}
        if(!enabled||!MODE.equals(g.mode)||!present(g))return;
        if("LIVIA:exit".equals(action)){g.mode="WORLD";return;}
        switch(stage){
            case 0: if("LIVIA:confront".equals(action)||"LIVIA:listen".equals(action)){trust="LIVIA:listen".equals(action)?12:5;respect=12;stage=1;}return;
            case 1: if("LIVIA:records".equals(action)||"LIVIA:accuse".equals(action)){trust=clamp(trust+8);respect=clamp(respect+12);stage=2;}return;
            case 2: if("LIVIA:hearing".equals(action)||"LIVIA:witnesses".equals(action)){trust=clamp(trust+10);respect=clamp(respect+15);affection=clamp(affection+8);stage=3;}return;
            case 3:
                if("LIVIA:duel".equals(action)){g.beginLiviaDuel();return;}
                if("LIVIA:revenge".equals(action)){finish(g,"Возмездие");return;}
                if("LIVIA:forgive".equals(action)||"LIVIA:redeem".equals(action)){trust=clamp(trust+20);respect=clamp(respect+15);affection=clamp(affection+20);finish(g,"LIVIA:forgive".equals(action)?"Прощение":"Искупление");}return;
            case 4:
                if(!survivor())return;
                switch(action){
                    case "LIVIA:join":if(!following&&trust>=35){following=true;memory="Ливия согласилась путешествовать с тобой.";}return;
                    case "LIVIA:leave":if(following){following=false;g.mode="WORLD";memory="Ливия вернулась в Мрачный замок.";}return;
                    case "LIVIA:talk":if(lastTalkDay!=g.day){lastTalkDay=g.day;trust=clamp(trust+6);respect=clamp(respect+4);affection=clamp(affection+7);memory="Вы обсудили прошлое и планы без взаимных обвинений.";}return;
                    case "LIVIA:aid":if(lastTalkDay!=g.day&&g.gold>=25){g.gold-=25;lastTalkDay=g.day;trust=clamp(trust+7);respect=clamp(respect+6);affection=clamp(affection+5);memory="Вы передали припасы семьям пострадавших.";}return;
                    case "LIVIA:romance":if(!romance&&trust>=65&&respect>=50&&affection>=45){romance=true;memory="Ливия ответила взаимностью. Вы договорились о свободном выборе и уважении.";}return;
                    case "LIVIA:date":if(romance&&lastDateDay!=g.day){lastDateDay=g.day;affection=clamp(affection+5);trust=clamp(trust+3);memory="Вы провели вечер вместе, обсуждая будущее.";}return;
                    case "LIVIA:break":if(romance){romance=false;memory="Вы расстались без принуждения и сохранили уважение.";}return;
                }
        }
    }
    void finish(GameEngine g,String result){if(stage!=3)return;stage=4;ending=result;following=false;romance=false;
        if("Возмездие".equals(result)){memory="Ливия отстранена от службы и передана военному суду.";g.mode="RESULT";g.message=memory;}
        else if("Последний поединок".equals(result)){memory="Ливия погибла в поединке.";g.mode="RESULT";}
        else {memory="Искупление".equals(result)?"Ливия сложила полномочия и обязалась помогать семьям погибших.":"Ливия приняла прощение и обязалась отвечать за свои приказы.";g.mode=MODE;}
        g.addXp(100);
    }
    int assist(int turn,int remaining){return active()&&turn%2==0?Math.min(Math.max(0,remaining),8+Math.min(12,victories/3)):0;}
    int protect(int incoming){return active()?Math.min(Math.max(0,incoming-1),3+Math.min(4,victories/5)):0;}
    void victory(GameEngine g){if("LIVIA_DUEL".equals(g.battleKind)){finish(g,"Последний поединок");return;}if(active())victories=Math.min(1000,victories+1);}
    JSONObject save() throws JSONException{return new JSONObject().put("enabled",enabled).put("following",following).put("romance",romance).put("stage",stage).put("trust",trust).put("respect",respect).put("affection",affection).put("lastTalkDay",lastTalkDay).put("lastDateDay",lastDateDay).put("victories",victories).put("ending",ending).put("memory",memory);}
    static LiviaStory restore(JSONObject j){LiviaStory s=new LiviaStory();if(j==null)return s;s.enabled=j.optBoolean("enabled",true);s.stage=Math.max(0,Math.min(4,j.optInt("stage")));s.trust=clamp(j.optInt("trust"));s.respect=clamp(j.optInt("respect"));s.affection=clamp(j.optInt("affection"));s.lastTalkDay=j.optInt("lastTalkDay",-1);s.lastDateDay=j.optInt("lastDateDay",-1);s.victories=Math.max(0,Math.min(1000,j.optInt("victories")));s.ending=j.optString("ending","");s.memory=j.optString("memory","");s.following=s.stage==4&&s.survivor()&&j.optBoolean("following");s.romance=s.stage==4&&s.survivor()&&j.optBoolean("romance");return s;}
}
