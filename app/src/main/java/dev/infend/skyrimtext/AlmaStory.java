package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.Locale;

/** Optional original questline. Decisions are authoritative, saved and replay-safe. */
final class AlmaStory {
    static final String ID="alma_crimson_immortal", MODE="ALMA", NAME="Альма";
    static final String[] TITLES={"Незнакомка под багровой луной","Тайна алых глаз","Кровь на руках","Запретные знания","Контракт Тёмного Братства","Призраки прошлого","По ту сторону Обливиона","Цена бессмертия","Сердце вампира","Между любовью и смертью","Последний договор","Вечность начинается сегодня"};
    static final String[] BODY={
        "У канала Рифтена стоит незнакомка с длинными багровыми волосами. Серебряный амулет ловит отблеск фонаря. Она замечает твой взгляд и отступает к стене.\n\nАльма:\n— Если ищешь неприятностей, сегодня не твой вечер. Если ищешь ответы, возможно, наш.",
        "Альма показывает клыки, не приближаясь. Она признаётся, что избегает Волкихар и не питается случайными прохожими.\n\nАльма:\n— Я вампир. Можешь уйти. Но не называй меня чудовищем, пока не узнаешь, что я сделала.",
        "Она достаёт печать с вырезанной чёрной ладонью. В прошлом Альма исполняла контракты Тёмного Братства, а затем отказалась убить невиновного.\n\nАльма:\n— Прошлое нельзя вырезать ножом. Я хочу отвечать за него сама.",
        "В старой книге Коллегии Винтерхолда описано смешение даэдрической и жизненной магии. Альма объясняет, как смертельная болезнь заставила её искать запретные знания.\n\nАльма:\n— Я думала, что лечу себя. На самом деле открыла дверь.",
        "Альма обнаруживает послание Братства. За её голову назначена награда. Она предлагает решить, стоит ли предупредить возможных жертв.\n\nАльма:\n— Я не прошу тебя убивать ради меня.",
        "В её дневнике перечислены имена людей, которых она не сумела спасти. Среди страниц спрятан ключ к договору.\n\nАльма:\n— Четыре века — слишком долгий срок, чтобы помнить только собственную боль.",
        "Серебряный амулет открывает видение Обливиона. В нём Альма видит своё отражение с иной, даэдрической природой.\n\nАльма:\n— Это часть меня, но не вся я. Не дай ей решать за нас.",
        "Из текста договора становится ясно: после гибели её душа возвращается, но каждое возвращение отдаёт часть памяти неизвестной сущности.\n\nАльма:\n— Бессмертие — не свобода. Я устала платить за него.",
        "Впервые за долгие годы Альма говорит о будущем. Она предлагает остаться рядом, не требуя обещаний.\n\nАльма:\n— Мне важно знать, чего хочешь ты. И я тоже хочу иметь право выбирать.",
        "Печать договора начинает трескаться. Альма предлагает готовиться к последнему выбору, а не бросаться навстречу опасности.\n\nАльма:\n— Не жертвуй собой ради красивой легенды. Мне нужен живой союзник.",
        "Условие договора найдено: отказаться от силы, разделить её по обоюдному согласию или принять даэдрическую природу. Ни один путь не отменяет последствий.\n\nАльма:\n— Я могу выбрать сама. Но хочу услышать тебя.",
        "Последние строки договора мерцают. Альма держит амулет на раскрытой ладони.\n\nАльма:\n— Какую бы дорогу мы ни выбрали, она должна быть нашей, а не навязанной."
    };
    static final String[] GOOD={"Представиться и выслушать","Принять её признание","Попросить рассказать о жертвах","Изучить записи вместе","Предупредить возможных жертв","Помочь разобрать дневник","Поддержать её выбор","Обсудить цену договора","Предложить союз без условий","Составить безопасный план","Обсудить свободный выбор"};
    static final String[] COOL={"Сохранить дистанцию","Признать, что нужно время","Не оправдывать прошлое","Попросить проверить выводы","Избежать столкновения","Не расспрашивать о прошлом","Остаться наблюдателем","Отложить решение","Остаться знакомыми","Предложить осторожность","Не вмешиваться в её решение"};
    boolean enabled=true, romance=false, married=false, companion=false;
    int stage=0, trust=0, respect=0, affection=0, love=0, jealousy=0, hurt=0, loyalty=0, lastTalkDay=-1;
    String ending="";
    int rank=1, mana=80, health=100, victories=0, cooldown=0, lastGiftDay=-1, lastRemarkDay=-1, stance=0, weapon=0, armor=0;
    boolean savedHero=false, hunterDefeated=false;
    int lastJealousyDay=-1, supplies=2, talisman=0, archiveStep=0, seranaStep=0, sealStep=0, lastPower=0, lastHeal=0;
    int level=1,xp=0,daysIndependent=0,lastIndependentDay=-1,postStep=0,engagementDay=-1,dates=0,study=0;
    boolean engaged=false,sharedHome=false;
    int lastPostDay=-1,personalAid=0,personalResearch=0;
    int lastInitiativeDay=-1,lastEventDay=-1,personalTasks=0,seranaTrust=0,katherineTrust=0,lastNpcDay=-1,lastHomeDay=-1;
    String mood(){return hurt>=35?"Обижена":jealousy>=50?"Обеспокоена":health<maxHealth()/3?"Утомлена":romance&&love>=75?"Счастлива":trust>=65?"Доверяет":"Спокойна";}
    String activity(GameEngine g){if(terminalLoss())return ending;if(active())return "Путешествует с героем";if(g.hour<6)return "Отдыхает";if(g.hour<12)return "Занимается исследованиями";if(g.hour<18)return "Выполняет личные поручения";return "Возвращается к месту ночлега";}
    void initiative(GameEngine g){if(!active()||lastInitiativeDay==g.day)return;lastInitiativeDay=g.day;if(health<maxHealth()/3||mana<maxMana()/4)remember("Альма предлагает отдохнуть и пополнить припасы.");else if(jealousy>=50)remember("Альма просит поговорить об отношениях.");else if(romance&&love>=75)remember("Альма предлагает вместе провести вечер.");else if(stage>=4&&stage<12&&!hunterDefeated)remember("Альма проверяет, нет ли следов преследователя.");}
    String compactContext(GameEngine g){if(!enabled)return "Альма отключена; не вводить в сцену.";if(stage==0)return "";if(terminalLoss())return aiContext();return "Альма: рядом="+present(g)+", место="+location(g)+", "+relationship()+", доверие="+trust+", настроение="+mood()+", цель="+goal+". Состояние меняет только движок.";}
    boolean eventReady(GameEngine g){return stage>=3&&!terminalLoss()&&lastEventDay!=g.day;}
    int eventIndex(GameEngine g){return Math.floorMod(g.day,AlmaLife.TASKS.length);}
    int epilogueBranch(){return mortal()?0:"Алая королева".equals(ending)?1:2;}
    String epilogueTask(){return postStep<3?AlmaLife.FINALE_TASKS[epilogueBranch()][postStep]:"Эпилог завершён";}
    boolean epilogueReady(GameEngine g){return stage==12&&!terminalLoss()&&postStep<3&&lastPostDay!=g.day;}
    void completePersonal(GameEngine g,boolean research){lastEventDay=g.day;personalTasks=GameEngine.nonnegativeAdd(personalTasks,1);gain(18);trust=clamp(trust+2);
        if(research){personalResearch=GameEngine.nonnegativeAdd(personalResearch,1);respect=clamp(respect+2);if(!mortal())bloodSkill=clamp(bloodSkill+1);remember(AlmaLife.RESEARCH[eventIndex(g)]);}
        else{personalAid=GameEngine.nonnegativeAdd(personalAid,1);loyalty=clamp(loyalty+2);remember(AlmaLife.RESULTS[eventIndex(g)]);}}

    String goal="Разобраться в природе проклятия";
    JSONObject pack=new JSONObject(), equippedPack=new JSONObject();
    int bloodSkill=15, bladeSkill=15, guardSkill=15, markTurns=0, shadowTurns=0;
    String skillsLabel(){return "Магия крови "+bloodSkill+"/100 | Клинок "+bladeSkill+"/100 | Защита "+guardSkill+"/100";}
    boolean equipPack(GameEngine g,String id){if(!enabled||terminalLoss()||!present(g)||stage<3||"COMBAT".equals(g.mode)||pack.optInt(id)<=0)return false;ItemCatalog.Item item=ItemCatalog.find(id);if(item==null||item.slot().isEmpty()||"ammo".equals(item.slot())||item.twoHanded())return false;String slot=item.slot();if(id.equals(equippedPack.optString(slot))){equippedPack.remove(slot);return true;}try{equippedPack.put(slot,id);}catch(JSONException e){return false;}return true;}
    String equipmentName(String slot,String fallback){ItemCatalog.Item item=ItemCatalog.find(equippedPack.optString(slot));return item!=null&&pack.optInt(item.id)>0?item.name:fallback;}
    int equipmentBonus(boolean weaponSlot){int n=0;java.util.Iterator<String> it=equippedPack.keys();while(it.hasNext()){String slot=it.next();ItemCatalog.Item item=ItemCatalog.find(equippedPack.optString(slot));if(item!=null&&pack.optInt(item.id)>0&&item.baseStat>0&&weaponSlot=="weapon".equals(slot))n+=Math.min(8,(int)(item.baseStat/5));}return Math.min(12,n);}
    int weaken(int incoming){if(!active()||mortal()||health<=0)return 0;int n=0;if(shadowTurns>0){shadowTurns--;n+=3;}if(markTurns>0){markTurns--;n+=2;}return Math.min(Math.max(0,incoming-1),n);}
    int combo(int remaining,boolean playerMagic){if(!active()||mortal()||!playerMagic||markTurns<=0||remaining<=0||mana<5)return 0;mana-=5;return Math.min(remaining,4+rank);}

    int needXp(){return level*100;}
    void gain(int amount){xp=GameEngine.nonnegativeAdd(xp,amount);while(level<100&&xp>=needXp()){xp-=needXp();level++;}if(level==100)xp=Math.min(xp,needXp());}
    String location(GameEngine g){if(active())return g.progress.location;if(stage<12||terminalLoss()||sharedHome)return "Рифтен";String[] route={"Рифтен","Винтерхолд","Вайтран"};return route[Math.floorMod(g.day/3,route.length)];}
    boolean present(GameEngine g){return active()||g.progress.location.equalsIgnoreCase(location(g))||(stage<12&&atRiften(g));}
    void independent(GameEngine g){if(!enabled||stage<3||terminalLoss()||companion||lastIndependentDay==g.day)return;lastIndependentDay=g.day;
        if(stage>=12)daysIndependent=GameEngine.nonnegativeAdd(daysIndependent,1);gain(12);health=Math.min(maxHealth(),health+15);mana=Math.min(maxMana(),mana+15);
        int branch=stage<12?0:mortal()?1:"Алая королева".equals(ending)?2:3;int index=Math.floorMod(g.day,AlmaLife.EVENTS[branch].length);goal=AlmaLife.GOALS[branch][index];remember("День "+g.day+" | "+location(g)+" | "+AlmaLife.EVENTS[branch][index]);}

    double packWeight(){double total=0;java.util.Iterator<String> it=pack.keys();while(it.hasNext()){String id=it.next();ItemCatalog.Item item=ItemCatalog.find(id);if(item!=null)total+=item.weight*pack.optInt(id);}return total;}
    boolean transferable(GameEngine g,String id){return id!=null&&ItemCatalog.find(id)!=null&&CompensationPowers.itemIndex(id)<0&&!id.equals("Септимы")&&!id.equals("Походный клинок")&&!id.equals("Дорожная одежда")&&!id.equals("Стальной меч")&&!id.equals("Стальная кираса")&&!g.progress.equipped(id)&&g.progress.temperQuality(id)==0&&!id.equals("Зелье магии");}
    boolean transfer(GameEngine g,String id,boolean give){if(!enabled||terminalLoss()||stage<3||!present(g)||"COMBAT".equals(g.mode)||!transferable(g,id))return false;
        if("Лечебное зелье".equals(id)){if(give){if(g.potions<=0||supplies>=10)return false;g.potions--;supplies++;}else{if(supplies<=0||g.potions==Integer.MAX_VALUE||g.progress.carryWeight()+ItemCatalog.find(id).weight>50.00001)return false;supplies--;g.potions++;}g.progress.sync(g);return true;}
        int n=pack.optInt(id);ItemCatalog.Item item=ItemCatalog.find(id);
        if(give){if(n>=100||packWeight()+item.weight>35.00001||!g.progress.removeItem(id,1))return false;try{pack.put(id,n+1);}catch(JSONException e){throw new IllegalStateException(e);}}
        else{if(n<=0||!g.progress.addItem(id,1))return false;if(n==1){pack.remove(id);java.util.Iterator<String> slots=equippedPack.keys();while(slots.hasNext()){String slot=slots.next();if(id.equals(equippedPack.optString(slot)))slots.remove();}}else try{pack.put(id,n-1);}catch(JSONException e){throw new IllegalStateException(e);}}g.progress.sync(g);return true;}
    String epilogue(){switch(ending){case "Новая жизнь":return "Альма учится жить без проклятия и помогает путникам.";case "Вечность вдвоём":return "Альма бережёт вашу связь и записывает общие приключения.";case "Алая королева":return "Альма исследует границу Обливиона и охраняет печать.";case "Разбитая клятва":return "Ваши дороги разошлись. Альма больше не сопровождает героя.";case "Последняя жертва":return "Память об Альме живёт в серебряном амулете.";default:return "";}}
    boolean setEnabled(GameEngine g,boolean on){if("COMBAT".equals(g.mode))return false;enabled=on;if(!on&&MODE.equals(g.mode))g.mode="WORLD";return true;}
    String inventoryLabel(){return "Лечебные зелья: "+supplies+"/10 | "+gearLabel()+" | "+(talisman==0?"Серебряный амулет":"Амулет алой печати");}
    String powerLabel(){switch(lastPower){case 1:return "Кровавый поцелуй";case 2:return "Багровое пламя";case 3:return "Метка смерти";case 4:return "Алая тень";case 5:return "Даэдрический облик";case 6:return "Удар кинжалом";default:return "Поддержка";}}
    String objective(){if(stage==0)return "Первая встреча в Рифтене.";if(stage==12)return terminalLoss()?ending:ending+" | "+epilogueTask();if(stage>=6&&archiveStep<2)return TITLES[stage]+" | Дополнительно: архив Коллегии";return TITLES[stage]+(companion?" | Альма рядом":" | Рифтен");}
    boolean at(GameEngine g,String fragment){return g.progress.location.toLowerCase(Locale.ROOT).replace('ё','е').contains(fragment);}
    void useSupply(){if(supplies>0&&health>0&&health<=maxHealth()/2){supplies--;health=Math.min(maxHealth(),health+35);remember("Альма выпила лечебное зелье.");}}

    String lastRemark="";
    org.json.JSONArray memory=new org.json.JSONArray();
    int maxMana(){return 80+20*(rank-1);}
    int maxHealth(){return 100+25*(rank-1);}
    boolean active(){return enabled&&companion&&!terminalLoss();}
    boolean mortal(){return "Новая жизнь".equals(ending);}
    String stanceLabel(){return new String[]{"Сбалансированная","Защитная","Наступательная"}[stance];}
    String gearLabel(){return equipmentName("weapon",weapon==0?"Алый кинжал":"Зачарованный кинжал")+" | "+equipmentName("body",armor==0?"Кожаный доспех":"Усиленный доспех");}
    void remember(String s){if(s.isEmpty())return;while(memory.length()>=1024)memory.remove(0);memory.put(s);lastRemark=s;}
    void beginBattle(){if(!active())return;savedHero=false;cooldown=0;lastPower=lastHeal=markTurns=shadowTurns=0;}
    void rest(int hours){if(!active())return;int h=Math.max(0,Math.min(24,hours));mana=Math.min(maxMana(),mana+h*8);health=Math.min(maxHealth(),health+h*10);}
    int assist(GameEngine g,int turn,int remaining){lastPower=lastHeal=0;if(!active()||remaining<=0||health<=0)return 0;useSupply();if(cooldown>0)cooldown--;if(turn%2!=0)return 0;
        int damage=5+rank*3+weapon*3+(stance==2?4:0)+(talisman*2)+equipmentBonus(true)+Math.min(5,bladeSkill/20);
        if(mortal()){lastPower=6;bladeSkill=clamp(bladeSkill+1);return Math.min(remaining,damage);}
        if(mana<10)return 0;int cost=10;
        if((g.hour>=20||g.hour<6))damage+=3;
        if(rank>=4&&stage>=7&&mana>=25&&turn%8==0){lastPower=5;cost=25;damage+=12;}
        else if(rank>=3&&turn%6==0){lastPower=3;damage+=7;markTurns=3;}
        else if(rank>=2&&turn%4==0){lastPower=4;damage+=4;shadowTurns=2;}
        else if(health<maxHealth()&&mana>=12){lastPower=1;cost=12;lastHeal=Math.min(maxHealth()-health,8+rank*2);health+=lastHeal;}
        else lastPower=2;
        mana-=cost;bloodSkill=clamp(bloodSkill+1);cooldown=1;return Math.min(remaining,damage);}
    int bloodLink(GameEngine g){if(!active()||mortal()||!romance||love<70||mana<12||health<=20||g.hp>=g.maxHp/3)return 0;int n=Math.min(g.maxHp-g.hp,Math.min(health-10,6+rank*2));if(n<=0)return 0;mana-=12;health-=n;g.hp+=n;return n;}
    int protect(int incoming){if(!active()||health<=0||incoming<2||stance==2||(!mortal()&&mana<5))return 0;int blocked=Math.min(incoming-1,2+rank+(stance==1?3:0)+armor*2+equipmentBonus(false)+guardSkill/25);guardSkill=clamp(guardSkill+1);if(!mortal())mana-=5;health=Math.max(0,health-1);return blocked;}
    boolean rescue(GameEngine g){if(!active()||!romance||love<85||mana<25||savedHero||health<=0||mortal())return false;savedHero=true;mana-=25;health=0;g.hp=Math.max(1,g.maxHp/5);remember("Алая клятва: Альма спасла героя от гибели.");return true;}
    void victory(GameEngine g){if(enabled&&"ALMA".equals(g.battleKind)&&!hunterDefeated){hunterDefeated=true;trust=clamp(trust+8);respect=clamp(respect+8);remember("Контракт Тёмного Братства: преследователь побеждён.");}if(!active())return;victories=GameEngine.nonnegativeAdd(victories,1);gain(35+rank*10);int old=rank;rank=Math.min(5,1+victories/5);if(rank>old){mana=maxMana();health=maxHealth();remember("Альма достигла магического ранга "+rank+".");}if(!mortal()&&health==0){health=Math.max(1,maxHealth()/3);remember("Альма вернулась после поражения, ослабленная проклятием.");}mana=Math.min(maxMana(),mana+8);}
    void observeTravel(GameEngine g){independent(g);if(!active())return;initiative(g);if(romance&&g.katherine.enabled&&g.katherine.romance&&lastJealousyDay!=g.day){lastJealousyDay=g.day;jealousy=clamp(jealousy+6);remember("Альма хочет спокойно обсудить отношения с Кэтрин.");}if(lastRemarkDay==g.day)return;lastRemarkDay=g.day;String loc=g.progress.location;String line=loc.toLowerCase(Locale.ROOT).contains("винтерхолд")?"Эти башни напоминают мне о запретных уроках.":loc.toLowerCase(Locale.ROOT).contains("рифтен")?"В Рифтене у каждого есть тайна.":"Путь в "+loc+" запомнится нам обоим.";remember("День "+g.day+" | "+line);}
    String aiContext(){if(!enabled)return "Дополнение Альма отключено: не вводи её в сцены, не используй прежние записи о ней.";if(stage==0)return "";if(terminalLoss())return "Альма: "+ending+". "+("Последняя жертва".equals(ending)?"Погибла; не вводи её как живого персонажа.":"Пути разошлись; не вводи её как спутницу или возлюбленную.");return "Альма: "+status()+"; отношения "+relationship()+"; доверие "+trust+"; любовь "+love+"; спутница "+companion+"; магический ранг "+rank+"; исход "+ending+"; уровень "+level+"; цель "+goal+"; помолвка "+engaged+"; общий дом "+sharedHome+". Не меняй эти подтверждённые значения. Последняя реплика: "+lastRemark;}

    static int clamp(int n){return Math.max(0,Math.min(100,n));}
    static boolean atRiften(GameEngine g){String l=g.progress.location.toLowerCase(Locale.ROOT).replace('ё','е');return l.contains("рифтен");}
    boolean available(GameEngine g){return enabled&&g.started&&"WORLD".equals(g.mode)&&!terminalLoss()&&present(g);}
    String status(){return stage==0?"Не встречена":stage<TITLES.length?TITLES[stage]:ending;}
    String relationship(){if(stage==0)return "Незнакомка";if("Разбитая клятва".equals(ending))return "Противник";if(married)return "Супруга";if(engaged)return "Помолвлены";if(romance&&love>=70)return "Возлюбленная";if(romance)return "Влюблённость";if(trust>=45)return "Друг";return "Знакомая";}
    boolean canRomance(){return stage>=8&&trust>=45&&affection>=35&&!terminalLoss();}
    boolean terminalLoss(){return "Последняя жертва".equals(ending)||"Разбитая клятва".equals(ending);}
    JSONObject save() throws JSONException{return new JSONObject().put("enabled",enabled).put("stage",stage).put("trust",trust).put("respect",respect).put("affection",affection).put("love",love).put("jealousy",jealousy).put("hurt",hurt).put("loyalty",loyalty).put("romance",romance).put("married",married).put("companion",companion).put("lastTalkDay",lastTalkDay).put("ending",ending).put("rank",rank).put("mana",mana).put("health",health).put("victories",victories).put("cooldown",cooldown).put("savedHero",savedHero).put("hunterDefeated",hunterDefeated).put("lastJealousyDay",lastJealousyDay).put("stance",stance).put("weapon",weapon).put("armor",armor).put("lastGiftDay",lastGiftDay).put("lastRemarkDay",lastRemarkDay).put("lastRemark",lastRemark).put("memory",memory).put("supplies",supplies).put("talisman",talisman).put("archiveStep",archiveStep).put("seranaStep",seranaStep).put("sealStep",sealStep).put("level",level).put("xp",xp).put("daysIndependent",daysIndependent).put("lastIndependentDay",lastIndependentDay).put("postStep",postStep).put("engagementDay",engagementDay).put("dates",dates).put("study",study).put("engaged",engaged).put("sharedHome",sharedHome).put("goal",goal).put("pack",pack).put("equippedPack",equippedPack).put("bloodSkill",bloodSkill).put("bladeSkill",bladeSkill).put("guardSkill",guardSkill).put("markTurns",markTurns).put("shadowTurns",shadowTurns).put("lastInitiativeDay",lastInitiativeDay).put("lastEventDay",lastEventDay).put("personalTasks",personalTasks).put("seranaTrust",seranaTrust).put("katherineTrust",katherineTrust).put("lastNpcDay",lastNpcDay).put("lastHomeDay",lastHomeDay).put("lastPostDay",lastPostDay).put("personalAid",personalAid).put("personalResearch",personalResearch);}
    static AlmaStory restore(JSONObject j){AlmaStory a=new AlmaStory();if(j==null)return a;
        a.enabled=j.optBoolean("enabled",true);a.stage=Math.max(0,Math.min(TITLES.length,j.optInt("stage")));
        a.trust=clamp(j.optInt("trust"));a.respect=clamp(j.optInt("respect"));a.affection=clamp(j.optInt("affection"));a.love=clamp(j.optInt("love"));a.jealousy=clamp(j.optInt("jealousy"));a.hurt=clamp(j.optInt("hurt"));a.loyalty=clamp(j.optInt("loyalty"));
        a.romance=j.optBoolean("romance")&&a.stage>=8;a.married=j.optBoolean("married")&&a.romance;a.companion=j.optBoolean("companion")&&a.stage>=3;a.lastTalkDay=j.optInt("lastTalkDay",-1);a.ending=a.stage==TITLES.length?j.optString("ending","Новая жизнь"):"";if(a.terminalLoss()){a.companion=false;a.romance=false;a.married=false;}a.rank=Math.max(1,Math.min(5,j.optInt("rank",1)));a.mana=Math.max(0,Math.min(a.maxMana(),j.optInt("mana",a.maxMana())));a.health=Math.max(0,Math.min(a.maxHealth(),j.optInt("health",a.maxHealth())));a.victories=Math.max(0,j.optInt("victories"));a.cooldown=Math.max(0,Math.min(2,j.optInt("cooldown")));a.savedHero=j.optBoolean("savedHero");a.hunterDefeated=j.optBoolean("hunterDefeated");a.lastJealousyDay=j.optInt("lastJealousyDay",-1);a.stance=Math.max(0,Math.min(2,j.optInt("stance")));a.weapon=Math.max(0,Math.min(1,j.optInt("weapon")));a.armor=Math.max(0,Math.min(1,j.optInt("armor")));a.lastGiftDay=j.optInt("lastGiftDay",-1);a.lastRemarkDay=j.optInt("lastRemarkDay",-1);a.lastRemark=j.optString("lastRemark","");a.supplies=Math.max(0,Math.min(10,j.optInt("supplies",2)));a.talisman=Math.max(0,Math.min(1,j.optInt("talisman")));a.archiveStep=Math.max(0,Math.min(2,j.optInt("archiveStep")));a.seranaStep=Math.max(0,Math.min(2,j.optInt("seranaStep")));a.sealStep=Math.max(0,Math.min(2,j.optInt("sealStep")));a.level=Math.max(1,Math.min(100,j.optInt("level",1)));a.xp=Math.max(0,Math.min(a.needXp(),j.optInt("xp")));a.daysIndependent=Math.max(0,j.optInt("daysIndependent"));a.lastIndependentDay=j.optInt("lastIndependentDay",-1);a.postStep=Math.max(0,Math.min(3,j.optInt("postStep")));a.engagementDay=j.optInt("engagementDay",-1);a.dates=Math.max(0,j.optInt("dates"));a.study=j.optInt("study")&4095;a.engaged=j.optBoolean("engaged")&&a.romance&&!a.married;a.sharedHome=j.optBoolean("sharedHome")&&a.married;a.goal=j.optString("goal","Разобраться в природе проклятия");JSONObject bag=j.optJSONObject("pack");if(bag!=null){java.util.Iterator<String> ids=bag.keys();while(ids.hasNext()){String id=ids.next();int count=Math.max(0,Math.min(100,bag.optInt(id)));if(count>0&&ItemCatalog.find(id)!=null&&CompensationPowers.itemIndex(id)<0)try{a.pack.put(id,count);}catch(JSONException ignored){}}}a.bloodSkill=clamp(j.optInt("bloodSkill",15));a.bladeSkill=clamp(j.optInt("bladeSkill",15));a.guardSkill=clamp(j.optInt("guardSkill",15));a.markTurns=Math.max(0,Math.min(3,j.optInt("markTurns")));a.shadowTurns=Math.max(0,Math.min(2,j.optInt("shadowTurns")));JSONObject eq=j.optJSONObject("equippedPack");if(eq!=null){java.util.Iterator<String> slots=eq.keys();while(slots.hasNext()){String slot=slots.next(),id=eq.optString(slot);ItemCatalog.Item item=ItemCatalog.find(id);if(item!=null&&a.pack.optInt(id)>0&&slot.equals(item.slot())&&!item.twoHanded()&&!"ammo".equals(slot))try{a.equippedPack.put(slot,id);}catch(JSONException ignored){}}}a.lastInitiativeDay=j.optInt("lastInitiativeDay",-1);a.lastEventDay=j.optInt("lastEventDay",-1);a.personalTasks=Math.max(0,j.optInt("personalTasks"));a.seranaTrust=clamp(j.optInt("seranaTrust",15*a.seranaStep));a.katherineTrust=clamp(j.optInt("katherineTrust"));a.lastNpcDay=j.optInt("lastNpcDay",-1);a.lastHomeDay=j.optInt("lastHomeDay",-1);a.lastPostDay=j.optInt("lastPostDay",-1);a.personalAid=Math.max(0,j.optInt("personalAid"));a.personalResearch=Math.max(0,j.optInt("personalResearch"));org.json.JSONArray m=j.optJSONArray("memory");if(m!=null)for(int i=Math.max(0,m.length()-1024);i<m.length();i++)a.memory.put(m.optString(i));return a;}
    GameEngine.Scene scene(GameEngine g){
        if(!enabled||terminalLoss()||!present(g))return new GameEngine.Scene("АЛАЯ БЕССМЕРТНАЯ","Встреча недоступна","Альма сейчас недоступна.").add("ALMA:exit","Вернуться","Продолжить путешествие");
        String stats=(lastRemark.isEmpty()?"":"\n\n"+lastRemark)+"\n\nОтношения: "+relationship()+" | Доверие "+trust+"/100 | Симпатия "+affection+"/100 | Любовь "+love+"/100";
        GameEngine.Scene s=new GameEngine.Scene("АЛАЯ БЕССМЕРТНАЯ",stage<TITLES.length?TITLES[stage]:ending,(stage<TITLES.length?BODY[stage]:"Альма завершила свою историю. "+epilogue())+stats);
        if(stage<12&&(study&(1<<stage))==0)s.add("ALMA:study:"+stage,"Углубиться в историю",AlmaChapters.TOPICS[stage]);
        if(epilogueReady(g))s.add("ALMA:post:"+postStep,epilogueTask(),"Эпилог "+(postStep+1)+"/3 | "+ending);
        if(stage<11){s.add("ALMA:good:"+stage,GOOD[stage],"Продолжить историю");s.add("ALMA:cool:"+stage,COOL[stage],"Осторожный выбор");}
        if(stage==11){s.add("ALMA:finish:mortal","Освободить Альму от проклятия","Новая жизнь");
            if(romance&&love>=60)s.add("ALMA:finish:eternal","Предложить разделить бессмертие","Вечность вдвоём");
            s.add("ALMA:finish:queen","Принять её даэдрическую природу","Алая королева");
            s.add("ALMA:finish:part","Разойтись и разрушить клятву","Разбитая клятва");
            if(trust>=65)s.add("ALMA:finish:sacrifice","Принять её решение о жертве","Последняя жертва");}
        if(stage>=3&&!terminalLoss())s.add(companion?"ALMA:dismiss":"ALMA:join",companion?"Попросить ждать в Рифтене":"Предложить путешествовать вместе","Союзница");
        if(stage>=3&&!terminalLoss()){
            if(eventReady(g)){s.add("ALMA:personal:"+g.day,"Помочь Альме",AlmaLife.TASKS[eventIndex(g)]);s.add("ALMA:research:"+g.day,"Выбрать исследование",AlmaLife.RESEARCH_TASKS[eventIndex(g)]);}
            if(seranaStep>=2&&atRiften(g)&&lastNpcDay!=g.day)s.add("ALMA:seranaTalk","Обсудить с Сераной новую жизнь","Доверие Сераны: "+seranaTrust+"/100");
            if(sharedHome&&married&&atRiften(g)&&lastHomeDay!=g.day)s.add("ALMA:homeEvening","Провести вечер дома","Совместный отдых");
            if(supplies<10&&g.gold>=35)s.add("ALMA:supply","Купить Альме лечебное зелье","35 септимов | "+supplies+"/10");
            if(supplies>0&&health<maxHealth())s.add("ALMA:heal","Предложить лечебное зелье","Восстановить до 35 здоровья Альмы");
            if(stage>=4&&at(g,"винтерхолд")&&archiveStep<2)s.add("ALMA:archive:"+archiveStep,archiveStep==0?"Исследовать архив Коллегии":"Сопоставить записи о договоре","Дополнительное исследование");
            if(stage>=6&&atRiften(g)&&seranaStep<2)s.add("ALMA:serana:"+seranaStep,seranaStep==0?"Встретиться с Сераной":"Продолжить разговор с Сераной","Авторская встреча вампиров");
            if(stage>=7&&at(g,"рифтен")&&archiveStep==2&&sealStep<2)s.add("ALMA:seal:"+sealStep,sealStep==0?"Изучить алую печать":"Завершить ритуал амулета","Дополнительное исследование");
            if(stage>=4&&stage<12&&!hunterDefeated)s.add("ALMA:hunt","Остановить преследователя Братства","Дополнительная боевая встреча");
            if(romance&&jealousy>0&&lastTalkDay!=g.day)s.add("ALMA:boundaries","Обсудить ревность и границы","Честный разговор");
            s.add("ALMA:stance","Обсудить боевую тактику",stanceLabel());
            if(weapon==0&&g.gold>=150)s.add("ALMA:weapon","Улучшить кинжал","150 септимов");
            if(armor==0&&g.gold>=120)s.add("ALMA:armor","Укрепить доспех","120 септимов");
            if(lastGiftDay!=g.day&&g.gold>=30)s.add("ALMA:gift","Подарить серебряное украшение","30 септимов | один раз в день");
            if(g.katherine.enabled&&g.katherine.stage>=3&&g.katherine.following&&lastTalkDay!=g.day&&g.katherine.companion.lastJoint!=g.day)s.add("ALMA:katherine","Поговорить с Кэтрин вместе","Обсудить доверие и магию");
        }
        if(canRomance()&&!romance&&hurt<15)s.add("ALMA:romance","Признаться в чувствах","Только при взаимной симпатии");
        if(!romance&&hurt>=15&&lastTalkDay!=g.day&&!terminalLoss())s.add("ALMA:reconcile","Поговорить о прошлом","Не обещает возобновления романа");
        if(romance&&!terminalLoss()){
            if(lastTalkDay!=g.day)s.add("ALMA:date","Провести время вместе","Один раз за игровой день");
            if(!married&&!engaged&&love>=85&&trust>=75&&stage>=10)s.add("ALMA:engage","Предложить помолвку","Взаимное согласие");
            if(engaged&&!married&&g.day>engagementDay&&atRiften(g))s.add("ALMA:marry","Сыграть свадьбу в Рифтене","Храм Мары");
            if(married&&!sharedHome&&atRiften(g)&&g.gold>=500)s.add("ALMA:home","Обустроить общий дом","500 септимов");
            s.add("ALMA:breakup","Поговорить о расставании","Завершить роман");
        }
        return s.add("ALMA:exit","Вернуться","Продолжить путешествие");
    }
    void choose(GameEngine g,String action){
        if("ALMA:open".equals(action)){if(available(g))g.mode=MODE;return;}
        if(!MODE.equals(g.mode))return;
        if("ALMA:exit".equals(action)){g.mode="WORLD";return;}
        if(!enabled||terminalLoss())return;
        if(!present(g))return;
        boolean advance=stage<11&&(action.equals("ALMA:good:"+stage)||action.equals("ALMA:cool:"+stage));
        if(advance){gain(20);boolean good=action.startsWith("ALMA:good:");trust=clamp(trust+(good?9:4));respect=clamp(respect+(good?8:5));affection=clamp(affection+(good?7:2));loyalty=clamp(loyalty+(good?6:2));if(romance)love=clamp(love+(good?9:3));stage++;}
        else if(stage<12&&(study&(1<<stage))==0&&action.equals("ALMA:study:"+stage)){study|=1<<stage;gain(15);respect=clamp(respect+2);remember(AlmaChapters.DETAIL[stage]);}
        else if(epilogueReady(g)&&action.equals("ALMA:post:"+postStep)){int branch=epilogueBranch();lastPostDay=g.day;remember(AlmaLife.FINALE_RESULTS[branch][postStep]);postStep++;gain(25);respect=clamp(respect+3);if(postStep==3)loyalty=clamp(loyalty+5);}
        else if("ALMA:join".equals(action)&&stage>=3&&!terminalLoss()&&!companion)companion=true;
        else if("ALMA:dismiss".equals(action)&&companion){companion=false;g.mode="WORLD";}
        else if(eventReady(g)&&action.equals("ALMA:personal:"+g.day))completePersonal(g,false);
        else if(eventReady(g)&&action.equals("ALMA:research:"+g.day))completePersonal(g,true);
        else if("ALMA:seranaTalk".equals(action)&&seranaStep>=2&&atRiften(g)&&lastNpcDay!=g.day){lastNpcDay=g.day;seranaTrust=clamp(seranaTrust+5);respect=clamp(respect+2);remember(AlmaLife.SERANA[Math.floorMod(g.day,AlmaLife.SERANA.length)]);}
        else if("ALMA:homeEvening".equals(action)&&sharedHome&&married&&atRiften(g)&&lastHomeDay!=g.day){lastHomeDay=g.day;love=clamp(love+3);hurt=clamp(hurt-4);health=Math.min(maxHealth(),health+20);mana=Math.min(maxMana(),mana+15);remember(AlmaLife.HOME[Math.floorMod(g.day,AlmaLife.HOME.length)]);}
        else if("ALMA:supply".equals(action)&&stage>=3&&supplies<10&&g.gold>=35){g.gold-=35;supplies++;remember("Припасы Альмы пополнены.");}
        else if("ALMA:heal".equals(action)&&stage>=3&&supplies>0&&health<maxHealth()){supplies--;health=Math.min(maxHealth(),health+35);remember("Альма восстановила здоровье.");}
        else if(stage>=4&&at(g,"винтерхолд")&&archiveStep<2&&action.equals("ALMA:archive:"+archiveStep)){archiveStep++;trust=clamp(trust+4);respect=clamp(respect+5);remember(archiveStep==1?"Архив Коллегии: найдено свидетельство о связи амулета с Обливионом.":"Альма: Печать можно переписать, не отдавая душу. Теперь нам нужен сам амулет.");}
        else if(stage>=6&&atRiften(g)&&seranaStep<2&&action.equals("ALMA:serana:"+seranaStep)){seranaStep++;seranaTrust=clamp(seranaTrust+15);respect=clamp(respect+3);try{g.sceneState.setContext(g.day,g.hour,g.progress.location);g.sceneState.confirm("npc","alma_serana","Серана","MET");}catch(JSONException ignored){}remember(seranaStep==1?"Серана: Бессмертие не обязано определять твою судьбу. Альма: Впервые слышу это от той, кто понимает цену.":"Серана и Альма обсудили отказ от даэдрической власти. Альма: Спасибо. Я сохраню право решать сама.");}
        else if(stage>=7&&atRiften(g)&&archiveStep==2&&sealStep<2&&action.equals("ALMA:seal:"+sealStep)){sealStep++;if(sealStep==2){talisman=1;remember("Амулет алой печати завершён: +2 к боевой поддержке.");}else remember("На амулете проступили руны. Альма нашла безопасный способ замкнуть печать.");}
        else if("ALMA:hunt".equals(action)&&stage>=4&&stage<12&&!hunterDefeated&&!terminalLoss()){g.beginAlmaBattle();return;}
        else if("ALMA:boundaries".equals(action)&&romance&&jealousy>0&&lastTalkDay!=g.day&&!terminalLoss()){lastTalkDay=g.day;jealousy=clamp(jealousy-12);trust=clamp(trust+3);remember("Альма: Мне важна честность, а не обещания, которые нельзя сдержать.");}
        else if("ALMA:stance".equals(action)&&stage>=3&&!terminalLoss()){stance=(stance+1)%3;remember("Тактика: "+stanceLabel());}
        else if("ALMA:weapon".equals(action)&&stage>=3&&!terminalLoss()&&weapon==0&&g.gold>=150){g.gold-=150;weapon=1;remember("Кинжал Альмы зачарован.");}
        else if("ALMA:armor".equals(action)&&stage>=3&&!terminalLoss()&&armor==0&&g.gold>=120){g.gold-=120;armor=1;remember("Доспех Альмы укреплён.");}
        else if("ALMA:gift".equals(action)&&stage>=3&&!terminalLoss()&&lastGiftDay!=g.day&&g.gold>=30){g.gold-=30;lastGiftDay=g.day;affection=clamp(affection+5);if(romance)love=clamp(love+4);remember("Альма приняла серебряное украшение.");}
        else if("ALMA:katherine".equals(action)&&stage>=3&&!terminalLoss()&&g.katherine.enabled&&g.katherine.stage>=3&&g.katherine.following&&lastTalkDay!=g.day&&g.katherine.companion.lastJoint!=g.day){lastTalkDay=g.day;respect=clamp(respect+3);jealousy=clamp(jealousy-5);g.katherine.jealousy=KatherineStory.clamp(g.katherine.jealousy-5);g.katherine.companion.lastJoint=g.day;katherineTrust=clamp(katherineTrust+5);g.katherine.remember(g,"Совместный разговор с Альмой о доверии и магии.");remember(AlmaLife.KATHERINE[Math.floorMod(g.day,AlmaLife.KATHERINE.length)]);}
        else if("ALMA:romance".equals(action)&&canRomance()&&!romance&&hurt<15){romance=true;love=Math.max(love,45);}
        else if("ALMA:date".equals(action)&&romance&&!terminalLoss()&&lastTalkDay!=g.day){lastTalkDay=g.day;dates=GameEngine.nonnegativeAdd(dates,1);love=clamp(love+8);trust=clamp(trust+3);affection=clamp(affection+4);hurt=clamp(hurt-5);remember(AlmaLife.DATES[Math.floorMod(dates-1,AlmaLife.DATES.length)]);}
        else if("ALMA:engage".equals(action)&&romance&&!engaged&&!married&&love>=85&&trust>=75&&stage>=10){engaged=true;engagementDay=g.day;remember("Альма приняла предложение. Свадьба возможна в Рифтене со следующего дня.");}
        else if("ALMA:marry".equals(action)&&romance&&engaged&&!married&&g.day>engagementDay&&atRiften(g)){married=true;engaged=false;remember("В храме Мары вы обменялись клятвами. Альма: Я выбираю нашу дорогу.");}
        else if("ALMA:home".equals(action)&&married&&!sharedHome&&atRiften(g)&&g.gold>=500){g.gold-=500;sharedHome=true;remember("Вы обустроили общий дом в Рифтене.");}
        else if("ALMA:breakup".equals(action)&&romance){romance=false;married=false;engaged=false;sharedHome=false;love=0;hurt=clamp(hurt+15);lastTalkDay=g.day;}
        else if("ALMA:reconcile".equals(action)&&!romance&&hurt>=15&&lastTalkDay!=g.day&&!terminalLoss()){hurt=clamp(hurt-5);lastTalkDay=g.day;}
        else if(stage==11&&action.startsWith("ALMA:finish:")){
            switch(action.substring("ALMA:finish:".length())){
                case "mortal":ending="Новая жизнь";break;
                case "eternal":if(!romance||love<60)return;ending="Вечность вдвоём";break;
                case "queen":ending="Алая королева";break;
                case "part":ending="Разбитая клятва";break;
                case "sacrifice":if(trust<65)return;ending="Последняя жертва";break;
                default:return;
            }stage=12;goal=epilogue();gain(100);if(terminalLoss()){companion=false;romance=false;married=false;engaged=false;sharedHome=false;}
        }else return;
        g.progress.setQuestOutcome(ID,stage==12?"COMPLETED":"ACTIVE");
        try{g.sceneState.setContext(g.day,g.hour,g.progress.location);g.sceneState.confirm("npc",ID,NAME,terminalLoss()?"Разбитая клятва".equals(ending)?"HOSTILE":"DEAD":stage>=3?"HELPED":"MET");}catch(JSONException ignored){}
        if(advance||action.startsWith("ALMA:finish:")||"ALMA:romance".equals(action)||"ALMA:marry".equals(action)||"ALMA:breakup".equals(action))remember("День "+g.day+" | "+status()+" | "+relationship());
        g.message="Альма: "+status()+". "+relationship()+"."+(lastRemark.isEmpty()?"":"\n"+lastRemark);
    }
}
