package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Locale;

/** Optional Helgen survivor arc. The engine, not generated dialogue, owns the outcome. */
final class LiviaStory {
    static final String NAME="Ливия", MODE="LIVIA", TITLE="Пепел Хелгена";
    boolean enabled=true, following=false, romance=false;
    int stage=0, trust=0, respect=0, affection=0, lastTalkDay=-1, lastDateDay=-1, victories=0;
    String ending="", memory="";
    int epilogue=0,lastEpilogueDay=-1,dates=0,stance=0,companionStamina=100,lastRestDay=-1;
    boolean castleReturn=false;
    JSONArray history=new JSONArray();
    private static final String[] REDEEM_TASKS={"Составить список семей погибших","Передать помощь пострадавшим","Засвидетельствовать выполнение обязательств"};
    private static final String[] FORGIVE_TASKS={"Посетить памятное место","Обсудить пределы приказа","Принять новую клятву Ливии"};
    private static final String[] REDEEM_RESULTS={"Вы сверили имена погибших и нашли семьи, которым нужна помощь.","Ливия передала помощь семьям и выслушала их без оправданий.","Военный писарь принял отчёт. Ливия завершила обещанную помощь, не снимая с себя ответственности."};
    private static final String[] FORGIVE_RESULTS={"Ливия зажгла свечу в память о погибших.","Вы поговорили о том, когда приказ перестаёт быть долгом.","Ливия поклялась защищать невиновных, даже когда это противоречит приказу."};
    void remember(GameEngine g,String value){memory=value;while(history.length()>=100)history.remove(0);history.put("День "+g.day+" | "+value);}
    String epilogueTitle(){if(epilogue>=3)return "Обязательства выполнены";return ("Искупление".equals(ending)?REDEEM_TASKS:FORGIVE_TASKS)[epilogue];}
    String stanceLabel(){return new String[]{"Сбалансированная","Защитная","Наступательная"}[stance];}
    String location(GameEngine g){return active()?g.progress.location:"Мрачный замок, Солитьюд";}
    boolean epilogueReady(GameEngine g){return stage==4&&survivor()&&epilogue<3&&lastEpilogueDay!=g.day&&castle(g)&&g.hour>=8&&g.hour<=20;}
    void rest(int hours){if(active())companionStamina=Math.min(100,companionStamina+Math.min(24,Math.max(0,hours))*12);}
    void daily(GameEngine g){if(active()&&lastRestDay!=g.day){if(lastRestDay>=0)companionStamina=Math.min(100,companionStamina+30);lastRestDay=g.day;}}
    void exit(GameEngine g){g.mode=castleReturn&&SolitudeCity.here(g)?SolitudeCity.PREFIX+"1":"WORLD";castleReturn=false;}
    static boolean matches(String name){return name!=null&&(NAME.equalsIgnoreCase(name)||"Имперский капитан".equalsIgnoreCase(name)||"Имперский капитан Хелгена".equalsIgnoreCase(name)||"Капитан Хелгена".equalsIgnoreCase(name));}
    static int clamp(int n){return Math.max(0,Math.min(100,n));}
    boolean survivor(){return !"Возмездие".equals(ending)&&!"Последний поединок".equals(ending);}
    boolean active(){return enabled&&following&&stage==4&&survivor();}
    boolean castle(GameEngine g){String loc=g.progress.location.toLowerCase(Locale.ROOT).replace('ё','е');return loc.equals("солитьюд")||loc.contains("мрачный замок");}
    boolean witnessed(GameEngine g){return "Хелген".equalsIgnoreCase(g.progress.startId);}
    boolean present(GameEngine g){return enabled&&survivor()&&(active()||castle(g));}
    boolean available(GameEngine g){return enabled&&g.started&&!g.road.pending()&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode))&&present(g)&&(!witnessed(g)||g.canonicalStage>=3);}
    boolean setEnabled(GameEngine g,boolean on){if("COMBAT".equals(g.mode))return false;enabled=on;if(!on&&MODE.equals(g.mode))g.mode="WORLD";return true;}
    String status(){return stage==0?"Мрачный замок, Солитьюд":stage==1?"Допрос капитана":stage==2?"Свидетельства Хелгена":stage==3?"Цена приговора":ending+(following?" | Спутница":"")+(romance?" | Возлюбленная":"");}
    String relation(){if(!survivor())return "Последний поединок".equals(ending)?"Погибла":"Под военным судом";return romance?"Возлюбленная":active()?"Спутница":trust>=55?"Доверяет":trust>=25?"Уважает":"Держит дистанцию";}
    String objective(){return TITLE+" | "+status()+(stage==4&&survivor()?" | "+epilogueTitle()+" ("+epilogue+"/3)":"");}
    String aiContext(GameEngine g){if(!enabled)return "Ливия: дополнение отключено; не вводить её в сцену.";if(!survivor())return "Ливия: "+ending+". "+memory+" Не возвращать в сцену, не восстанавливать спутницу или роман.";if(witnessed(g)&&g.canonicalStage<3)return "Ливия: имперский капитан Хелгена. Ещё не наступила повторная встреча; не раскрывать её дальнейшую судьбу.";return "Ливия, имперский капитан, пережила нападение Алдуина. "+(witnessed(g)?"Приказала казнить героя в Хелгене.":"Герой не обязательно присутствовал на казни.")+" Этап "+stage+"/4, исход="+ending+", рядом="+active()+", роман="+romance+", доверие="+trust+", место="+location(g)+", эпилог="+epilogue+"/3, тактика="+stanceLabel()+". ИИ не вправе менять эти флаги.";}
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
            if(epilogueReady(g))s.add("LIVIA:epilogue:"+epilogue,epilogueTitle(),"Мрачный замок | Следующий шаг выбранного пути");
            if(following){s.add("LIVIA:stance","Изменить боевую тактику",stanceLabel());s.add("LIVIA:rest","Предложить привал","Восстановление запаса сил во время отдыха");}
            if(lastTalkDay!=g.day){s.add("LIVIA:talk","Поговорить откровенно","Доверие и симпатия");if(g.gold>=25)s.add("LIVIA:aid","Помочь семьям пострадавших","25 септимов; один раз за день");}
            if(!romance&&trust>=65&&respect>=50&&affection>=45)s.add("LIVIA:romance","Признаться в чувствах","Взаимное согласие");
            if(romance){if(lastDateDay!=g.day){s.add("LIVIA:date","Провести вечер вдвоём","Спокойный разговор о будущем");if(castle(g))s.add("LIVIA:walk","Прогуляться по Солитьюду","Общая прогулка, без обязательств");}s.add("LIVIA:break","Прекратить роман","Сохранить уважение и возможность дружбы");}
        }
        return s.add("LIVIA:exit","Продолжить путь","Вернуться к приключению");
    }
    void choose(GameEngine g,String action){
        if("LIVIA:open".equals(action)){if(available(g)){castleReturn=false;g.mode=MODE;}return;}
        if(!enabled||!MODE.equals(g.mode)||!present(g)||g.road.pending()||(witnessed(g)&&g.canonicalStage<3))return;
        if("LIVIA:exit".equals(action)){exit(g);return;}
        switch(stage){
            case 0: if("LIVIA:confront".equals(action)||"LIVIA:listen".equals(action)){trust="LIVIA:listen".equals(action)?12:5;respect=12;stage=1;remember(g,"Повторная встреча в Мрачном замке.");}return;
            case 1: if("LIVIA:records".equals(action)||"LIVIA:accuse".equals(action)){boolean records="LIVIA:records".equals(action);trust=clamp(trust+(records?10:5));respect=clamp(respect+(records?10:15));remember(g,records?"Вы сохранили список как доказательство.":"Ливия признала ответственность за гибель невиновных.");stage=2;}return;
            case 2: if("LIVIA:hearing".equals(action)||"LIVIA:witnesses".equals(action)){boolean hearing="LIVIA:hearing".equals(action);trust=clamp(trust+(hearing?8:12));respect=clamp(respect+(hearing?18:12));affection=clamp(affection+(hearing?4:10));remember(g,hearing?"Свидетельства переданы писарю.":"Вы выслушали обоих выживших и собрали показания.");stage=3;}return;
            case 3:
                if("LIVIA:duel".equals(action)){g.beginLiviaDuel();return;}
                if("LIVIA:revenge".equals(action)){finish(g,"Возмездие");return;}
                if("LIVIA:forgive".equals(action)||"LIVIA:redeem".equals(action)){trust=clamp(trust+20);respect=clamp(respect+15);affection=clamp(affection+20);finish(g,"LIVIA:forgive".equals(action)?"Прощение":"Искупление");}return;
            case 4:
                if(!survivor())return;
                if(action.equals("LIVIA:epilogue:"+epilogue)&&epilogueReady(g)){String value=("Искупление".equals(ending)?REDEEM_RESULTS:FORGIVE_RESULTS)[epilogue];epilogue++;lastEpilogueDay=g.day;trust=clamp(trust+5);respect=clamp(respect+8);affection=clamp(affection+3);g.addXp(25);remember(g,value);return;}
                switch(action){
                    case "LIVIA:stance":if(following){stance=(stance+1)%3;remember(g,"Выбрана тактика: "+stanceLabel());}return;
                    case "LIVIA:rest":if(following&&g.restHours(2,false))remember(g,"После короткого привала Ливия восстановила силы.");return;
                    case "LIVIA:join":if(!following&&trust>=35){following=true;lastRestDay=g.day;remember(g,"Ливия согласилась путешествовать с тобой.");}return;
                    case "LIVIA:leave":if(following){following=false;exit(g);remember(g,"Ливия вернулась в Мрачный замок.");}return;
                    case "LIVIA:talk":if(lastTalkDay!=g.day){lastTalkDay=g.day;trust=clamp(trust+6);respect=clamp(respect+4);affection=clamp(affection+7);remember(g,"Вы обсудили прошлое и планы без взаимных обвинений.");}return;
                    case "LIVIA:aid":if(lastTalkDay!=g.day&&g.gold>=25){g.gold-=25;lastTalkDay=g.day;trust=clamp(trust+7);respect=clamp(respect+6);affection=clamp(affection+5);remember(g,"Вы передали припасы семьям пострадавших.");}return;
                    case "LIVIA:romance":if(!romance&&trust>=65&&respect>=50&&affection>=45){romance=true;remember(g,"Ливия ответила взаимностью. Вы договорились о свободном выборе и уважении.");}return;
                    case "LIVIA:walk":case "LIVIA:date":if(romance&&lastDateDay!=g.day&&(!"LIVIA:walk".equals(action)||castle(g))){lastDateDay=g.day;dates=GameEngine.nonnegativeAdd(dates,1);affection=clamp(affection+5);trust=clamp(trust+3);remember(g,"LIVIA:walk".equals(action)?"Вы гуляли по Солитьюду, остановившись у морской арки.":"Вы провели вечер вместе, обсуждая будущее.");}return;
                    case "LIVIA:break":if(romance){romance=false;remember(g,"Вы расстались без принуждения и сохранили уважение.");}return;
                }
        }
    }
    void finish(GameEngine g,String result){if(stage!=3)return;stage=4;ending=result;following=false;romance=false;
        if("Возмездие".equals(result)){memory="Ливия отстранена от службы и передана военному суду.";g.mode="RESULT";g.message=memory;}
        else if("Последний поединок".equals(result)){memory="Ливия погибла в поединке.";g.mode="RESULT";}
        else {memory="Искупление".equals(result)?"Ливия сложила полномочия и обязалась помогать семьям погибших.":"Ливия приняла прощение и обязалась отвечать за свои приказы.";g.mode=MODE;}
        remember(g,memory);g.addXp(100);
    }
    boolean fixRestore(GameEngine g){
        if(!g.fixShop.editorEnabled||stage!=4||!"Последний поединок".equals(ending)||!"WORLD".equals(g.mode)&&!"RESULT".equals(g.mode))return false;
        ending="Прощение";following=false;romance=false;trust=Math.max(35,trust);memory="Ливия возвращена к жизни магией фикскоинов. Она свободна, но отношения предстоит восстановить.";
        try{g.sceneState.setContext(g.day,g.hour,g.progress.location);org.json.JSONArray facts=g.sceneState.npcFacts();for(int i=0;i<facts.length();i++){org.json.JSONObject n=facts.optJSONObject(i);if(n!=null&&matches(n.optString("title"))&&"DEAD".equals(n.optString("status")))g.sceneState.fixNpc(n.optString("id"),50,true);}}catch(Exception ignored){}
        return true;
    }
    void defeatedInDuel(GameEngine g){if(stage==3&&"LIVIA_DUEL".equals(g.battleKind)){memory="Ливия остановила поединок, когда ты упал. Она не нанесла последнего удара. Ты можешь вернуться и принять решение.";g.message+="\n\n"+memory;}}
    int assist(int turn,int remaining){if(!active()||turn%2!=0||remaining<=0||companionStamina<8)return 0;companionStamina-=8;return Math.min(remaining,8+Math.min(12,victories/3)+(stance==2?4:stance==1?-2:0));}
    int protect(int incoming){if(!active()||incoming<=1||companionStamina<4)return 0;int amount=Math.min(incoming-1,3+Math.min(4,victories/5)+(stance==1?3:stance==2?-1:0));if(amount>0)companionStamina-=4;return amount;}
    void victory(GameEngine g){if("LIVIA_DUEL".equals(g.battleKind)){finish(g,"Последний поединок");return;}if(active())victories=Math.min(1000,victories+1);}
    JSONObject save() throws JSONException{return new JSONObject().put("enabled",enabled).put("following",following).put("romance",romance).put("stage",stage).put("trust",trust).put("respect",respect).put("affection",affection).put("lastTalkDay",lastTalkDay).put("lastDateDay",lastDateDay).put("victories",victories).put("ending",ending).put("memory",memory).put("epilogue",epilogue).put("lastEpilogueDay",lastEpilogueDay).put("dates",dates).put("stance",stance).put("companionStamina",companionStamina).put("lastRestDay",lastRestDay).put("castleReturn",castleReturn).put("history",history);}
    static LiviaStory restore(JSONObject j){LiviaStory s=new LiviaStory();if(j==null)return s;s.enabled=j.optBoolean("enabled",true);s.stage=Math.max(0,Math.min(4,j.optInt("stage")));s.trust=clamp(j.optInt("trust"));s.respect=clamp(j.optInt("respect"));s.affection=clamp(j.optInt("affection"));s.lastTalkDay=j.optInt("lastTalkDay",-1);s.lastDateDay=j.optInt("lastDateDay",-1);s.victories=Math.max(0,Math.min(1000,j.optInt("victories")));s.ending=j.optString("ending","");s.memory=j.optString("memory","");s.epilogue=Math.max(0,Math.min(3,j.optInt("epilogue")));s.lastEpilogueDay=j.optInt("lastEpilogueDay",-1);s.dates=Math.max(0,j.optInt("dates"));s.stance=Math.max(0,Math.min(2,j.optInt("stance")));s.companionStamina=clamp(j.optInt("companionStamina",100));s.lastRestDay=j.optInt("lastRestDay",-1);s.castleReturn=j.optBoolean("castleReturn");JSONArray events=j.optJSONArray("history");if(events!=null)for(int i=Math.max(0,events.length()-100);i<events.length();i++){String item=events.optString(i);if(!item.isEmpty())s.history.put(item);}s.following=s.stage==4&&s.survivor()&&j.optBoolean("following");s.romance=s.stage==4&&s.survivor()&&j.optBoolean("romance");return s;}
}
