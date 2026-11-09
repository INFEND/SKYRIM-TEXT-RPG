package dev.infend.skyrimtext;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Offline, saveable, original fan-fiction campaign set in a Skyrim-inspired world. */
public final class GameEngine {
    public static final String[] REGIONS = {"Вайтран", "Рифтен", "Виндхельм", "Солитьюд", "Маркарт", "Фолкрит", "Винтерхолд", "Данстар", "Морфал", "Солстейм"};
    public static final String[] RACES = RpgContent.RACES;
    public static final String[] CLASSES = RpgContent.CLASSES;
    private static final String[] DESCRIPTIONS = {
        "Ветер гуляет над золотой равниной. Стража несёт дозор у городских ворот, а с рынка доносятся голоса торговцев.",
        "Мокрая брусчатка отражает огни трактиров. В переулках Рифтена слишком часто оборачиваются на чужие шаги.",
        "Холодный ветер несёт снег с моря. У серых стен Виндхельма слышны тяжёлые удары кузнечных молотов.",
        "Над гаваньью Солитьюда поднимаются башни. При дворе говорят о древних печатях, но редко вслух.",
        "Каменные арки Маркарта скрывают длинные тени. В горах будто продолжает работать забытый механизм.",
        "Тёмные сосны Фолкрита шепчутся над древними захоронениями. Птицы избегают заброшенного алтаря.",
        "Под каменными мостами Винтерхолда ревёт ледяное море. Сквозь метель мерцает свет старой башни.",
        "Корабли вмерзли в залив Данстара. По ночам рыбаки замечают странное сияние на горизонте.",
        "Морфал хранит в болотах туман и слишком долгую память. В сумерках огоньки мерцают там, куда люди не ходят.",
        "На Солстейме чёрный пепел ложится на скалы, а море блестит под суровым северным светом."
    };

    public static final class Choice {
        public final String action, title, detail;
        public final boolean special;
        public Choice(String action, String title, String detail) { this(action, title, detail, false); }
        public Choice(String action, String title, String detail, boolean special) {
            this.action = action; this.title = title; this.detail = detail; this.special = special;
        }
    }
    public static final class Scene {
        public final String kicker, title, body;
        public final List<Choice> choices = new ArrayList<>();
        Scene(String kicker, String title, String body) { this.kicker = kicker; this.title = title; this.body = body; }
        Scene add(String action, String title, String detail) { choices.add(new Choice(action,title,detail)); return this; }
        Scene primary(String action, String title, String detail) { choices.add(new Choice(action,title,detail,true)); return this; }
    }

    public boolean started;
    public String name = "Странник", race = "Норд", heroClass = "Воин";
    public int region = 0, day = 1, hour = 8, level = 1, xp = 0, maxHp = 100, hp = 100;
    public int maxStamina = 65, stamina = 65, maxMagicka = 40, magicka = 40;
    public int gold = 85, potions = 3, manaPotions = 1, attack = 12, armor = 2;
    public int questStage = 0, sideQuest = 0, sideKills = 0, boardCompleted = 0, explored = 0, victories = 0, reputation = 0;
    public boolean swordUpgraded = false, armorUpgraded = false;
    public String mode = "WORLD", message = "Новый день открывает новые дороги.";
    public String enemyName = "", battleLog = "", battleKind = "", enemyIntro = "";
    public int enemyHp = 0, enemyMaxHp = 0, enemyDamage = 0;
    public RpgProgress progress = new RpgProgress();
    public AiGameTurn aiTurn;
    public SceneState sceneState = new SceneState();
    public String aiJournal = "";
    private final Random random = new Random();

    public void start(String desiredName, String desiredRace, String desiredClass) {
        start(desiredName,desiredRace,desiredClass,0,new int[]{0,0,0});
    }
    public void start(String desiredName,String desiredRace,String desiredClass,int origin,int[] points){
        name=desiredName==null||desiredName.trim().isEmpty()?RpgContent.randomName():desiredName.trim();
        if(name.length()>28)name=name.substring(0,28);
        race=contains(RACES,desiredRace)?desiredRace:"Норд";
        heroClass=contains(CLASSES,desiredClass)?desiredClass:"Без класса";
        started=true;aiTurn=null;sceneState=new SceneState();aiJournal="";day=1;hour=8;level=1;xp=0;gold=85;potions=3;manaPotions=1;
        questStage=0;sideQuest=0;sideKills=0;boardCompleted=0;explored=0;victories=0;reputation=0;
        swordUpgraded=false;armorUpgraded=false;
        maxHp=100;maxStamina=100;maxMagicka=100;attack=12;armor=2;
        switch(heroClass){
            case "Воин":maxHp+=15;maxStamina+=10;attack+=4;break;
            case "Маг":maxMagicka+=25;attack+=1;break;
            case "Вор":maxStamina+=15;gold+=30;attack+=2;break;
            case "Лучник":maxStamina+=10;attack+=3;break;
            case "Паладин":maxHp+=15;maxMagicka+=10;armor+=2;break;
            case "Ассасин":attack+=3;maxStamina+=15;break;
            case "Боевой маг":maxMagicka+=20;attack+=2;break;
            case "Алхимик":potions+=2;manaPotions+=2;break;
            case "Следопыт":maxStamina+=20;armor+=1;break;
        }
        int ri=RpgContent.raceIndex(race);
        if(ri==7)maxMagicka+=50;
        if(ri==4)gold+=30;
        if(points!=null&&points.length>=3){
            int total=0;for(int p:points)total+=Math.max(0,p);
            if(total==5){
                maxHp+=Math.max(0,points[0])*10;
                maxMagicka+=Math.max(0,points[1])*10;
                maxStamina+=Math.max(0,points[2])*10;
            }
        }
        hp=maxHp;magicka=maxMagicka;stamina=maxStamina;
        progress=RpgProgress.newHero(this,origin,points);
        String[] back=RpgContent.startParts(origin);
        region=Integer.parseInt(back[2]);
        mode="INTRO";message="";
    }

    public Scene scene() {
        Scene s=baseScene();
        if(started)progress.fillChoices(this,s);
        // Every offline scene follows the same long-form literary presentation.
        // SceneNarrative is text-only: action IDs, story flags and saves are unchanged.
        return SceneNarrative.expand(this,s);
    }
    private Scene baseScene(){
        if (!started) return new Scene("НОВАЯ ИСТОРИЯ", "Твоя сага начинается", "Создай героя, чтобы отправиться в суровые северные земли. Каждое решение меняет путь, а прогресс сохраняется автоматически.");
        switch(mode) {
            case "INTRO": return new Scene("ПРОЛОГ • ДЕНЬ "+day,progress.startId,
                progress.recent+"\n\n"+DESCRIPTIONS[Math.max(0,Math.min(region,DESCRIPTIONS.length-1))]+
                "\n\nСегодня дорога не обещает лёгких ответов. На ветру пахнет костром, снегом и переменами. Любой случайный попутчик может оказаться союзником, а любое решение оставить след.")
                .primary("GO_WORLD","Сделать первый шаг","Начать историю и выйти на дорогу");
            case "TRAVEL": return travelScene();
            case "QUEST": return questScene();
            case "DRAGON": return DragonArc.scene(this);
            case "TALK": return talkScene();
            case "SHOP": return shopScene();
            case "EVENT": return eventScene();
            case "COMBAT": return battleScene();
            case "RESULT": return new Scene("ПОСЛЕДСТВИЯ", "Событие завершено", message).primary("GO_WORLD", "Продолжить путь", "Вернуться к исследованиям");
            case "DEFEAT": return new Scene("ПОРАЖЕНИЕ", "Тьма отступает", message).primary("GO_WORLD", "Подняться на ноги", "Приключение продолжается");
            default: return worldScene();
        }
    }

    private Scene worldScene() {
        Scene s = new Scene("СКАЙРИМ • ДЕНЬ " + day, progress.location + " · " + REGIONS[region], DESCRIPTIONS[region] + "\n\nМестность: "+progress.location+"." +
            (message.isEmpty()?"":"\n\n"+message));
        s.primary("DRAGON_OPEN","Главная история • Драконий след",DragonArc.status(this));
        s.add("EXPLORE","Исследовать окрестности","Случайные встречи, тайники и опасности");
        s.add("QUEST","История хранителей","Пепельные печати • " + questStatus());
        s.add("TALK","Поговорить с местными","Слухи, события и полезные сведения");
        s.add("TRAVEL","Отправиться в путь","Выбрать другой город");
        s.add("SHOP","Торговая лавка","Зелья, оружие и снаряжение");
        s.add("BOARD","Доска заказов","Дополнительное поручение");
        s.add("REST","Отдохнуть в трактире","20 септимов • восстановить силы");
        return s;
    }

    private Scene travelScene() {
        Scene s = new Scene("ДОРОГИ СЕВЕРА", "Куда держать путь?", "Поездки занимают несколько дней и требуют перехода между дорогами владений. На Солстейм ходит корабль из Виндхельма.");
        for(int i=0;i<REGIONS.length;i++)if(i!=region){
            WorldAtlas.Place target=WorldAtlas.find(TravelPlanner.HUBS[i]);
            TravelPlanner.Route route=TravelPlanner.plan(this,target);
            s.add("REGION:"+i,REGIONS[i],regionHint(i)+(route.allowed?
                " • "+route.days+" дн."+(route.fare>0?" • "+route.fare+" септимов":""):" • "+route.problem));
        }
        return s.add("GO_WORLD","Остаться здесь","Вернуться в город");
    }
    private String regionHint(int i) {
        if(i==5 && questStage==1) return "Цель задания: древний алтарь";
        if(i==6 && questStage==2) return "Цель задания: ледяная башня";
        if(i==3 && questStage==3) return "Цель задания: завершить историю";
        return (i==0?"Центральные равнины":i==1?"Южные леса":i==2?"Восточные снега":i==3?"Северо-западное побережье":i==4?"Каменные ущелья":i==5?"Тёмный лес":i==6?"Ледяное побережье":i==7?"Северная гавань":i==8?"Туманные болота":"Пепельный остров");
    }

    private Scene questScene() {
        switch(questStage) {
            case 0: return new Scene("ГЛАВА I", "Пепельные печати", "Пожилой хранитель Эйрн сообщает: из трёх святилищ исчезли печати, которые удерживали нечто под снегами. Он просит тебя найти след реликвии в лесах Фолкрита.\n\n«Никаких легенд не осталось, лишь чёрный пепел на камнях». ")
                .primary("Q_ACCEPT","Принять поручение","+40 септимов на дорогу").add("GO_WORLD","Пока отказаться","Вернуться позже");
            case 1: return new Scene("ГЛАВА I", "Алтарь среди сосен", "След ведёт к забытому алтарю в Фолкрите. По словам охотников, там поселилась необычная тишина.\n\nТекущая цель: добраться до Фолкрита и изучить камни.")
                .primary(region==5?"Q_ALTAR":"TRAVEL",region==5?"Осмотреть алтарь":"Открыть карту","Найти высеченный знак").add("GO_WORLD","Вернуться","Задание останется активным");
            case 2: return new Scene("ГЛАВА II", "Ледяная башня", "Под плитой алтаря ты находишь серебряную пластину с направлением на Винтерхолд. Там спрятан журнал последнего хранителя. Вход охраняет пепельный страж.\n\nТекущая цель: добраться до Винтерхолда и забрать журнал.")
                .primary(region==6?"Q_BOSS":"TRAVEL",region==6?"Войти в башню":"Открыть карту",region==6?"Сразиться со стражем":"Путь к Винтерхолду").add("GO_WORLD","Вернуться","Подготовиться к битве");
            case 3: return new Scene("ГЛАВА III", "Последнее свидетельство", "Журнал рассказывает о подземном разломе и старой клятве. Для завершения обряда копию записей необходимо передать хранителям Солитьюда.\n\nТекущая цель: отправиться в Солитьюд.")
                .primary(region==3?"Q_FINISH":"TRAVEL",region==3?"Передать журнал":"Открыть карту","Завершить сюжетную линию").add("GO_WORLD","Вернуться","Сначала подготовиться");
            default: return new Scene("ЭПИЛОГ", "Печати сохранены", "Слово о твоём путешествии разошлось по северным трактам. Основная история завершена, но на дорогах осталось немало приключений.")
                .primary("GO_WORLD","Продолжить путешествие","Свободное исследование мира");
        }
    }
    private Scene talkScene() {
        String[][] locals = {
            {"Седой караульный", "Стражник замечает твою усталость: «За стенами опаснее, чем кажется. Держи клинок под рукой»"},
            {"Торговка пряностями", "«В некоторых лесах птицы внезапно замолкли. Не к добру», говорит она, перебирая сушёные травы"},
            {"Усталый путешественник", "«Снег заметает старые тропы, но камни хранят память дольше людей»"}
        };
        String[] l=locals[(region+day)%locals.length];
        return new Scene("РАЗГОВОР",l[0],l[1]+".\n\nЧто ответишь?")
            .primary("TALK_HELP","Расспросить о слухах","Получить подсказку")
            .add("TALK_GIVE","Помочь септимовой","Пожертвовать 10 септимов")
            .add("GO_WORLD","Попрощаться","Вернуться на улицу");
    }
    private Scene shopScene() {
        return new Scene("ТОРГОВЛЯ • " + REGIONS[region], "Северная лавка", "На прилавке лежат походные припасы и изделия кузнеца.\n\nТвой кошель: " + gold + " септимов.")
            .primary("BUY_HP","Лечебное зелье • 35", "Восстанавливает 45 здоровья")
            .add("BUY_MP","Зелье магии • 30", "Восстанавливает 35 магии")
            .add("BUY_SWORD",swordUpgraded?"Клинок приобретён":"Закалённый клинок • 180", swordUpgraded?"Уже в инвентаре":"Постоянно +6 к атаке")
            .add("BUY_ARMOR",armorUpgraded?"Доспех приобретён":"Стальной нагрудник • 160",armorUpgraded?"Уже в инвентаре":"Постоянно +4 к защите")
            .add("GO_WORLD","Покинуть лавку","Вернуться на улицу");
    }
    private Scene eventScene() {
        return new Scene("СЛУЧАЙНАЯ ВСТРЕЧА", "Выбор за тобой", message)
            .primary("EVENT_HELP","Оказать помощь","Проявить благородство")
            .add("EVENT_SEARCH","Осмотреть местность","Проверить, что скрыто неподалёку")
            .add("EVENT_LEAVE","Пройти мимо","Не рисковать");
    }
    private Scene battleScene() {
        return new Scene("БОЙ • " + REGIONS[region], enemyName, enemyIntro + "\n\n" + battleLog +
            "\n\nПротивник: " + enemyHp + "/" + enemyMaxHp + " здоровья.\nТы: " + hp + "/" + maxHp + " здоровья.")
            .primary("ATTACK","Обычный удар","Стабильный урон, без затрат")
            .add("HEAVY","Сильный удар","−10 выносливости • риск промаха")
            .add("MAGIC","Магическая вспышка","−12 магии • усиленный урон")
            .add("DEFEND","Защититься","Уменьшить ответный урон")
            .add("POTION","Выпить лечебное зелье","Осталось: " + potions)
            .add("FLEE","Отступить","Шанс выйти из боя без награды");
    }

    public void choose(String action) {
        if (!started || action==null) return;
        // A pending battle cannot be escaped by invoking a world, map or quest action.
        // This guard also protects saved games restored directly into COMBAT mode.
        if ("COMBAT".equals(mode) && !("ATTACK".equals(action) || "HEAVY".equals(action)
                || "MAGIC".equals(action) || "DEFEND".equals(action)
                || "POTION".equals(action) || "FLEE".equals(action))) return;
        if("DRAGON_OPEN".equals(action)){
            mode="DRAGON";return;
        }
        if(action.startsWith("DRAGON_NEXT:")){
            DragonContinuation.perform(this,action);return;
        }
        if(action.startsWith("DRAGON:")){
            boolean dragonFight=DragonArc.perform(this,action);
            if(dragonFight){
                int prepared=progress.dragonInsight;
                startBattle("Мирмулнир",Math.max(95,170+level*8-prepared*7),
                    Math.max(9,17+level*2-prepared/2),"DRAGON",
                    "Дракон резко обрывает круг над дозорной башней. Стража пытается удержать строй. "+
                    "Укрытия из камня ещё стоят, а замеченные ранее слабости крылатого врага дают шанс выстоять.");
            }
            progress.sync(this);return;
        }
        if (action.startsWith("ACT:")){progress.perform(this,action);progress.sync(this);return;}
        if(action.startsWith("LOC:")){
            WorldAtlas.Place target=WorldAtlas.find(action.substring(4));
            if(target==null)return;
            TravelPlanner.Route route=TravelPlanner.plan(this,target);
            if(!route.allowed){message=route.problem;mode="RESULT";return;}
            if(route.days==0){message="Ты уже находишься здесь.";mode="WORLD";return;}
            gold-=route.fare;
            day+=route.days;
            region=target.region;
            progress.location=target.name;
            // Register only locations reached through the validated atlas route.
            // A stable key keeps repeat visits idempotent across save/load.
            try {
                sceneState.confirm("sublocation", "atlas_" + Integer.toHexString(target.name.hashCode()),
                        target.name, "VISITED");
            } catch (JSONException ignored) {
                // Optional scene metadata cannot invalidate a completed journey.
            }
            stamina=Math.max(1,stamina-Math.min(Math.max(0,stamina-1),route.days*6));
            magicka=Math.max(1,magicka-Math.min(Math.max(0,magicka-1),route.days*2));
            message="Ты преодолеваешь дорогу, постепенно оставляя за спиной стоянки, перевалы и чужие костры. "+
                route.summary()+"\n\nМесто назначения: "+target.name+". "+target.description;
            mode="RESULT";return;
        }
        if(action.startsWith("REGION:")){
            try{
                int target=Integer.parseInt(action.substring(7));
                if(target>=0&&target<REGIONS.length)choose("LOC:"+TravelPlanner.HUBS[target]);
            }catch(NumberFormatException ignored){}
            return;
        }
        switch(action) {
            case "GO_WORLD": mode="WORLD"; if(message.isEmpty()) message="Ветер зовёт обратно на дорогу."; break;
            case "TRAVEL": mode="TRAVEL"; break;
            case "SHOP": mode="SHOP"; break;
            case "QUEST": mode="QUEST"; break;
            case "TALK": mode="TALK"; break;
            case "EXPLORE": explore(); break;
            case "BOARD": board(); break;
            case "REST":
                if(gold<20) {message="Хозяин трактира просит 20 септимов. Придётся поискать заработок.";mode="RESULT";}
                else {gold-=20;day++;hp=maxHp;magicka=maxMagicka;stamina=maxStamina;message="После тёплого ужина и долгого сна силы восстановились.";mode="RESULT";} break;
            case "Q_ACCEPT": if(questStage==0) {questStage=1;gold+=40;message="Ты принимаешь 40 септимов и получаешь первую зацепку: алтарь находится в Фолкрите.";mode="RESULT";} break;
            case "Q_ALTAR": if(questStage==1 && region==5) {questStage=2;addXp(35);message="Ты находишь под алтарём серебряную пластину. На ней указан Винтерхолд. +35 опыта.";mode="RESULT";} break;
            case "Q_BOSS": if(questStage==2 && region==6) startBattle("Пепельный страж",90+level*11,12+level*2,"QUEST","Внутри башни оживает существо из пепла и холодного камня."); break;
            case "Q_FINISH": if(questStage==3 && region==3) {questStage=4;gold+=300;reputation+=5;addXp(180);message="Хранители принимают журнал и восстанавливают печать. Скайрим получает ещё один мирный рассвет. Награда: 300 септимов и 180 опыта.";mode="RESULT";} break;
            case "TALK_HELP": message=questStage==1?"Слух подтверждается: ищи старые плиты в Фолкрите.":questStage==2?"О башне в Винтерхолде говорят вполголоса. Возьми зелья.":"Поговаривают, что на дороге появилось больше разбойников.";mode="RESULT";break;
            case "TALK_GIVE": if(gold>=10){gold-=10;reputation++;addXp(8);message="Собеседник благодарит тебя. Репутация +1, опыт +8.";}else message="В кошельке нет десяти септимов.";mode="RESULT";break;
            case "BUY_HP": buy(35,"Лечебное зелье куплено.","HP");break;
            case "BUY_MP": buy(30,"Зелье магии куплено.","MP");break;
            case "BUY_SWORD": buy(180,"Клинок закалён. Атака +6.","SWORD");break;
            case "BUY_ARMOR": buy(160,"Ты облачаешься в стальной нагрудник. Защита +4.","ARMOR");break;
            case "SIDE_ACCEPT": if(sideQuest==2) {sideQuest=1;sideKills=0;message="Заказ принят. Победи трёх противников во время исследования мира.";mode="RESULT";} break;
            case "SIDE_CLAIM": if(sideQuest==1 && sideKills>=3) {sideQuest=0;sideKills=0;boardCompleted++;gold+=120;addXp(65);message="Заказ выполнен. +120 септимов и +65 опыта.";mode="RESULT";}break;
            case "EVENT_HELP": eventChoice(true);break;
            case "EVENT_SEARCH": eventChoice(false);break;
            case "EVENT_LEAVE": mode="WORLD";message="Ты оставляешь встречу позади и возвращаешься на дорогу.";break;
            case "ATTACK": combatTurn("ATTACK");break;
            case "HEAVY": combatTurn("HEAVY");break;
            case "MAGIC": combatTurn("MAGIC");break;
            case "DEFEND": combatTurn("DEFEND");break;
            case "POTION": combatTurn("POTION");break;
            case "FLEE": combatTurn("FLEE");break;
        }
    }
    private void buy(int price,String success,String item) {
        if((item.equals("SWORD")&&swordUpgraded)||(item.equals("ARMOR")&&armorUpgraded)) {
            message="Этот предмет у тебя уже есть.";mode="RESULT";return;
        }
        if(gold<price){message="Недостаточно септимов. Нужно ещё "+(price-gold)+" септимов.";mode="RESULT";return;}
        progress.sync(this);
        String target=item.equals("HP")?"Лечебное зелье":item.equals("MP")?"Зелье магии":
            item.equals("SWORD")?"Стальной меч":"Стальная кираса";
        String replaced=item.equals("SWORD")?"Походный клинок":item.equals("ARMOR")?"Дорожная одежда":null;
        ItemCatalog.Item incoming=ItemCatalog.find(target);
        ItemCatalog.Item previous=replaced==null?null:ItemCatalog.find(replaced);
        if(incoming==null){message="Предмет не найден в каталоге. Покупка отменена.";mode="RESULT";return;}
        double projected=progress.carryWeight()+incoming.weight;
        if(previous!=null&&progress.quantity(replaced)>0)projected-=previous.weight;
        if(projected>50.00001){
            message="Нельзя взять предмет: вес превысит 50 кг. Септимы не списаны.";
            mode="RESULT";return;
        }
        gold-=price;
        switch(item){
            case "HP":potions++;break;
            case "MP":manaPotions++;break;
            case "SWORD":swordUpgraded=true;attack+=6;break;
            case "ARMOR":armorUpgraded=true;armor+=4;break;
        }
        progress.sync(this);message=success;mode="RESULT";
    }
    private void explore() {
        explored++;stamina=Math.max(0,stamina-5);
        int roll=random.nextInt(100);
        if(roll<49){
            String[] enemies={"Лесной разбойник","Снежный волк","Изгой","Пещерный охотник","Наёмник с дороги"};
            String foe=enemies[(region+random.nextInt(enemies.length))%enemies.length];
            startBattle(foe,40+level*13+random.nextInt(20),6+level*3,"WILD","На тропе появляется враг. Отступать уже поздно.");
        } else if(roll<79) {
            String[] events={
                "У ручья сидит раненный путник. Его заплечный мешок лежит в нескольких шагах.",
                "Ты слышишь шум за поваленным деревом. Между корнями что-то блестит.",
                "На пустой дороге стоит телега со сломанным колесом. Хозяина нигде не видно.",
                "В снегу виднеются свежие следы и брошенный дорожный сундук."
            };
            message=events[random.nextInt(events.length)];mode="EVENT";
        } else {
            int found=15+random.nextInt(41);gold+=found;addXp(12);
            boolean extraPotion=random.nextInt(4)==0;
            boolean canTake=progress.carryWeight()+0.5<=50.00001;
            if(extraPotion&&canTake)potions++;
            message="В зарослях найден старый тайник. Внутри "+found+" септимов. +12 опыта."+
                (extraPotion?(canTake?" Найдено лечебное зелье.":" Зелье пришлось оставить из-за веса."):"");
            mode="RESULT";
        }
    }
    private void eventChoice(boolean help) {
        // The exact percentage shown in the scene is the same value used in this roll.
        String action=help?"EVENT_HELP":"EVENT_SEARCH";
        int successChance=progress.chance(this,action);
        boolean succeeded=random.nextInt(100)<successChance;
        if(succeeded){
            if(help){
                reputation++;int gain=10+random.nextInt(16);addXp(gain);
                if(random.nextBoolean()){
                    boolean canCarry=progress.carryWeight()+0.5<=50.00001;
                    if(canCarry)potions++;
                    message=canCarry?"Ты помогаешь незнакомцу и получаешь лечебное зелье. Репутация +1, опыт +"+gain+".":
                        "Ты помогаешь путнику, но оставляешь зелье из-за тяжёлой поклажи. Репутация +1, опыт +"+gain+".";
                }
                else{gold+=25;message="В благодарность тебе вручают 25 септимов. Репутация +1, опыт +"+gain+".";}
            }else{
                int found=28+random.nextInt(44);gold+=found;addXp(10);
                message="Под камнем найдено "+found+" септимов. Получено 10 опыта.";
            }
            progress.completed.add((help?"Помощь путнику":"Исследование находки")+" • день "+day);
        }else{
            if(help){reputation=Math.max(-50,reputation-1);message="Незнакомец принимает жест за угрозу и уходит. Репутация −1.";}
            else{hp=Math.max(1,hp-17);message="Сработала старая ловушка! −17 здоровья.";}
            progress.failed.add((help?"Помощь путнику":"Исследование находки")+" • день "+day);
        }
        mode="RESULT";
    }
    private void board() {
        if(sideQuest==0){mode="RESULT";sideQuest=2;message="На доске висит объявление: «Избавьте дороги от трёх опасных противников. Плата 120 септимов». Открой доску ещё раз, чтобы принять заказ.";}
        else if(sideQuest==2){mode="RESULT";sideQuest=1;sideKills=0;message="Заказ принят. Победи трёх врагов в случайных встречах. Текущий прогресс: 0/3.";}
        else if(sideKills>=3){sideQuest=0;sideKills=0;gold+=120;addXp(65);message="Заказ выполнен. +120 септимов, +65 опыта.";mode="RESULT";}
        else {message="Заказ активен. Побеждено противников: "+sideKills+"/3.";mode="RESULT";}
    }
    private void startBattle(String name,int health,int dmg,String kind,String intro) {
        enemyName=name;enemyHp=health;enemyMaxHp=health;enemyDamage=dmg;battleKind=kind;enemyIntro=intro;
        battleLog="В бою важна тактика. Следи за запасом сил и магии.";mode="COMBAT";
    }
    private void combatTurn(String action) {
        if(!mode.equals("COMBAT"))return;
        int damage=0;boolean defending=false;String actionText="";
        switch(action){
            case "ATTACK": damage=Math.max(1,attack+random.nextInt(9)-3);actionText="Ты наносишь удар: −"+damage+" здоровья врагу.";break;
            case "HEAVY":if(stamina<10){battleLog="Недостаточно выносливости для сильного удара.";return;}
                int hitChance=progress.chance(this,"HEAVY");stamina-=10;
                if(random.nextInt(100)<hitChance){damage=Math.max(1,attack*2+random.nextInt(9));actionText="Сильный удар! −"+damage+" здоровья врагу.";}else actionText="Сильный удар проходит мимо.";break;
            case "MAGIC":if(magicka<12){battleLog="Недостаточно магии. Нужны 12 единиц.";return;}magicka-=12;damage=18+level*4+random.nextInt(8);actionText="Магическая вспышка обжигает врага: −"+damage+".";break;
            case "DEFEND": defending=true;stamina=Math.min(maxStamina,stamina+12);magicka=Math.min(maxMagicka,magicka+5);actionText="Ты встаёшь в оборону, восстанавливая силы.";break;
            case "POTION":if(potions==0){battleLog="Лечебных зелий больше нет.";return;}potions--;int restored=Math.min(45,maxHp-hp);hp+=restored;actionText="Ты восстанавливаешь "+restored+" здоровья.";break;
            case "FLEE":if(battleKind.equals("WILD")&&random.nextInt(100)<progress.chance(this,"FLEE")){mode="RESULT";message="Ты уходишь от противника. Трофеев нет.";return;}actionText="Попытка отступить не удалась.";break;
            default:return;
        }
        enemyHp=Math.max(0,enemyHp-damage);
        if(enemyHp==0) {winBattle(actionText);return;}
        int incoming=Math.max(1,enemyDamage+random.nextInt(8)-armor-(defending?enemyDamage/2+3:0));
        hp=Math.max(0,hp-incoming);stamina=Math.min(maxStamina,stamina+3);
        battleLog=actionText+"\n"+enemyName+" отвечает: −"+incoming+" здоровья.";
        if(hp==0) {
            int penalty=gold/3;gold-=penalty;hp=Math.max(1,maxHp/2);stamina=Math.max(1,maxStamina/2);
            mode="DEFEAT";message="Ты приходишь в себя у дороги. Прохожие спасли тебя, но потеряно "+penalty+" септимов. Теперь можно подготовиться и вернуться.";
        }
    }
    private void winBattle(String lastHit) {
        int reward=25+random.nextInt(26)+level*4;int gain=30+level*9;
        gold+=reward;victories++;if(sideQuest==1&&sideKills<3)sideKills++;
        if("QUEST".equals(battleKind)) {questStage=3;reward+=50;gold+=50;gain+=65;}
        if("DRAGON".equals(battleKind)&&progress.dragonStage==3){
            progress.dragonStage=4;
            reward+=85;gold+=85;gain+=90;
            progress.dragonRecent="У сторожевой башни повержен дракон. "+
                "Над полем прозвучал голос, которому откликнулись горы. Следующая цель: Айварстед.";
        }
        addXp(gain);mode="RESULT";
        message=lastHit+"\n\nПобеда над «"+enemyName+"»! +"+reward+" септимов, +"+gain+" опыта.";
        if("QUEST".equals(battleKind))message+="\n\nЗа стражем ты находишь журнал хранителя. Следующая цель: Солитьюд.";
        if("DRAGON".equals(battleKind))message+="\n\n"+progress.dragonRecent;
        if(sideQuest==1)message+="\nЗаказ доски: "+sideKills+"/3.";
    }
    public void addXp(int amount) {
        xp+=amount;
        while(xp>=level*80 && level<100){xp-=level*80;level++;maxHp+=10;maxStamina+=10;maxMagicka+=10;progress.perkPoints++;hp=maxHp;stamina=maxStamina;magicka=maxMagicka;}
    }
    /** Time-limited rest available outside combat, tracked in the old save schema. */
    public boolean waitHours(int hours){
        if(!started||"COMBAT".equals(mode)||hours<1||hours>24)return false;
        int total=hour+hours;
        day+=total/24;hour=total%24;
        hp=Math.min(maxHp,hp+hours*3);
        stamina=Math.min(maxStamina,stamina+hours*7);
        magicka=Math.min(maxMagicka,magicka+hours*6);
        message="Отдых длился "+hours+" ч. Наступил день "+day+", "+
            String.format(java.util.Locale.ROOT,"%02d:00",hour)+".";
        progress.recent=message;
        progress.sync(this);
        return true;
    }
    public boolean drinkHealing() {if(!started||potions<=0||hp>=maxHp)return false;potions--;hp=Math.min(maxHp,hp+45);return true;}
    public boolean drinkMana() {if(!started||manaPotions<=0||magicka>=maxMagicka)return false;manaPotions--;magicka=Math.min(maxMagicka,magicka+35);return true;}
    public String questStatus() {switch(questStage){case 0:return "не начато";case 1:return "Фолкрит";case 2:return "Винтерхолд";case 3:return "Солитьюд";default:return "завершено";}}
    public String questDetails() {switch(questStage){case 0:return "Поговори с хранителем в разделе «Главная история».";case 1:return "Доберись до Фолкрита и осмотри алтарь.";case 2:return "Найди башню в Винтерхолде и победи пепельного стража.";case 3:return "Передай журнал хранителям Солитьюда.";default:return "Сюжетная линия завершена. Ты можешь продолжать приключения.";}}
    private static boolean contains(String[] arr,String v){for(String s:arr)if(s.equals(v))return true;return false;}
    public JSONObject toJson() throws JSONException {
        JSONObject j=new JSONObject();
        j.put("schema",1).put("started",started).put("name",name).put("race",race).put("heroClass",heroClass)
         .put("region",region).put("day",day).put("hour",hour).put("level",level).put("xp",xp).put("maxHp",maxHp).put("hp",hp)
         .put("maxStamina",maxStamina).put("stamina",stamina).put("maxMagicka",maxMagicka).put("magicka",magicka)
         .put("gold",gold).put("potions",potions).put("manaPotions",manaPotions).put("attack",attack).put("armor",armor)
         .put("questStage",questStage).put("sideQuest",sideQuest).put("sideKills",sideKills).put("boardCompleted",boardCompleted).put("explored",explored)
         .put("victories",victories).put("reputation",reputation).put("swordUpgraded",swordUpgraded)
         .put("armorUpgraded",armorUpgraded).put("mode",mode).put("message",message).put("enemyName",enemyName)
         .put("battleLog",battleLog).put("battleKind",battleKind).put("enemyIntro",enemyIntro)
         .put("enemyHp",enemyHp).put("enemyMaxHp",enemyMaxHp).put("enemyDamage",enemyDamage).put("progress",progress.toJson())
         .put("aiJournal",aiJournal).put("aiTurn",aiTurn==null?JSONObject.NULL:aiTurn.save())
         .put("sceneState",sceneState.toJson());
        return j;
    }
    public static GameEngine fromJson(String json) {
        GameEngine g=new GameEngine();
        if(json==null||json.isEmpty())return g;
        try{
            JSONObject j=new JSONObject(json);
            if(j.optInt("schema")!=1)return g;
            g.started=j.optBoolean("started",false);g.name=j.optString("name",g.name);
            g.race=j.optString("race",g.race);g.heroClass=j.optString("heroClass",g.heroClass);
            g.region=Math.max(0,Math.min(REGIONS.length-1,j.optInt("region",0)));
            g.day=Math.max(1,j.optInt("day",1));g.hour=Math.max(0,Math.min(23,j.optInt("hour",8)));g.level=Math.max(1,j.optInt("level",1));g.xp=Math.max(0,j.optInt("xp",0));
            g.maxHp=Math.max(1,j.optInt("maxHp",100));g.hp=Math.max(0,Math.min(g.maxHp,j.optInt("hp",100)));
            g.maxStamina=Math.max(1,j.optInt("maxStamina",65));g.stamina=Math.max(0,Math.min(g.maxStamina,j.optInt("stamina",65)));
            g.maxMagicka=Math.max(1,j.optInt("maxMagicka",40));g.magicka=Math.max(0,Math.min(g.maxMagicka,j.optInt("magicka",40)));
            g.gold=Math.max(0,j.optInt("gold",85));g.potions=Math.max(0,j.optInt("potions",3));g.manaPotions=Math.max(0,j.optInt("manaPotions",1));
            g.attack=Math.max(1,j.optInt("attack",12));g.armor=Math.max(0,j.optInt("armor",2));
            g.questStage=Math.max(0,Math.min(4,j.optInt("questStage",0)));g.sideQuest=j.optInt("sideQuest",0);
            g.sideKills=Math.max(0,j.optInt("sideKills",0));g.boardCompleted=Math.max(0,j.optInt("boardCompleted",0));g.explored=Math.max(0,j.optInt("explored",0));
            g.victories=Math.max(0,j.optInt("victories",0));g.reputation=j.optInt("reputation",0);
            g.swordUpgraded=j.optBoolean("swordUpgraded",false);g.armorUpgraded=j.optBoolean("armorUpgraded",false);
            g.mode=j.optString("mode","WORLD");g.message=j.optString("message","");
            g.enemyName=j.optString("enemyName","");g.battleLog=j.optString("battleLog","");
            g.battleKind=j.optString("battleKind","");g.enemyIntro=j.optString("enemyIntro","");
            g.enemyHp=Math.max(0,j.optInt("enemyHp",0));g.enemyMaxHp=Math.max(0,j.optInt("enemyMaxHp",0));g.enemyDamage=Math.max(0,j.optInt("enemyDamage",0));
            g.progress=RpgProgress.fromJson(j.optJSONObject("progress"));
            g.aiJournal=j.optString("aiJournal","");
            g.aiTurn=AiGameTurn.restore(j.optJSONObject("aiTurn"));
            g.sceneState=SceneState.fromJson(j.optJSONObject("sceneState"));
            // Reconcile legacy schema-1 saves with the authoritative quest stage.
            if(g.questStage>=4)g.progress.setQuestOutcome("ash_seals","COMPLETED");
            else if(g.questStage>0)g.progress.setQuestOutcome("ash_seals","ACTIVE");
            // Rebuild missing continuation ledger entries from authoritative schema-1 stage.
            // Never overwrite terminal outcomes already stored in newer saves.
            if(g.progress.dragonStage>=6){
                for(int chapter=0;chapter<4;chapter++){
                    String questId="dragon_ash_"+chapter;
                    if(chapter<g.progress.dragonNextStage)
                        g.progress.setQuestOutcome(questId,"COMPLETED");
                    else if(chapter==g.progress.dragonNextStage)
                        g.progress.setQuestOutcome(questId,"ACTIVE");
                }
            }
            if(j.optJSONObject("progress")==null)g.progress.location=REGIONS[g.region];
            g.progress.sync(g);
            if(g.mode.equals("COMBAT")&&g.enemyHp<=0)g.mode="WORLD";
        }catch(Exception ignore){return new GameEngine();}
        return g;
    }
}
