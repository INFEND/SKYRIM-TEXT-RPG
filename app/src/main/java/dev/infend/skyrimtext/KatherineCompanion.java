package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/** Extended, saved companion mechanics. No playable-character mode. */
final class KatherineCompanion {
    static final String[] SPELLS={"Магический импульс","Огонь","Мороз","Молния","Двойной Источник","Пространственный разлом"};
    static final String[] GEAR={"Чёрное платье","Одеяние чародейки","Одеяние Нулевого Предела"};
    static final String[] FOCUS={"Чёрный амулет","Серебряный фокус","Кристалл Нулевого Предела"};
    int hp=100,xp=0,level=1,spell=0,gear=0,focus=0,love=0,hurt=0,attachment=0,lastDate=-1,lastJoint=-1,lastRepair=-1,lastDamon=-1,lastElisif=-1,profile=0;
    boolean engaged=false,married=false,dragonEcho=false,portalUsed=false,damonSafe=false,elisifMet=false;
    String ending="",mood="Спокойна",lastSpell="";
    JSONArray milestones=new JSONArray();
    int burn=0, slow=0, shock=0;
    int healthPotions=0,magicPotions=0,lastIndependentDay=-1,lastAiDay=-1;
    boolean aiInitiatives=true;
    String lastAiIntent="";
    String currentGoal="Узнать правду о Нулевом Пределе";
    String inventoryLabel(){return gearName()+" | Лечебные зелья: "+healthPotions+" | Зелья магии: "+magicPotions;}
    void independentTick(GameEngine g,KatherineStory k){if(!k.enabled||k.active()||k.stage<11||!"Свободная дорога".equals(ending)||lastIndependentDay==g.day)return;lastIndependentDay=g.day;String place=independentLocation(g.day,12);currentGoal=place.contains("Коллегия")?"Исследовать пространственный резонанс":place.equals("Солитьюд")?"Проверить сведения у Элисиф":"Навестить Деймона и собрать припасы";k.remember(g,"Самостоятельные дела Кэтрин: "+place+" | "+currentGoal);}
    String useSupplies(KatherineStory k){if(!k.active())return "";if(hp<maxHp()/3&&healthPotions>0){healthPotions--;hp=Math.min(maxHp(),hp+40);return "Кэтрин использует собственное лечебное зелье.";}if(k.mana<k.maxMana()/4&&magicPotions>0){magicPotions--;k.mana=Math.min(k.maxMana(),k.mana+45);return "Кэтрин восстанавливает магию своим зельем.";}return "";}
    static final String[] PROFILES={"Запечатанная магия","Опытная чародейка","Верховная чародейка"};
    String profileName(){return PROFILES[profile];}
    String independentLocation(int day,int hour){if(!"Свободная дорога".equals(ending))return "Хижина Анис";if(hour<8||hour>=20)return "Хижина Анис";switch(Math.floorMod(day,3)){case 0:return "Коллегия Винтерхолда";case 1:return "Солитьюд";default:return "Ривервуд";}}
    void beginBattle(){burn=slow=shock=0;}
    int ongoingDamage(int remaining){if(burn<=0||remaining<=0)return 0;burn--;return Math.min(remaining,3);}
    int weaken(int incoming){int reduction=0;if(slow>0){slow--;reduction+=3;}if(shock>0){shock--;reduction+=2;}return Math.min(Math.max(0,incoming-1),reduction);}
    void castEffect(int spell){if(spell==1)burn=2;else if(spell==2)slow=2;else if(spell==3)shock=2;else if(spell==4){burn=2;shock=2;}else if(spell==5)slow=2;}
    int maxHp(){return 100+10*(level-1)+gear*10;}
    int needXp(){return level*100;}
    int limitSpell(int rank){return rank<=0?0:rank==1?3:rank==2?4:5;}
    String spellName(){return SPELLS[spell];}
    String gearName(){return GEAR[gear]+" | "+FOCUS[focus];}
    String relation(KatherineStory k){return married?"Супруга":engaged?"Помолвка":k.romance?"Возлюбленная":k.active()?"Спутница":k.status();}
    void milestone(String event){for(int i=0;i<milestones.length();i++)if(event.equals(milestones.optString(i)))return;milestones.put(event);}
    void gain(int amount){if(amount<=0||level>=100)return;xp=GameEngine.nonnegativeAdd(xp,amount);while(level<100&&xp>=needXp()){xp-=needXp();level++;hp=maxHp();}if(level==100)xp=0;}
    void rest(int hours){hp=Math.min(maxHp(),hp+Math.max(0,Math.min(24,hours))*8);}
    void updateMood(KatherineStory k){mood=hurt>=60?"Обижена":hp<maxHp()/3?"Ослаблена":k.jealousy>=50?"Ревнует":k.romance&&love>=75?"Влюблена":k.trust>=70?"Доверяет":"Насторожена";}
    void endRomance(){engaged=false;married=false;}
    JSONObject save() throws JSONException{return new JSONObject().put("hp",hp).put("xp",xp).put("level",level).put("spell",spell).put("gear",gear).put("focus",focus).put("love",love).put("hurt",hurt).put("attachment",attachment).put("lastDate",lastDate).put("lastJoint",lastJoint).put("lastRepair",lastRepair).put("lastDamon",lastDamon).put("lastElisif",lastElisif).put("profile",profile).put("engaged",engaged).put("married",married).put("dragonEcho",dragonEcho).put("portalUsed",portalUsed).put("damonSafe",damonSafe).put("elisifMet",elisifMet).put("ending",ending).put("mood",mood).put("lastSpell",lastSpell).put("milestones",milestones).put("burn",burn).put("slow",slow).put("shock",shock).put("healthPotions",healthPotions).put("magicPotions",magicPotions).put("lastIndependentDay",lastIndependentDay).put("currentGoal",currentGoal).put("lastAiDay",lastAiDay).put("lastAiIntent",lastAiIntent).put("aiInitiatives",aiInitiatives);}
    static KatherineCompanion restore(JSONObject j,int rank,boolean romance){KatherineCompanion c=new KatherineCompanion();if(j==null)return c;
        c.level=Math.max(1,Math.min(100,j.optInt("level",1)));c.xp=Math.max(0,Math.min(c.needXp()-1,j.optInt("xp")));c.gear=Math.max(0,Math.min(2,j.optInt("gear")));c.focus=Math.max(0,Math.min(2,j.optInt("focus")));c.hp=Math.max(0,Math.min(c.maxHp(),j.optInt("hp",c.maxHp())));c.spell=Math.max(0,Math.min(c.limitSpell(rank),j.optInt("spell")));c.love=KatherineStory.clamp(j.optInt("love"));c.hurt=KatherineStory.clamp(j.optInt("hurt"));c.attachment=KatherineStory.clamp(j.optInt("attachment"));c.lastDate=j.optInt("lastDate",-1);c.lastJoint=j.optInt("lastJoint",-1);c.lastRepair=j.optInt("lastRepair",-1);c.lastDamon=j.optInt("lastDamon",-1);c.lastElisif=j.optInt("lastElisif",-1);c.profile=Math.max(0,Math.min(2,j.optInt("profile")));c.engaged=romance&&j.optBoolean("engaged");c.married=c.engaged&&j.optBoolean("married");c.dragonEcho=j.optBoolean("dragonEcho");c.portalUsed=j.optBoolean("portalUsed");c.damonSafe=j.optBoolean("damonSafe");c.elisifMet=j.optBoolean("elisifMet");c.ending=j.optString("ending","");c.mood=j.optString("mood","Спокойна");c.lastSpell=j.optString("lastSpell","");c.burn=Math.max(0,Math.min(2,j.optInt("burn")));c.slow=Math.max(0,Math.min(2,j.optInt("slow")));c.shock=Math.max(0,Math.min(2,j.optInt("shock")));c.healthPotions=Math.max(0,Math.min(99,j.optInt("healthPotions")));c.magicPotions=Math.max(0,Math.min(99,j.optInt("magicPotions")));c.lastIndependentDay=j.optInt("lastIndependentDay",-1);c.lastAiDay=j.optInt("lastAiDay",-1);c.lastAiIntent=j.optString("lastAiIntent","");c.aiInitiatives=j.optBoolean("aiInitiatives",true);c.currentGoal=j.optString("currentGoal","Узнать правду о Нулевом Пределе");JSONArray arr=j.optJSONArray("milestones");if(arr!=null)for(int i=0;i<Math.min(256,arr.length());i++)c.milestone(arr.optString(i));return c;}
    void appendChoices(GameEngine.Scene s,GameEngine g,KatherineStory k){
        if(k.active()){
            if(g.potions>0&&healthPotions<99)s.add("KATH:stockhp","Передать лечебное зелье в запас","У Кэтрин: "+healthPotions);
            if(g.manaPotions>0&&magicPotions<99)s.add("KATH:stockmana","Передать зелье магии в запас","У Кэтрин: "+magicPotions);
            if(k.companion.hp<k.companion.maxHp()&&g.potions>0)s.add("KATH:heal","Передать лечебное зелье","Лечение Кэтрин | −1 зелье");
            if(k.magicRank>0)s.add("KATH:spell","Выбрать заклинание",spellName());
            if(gear<2&&g.gold>=120*(gear+1))s.add("KATH:gear","Улучшить одеяние",120*(gear+1)+" септимов");
            if(focus<2&&g.gold>=100*(focus+1))s.add("KATH:focus","Улучшить магический фокус",100*(focus+1)+" септимов");
            if(k.magicRank>=3&&!dragonEcho&&g.progress.dragonStage>=4)s.add("KATH:echo","Исследовать драконий отклик","Не означает подтверждения драконьей души");
            if(k.magicRank>=3&&k.mana>=35&&!portalUsed&&!KatherineStory.atHut(g))s.add("KATH:portal","Стабилизировать пространство","Вернуться в хижину Анис | 35 магии");
        }
        if(k.romance&&lastDate!=g.day)s.add("KATH:date","Провести время вместе","Свидание и личный разговор");
        if(k.romance&&love>=65&&k.trust>=80&&hurt<25&&!engaged&&damonSafe)s.add("KATH:engage","Обсудить помолвку","Добровольное решение");
        if(k.romance&&engaged&&!married&&love>=85&&k.trust>=90&&hurt<15&&g.progress.location.contains("Рифтен"))s.add("KATH:wedding","Обсудить свадьбу в Рифтене","Оба согласны");
        if(hurt>0&&lastRepair!=g.day)s.add("KATH:apology","Искренне извиниться","Восстановить доверие");
        if(g.alma.active()&&k.active()&&lastJoint!=g.day)s.add("KATH:alma","Поговорить с Альмой вместе","Обсудить границы и сотрудничество");
        if(KatherineStory.atHut(g)&&k.stage>=4&&lastDamon!=g.day)s.add("KATH:damon","Поговорить с Деймоном","Узнать о его состоянии и намерениях");
        if(k.atPalace(g)&&k.active()&&k.stage>=6&&!elisifMet)s.add("KATH:elisif","Встретиться с Элисиф","Попросить помощи подруги Кэтрин");
        if(k.stage==10&&k.active())s.add("KATH:ending_free","Предложить свободу и независимость","Кэтрин выбирает собственную дорогу").add("KATH:ending_alliance","Предложить постоянный союз","Продолжить путешествие вместе");
    }
    boolean choose(GameEngine g,KatherineStory k,String action){
        switch(action){
            case "KATH:stockhp":if(k.active()&&g.potions>0&&healthPotions<99){g.potions--;healthPotions++;g.progress.sync(g);k.remember(g,"Кэтрин убрала лечебное зелье в собственную сумку.");}return true;
            case "KATH:stockmana":if(k.active()&&g.manaPotions>0&&magicPotions<99){g.manaPotions--;magicPotions++;g.progress.sync(g);k.remember(g,"Кэтрин убрала зелье магии в собственную сумку.");}return true;
            case "KATH:heal":if(k.active()&&hp<maxHp()&&g.potions>0){g.potions--;hp=Math.min(maxHp(),hp+40);g.progress.sync(g);k.remember(g,"Кэтрин приняла лечебное зелье.");}return true;
            case "KATH:spell":if(k.active()&&k.magicRank>0){spell=(spell+1)%(limitSpell(k.magicRank)+1);k.remember(g,"Кэтрин выбрала заклинание: "+spellName());}return true;
            case "KATH:gear":if(k.active()&&gear<2&&g.gold>=120*(gear+1)){g.gold-=120*(gear+1);gear++;hp=Math.min(maxHp(),hp+10);k.remember(g,"Кэтрин получила: "+GEAR[gear]);}return true;
            case "KATH:focus":if(k.active()&&focus<2&&g.gold>=100*(focus+1)){g.gold-=100*(focus+1);focus++;k.remember(g,"Кэтрин получила: "+FOCUS[focus]);}return true;
            case "KATH:echo":if(k.active()&&k.magicRank>=3&&!dragonEcho&&g.progress.dragonStage>=4){dragonEcho=true;milestone("Изучен драконий отклик");k.remember(g,"Кэтрин исследовала резонанс драконьей магии. Природа отклика не доказана.");}return true;
            case "KATH:portal":if(k.active()&&k.magicRank>=3&&!portalUsed&&k.mana>=35&&!KatherineStory.atHut(g)){WorldAtlas.Place p=WorldAtlas.find("Хижина Анис");if(p!=null){k.mana-=35;portalUsed=true;g.progress.location=p.name;g.region=p.region;try { g.sceneState.confirm("sublocation", "atlas_" + Integer.toHexString(p.name.hashCode()), p.name, "VISITED"); } catch (org.json.JSONException ignored) {} g.mode="WORLD";g.alma.observeTravel(g);k.observeTravel(g);k.remember(g,"Пространственный переход в хижину Анис.");}}return true;
            case "KATH:date":if(k.romance&&lastDate!=g.day){lastDate=g.day;love=KatherineStory.clamp(love+10);attachment=KatherineStory.clamp(attachment+6);k.affection=KatherineStory.clamp(k.affection+4);String[] scenes={"У огня Кэтрин рассказывает, как соскучилась по тихим вечерам. Она спрашивает, можно ли взять тебя за руку. Ты соглашаешься.","Кэтрин: Я привыкла защищать себя сама. Но рядом с тобой мне не приходится всегда быть настороже. Вы обмениваетесь нежным поцелуем.","Кэтрин предлагает прогулку вдали от шумных улиц. Вы обсуждаете планы, не давая обещаний, которых не сможете выполнить."};k.remember(g,scenes[Math.floorMod(g.day,scenes.length)]);}return true;
            case "KATH:engage":if(k.romance&&!engaged&&love>=65&&k.trust>=80&&hurt<25&&damonSafe){engaged=true;milestone("Помолвка");k.remember(g,"Кэтрин и герой по обоюдному согласию решили обручиться.");}return true;
            case "KATH:wedding":if(k.romance&&engaged&&!married&&love>=85&&k.trust>=90&&hurt<15&&g.progress.location.contains("Рифтен")){married=true;milestone("Свадьба");k.remember(g,"Кэтрин и герой заключили добровольный союз в храме Мары.");}return true;
            case "KATH:apology":if(hurt>0&&lastRepair!=g.day){lastRepair=g.day;hurt=KatherineStory.clamp(hurt-15);k.trust=KatherineStory.clamp(k.trust+4);k.remember(g,"Кэтрин выслушала извинение. Доверие возвращается постепенно.");}return true;
            case "KATH:alma":if(g.alma.active()&&k.active()&&lastJoint!=g.day){lastJoint=g.day;k.jealousy=KatherineStory.clamp(k.jealousy-8);g.alma.jealousy=AlmaStory.clamp(g.alma.jealousy-8);k.respect=KatherineStory.clamp(k.respect+3);g.alma.respect=AlmaStory.clamp(g.alma.respect+3);g.alma.remember("Альма и Кэтрин договорились уважать выбор друг друга.");k.remember(g,"Кэтрин: Не собираюсь превращать наши чувства в состязание. Альма: Тогда мы понимаем друг друга.");}return true;
            case "KATH:damon":if(KatherineStory.atHut(g)&&k.stage>=4&&lastDamon!=g.day){lastDamon=g.day;damonSafe=true;milestone("Деймон в безопасности");if(k.stage>=11)k.remember(g,"Деймон: Круг больше не определяет нашу жизнь. Кэтрин: Я всё ещё хочу знать, что произошло, но теперь сама выбираю, как искать ответы.");else k.remember(g,"Деймон: Спасибо, что помог. Кэтрин, не обещай мне того, чего не хочешь. Кэтрин: Я останусь верна собственному выбору.");}return true;
            case "KATH:elisif":if(k.atPalace(g)&&k.active()&&k.stage>=6&&!elisifMet){elisifMet=true;milestone("Встреча с Элисиф");k.respect=KatherineStory.clamp(k.respect+6);k.remember(g,"Элисиф приняла Кэтрин и предложила безопасное убежище, если Круг вернётся.");}return true;
            case "KATH:ending_free":case "KATH:ending_alliance":if(k.stage==10&&k.active()){ending=action.endsWith("free")?"Свободная дорога":"Союз двух миров";k.stage=11;k.respect=KatherineStory.clamp(k.respect+10);g.progress.setQuestOutcome(KatherineStory.FINALE,"COMPLETED");milestone(ending);if(action.endsWith("free")){k.following=false;g.mode="WORLD";}k.remember(g,"Кэтрин выбрала: "+ending);}return true;
            default:return false;
        }
    }
}
