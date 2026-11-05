package dev.infend.skyrimtext;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Offline, saveable, original fan-fiction campaign set in a Skyrim-inspired world. */
public final class GameEngine {
    public static final String[] REGIONS = {"Вайтран", "Рифтен", "Виндхельм", "Солитьюд", "Маркарт", "Фолкрит", "Винтерхолд", "Данстар"};
    public static final String[] RACES = {"Норд", "Бретон", "Данмер", "Каджит", "Имперец", "Орк"};
    public static final String[] CLASSES = {"Воин", "Маг", "Вор"};
    private static final String[] DESCRIPTIONS = {
        "Ветер гуляет над золотой равниной. Стража несёт дозор у городских ворот, а с рынка доносятся голоса торговцев.",
        "Мокрая брусчатка отражает огни трактиров. В переулках Рифтена слишком часто оборачиваются на чужие шаги.",
        "Холодный ветер несёт снег с моря. У серых стен Виндхельма слышны тяжёлые удары кузнечных молотов.",
        "Над гаваньью Солитьюда поднимаются башни. При дворе говорят о древних печатях, но редко вслух.",
        "Каменные арки Маркарта скрывают длинные тени. В горах будто продолжает работать забытый механизм.",
        "Тёмные сосны Фолкрита шепчутся над древними захоронениями. Птицы избегают заброшенного алтаря.",
        "Под каменными мостами Винтерхолда ревёт ледяное море. Сквозь метель мерцает свет старой башни.",
        "Корабли вмерзли в залив Данстара. По ночам рыбаки замечают странное сияние на горизонте."
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
    public int region = 0, day = 1, level = 1, xp = 0, maxHp = 100, hp = 100;
    public int maxStamina = 65, stamina = 65, maxMagicka = 40, magicka = 40;
    public int gold = 85, potions = 3, manaPotions = 1, attack = 12, armor = 2;
    public int questStage = 0, sideQuest = 0, sideKills = 0, explored = 0, victories = 0, reputation = 0;
    public boolean swordUpgraded = false, armorUpgraded = false;
    public String mode = "WORLD", message = "Новый день открывает новые дороги.";
    public String enemyName = "", battleLog = "", battleKind = "", enemyIntro = "";
    public int enemyHp = 0, enemyMaxHp = 0, enemyDamage = 0;
    private final Random random = new Random();

    public void start(String desiredName, String desiredRace, String desiredClass) {
        name = desiredName == null || desiredName.trim().isEmpty() ? "Странник" : desiredName.trim();
        if (name.length() > 22) name = name.substring(0,22);
        race = contains(RACES, desiredRace) ? desiredRace : RACES[0];
        heroClass = contains(CLASSES, desiredClass) ? desiredClass : CLASSES[0];
        started=true; region=0; day=1; level=1; xp=0; gold=85; potions=3; manaPotions=1;
        questStage=0; sideQuest=0; sideKills=0; explored=0; victories=0; reputation=0;
        swordUpgraded=false; armorUpgraded=false;
        maxHp=100; maxStamina=65; maxMagicka=40; attack=12; armor=2;
        switch(heroClass) {
            case "Воин": maxHp+=25; maxStamina+=15; attack+=4; armor+=2; break;
            case "Маг": maxMagicka+=45; maxHp-=15; attack+=1; break;
            case "Вор": maxStamina+=30; gold+=30; attack+=2; break;
        }
        switch(race) {
            case "Норд": maxHp+=10; break;
            case "Бретон": maxMagicka+=15; break;
            case "Данмер": attack+=2; break;
            case "Каджит": maxStamina+=10; break;
            case "Имперец": gold+=40; break;
            case "Орк": armor+=2; break;
        }
        hp=maxHp; stamina=maxStamina; magicka=maxMagicka;
        mode="INTRO";
        message="";
    }

    public Scene scene() {
        if (!started) return new Scene("НОВАЯ ИСТОРИЯ", "Твоя сага начинается", "Создай героя, чтобы отправиться в суровые северные земли. Каждое решение меняет путь, а прогресс сохраняется автоматически.");
        switch(mode) {
            case "INTRO": return new Scene("ПРОЛОГ • ДЕНЬ 1", "Пепел на снегу",
                "Ты прибываешь в Вайтран перед сумерками. На дороге найден обломок чёрного камня с выжженным символом. Когда пальцы касаются его поверхности, в голове раздаётся шёпот: «Найди того, кто помнит печати».\n\nНа площади горит свет. У доски объявлений ждёт незнакомец в дорожном плаще.")
                .primary("GO_WORLD", "Войти в город", "Начать путешествие");
            case "TRAVEL": return travelScene();
            case "QUEST": return questScene();
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
        Scene s = new Scene("СКАЙРИМ • ДЕНЬ " + day, REGIONS[region], DESCRIPTIONS[region] +
            (message.isEmpty()?"":"\n\n"+message));
        s.primary("EXPLORE","Исследовать окрестности","Случайные встречи, тайники и опасности");
        s.add("QUEST","Главная история","Пепельные печати • " + questStatus());
        s.add("TALK","Поговорить с местными","Слухи, события и полезные сведения");
        s.add("TRAVEL","Отправиться в путь","Выбрать другой город");
        s.add("SHOP","Торговая лавка","Зелья, оружие и снаряжение");
        s.add("BOARD","Доска заказов","Дополнительное поручение");
        s.add("REST","Отдохнуть в трактире","20 монет • восстановить силы");
        return s;
    }

    private Scene travelScene() {
        Scene s = new Scene("КАРТА СЕВЕРА", "Куда держать путь?", "Дорога занимает день. Открыты все восемь городов. В пути ты восстановишь немного сил.");
        for(int i=0;i<REGIONS.length;i++) if(i!=region) s.add("REGION:"+i, REGIONS[i], regionHint(i));
        return s.add("GO_WORLD","Остаться здесь","Вернуться в город");
    }
    private String regionHint(int i) {
        if(i==5 && questStage==1) return "Цель задания: древний алтарь";
        if(i==6 && questStage==2) return "Цель задания: ледяная башня";
        if(i==3 && questStage==3) return "Цель задания: завершить историю";
        return (i==0?"Центральные равнины":i==1?"Южные леса":i==2?"Восточные снега":i==3?"Северо-западное побережье":i==4?"Каменные ущелья":i==5?"Тёмный лес":i==6?"Ледяное побережье":"Северная гавань");
    }

    private Scene questScene() {
        switch(questStage) {
            case 0: return new Scene("ГЛАВА I", "Пепельные печати", "Пожилой хранитель Эйрн сообщает: из трёх святилищ исчезли печати, которые удерживали нечто под снегами. Он просит тебя найти след реликвии в лесах Фолкрита.\n\n«Никаких легенд не осталось, лишь чёрный пепел на камнях». ")
                .primary("Q_ACCEPT","Принять поручение","+40 золота на дорогу").add("GO_WORLD","Пока отказаться","Вернуться позже");
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
            .add("TALK_GIVE","Помочь монетой","Пожертвовать 10 монет")
            .add("GO_WORLD","Попрощаться","Вернуться на улицу");
    }
    private Scene shopScene() {
        return new Scene("ТОРГОВЛЯ • " + REGIONS[region], "Северная лавка", "На прилавке лежат походные припасы и изделия кузнеца.\n\nТвой кошель: " + gold + " золота.")
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
        if (action.startsWith("REGION:")) {
            try { int to=Integer.parseInt(action.substring(7)); if(to>=0 && to<REGIONS.length) {
                region=to; day++; stamina=Math.min(maxStamina,stamina+20); magicka=Math.min(maxMagicka,magicka+10);
                message="После долгого пути ты входишь в " + REGIONS[to] + "."; mode="WORLD";
            } } catch(NumberFormatException ignored) {} return;
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
                if(gold<20) {message="Хозяин трактира просит 20 монет. Придётся поискать заработок.";mode="RESULT";}
                else {gold-=20;day++;hp=maxHp;magicka=maxMagicka;stamina=maxStamina;message="После тёплого ужина и долгого сна силы восстановились.";mode="RESULT";} break;
            case "Q_ACCEPT": questStage=1;gold+=40;message="Ты принимаешь 40 монет и получаешь первую зацепку: алтарь находится в Фолкрите.";mode="RESULT";break;
            case "Q_ALTAR": if(questStage==1 && region==5) {questStage=2;addXp(35);message="Ты находишь под алтарём серебряную пластину. На ней указан Винтерхолд. +35 опыта.";mode="RESULT";} break;
            case "Q_BOSS": if(questStage==2 && region==6) startBattle("Пепельный страж",90+level*11,12+level*2,"QUEST","Внутри башни оживает существо из пепла и холодного камня."); break;
            case "Q_FINISH": if(questStage==3 && region==3) {questStage=4;gold+=300;reputation+=5;addXp(180);message="Хранители принимают журнал и восстанавливают печать. Скайрим получает ещё один мирный рассвет. Награда: 300 золота и 180 опыта.";mode="RESULT";} break;
            case "TALK_HELP": message=questStage==1?"Слух подтверждается: ищи старые плиты в Фолкрите.":questStage==2?"О башне в Винтерхолде говорят вполголоса. Возьми зелья.":"Поговаривают, что на дороге появилось больше разбойников.";mode="RESULT";break;
            case "TALK_GIVE": if(gold>=10){gold-=10;reputation++;addXp(8);message="Собеседник благодарит тебя. Репутация +1, опыт +8.";}else message="В кошельке нет десяти монет.";mode="RESULT";break;
            case "BUY_HP": buy(35,"Лечебное зелье куплено.","HP");break;
            case "BUY_MP": buy(30,"Зелье магии куплено.","MP");break;
            case "BUY_SWORD": buy(180,"Клинок закалён. Атака +6.","SWORD");break;
            case "BUY_ARMOR": buy(160,"Ты облачаешься в стальной нагрудник. Защита +4.","ARMOR");break;
            case "SIDE_ACCEPT": sideQuest=1;sideKills=0;message="Заказ принят. Победи трёх противников во время исследования мира.";mode="RESULT";break;
            case "SIDE_CLAIM": if(sideQuest==1 && sideKills>=3) {sideQuest=0;sideKills=0;gold+=120;addXp(65);message="Заказ выполнен. +120 золота и +65 опыта.";mode="RESULT";}break;
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
        if((item.equals("SWORD")&&swordUpgraded)||(item.equals("ARMOR")&&armorUpgraded)) {message="Этот предмет у тебя уже есть.";mode="RESULT";return;}
        if(gold<price){message="Недостаточно золота. Нужно ещё " + (price-gold) + " монет.";mode="RESULT";return;}
        gold-=price;
        switch(item){case "HP":potions++;break;case "MP":manaPotions++;break;case "SWORD":swordUpgraded=true;attack+=6;break;case "ARMOR":armorUpgraded=true;armor+=4;break;}
        message=success;mode="RESULT";
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
            if(random.nextInt(4)==0) potions++;
            message="В зарослях найден старый тайник. Внутри " + found + " золотых монет. +12 опыта.";mode="RESULT";
        }
    }
    private void eventChoice(boolean help) {
        if(help){reputation++;int gain=10+random.nextInt(16);addXp(gain);if(random.nextBoolean()){potions++;message="Ты помогаешь незнакомцу и получаешь лечебное зелье. Репутация +1, опыт +"+gain+".";}else{gold+=25;message="В благодарность тебе вручают 25 золотых. Репутация +1, опыт +"+gain+".";}}
        else {if(random.nextInt(3)==0){hp=Math.max(1,hp-17);message="Сработала старая ловушка! −17 здоровья.";}else{int found=28+random.nextInt(44);gold+=found;message="Под камнем найдено " + found + " монет.";}addXp(10);}
        mode="RESULT";
    }
    private void board() {
        if(sideQuest==0){mode="RESULT";sideQuest=2;message="На доске висит объявление: «Избавьте дороги от трёх опасных противников. Плата 120 монет». Открой доску ещё раз, чтобы принять заказ.";}
        else if(sideQuest==2){mode="RESULT";sideQuest=1;sideKills=0;message="Заказ принят. Победи трёх врагов в случайных встречах. Текущий прогресс: 0/3.";}
        else if(sideKills>=3){sideQuest=0;sideKills=0;gold+=120;addXp(65);message="Заказ выполнен. +120 золота, +65 опыта.";mode="RESULT";}
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
            case "HEAVY":if(stamina<10){battleLog="Недостаточно выносливости для сильного удара.";return;}stamina-=10;
                if(random.nextInt(100)<72){damage=Math.max(1,attack*2+random.nextInt(9));actionText="Сильный удар! −"+damage+" здоровья врагу.";}else actionText="Сильный удар проходит мимо.";break;
            case "MAGIC":if(magicka<12){battleLog="Недостаточно магии. Нужны 12 единиц.";return;}magicka-=12;damage=18+level*4+random.nextInt(8);actionText="Магическая вспышка обжигает врага: −"+damage+".";break;
            case "DEFEND": defending=true;stamina=Math.min(maxStamina,stamina+12);magicka=Math.min(maxMagicka,magicka+5);actionText="Ты встаёшь в оборону, восстанавливая силы.";break;
            case "POTION":if(potions==0){battleLog="Лечебных зелий больше нет.";return;}potions--;int restored=Math.min(45,maxHp-hp);hp+=restored;actionText="Ты восстанавливаешь "+restored+" здоровья.";break;
            case "FLEE":if(battleKind.equals("WILD")&&random.nextInt(100)<60){mode="RESULT";message="Ты уходишь от противника. Трофеев нет.";return;}actionText="Попытка отступить не удалась.";break;
            default:return;
        }
        enemyHp=Math.max(0,enemyHp-damage);
        if(enemyHp==0) {winBattle(actionText);return;}
        int incoming=Math.max(1,enemyDamage+random.nextInt(8)-armor-(defending?enemyDamage/2+3:0));
        hp=Math.max(0,hp-incoming);stamina=Math.min(maxStamina,stamina+3);
        battleLog=actionText+"\n"+enemyName+" отвечает: −"+incoming+" здоровья.";
        if(hp==0) {
            int penalty=gold/3;gold-=penalty;hp=Math.max(1,maxHp/2);stamina=Math.max(1,maxStamina/2);
            mode="DEFEAT";message="Ты приходишь в себя у дороги. Прохожие спасли тебя, но потеряно "+penalty+" золота. Теперь можно подготовиться и вернуться.";
        }
    }
    private void winBattle(String lastHit) {
        int reward=25+random.nextInt(26)+level*4;int gain=30+level*9;
        gold+=reward;victories++;if(sideQuest==1&&sideKills<3)sideKills++;
        if("QUEST".equals(battleKind)) {questStage=3;reward+=50;gold+=50;gain+=65;}
        addXp(gain);mode="RESULT";
        message=lastHit+"\n\nПобеда над «"+enemyName+"»! +"+reward+" золота, +"+gain+" опыта.";
        if("QUEST".equals(battleKind))message+="\n\nЗа стражем ты находишь журнал хранителя. Следующая цель: Солитьюд.";
        if(sideQuest==1)message+="\nЗаказ доски: "+sideKills+"/3.";
    }
    public void addXp(int amount) {
        xp+=amount;
        while(xp>=level*80 && level<50){xp-=level*80;level++;maxHp+=16;maxStamina+=6;maxMagicka+=6;attack+=3;armor+=(level%3==0?1:0);hp=maxHp;stamina=maxStamina;magicka=maxMagicka;}
    }
    public boolean drinkHealing() {if(!started||potions<=0||hp>=maxHp)return false;potions--;hp=Math.min(maxHp,hp+45);return true;}
    public boolean drinkMana() {if(!started||manaPotions<=0||magicka>=maxMagicka)return false;manaPotions--;magicka=Math.min(maxMagicka,magicka+35);return true;}
    public String questStatus() {switch(questStage){case 0:return "не начато";case 1:return "Фолкрит";case 2:return "Винтерхолд";case 3:return "Солитьюд";default:return "завершено";}}
    public String questDetails() {switch(questStage){case 0:return "Поговори с хранителем в разделе «Главная история».";case 1:return "Доберись до Фолкрита и осмотри алтарь.";case 2:return "Найди башню в Винтерхолде и победи пепельного стража.";case 3:return "Передай журнал хранителям Солитьюда.";default:return "Сюжетная линия завершена. Ты можешь продолжать приключения.";}}
    private static boolean contains(String[] arr,String v){for(String s:arr)if(s.equals(v))return true;return false;}
    public JSONObject toJson() throws JSONException {
        JSONObject j=new JSONObject();
        j.put("schema",1).put("started",started).put("name",name).put("race",race).put("heroClass",heroClass)
         .put("region",region).put("day",day).put("level",level).put("xp",xp).put("maxHp",maxHp).put("hp",hp)
         .put("maxStamina",maxStamina).put("stamina",stamina).put("maxMagicka",maxMagicka).put("magicka",magicka)
         .put("gold",gold).put("potions",potions).put("manaPotions",manaPotions).put("attack",attack).put("armor",armor)
         .put("questStage",questStage).put("sideQuest",sideQuest).put("sideKills",sideKills).put("explored",explored)
         .put("victories",victories).put("reputation",reputation).put("swordUpgraded",swordUpgraded)
         .put("armorUpgraded",armorUpgraded).put("mode",mode).put("message",message).put("enemyName",enemyName)
         .put("battleLog",battleLog).put("battleKind",battleKind).put("enemyIntro",enemyIntro)
         .put("enemyHp",enemyHp).put("enemyMaxHp",enemyMaxHp).put("enemyDamage",enemyDamage);
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
            g.day=Math.max(1,j.optInt("day",1));g.level=Math.max(1,j.optInt("level",1));g.xp=Math.max(0,j.optInt("xp",0));
            g.maxHp=Math.max(1,j.optInt("maxHp",100));g.hp=Math.max(0,Math.min(g.maxHp,j.optInt("hp",100)));
            g.maxStamina=Math.max(1,j.optInt("maxStamina",65));g.stamina=Math.max(0,Math.min(g.maxStamina,j.optInt("stamina",65)));
            g.maxMagicka=Math.max(1,j.optInt("maxMagicka",40));g.magicka=Math.max(0,Math.min(g.maxMagicka,j.optInt("magicka",40)));
            g.gold=Math.max(0,j.optInt("gold",85));g.potions=Math.max(0,j.optInt("potions",3));g.manaPotions=Math.max(0,j.optInt("manaPotions",1));
            g.attack=Math.max(1,j.optInt("attack",12));g.armor=Math.max(0,j.optInt("armor",2));
            g.questStage=Math.max(0,Math.min(4,j.optInt("questStage",0)));g.sideQuest=j.optInt("sideQuest",0);
            g.sideKills=Math.max(0,j.optInt("sideKills",0));g.explored=Math.max(0,j.optInt("explored",0));
            g.victories=Math.max(0,j.optInt("victories",0));g.reputation=j.optInt("reputation",0);
            g.swordUpgraded=j.optBoolean("swordUpgraded",false);g.armorUpgraded=j.optBoolean("armorUpgraded",false);
            g.mode=j.optString("mode","WORLD");g.message=j.optString("message","");
            g.enemyName=j.optString("enemyName","");g.battleLog=j.optString("battleLog","");
            g.battleKind=j.optString("battleKind","");g.enemyIntro=j.optString("enemyIntro","");
            g.enemyHp=Math.max(0,j.optInt("enemyHp",0));g.enemyMaxHp=Math.max(0,j.optInt("enemyMaxHp",0));g.enemyDamage=Math.max(0,j.optInt("enemyDamage",0));
            if(g.mode.equals("COMBAT")&&g.enemyHp<=0)g.mode="WORLD";
        }catch(Exception ignore){return new GameEngine();}
        return g;
    }
}
