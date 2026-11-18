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
    public String gender = CharacterIdentity.MALE;
    public int portrait = 1;
    public int region = 0, day = 1, hour = 8, level = 1, xp = 0, maxHp = 100, hp = 100;
    public int maxStamina = 65, stamina = 65, maxMagicka = 40, magicka = 40;
    public int gold = 85, potions = 3, manaPotions = 1, attack = 12, armor = 2;
    public int canonicalStage=0,canonicalPhase=0,canonicalPage=0,canonicalShouts=0,canonicalTreaty=0,canonicalPaarthurnax=0;
    public int canonicalShoutCooldown=0;
    public boolean canonicalPrepared=false;
    public String canonicalRecent="";
    public int questStage = 0, sideQuest = 0, sideKills = 0, boardCompleted = 0, explored = 0, victories = 0, reputation = 0;
    public boolean swordUpgraded = false, armorUpgraded = false;
    public String mode = "WORLD", message = "Новый день открывает новые дороги.";
    public String enemyName = "", battleLog = "", battleKind = "", enemyIntro = "";
    public int enemyHp = 0, enemyMaxHp = 0, enemyDamage = 0;
    public RpgProgress progress = new RpgProgress();
    public LivingNeeds needs=new LivingNeeds();
    SolitudeCity solitude=new SolitudeCity();
    public long fixSpent=0;
    public FixShopState fixShop=new FixShopState();
    public CompensationPowers compensation=new CompensationPowers();
    public JSONObject lastLivingResult=new JSONObject();
    public int lastCheckChance=-1,lastCheckRoll=-1;
    public boolean rollCheck(int chance){lastCheckChance=Math.max(0,Math.min(100,chance));lastCheckRoll=random.nextInt(100)+1;if(fixShop.gifts.retry(day,lastCheckChance,lastCheckRoll)){lastCheckRoll=random.nextInt(100)+1;recordVisualEvent("Благосклонность судьбы: повтор проверки");}return lastCheckRoll<=lastCheckChance;}
    public LivingEconomy economy=new LivingEconomy();
    public final int[] regenBonus=new int[3];
    public boolean regenIgnorePenalty;
    public void syncLivingSystems(){needs.sync(day,hour);economy.sync(day);if(started&&itachi!=null&&!"COMBAT".equals(mode))itachi.daily(this);if(started&&livia!=null&&!"COMBAT".equals(mode))livia.daily(this);}
    public int regenRate(int index){return Math.max(0,1+regenBonus[index]+(index==1?compensation.passiveBonus(this,2):0)+progress.artifacts.regen(progress,index)); }
    private void regenerateCombat(){if(hp<=0||!"COMBAT".equals(mode))return;int penalty=regenIgnorePenalty?0:needs.penalty();hp=(int)Math.min(maxHp,(long)hp+(poisonTurns>0&&!regenIgnorePenalty?regenBonus[0]:Math.max(0,regenRate(0)-(penalty>0?1:0))));magicka=(int)Math.min(maxMagicka,(long)magicka+Math.max(regenBonus[1],regenRate(1)-(penalty>0?1:0)));stamina=(int)Math.min(maxStamina,(long)stamina+(frostTurns>0&&!regenIgnorePenalty?regenBonus[2]:Math.max(0,regenRate(2)-(penalty>0?1:0))));}
    public AiGameTurn aiTurn;
    public SceneState sceneState = new SceneState();
    RoadEvents road = new RoadEvents();
    public KatherineStory katherine = new KatherineStory();
    public AlmaStory alma = new AlmaStory();
    public LiviaStory livia = new LiviaStory();
    public ItachiStory itachi = new ItachiStory();
    public String aiJournal = "";
    public int roadSummonTurns,roadSummonKind;
    public int poisonTurns,frostTurns,battleTurns,battleDealt,battleTaken,lastDealt,lastTaken,lastHealing,lastBlocked;
    public boolean lastHeavyHit,lastMiss,lastCritical;
    public final int[] holdReputation=new int[10];
    public void resetBattleVisuals(){roadSummonTurns=roadSummonKind=0;java.util.Arrays.fill(compensation.cooldown,0);poisonTurns=frostTurns=battleTurns=battleDealt=battleTaken=lastDealt=lastTaken=lastHealing=lastBlocked=0;lastHeavyHit=lastMiss=lastCritical=false;}
    private MerchantLedger merchants=new MerchantLedger();
    FactionCampaign factions=new FactionCampaign();
    FactionStories factionStories=new FactionStories();
    int factionBattle=-1;
    public int merchantGold(){return merchants.budget(region,day);}
    public int merchantStock(String action){return merchants.stock(region,day,action.startsWith("BUY_")?action.substring(4):action);}
    public int merchantRestockDay(){return merchants.restockDay(day);}
    public java.util.List<String> merchantExtras(){
        java.util.ArrayList<String> result=new java.util.ArrayList<>();
        for(int i=4;i<MerchantLedger.KEYS.length;i++)if(ItemCatalog.find(MerchantLedger.KEYS[i])!=null&&MerchantLedger.capacity(region,(Math.max(1,day)-1)/MerchantLedger.RESTOCK_DAYS,i)>0)result.add(MerchantLedger.KEYS[i]);
        return result;
    }
    public int merchantExtraPrice(String id){ItemCatalog.Item item=ItemCatalog.find(id);if(item==null||item.value<=0)return 0;return Math.max(1,(int)Math.min(Integer.MAX_VALUE,((long)item.value*(120-5*progress.branches.rank(12)-(progress.advanced.has(AdvancedPerks.HAGGLING)?5:0)-(progress.advanced.has(AdvancedPerks.MERCHANT)?5:0))+99)/100));}
    public boolean buyExtra(String id){
        if(!started||!"SHOP".equals(mode)||!merchantExtras().contains(id))return false;
        int price=merchantExtraPrice(id);ItemCatalog.Item item=ItemCatalog.find(id);
        if(price<=0||merchantStock(id)<=0||gold<price||item==null||progress.carryWeight()+item.weight>progress.carryLimit()+0.00001)return false;
        if(!progress.addItem(id,1))return false;
        if(!merchants.purchase(region,day,id,price)){progress.removeItem(id,1);return false;}
        gold-=price;progress.sync(this);message="Куплено: "+id+" | −"+price+" септимов.";recordVisualEvent(message);return true;
    }
    public int maxExtraPurchase(String id){
        ItemCatalog.Item item=ItemCatalog.find(id);int price=merchantExtraPrice(id);
        if(!"SHOP".equals(mode)||item==null||price<=0||!merchantExtras().contains(id))return 0;
        int weight=item.weight<=0?99:(int)Math.floor((progress.carryLimit()+0.00001-progress.carryWeight())/item.weight);
        return Math.max(0,Math.min(99,Math.min(merchantStock(id),Math.min(gold/price,weight))));
    }
    public boolean buyExtraMany(String id,int amount){
        if(amount<1||amount>maxExtraPurchase(id))return false;
        int total=merchantExtraPrice(id)*amount;
        if(!progress.addItem(id,amount))return false;
        if(!merchants.purchaseMany(region,day,id,total,amount)){progress.removeItem(id,amount);return false;}
        gold-=total;progress.sync(this);message="Куплено: "+id+" ×"+amount+" | −"+total+" септимов.";recordVisualEvent(message);return true;
    }
    public int maxSale(ItemCatalog.Item item){
        if(!canSell(item))return 0;int price=salePrice(item);if(price<=0)return 0;
        return Math.max(0,Math.min(99,Math.min(progress.quantity(item.id),Math.min(merchantGold()/price,(Integer.MAX_VALUE-gold)/price))));
    }
    public boolean sellMany(String id,int amount){
        ItemCatalog.Item item=ItemCatalog.find(id);if(amount<1||amount>maxSale(item))return false;
        int total=salePrice(item)*amount;
        if(!progress.removeItem(id,amount))return false;
        if(!merchants.pay(region,day,total)){progress.addItem(id,amount);return false;}
        gold+=total;progress.sync(this);message="Продано: "+id+" ×"+amount+" | +"+total+" септимов.";recordVisualEvent(message);return true;
    }
    public boolean canSell(ItemCatalog.Item item){
        return started&&"SHOP".equals(mode)&&item!=null&&item.value>0&&progress.quantity(item.id)>0
            &&!progress.equipped(item.id)&&!progress.artifacts.filled(item.id)&&progress.temperQuality(item.id)==0&&!"Квестовые предметы".equals(item.category)&&!"Ключи".equals(item.category)
            &&!"Септимы".equals(item.id)&&!"Лечебное зелье".equals(item.id)&&!"Зелье магии".equals(item.id)
            &&!"Походный клинок".equals(item.id)&&!"Дорожная одежда".equals(item.id)
            &&!(swordUpgraded&&"Стальной меч".equals(item.id))&&!(armorUpgraded&&"Стальная кираса".equals(item.id));
    }
    public int salePrice(ItemCatalog.Item item){return item==null||item.value<=0?0:Math.max(1,(int)((long)item.value*(30+progress.skillLevel(12)/5+5*progress.branches.rank(12)+(progress.advanced.has(AdvancedPerks.MASTER_TRADER)?8:0))/100));}
    public boolean sellOne(String id){
        ItemCatalog.Item item=ItemCatalog.find(id);if(!canSell(item))return false;
        int price=salePrice(item);if((long)gold+price>Integer.MAX_VALUE||merchantGold()<price||!progress.removeItem(id,1))return false;
        if(!merchants.pay(region,day,price)){progress.addItem(id,1);return false;}
        gold+=price;progress.sync(this);message="Продано: "+item.name+" | +"+price+" септимов.";recordVisualEvent(message);return true;
    }
    public final java.util.LinkedHashSet<String> visitedVisual=new java.util.LinkedHashSet<>();
    public org.json.JSONArray visualChronicle=new org.json.JSONArray();
    public void recordVisualEvent(String event){recordVisualEvent(event,null);}
    public void recordVisualEvent(String event,JSONObject details){
        if(event==null||event.trim().isEmpty())return;
        String value=event.trim();if(value.length()>360)value=value.substring(0,360);
        try{org.json.JSONObject entry=new org.json.JSONObject().put("day",day).put("hour",hour).put("location",progress.location).put("event",value);
            if(details!=null)entry.put("details",details);
            org.json.JSONObject last=visualChronicle.optJSONObject(visualChronicle.length()-1);
            if(last!=null&&last.toString().equals(entry.toString()))return;
            if(visualChronicle.length()>=80)visualChronicle.remove(0);visualChronicle.put(entry);
        }catch(org.json.JSONException ignored){}
    }
    public java.util.List<String> latestVisualRoute(){
        for(int i=visualChronicle.length()-1;i>=0;i--){
            JSONObject entry=visualChronicle.optJSONObject(i);JSONObject d=entry==null?null:entry.optJSONObject("details");
            org.json.JSONArray a=d==null?null:d.optJSONArray("route");if(a==null||a.length()<2)continue;
            java.util.ArrayList<String> out=new java.util.ArrayList<>();
            for(int j=0;j<Math.min(32,a.length());j++){String name=a.optString(j);if(!name.isEmpty())out.add(name);}
            if(out.size()>=2)return out;
        }return java.util.Collections.emptyList();
    }
    private final Random random = new Random();

    public void start(String desiredName, String desiredRace, String desiredClass) {
        start(desiredName,desiredRace,desiredClass,0,new int[]{0,0,0});
    }
    public void start(String desiredName,String desiredRace,String desiredClass,int origin,int[] points){
        start(desiredName,desiredRace,desiredClass,origin,points,CharacterIdentity.MALE,1);
    }
    public void start(String desiredName,String desiredRace,String desiredClass,int origin,int[] points,
                      String desiredGender,int desiredPortrait){
        gender=CharacterIdentity.normalizeGender(desiredGender);
        portrait=CharacterIdentity.normalizePortrait(desiredPortrait);
        name=desiredName==null||desiredName.trim().isEmpty()?RpgContent.randomName(gender):desiredName.trim();
        if(name.length()>28)name=name.substring(0,28);
        race=contains(RACES,desiredRace)?desiredRace:"Норд";
        heroClass=contains(CLASSES,desiredClass)?desiredClass:"Без класса";
        started=true;solitude=new SolitudeCity();compensation=new CompensationPowers();compensation.installCatalog();lastLivingResult=new JSONObject();lastCheckChance=lastCheckRoll=-1;needs=new LivingNeeds();economy=new LivingEconomy();java.util.Arrays.fill(regenBonus,0);regenIgnorePenalty=false;fixSpent=0;fixShop=new FixShopState();aiTurn=null;sceneState=new SceneState();road=new RoadEvents();katherine=new KatherineStory();alma=new AlmaStory();livia=new LiviaStory();itachi=new ItachiStory();aiJournal="";day=1;hour=8;level=1;xp=0;gold=85;potions=3;manaPotions=1;
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
        canonicalStage=canonicalPhase=canonicalPage=canonicalShouts=canonicalTreaty=canonicalPaarthurnax=canonicalShoutCooldown=0;canonicalPrepared=false;canonicalRecent="";
        syncLivingSystems();mode="INTRO";message="";merchants=new MerchantLedger();factions=new FactionCampaign();factionStories=new FactionStories();factionBattle=-1;resetBattleVisuals();java.util.Arrays.fill(holdReputation,0);visitedVisual.clear();visualChronicle=new org.json.JSONArray();visitedVisual.add(progress.location);
    }

    public Scene scene() {
        if(started&&RoadEvents.MODE.equals(mode))return road.scene(this);
        if(started&&road.pending()&&("RESULT".equals(mode)||"DEFEAT".equals(mode)))return new Scene("В ДОРОГЕ","Последствия встречи",message).primary("GO_WORLD","Продолжить переход","Следовать к месту назначения");
        if(started&&ItachiStory.MODE.equals(mode)){if(itachi.present(this))return itachi.scene(this);mode="WORLD";}
        if(started&&LiviaStory.MODE.equals(mode)){if(livia.enabled&&livia.present(this))return livia.scene(this);mode="WORLD";}
        if(started&&AlmaStory.MODE.equals(mode)){if(alma.enabled)return alma.scene(this);mode="WORLD";}
        if(started&&KatherineStory.MODE.equals(mode))return katherine.scene(this);
        if(started&&"CANON".equals(mode))return CanonicalCampaign.scene(this);
        if(started&&SolitudeCity.isMode(mode)){if(SolitudeCity.here(this))return solitude.scene(this);mode="WORLD";}
        Scene s=baseScene();
        if(started){progress.fillChoices(this,s);FixShopAdventure.choices(this,s);FixShopAdventure.endings(this,s);FixShopWorld.choices(this,s);FixShopHome.choices(this,s);FixShopGifts.choices(this,s);}
        if(itachi.available(this))s.add("ITACHI:open","Поговорить с Итачи",itachi.objective(this));
        if(livia.available(this))s.add("LIVIA:open","Поговорить с Ливией",livia.objective());
        if(alma.available(this))s.add("ALMA:open","Поговорить с Альмой","Алая Бессмертная | "+alma.status());
        if(katherine.available(this))s.add("KATH:open","Поговорить с Кэтрин","Нулевой Предел | "+katherine.status());
        // Every offline scene follows the same long-form literary presentation.
        // SceneNarrative is text-only: action IDs, story flags and saves are unchanged.
        return SceneNarrative.expand(this,s);
    }
    private Scene baseScene(){
        if (!started) return new Scene("НОВАЯ ИСТОРИЯ", "Твоя сага начинается", "Создай героя, чтобы отправиться в суровые северные земли. Каждое решение меняет путь, а прогресс сохраняется автоматически.");
        switch(mode) {
            case "INTRO": return new Scene("ПРОЛОГ | ДЕНЬ "+day,progress.startId,
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
        Scene s = new Scene("СКАЙРИМ | ДЕНЬ " + day, progress.location + " | " + REGIONS[region], DESCRIPTIONS[region] + "\n\nМестность: "+progress.location+"." +
            (message.isEmpty()?"":"\n\n"+message));
        s.primary("CANON_OPEN","Главная история | Путь Довакина",CanonicalCampaign.objective(this));
        s.add("DRAGON_OPEN","Драконий след",DragonArc.status(this));
        s.add("EXPLORE","Исследовать окрестности","Случайные встречи, тайники и опасности");
        SolitudeWolf.worldChoices(this,s);
        if(SolitudeCity.here(this))s.add("SOLITUDE:open","Жизнь Солитьюда","Районы столицы, местные занятия, услуги и заработок");
        s.add("QUEST","История хранителей","Пепельные печати | " + questStatus());
        s.add("TALK","Поговорить с местными","Слухи, события и полезные сведения");
        s.add("TRAVEL","Отправиться в путь","Выбрать другой город");
        s.add("SHOP","Торговая лавка","Зелья, оружие и снаряжение");
        s.add("BOARD","Доска заказов","Дополнительное поручение");
        s.add("REST","Отдохнуть в трактире","20 септимов | восстановить силы");
        RoadQuests.choices(this,s);road.fineChoices(this,s);road.estateChoices(this,s);progress.artifacts.choices(this,s);
        return s;
    }

    private Scene travelScene() {
        Scene s = new Scene("ДОРОГИ СЕВЕРА", "Куда держать путь?", "Поездки занимают несколько дней и требуют перехода между дорогами владений. На Солстейм ходит корабль из Виндхельма.");
        for(int i=0;i<REGIONS.length;i++)if(i!=region){
            WorldAtlas.Place target=WorldAtlas.find(TravelPlanner.HUBS[i]);
            TravelPlanner.Route route=TravelPlanner.plan(this,target);
            s.add("REGION:"+i,REGIONS[i],regionHint(i)+(route.allowed?
                " | "+route.days+" дн."+(route.fare>0?" | "+route.fare+" септимов":""):" | "+route.problem));
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
        return new Scene("ТОРГОВЛЯ | " + REGIONS[region], "Северная лавка", "На прилавке лежат походные припасы и изделия кузнеца.\n\nТвой кошель: " + gold + " септимов.\nКошелёк торговца: "+merchantGold()+" септимов.\nНовая поставка: день "+merchantRestockDay()+".")
            .primary("BUY_HP","Лечебное зелье | 35", "Восстанавливает 45 здоровья | Осталось: "+merchantStock("HP"))
            .add("BUY_MP","Зелье магии | 30", "Восстанавливает 35 магии | Осталось: "+merchantStock("MP"))
            .add("BUY_SWORD",swordUpgraded?"Клинок приобретён":"Закалённый клинок | 180", swordUpgraded?"Уже в инвентаре":"Стальной меч | экипируется при покупке")
            .add("BUY_ARMOR",armorUpgraded?"Доспех приобретён":"Стальной нагрудник | 160",armorUpgraded?"Уже в инвентаре":"Стальная кираса | экипируется при покупке")
            .add("GO_WORLD","Покинуть лавку","Вернуться на улицу");
    }
    private Scene eventScene() {
        return new Scene("СЛУЧАЙНАЯ ВСТРЕЧА", "Выбор за тобой", message)
            .primary("EVENT_HELP","Оказать помощь","Проявить благородство")
            .add("EVENT_SEARCH","Осмотреть местность","Проверить, что скрыто неподалёку")
            .add("EVENT_LEAVE","Пройти мимо","Не рисковать");
    }
    private Scene battleScene() {
        Scene s=new Scene("БОЙ | " + REGIONS[region], enemyName, enemyIntro + "\n\n" + battleLog +
            "\n\nПротивник: " + enemyHp + "/" + enemyMaxHp + " здоровья."+road.groupStatus(this)+"\nТы: " + hp + "/" + maxHp + " здоровья.")
            .primary("ATTACK","Обычный удар","Стабильный урон, без затрат")
            .add("HEAVY","Сильный удар","−"+heavyCost()+" выносливости | риск промаха")
            .add("MAGIC","Магическая вспышка","−"+magicCost()+" магии | усиленный урон")
            .add("DEFEND","Защититься","Уменьшить ответный урон")
            .add("POTION","Выпить лечебное зелье","Осталось: " + potions);
        road.targetChoices(this,s);progress.artifacts.combatChoices(this,s);
        if(roadSummonTurns==0&&progress.equipped("Роза Сангвина"))s.add("ROAD_SUMMON_ROSE","Призвать дремора","20 магии | три хода помощи");
        if(roadSummonTurns==0&&progress.equipped("Этериевый посох"))s.add("ROAD_SUMMON_AETHER","Призвать автоматон","20 магии | три хода помощи");
        if("WILD".equals(battleKind))s.add("FLEE","Отступить","Шанс выйти из боя без награды");
        if("CANONICAL".equals(battleKind)&&canonicalShouts!=0&&canonicalShoutCooldown==0)s.add("CANON_SHOUT",CanonicalCampaign.combatShout(this),"Урон и ослабление врага | Восстановление 3 хода");
        if(fixShop.gifts.availableBattle(FixShopGifts.TIME))s.add("FIXGIFT:time","Пески времени","Пропуск ответного удара | раз за бой");
        if(fixShop.gifts.canBalance(this))s.add("FIXGIFT:balance","Равновесие стихий","Магия → выносливость | раз за бой");
        if(fixShop.gifts.canEscape(this))s.add("FIXGIFT:escape","Тропа призрака","Гарантированное отступление без трофеев");
        if(fixShop.gifts.canBlood(this))s.add("FIXGIFT:blood","Кровавый обмен","Здоровье → магия | раз за бой");
        for(int i=0;i<CompensationPowers.SLOTS;i++)if(compensation.available(this,i))s.add("FIX_POWER:"+i,compensation.powerName(i),compensation.detail(i));
        return s;
    }

    /** Record only facts produced by successful, engine-authorized actions. */
    private void confirmSceneFact(String kind, String key, String title, String status) {
        try { sceneState.setContext(day,hour,progress.location);sceneState.confirm(kind, key, title, status); }
        catch (JSONException ignored) {
            // Optional scene memory must never cancel an already committed game action.
        }
    }
    private String currentConversationId() {
        String[] names = {"Седой караульный", "Торговка пряностями", "Усталый путешественник"};
        return "local_" + Math.floorMod(region + day, names.length) + "_region_" + region;
    }
    private String currentConversationName() {
        String[] names = {"Седой караульный", "Торговка пряностями", "Усталый путешественник"};
        return names[Math.floorMod(region + day, names.length)];
    }
    private void confirmCurrentConversation() {
        try {
            // Reopening a conversation must not erase an already earned NPC status.
            sceneState.setContext(day,hour,progress.location);
            if (!sceneState.contains("npc", currentConversationId()))
                sceneState.confirm("npc", currentConversationId(), currentConversationName(), "MET");
            else sceneState.revisitNpc(currentConversationId());
        } catch (JSONException ignored) {
            // Scene memory is optional and cannot block dialogue.
        }
    }
    private boolean hasNpcStatus(String suffix) {
        try { return "HELPED".equals(sceneState.status("npc", currentConversationId()))
                || sceneState.contains("npc", currentConversationId() + suffix); }
        catch (JSONException ignored) { return false; }
    }

    public void choose(String action) {
        if (!started || action==null) return;
        lastCheckChance=lastCheckRoll=-1;
        if(solitude.custody()){solitude.choose(this,action);return;}
        if(RoadEvents.MODE.equals(mode)){road.choose(this,action);return;}
        if(road.pending()&&!"COMBAT".equals(mode)){if("GO_WORLD".equals(action))road.resume(this);return;}
        if(action.startsWith("WOLF:")){SolitudeWolf.chooseWorld(this,action);return;}
        if(action.startsWith("SOLITUDE:")||SolitudeCity.isMode(mode)){solitude.choose(this,action);return;}
        if(action.startsWith("FIXGIFT:")){if("FIXGIFT:escape".equals(action)){if(fixShop.gifts.escape(this))recordVisualEvent(message);}else if("FIXGIFT:blood".equals(action)||"FIXGIFT:time".equals(action)||"FIXGIFT:balance".equals(action)){if("COMBAT".equals(mode))combatTurn(action);}else FixShopGifts.choose(this,action);return;}
        if(action.startsWith("FIXRESIDENCE:")){FixShopHome.choose(this,action);return;}
        if(action.startsWith("FIXPLACE:")||action.startsWith("FIXHOME:")||action.startsWith("FIXNPC:")||action.startsWith("FIXCUSTOM:")){FixShopWorld.choose(this,action);return;}
        if(action.startsWith("FIXQUEST:")){FixShopAdventure.choose(this,action);return;}
        if(action.startsWith("ROAD:"))return;
        if(action.startsWith("ROAD_TARGET:")){road.selectTarget(this,action);return;}
        if("ROAD_SUMMON_ROSE".equals(action)||"ROAD_SUMMON_AETHER".equals(action)){combatTurn(action);return;}
        if(action.startsWith("ROADQUEST:")){RoadQuests.choose(this,action);return;}
        if(action.startsWith("ROADSTONE:")){progress.artifacts.activate(this,action);return;}
        if("ROADSTONE_TOWER".equals(action)){progress.artifacts.useTower(this);return;}
        if(action.startsWith("ROADSTAR:")){progress.artifacts.releaseOutside(this,action);return;}
        if(action.startsWith("ROADSTONE_")||action.startsWith("ROADSTAR_")){combatTurn(action);return;}
        if(action.startsWith("ROADFINE:")){road.payFine(this,action);return;}
        if(action.startsWith("ROADJAIL:")){if("ROADJAIL:3".equals(action)&&SolitudeCity.here(this))solitude.beginSentence(this);else road.serveSentence(this,action);return;}
        if(action.startsWith("ROADESTATE:")){road.buyEstate(this,action);return;}
        if(action.startsWith("ROADSTART:")){RoadQuests.start(this,action.substring("ROADSTART:".length()));return;}
        if(action.startsWith("ROADDEBT:")){if("ROADDEBT:pay".equals(action)||"ROADDEBT:negotiate".equals(action))RoadQuests.payDebt(this,"ROADDEBT:negotiate".equals(action));return;}
        int fixPower=CompensationPowers.actionIndex(action);if(fixPower>=0){if(compensation.available(this,fixPower))combatTurn(action);return;}
        // A pending battle cannot be escaped by invoking a world, map or quest action.
        // This guard also protects saved games restored directly into COMBAT mode.
        if("CANON_SHOUT".equals(action)){if("COMBAT".equals(mode)&&"CANONICAL".equals(battleKind))combatTurn(action);return;}
        if ("COMBAT".equals(mode) && !("ATTACK".equals(action) || "HEAVY".equals(action)
                || "MAGIC".equals(action) || "DEFEND".equals(action)
                || "POTION".equals(action) || "FLEE".equals(action))) return;
        // Event rewards and penalties are single-use. A stale button or replayed
        // action must not resolve the same encounter again after mode changes.
        if (("EVENT_HELP".equals(action) || "EVENT_SEARCH".equals(action)
                || "EVENT_LEAVE".equals(action)) && !"EVENT".equals(mode)) return;
        // Conversations cannot be replayed after leaving their actual scene.
        if (("TALK_HELP".equals(action) || "TALK_GIVE".equals(action))
                && !"TALK".equals(mode)) return;
        // A saved or stale quest button must not initiate a second encounter.
        if (("Q_ACCEPT".equals(action) || "Q_ALTAR".equals(action)
                || "Q_BOSS".equals(action) || "Q_FINISH".equals(action))
                && !"QUEST".equals(mode)) return;
        if(action.startsWith("ITACHI:")){itachi.choose(this,action);return;}
        if(ItachiStory.MODE.equals(mode))return;
        if(action.startsWith("LIVIA:")){livia.choose(this,action);return;}
        if(LiviaStory.MODE.equals(mode))return;
        if(action.startsWith("ALMA:")){alma.choose(this,action);return;}
        if(AlmaStory.MODE.equals(mode))return;
        if(action.startsWith("KATH:")){katherine.choose(this,action);return;}
        if(KatherineStory.MODE.equals(mode))return;
        if("CANON_OPEN".equals(action)){CanonicalCampaign.open(this);return;}
        if(action.startsWith("CANON:")){CanonicalCampaign.perform(this,action);return;}
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
            if(!road.resuming&&road.begin(this,route,target))return;
            gold-=route.fare;
            day+=route.days;
            region=target.region;
            progress.location=target.name;
            katherine.observeTravel(this);katherine.autonomous(this);alma.observeTravel(this);
            katherine.rest(Math.min(24,route.days*3));alma.rest(Math.min(24,route.days*3));itachi.rest(Math.min(24,route.days*3));livia.rest(Math.min(24,route.days*3));
            // Register only locations reached through the validated atlas route.
            // A stable key keeps repeat visits idempotent across save/load.
            try {
                sceneState.confirm("sublocation", "atlas_" + Integer.toHexString(target.name.hashCode()),
                        target.name, "VISITED");
            } catch (JSONException ignored) {
                // Optional scene metadata cannot invalidate a completed journey.
            }
            stamina=Math.max(1,stamina-Math.min(Math.max(0,stamina-1),route.days*Math.max(1,(progress.advanced.has(AdvancedPerks.STEALTH)?5:6)-Math.max(road.hasMount(this)?2:0,fixShop.world.travelBonus()))));
            magicka=Math.max(1,magicka-Math.min(Math.max(0,magicka-1),route.days*2));
            String roadEvent=road.resuming?road.outcome:"";
            message="Ты преодолеваешь дорогу, постепенно оставляя за спиной стоянки, перевалы и чужие костры. "+
                route.summary()+"\n\nМесто назначения: "+target.name+". "+target.description+
                (roadEvent.isEmpty()?"":"\n\nВ дороге: "+roadEvent);
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
            case "TALK": confirmCurrentConversation(); mode="TALK"; break;
            case "EXPLORE": explore(); break;
            case "BOARD": board(); break;
            case "REST":
                if(gold<20) {message="Хозяин трактира просит 20 септимов. Придётся поискать заработок.";mode="RESULT";}
                else {gold-=20;day++;syncLivingSystems();alma.rest(8);itachi.rest(8);livia.rest(8);alma.observeTravel(this);needs.tavernRest();hp=maxHp;magicka=maxMagicka;stamina=maxStamina;message="После тёплого ужина и долгого сна силы восстановились.";mode="RESULT";} break;
            case "Q_ACCEPT": if(questStage==0) {questStage=1;gold=nonnegativeAdd(gold,40);message="Ты принимаешь 40 септимов и получаешь первую зацепку: алтарь находится в Фолкрите.";mode="RESULT";} break;
            case "Q_ALTAR": if(questStage==1 && region==5) {questStage=2;confirmSceneFact("clue", "quest_silver_plate", "Серебряная пластина под алтарём Фолкрита", "FOUND");addXp(35);message="Ты находишь под алтарём серебряную пластину. На ней указан Винтерхолд. +35 опыта.";mode="RESULT";} break;
            case "Q_BOSS": if(questStage==2 && region==6) startBattle("Пепельный страж",90+level*11,12+level*2,"QUEST","Внутри башни оживает существо из пепла и холодного камня."); break;
            case "Q_FINISH": if(questStage==3 && region==3) {questStage=4;confirmSceneFact("clue", "quest_seal", "Печать хранителей восстановлена", "RESOLVED");gold=nonnegativeAdd(gold,300);reputation+=5;addXp(180);message="Хранители принимают журнал и восстанавливают печать. Скайрим получает ещё один мирный рассвет. Награда: 300 септимов и 180 опыта.";mode="RESULT";} break;
            case "TALK_HELP":
                confirmCurrentConversation();
                if(!hasNpcStatus("_helped"))
                    confirmSceneFact("npc", currentConversationId(), currentConversationName(), "INFORMED");
                message=questStage==1?"Слух подтверждается: ищи старые плиты в Фолкрите.":questStage==2?"О башне в Винтерхолде говорят вполголоса. Возьми зелья.":"Поговаривают, что на дороге появилось больше разбойников.";
                mode="RESULT";break;
            case "TALK_GIVE":
                confirmCurrentConversation();
                if(hasNpcStatus("_helped")) {
                    message="Ты уже помог этому собеседнику. Повторная награда не положена.";
                } else if(gold>=10) {
                    // Commit the one-time achievement BEFORE paying or awarding anything.
                    // A full optional ledger must never turn charity into an XP farm.
                    try {
                        sceneState.setContext(day,hour,progress.location);
                        sceneState.confirm("npc", currentConversationId(), currentConversationName(), "HELPED");
                        gold-=10;reputation++;addXp(8);
                        message="Собеседник благодарит тебя. Репутация +1, опыт +8.";
                    } catch(JSONException ledgerFull) {
                        message="Журнал знакомств заполнен. Награда и септимы не изменились.";
                    }
                } else message="В кошельке нет десяти септимов.";
                mode="RESULT";break;
            case "BUY_HP": buy(35,"Лечебное зелье куплено.","HP");break;
            case "BUY_MP": buy(30,"Зелье магии куплено.","MP");break;
            case "BUY_SWORD": buy(180,"Клинок закалён. Атака +6.","SWORD");break;
            case "BUY_ARMOR": buy(160,"Ты облачаешься в стальной нагрудник. Защита +4.","ARMOR");break;
            case "SIDE_ACCEPT": if(sideQuest==2) {sideQuest=1;sideKills=0;message="Заказ принят. Победи трёх противников во время исследования мира.";mode="RESULT";} break;
            case "SIDE_CLAIM": claimBoard();break;
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
        if(!"SHOP".equals(mode))return;
        if(merchantStock(item)<=0){message="Товар закончился. Новая поставка: день "+merchantRestockDay()+".";mode="RESULT";return;}
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
        if(projected>progress.carryLimit()+0.00001){
            message="Нельзя взять предмет: вес превысит 50 кг. Септимы не списаны.";
            mode="RESULT";return;
        }
        if(!merchants.purchase(region,day,item,price)){message="Товар закончился.";mode="RESULT";return;}
        gold-=price;
        switch(item){
            case "HP":potions++;break;
            case "MP":manaPotions++;break;
            case "SWORD":swordUpgraded=true;attack+=6;break;
            case "ARMOR":armorUpgraded=true;armor+=4;break;
        }
        progress.sync(this);
        if(item.equals("SWORD")||item.equals("ARMOR"))progress.shopEquip(target);
        message=success;mode="RESULT";
    }
    private void explore() {
        explored++;stamina=Math.max(0,stamina-5);
        int roll=random.nextInt(100);
        if(roll<49){
            String[] enemies={"Лесной разбойник","Снежный волк","Изгой","Пещерный охотник","Наёмник с дороги","Морозный паук","Ледяной призрак"};
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
            int found=15+random.nextInt(41);gold=nonnegativeAdd(gold,found);addXp(12);
            boolean extraPotion=random.nextInt(4)==0;
            boolean canTake=progress.carryWeight()+0.5<=progress.carryLimit()+0.00001;
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
        boolean succeeded=rollCheck(successChance);
        if(succeeded){
            if(help){
                reputation++;int gain=10+random.nextInt(16);addXp(gain);
                if(random.nextBoolean()){
                    boolean canCarry=progress.carryWeight()+0.5<=progress.carryLimit()+0.00001;
                    if(canCarry)potions++;
                    message=canCarry?"Ты помогаешь незнакомцу и получаешь лечебное зелье. Репутация +1, опыт +"+gain+".":
                        "Ты помогаешь путнику, но оставляешь зелье из-за тяжёлой поклажи. Репутация +1, опыт +"+gain+".";
                }
                else{gold=nonnegativeAdd(gold,25);message="В благодарность тебе вручают 25 септимов. Репутация +1, опыт +"+gain+".";}
            }else{
                int found=28+random.nextInt(44);gold=nonnegativeAdd(gold,found);addXp(10);
                message="Под камнем найдено "+found+" септимов. Получено 10 опыта.";
            }
            progress.completed.add((help?"Помощь путнику":"Исследование находки")+" | день "+day);
        }else{
            if(help){reputation=Math.max(-50,reputation-1);message="Незнакомец принимает жест за угрозу и уходит. Репутация −1.";}
            else{hp=Math.max(1,hp-17);message="Сработала старая ловушка! −17 здоровья.";}
            progress.failed.add((help?"Помощь путнику":"Исследование находки")+" | день "+day);
        }
        mode="RESULT";
    }
    private void claimBoard() {
        if(sideQuest!=1 || sideKills<3)return;
        sideQuest=0;sideKills=0;boardCompleted=nonnegativeAdd(boardCompleted,1);
        gold=nonnegativeAdd(gold,120);addXp(65);
        message="Заказ выполнен. +120 септимов и +65 опыта.";mode="RESULT";
    }
    private void board() {
        if(sideQuest==0){mode="RESULT";sideQuest=2;message="На доске висит объявление: «Избавьте дороги от трёх опасных противников. Плата 120 септимов». Открой доску ещё раз, чтобы принять заказ.";}
        else if(sideQuest==2){mode="RESULT";sideQuest=1;sideKills=0;message="Заказ принят. Победи трёх врагов в случайных встречах. Текущий прогресс: 0/3.";}
        else if(sideKills>=3){claimBoard();}
        else {message="Заказ активен. Побеждено противников: "+sideKills+"/3.";mode="RESULT";}
    }
    void beginFactionBattle(int f,String name,int health,int damage,String intro){factionBattle=f;startBattle(name,health,damage,"FACTION",intro);}
    void beginCanonicalBattle(String name,int health,int damage,String intro){canonicalShoutCooldown=0;startBattle(name,health,damage,"CANONICAL",intro);}
    void beginRoadQuestBattle(String id,String enemy,String intro){startBattle(enemy,55+level*5,6+level/3,"ROAD_QUEST:"+id,intro);}
    void beginRoadBattle(String name,int health,int damage,String intro){startBattle(name,health,damage,"WILD",intro);}
    void beginLiviaDuel(){startBattle("Ливия",100+level*9,10+level*2,"LIVIA_DUEL","Ливия обнажает меч. Этот поединок может завершиться её гибелью.");}
    void beginAlmaBattle(){startBattle("Убийца Тёмного Братства",75+level*8,8+level*2,"ALMA","Преследователь преграждает дорогу. Альма предупреждает о засаде.");}
    void beginKatherineBattle(){startBattle("Агент Круга Трёх Когтей",75+level*8,8+level*2,"KATHERINE","Кэтрин поднимает барьер. Агент Круга требует, чтобы она сдалась.");}
    void beginFixShopBattle(int i,String name,String intro){startBattle(name,65+level*6,7+level*2,"FIX_QUEST:"+i,intro);}
    void beginSolitudeWolfBattle(String stage,String name,int health,int damage,String intro){startBattle(name,health,damage,SolitudeWolf.KIND+stage,intro);}
    private void startBattle(String name,int health,int dmg,String kind,String intro) {
        resetBattleVisuals();fixShop.gifts.beginBattle();progress.artifacts.trapTurns=0;alma.beginBattle();itachi.beginBattle();katherine.companion.beginBattle();enemyName=name;enemyHp=health;enemyMaxHp=health;enemyDamage=dmg;battleKind=kind;enemyIntro=intro;
        battleLog="В бою важна тактика. Следи за запасом сил и магии.";mode="COMBAT";
    }
    // Retain class/quest/AI bonuses in old saves without counting the legacy
    // shop's fixed equipment bonus twice. New gear uses the catalog statistic.
    public int effectiveAttack(){
        int intrinsic=Math.max(0,attack-(swordUpgraded?6:0)-7)+compensation.passiveBonus(this,0);
        return Math.max(1,intrinsic+(int)Math.round(progress.equippedDamage()*(1+.05*progress.branches.rank(progress.equippedWeaponSkill()))));
    }
    public int effectiveArmor(){
        return Math.max(0,armor-(armorUpgraded?4:0))+(int)Math.round(progress.equippedArmor());
    }
    public int heavyCost(){int skill=progress.equippedWeaponSkill();return fixShop.gifts.cost((skill==0&&progress.advanced.has(AdvancedPerks.FIGHTING)||skill==1&&progress.advanced.has(AdvancedPerks.CHAMPION)?7:10)-(factionStories.beast()==1?1:0),FixShopGifts.ENDURANCE);}
    public int magicCost(){return fixShop.gifts.cost((progress.advanced.has(AdvancedPerks.NOVICE_DESTRUCTION)?9:12)-(progress.advanced.has(AdvancedPerks.MASTER_DESTRUCTION)?3:0),FixShopGifts.ARCHMAGE);}
    public int criticalChance(){int skill=progress.equippedWeaponSkill();return 8+(skill==2&&progress.advanced.has(AdvancedPerks.CRITICAL_SHOT)?10:0)+(skill==0&&progress.advanced.has(AdvancedPerks.BLADESMAN)||skill==1&&progress.advanced.has(AdvancedPerks.GREAT_CRITICAL)?8:0)+(skill==2&&progress.advanced.has(AdvancedPerks.BULLSEYE)?12:0);}
    private void combatTurn(String action) {
        if(!mode.equals("COMBAT"))return;
        lastDealt=lastTaken=lastHealing=lastBlocked=0;lastHeavyHit=lastMiss=lastCritical=false;
        int damage=0,artifactWeakness=0;boolean defending=false,shadow=false;String actionText="";
        int custom=CompensationPowers.actionIndex(action);
        if(custom>=0){if(!compensation.available(this,custom))return;magicka-=compensation.cost(custom);if(!compensation.safe[custom])hp-=2;compensation.cooldown[custom]=4;
            int power=compensation.strength(custom);if(compensation.powerType[custom]==1){lastHealing=Math.min(maxHp-hp,power);hp+=lastHealing;actionText=compensation.powerName(custom)+": +"+lastHealing+" здоровья.";}else{damage=power;defending=compensation.powerType[custom]==2;actionText=compensation.powerName(custom)+": −"+damage+" здоровья врагу.";}
        }else switch(action){
            case "ROADSTONE_RITUAL":if(!progress.artifacts.powerReady(this,10)||roadSummonTurns>0)return;progress.artifacts.usePower(this,10);roadSummonKind=3;roadSummonTurns=3;actionText="Камень Ритуала поднимает мертвеца.";break;
            case "ROADSTONE_SERPENT":if(!progress.artifacts.powerReady(this,11))return;progress.artifacts.usePower(this,11);damage=25;defending=true;actionText="Камень Змея отравляет и ослабляет врага.";break;
            case "ROADSTONE_SHADOW":if(!progress.artifacts.powerReady(this,12))return;progress.artifacts.usePower(this,12);shadow=true;actionText="Камень Тени скрывает героя.";break;
            case "ROADSTAR_TRAP":if(progress.artifacts.trapTurns>0||magicka<10||!((progress.artifacts.whiteSoul==0&&progress.quantity("Звезда Азуры")>0)||(progress.artifacts.blackSoul==0&&progress.quantity("Звезда Чёрной Азуры")>0)))return;magicka-=10;progress.artifacts.trapTurns=3;actionText="Наложен захват души на три хода.";break;
            case "ROADSTAR_WHITE":case "ROADSTAR_BLACK":int mana=progress.artifacts.release(this,"ROADSTAR_WHITE".equals(action)?1:2);if(mana==0)return;actionText="Душа высвобождена: +"+mana+" магии.";break;
            case "ROAD_SUMMON_ROSE":case "ROAD_SUMMON_AETHER":if(roadSummonTurns>0||magicka<20||!progress.equipped("ROAD_SUMMON_ROSE".equals(action)?"Роза Сангвина":"Этериевый посох"))return;magicka-=20;roadSummonKind="ROAD_SUMMON_ROSE".equals(action)?1:2;roadSummonTurns=3;actionText=roadSummonKind==1?"Роза Сангвина призывает дремора.":"Этериевый посох призывает автоматон.";break;
            case "FIXGIFT:time":if(!fixShop.gifts.useBattle(FixShopGifts.TIME))return;shadow=true;actionText="Пески времени останавливают ответную атаку.";break;
            case "FIXGIFT:balance":if(!fixShop.gifts.balance(this))return;defending=true;actionText="Равновесие стихий превращает магию в выносливость.";break;
            case "FIXGIFT:blood":if(!fixShop.gifts.blood(this))return;defending=true;actionText="Кровавый обмен: жизненная сила обращена в магию.";break;
            case "ATTACK": damage=Math.max(1,effectiveAttack()+random.nextInt(9)-3);actionText="Ты наносишь удар: −"+damage+" здоровья врагу.";break;
            case "HEAVY":if(stamina<heavyCost()){battleLog="Недостаточно выносливости для сильного удара.";return;}
                int hitChance=progress.chance(this,"HEAVY");stamina-=heavyCost();
                if(rollCheck(hitChance)){damage=Math.max(1,(int)Math.round((effectiveAttack()*2+random.nextInt(9))*(1+(progress.equippedWeaponSkill()==0&&progress.advanced.has(AdvancedPerks.PARALYZING)||progress.equippedWeaponSkill()==1&&progress.advanced.has(AdvancedPerks.SWEEP)?.15:0))));actionText="Сильный удар! −"+damage+" здоровья врагу.";}else actionText="Сильный удар проходит мимо.";break;
            case "MAGIC":if(magicka<magicCost()){battleLog="Недостаточно магии. Нужны "+magicCost()+" единиц.";return;}magicka-=magicCost();damage=(int)Math.round((18+level*4+random.nextInt(8))*(1+.05*progress.branches.rank(13)+(progress.advanced.has(AdvancedPerks.DUAL_CAST)?.10:0)));actionText="Магическая вспышка обжигает врага: −"+damage+".";break;
            case "CANON_SHOUT":if(!"CANONICAL".equals(battleKind)||canonicalShouts==0||canonicalShoutCooldown>0)return;
                damage=24+level*3+((canonicalShouts&4)!=0?12:0);defending=true;canonicalShoutCooldown=3;actionText=CanonicalCampaign.combatShout(this)+": −"+damage+" здоровья врагу.";break;
            case "DEFEND": defending=true;stamina=Math.min(maxStamina,nonnegativeAdd(stamina,12));magicka=Math.min(maxMagicka,nonnegativeAdd(magicka,5+(progress.advanced.has(AdvancedPerks.RECOVERY)?3:0)+(progress.advanced.has(AdvancedPerks.REGENERATION)?5:0)));actionText="Ты встаёшь в оборону, восстанавливая силы.";break;
            case "POTION":if(potions==0){battleLog="Лечебных зелий больше нет.";return;}potions--;int restored=Math.min(healingAmount(),maxHp-hp);hp+=restored;restoreRespite();lastHealing=restored;actionText="Ты восстанавливаешь "+restored+" здоровья.";break;
            case "FLEE":if(battleKind.equals("WILD")&&rollCheck(progress.chance(this,"FLEE"))){mode="RESULT";poisonTurns=frostTurns=0;message="Ты уходишь от противника. Трофеев нет.";return;}actionText="Попытка отступить не удалась.";break;
            default:return;
        }
        if(damage>0&&("ATTACK".equals(action)||"HEAVY".equals(action))&&progress.equippedWeaponSkill()==2&&progress.advanced.has(AdvancedPerks.POWER_SHOT)){damage=(int)Math.round(damage*1.10);actionText="Мощный выстрел: −"+damage+" здоровья врагу.";}
        if("ATTACK".equals(action)&&damage>0&&random.nextInt(100)<criticalChance()){
            lastCritical=true;damage=Math.max(1,(int)Math.round(damage*1.5));actionText="Критический удар! −"+damage+" здоровья врагу.";
        }
        if(damage>0&&("ATTACK".equals(action)||"HEAVY".equals(action)||"MAGIC".equals(action))){int ambush=fixShop.gifts.ambush(damage);if(ambush>0){damage=nonnegativeAdd(damage,ambush);actionText+="\nЗасада ночного клинка: +"+ambush+" урона.";}}
        if(defending&&"DEFEND".equals(action)&&stamina<maxStamina&&fixShop.gifts.useBattle(FixShopGifts.RESOLVE)){stamina=Math.min(maxStamina,nonnegativeAdd(stamina,Math.max(1,maxStamina/10)));actionText+="\nПечать решимости восстанавливает выносливость.";}
        compensation.tick();
        if(!"CANON_SHOUT".equals(action)&&canonicalShoutCooldown>0)canonicalShoutCooldown--;
        String kathSupply=katherine.companion.useSupplies(katherine);if(!kathSupply.isEmpty())actionText+="\n"+kathSupply;
        int kathBurn=katherine.active()?katherine.companion.ongoingDamage(Math.max(0,enemyHp-damage)):0;
        if(kathBurn>0){damage+=kathBurn;actionText+="\nКэтрин: остаточное пламя, −"+kathBurn+" здоровья врагу.";}
        int kathDamage=katherine.assist(this,battleTurns+1,Math.max(0,enemyHp-damage));
        if(kathDamage>0){damage+=kathDamage;actionText+="\nКэтрин: "+katherine.companion.lastSpell+", −"+kathDamage+" здоровья врагу.";}
        if(damage>0&&("ATTACK".equals(action)||"HEAVY".equals(action))){String weapon=progress.equippedName("weapon");int bonus=0;switch(weapon){
            case "Эбонитовый клинок":case "Клинок Горя":int drain=Math.min(10,Math.min(Math.max(0,maxHp-hp),Math.max(0,enemyHp)));hp+=drain;lastHealing+=drain;bonus=10;actionText+="\n"+weapon+": поглощение здоровья +"+drain+".";break;
            case "Молот Волендранг":int restoredStamina=Math.min(50,Math.max(0,maxStamina-stamina));stamina+=restoredStamina;actionText+="\nВолендранг: запас сил +"+restoredStamina+".";break;
            case "Булава Молаг Бала":artifactWeakness=8;progress.artifacts.trapTurns=Math.max(3,progress.artifacts.trapTurns);actionText+="\nБулава Молаг Бала ослабляет ответ врага и накладывает захват души.";break;
            case "Сияние Рассвета":bonus=RoadArtifacts.undead(enemyName)?20:10;actionText+="\nСияние Рассвета: огненный урон +"+bonus+".";break;
            case "Лук Ауриэля":bonus=RoadArtifacts.undead(enemyName)?30:10;actionText+="\nСолнечный урон +"+bonus+".";break;
            case "Вутрад":if(RoadArtifacts.elf(enemyName)){bonus=Math.max(1,damage/2);actionText+="\nВутрад: дополнительный урон эльфам +"+bonus+".";}break;
        }damage=nonnegativeAdd(damage,bonus);}
        if(damage>0&&("ATTACK".equals(action)||"HEAVY".equals(action))&&progress.equipped("Бритва Мерунеса")&&"WILD".equals(battleKind)&&!enemyName.toLowerCase(java.util.Locale.ROOT).contains("карстааг")&&random.nextInt(10000)<198){damage=enemyHp;actionText+="\nБритва Мерунеса срабатывает: противник падает замертво.";}
        if(progress.equipped("Эбонитовая кольчуга")&&enemyHp>damage){int aura=Math.min(5,enemyHp-damage);damage+=aura;actionText+="\nЯдовитая аура Эбонитовой кольчуги: −"+aura+" здоровья врагу.";}
        if("ACTIVE".equals(progress.questOutcome("road_boethiah_hireling"))&&enemyHp>damage){int assist=Math.min(3,enemyHp-damage);damage+=assist;actionText+="\nНаёмник из Виндхельма помогает в бою: −"+assist+" здоровья врагу.";}
        if(road.hasDog(this)&&enemyHp>damage&&(battleTurns+1)%3==0){damage+=3;actionText+="\nБродячий пёс помогает в бою: −3 здоровья врагу.";}
        int petDamage=fixShop.world.petDamage(battleTurns+1,Math.max(0,enemyHp-damage));if(petDamage>0){damage+=petDamage;actionText+="\n"+fixShop.world.petName()+": −"+petDamage+" здоровья врагу.";}
        int customDamage=fixShop.world.companionDamage(battleTurns+1,Math.max(0,enemyHp-damage));if(customDamage>0){damage+=customDamage;actionText+="\n"+fixShop.world.companionName()+": −"+customDamage+" здоровья врагу.";}
        int itachiOngoing=itachi.ongoingDamage(Math.max(0,enemyHp-damage));if(itachiOngoing>0){damage+=itachiOngoing;actionText+="\nИтачи: остаточное пламя, клоны или отражение, −"+itachiOngoing+" здоровья врагу.";}
        boolean itachiWild="WILD".equals(battleKind)&&!road.groupActive(this)&&!enemyName.toLowerCase(java.util.Locale.ROOT).contains("карстааг")&&!enemyName.toLowerCase(java.util.Locale.ROOT).contains("дракон");
        int itachiDamage=itachi.assist(battleTurns+1,Math.max(0,enemyHp-damage),itachiWild);if(itachiDamage>0){damage+=itachiDamage;actionText+="\nИтачи: "+itachi.lastPower+", −"+itachiDamage+" здоровья врагу.";} 
        int liviaDamage=livia.assist(battleTurns+1,Math.max(0,enemyHp-damage));if(liviaDamage>0){damage+=liviaDamage;actionText+="\nЛивия: удар мечом, −"+liviaDamage+" здоровья врагу.";}
        int almaDamage=alma.assist(this,battleTurns+1,Math.max(0,enemyHp-damage));if(almaDamage>0){damage+=almaDamage;actionText+="\nАльма: "+alma.powerLabel()+", −"+almaDamage+" здоровья врагу.";}
        int almaCombo=alma.combo(Math.max(0,enemyHp-damage),"MAGIC".equals(action));if(almaCombo>0){damage+=almaCombo;actionText+="\nАльма: Кровавая связка, −"+almaCombo+" здоровья врагу.";}
        if(roadSummonTurns>0){int summon=Math.min(Math.max(0,enemyHp-damage),roadSummonKind==1?10+level/2:7+level/3);if(summon>0){damage+=summon;actionText+="\n"+(roadSummonKind==1?"Дремора":roadSummonKind==3?"Поднятый мертвец":"Двемерский автоматон")+" наносит "+summon+" урона.";}if(--roadSummonTurns==0){roadSummonKind=0;actionText+="\nПризыв рассеивается.";}}
        int roadAssist=road.allyDamage(this,damage);if(roadAssist>0){damage+=roadAssist;actionText+="\nСоратники атакуют великана: −"+roadAssist+" здоровья.";}
        if("ATTACK".equals(action)||"HEAVY".equals(action)){int heal=fixShop.gifts.drain(Math.min(enemyHp,damage),maxHp-hp);if(heal>0){hp+=heal;lastHealing+=heal;actionText+="\nПохититель жизненной силы: +"+heal+" здоровья.";}}
        lastDealt=Math.min(enemyHp,damage);lastHeavyHit="HEAVY".equals(action)&&lastDealt>0;lastMiss="HEAVY".equals(action)&&lastDealt==0;battleDealt+=lastDealt;battleTurns++;
        enemyHp=Math.max(0,enemyHp-damage);road.syncGroup(this);
        boolean defeatedTarget=false;
        if(enemyHp==0) {road.recordRoadKill(this);if(!("CANONICAL".equals(battleKind)&&(canonicalStage==22||canonicalStage==27||canonicalStage==30)))actionText+=progress.artifacts.capture(this,enemyName);poisonTurns=frostTurns=0;if(road.nextOpponent(this,actionText)){actionText=battleLog;defeatedTarget=true;itachi.nextTarget();}else{winBattle(actionText);return;}}
        int baseIncoming=enemyDamage+random.nextInt(8)-Math.max(0,armor-(armorUpgraded?4:0))-(defeatedTarget?0:artifactWeakness);
        baseIncoming=Math.max(1,baseIncoming-compensation.passiveBonus(this,1));
        int rawIncoming=Math.max(1,baseIncoming-(defending?enemyDamage/2+3+2*progress.branches.rank(3)+(progress.advanced.has(AdvancedPerks.QUICK_REFLEXES)?2:0)+(progress.advanced.has(AdvancedPerks.SHIELD_WALL)&&!"Не надето".equals(progress.equippedName("shield"))?3:0)+(progress.advanced.has(AdvancedPerks.SHIELD_CHARGE)&&!"Не надето".equals(progress.equippedName("shield"))?4:0):0));
        int incoming=Math.max(1,(int)Math.round(rawIncoming*(1-progress.damageReduction())));
        if(road.magicalEnemy(enemyName))incoming=progress.artifacts.magicAdjust(progress,incoming);
        int flank=road.extraIncoming(this,defending,random);incoming+=flank;if(flank>0)actionText+="\nОстальные противники атакуют одновременно: −"+flank+" здоровья до защиты спутников.";
        if(defending)lastBlocked=Math.max(0,Math.max(1,(int)Math.round(Math.max(1,baseIncoming)*(1-progress.damageReduction())))-(incoming-flank));
        if(defending&&progress.equipped("Разрушитель Заклинаний")&&road.magicalEnemy(enemyName)){int ward=Math.min(10,incoming);incoming-=ward;lastBlocked+=ward;actionText+="\nРазрушитель Заклинаний: оберег поглощает "+ward+" магического урона.";}
        if(defending&&progress.equipped("Этериевый щит")&&battleTurns%3==0){int phase=Math.max(0,incoming-flank);incoming-=phase;lastBlocked+=phase;actionText+="\nЭтериевый щит: текущий враг становится бесплотным и пропускает ответную атаку.";}
        if(shadow){lastBlocked+=incoming;incoming=0;actionText+="\nОтветная атака не достигает скрытого героя.";}
        if(progress.artifacts.trapTurns>0)progress.artifacts.trapTurns--;
        int kathWeakness=katherine.active()?katherine.companion.weaken(incoming):0;incoming-=kathWeakness;
        if(kathWeakness>0){lastBlocked+=kathWeakness;actionText+="\nКэтрин: ослабление противника, −"+kathWeakness+" входящего урона.";}
        int kathProtection=katherine.protect(incoming);incoming-=kathProtection;
        if(kathProtection>0){lastBlocked+=kathProtection;actionText+="\nКэтрин: защитный барьер, −"+kathProtection+" входящего урона.";}
        int itachiProtection=itachi.protect(incoming,itachiWild&&!defeatedTarget);incoming-=itachiProtection;if(itachiProtection>0){lastBlocked+=itachiProtection;actionText+="\nИтачи: "+itachi.lastDefense+", −"+itachiProtection+" входящего урона.";}
        int liviaProtection=livia.protect(incoming);incoming-=liviaProtection;if(liviaProtection>0){lastBlocked+=liviaProtection;actionText+="\nЛивия: прикрывает щитом, −"+liviaProtection+" входящего урона.";}
        int almaWeakness=alma.weaken(incoming);incoming-=almaWeakness;if(almaWeakness>0){lastBlocked+=almaWeakness;actionText+="\nАльма: Алая тень / Метка смерти, −"+almaWeakness+" входящего урона.";}
        int almaProtection=alma.protect(incoming);incoming-=almaProtection;if(almaProtection>0){lastBlocked+=almaProtection;actionText+="\nАльма: защитная магия, −"+almaProtection+" входящего урона.";}
        if(fixShop.gifts.on(FixShopGifts.WARD)){poisonTurns=frostTurns=0;}
        int aegis=fixShop.gifts.absorb(incoming);if(aegis>0){incoming-=aegis;lastBlocked+=aegis;actionText+="\nЭгида Этериуса поглощает "+aegis+" урона.";}
        int poisoned=poisonTurns>0?Math.max(1,maxHp/40):0;
        if(poisonTurns>0)poisonTurns--;if(frostTurns>0){frostTurns--;stamina=Math.max(0,stamina-4);}
        String foe=enemyName.toLowerCase(java.util.Locale.ROOT);
        if(!fixShop.gifts.on(FixShopGifts.WARD)&&!defending&&foe.contains("морозный паук"))poisonTurns=2;
        if(!fixShop.gifts.on(FixShopGifts.WARD)&&!defending&&foe.contains("ледяной призрак"))frostTurns=2;
        lastTaken=Math.min(hp,incoming+poisoned);battleTaken+=lastTaken;
        hp=Math.max(0,hp-incoming-poisoned);stamina=Math.min(maxStamina,nonnegativeAdd(stamina,3+(progress.hasArmorType(5)&&progress.advanced.has(AdvancedPerks.WIND_WALKER)?2:0)));
        battleLog=actionText+"\n"+(road.groupActive(this)?"Отряд противников":enemyName)+" отвечает: −"+incoming+" здоровья."+(poisoned>0?"\nЯд: −"+poisoned+" здоровья.":"")+(poisonTurns>0?"\nОтравление: "+poisonTurns+" хода.":"")+(frostTurns>0?"\nОбморожение: "+frostTurns+" хода.":"");
        if(hp>0){int link=alma.bloodLink(this);if(link>0){lastHealing+=link;battleLog+="\nАльма: Связь душ, +"+link+" здоровья." ;}}
        if(hp==0&&alma.rescue(this))battleLog+="\nАльма: Алая клятва спасла героя.";
        if(hp==0&&fixShop.gifts.rescue(this)){lastHealing+=hp;battleLog+="\nКлятва Феникса возвращает героя к жизни.";}
        if(hp==0) {
            int penalty=gold>0&&fixShop.gifts.insure(day)?0:gold/3;gold-=penalty;hp=Math.max(1,maxHp/2);stamina=Math.max(1,maxStamina/2);
            poisonTurns=frostTurns=0;mode="DEFEAT";message="Ты приходишь в себя у дороги. Прохожие спасли тебя, но потеряно "+penalty+" септимов. Теперь можно подготовиться и вернуться.";livia.defeatedInDuel(this);
        }else regenerateCombat();
    }
    private void winBattle(String lastHit) {
        int reward=25+random.nextInt(26)+level*4;int gain=30+level*9;
        boolean nonlethal="CANONICAL".equals(battleKind)&&(canonicalStage==22||canonicalStage==27||canonicalStage==30);
        if(nonlethal)reward=0;else reward+=fixShop.gifts.loot(reward);
        gold=nonnegativeAdd(gold,reward);victories++;if(!nonlethal&&"WILD".equals(battleKind)&&sideQuest==1&&sideKills<3)sideKills++;
        if("QUEST".equals(battleKind)) {questStage=3;reward+=50;gold=nonnegativeAdd(gold,50);gain+=65;}
        if("DRAGON".equals(battleKind)&&progress.dragonStage==3){
            progress.dragonStage=4;
            reward+=85;gold=nonnegativeAdd(gold,85);gain+=90;
            progress.dragonRecent="У сторожевой башни повержен дракон. "+
                "Над полем прозвучал голос, которому откликнулись горы. Следующая цель: Айварстед.";
        }
        addXp(gain);mode="RESULT";
        katherine.victory(this);alma.victory(this);livia.victory(this);itachi.victory();
        message=lastHit+"\n\nПобеда над «"+enemyName+"»! +"+reward+" септимов, +"+gain+" опыта.";
        if("QUEST".equals(battleKind))message+="\n\nЗа стражем ты находишь журнал хранителя. Следующая цель: Солитьюд.";
        if("DRAGON".equals(battleKind))message+="\n\n"+progress.dragonRecent;
        if(sideQuest==1)message+="\nЗаказ доски: "+sideKills+"/3.";
        if(battleKind.startsWith("FIX_QUEST:"))FixShopAdventure.victory(this,battleKind);
        if(battleKind.startsWith("ROAD_QUEST:"))RoadQuests.victory(this,battleKind);
        if(battleKind.startsWith(SolitudeWolf.KIND))SolitudeWolf.victory(this);
        if("WILD".equals(battleKind)&&road.pending())road.applyVictory(this);
        if("FACTION".equals(battleKind)){factionStories.victory(factionBattle);factionBattle=-1;message+="\n\nОткрой сюжет фракции, чтобы завершить главу.";}
        if("CANONICAL".equals(battleKind)){
            if(canonicalStage==27)message="Ловушка сомкнулась. Одавинг жив и готов к разговору.";
            if(canonicalStage==30)message="Тсун признаёт твоё мастерство и пропускает к Залу доблести.";
            if(canonicalStage==22)message="Алдуин отступает. Он ещё жив.";
            CanonicalCampaign.victory(this);
        }
    }
    public static int experienceRequired(int level){return Math.max(1,Math.min(100,level))*100;}
    public int experienceRequired(){return experienceRequired(level);}
    public static int nonnegativeAdd(int value,int amount){return (int)Math.max(0,Math.min(Integer.MAX_VALUE,(long)value+amount));}
    public void addXp(int amount) {
        if(amount<=0)return;xp=nonnegativeAdd(xp,amount);
        while(level<100 && xp>=experienceRequired()){xp-=experienceRequired();level++;maxHp=nonnegativeAdd(maxHp,10);maxStamina=nonnegativeAdd(maxStamina,10);maxMagicka=nonnegativeAdd(maxMagicka,10);progress.perkPoints=nonnegativeAdd(progress.perkPoints,1);hp=maxHp;stamina=maxStamina;magicka=maxMagicka;}
    }
    /** Time-limited rest available outside combat, tracked in the old save schema. */
    public boolean fixTeleport(String name){
        return fixShop.editorEnabled&&fixTravelToVisited(name);
    }
    boolean fixTravelToVisited(String name){
        if(!started||road.pending()||!"WORLD".equals(mode)&&!"RESULT".equals(mode)||!visitedVisual.contains(name)||name.equals(progress.location))return false;
        WorldAtlas.Place target=WorldAtlas.find(name);if(target==null)return false;region=target.region;progress.location=target.name;katherine.observeTravel(this);alma.observeTravel(this);mode="WORLD";message="Магический переход: "+target.name;recordVisualEvent(message);return true;
    }
    public boolean waitHours(int hours){return restHours(hours,hours>=6);}
    public boolean restHours(int hours,boolean sleep){if(solitude.custody())return false;return restHoursInternal(hours,sleep);}
    boolean restPrisonDay(){return solitude.confined()&&SolitudeJustice.CELL.equals(mode)&&restHoursInternal(24,true);}
    private boolean restHoursInternal(int hours,boolean sleep){
        if(!started||"COMBAT".equals(mode)||road.pending()||hours<1||hours>24)return false;
        lastCheckChance=lastCheckRoll=-1;
        int total=hour+hours;
        if(day>Integer.MAX_VALUE-total/24)return false;
        day+=total/24;hour=total%24;
        syncLivingSystems();needs.recover(hours,sleep);
        katherine.rest(hours);katherine.autonomous(this);alma.rest(hours);itachi.rest(hours);livia.rest(hours);alma.observeTravel(this);
        hp=(int)Math.min(maxHp,(long)hp+hours*(3L+regenBonus[0]));
        stamina=(int)Math.min(maxStamina,(long)stamina+hours*(7L+regenBonus[2]));
        magicka=(int)Math.min(maxMagicka,(long)magicka+hours*(6L+regenBonus[1]));
        message=(sleep?"Сон":"Отдых")+" длился "+hours+" ч. Наступил день "+day+", "+
            String.format(java.util.Locale.ROOT,"%02d:00",hour)+".";
        progress.recent=message;
        progress.sync(this);
        return true;
    }
    public static boolean needItem(ItemCatalog.Item item,boolean water){if(item==null)return false;String cat=item.category.toLowerCase(java.util.Locale.ROOT);boolean drink=cat.contains("напит")||cat.contains("вода");return water?drink:cat.equals("еда")||cat.contains("пища");}
    public boolean consumeNeed(String id,boolean water){if(!started||"COMBAT".equals(mode)||!needItem(ItemCatalog.find(id),water))return false;syncLivingSystems();if(!needs.enabled(water?LivingNeeds.WATER:LivingNeeds.FOOD)||(water?needs.water:needs.food)>=100)return false;if(!progress.removeItem(id,1))return false;lastCheckChance=lastCheckRoll=-1;if(water)needs.drink();else needs.eat();return true;}
    public int healingAmount(){return 45+5*progress.branches.rank(14)+(progress.advanced.has(AdvancedPerks.PHYSICIAN)?10:0)+(progress.advanced.has(AdvancedPerks.BENEFACTOR)?10:0)+(progress.advanced.has(AdvancedPerks.PURE_MIXTURE)?10:0)+(progress.advanced.has(AdvancedPerks.REGENERATION)?10:0);}
    private void restoreRespite(){if(progress.advanced.has(AdvancedPerks.RESPITE))stamina=Math.min(maxStamina,nonnegativeAdd(stamina,10));}
    public int staminaPotionAmount(){return 50+(progress.advanced.has(AdvancedPerks.PHYSICIAN)?10:0)+(progress.advanced.has(AdvancedPerks.BENEFACTOR)?10:0)+(progress.advanced.has(AdvancedPerks.PURE_MIXTURE)?10:0);}
    public boolean drinkHealing() {if(!started||"COMBAT".equals(mode)||potions<=0||hp>=maxHp)return false;potions--;hp=Math.min(maxHp,nonnegativeAdd(hp,healingAmount()));restoreRespite();progress.sync(this);return true;}
    public boolean drinkStamina(){if(!started||"COMBAT".equals(mode)||stamina>=maxStamina||!progress.removeItem("Зелье запаса сил",1))return false;stamina=Math.min(maxStamina,nonnegativeAdd(stamina,staminaPotionAmount()));progress.sync(this);return true;}
    public boolean drinkMana() {if(!started||"COMBAT".equals(mode)||manaPotions<=0||magicka>=maxMagicka)return false;manaPotions--;magicka=Math.min(maxMagicka,nonnegativeAdd(magicka,35));return true;}
    public String questStatus() {switch(questStage){case 0:return "не начато";case 1:return "Фолкрит";case 2:return "Винтерхолд";case 3:return "Солитьюд";default:return "завершено";}}
    public String questDetails() {switch(questStage){case 0:return "Поговори с хранителем в разделе «Главная история».";case 1:return "Доберись до Фолкрита и осмотри алтарь.";case 2:return "Найди башню в Винтерхолде и победи пепельного стража.";case 3:return "Передай журнал хранителям Солитьюда.";default:return "Сюжетная линия завершена. Ты можешь продолжать приключения.";}}
    private static boolean contains(String[] arr,String v){for(String s:arr)if(s.equals(v))return true;return false;}
    public JSONObject toJson() throws JSONException {
        syncLivingSystems();
        JSONObject j=new JSONObject();
        j.put("needs",needs.toJson()).put("fixSpent",fixSpent).put("fixShop",fixShop.toJson()).put("economy",economy.toJson()).put("regenBonus",new org.json.JSONArray(regenBonus)).put("regenIgnorePenalty",regenIgnorePenalty).put("compensation",compensation.toJson()).put("lastLivingResult",lastLivingResult);
        j.put("katherine",katherine.save()).put("solitudeCity",solitude.save());
        j.put("alma",alma.save());j.put("livia",livia.save());j.put("itachi",itachi.save());
        j.put("schema",1).put("started",started).put("name",name).put("race",race).put("heroClass",heroClass)
         .put("gender",gender).put("portrait",portrait)
         .put("region",region).put("day",day).put("hour",hour).put("level",level).put("xp",xp).put("maxHp",maxHp).put("hp",hp)
         .put("maxStamina",maxStamina).put("stamina",stamina).put("maxMagicka",maxMagicka).put("magicka",magicka)
         .put("gold",gold).put("potions",potions).put("manaPotions",manaPotions).put("attack",attack).put("armor",armor)
         .put("canonicalStage",canonicalStage).put("canonicalPhase",canonicalPhase).put("canonicalPage",canonicalPage)
         .put("canonicalShoutCooldown",canonicalShoutCooldown).put("canonicalShouts",canonicalShouts).put("canonicalTreaty",canonicalTreaty).put("canonicalPaarthurnax",canonicalPaarthurnax)
         .put("canonicalPrepared",canonicalPrepared).put("canonicalRecent",canonicalRecent)
         .put("questStage",questStage).put("sideQuest",sideQuest).put("sideKills",sideKills).put("boardCompleted",boardCompleted).put("explored",explored)
         .put("victories",victories).put("reputation",reputation).put("swordUpgraded",swordUpgraded)
         .put("armorUpgraded",armorUpgraded).put("mode",mode).put("message",message).put("enemyName",enemyName)
         .put("battleLog",battleLog).put("battleKind",battleKind).put("enemyIntro",enemyIntro)
         .put("enemyHp",enemyHp).put("enemyMaxHp",enemyMaxHp).put("enemyDamage",enemyDamage).put("progress",progress.toJson())
         .put("aiJournal",aiJournal).put("aiTurn",aiTurn==null?JSONObject.NULL:aiTurn.save())
         .put("road",road.save()).put("sceneState",sceneState.toJson()).put("merchants",merchants.toJson()).put("factions",factions.toJson()).put("factionStories",factionStories.toJson()).put("factionBattle",factionBattle)
         .put("visitedVisual",new org.json.JSONArray(visitedVisual)).put("visualChronicle",visualChronicle)
         .put("holdReputation",new org.json.JSONArray(holdReputation)).put("roadSummonTurns",roadSummonTurns).put("roadSummonKind",roadSummonKind).put("poisonTurns",poisonTurns).put("frostTurns",frostTurns).put("battleTurns",battleTurns).put("battleDealt",battleDealt).put("battleTaken",battleTaken);
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
            g.gender=CharacterIdentity.normalizeGender(j.optString("gender",CharacterIdentity.MALE));
            g.portrait=CharacterIdentity.normalizePortrait(j.optInt("portrait",1));
            g.region=Math.max(0,Math.min(REGIONS.length-1,j.optInt("region",0)));
            g.day=Math.max(1,j.optInt("day",1));g.hour=Math.max(0,Math.min(23,j.optInt("hour",8)));g.level=Math.max(1,Math.min(100,j.optInt("level",1)));g.xp=Math.max(0,j.optInt("xp",0));
            g.maxHp=Math.max(1,j.optInt("maxHp",100));g.hp=Math.max(0,Math.min(g.maxHp,j.optInt("hp",100)));
            g.maxStamina=Math.max(1,j.optInt("maxStamina",65));g.stamina=Math.max(0,Math.min(g.maxStamina,j.optInt("stamina",65)));
            g.maxMagicka=Math.max(1,j.optInt("maxMagicka",40));g.magicka=Math.max(0,Math.min(g.maxMagicka,j.optInt("magicka",40)));
            g.gold=Math.max(0,j.optInt("gold",85));g.potions=Math.max(0,j.optInt("potions",3));g.manaPotions=Math.max(0,j.optInt("manaPotions",1));
            g.attack=Math.max(1,j.optInt("attack",12));g.armor=Math.max(0,j.optInt("armor",2));
            g.canonicalStage=Math.max(0,Math.min(CanonicalCampaign.COUNT+1,j.optInt("canonicalStage",0)));
            g.canonicalPhase=Math.max(0,Math.min(1,j.optInt("canonicalPhase",0)));g.canonicalPage=Math.max(0,Math.min(500,j.optInt("canonicalPage",0)));
            g.canonicalShoutCooldown=Math.max(0,Math.min(3,j.optInt("canonicalShoutCooldown",0)));
            g.canonicalShouts=j.optInt("canonicalShouts",0)&31;g.canonicalTreaty=Math.max(0,Math.min(3,j.optInt("canonicalTreaty",0)));
            g.canonicalPaarthurnax=Math.max(0,Math.min(2,j.optInt("canonicalPaarthurnax",0)));g.canonicalPrepared=j.optBoolean("canonicalPrepared",false);
            g.canonicalRecent=j.optString("canonicalRecent","");if(g.canonicalRecent.length()>8192)g.canonicalRecent=g.canonicalRecent.substring(0,8192);
            g.questStage=Math.max(0,Math.min(4,j.optInt("questStage",0)));g.sideQuest=Math.max(0,Math.min(2,j.optInt("sideQuest",0)));
            g.sideKills=g.sideQuest==1?Math.max(0,Math.min(3,j.optInt("sideKills",0))):0;g.boardCompleted=Math.max(0,j.optInt("boardCompleted",0));g.explored=Math.max(0,j.optInt("explored",0));
            g.victories=Math.max(0,j.optInt("victories",0));g.reputation=j.optInt("reputation",0);
            g.swordUpgraded=j.optBoolean("swordUpgraded",false);g.armorUpgraded=j.optBoolean("armorUpgraded",false);
            g.mode=j.optString("mode","WORLD");g.message=j.optString("message","");
            g.enemyName=j.optString("enemyName","");g.battleLog=j.optString("battleLog","");
            g.battleKind=j.optString("battleKind","");g.enemyIntro=j.optString("enemyIntro","");
            g.enemyHp=Math.max(0,j.optInt("enemyHp",0));g.enemyMaxHp=Math.max(0,j.optInt("enemyMaxHp",0));g.enemyDamage=Math.max(0,j.optInt("enemyDamage",0));
            g.compensation=CompensationPowers.fromJson(j.optJSONObject("compensation"));g.compensation.installCatalog();
            g.progress=RpgProgress.fromJson(j.optJSONObject("progress"));
            g.solitude=SolitudeCity.restore(j.optJSONObject("solitudeCity"));
             g.needs=LivingNeeds.fromJson(j.optJSONObject("needs"));g.economy=LivingEconomy.fromJson(j.optJSONObject("economy"));g.syncLivingSystems();g.fixSpent=Math.max(0,j.optLong("fixSpent",0));g.fixShop=FixShopState.fromJson(j.optJSONObject("fixShop"));g.fixShop.migrateRemoved(g);org.json.JSONArray rates=j.optJSONArray("regenBonus");if(rates!=null)for(int i=0;i<3;i++)g.regenBonus[i]=Math.max(0,Math.min(1000000,rates.optInt(i)));g.regenIgnorePenalty=j.optBoolean("regenIgnorePenalty");g.compensation.reconcile(g);JSONObject result=j.optJSONObject("lastLivingResult");if(result!=null)g.lastLivingResult=result;
            g.merchants=MerchantLedger.fromJson(j.optJSONObject("merchants"));g.factions=FactionCampaign.fromJson(j.optJSONObject("factions"));g.factionStories=FactionStories.fromJson(j.optJSONObject("factionStories"));g.factionBattle=Math.max(-1,Math.min(6,j.optInt("factionBattle",-1)));
            org.json.JSONArray holds=j.optJSONArray("holdReputation");if(holds!=null)for(int i=0;i<g.holdReputation.length;i++)g.holdReputation[i]=Math.max(-100,Math.min(100,holds.optInt(i,0)));
            if("COMBAT".equals(g.mode)){g.poisonTurns=Math.max(0,Math.min(2,j.optInt("poisonTurns")));g.frostTurns=Math.max(0,Math.min(2,j.optInt("frostTurns")));}
            g.roadSummonTurns=Math.max(0,Math.min(3,j.optInt("roadSummonTurns")));g.roadSummonKind=g.roadSummonTurns>0?Math.max(1,Math.min(3,j.optInt("roadSummonKind"))):0;g.battleTurns=Math.max(0,j.optInt("battleTurns"));g.battleDealt=Math.max(0,j.optInt("battleDealt"));g.battleTaken=Math.max(0,j.optInt("battleTaken"));
            org.json.JSONArray visited=j.optJSONArray("visitedVisual");
            if(visited!=null)for(int i=0;i<Math.min(visited.length(),2000);i++){String v=visited.optString(i);if(!v.isEmpty())g.visitedVisual.add(v);}
            if(g.started)g.visitedVisual.add(g.progress.location);
            org.json.JSONArray history=j.optJSONArray("visualChronicle");
            if(history!=null)for(int i=Math.max(0,history.length()-80);i<history.length();i++){org.json.JSONObject entry=history.optJSONObject(i);if(entry!=null)g.visualChronicle.put(entry);}
            g.aiJournal=j.optString("aiJournal","");
            g.aiTurn=AiGameTurn.restore(j.optJSONObject("aiTurn"));
            g.sceneState=SceneState.fromJson(j.optJSONObject("sceneState"));
            g.road=RoadEvents.restore(j.optJSONObject("road"));if(RoadEvents.MODE.equals(g.mode)&&!g.road.pending())g.mode="WORLD";
             g.katherine=KatherineStory.restore(j.optJSONObject("katherine"));
             g.itachi=ItachiStory.restore(j.optJSONObject("itachi"));if(!"COMBAT".equals(g.mode))g.itachi.beginBattle();if(ItachiStory.MODE.equals(g.mode)&&!g.itachi.present(g))g.mode="WORLD";
             g.livia=LiviaStory.restore(j.optJSONObject("livia"));if(LiviaStory.MODE.equals(g.mode)&&!g.livia.present(g))g.mode="WORLD";
             g.alma=AlmaStory.restore(j.optJSONObject("alma"));if(!g.alma.enabled&&AlmaStory.MODE.equals(g.mode))g.mode="WORLD";
            // Reconcile legacy schema-1 saves with the authoritative quest stage.
            if(g.questStage>=4)g.progress.setQuestOutcome("ash_seals","COMPLETED");
            else if(g.questStage>0)g.progress.setQuestOutcome("ash_seals","ACTIVE");
            // Rebuild missing continuation ledger entries from authoritative schema-1 stage.
            // Never overwrite terminal outcomes already stored in newer saves.
            if(g.progress.dragonStage>=6){
                for(int chapter=0;chapter<5;chapter++){
                    String questId="dragon_ash_"+chapter;
                    if(chapter<g.progress.dragonNextStage)
                        g.progress.setQuestOutcome(questId,"COMPLETED");
                    else if(chapter==g.progress.dragonNextStage)
                        g.progress.setQuestOutcome(questId,"ACTIVE");
                }
            }
            if(j.optJSONObject("progress")==null)g.progress.location=REGIONS[g.region];
            if(SolitudeCity.isMode(g.mode)&&(!SolitudeCity.here(g)||!SolitudeCity.validMode(g.mode)))g.mode="WORLD";
            g.solitude.reconcilePrison(g);
            g.progress.sync(g);
            if(g.mode.equals("COMBAT")&&g.enemyHp<=0)g.mode=g.road.pending()?"RESULT":"WORLD";
        }catch(Exception ignore){return new GameEngine();}
        return g;
    }
}
