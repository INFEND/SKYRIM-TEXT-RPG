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
        new Job("Проверить грузовую ведомость","Ты сверяешь метки на ящиках с записями и находишь ошибку в счёте.",12,2,4,19,16,1)
    };
    private final int[] lastDay=new int[JOBS.length];
    private SolitudeShops shops=new SolitudeShops();
    private SolitudeHome home=new SolitudeHome();
    static boolean here(GameEngine g){return g.started&&g.region==3&&"Солитьюд".equals(g.progress.location);}
    static boolean isMode(String m){return MODE.equals(m)||(m!=null&&m.startsWith(PREFIX));}
    private static int district(String mode){
        for(int i=0;i<PLACES.length;i++)if((PREFIX+i).equals(mode))return i;
        return -1;
    }
    static boolean validMode(String mode){return MODE.equals(mode)||district(mode)>=0||SolitudeShops.index(mode)>=0||SolitudeHome.MODE.equals(mode)||SolitudeJustice.MODE.equals(mode);}
    private String blocked(GameEngine g,int i){
        Job j=JOBS[i];
        if(lastDay[i]>=g.day)return "Уже выполнено сегодня";
        int start=i/2==6?6:8, end=i/2==6?23:20;
        if(g.hour<start||g.hour+j.hours>end)return "Доступно "+start+":00–"+end+":00";
        if(g.stamina<j.stamina)return "Не хватает запаса сил";
        if(j.gold<0&&g.gold< -j.gold)return "Не хватает септимов";
        if(j.gold>0&&g.gold>Integer.MAX_VALUE-j.gold)return "Кошелёк переполнен";
        return "";
    }
    private String detail(GameEngine g,int i){
        Job j=JOBS[i];String why=blocked(g,i);
        return (why.isEmpty()?"":why+" | ")+j.hours+" ч."+(j.stamina>0?" | −"+j.stamina+" запаса сил":"")+
            (j.gold==0?"":" | "+(j.gold>0?"+":"")+j.gold+" септимов")+
            (j.skill<0?"":" | "+RpgContent.SKILLS[j.skill]);
    }
    GameEngine.Scene scene(GameEngine g){
        if(SolitudeJustice.MODE.equals(g.mode))return SolitudeJustice.scene(g);
        if(SolitudeHome.MODE.equals(g.mode))return home.scene(g);
        int store=SolitudeShops.index(g.mode);if(store>=0)return shops.scene(g,store);
        int d=district(g.mode);
        if(d<0){
            GameEngine.Scene s=new GameEngine.Scene("СОЛИТЬЮД | ДЕНЬ "+g.day,"Жизнь столицы",
                "Над городом возвышается Синий дворец, под аркой шумит море. Выбери, куда отправиться. Местные поручения и уроки имеют свои часы работы и не требуют перехода на глобальной карте.");
            for(int i=0;i<PLACES.length;i++)s.add(PREFIX+"visit:"+i,PLACES[i],PROSE[i]);
            s.add("SOLITUDE:home","Высокий шпиль",SolitudeHome.owns(g)?"Собственный дом":"Поместье | покупка в Синем дворце");
            return s.add("GO_WORLD","Вернуться к приключению","Открыть обычные действия города");
        }
        GameEngine.Scene s=new GameEngine.Scene("СОЛИТЬЮД | "+g.hour+":00",PLACES[d],PROSE[d]+
            "\n\n"+(g.message.isEmpty()?"Выбери занятие или вернись на улицу.":g.message));
        for(int i=d*2;i<d*2+2;i++)if(blocked(g,i).isEmpty())s.add(PREFIX+"job:"+i,JOBS[i].title,detail(g,i));
        else s=newBlocked(s,g,i);
        if(d==0||d==5)s.add(PREFIX+"shop","Открыть торговлю","Существующий торговый инвентарь города");
        if(d==6)s.add(PREFIX+"rest","Снять комнату","20 септимов | существующий отдых в трактире");
        if(d==1){String old=g.mode;g.mode="WORLD";boolean available=g.livia.available(g);g.mode=old;
            if(available)s.add(PREFIX+"livia","Поговорить с Ливией",g.livia.objective());}
        for(int i=0;i<SolitudeShops.NAMES.length;i++)if(SolitudeShops.district(i)==d)s.add(PREFIX+"store:"+i,SolitudeShops.NAMES[i],SolitudeShops.OWNERS[i]+" | "+(SolitudeShops.open(g,i)?"Открыто":"Закрыто (8:00–20:00)"));
        if(d==2)home.palace(g,s);
        if(d==1)SolitudeJustice.choices(g,s);
        SolitudeEncounters.choices(g,s,d);
        return s.add(PREFIX+"back","На улицы Солитьюда","Вернуться к выбору районов").add("GO_WORLD","Завершить прогулку","Вернуться к приключению");
    }
    private GameEngine.Scene newBlocked(GameEngine.Scene s,GameEngine g,int i){
        GameEngine.Scene next=new GameEngine.Scene(s.kicker,s.title,s.body+"\n\n"+JOBS[i].title+": "+blocked(g,i)+".");
        next.choices.addAll(s.choices);return next;
    }
    void choose(GameEngine g,String action){
        if(!here(g)||g.road.pending()||"COMBAT".equals(g.mode)||g.hp<=0)return;
        if((PREFIX+"open").equals(action)&&"WORLD".equals(g.mode)){g.mode=MODE;g.message="";return;}
        if(!validMode(g.mode))return;
        if("GO_WORLD".equals(action)){g.mode="WORLD";return;}
        if((PREFIX+"back").equals(action)){g.mode=MODE;g.message="";return;}
        if(SolitudeJustice.MODE.equals(g.mode)){SolitudeJustice.choose(g,action);return;}
        if(SolitudeHome.MODE.equals(g.mode)){home.choose(g,action);return;}
        int store=SolitudeShops.index(g.mode);if(store>=0){shops.choose(g,store,action);return;}
        int d=district(g.mode);
        if(MODE.equals(g.mode)){
            if(SolitudeHome.MODE.equals(action)){g.mode=SolitudeHome.MODE;g.message="";return;}
            for(int i=0;i<PLACES.length;i++)if((PREFIX+"visit:"+i).equals(action)){g.mode=PREFIX+i;g.message="";return;}
            return;
        }
        for(int i=0;i<SolitudeShops.NAMES.length;i++)if(d==SolitudeShops.district(i)&&(PREFIX+"store:"+i).equals(action)){g.mode=SolitudeShops.MODE+i;g.message="";return;}
        if(d==2&&home.palaceAction(g,action))return;
        if(d==1&&SolitudeJustice.choose(g,action))return;
        if(SolitudeEncounters.choose(g,d,action))return;
        if((PREFIX+"shop").equals(action)&&(d==0||d==5)){g.mode="WORLD";g.choose("SHOP");return;}
        if((PREFIX+"rest").equals(action)&&d==6){g.mode="WORLD";g.choose("REST");return;}
        if((PREFIX+"livia").equals(action)&&d==1){g.mode="WORLD";if(g.livia.available(g)){g.choose("LIVIA:open");g.livia.castleReturn=LiviaStory.MODE.equals(g.mode);}else g.mode=PREFIX+d;return;}
        for(int i=d*2;i<d*2+2;i++)if((PREFIX+"job:"+i).equals(action)){
            if(!blocked(g,i).isEmpty())return;
            Job j=JOBS[i];lastDay[i]=g.day;
            g.stamina-=j.stamina;g.gold=GameEngine.nonnegativeAdd(g.gold,j.gold);
            g.hour+=j.hours; // opening hours prevent a midnight rollover
            g.syncLivingSystems();
            if(i==13){g.needs.eat();g.needs.drink();g.stamina=Math.min(g.maxStamina,GameEngine.nonnegativeAdd(g.stamina,15));}
            if(j.skill>=0)g.progress.skillExperience(g,j.skill,j.xp);
            if(j.rep>0)g.holdReputation[3]=Math.min(100,g.holdReputation[3]+j.rep);
            g.message=j.result;
            g.progress.recent=g.message;
            g.recordVisualEvent(j.title+": "+j.result);
            g.progress.sync(g);return;
        }
    }
    JSONObject save(){JSONObject o=new JSONObject();try{o.put("lastDay",new JSONArray(lastDay)).put("shops",shops.save()).put("home",home.save());}catch(Exception ignored){}return o;}
    static SolitudeCity restore(JSONObject o){
        SolitudeCity s=new SolitudeCity();JSONArray a=o==null?null:o.optJSONArray("lastDay");
        if(a!=null)for(int i=0;i<s.lastDay.length;i++)s.lastDay[i]=Math.max(0,a.optInt(i,0));
        s.shops=SolitudeShops.restore(o==null?null:o.optJSONObject("shops"));
        s.home=SolitudeHome.restore(o==null?null:o.optJSONObject("home"));
        return s;
    }
}
