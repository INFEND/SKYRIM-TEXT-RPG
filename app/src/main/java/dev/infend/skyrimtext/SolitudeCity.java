package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.json.JSONArray;

/** Local activities stay inside the city; the atlas location remains Solitude. */
final class SolitudeCity {
    static final String MODE="SOLITUDE", PREFIX="SOLITUDE:";
    private static final String[] PLACES={
        "Главные ворота и рыночная площадь","Мрачный замок","Синий дворец","Коллегия бардов",
        "Храм Богов","Кузница Солитьюда","Смеющаяся крыса","Гавань Солитьюда"
    };
    private static final String[] PROSE={
        "Каменная арка возвышается над дорогой. На площади рядом с плахой стражники следят за толпой, а торговцы раскладывают товары.",
        "Внутри крепостных стен гремит оружие. Имперские солдаты сверяют караульные списки; вдалеке офицер поправляет карту владения.",
        "Свет проникает через высокие окна. Придворные негромко обсуждают прошения жителей, пока дворцовая стража наблюдает за посетителями.",
        "За дверями слышны струны и голоса учеников. Старые песни здесь разбирают с той же тщательностью, с какой кузнец правит лезвие.",
        "У алтарей горят свечи. Сюда приходят не только за благословением, но и за помощью в трудную минуту.",
        "Молот отбивает ритм над раскалённой наковальней. Бейранд следит за заготовками и просит не трогать горячий металл без клещей.",
        "В трактире пахнет тушёным мясом и элем. Из-за столов доносятся дорожные истории; хозяин присматривает за залом.",
        "Под каменной аркой скрипят мачты. Канаты намокли от морской воды; грузчики торопятся закончить работу до прилива."
    };
    // Positive gold = wages, negative = paid service. Skill -1 = no experience.
    private static final class Job {
        final String title, result; final int skill,hours,stamina,gold,xp,rep;
        Job(String t,String r,int k,int h,int s,int g,int x,int p){title=t;result=r;skill=k;hours=h;stamina=s;gold=g;xp=x;rep=p;}
    }
    private static final Job[] JOBS={
        new Job("Помочь торговцам с товаром","Ты сверяешь ящики с ведомостью и разгружаешь телегу. Торговцы благодарят за работу.",12,2,10,18,12,1),
        new Job("Выслушать городской дозор","Стражник объясняет, почему у ворот проверяют повозки. Ты запоминаешь местные порядки.",12,1,0,0,10,0),
        new Job("Тренировка со щитом","На учебном дворе ты отрабатываешь стойку и отвод удара под наблюдением солдата.",3,2,14,-12,22,0),
        new Job("Помочь с армейскими припасами","Ты пересчитываешь ремни, щиты и мешки зерна для гарнизона.",12,2,8,22,12,1),
        new Job("Помочь составить прошение","Ты помогаешь посетителю изложить просьбу ясно и без лишних обвинений.",12,1,2,0,16,1),
        new Job("Изучить придворный этикет","Ты наблюдаешь порядок приёма и учишься обращаться к знати без нарушения правил.",12,2,0,0,18,0),
        new Job("Урок сценической речи","Ты повторяешь строфы и исправляешь дыхание под руководством барда.",12,2,4,-15,24,0),
        new Job("Переписать старую балладу","Ты помогаешь разобрать выцветший текст и получаешь плату за чистую копию.",12,2,3,16,14,1),
        new Job("Упражнение в восстановлении","Ты изучаешь основы лечения под присмотром служителя храма.",14,2,4,-10,22,0),
        new Job("Помочь нуждающимся","Ты разносишь воду и припасы тем, кто пришёл за помощью.",14,2,8,0,12,2),
        new Job("Поработать у наковальни","Ты поддерживаешь жар горна и обрабатываешь простую заготовку.",6,2,12,20,18,1),
        new Job("Урок кузнечного дела","Бейранд показывает, как отличить перегретую сталь и исправить неровную кромку.",6,2,8,-18,26,0),
        new Job("Помочь хозяину трактира","Ты убираешь столы и приносишь припасы из кладовой.",12,2,8,15,12,1),
        new Job("Поесть и выпить воды","Горячая похлёбка и вода возвращают силы после долгой дороги.",-1,1,0,-8,0,0),
        new Job("Разгрузить торговое судно","Ты переносишь груз с причала и помогаешь закрепить пустые бочки.",-1,3,18,30,0,1),
        new Job("Проверить грузовую ведомость","Ты сверяешь метки на ящиках с записями и находишь ошибку в счёте.",12,2,4,19,16,1),
        new Job("Разобрать рыночные весы","Ты проверяешь гири и очищаешь чаши весов.",12,1,3,9,8,0),
        new Job("Упражнение с мечом","Под присмотром гарнизона ты отрабатываешь безопасные выпады.",0,2,12,-14,22,0),
        new Job("Разобрать архив прошений","Ты раскладываешь бумаги по датам и передаёшь их управителю.",12,2,3,12,10,1),
        new Job("Репетиция баллады","Ты разучиваешь строфы и упражняешься в выразительном чтении.",12,1,2,-8,14,0),
        new Job("Уход за храмом","Ты очищаешь подсвечники и подметаешь пол у алтарей.",14,1,4,0,8,1),
        new Job("Подготовить кузнечные инструменты","Ты очищаешь клещи и сортируешь заготовки для Бейранда.",6,1,6,10,10,0),
        new Job("Подготовить зал трактира","Ты расставляешь кружки и готовишь столы к приёму гостей.",12,1,4,8,8,0),
        new Job("Проверить причальные канаты","Ты осматриваешь крепления и заменяешь изношенные верёвки из запасов порта.",-1,2,10,17,0,1),
        new Job("Упаковать товары","Ты оборачиваешь хрупкие товары тканью и подписываешь ящики.",12,1,4,10,8,0),
        new Job("Осмотреть учебные щиты","Ты проверяешь ремни и крепления учебного снаряжения.",3,1,5,10,8,0),
        new Job("Переписать опись дворца","Ты сверяешь перечень имущества с архивной копией.",12,2,3,14,10,1),
        new Job("Настроить инструменты","Ты помогаешь ученикам подготовить инструменты к занятию.",12,1,2,8,8,0),
        new Job("Подготовить храмовые припасы","Ты сортируешь чистые повязки и сосуды с водой.",14,1,4,0,10,1),
        new Job("Очистить кузнечный двор","Ты убираешь окалину и складываешь уголь под навесом.",6,1,7,11,10,0),
        new Job("Разнести припасы трактира","Ты переносишь мешки муки и корзины овощей в кладовую.",12,1,7,10,8,0),
        new Job("Осмотреть грузовые сети","Ты проверяешь узлы и откладываешь повреждённые сети.",-1,1,6,12,0,1)
    };
    // Preserve the original 0–15 save/action IDs; new jobs are appended.
    private static int jobDistrict(int i){return i<16?i/2:(i-16)%8;}
    private final int[] lastDay=new int[JOBS.length];
    {java.util.Arrays.fill(lastDay,-1);} // Day zero must allow the first shift.
    // Authored multi-day city work commission, distinct from canonical Skyrim quests.
    private final int[] workDays=new int[8], workLastDay=new int[8];
    {java.util.Arrays.fill(workLastDay,-1);} // Day zero is a valid first working day.
    private final boolean[] workRewarded=new boolean[8];
    // Only actual civic service counts towards a three-day commission.
    // Lessons, meals, paid training and observation cannot earn a work bonus.
    // Existing job IDs are stable across saves and OTA updates.
    static boolean commissionEligible(int job){
        switch(job){
            case 0: case 3: case 4: case 7: case 9: case 10:
            case 12: case 14: case 15: case 16: case 18: case 20:
            case 21: case 22: case 23: case 24: case 25: case 26:
            case 27: case 28: case 29: case 30: case 31:
                return true;
            default: return false;
        }
    }
    private void recordWorkDay(int d,int day){
        if(!workRewarded[d]&&workDays[d]<3&&workLastDay[d]!=day){
            workDays[d]++;workLastDay[d]=day;
        }
    }
    // One city-wide register shows every district's commission without moving the
    // player or granting remote rewards. The actual supervisor still pays in person.
    private String commissionLedger(){
        StringBuilder out=new StringBuilder("Авторские трудовые ведомости Солитьюда.");
        for(int i=0;i<PLACES.length;i++){
            out.append("\n").append(PLACES[i]).append(": ");
            if(workRewarded[i])out.append("награда получена");
            else if(workDays[i]>=3)out.append("3/3, награда у ответственного NPC");
            else out.append(workDays[i]).append("/3 смен в разные дни");
        }
        return out.toString();
    }
    // Read-only overview of all established city contacts, including off-duty NPCs.
    // Uses the shared SceneState facts rather than inventing a second reputation ledger.
    private static String relationshipLedger(GameEngine g){
        StringBuilder out=new StringBuilder("Жители Солитьюда | отношения и доступность");
        java.util.HashSet<String> seen=new java.util.HashSet<>();
        org.json.JSONArray facts=g.sceneState.npcFacts();
        for(int group=0;group<2;group++){
            String[] names=group==0?SolitudeEncounters.NAMES:SolitudeResidents.NAMES;
            for(int i=0;i<names.length;i++){
                String name=names[i];
                if(!seen.add(name.toLowerCase(java.util.Locale.ROOT)))continue;
                org.json.JSONObject fact=null;
                for(int k=0;k<facts.length();k++){
                    org.json.JSONObject candidate=facts.optJSONObject(k);
                    if(candidate!=null&&name.equalsIgnoreCase(candidate.optString("title"))){fact=candidate;break;}
                }
                String status=g.sceneState.npcStatusByName(name);
                boolean unavailable="DEAD".equals(status)||"MISSING".equals(status)||"HOSTILE".equals(status);
                boolean present=group==0?SolitudeEncounters.present(g,i):
                    SolitudeResidents.present(g,i,SolitudeResidents.district(i,g.hour));
                out.append("\n• ").append(name).append(": ");
                out.append(fact==null?"ещё не знакомы":"отношение "+fact.optInt("relation",50)+"/100");
                out.append(" | ");
                if("DEAD".equals(status))out.append("погиб");
                else if("MISSING".equals(status))out.append("пропал");
                else if("HOSTILE".equals(status))out.append("конфликт");
                else if(present)out.append("доступен сейчас");
                else if(!unavailable)out.append("сейчас отсутствует");
                if(fact!=null&&fact.optInt("lastDay",-1)>=0)
                    out.append(" | встреча: день ").append(fact.optInt("lastDay"));
            }
        }
        return out.toString();
    }
    private void commissionChoices(GameEngine g,GameEngine.Scene s,int d){
        if(workRewarded[d])return;
        if(workDays[d]<3)s.add(PREFIX+"work-progress:"+d,"Трудовая ведомость",
            "Авторская цепочка | "+workDays[d]+"/3 смен в разные дни");
        else {
            s.add(PREFIX+"work-reward:"+d,"Получить награду за три смены",
                "Авторская цепочка | 40 септимов, +2 репутации | требуется присутствие ответственного");
            s.add(PREFIX+"work-donate:"+d,"Передать награду на нужды города",
                "Авторская цепочка | без септимов, +4 репутации | та же однократная награда");
        }
    }
    private SolitudeShops shops=new SolitudeShops();
    private SolitudeHome home=new SolitudeHome();
    boolean homeEnchanting(GameEngine g){return SolitudeHome.MODE.equals(g.mode)&&SolitudeHome.owns(g)&&home.room(4);}
    boolean homeLaboratory(GameEngine g){return SolitudeHome.MODE.equals(g.mode)&&SolitudeHome.owns(g)&&home.room(3);}
    void companionChoices(GameEngine g,GameEngine.Scene s){home.companionChoices(g,s);}
    void companionAction(GameEngine g,String a){home.companionAction(g,a);}
    int jordisDamage(GameEngine g,int turn,int remaining){return home.jordisActive(g)&&turn%2==0?Math.min(Math.max(0,remaining),4+Math.min(10,g.level/5)+home.jordisWeaponBonus()):0;}
    int jordisProtection(GameEngine g,int incoming,boolean defending){return defending&&home.jordisActive(g)?home.jordisProtection(incoming):0;}
    private SolitudeJustice justice=new SolitudeJustice();
    private final SolitudeFence fence=new SolitudeFence();
    boolean confined(){return justice.confined();}
    boolean custody(){return confined()||justice.detained();}
    void beginSentence(GameEngine g){justice.begin(g);}
    void reconcilePrison(GameEngine g){if(justice.confined()&&here(g))g.mode=SolitudeJustice.CELL;else if(justice.detained()&&here(g))g.mode=SolitudeJustice.MODE;else if(SolitudeJustice.CELL.equals(g.mode))g.mode=SolitudeJustice.MODE;}
    static boolean here(GameEngine g){return g.started&&g.region==3&&"Солитьюд".equals(g.progress.location);}
    static boolean isMode(String m){return MODE.equals(m)||(m!=null&&m.startsWith(PREFIX));}
    private static int district(String mode){
        for(int i=0;i<PLACES.length;i++)if((PREFIX+i).equals(mode))return i;
        return -1;
    }
    static boolean validMode(String mode){return MODE.equals(mode)||district(mode)>=0||SolitudeShops.index(mode)>=0||SolitudeHome.MODE.equals(mode)||SolitudeJustice.MODE.equals(mode)||SolitudeJustice.CELL.equals(mode)||SolitudeCanonical.graceMode(mode);}
    private String blocked(GameEngine g,int i){return blocked(g,i,false);}
    private String blocked(GameEngine g,int i,boolean volunteer){
        Job j=JOBS[i];
        if(lastDay[i]>=g.day)return "Уже выполнено сегодня";
        int d=jobDistrict(i);int start=d==6?6:8, end=d==6?23:20;
        if(g.hour<start||g.hour+j.hours>end)return "Доступно "+start+":00–"+end+":00";
        if(!SolitudeEncounters.availableForWork(g,d,j.hours))return "Ответственный житель недоступен | "+SolitudeEncounters.workHours(d);
        if(g.stamina<j.stamina)return "Не хватает запаса сил";
        if(j.gold<0&&g.gold< -j.gold)return "Не хватает септимов";
        if(j.gold>0&&!volunteer&&g.gold>Integer.MAX_VALUE-j.gold)return "Кошелёк переполнен";
        return "";
    }
    // Existing residents supervise local shifts. Work strengthens the shared NPC ledger,
    // rather than creating a second, incompatible relationship system.
    private static final int[] WORK_CONTACT={8,7,0,3,-1,-1,1,2};
    private static void rememberWork(GameEngine g,int district,boolean residentPresent,boolean supervisorPresent,boolean volunteer){
        int resident=WORK_CONTACT[district];
        if(!residentPresent&&!supervisorPresent)return;
        // Prefer the actual district supervisor, including Vittoria at the docks.
        // A resident is credited only if present when the shift began.
        String key=supervisorPresent?"solitude_"+new String[]{"jala","tullius","falk","viarmo","rorlund","beirand","corpulus","vittoria"}[district]:
            "solitude_"+SolitudeResidents.IDS[resident];
        String name=supervisorPresent?SolitudeEncounters.NAMES[district]:SolitudeResidents.NAMES[resident];
        try{
            g.sceneState.setContext(g.day,g.hour,"Солитьюд");
            String status=g.sceneState.status("npc",key);
            if(volunteer&&!"HELPED".equals(status)&&!"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status))g.sceneState.confirm("npc",key,name,"HELPED");
            else if(status.isEmpty())g.sceneState.confirm("npc",key,name,"MET");
            else if("MET".equals(status))g.sceneState.confirm("npc",key,name,"INFORMED");
            else if("INFORMED".equals(status))g.sceneState.confirm("npc",key,name,"HELPED");
            else g.sceneState.revisitNpc(key);
        }catch(org.json.JSONException ignored){/* Never undo an already completed shift. */}
    }
    private String detail(GameEngine g,int i){
        Job j=JOBS[i];String why=blocked(g,i);
        return (why.isEmpty()?"":why+" | ")+"Авторское занятие | "+j.hours+" ч."+(j.stamina>0?" | −"+j.stamina+" запаса сил":"")+
            (j.gold==0?"":" | "+(j.gold>0?"+":"")+j.gold+" септимов")+
            (j.skill<0?"":" | "+RpgContent.SKILLS[j.skill]);
    }
    GameEngine.Scene scene(GameEngine g){
        if(justice.confined())return justice.cell(g);
        if(SolitudeJustice.MODE.equals(g.mode))return justice.scene(g);
        if(SolitudeHome.MODE.equals(g.mode))return home.scene(g);
        if(SolitudeCanonical.graceMode(g.mode))return SolitudeCanonical.graceScene(g);
        int store=SolitudeShops.index(g.mode);if(store>=0)return shops.scene(g,store);
        int d=district(g.mode);
        if(d<0){
            GameEngine.Scene s=new GameEngine.Scene("СОЛИТЬЮД | ДЕНЬ "+g.day,"Жизнь столицы",
                "Над городом возвышается Синий дворец, под аркой шумит море. Выбери, куда отправиться. Местные поручения и уроки имеют свои часы работы и не требуют перехода на глобальной карте."+(g.message.isEmpty()?"":"\n\n"+g.message));
            for(int i=0;i<PLACES.length;i++)s.add(PREFIX+"visit:"+i,PLACES[i],PROSE[i]);
            s.add(SolitudeCanonical.ADDVAR,"Дом Аддвара","Жилой дом торговца рыбой");
            s.add(SolitudeCanonical.CATACOMBS,"Катакомбы Солитьюда","Место погребения жителей");
            s.add("SOLITUDE:home","Высокий шпиль",SolitudeHome.owns(g)?"Собственный дом":"Поместье | покупка в Синем дворце");
            s.add(PREFIX+"work-ledger","Трудовые ведомости","Все восемь районов | учёт смен, выполненные поручения и невыплаченные награды");
            s.add(PREFIX+"relations-ledger","Отношения с жителями","Общий реестр знакомых, отношений и доступности | только просмотр");
            return s.add("GO_WORLD","Вернуться к приключению","Открыть обычные действия города");
        }
        GameEngine.Scene s=new GameEngine.Scene("СОЛИТЬЮД | "+g.hour+":00",PLACES[d],PROSE[d]+
            "\n\n"+(g.message.isEmpty()?"Выбери занятие или вернись на улицу.":g.message));
        for(int i=0;i<JOBS.length;i++)if(jobDistrict(i)==d){
            String paidBlock=blocked(g,i);
            String freeBlock=JOBS[i].gold>0?blocked(g,i,true):paidBlock;
            if(paidBlock.isEmpty())s.add(PREFIX+"job:"+i,JOBS[i].title,detail(g,i));
            if(JOBS[i].gold>0&&freeBlock.isEmpty())s.add(PREFIX+"volunteer:"+i,"Помочь без оплаты: "+JOBS[i].title,
                "Авторская добровольная помощь | без заработка | общая память отношений NPC | тот же дневной лимит");
            if(!paidBlock.isEmpty()&&!freeBlock.isEmpty())s=newBlocked(s,g,i);
            else if(!paidBlock.isEmpty()){
                GameEngine.Scene next=new GameEngine.Scene(s.kicker,s.title,
                    s.body+"\n\n"+JOBS[i].title+": "+paidBlock+" (оплачиваемая смена).");
                next.choices.addAll(s.choices);
                s=next;
            }
        }
        if(d==0||d==5)s.add(PREFIX+"shop","Открыть торговлю","Существующий торговый инвентарь города");
        if(d==6)s.add(PREFIX+"rest","Снять комнату","20 септимов | существующий отдых в трактире");
        if(d==1){String old=g.mode;g.mode="WORLD";boolean available=g.livia.available(g);g.mode=old;
            if(available)s.add(PREFIX+"livia","Поговорить с Ливией",g.livia.objective());}
        for(int i=0;i<SolitudeShops.NAMES.length;i++)if(SolitudeShops.district(i)==d)s.add(PREFIX+"store:"+i,SolitudeShops.NAMES[i],SolitudeShops.OWNERS[i]+" | "+(SolitudeShops.open(g,i)?"Открыто":"Закрыто (8:00–20:00)"));
        if(d==2)home.palace(g,s);
        if(d==1){SolitudeJustice.choices(g,s);justice.trespassChoice(g,s);SolitudeCanonical.castleChoices(g,s);}
        if(d==0)justice.theftChoice(g,s);
        if(d==7)fence.choices(g,s);
        SolitudeCanonical.districtChoices(g,s,d);
        SolitudeEncounters.choices(g,s,d);
        SolitudeResidents.choices(g,s,d);
        commissionChoices(g,s,d);
        if(d==5)SolitudeCraft.choices(g,s);
        return s.add(PREFIX+"back","На улицы Солитьюда","Вернуться к выбору районов").add("GO_WORLD","Завершить прогулку","Вернуться к приключению");
    }
    private GameEngine.Scene newBlocked(GameEngine.Scene s,GameEngine g,int i){
        GameEngine.Scene next=new GameEngine.Scene(s.kicker,s.title,s.body+"\n\n"+JOBS[i].title+": "+blocked(g,i)+".");
        next.choices.addAll(s.choices);return next;
    }
    void choose(GameEngine g,String action){
        if(!here(g)||g.road.pending()||"COMBAT".equals(g.mode)||g.hp<=0)return;
        if(justice.confined()){justice.cellAction(g,action);return;}
        if(justice.detained()){justice.choose(g,action);return;}
        if((PREFIX+"open").equals(action)&&"WORLD".equals(g.mode)){g.mode=MODE;g.message="";return;}
        if(!validMode(g.mode))return;
        if("GO_WORLD".equals(action)){g.mode="WORLD";return;}
        if((PREFIX+"back").equals(action)){g.mode=MODE;g.message="";return;}
        if(SolitudeJustice.MODE.equals(g.mode)){justice.choose(g,action);return;}
        if(SolitudeCraft.choose(g,action))return;
        if(fence.choose(g,action))return;
        if(SolitudeCanonical.choose(g,action))return;
        if(SolitudeCanonical.graceMode(g.mode))return;
        if(SolitudeHome.MODE.equals(g.mode)){home.choose(g,action);return;}
        int store=SolitudeShops.index(g.mode);if(store>=0){shops.choose(g,store,action);return;}
        int d=district(g.mode);
        if(MODE.equals(g.mode)){
            if((PREFIX+"relations-ledger").equals(action)){
                g.message=relationshipLedger(g);g.progress.recent=g.message;g.progress.sync(g);return;
            }
            if((PREFIX+"work-ledger").equals(action)){
                g.message=commissionLedger();g.progress.recent=g.message;g.progress.sync(g);return;
            }
            if(SolitudeCanonical.graceMode(action)){g.mode=action;g.message="";return;}
            if(SolitudeHome.MODE.equals(action)){g.mode=SolitudeHome.MODE;g.message="";return;}
            for(int i=0;i<PLACES.length;i++)if((PREFIX+"visit:"+i).equals(action)){g.mode=PREFIX+i;g.message="";return;}
            return;
        }
        for(int i=0;i<SolitudeShops.NAMES.length;i++)if(d==SolitudeShops.district(i)&&(PREFIX+"store:"+i).equals(action)){g.mode=SolitudeShops.MODE+i;g.message="";return;}
        if(d==2&&home.palaceAction(g,action))return;
        if(d==1&&justice.choose(g,action))return;
        if((d==0||d==1)&&justice.theft(g,action))return;
        if(SolitudeEncounters.choose(g,d,action))return;
        if(SolitudeResidents.choose(g,d,action))return;
        if(d>=0&&action.equals(PREFIX+"work-progress:"+d)){
            g.message="Трудовая ведомость: "+workDays[d]+"/3 смен в разные дни.";
            g.progress.recent=g.message;g.progress.sync(g);return;
        }
        if(d>=0&&(action.equals(PREFIX+"work-reward:"+d)||action.equals(PREFIX+"work-donate:"+d))){
            boolean donate=action.equals(PREFIX+"work-donate:"+d);
            if(workRewarded[d]||workDays[d]<3||!SolitudeEncounters.present(g,d))return;
            if(!donate&&g.gold>Integer.MAX_VALUE-40){g.message="Кошелёк переполнен.";return;}
            workRewarded[d]=true;if(!donate)g.gold+=40;
            // Record the completed contract in the same persistent NPC ledger
            // used by dialogue and civic work; never create a parallel reputation.
            try{
                String key="solitude_"+new String[]{"jala","tullius","falk","viarmo","rorlund","beirand","corpulus","vittoria"}[d];
                g.sceneState.setContext(g.day,g.hour,"Солитьюд");
                String old=g.sceneState.status("npc",key);
                if(!"DEAD".equals(old)&&!"MISSING".equals(old)&&!"HOSTILE".equals(old)){
                    if(!"HELPED".equals(old))g.sceneState.confirm("npc",key,SolitudeEncounters.NAMES[d],"HELPED");
                    else g.sceneState.revisitNpc(key);
                }
            }catch(org.json.JSONException ignored){/* Payment remains one-time and persistent. */}
            g.holdReputation[3]=Math.min(100,g.holdReputation[3]+(donate?4:2));
            g.addXp(40);
            g.message=donate?"Авторская трудовая ведомость закрыта: награда передана городу, +4 репутации, +40 опыта.":
                "Авторская трудовая ведомость закрыта: +40 септимов, +2 репутации, +40 опыта.";
            g.progress.recent=g.message;g.recordVisualEvent(g.message);g.progress.sync(g);return;
        }
        if((PREFIX+"shop").equals(action)&&(d==0||d==5)){g.mode="WORLD";g.choose("SHOP");return;}
        if((PREFIX+"rest").equals(action)&&d==6){g.mode="WORLD";g.choose("REST");return;}
        if((PREFIX+"livia").equals(action)&&d==1){g.mode="WORLD";if(g.livia.available(g)){g.choose("LIVIA:open");g.livia.castleReturn=LiviaStory.MODE.equals(g.mode);}else g.mode=PREFIX+d;return;}
        for(int i=0;i<JOBS.length;i++)if(jobDistrict(i)==d&&((PREFIX+"job:"+i).equals(action)||(JOBS[i].gold>0&&(PREFIX+"volunteer:"+i).equals(action)))){
            boolean volunteer=(PREFIX+"volunteer:"+i).equals(action);
            if(!blocked(g,i,volunteer).isEmpty())return;
            Job j=JOBS[i];
            // Snapshot attendance before advancing time: the supervisor may leave
            // during a legitimate shift, but completed work must still count.
            int resident=WORK_CONTACT[d];
            boolean residentPresent=resident>=0&&SolitudeResidents.present(g,resident,d);
            boolean supervisorPresent=SolitudeEncounters.present(g,d);
            lastDay[i]=g.day;
            if(commissionEligible(i))recordWorkDay(d,g.day);
            g.stamina-=j.stamina;g.gold=GameEngine.nonnegativeAdd(g.gold,volunteer?0:j.gold);
            g.hour+=j.hours; // opening hours prevent a midnight rollover
            g.syncLivingSystems();
            if(i==13){g.needs.eat();g.needs.drink();g.stamina=Math.min(g.maxStamina,GameEngine.nonnegativeAdd(g.stamina,15));}
            if(j.skill>=0)g.progress.skillExperience(g,j.skill,j.xp);
            if(j.rep>0)g.holdReputation[3]=Math.min(100,g.holdReputation[3]+j.rep);
            rememberWork(g,d,residentPresent,supervisorPresent,volunteer);
            g.message=j.result+(volunteer?" Ты отказался от платы и помог безвозмездно.":"");
            g.progress.recent=g.message;
            g.recordVisualEvent(j.title+": "+j.result);
            g.progress.sync(g);return;
        }
    }
    JSONObject save(){JSONObject o=new JSONObject();try{o.put("lastDay",new JSONArray(lastDay)).put("workDays",new JSONArray(workDays)).put("workLastDay",new JSONArray(workLastDay)).put("workRewarded",new JSONArray(workRewarded)).put("shops",shops.save()).put("home",home.save()).put("justice",justice.save()).put("fenceDay",fence.save());}catch(Exception ignored){}return o;}
    static SolitudeCity restore(JSONObject o){
        SolitudeCity s=new SolitudeCity();JSONArray a=o==null?null:o.optJSONArray("lastDay");
        if(a!=null)for(int i=0;i<Math.min(a.length(),s.lastDay.length);i++)s.lastDay[i]=Math.max(-1,a.optInt(i,-1));
        JSONArray wd=o==null?null:o.optJSONArray("workDays");
        JSONArray wl=o==null?null:o.optJSONArray("workLastDay");
        JSONArray wr=o==null?null:o.optJSONArray("workRewarded");
        for(int i=0;i<8;i++){
            if(wd!=null)s.workDays[i]=Math.min(3,Math.max(0,wd.optInt(i)));
            if(wl!=null)s.workLastDay[i]=Math.max(-1,wl.optInt(i,-1));
            if(wr!=null)s.workRewarded[i]=wr.optBoolean(i);
        }
        s.shops=SolitudeShops.restore(o==null?null:o.optJSONObject("shops"));
        s.home=SolitudeHome.restore(o==null?null:o.optJSONObject("home"));
        s.justice=SolitudeJustice.restore(o==null?null:o.optJSONObject("justice"));
        s.fence.restore(o==null?0:o.optInt("fenceDay"));
        return s;
    }
}
