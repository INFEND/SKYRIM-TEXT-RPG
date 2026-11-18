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
    private static String npcKey(GameEngine g,int d){
        JSONArray a=g.sceneState.npcFacts();
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&NAMES[d].equalsIgnoreCase(o.optString("title")))return o.optString("id");}
        return "solitude_"+IDS[d];
    }
    private static boolean present(GameEngine g,int d){
        String status=g.sceneState.npcStatusByName(NAMES[d]);
        return g.hour>=OPEN[d]&&g.hour<CLOSE[d]&&!"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
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
        if(!present(g,d))return;
        s.add("SOLITUDE:talk:"+d,"Поговорить: "+NAMES[d],"Разговор и знакомство");
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
        if(!action.startsWith("SOLITUDE:talk:")&&!action.startsWith("SOLITUDE:quest:"))return false;
        if(!present(g,d))return true;
        if(action.equals("SOLITUDE:talk:"+d)){
            remember(g,d,"MET");report(g,NAMES[d]+": «"+TOPICS[d]+"»");return true;
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
