package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;

/** Original local stories, independent of canonical Skyrim quest progression. */
final class SolitudeEncounters {
    private SolitudeEncounters(){}
    static final String[] NAMES={"Яла","Генерал Туллий","Фолк Огнебород","Виармо","Рорлунд","Бейранд","Корпул Виний","Виттория Вичи"};
    private static final String[] IDS={"jala","tullius","falk","viarmo","rorlund","beirand","corpulus","vittoria"};
    private static final String[] TOPICS={
        "На рынке все знают цену соли и зерну. Я не люблю, когда покупатели узнают о недостаче последними.",
        "Снабжение и дисциплина важнее громких речей. Не путай помощь гарнизону с вступлением в Легион.",
        "Прошения жителей поступают ежедневно. При дворе особенно ценят проверенные свидетельства.",
        "Песня живёт дольше певца, если её слова переданы точно. У нас найдётся работа для внимательного читателя.",
        "Храм открыт для людей любой веры. Иногда человеку нужнее еда и покой, чем красивое обещание.",
        "Хорошая сталь требует терпения. Даже простой гвоздь может выдать руку неопытного кузнеца.",
        "В моей таверне знают много историй, но я не отвечаю за правдивость каждой из них.",
        "В порту нет мелочей. Один неверный знак в ведомости способен задержать целый груз."
    };
    // Authored lines about established places and occupations; not original game quotes.
    private static final String[] LOCAL={
        "На рынке у ворот удобно купить свежие припасы. В гавани свои торговые дела, там спроси у грузчиков.",
        "Мрачный замок — оплот Легиона. Держись подальше от закрытых помещений и уважай караул.",
        "В Синем дворце принимают просителей. За имуществом и делами двора обращаются ко мне.",
        "В Коллегии бардов хранят старые песни. Внимание к словам здесь ценят не меньше хорошего голоса.",
        "В Храме Богов можно найти утешение. Не путай храм с катакомбами, где покоятся умершие.",
        "У кузницы можно освоить ремесло и приобрести снаряжение. За горном нужен внимательный присмотр.",
        "Смеющаяся крыса принимает путешественников. За едой, постелью и слухами лучше обращаться до закрытия.",
        "Гавань связывает столицу с торговыми путями. На причалах у каждого груза есть своя ведомость."
    };
    private static final int[] OPEN={8,8,9,9,7,8,6,8}, CLOSE={18,18,17,19,19,18,23,18};
    static final class Quest {
        final String id,title,offer,task,work,finish,alternative;
        final int giver,target,hours,stamina,gold,rep;
        Quest(String id,String title,int giver,int target,String offer,String task,String work,String finish,String alternative,int hours,int stamina,int gold,int rep){
            this.id=id;this.title=title;this.giver=giver;this.target=target;this.offer=offer;this.task=task;this.work=work;this.finish=finish;this.alternative=alternative;this.hours=hours;this.stamina=stamina;this.gold=gold;this.rep=rep;
        }
        String key(){return "solitude_"+id;}
        String step(){return key()+"_step";}
    }
    static final Quest[] QUESTS={
        new Quest("cargo","Пропавшая накладная",7,0,"Виттория просит сверить грузовую накладную с рыночной книгой.","Сверить записи у Ялы","В книге обнаруживается двойная запись. Ты переписываешь обе строки и отмечаешь дату.","Виттория исправляет накладную, не обвиняя грузчиков без доказательств.","Передать копию без обвинений",2,5,45,2),
        new Quest("petition","Голос простых людей",2,4,"Фолк просит выяснить, какие припасы нужны храму.","Собрать сведения у Рорлунда","Рорлунд показывает список нуждающихся. Ты записываешь потребности, не раскрывая имён.","Фолк принимает точное прошение и обещает передать его на рассмотрение.","Отказаться от платы в пользу храма",2,4,35,3),
        new Quest("verse","Строка на полях",3,6,"Виармо ищет свидетеля, помнящего старый вариант городской баллады.","Расспросить Корпула о балладе","Корпул вспоминает слова старого посетителя. Ты сравниваешь их с записанным отрывком.","Виармо добавляет вариант в архив, сохранив оба текста.","Оставить копию как дар коллегии",1,2,32,2),
        new Quest("supplies","Честная поставка",1,5,"Генерал Туллий просит уточнить причину задержки партии крепежа.","Проверить партию у Бейранда","Бейранд показывает неудачную заготовку и запись о повторной ковке. Причина задержки подтверждена.","Туллий принимает отчёт и не возлагает вину на солдат.","Отказаться от денежной награды",2,8,50,3)
    };
    static String state(GameEngine g,Quest q){String s=g.progress.questOutcome(q.key());return s==null?"":s;}
    static boolean worked(GameEngine g,Quest q){return "COMPLETED".equals(g.progress.questOutcome(q.step()));}
    static String objective(GameEngine g,Quest q){
        String state=state(g,q);
        if("COMPLETED".equals(state))return "Завершено"+("COMPLETED".equals(g.progress.questOutcome(q.key()+"_gift"))?" | Без оплаты":" | Награда получена");
        if("ACTIVE".equals(state))return worked(g,q)?"Вернуться к "+NAMES[q.giver]:q.task;
        return q.offer;
    }
    static boolean availableForWork(GameEngine g,int d,int hours){return present(g,d)&&g.hour+hours<=CLOSE[d];}
    static String workHours(int d){return OPEN[d]+":00–"+CLOSE[d]+":00";}
    private static String npcKey(GameEngine g,int d){
        JSONArray a=g.sceneState.npcFacts();
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&NAMES[d].equalsIgnoreCase(o.optString("title")))return o.optString("id");}
        return "solitude_"+IDS[d];
    }
    private static boolean onDuty(GameEngine g,int d){return d>=0&&d<NAMES.length&&g.hour>=OPEN[d]&&g.hour<CLOSE[d];}
    static boolean present(GameEngine g,int d){
        if(d<0||d>=NAMES.length)return false;
        String status=g.sceneState.npcStatusByName(NAMES[d]);
        return g.hour>=OPEN[d]&&g.hour<CLOSE[d]&&!"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
    }
    private static JSONObject fact(GameEngine g,int d){
        String key=npcKey(g,d);JSONArray facts=g.sceneState.npcFacts();
        for(int i=0;i<facts.length();i++){JSONObject f=facts.optJSONObject(i);if(f!=null&&key.equals(f.optString("id")))return f;}
        return null;
    }
    private static String memory(GameEngine g,int d){
        JSONObject f=fact(g,d);if(f==null)return "";
        StringBuilder b=new StringBuilder("Отношение: ").append(f.optInt("relation",50)).append("/100");
        if(f.optInt("lastDay")>0)b.append(" | Последняя встреча: день ").append(f.optInt("lastDay"));
        for(Quest q:QUESTS)if(q.giver==d||q.target==d){
            if("COMPLETED".equals(state(g,q)))b.append("\nПомнит: ").append(q.title).append(" — завершено.").append("COMPLETED".equals(g.progress.questOutcome(q.key()+"_gift"))?" Ты отказался от платы.":"");
            else if("ACTIVE".equals(state(g,q)))b.append("\nПомнит поручение: ").append(q.title).append(". ").append(objective(g,q));
        }
        return b.toString();
    }
    private static void remember(GameEngine g,int d,String status){
        try{
            String key=npcKey(g,d);g.sceneState.setContext(g.day,g.hour,"Солитьюд");
            String old=g.sceneState.status("npc",key);
            if("HELPED".equals(old)||("INFORMED".equals(old)&&"MET".equals(status)))g.sceneState.revisitNpc(key);
            else g.sceneState.confirm("npc",key,NAMES[d],status);
        }catch(org.json.JSONException ignored){}
    }
    private static void report(GameEngine g,String message){
        g.message=message;g.progress.recent=message;g.recordVisualEvent(message);g.progress.sync(g);
    }
    static void choices(GameEngine g,GameEngine.Scene s,int d){
        if(onDuty(g,d)&&"HOSTILE".equals(g.sceneState.npcStatusByName(NAMES[d]))){
            s.add("SOLITUDE:supervisor:reconcile:"+d,"Примириться: "+NAMES[d],"Авторское действие | 1 час | 10 сил | без награды");
            return;
        }
        if(!present(g,d))return;
        JSONObject f=fact(g,d);
        s.add("SOLITUDE:talk:"+d,"Поговорить: "+NAMES[d],f==null?"Разговор и знакомство":"Отношение: "+f.optInt("relation",50)+"/100");
        s.add("SOLITUDE:local:"+d,"Расспросить "+NAMES[d]+" о городе","Местные сведения | авторский диалог");
        if(f!=null)s.add("SOLITUDE:memory:"+d,"Вспомнить прошлые встречи: "+NAMES[d],"Общие воспоминания и поручения");
        for(int i=0;i<QUESTS.length;i++){
            Quest q=QUESTS[i];String state=state(g,q);
            if(d==q.giver&&state.isEmpty())s.add("SOLITUDE:quest:"+i+":accept",q.title,"Авторское поручение | "+q.offer);
            if(d==q.target&&"ACTIVE".equals(state)&&!worked(g,q)&&canWork(g,d,q))
                s.add("SOLITUDE:quest:"+i+":work",q.task,q.hours+" ч. | −"+q.stamina+" запаса сил");
            if(d==q.giver&&"ACTIVE".equals(state)&&worked(g,q)){
                if(g.gold<=Integer.MAX_VALUE-q.gold)s.add("SOLITUDE:quest:"+i+":finish","Сдать: "+q.title,"+"+q.gold+" септимов | +"+q.rep+" репутации");
                s.add("SOLITUDE:quest:"+i+":gift",q.alternative,"Без оплаты | дополнительная репутация");
            }
        }
    }
    private static boolean canWork(GameEngine g,int d,Quest q){return present(g,d)&&g.hour+q.hours<=CLOSE[d]&&g.stamina>=q.stamina;}
    static boolean choose(GameEngine g,int d,String action){
        if(action.equals("SOLITUDE:supervisor:reconcile:"+d)){
            if(!onDuty(g,d)||!"HOSTILE".equals(g.sceneState.npcStatusByName(NAMES[d])))return true;
            if(g.hour>=23||g.stamina<10){report(g,"Для примирения нужны один час и 10 единиц запаса сил.");return true;}
            try{
                g.sceneState.setContext(g.day,g.hour,"Солитьюд");
                g.sceneState.confirm("npc",npcKey(g,d),NAMES[d],"INFORMED");
            }catch(org.json.JSONException ignored){return true;}
            g.stamina-=10;g.hour++;g.syncLivingSystems();
            report(g,NAMES[d]+": Конфликт улажен. Доступ к городским занятиям восстановлен.");return true;
        }
        if(!action.startsWith("SOLITUDE:talk:")&&!action.startsWith("SOLITUDE:quest:")&&!action.startsWith("SOLITUDE:local:")&&!action.startsWith("SOLITUDE:memory:"))return false;
        if(!present(g,d))return true;
        if(action.equals("SOLITUDE:talk:"+d)){
            JSONObject f=fact(g,d);String greeting=f==null?"":"HELPED".equals(f.optString("status"))?"Спасибо за прежнюю помощь. ":"Снова встретились. ";
            remember(g,d,"MET");report(g,NAMES[d]+": «"+greeting+TOPICS[d]+"»");return true;
        }
        if(action.equals("SOLITUDE:local:"+d)){
            remember(g,d,"MET");report(g,NAMES[d]+": «"+LOCAL[d]+"»");return true;
        }
        if(action.equals("SOLITUDE:memory:"+d)){
            if(fact(g,d)==null)return true;
            String remembered=memory(g,d);remember(g,d,"MET");report(g,NAMES[d]+"\n"+remembered);return true;
        }
        for(int i=0;i<QUESTS.length;i++){
            Quest q=QUESTS[i];String prefix="SOLITUDE:quest:"+i+":",state=state(g,q);
            if(action.equals(prefix+"accept")&&d==q.giver&&state.isEmpty()){
                g.progress.setQuestOutcome(q.key(),"ACTIVE");remember(g,d,"INFORMED");report(g,q.offer+"\n\nЦель: "+q.task);return true;
            }
            if(action.equals(prefix+"work")&&d==q.target&&"ACTIVE".equals(state)&&!worked(g,q)&&canWork(g,d,q)){
                g.stamina-=q.stamina;g.hour+=q.hours;g.syncLivingSystems();
                g.progress.setQuestOutcome(q.step(),"COMPLETED");remember(g,d,"INFORMED");report(g,q.work+"\n\n"+objective(g,q));return true;
            }
            boolean gift=action.equals(prefix+"gift");
            if((gift||action.equals(prefix+"finish"))&&d==q.giver&&"ACTIVE".equals(state)&&worked(g,q)){
                if(!gift&&g.gold>Integer.MAX_VALUE-q.gold)return true;
                g.progress.setQuestOutcome(q.key(),"COMPLETED");
                if(gift)g.progress.setQuestOutcome(q.key()+"_gift","COMPLETED");else g.gold+=q.gold;
                g.holdReputation[3]=Math.min(100,g.holdReputation[3]+q.rep+(gift?2:0));g.addXp(30);remember(g,d,"HELPED");
                report(g,q.finish+(gift?" Ты оставляешь денежную награду.":" Награда: "+q.gold+" септимов.")+"\n\n"+objective(g,q));return true;
            }
        }
        return true;
    }
}
