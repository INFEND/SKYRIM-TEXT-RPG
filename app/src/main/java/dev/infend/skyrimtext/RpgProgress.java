package dev.infend.skyrimtext;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public final class RpgProgress {
    public final JSONObject inventory = new JSONObject();
    public final ArrayList<String> completed = new ArrayList<>();
    public final ArrayList<String> failed = new ArrayList<>();
    // Quest outcomes are separate from repeatable skill-check history.
    public final LinkedHashMap<String,String> questOutcomes = new LinkedHashMap<>();
    public void setQuestOutcome(String id,String state){
        if(id==null||!id.matches("[a-z0-9_]{1,48}"))return;
        if(!("ACTIVE".equals(state)||"COMPLETED".equals(state)||"FAILED".equals(state)))return;
        String old=questOutcomes.get(id);
        if("COMPLETED".equals(old)||"FAILED".equals(old))return; // terminal outcomes cannot be farmed
        questOutcomes.put(id,state);
    }
    public String questOutcome(String id){return questOutcomes.get(id);}

    public final int[] skillLevels = new int[RpgContent.SKILLS.length];
    public final int[] skillXp = new int[RpgContent.SKILLS.length];
    private final int[] combatPerks=new int[6];
    private final int[] studyPerks=new int[RpgContent.SKILLS.length];
    TalentBranches branches=new TalentBranches();
    AdvancedPerks advanced=new AdvancedPerks();
    public int talentRank(int skill){return skill<0||skill>=studyPerks.length?0:isCombatPerk(skill)?combatPerkRank(skill):studyPerks[skill];}
    public int talentNeed(int skill,int rank){return isCombatPerk(skill)?Math.max(0,(rank-1)*20):rank*20;}
    public boolean canBuyTalent(int skill){int rank=talentRank(skill);return skill>=0&&skill<studyPerks.length&&rank<5&&perkPoints>0&&skillLevel(skill)>=talentNeed(skill,rank+1);}
    public boolean buyTalent(GameEngine g,int skill){if(!canBuyTalent(skill))return false;if(isCombatPerk(skill))return buyCombatPerk(skill);studyPerks[skill]++;perkPoints--;skillExperience(g,skill,70);return true;}
    private final LinkedHashMap<String,String> equipped=new LinkedHashMap<>();
    private boolean equipmentInitialized;
    // One improved exemplar per item ID until the inventory supports individual instances.
    private final LinkedHashMap<String,Integer> tempered=new LinkedHashMap<>();
    private static final String[] QUALITY={"Обычное","Хорошее","Отличное","Превосходное","Безупречное","Эпическое","Легендарное"};
    private static final int[] TEMPER_LEVELS={0,14,31,65,100,134,168};
    public int temperQuality(String id){return quantity(id)>0?tempered.getOrDefault(id,0):0;}
    public String temperQualityName(String id){return QUALITY[temperQuality(id)];}
    public int smithingQuality(){
        int tier=0;for(int i=1;i<TEMPER_LEVELS.length;i++)if(skillLevel(6)>=TEMPER_LEVELS[i])tier=i;
        return tier;
    }
    public String smithingQualityName(){return QUALITY[smithingQuality()];}
    public String temperTargetName(ItemCatalog.Item item){return QUALITY[temperTarget(item)];}
    private int temperTarget(ItemCatalog.Item item){int tier=smithingQuality();return advanced.has(AdvancedPerks.STEEL)&&item.id.toLowerCase(java.util.Locale.ROOT).startsWith("стальн")?Math.min(QUALITY.length-1,tier+1):tier;}
    public double temperBonus(ItemCatalog.Item item){
        int tier=temperQuality(item.id);
        if(tier==0)return 0;
        double bonus=tier==1?1:1+(tier-1)*1.8;
        return "body".equals(item.slot())?2*bonus:bonus;
    }
    public boolean atForge(){
        String[] towns={"Вайтран","Ривервуд","Виндхельм","Маркарт","Рифтен","Солитьюд","Данстар","Фолкрит","Морфал","Воронья Скала","Деревня скаалов","Драконий Мост","Шорский Камень","Картвастен"};
        for(String town:towns)if(town.equalsIgnoreCase(location))return true;
        return false;
    }
    public String temperBlock(ItemCatalog.Item item,GameEngine g){
        String material=item.temperMaterial();
        if(material.isEmpty())return "Улучшение этого предмета пока недоступно";
        if(quantity(item.id)!=1)return "Для улучшения нужен одиночный экземпляр";
        if("COMBAT".equals(g.mode)||!atForge())return "Нужна кузница в поселении, вне боя";
        if(temperTarget(item)<=temperQuality(item.id))return "Нужен более высокий навык кузнечного дела";
        if(quantity(material)<1)return "Не хватает материала: "+material;
        return "";
    }
    public boolean temper(ItemCatalog.Item item,GameEngine g){
        if(!temperBlock(item,g).isEmpty())return false;
        int next=temperTarget(item);
        if(!removeItem(item.temperMaterial(),1))return false;
        tempered.put(item.id,next);
        skillExperience(g,6,Math.max(1,30+next*15));
        return true;
    }

    private static final String[] SLOTS={"weapon","shield","head","body","hands","feet","ring","neck","ammo"};
    public static boolean isCombatPerk(int skill){return skill==0||skill==1||skill==2||skill==4||skill==5;}
    public int combatPerkRank(int skill){return isCombatPerk(skill)?combatPerks[skill]:0;}
    public int nextCombatPerkLevel(int skill){
        int rank=combatPerkRank(skill);
        if(rank>=5)return 101;
        int[] levels=(skill==2||skill==4||skill==5)?new int[]{0,20,40,60,80}:new int[]{0,20,40,60,80};
        return levels[rank];
    }
    public String combatPerkName(int skill){
        switch(skill){case 0:return "Сильная рука";case 1:return "Варвар";case 2:return "Избыточное натяжение";
            case 4:return "Джаггернаут";case 5:return "Ловкость в обороне";default:return "";}
    }
    public boolean buyCombatPerk(int skill){
        if(!isCombatPerk(skill)||perkPoints<1||combatPerkRank(skill)>=5||skillLevel(skill)<nextCombatPerkLevel(skill))return false;
        combatPerks[skill]++;perkPoints--;return true;
    }
    private void cleanEquipment(){
        for(String slot:SLOTS){
            String id=equipped.get(slot);
            if(id==null)continue;
            ItemCatalog.Item item=ItemCatalog.find(id);
            if(item==null||quantity(id)<1||!slot.equals(item.slot()))equipped.remove(slot);
        }
        ItemCatalog.Item weapon=ItemCatalog.find(equipped.get("weapon"));
        if(weapon!=null&&weapon.twoHanded())equipped.remove("shield");
    }
    public boolean equipped(String id){return id!=null&&equipped.containsValue(id)&&quantity(id)>0;}
    public boolean equip(String id){
        ItemCatalog.Item item=ItemCatalog.find(id);
        if(item==null||item.slot().isEmpty()||quantity(id)<1)return false;
        if("shield".equals(item.slot())){
            ItemCatalog.Item weapon=ItemCatalog.find(equipped.get("weapon"));
            if(weapon!=null&&weapon.twoHanded())return false;
        }
        equipmentInitialized=true;
        equipped.put(item.slot(),id);cleanEquipment();return true;
    }
    public void unequip(String id){if(id==null)return;equipmentInitialized=true;equipped.values().removeIf(id::equals);}
    public String equippedName(String slot){String id=equipped.get(slot);return id==null||quantity(id)<1?"Не надето":id;}
    public double equippedDamage(){
        ItemCatalog.Item item=ItemCatalog.find(equipped.get("weapon"));
        return item!=null&&quantity(item.id)>0&&item.baseStat>=0?item.currentStat(this):0;
    }
    public int equippedWeaponSkill(){
        ItemCatalog.Item item=ItemCatalog.find(equipped.get("weapon"));
        return item!=null&&quantity(item.id)>0&&item.statSkill>=0&&item.statSkill<=2?item.statSkill:0;
    }
    public double equippedArmor(){
        double total=0;
        for(String slot:SLOTS){
            ItemCatalog.Item item=ItemCatalog.find(equipped.get(slot));
            if(item!=null&&quantity(item.id)>0&&item.wearable()&&item.baseStat>0)total+=item.currentStat(this);
        }
        return total;
    }
    public int armorPieces(){
        int count=0;
        for(String slot:SLOTS){ItemCatalog.Item item=ItemCatalog.find(equipped.get(slot));
            if(item!=null&&quantity(item.id)>0&&item.wearable()&&item.baseStat>0)count++;}
        return count;
    }
    public double damageReduction(){
        boolean heavy=false,light=false;for(String slot:SLOTS){ItemCatalog.Item item=ItemCatalog.find(equipped.get(slot));if(item!=null&&item.wearable()&&item.baseStat>0){heavy|=item.statSkill==4;light|=item.statSkill==5;}}
        double base=Math.min(0.8,(equippedArmor()+25*armorPieces())*0.0012);
        int heavyPieces=0,lightPieces=0;
        for(String slot:new String[]{"head","body","hands","feet"}){ItemCatalog.Item item=ItemCatalog.find(equipped.get(slot));if(item!=null&&item.baseStat>0){if(item.statSkill==4)heavyPieces++;if(item.statSkill==5)lightPieces++;}}
        double fit=heavyPieces==4&&advanced.has(AdvancedPerks.WELL_FITTED)?.05:lightPieces==4&&advanced.has(AdvancedPerks.CUSTOM_FIT)?.05:0;
        return Math.min(0.8,base+(heavy?.02*branches.rank(4):0)+(light?.02*branches.rank(5):0)+fit);
    }
    public void shopEquip(String id){equip(id);}

    public final int[] spent = new int[3];
    public String startId="Хелген", location="Хелген";
    public String recent="Новая история начинается.";
    public int perkPoints=0;
    public int activePowerDay=-1;
    public int difficulty=0;
    // First dragon story arc is kept inside existing schema-1 save data.
    public int dragonStage=0;
    public int dragonInsight=0;
    public int dragonNextStage=0;
    public String dragonNextRecent="";
    public final HashSet<String> dragonNextUsed=new HashSet<>();
    // Failed risky attempts remain spent, but are not evidence.
    public final HashSet<String> dragonNextFailed=new HashSet<>();
    public String dragonRecent="";
    public final HashSet<String> dragonUsed=new HashSet<>();
    public int actionDay=-1;
    public final HashSet<String> attemptedToday=new HashSet<>();
    public RpgProgress(){Arrays.fill(skillLevels,15);}
    public static RpgProgress newHero(GameEngine g,int start,int[] points){
        RpgProgress p=new RpgProgress();
        String[] data=RpgContent.startParts(start);
        p.startId=data[0];p.location=data[1];
        if(points!=null&&points.length>=3){
            int sum=0;for(int i=0;i<3;i++){p.spent[i]=Math.max(0,Math.min(5,points[i]));sum+=p.spent[i];}
            if(sum>5){Arrays.fill(p.spent,0);p.spent[0]=5;}
        }
        p.quantity("Походный клинок",1);
        p.quantity("Дорожная одежда",1);
        p.quantity("Факел",1);
        p.quantity("Отмычка",5);
        p.sync(g);
        p.recent="Предыстория: "+data[0]+". "+data[3];
        return p;
    }
    public void sync(GameEngine g){
        quantity("Септимы",Math.max(0,g.gold));
        quantity("Лечебное зелье",Math.max(0,g.potions));
        quantity("Зелье магии",Math.max(0,g.manaPotions));
        if(g.swordUpgraded){quantity("Походный клинок",0);quantity("Стальной меч",1);}
        if(g.armorUpgraded){quantity("Дорожная одежда",0);quantity("Стальная кираса",1);}
        if(!equipmentInitialized){
            equipmentInitialized=true;
            equip(g.swordUpgraded?"Стальной меч":"Походный клинок");
            equip(g.armorUpgraded?"Стальная кираса":"Дорожная одежда");
        }
        cleanEquipment();
    }
    public void quantity(String id,int n){
        try{inventory.put(id,Math.max(0,n));if(n<=0)tempered.remove(id);}catch(Exception ignored){}
    }
    public int quantity(String id){return inventory.optInt(id,0);}
    public boolean addItem(String id,int quantity){
        if(quantity<=0||ItemCatalog.find(id)==null)return false;
        ItemCatalog.Item item=ItemCatalog.find(id);
        if(item.weight>0&&carryWeight()+item.weight*quantity>50.00001)return false;
        quantity(id,quantity(id)+quantity);return true;
    }
    public boolean removeItem(String id,int amount){
        if(amount<1||quantity(id)<amount)return false;
        quantity(id,quantity(id)-amount);
        if(quantity(id)==0)unequip(id);
        return true;
    }
    public double carryWeight(){
        double sum=0;Iterator<String> names=inventory.keys();
        while(names.hasNext()){
            String n=names.next();ItemCatalog.Item item=ItemCatalog.find(n);
            if(item!=null)sum+=item.weight*inventory.optInt(n,0);
        }
        return Math.round(sum*100.0)/100.0;
    }
    public ArrayList<ItemCatalog.Item> owned(String group,String sort){
        ArrayList<ItemCatalog.Item> list=new ArrayList<>();
        for(ItemCatalog.Item item:ItemCatalog.sorted(group,sort))
            if(quantity(item.id)>0)list.add(item);
        return list;
    }
    public int skillLevel(int index){return skillLevels[Math.max(0,Math.min(index,skillLevels.length-1))];}
    public int skillNeed(int index){int lvl=skillLevel(index);return Math.max(100,100*lvl);}
    public void skillExperience(GameEngine g,int index,int amount){
        if(index<0||index>=skillLevels.length||amount<=0)return;
        if(skillLevels[index]<100){
            skillXp[index]+=amount+branches.experienceBonus(index,amount);
            while(skillLevels[index]<100&&skillXp[index]>=skillNeed(index)){
                skillXp[index]-=skillNeed(index);skillLevels[index]++;
                if(skillLevels[index]==100)skillXp[index]=0;
            }
        }
        g.addXp(amount);
    }
    public int chance(GameEngine g,int actionIndex,int skillIndex,int baseDifficulty){
        int stat=actionIndex%3;
        int exhaustion=stat==1?(g.maxMagicka-g.magicka):stat==2?(g.maxStamina-g.stamina):Math.max(0,g.maxHp-g.hp);
        int difficulty=baseDifficulty+Math.max(0,15-g.level)*2;
        int danger=Math.max(0,g.region/2)+difficulty/10;
        int skill=skillIndex<0?0:(skillLevel(skillIndex)-15)/2;
        int context=Math.min(18,g.reputation/3)+skill+branches.chanceBonus(skillIndex)+(skillIndex==12&&advanced.has(AdvancedPerks.PERSUASION)?5:0)-((int)carryWeight()>45?8:0);
        return RpgContent.successChance(g,stat,difficulty,danger,exhaustion,context);
    }
    public int chance(GameEngine g,String action){
        if(action==null)return -1;
        if(action.startsWith("CANON:"))return CanonicalCampaign.chance(g,action);
        if(action.startsWith("DRAGON_NEXT:"))return DragonContinuation.chance(g,action);
        if(action.startsWith("DRAGON:"))return DragonArc.chance(g,action);
        if(action.matches("ACT:[0-9]")){
            if(!("WORLD".equals(g.mode)||"TALK".equals(g.mode)||"EVENT".equals(g.mode)||"QUEST".equals(g.mode)))return -1;
            int which=0;try{which=Integer.parseInt(action.substring(4));}catch(Exception ignored){}
            return chance(g,which,skillFor(which),12+(which*5)%34);
        }
        switch(action){
            case "HEAVY":return g.stamina>=g.heavyCost()?chance(g,2,equippedWeaponSkill(),30):-1;
            case "FLEE":return "WILD".equals(g.battleKind)?chance(g,2,9,38):-1;
            case "EVENT_HELP":return chance(g,0,14,15);
            case "EVENT_SEARCH":return chance(g,2,9,25);
            // These actions do not make an independent success roll. Do not advertise a fictitious percentage.
            case "ATTACK":case "MAGIC":case "TALK_HELP":case "TALK_GIVE":
            case "Q_ALTAR":case "Q_BOSS":case "EXPLORE":return -1;
            default:return -1; // Household actions and navigation are deterministic.
        }
    }
    private int skillFor(int index){int[] map={9,12,0,14,10,15,2,13,3,9};return map[Math.floorMod(index,map.length)];}
    private static final String[] ACTIONS={
        "Осмотреть едва заметные следы|Проверить, куда ведут примятые травы и мокрая земля.",
        "Поговорить с прохожим|Выведать новости и скрытые обстоятельства.",
        "Принять выгодную позицию|Отыскать укрытие и подготовиться к опасности.",
        "Почувствовать магическую аномалию|Изучить след магии в камне и воздухе.",
        "Взломать запертую шкатулку|Рискнуть отмычкой ради возможной добычи.",
        "Разобрать древнюю надпись|Понять назначение забытых символов.",
        "Взобраться на возвышение|Осмотреть долину и ближайшие дороги.",
        "Применить боевое заклинание|Изгнать угрозу магией, если найдётся цель.",
        "Отразить внезапный выпад|Применить блокирование и осторожность.",
        "Пробраться незамеченным|Обойти опасный участок в тени."
    };
    public void fillChoices(GameEngine g,GameEngine.Scene s){
        // Context-specific choices only. A generic loot action must never appear in combat,
        // shops, introductions, defeat, or after-event results.
        if(!("WORLD".equals(g.mode)||"TALK".equals(g.mode)||"EVENT".equals(g.mode)||"QUEST".equals(g.mode)))return;
        if(actionDay!=g.day){attemptedToday.clear();actionDay=g.day;}
        HashSet<String> used=new HashSet<>();
        for(GameEngine.Choice c:s.choices)used.add(c.action);
        int i=0;
        while(s.choices.size()<10&&i<30){
            int index=(i+Math.max(0,g.region))%10;i++;
            String[] a=ACTIONS[index].split("\\|",2);
            String action="ACT:"+index;if(used.contains(action)||attemptedToday.contains(action))continue;
            s.choices.add(new GameEngine.Choice(action,a[0],a[1],false));used.add(action);
        }
    }
    public boolean perform(GameEngine g,String action){
        if(action==null||!action.matches("ACT:[0-9]"))return false;
        if(!("WORLD".equals(g.mode)||"TALK".equals(g.mode)||"EVENT".equals(g.mode)||"QUEST".equals(g.mode)))return false;
        if(actionDay!=g.day){attemptedToday.clear();actionDay=g.day;}
        if(attemptedToday.contains(action))return false;
        int index=0;try{index=Integer.parseInt(action.substring(4));}catch(Exception ignored){}
        index=Math.floorMod(index,10);
        String[] a=ACTIONS[index].split("\\|",2);
        int skill=skillFor(index);
        int chance=chance(g,action);
        attemptedToday.add(action);
        boolean success=new Random().nextInt(100)<chance;
        int fatigue=3+(index%4)*2;
        g.stamina=Math.max(0,g.stamina-fatigue);
        if(success){
            // Ordinary repeatable checks are not quests or sources of infinite septims.
            // Their reward is practice, not fabricated loot or a completed quest entry.
            skillExperience(g,skill,14+index*2);
            recent="Успех: "+a[0]+". Навык «"+RpgContent.SKILLS[skill]+"» получает опыт.";
        }else{
            int harm=2+(index%4)*3;
            if(index==2||index==7||index==8){g.hp=Math.max(1,g.hp-harm);}
            recent="Не удалось: "+a[0]+". "+(index==2||index==7||index==8?
                "Риск обернулся ударом и потерей "+harm+" здоровья.":"Время упущено, а следы скрылись в сумерках.");
            // Failed checks belong to the event history, not the failed-quests journal.
        }
        g.message=recent;
        g.mode="RESULT";
        if(completed.size()>80)completed.remove(0);
        if(failed.size()>80)failed.remove(0);
        return true;
    }
    public void observe(GameEngine g,String action,int oldQuest){
        if(action==null)return;
        switch(action){
            case "Q_ACCEPT":setQuestOutcome("ash_seals","ACTIVE");recent="Принято сюжетное поручение: «Пепельные печати».";break;
            case "Q_ALTAR":
                if(g.questStage>oldQuest)completed.add("Исследование древнего алтаря • день "+g.day);
                break;
            case "Q_FINISH":
                if(g.questStage>oldQuest){setQuestOutcome("ash_seals","COMPLETED");completed.add("Пепельные печати • день "+g.day);}
                break;
            case "EXPLORE":skillExperience(g,9,6);break;
            case "TALK_HELP":skillExperience(g,12,6);break;
            case "ATTACK":skillExperience(g,0,8);break;
            case "MAGIC":skillExperience(g,13,8);break;
            case "HEAVY":skillExperience(g,1,8);break;
            case "DEFEND":skillExperience(g,3,8);break;
            case "FLEE":skillExperience(g,9,8);break;
        }
        sync(g);
    }
    public String racePassive(GameEngine g){return RpgContent.PASSIVES[RpgContent.raceIndex(g.race)];}
    public String racePower(GameEngine g){return RpgContent.POWERS[RpgContent.raceIndex(g.race)];}
    public boolean useRacePower(GameEngine g){
        if(activePowerDay==g.day)return false;
        activePowerDay=g.day;
        int index=RpgContent.raceIndex(g.race);
        if(index==1||index==7)g.magicka=Math.min(g.maxMagicka,g.magicka+40);
        else if(index==6)g.stamina=g.maxStamina;
        else if(index==9)g.hp=Math.min(g.maxHp,g.hp+50);
        else if(index==4)g.stamina=Math.min(g.maxStamina,g.stamina+30);
        else if(index==5)g.hp=Math.min(g.maxHp,g.hp+30);
        recent="Расовый талант: "+racePower(g)+" | разовое восстановление ресурсов (один раз в игровой день).";
        return true;
    }
    public JSONObject toJson(){
        JSONObject o=new JSONObject();
        try{
            o.put("startId",startId).put("location",location).put("recent",recent)
             .put("points",new JSONArray(spent)).put("skillLevels",new JSONArray(skillLevels))
             .put("skillXp",new JSONArray(skillXp)).put("inventory",inventory)
             .put("completed",new JSONArray(completed)).put("failed",new JSONArray(failed))
             .put("questOutcomes",new JSONObject(questOutcomes))
             .put("perks",perkPoints).put("combatPerks",new JSONArray(combatPerks)).put("studyPerks",new JSONArray(studyPerks)).put("talentBranches",branches.toJson()).put("advancedPerks",advanced.toJson())
             .put("equipmentInitialized",equipmentInitialized).put("equipped",new JSONObject(equipped))
             .put("tempered",new JSONObject(tempered))
             .put("activePowerDay",activePowerDay).put("difficulty",difficulty)
             .put("actionDay",actionDay).put("attemptedToday",new JSONArray(attemptedToday))
             .put("dragonStage",dragonStage).put("dragonInsight",dragonInsight)
             .put("dragonRecent",dragonRecent).put("dragonUsed",new JSONArray(dragonUsed))
             .put("dragonNextStage",dragonNextStage).put("dragonNextRecent",dragonNextRecent)
             .put("dragonNextUsed",new JSONArray(dragonNextUsed))
             .put("dragonNextFailed",new JSONArray(dragonNextFailed));
        }catch(Exception ignored){}
        return o;
    }
    public static RpgProgress fromJson(JSONObject obj){
        RpgProgress p=new RpgProgress();if(obj==null)return p;
        p.branches=TalentBranches.fromJson(obj.optJSONArray("talentBranches"));p.advanced=AdvancedPerks.fromJson(obj.optJSONArray("advancedPerks"));
        p.startId=obj.optString("startId","Хелген");p.location=obj.optString("location","Хелген");
        p.recent=obj.optString("recent","");p.perkPoints=Math.max(0,obj.optInt("perks",0));
        p.activePowerDay=obj.optInt("activePowerDay",-1);p.difficulty=obj.optInt("difficulty",0);
        p.actionDay=obj.optInt("actionDay",-1);
        p.dragonStage=Math.max(0,Math.min(DragonArc.FINAL_STAGE,obj.optInt("dragonStage",0)));
        p.dragonInsight=Math.max(0,Math.min(15,obj.optInt("dragonInsight",0)));
        p.dragonRecent=obj.optString("dragonRecent","");
        p.dragonNextStage=Math.max(0,Math.min(5,obj.optInt("dragonNextStage",0)));
        p.dragonNextRecent=obj.optString("dragonNextRecent","");
        JSONArray nextActions=obj.optJSONArray("dragonNextUsed");
        if(nextActions!=null)for(int i=0;i<nextActions.length();i++){
            String value=nextActions.optString(i,"");
            if(value.matches("DRAGON_NEXT:[0-4]:(?:[1-9]|10)"))p.dragonNextUsed.add(value);
        }
        JSONArray nextFailed=obj.optJSONArray("dragonNextFailed");
        if(nextFailed!=null)for(int i=0;i<nextFailed.length();i++){
            String value=nextFailed.optString(i,"");
            if(value.matches("DRAGON_NEXT:[0-4]:[357]")&&p.dragonNextUsed.contains(value))p.dragonNextFailed.add(value);
        }
        JSONArray dragonActions=obj.optJSONArray("dragonUsed");
        if(dragonActions!=null)for(int i=0;i<dragonActions.length();i++){
            String value=dragonActions.optString(i,"");
            if(value.matches("DRAGON:[0-5]:[1-8]"))p.dragonUsed.add(value);
        }
        JSONArray attempted=obj.optJSONArray("attemptedToday");
        if(attempted!=null)for(int i=0;i<attempted.length();i++){
            String key=attempted.optString(i,"");
            if(key.matches("ACT:[0-9]"))p.attemptedToday.add(key);
        }
        JSONArray levels=obj.optJSONArray("skillLevels"),xp=obj.optJSONArray("skillXp"),pts=obj.optJSONArray("points");
        for(int i=0;i<p.skillLevels.length;i++){
            if(levels!=null)p.skillLevels[i]=Math.max(0,Math.min(100,levels.optInt(i,15)));
            if(xp!=null)p.skillXp[i]=Math.max(0,xp.optInt(i,0));
        }
        for(int i=0;i<3;i++)if(pts!=null)p.spent[i]=Math.max(0,Math.min(5,pts.optInt(i,0)));
        JSONObject inv=obj.optJSONObject("inventory");
        if(inv!=null){Iterator<String> keys=inv.keys();while(keys.hasNext()){String k=keys.next();p.quantity(k,inv.optInt(k,0));}}
        // Older saves contained an invalid armor key; convert without duplicating weight.
        int legacyArmor=p.quantity("Стальной кираса");
        if(legacyArmor>0){
            p.quantity("Стальная кираса",Math.max(legacyArmor,p.quantity("Стальная кираса")));
            p.inventory.remove("Стальной кираса");
        }
        JSONArray study=obj.optJSONArray("studyPerks");if(study!=null)for(int i=0;i<p.studyPerks.length;i++)if(!isCombatPerk(i))p.studyPerks[i]=Math.max(0,Math.min(5,study.optInt(i,0)));
        JSONArray perkRanks=obj.optJSONArray("combatPerks");
        if(perkRanks!=null)for(int i=0;i<p.combatPerks.length;i++)
            if(isCombatPerk(i))p.combatPerks[i]=Math.max(0,Math.min(5,perkRanks.optInt(i,0)));
        p.equipmentInitialized=obj.optBoolean("equipmentInitialized",false);
        JSONObject gear=obj.optJSONObject("equipped");
        if(gear!=null)for(String slot:SLOTS){
            String id=gear.optString(slot,"");if(!id.isEmpty())p.equipped.put(slot,id);
        }
        JSONObject temper=obj.optJSONObject("tempered");
        if(temper!=null){Iterator<String> keys=temper.keys();while(keys.hasNext()){
            String id=keys.next();ItemCatalog.Item item=ItemCatalog.find(id);int quality=temper.optInt(id,0);
            if(item!=null&&p.quantity(id)>0&&!item.temperMaterial().isEmpty()&&quality>0&&quality<=6)
                p.tempered.put(id,quality);
        }}
        p.cleanEquipment();
        JSONArray done=obj.optJSONArray("completed"),bad=obj.optJSONArray("failed");
        if(done!=null)for(int i=0;i<done.length();i++)p.completed.add(done.optString(i));
        if(bad!=null)for(int i=0;i<bad.length();i++)p.failed.add(bad.optString(i));
        JSONObject outcomes=obj.optJSONObject("questOutcomes");
        if(outcomes!=null){Iterator<String> keys=outcomes.keys();while(keys.hasNext()){
            String id=keys.next();p.setQuestOutcome(id,outcomes.optString(id,""));
        }}
        return p;
    }
}
