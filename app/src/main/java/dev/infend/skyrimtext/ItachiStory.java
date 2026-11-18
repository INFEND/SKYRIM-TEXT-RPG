package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.Locale;

/** Optional crossover arc; all choices and combat effects are engine-owned. */
final class ItachiStory {
    static final String MODE="ITACHI", TITLE="Алая Ворона", NAME="Итачи";
    static final int FINAL=6, MAX_ENERGY=100;
    boolean enabled=true, following=false, romance=false;
    int stage=0, trust=0, respect=0, affection=0, energy=MAX_ENERGY, victories=0, lastTalk=-1, lastDate=-1;
    String ending="", memory="", lastPower="", lastDefense="";
    int level=1,xp=0,health=100,eyeStrain=0,stance=0,blade=15,illusion=15,spirit=15,gear=0,supplies=2;
    int burn=0,stun=0,shield=0,crows=0,clones=0,reflection=0,lastDaily=-1,lastGift=-1,love=0,attachment=0,hurt=0,dates=0;
    boolean engaged=false,married=false;
    ItachiLife life=new ItachiLife();
    org.json.JSONArray memories=new org.json.JSONArray();
    static final String[] ROUTE={"Ривервуд","Вайтран","Винтерхолд","Рифтен"};
    static final String[] DAILY={"Итачи тренировалась в лесу.","Итачи изучила записи о границе миров.","Итачи помогла путнику.","Итачи пополнила припасы."};
    static final String[] DATES={"Вы встретили рассвет. Итачи рассказала, почему ценит тишину.","За чаем она поделилась воспоминанием о родном доме.","На берегу она впервые заговорила о будущем.","Вы читали записи о созвездиях и обсуждали совместные планы."};
    int maxHealth(){return 100+5*(level-1);}
    int needXp(){return level*80;}
    void gain(int amount){xp=Math.min(1000000,xp+Math.max(0,amount));while(level<50&&xp>=needXp()){xp-=needXp();level++;health=Math.min(maxHealth(),health+5);}if(level==50)xp=Math.min(xp,needXp());}
    String stanceLabel(){return new String[]{"Сбалансированная","Защитная","Наступательная"}[stance];}
    String mood(){return hurt>=35?"Отстранена":eyeStrain>=60?"Утомлена":romance&&love>=70?"Счастлива":trust>=60?"Доверяет":"Сосредоточена";}
    String stats(){return "Уровень "+level+" | Опыт "+xp+"/"+needXp()+" | Здоровье "+health+"/"+maxHealth()+" | Энергия "+energy+"/100 | Нагрузка на зрение "+eyeStrain+"/100";}
    String skills(){return "Клинок "+blade+"/100 | Иллюзии "+illusion+"/100 | Духовные техники "+spirit+"/100 | "+stanceLabel();}
    void remember(String text){memory=text;while(memories.length()>=40)memories.remove(0);memories.put(text);}
    void daily(GameEngine g){if(g.aiNarrativeMode||!enabled||stage!=FINAL||lastDaily==g.day||g.road.pending()||g.solitude.custody())return;lastDaily=g.day;if(!following){recover(6);gain(10);}life.daily(g,this);}
    void recover(int hours){int h=Math.max(0,Math.min(24,hours));energy=clamp(energy+h*6);eyeStrain=clamp(eyeStrain-h*5);health=Math.min(maxHealth(),health+h*7);}
    void beginBattle(){burn=stun=shield=crows=clones=reflection=0;lastPower=lastDefense="";}
    void nextTarget(){burn=stun=reflection=0;}
    private static final String[] TITLES={"Чёрное перо","Глаза незнакомки","Отголоски иного мира","Цена силы","Наследие воронов","Выбор собственной судьбы"};
    private static final String[] BODY={
        "На дороге у Ривервуда ворон слетает с ветки. Под деревом стоит женщина в чёрном одеянии с красными облаками. Она замечает тебя раньше, чем ты успеваешь заговорить.\n\nИтачи:\n— Не бойся. Я не ищу здесь врагов.",
        "У костра Итачи на мгновение открывает алые глаза. Она объясняет, что видит движения и распознаёт обман, но не читает любую мысль по желанию.\n\nИтачи:\n— Сила не делает человека правым.",
        "Итачи рассказывает о мире, откуда пришла. В её воспоминаниях есть война, долг и решения, которые невозможно отменить. На твою карту она наносит знак неизвестной печати.\n\nИтачи:\n— Мне нужно узнать, почему я оказалась здесь.",
        "Во время упражнения её глаза тускнеют. Высшие техники требуют энергии и оставляют боль. Итачи прекращает тренировку по собственной воле.\n\nИтачи:\n— Я умею побеждать. Но не всегда умею останавливаться.",
        "В записях Коллегии обнаруживается описание нестабильной границы миров. Печать реагирует на духовную энергию Итачи; открывать её без подготовки опасно.\n\nИтачи:\n— Не стоит жертвовать этим миром ради возвращения в мой.",
        "Итачи закрывает печать, сохранив записи о её природе. Теперь она сама решает, оставаться ли странницей или принять новый дом.\n\nИтачи:\n— Я не хочу, чтобы прошлое выбирало за меня."
    };
    private static int clamp(int x){return Math.max(0,Math.min(100,x));}
    boolean active(){return enabled&&following&&stage==FINAL;}
    String location(GameEngine g){if(active())return g.progress.location;if(stage==4||stage==5)return "Винтерхолд";if(stage==FINAL&&life.sharedHome&&SolitudeHome.owns(g))return "Солитьюд";if(stage==FINAL&&!following)return ROUTE[Math.floorMod(g.day/3,ROUTE.length)];return "Ривервуд";}
    boolean present(GameEngine g){return enabled&&(active()||location(g).equalsIgnoreCase(g.progress.location)||g.progress.location.toLowerCase(Locale.ROOT).contains(location(g).toLowerCase(Locale.ROOT)));}
    boolean available(GameEngine g){return enabled&&g.started&&!g.road.pending()&&!g.solitude.custody()&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode))&&present(g);}
    boolean setEnabled(GameEngine g,boolean on){if("COMBAT".equals(g.mode))return false;enabled=on;if(!on){beginBattle();if(MODE.equals(g.mode))g.mode=life.homeReturn&&SolitudeHome.owns(g)?SolitudeHome.MODE:"WORLD";life.homeReturn=false;}return true;}
    String relation(){return married?"Супруга":engaged?"Невеста":romance?"Возлюбленная":active()?"Спутница":trust>=60?"Доверяет":trust>=25?"Знакомая":"Незнакомка";}
    String objective(GameEngine g){return TITLE+" | "+(stage<FINAL?TITLES[stage]+" | "+location(g):ending+" | "+relation());}
    String aiContext(GameEngine g){if(!enabled)return "Итачи: дополнение отключено, не вводить её в сцены.";if(stage==0)return g.aiNarrativeMode&&present(g)?"Итачи ещё не знакома с героем; допустима первая встреча в текущем месте. Подтверждение только через выбранный companionAction.":"Итачи ещё не знакома с героем. Не вводить её вне встречи движка.";return "Итачи: "+ItachiProfile.CHARACTER+" Этап "+stage+"/"+FINAL+", место="+location(g)+", рядом="+active()+", доверие="+trust+", роман="+romance+", энергия="+energy+". "+stats()+". "+skills()+". Настроение: "+mood()+". Последнее событие: "+memory+". Цель: "+life.goal+". Занятие: "+life.activity+". Отношения с отрядом: "+life.groupLabel()+". ИИ не меняет флаги и не придумывает применение способностей.";}
    GameEngine.Scene scene(GameEngine g){
        GameEngine.Scene s;
        if(stage<FINAL){
            s=new GameEngine.Scene(TITLE,TITLES[stage],BODY[stage]);
            if(stage==FINAL-1)s.primary("ITACHI:stay","Предложить ей остаться","Новый дом по её собственному выбору").add("ITACHI:free","Поддержать её свободу","Она останется независимой странницей");
            else {String[] help={"Представиться и выслушать","Попросить объяснить Шаринган","Изучить знак на карте","Предложить отдых","Исследовать печать вместе"};String[] quiet={"Не расспрашивать о прошлом","Не просить демонстрации","Дать ей время","Оставить решение о тренировке ей","Предложить осторожный подход"};s.primary("ITACHI:help:"+stage,help[stage],"Доверие и уважение").add("ITACHI:caution:"+stage,quiet[stage],"Уважать её границы");}
        }else{
            s=new GameEngine.Scene(TITLE,relation(),"Итачи спокойно смотрит на тебя.\n\nИтачи:\n— "+(romance?"Рядом с тобой я могу позволить себе тишину.":"У каждого из нас свой путь. Иногда они совпадают.")+"\n\nДоверие "+trust+"/100 | Уважение "+respect+"/100 | Симпатия "+affection+"/100\n"+stats()+"\n"+skills()+"\nНастроение: "+mood()+" | Любовь "+love+"/100 | Привязанность "+attachment+"/100\n"+memory);
            if(!following&&trust>=35)s.add("ITACHI:join","Пригласить в отряд","Иллюзии, огонь и защита");
            if(following)s.add("ITACHI:leave","Отпустить спутницу","Продолжит самостоятельное путешествие").add("ITACHI:stance","Изменить тактику",stanceLabel());
            if(lastTalk!=g.day)s.add("ITACHI:talk","Поговорить у костра","Доверие и симпатия").add("ITACHI:train","Тренироваться вместе","Уважение и восстановление энергии");
            if(!romance&&trust>=75&&respect>=65&&affection>=55)s.add("ITACHI:romance","Признаться в чувствах","Взаимное согласие");
            if(lastGift!=g.day&&g.gold>=25)s.add("ITACHI:gift","Подарить чай и свитки","25 септимов");
            if(gear<3&&g.gold>=100*(gear+1))s.add("ITACHI:gear","Улучшить снаряжение",(100*(gear+1))+" септимов");
            if(supplies<5&&g.gold>=30)s.add("ITACHI:supply","Передать лечебные припасы","30 септимов");
            if(romance){if(lastDate!=g.day)s.add("ITACHI:date","Провести вечер вдвоём","Уникальная сцена свидания");if(!engaged&&!married&&!life.otherMarriage(g)&&love>=75&&trust>=85&&attachment>=60&&dates>=3)s.add("ITACHI:engage","Предложить помолвку","Взаимное согласие");if(engaged&&!married&&!life.otherMarriage(g)&&love>=90&&g.day>life.engagementDay&&"Рифтен".equals(g.progress.location))s.add("ITACHI:wedding","Заключить союз в храме Мары","Взаимное согласие");s.add("ITACHI:break","Завершить отношения","Без принуждения");}
            if(hurt>0&&lastTalk!=g.day)s.add("ITACHI:apology","Обсудить обиду","Восстановление доверия");
            life.choices(s,g,this);
        }
        return s.add("ITACHI:exit","Продолжить путь","Вернуться к приключению");
    }
    void choose(GameEngine g,String action){
        if("ITACHI:open".equals(action)){if(available(g))g.mode=MODE;return;}
        if(!MODE.equals(g.mode)||!present(g)||g.road.pending()||g.solitude.custody())return;
        if("ITACHI:exit".equals(action)){g.mode=life.homeReturn&&SolitudeHome.owns(g)?SolitudeHome.MODE:"WORLD";life.homeReturn=false;return;}
        if(stage<FINAL-1){
            if(!("ITACHI:help:"+stage).equals(action)&&!("ITACHI:caution:"+stage).equals(action))return;
            boolean help=("ITACHI:help:"+stage).equals(action);trust=clamp(trust+(help?12:4));respect=clamp(respect+9);affection=clamp(affection+(help?7:2));attachment=clamp(attachment+(help?5:2));remember(TITLES[stage]+": "+(help?"Вы действовали вместе.":"Ты уважил её самостоятельность."));gain(20);stage++;
            if(!present(g)){g.mode="WORLD";g.message="Следующая встреча: "+location(g);}
            return;
        }
        if(stage==FINAL-1){
            if(!"ITACHI:stay".equals(action)&&!"ITACHI:free".equals(action))return;
            stage=FINAL;ending="ITACHI:stay".equals(action)?"Новый дом":"Свободная дорога";trust=clamp(trust+12);respect=clamp(respect+10);affection=clamp(affection+8);gain(60);remember("Вы закрыли межмировую печать. "+ending+".");g.addXp(100);if(!present(g))g.mode="WORLD";return;
        }
        if(life.choose(g,this,action))return;
        switch(action){
            case "ITACHI:join":if(!following&&trust>=35){following=true;remember("Итачи согласилась путешествовать с тобой.");}break;
            case "ITACHI:leave":if(following){following=false;g.mode=life.homeReturn&&SolitudeHome.owns(g)?SolitudeHome.MODE:"WORLD";life.homeReturn=false;remember("Итачи продолжила самостоятельное путешествие.");}break;
            case "ITACHI:talk":if(lastTalk!=g.day){lastTalk=g.day;trust=clamp(trust+6);respect=clamp(respect+4);affection=clamp(affection+7);attachment=clamp(attachment+4);remember("Итачи рассказала о том, что ценит свободу выбора.");}break;
            case "ITACHI:train":if(lastTalk!=g.day){lastTalk=g.day;respect=clamp(respect+7);trust=clamp(trust+4);recover(3);blade=clamp(blade+2);illusion=clamp(illusion+2);spirit=clamp(spirit+2);gain(25);remember("Вы разобрали тактику и отработали защиту.");}break;
            case "ITACHI:romance":if(!romance&&trust>=75&&respect>=65&&affection>=55){romance=true;love=Math.max(love,45);attachment=Math.max(attachment,35);remember("Итачи ответила взаимностью. Ваш союз основан на свободном выборе.");}break;
            case "ITACHI:date":if(romance&&lastDate!=g.day){lastDate=g.day;trust=clamp(trust+3);affection=clamp(affection+5);love=clamp(love+10);attachment=clamp(attachment+7);remember(DATES[Math.floorMod(dates,DATES.length)]);dates=Math.min(10000,dates+1);}break;
            case "ITACHI:stance":if(following)stance=(stance+1)%3;break;
            case "ITACHI:gift":if(lastGift!=g.day&&g.gold>=25){g.gold-=25;lastGift=g.day;affection=clamp(affection+5);attachment=clamp(attachment+3);if(romance)love=clamp(love+3);remember("Итачи приняла чай и свитки с благодарностью.");}break;
            case "ITACHI:gear":if(gear<3&&g.gold>=100*(gear+1)){g.gold-=100*(gear+1);gear++;remember("Снаряжение Итачи улучшено.");}break;
            case "ITACHI:supply":if(supplies<5&&g.gold>=30){g.gold-=30;supplies++;}break;
            case "ITACHI:engage":if(romance&&!engaged&&!married&&!life.otherMarriage(g)&&love>=75&&trust>=85&&attachment>=60&&dates>=3){engaged=true;life.engagementDay=g.day;remember("Вы обручились по взаимному согласию.");}break;
            case "ITACHI:wedding":if(romance&&engaged&&!married&&!life.otherMarriage(g)&&love>=90&&g.day>life.engagementDay&&"Рифтен".equals(g.progress.location)){married=true;engaged=false;remember("Вы заключили добровольный союз в храме Мары.");}break;
            case "ITACHI:apology":if(hurt>0&&lastTalk!=g.day){lastTalk=g.day;hurt=clamp(hurt-15);trust=clamp(trust+2);remember("Вы спокойно обсудили разногласия.");}break;
            case "ITACHI:break":if(romance){romance=engaged=married=false;life.endRomance();love=clamp(love-25);hurt=clamp(hurt+20);remember("Вы завершили отношения без принуждения.");}break;
        }
    }
    void rest(int hours){if(active())recover(hours);}
    int ongoingDamage(int remaining){if(!active()||health<=0||remaining<=0)return 0;int n=0;if(burn>0){burn--;n+=5+spirit/25;}if(clones>0){clones--;n+=3+blade/35;}if(reflection>0){n+=reflection;reflection=0;}return Math.min(remaining,n);}
    int assist(int turn,int remaining){return assist(turn,remaining,false);}
    int assist(int turn,int remaining,boolean sealAllowed){lastPower="";if(!active()||remaining<=0||turn%2!=0)return 0;
        if(supplies>0&&health<=maxHealth()/2){supplies--;health=Math.min(maxHealth(),health+35);}
        if(health<=0)return 0;
        int cost=0,damage=7+Math.min(8,victories/4)+gear*2+life.bonus(true)+(stance==2?3:0);lastPower="Кунаи";
        if(energy>=35&&eyeStrain<70&&turn%8==0){cost=35;eyeStrain=clamp(eyeStrain+20);shield=3;damage+=17;lastPower="Сусаноо / Клинок Тоцука";spirit=clamp(spirit+1);if(sealAllowed&&remaining<=15+level/2){damage=remaining;lastPower="Клинок Тоцука: запечатывание";}}
        else if(energy>=24&&eyeStrain<80&&turn%6==0){cost=24;eyeStrain=clamp(eyeStrain+12);burn=3;damage+=12;lastPower="Аматэрасу";spirit=clamp(spirit+1);}
        else if(energy>=12&&eyeStrain<85&&turn%4==0){cost=12;eyeStrain=clamp(eyeStrain+6);stun=1;damage+=5;lastPower="Цукуёми";illusion=clamp(illusion+1);}
        else if(energy>=8&&turn%10==0){cost=8;clones=3;lastPower="Теневые клоны";}
        else if(energy>=6&&turn%10==2){cost=6;crows=2;lastPower="Вороны";}
        else if(energy>=4&&eyeStrain<95){cost=4;lastPower="Шаринган / точный удар";blade=clamp(blade+1);}
        energy-=cost;return Math.min(remaining,damage);
    }
    int protect(int incoming){return protect(incoming,true);}
    int protect(int incoming,boolean allowStun){lastDefense="";if(!active()||health<=0||incoming<=0)return 0;int n=0;
        if(stun>0){stun--;n=allowStun?incoming:Math.min(incoming/2,8);lastDefense="Цукуёми";}
        else if(shield>0){shield--;n=Math.min(incoming,8+gear+(stance==1?4:0));reflection=Math.min(6,n/2);lastDefense="Сусаноо / Зеркало Ята";}
        else if(crows>0){crows--;n=Math.min(incoming,4+illusion/25);lastDefense="Вороны: отвлечение";}
        else if(energy>=5&&incoming>1){energy-=5;n=Math.min(incoming-1,4+Math.min(5,victories/5)+(stance==1?3:0));lastDefense="Шаринган: предугадывание";}
        if(n>0){n=Math.min(incoming,n+life.bonus(false));health=Math.max(0,health-Math.max(1,n/5));}return n;
    }
    void victory(){if(active()){victories=Math.min(1000,victories+1);gain(35);beginBattle();}}
    JSONObject save() throws JSONException{return new JSONObject().put("enabled",enabled).put("following",following).put("romance",romance).put("stage",stage).put("trust",trust).put("respect",respect).put("affection",affection).put("energy",energy).put("victories",victories).put("lastTalk",lastTalk).put("lastDate",lastDate).put("ending",ending).put("memory",memory).put("level",level).put("xp",xp).put("health",health).put("eyeStrain",eyeStrain).put("stance",stance).put("blade",blade).put("illusion",illusion).put("spirit",spirit).put("gear",gear).put("supplies",supplies).put("burn",burn).put("stun",stun).put("shield",shield).put("crows",crows).put("clones",clones).put("reflection",reflection).put("lastDaily",lastDaily).put("lastGift",lastGift).put("love",love).put("attachment",attachment).put("hurt",hurt).put("dates",dates).put("engaged",engaged).put("married",married).put("memories",memories).put("life",life.save());}
    static ItachiStory restore(JSONObject j){ItachiStory s=new ItachiStory();if(j==null)return s;s.enabled=j.optBoolean("enabled",true);s.stage=Math.max(0,Math.min(FINAL,j.optInt("stage")));s.trust=clamp(j.optInt("trust"));s.respect=clamp(j.optInt("respect"));s.affection=clamp(j.optInt("affection"));s.energy=clamp(j.optInt("energy",100));s.victories=Math.max(0,Math.min(1000,j.optInt("victories")));s.lastTalk=j.optInt("lastTalk",-1);s.lastDate=j.optInt("lastDate",-1);s.ending=j.optString("ending");s.memory=j.optString("memory");s.following=s.stage==FINAL&&j.optBoolean("following");s.romance=s.stage==FINAL&&j.optBoolean("romance");s.level=Math.max(1,Math.min(50,j.optInt("level",1)));s.xp=Math.max(0,Math.min(1000000,j.optInt("xp",0)));s.health=Math.max(0,Math.min(345,j.optInt("health",100)));s.eyeStrain=Math.max(0,Math.min(100,j.optInt("eyeStrain",0)));s.stance=Math.max(0,Math.min(2,j.optInt("stance",0)));s.blade=Math.max(0,Math.min(100,j.optInt("blade",15)));s.illusion=Math.max(0,Math.min(100,j.optInt("illusion",15)));s.spirit=Math.max(0,Math.min(100,j.optInt("spirit",15)));s.gear=Math.max(0,Math.min(3,j.optInt("gear",0)));s.supplies=Math.max(0,Math.min(5,j.optInt("supplies",2)));s.burn=Math.max(0,Math.min(3,j.optInt("burn",0)));s.stun=Math.max(0,Math.min(1,j.optInt("stun",0)));s.shield=Math.max(0,Math.min(3,j.optInt("shield",0)));s.crows=Math.max(0,Math.min(2,j.optInt("crows",0)));s.clones=Math.max(0,Math.min(3,j.optInt("clones",0)));s.reflection=Math.max(0,Math.min(6,j.optInt("reflection",0)));s.lastDaily=Math.max(-1,Math.min(2147483647,j.optInt("lastDaily",-1)));s.lastGift=Math.max(-1,Math.min(2147483647,j.optInt("lastGift",-1)));s.love=Math.max(0,Math.min(100,j.optInt("love",0)));s.attachment=Math.max(0,Math.min(100,j.optInt("attachment",0)));s.hurt=Math.max(0,Math.min(100,j.optInt("hurt",0)));s.dates=Math.max(0,Math.min(10000,j.optInt("dates",0)));s.xp=Math.min(s.xp,s.needXp());s.health=Math.min(s.health,s.maxHealth());if(s.romance&&!j.has("love"))s.love=45;if(s.romance&&!j.has("attachment"))s.attachment=35;s.married=s.romance&&j.optBoolean("married");s.engaged=s.romance&&!s.married&&j.optBoolean("engaged");org.json.JSONArray history=j.optJSONArray("memories");if(history!=null)for(int i=Math.max(0,history.length()-40);i<history.length();i++)s.memories.put(history.optString(i));s.life=ItachiLife.restore(j.optJSONObject("life"),s);if(!s.active())s.beginBattle();return s;}
}
