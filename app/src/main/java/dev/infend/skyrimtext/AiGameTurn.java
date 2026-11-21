package dev.infend.skyrimtext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;

/** AI-controlled turn; validate every patch before committing a cloned GameEngine. */
public final class AiGameTurn {
    public static final class Option {
        public final String title,detail,skill,combatAction,companionAction;
        public final int chance;
        /** A streamed preview is selectable only after its entire JSON object closes. */
        public final boolean ready;
        Option(String t,String d,String s,int c,String a){this(t,d,s,c,a,"",true);}
        Option(String t,String d,String s,int c,String a,String n){this(t,d,s,c,a,n,true);}
        Option(String t,String d,String s,int c,String a,String n,boolean complete){
            title=t;detail=d;skill=s;companionAction=AiCompanionActions.code(n);
            chance=companionAction.isEmpty()?c:-1;combatAction=a;
            ready=complete&&t!=null&&t.trim().length()>=4&&d!=null&&d.trim().length()>=8;
        }
    }
    private static final SecureRandom DICE=new SecureRandom();
    public final String scene,journal;
    public final int customChance;
    public final ArrayList<Option> choices=new ArrayList<>();
    private final JSONObject effects;
    private AiGameTurn(String s,String j,int c,JSONObject e){scene=s;journal=j;customChance=c;effects=e;}
    private static String limited(String s,int n){
        if(s==null)return "";
        s=s.trim();return s.substring(0,Math.min(n,s.length()));
    }
    private static int probability(JSONObject o,String field)throws JSONException{
        if(!o.has(field)||o.isNull(field))return -1;
        Object raw=o.opt(field);
        if(!(raw instanceof Number))throw new JSONException("Шанс должен быть числом либо null");
        double n=((Number)raw).doubleValue();
        if(!Double.isFinite(n)||n<0||n>100||n!=Math.rint(n))throw new JSONException("Неверный шанс действия");
        return (int)n;
    }
    /** Model-only compatibility: accept explicit numeric strings/percentages and
     * known null aliases; never invent a chance or weaken effects validation. */
    private static void normalizeModelChance(JSONObject o,String key)throws JSONException{
        Object raw=o.opt(key);
        if(!(raw instanceof String))return;
        String value=((String)raw).trim().toLowerCase(java.util.Locale.ROOT);
        if(value.equals("null")||value.equals("без проверки")||value.equals("none")||
                value.equals("без броска")||value.equals("n/a")){
            o.put(key,JSONObject.NULL);return;
        }
        if(value.length()<=32&&value.matches("[+]?[0-9]{1,3}(?:\\.0+)?\\s*%?")){
            try{
                int n=new java.math.BigDecimal(value.replace("%","").trim()).intValueExact();
                if(n>=0&&n<=100)o.put(key,n);
            }catch(ArithmeticException|NumberFormatException ignored){}
        }
    }

    // Model-only normalization. Accept lossless integral strings, never round,
    // clamp, guess a reward, or interpret prose as a numeric effect.
    private static final String[] MODEL_DELTAS={
        "hpDelta","maxHpDelta","staminaDelta","maxStaminaDelta","magickaDelta",
        "maxMagickaDelta","goldDelta","xpDelta","hourDelta","dayDelta",
        "reputationDelta","attackDelta","armorDelta","potionsDelta","manaPotionsDelta",
        "questStageDelta","dragonStageDelta"
    };
    private static boolean absentModelValue(Object raw){
        if(raw==null||raw==JSONObject.NULL)return true;
        if(!(raw instanceof String))return false;
        String v=((String)raw).trim().toLowerCase(java.util.Locale.ROOT);
        return v.isEmpty()||v.equals("null")||v.equals("none")||v.equals("n/a")
            ||v.equals("без изменений")||v.equals("нет изменений");
    }
    private static void normalizeModelInt(JSONObject object,String key,boolean optional)throws JSONException{
        if(!object.has(key))return;
        Object raw=object.opt(key);
        if(optional&&absentModelValue(raw)){object.remove(key);return;}
        if(raw instanceof String){
            String v=((String)raw).trim().replace('\u2212','-');
            // A decimal representation is permitted only if exactly integral.
            if(v.length()<=32&&v.matches("[+-]?[0-9]+(?:\\.[0-9]+)?")){
                try{object.put(key,new java.math.BigDecimal(v).intValueExact());return;}
                catch(ArithmeticException|NumberFormatException ignored){}
            }
        }
        if(raw instanceof Number){
            try{object.put(key,new java.math.BigDecimal(raw.toString()).intValueExact());return;}
            catch(ArithmeticException|NumberFormatException ignored){}
        }
        throw new JSONException("Недопустимое числовое значение: "+key);
    }
    private static void normalizeOptionalModelField(JSONObject object,String key){
        if(object.has(key)&&absentModelValue(object.opt(key)))object.remove(key);
    }
    // Optional XP rewards must never make a completed model scene unplayable.
    // Drop invalid rewards instead of granting a maximum or guessing an amount.
    private static boolean normalizeModelXp(JSONObject object,String key,int limit)throws JSONException{
        try{
            normalizeModelInt(object,key,true);
            if(!object.has(key))return false;
            int amount=change(object,key,limit);
            if(amount>=0)return true;
        }catch(JSONException ignored){}
        object.remove(key);
        return false;
    }
    private static void normalizeModelEffects(JSONObject root)throws JSONException{
        normalizeOptionalModelField(root,"effects");
        JSONObject e=root.optJSONObject("effects");
        if(e==null)return; // parseInternal still rejects malformed non-object effects.
        for(String field:MODEL_DELTAS){
            if("xpDelta".equals(field))normalizeModelXp(e,field,10000);
            else normalizeModelInt(e,field,true);
        }
        for(String field:new String[]{"location","quest","questStatus","katherineIntent",
                "inventory","skills","enemy","sceneFacts","questUpdates","npcRewrites","equipmentChanges"})normalizeOptionalModelField(e,field);
        JSONArray inventory=e.optJSONArray("inventory");
        if(inventory!=null)for(int i=0;i<inventory.length();i++){
            JSONObject item=inventory.optJSONObject(i);
            if(item!=null)normalizeModelInt(item,"delta",true);
        }
        JSONArray skills=e.optJSONArray("skills");
        if(skills!=null){
            JSONArray valid=new JSONArray();
            HashSet<Integer> rewarded=new HashSet<>();
            for(int i=0;i<skills.length();i++){
                JSONObject skill=skills.optJSONObject(i);
                if(skill==null)throw new JSONException("Неверный навык");
                normalizeModelInt(skill,"index",false);
                Object rawIndex=skill.opt("index");
                if(!(rawIndex instanceof Number))throw new JSONException("Индекс навыка должен быть целым числом");
                int index=((Number)rawIndex).intValue();
                if(index<0||index>=RpgContent.SKILLS.length)throw new JSONException("Неверный индекс навыка");
                if(normalizeModelXp(skill,"xp",250)&&skill.optInt("xp")>0&&rewarded.add(index))
                    valid.put(skill);
            }
            e.put("skills",valid);
        }
        JSONObject enemy=e.optJSONObject("enemy");
        if(enemy!=null){
            normalizeModelInt(enemy,"hp",false);
            normalizeModelInt(enemy,"maxHp",true);
            normalizeModelInt(enemy,"damage",true);
        }
    }

    /** Strip model protocol accidentally emitted inside the story. Never apply
     * effects found in prose; only the validated top-level object is authoritative. */
    static String cleanScene(String text){
        if(text==null)return "";
        java.util.regex.Matcher m=java.util.regex.Pattern.compile(
            "(?iu)(?:^|\\s)[\"']?(?:effects|customChance|choices|journal)[\"']?\\s*(?:=|:)\\s*(?=\\{|\\[|[0-9]|null|\"|$)"
        ).matcher(text);
        return m.find()?text.substring(0,m.start()).trim():text;
    }

    /** Recover only a missing final root brace; all choices and effects still
     * undergo exactly the same validation. Never fabricate missing fields. */
    public static AiGameTurn parseLocalModel(String response,int lockedChance)throws JSONException{
        try{return parseModel(response,lockedChance);}
        catch(JSONException original){
            if(response==null)throw original;
            String tail=response.trim();
            if(!(tail.endsWith("}")||tail.endsWith("]")))throw original;
            try{
                JSONObject repaired=extractObject(tail+"}");
                if(!repaired.has("effects")||!repaired.has("journal")||!repaired.has("customChance"))throw original;
                return parseModel(repaired.toString(),lockedChance);
            }catch(JSONException ignored){throw original;}
        }
    }

    public static AiGameTurn parseModel(String response,int lockedChance)throws JSONException{
        if(response==null)throw new JSONException("Пустой ответ ИИ");
        JSONObject json=extractObject(response);
        json.put("scene",cleanScene(json.optString("scene","")));
        normalizeModelChance(json,"customChance");
        normalizeModelEffects(json);
        if(lockedChance!=-2){
            // The engine owns the chance. Missing echo or known null is not a new roll.
            if(json.has("customChance")&&!json.isNull("customChance")&&
                    probability(json,"customChance")!=lockedChance)
                throw new JSONException("ИИ изменил шанс после броска");
            json.put("customChance",lockedChance<0?JSONObject.NULL:lockedChance);
        }
        JSONArray options=json.optJSONArray("choices");
        if(options!=null)for(int i=0;i<options.length();i++){
            JSONObject o=options.optJSONObject(i);
            if(o!=null)normalizeModelChance(o,"chance");
        }
        return parseInternal(json.toString(),true);
    }

    /**
     * Recover a complete JSON object from fenced or interrupted model output.
     * Braces inside quoted narrative text do not terminate the object.
     * Never combine two unrelated objects into one candidate.
     */
    private static JSONObject extractObject(String response)throws JSONException{
        JSONException last=null;
        for(int start=response.indexOf('{');start>=0;start=response.indexOf('{',start+1)){
            int depth=0;boolean quoted=false,escaped=false;
            for(int i=start;i<response.length();i++){
                char c=response.charAt(i);
                if(quoted){
                    if(escaped)escaped=false;
                    else if(c=='\\')escaped=true;
                    else if(c=='"')quoted=false;
                }else if(c=='"')quoted=true;
                else if(c=='{')depth++;
                else if(c=='}'&&--depth==0){
                    try{
                        JSONObject candidate=new JSONObject(response.substring(start,i+1));
                        if(candidate.has("scene")&&candidate.has("choices"))return candidate;
                    }catch(JSONException ex){last=ex;}
                    break;
                }
            }
        }
        if(last!=null)throw last;
        throw new JSONException("ИИ не вернул полный игровой JSON");
    }
    public static AiGameTurn parse(String response)throws JSONException{
        return parseInternal(response,true);
    }
    private static AiGameTurn parseInternal(String response,boolean strictNew)throws JSONException{
        if(response==null)throw new JSONException("Пустой ответ ИИ");
        JSONObject json=extractObject(response);
        String rawScene=limited(json.optString("scene",""),30000);
        String scene=limited(cleanScene(rawScene),30000);
        JSONArray array=json.optJSONArray("choices");
        if((strictNew&&scene.length()<200)||scene.isEmpty())throw new JSONException("ИИ вернул слишком короткую сцену");
        if(array==null||array.length()!=10)throw new JSONException("Нужно ровно десять вариантов ответа");
        if(json.has("effects")&&!json.isNull("effects")&&!(json.opt("effects") instanceof JSONObject))
            throw new JSONException("Эффекты должны быть JSON-объектом");
        AiGameTurn turn=new AiGameTurn(scene,limited(json.optString("journal",""),500),
                probability(json,"customChance"),json.optJSONObject("effects"));
        HashSet<String> unique=new HashSet<>();
        for(int i=0;i<10;i++){
            JSONObject c=array.optJSONObject(i);
            if(c==null)throw new JSONException("Ошибка варианта "+(i+1));
            String title=limited(c.optString("title",""),110);
            String detail=limited(c.optString("detail",""),270);
            if(title.length()<4||detail.length()<8||!unique.add(strictNew?title.toLowerCase(java.util.Locale.ROOT).replaceAll("[\\p{Punct}\\s]+",""):title.toLowerCase(java.util.Locale.ROOT)))
                throw new JSONException("Действия пусты либо повторяются");
            turn.choices.add(new Option(title,detail,limited(c.optString("skill",""),65),
                    probability(c,"chance"),combatAction(c.optString("combatAction","")),c.optString("companionAction","")));
        }
        return turn;
    }
    private static String combatAction(String action){
        switch(action){case "ATTACK":case "HEAVY":case "MAGIC":case "DEFEND":case "POTION":case "FLEE":case "FIXGIFT:blood":case "FIXGIFT:time":case "FIXGIFT:balance":case "FIXGIFT:escape":return action;default:return "";}
    }
    /** Short, isolated AI request. The returned chance is locked before a roll. */
    public static int parseChance(String response)throws JSONException{
        if(response==null||response.length()>3000)throw new JSONException("Нет оценки шанса ИИ");
        String s=response.trim();
        int begin=s.indexOf('{'),end=s.lastIndexOf('}');
        if(begin<0||end<=begin)throw new JSONException("ИИ не прислал шанс JSON");
        JSONObject obj=new JSONObject(s.substring(begin,end+1));
        if(!obj.has("customChance"))throw new JSONException("В ответе нет customChance");
        normalizeModelChance(obj,"customChance");
        return probability(obj,"customChance");
    }
    public static int roll(){return DICE.nextInt(100)+1;}
    public static boolean success(int chance,int roll){return chance<0||roll<=chance;}
    public JSONObject save()throws JSONException{
        JSONObject out=new JSONObject().put("scene",scene).put("journal",journal);
        JSONArray array=new JSONArray();
        for(Option o:choices){
            JSONObject c=new JSONObject().put("title",o.title).put("detail",o.detail).put("skill",o.skill);
            c.put("chance",o.chance<0?JSONObject.NULL:o.chance);
            if(!o.combatAction.isEmpty())c.put("combatAction",o.combatAction);
            if(!o.companionAction.isEmpty())c.put("companionAction",o.companionAction);
            array.put(c);
        }
        return out.put("choices",array);
    }
    public static AiGameTurn restore(JSONObject json){
        if(json==null)return null;
        try{return parseInternal(json.toString(),false);}catch(Exception e){return null;}
    }
    /** Decode only the top-level scene string, including incomplete JSON fragments. */
    public static String previewScene(String partial){
        return cleanScene(AiScenePreview.read(partial));
    }

    /** Read choices directly from the accumulated JSON, even while title/detail are
     * being generated. Preview is deliberately read-only and never applies effects. */
    public static ArrayList<Option> previewChoices(String partial){
        ArrayList<Option> out=new ArrayList<>();
        if(partial==null)return out;
        int start=0, thought=partial.indexOf("<think>");
        if(thought>=0){
            int close=partial.indexOf("</think>",thought);
            if(close<0)return out;
            start=close+8;
        }
        start=partial.indexOf('{',start);
        if(start<0)return out;
        int array=previewField(partial,start,"choices");
        if(array<0||partial.charAt(array)!='[')return out;
        int i=previewSpace(partial,array+1);
        while(i<partial.length()&&out.size()<10&&partial.charAt(i)=='{'){
            int end=previewObjectEnd(partial,i);
            String raw=partial.substring(i,end<0?partial.length():end+1);
            String title=previewString(raw,"title"),detail=previewString(raw,"detail"),
                skill=previewString(raw,"skill");
            int chance=-1;String action="";
            if(end>=0){
                try{
                    JSONObject c=new JSONObject(raw);
                    normalizeModelChance(c,"chance");
                    chance=probability(c,"chance");
                    action=combatAction(c.optString("combatAction",""));
                }catch(JSONException ignored){}
            }
            if(!title.isEmpty()||!detail.isEmpty())
                out.add(new Option(limited(title,110),limited(detail,270),limited(skill,65),chance,action,"",end>=0));
            if(end<0)break;
            i=previewSpace(partial,end+1);
            if(i>=partial.length()||partial.charAt(i)!=',')break;
            i=previewSpace(partial,i+1);
        }
        return out;
    }
    private static int previewSpace(String s,int i){
        while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;
        return i;
    }
    private static int previewQuoteEnd(String s,int i){
        boolean escaped=false;
        for(int j=i+1;j<s.length();j++){
            char c=s.charAt(j);
            if(escaped)escaped=false;
            else if(c=='\\')escaped=true;
            else if(c=='"')return j;
        }
        return -1;
    }
    private static int previewObjectEnd(String s,int start){
        int depth=0;
        for(int i=start;i<s.length();i++){
            char c=s.charAt(i);
            if(c=='"'){
                i=previewQuoteEnd(s,i);
                if(i<0)return -1;
            }else if(c=='{')depth++;
            else if(c=='}'&&--depth==0)return i;
        }
        return -1;
    }
    /** Locate a direct object member, ignoring nested keys and quoted prose. */
    private static int previewField(String s,int start,String key){
        int depth=0;char previous=0;
        for(int i=start;i<s.length();i++){
            char c=s.charAt(i);
            if(Character.isWhitespace(c))continue;
            if(c=='"'){
                int end=previewQuoteEnd(s,i);
                if(end<0)return -1;
                if(depth==1&&(previous=='{'||previous==',')&&
                        s.substring(i+1,end).equals(key)){
                    int colon=previewSpace(s,end+1);
                    if(colon>=s.length()||s.charAt(colon)!=':')return -1;
                    int value=previewSpace(s,colon+1);
                    return value<s.length()?value:-1;
                }
                i=end;previous='"';continue;
            }
            if(c=='{'||c=='[')depth++;
            else if(c=='}'||c==']'){
                if(--depth==0)return -1;
            }
            previous=c;
        }
        return -1;
    }
    private static String previewString(String object,String key){
        int value=previewField(object,0,key);
        if(value<0||object.charAt(value)!='"')return "";
        // Reuse the tested incremental string decoder, including Unicode and escapes.
        return AiScenePreview.read("{\"scene\":"+object.substring(value));
    }

    private static int change(JSONObject e,String name,int limit)throws JSONException{
        if(!e.has(name))return 0;
        Object raw=e.opt(name);
        if(!(raw instanceof Number))throw new JSONException("Параметр не число: "+name);
        double v=((Number)raw).doubleValue();
        if(!Double.isFinite(v)||v!=Math.rint(v)||Math.abs(v)>limit)throw new JSONException("Недопустимое изменение "+name);
        return (int)v;
    }
    private static int requiredEnemyInt(JSONObject enemy,String key,int min,int max)throws JSONException{
        if(!enemy.has(key)) {
            if("hp".equals(key))throw new JSONException("Не указано здоровье противника");
            return "damage".equals(key)?1:min;
        }
        Object raw=enemy.opt(key);
        if(!(raw instanceof Number))throw new JSONException("Неверный параметр противника: "+key);
        double value=((Number)raw).doubleValue();
        if(!Double.isFinite(value)||value!=Math.rint(value)||value<min||value>max)
            throw new JSONException("Параметр противника вне диапазона: "+key);
        return (int)value;
    }
    private static int cap(long x,int min,int max){return (int)Math.max(min,Math.min(max,x));}
    private static int questReward(JSONObject quest,String key,int previous,int max)throws JSONException{
        if(!quest.has(key))return previous;
        Object raw=quest.opt(key);
        if(!(raw instanceof Number))throw new JSONException("Награда задания должна быть числом: "+key);
        double value=((Number)raw).doubleValue();
        if(!Double.isFinite(value)||value!=Math.rint(value)||value<0||value>max)
            throw new JSONException("Награда задания вне диапазона: "+key);
        return (int)value;
    }
    public void apply(GameEngine g,boolean acted)throws JSONException{
        if(!acted||effects==null)return;
        // Active encounters are owned exclusively by native combat.
        if(g.enemyHp>0)throw new JSONException("Сначала выполни боевое действие через движок");
        JSONObject e=effects;
        int maxHp=cap((long)g.maxHp+change(e,"maxHpDelta",50),1,Math.max(10000,g.maxHp));
        int maxSt=cap((long)g.maxStamina+change(e,"maxStaminaDelta",50),1,Math.max(10000,g.maxStamina));
        int maxMg=cap((long)g.maxMagicka+change(e,"maxMagickaDelta",50),1,Math.max(10000,g.maxMagicka));
        int hp=cap((long)g.hp+change(e,"hpDelta",500),0,maxHp);
        int st=cap((long)g.stamina+change(e,"staminaDelta",500),0,maxSt);
        int mg=cap((long)g.magicka+change(e,"magickaDelta",500),0,maxMg);
        int gold=cap((long)g.gold+change(e,"goldDelta",5000),0,Math.max(10000000,g.gold));
        int addHours=change(e,"hourDelta",24);
        if(addHours<0 || change(e,"dayDelta",30)<0)
            throw new JSONException("Игровое время нельзя переводить назад");
        int totalHours=g.hour+addHours;
        int nextHour=Math.floorMod(totalHours,24);
        int day=cap((long)g.day+change(e,"dayDelta",30)+Math.floorDiv(totalHours,24),1,1000000);
        int rep=cap((long)g.reputation+change(e,"reputationDelta",30),-100,100);
        int attack=cap((long)g.attack+change(e,"attackDelta",15),1,5000);
        int armor=cap((long)g.armor+change(e,"armorDelta",15),0,5000);
        int pots=cap((long)g.potions+change(e,"potionsDelta",20),0,9999);
        int manaPots=cap((long)g.manaPotions+change(e,"manaPotionsDelta",20),0,9999);
        int xp=change(e,"xpDelta",10000);
        if(xp<0)throw new JSONException("Опыт не может уменьшаться");
        String loc=limited(e.optString("location",""),80);
        WorldAtlas.Place place=loc.isEmpty()?null:WorldAtlas.find(loc);
        if(!loc.isEmpty()&&place==null)throw new JSONException("Неизвестная локация: "+loc);
        // A pending encounter cannot be escaped by an AI location patch.
        if(g.enemyHp>0 && place!=null && !place.name.equals(g.progress.location))
            throw new JSONException("Нельзя покинуть локацию во время боя");
        java.util.LinkedHashMap<String,Integer> inventoryChanges=new java.util.LinkedHashMap<>();
        if(e.has("inventory") && !e.isNull("inventory") && !(e.opt("inventory") instanceof JSONArray))
            throw new JSONException("Инвентарь должен быть массивом");
        JSONArray inv=e.optJSONArray("inventory");
        if(inv!=null){
            if(inv.length()>16)throw new JSONException("Слишком много предметов");
            java.util.LinkedHashMap<String,Integer> changes=new java.util.LinkedHashMap<>();
            for(int i=0;i<inv.length();i++){
                JSONObject item=inv.optJSONObject(i);
                if(item==null)throw new JSONException("Неверная запись инвентаря");
                String name=limited(item.optString("name",""),80);
                if("Септимы".equals(name)||"Лечебное зелье".equals(name)||"Зелье магии".equals(name))
                    throw new JSONException("Валюту и зелья меняют отдельные поля");
                if(ItemCatalog.find(name)==null)throw new JSONException("Предмет неизвестен: "+name);
                if(CompensationPowers.itemIndex(name)>=0)throw new JSONException("Системный предмет не изменяется ИИ");
                changes.put(name,changes.getOrDefault(name,0)+change(item,"delta",50));
            }
            double weight=g.progress.carryWeight();
            for(String name:changes.keySet()){
                int amount=g.progress.quantity(name)+changes.get(name);
                if(amount<0||amount>9999)throw new JSONException("Неверное количество "+name);
                weight+=changes.get(name)*ItemCatalog.find(name).weight;
            }
            if(weight>Math.max(g.progress.carryLimit(),g.progress.carryWeight())+0.0001 || weight < -0.0001)throw new JSONException("Недопустимый вес рюкзака");
            inventoryChanges.putAll(changes);
        }
        int q=change(e,"questStageDelta",1),d=change(e,"dragonStageDelta",1);
        if(q<0||d<0)throw new JSONException("Сюжетный этап нельзя откатывать");
        if(e.has("skills") && !e.isNull("skills") && !(e.opt("skills") instanceof JSONArray))
            throw new JSONException("Навыки должны быть массивом");
        JSONArray skills=e.optJSONArray("skills");
        if(skills!=null){
            if(skills.length()>18)throw new JSONException("Слишком много изменений навыков");
            for(int i=0;i<skills.length();i++){
                JSONObject sk=skills.optJSONObject(i);if(sk==null)throw new JSONException("Неверный навык");
                Object index=sk.opt("index");
                if(!(index instanceof Number))throw new JSONException("Индекс навыка должен быть целым числом");
                double numericIndex=((Number)index).doubleValue();
                int ix=sk.optInt("index",-1);
                if(!Double.isFinite(numericIndex)||numericIndex!=ix||ix<0||ix>=RpgContent.SKILLS.length||change(sk,"xp",250)<0)
                    throw new JSONException("Неверный навык или опыт");
            }
        }
        String quest=limited(e.optString("quest",""),180);
        String questStatus=e.optString("questStatus","ACTIVE");
        if(!quest.isEmpty()&&!("ACTIVE".equals(questStatus)||"COMPLETED".equals(questStatus)||"FAILED".equals(questStatus)))
            throw new JSONException("Неизвестный статус квеста");
        if(e.has("enemy") && !e.isNull("enemy") && !(e.opt("enemy") instanceof JSONObject))
            throw new JSONException("Противник должен быть объектом");
        JSONObject enemy=e.optJSONObject("enemy");
        if(enemy!=null){
            String name=limited(enemy.optString("name",""),70);
            int health=requiredEnemyInt(enemy,"hp",0,50000);
            if(health>0&&name.isEmpty())throw new JSONException("Противник без имени");
            if(g.enemyHp>0 && (health==0 || !name.equals(g.enemyName)))
                throw new JSONException("ИИ не может завершить или заменить незавершённый бой");
            if(health>0){
                requiredEnemyInt(enemy,"maxHp",health,50000);
                requiredEnemyInt(enemy,"damage",0,1000);
            }
        }
        // Persist only facts attached to a successfully resolved action. Validate
        // the complete batch against a detached ledger before changing the game.
        SceneState nextSceneState=null;
        if(e.has("sceneFacts")){
            if(!(e.opt("sceneFacts") instanceof JSONArray))
                throw new JSONException("Факты сцены должны быть массивом");
            JSONArray facts=e.getJSONArray("sceneFacts");
            if(facts.length()>12)throw new JSONException("Слишком много фактов сцены");
            nextSceneState=SceneState.fromJson(g.sceneState.toJson());
            nextSceneState.setContext(day,nextHour,place==null?g.progress.location:place.name);
            java.util.HashSet<String> factIds=new java.util.HashSet<>();
            for(int i=0;i<facts.length();i++){
                JSONObject fact=facts.optJSONObject(i);
                if(fact==null)throw new JSONException("Неверная запись факта сцены");
                String kind=fact.optString("kind","");
                String id=fact.optString("id","");
                String title=fact.optString("title","");
                String status=fact.optString("status","");
                if("npc".equals(kind)&&AiCompanionActions.protectedNpc(id,title))continue;
                if(!("npc".equals(kind)||"clue".equals(kind)||"sublocation".equals(kind)))
                    throw new JSONException("Недопустимый тип факта");
                if(!factIds.add(kind+":"+id))throw new JSONException("Повтор факта сцены");
                if("npc".equals(kind) &&
                    !("MET".equals(status)||"INFORMED".equals(status)||"HELPED".equals(status)||
                      "HOSTILE".equals(status)||"DEAD".equals(status)||"MISSING".equals(status)))
                    throw new JSONException("Недопустимое состояние NPC");
                if("clue".equals(kind) &&
                    !("FOUND".equals(status)||"EXAMINED".equals(status)||"USED".equals(status)))
                    throw new JSONException("Недопустимое состояние улики");
                if("sublocation".equals(kind) &&
                    !("DISCOVERED".equals(status)||"VISITED".equals(status)||"CLEARED".equals(status)))
                    throw new JSONException("Недопустимое состояние подлокации");
                nextSceneState.confirm(kind,id,title,status);
            }
        }
        // Relationship changes and resurrection of confirmed NPCs also run
        // against a detached SceneState copy: no partial mutation on errors.
        if(e.has("npcRewrites")){
            if(!(e.opt("npcRewrites") instanceof JSONArray))
                throw new JSONException("Изменения NPC должны быть массивом");
            JSONArray rewrites=e.getJSONArray("npcRewrites");
            if(rewrites.length()>8)throw new JSONException("Слишком много изменений NPC");
            if(nextSceneState==null)nextSceneState=SceneState.fromJson(g.sceneState.toJson());
            nextSceneState.setContext(day,nextHour,place==null?g.progress.location:place.name);
            java.util.HashSet<String> rewritten=new java.util.HashSet<>();
            for(int i=0;i<rewrites.length();i++){
                JSONObject rewrite=rewrites.optJSONObject(i);
                if(rewrite==null)throw new JSONException("Недопустимое изменение NPC");
                String id=rewrite.optString("id","");
                if(!id.matches("[a-zA-Z0-9_\\-]{1,48}")||!rewritten.add(id))
                    throw new JSONException("Неверный или повторный NPC ID");
                Object relationValue=rewrite.opt("relation");
                if(!(relationValue instanceof Number))throw new JSONException("Отношение NPC должно быть числом");
                double relationRaw=((Number)relationValue).doubleValue();
                if(!Double.isFinite(relationRaw)||relationRaw!=Math.rint(relationRaw)||
                        relationRaw<0||relationRaw>100)
                    throw new JSONException("Отношение NPC вне диапазона 0–100");
                if(rewrite.has("resurrect")&&!(rewrite.opt("resurrect") instanceof Boolean))
                    throw new JSONException("resurrect должен быть логическим значением");
                boolean resurrect=rewrite.optBoolean("resurrect",false);
                String npcTitle="";
                JSONArray known=nextSceneState.npcFacts();
                for(int n=0;n<known.length();n++){
                    JSONObject fact=known.optJSONObject(n);
                    if(fact!=null&&id.equals(fact.optString("id"))){
                        npcTitle=fact.optString("title");break;
                    }
                }
                if(npcTitle.isEmpty()||AiCompanionActions.protectedNpc(id,npcTitle))
                    throw new JSONException("NPC недоступен для изменения");
                if(!nextSceneState.fixNpc(id,(int)relationRaw,resurrect))
                    throw new JSONException("Нельзя применить изменение NPC");
            }
        }
        // Per-quest stage and objective updates are checked on a detached ledger.
        // No result is applied if any item is malformed or rolls back a quest.
        JSONObject validatedQuests=null;
        int questGoldReward=0,questXpReward=0;
        if(e.has("questUpdates")){
            if(!(e.opt("questUpdates") instanceof JSONArray))
                throw new JSONException("Обновления заданий должны быть массивом");
            JSONArray updates=e.getJSONArray("questUpdates");
            if(updates.length()>8)throw new JSONException("Не более восьми заданий за ход");
            validatedQuests=new JSONObject(g.progress.aiQuestRecords.toString());
            java.util.HashSet<String> seenQuests=new java.util.HashSet<>();
            for(int i=0;i<updates.length();i++){
                JSONObject update=updates.optJSONObject(i);
                if(update==null)throw new JSONException("Неверная запись задания");
                String id=update.optString("id","");
                String title=update.optString("title","").trim();
                String objective=update.optString("objective","").trim();
                String status=update.optString("status","ACTIVE");
                Object stageValue=update.opt("stage");
                if(!id.matches("[a-z0-9_]{1,48}")||!seenQuests.add(id)||
                        title.isEmpty()||title.length()>120||objective.length()>240||
                        !(stageValue instanceof Number))
                    throw new JSONException("Некорректное название, ID или стадия задания");
                double stageNumber=((Number)stageValue).doubleValue();
                if(!Double.isFinite(stageNumber)||stageNumber!=Math.rint(stageNumber)||
                        stageNumber<0||stageNumber>100)
                    throw new JSONException("Стадия задания должна быть числом 0–100");
                if(!("ACTIVE".equals(status)||"COMPLETED".equals(status)||"FAILED".equals(status)))
                    throw new JSONException("Неизвестный статус задания");
                JSONObject prior=validatedQuests.optJSONObject(id);
                if(prior==null&&validatedQuests.length()>=64)
                    throw new JSONException("Достигнут предел авторских заданий");
                if(prior!=null){
                    String previous=prior.optString("status","ACTIVE");
                    if(("COMPLETED".equals(previous)||"FAILED".equals(previous))&&
                            !previous.equals(status))
                        throw new JSONException("Нельзя переписать финал задания");
                    if(stageNumber<prior.optInt("stage",0))
                        throw new JSONException("Нельзя откатить стадию задания");
                }
                int rewardGold=questReward(update,"rewardGold",
                    prior==null?0:prior.optInt("rewardGold",0),5000);
                int rewardXp=questReward(update,"rewardXp",
                    prior==null?0:prior.optInt("rewardXp",0),10000);
                if(prior!=null&&((prior.has("rewardGold")&&rewardGold!=prior.optInt("rewardGold"))
                    ||(prior.has("rewardXp")&&rewardXp!=prior.optInt("rewardXp"))))
                    throw new JSONException("Нельзя менять обещанную награду задания");
                if("COMPLETED".equals(status)
                    &&(prior==null||!"COMPLETED".equals(prior.optString("status")))){
                    if(questGoldReward>5000-rewardGold||questXpReward>10000-rewardXp)
                        throw new JSONException("Слишком большая суммарная награда за ход");
                    questGoldReward+=rewardGold;questXpReward+=rewardXp;
                }
                validatedQuests.put(id,new JSONObject().put("title",title)
                    .put("objective",objective).put("stage",(int)stageNumber)
                    .put("status",status).put("rewardGold",rewardGold)
                    .put("rewardXp",rewardXp));
            }
        }
        ArrayList<AiEquipmentActions.Change> equipmentChanges=
            AiEquipmentActions.prepare(g,e,inventoryChanges);
        // Commit only after all model-controlled fields have passed validation.
        for(String name:inventoryChanges.keySet())
            g.progress.quantity(name,g.progress.quantity(name)+inventoryChanges.get(name));
        if(enemy!=null){
            int health=cap(enemy.optInt("hp",0),0,50000);
            if(health>0&&(!"COMBAT".equals(g.mode)||g.enemyHp<=0)){g.resetBattleVisuals();g.fixShop.gifts.beginBattle();g.fixShop.seals.beginBattle();g.katherine.companion.beginBattle();g.alma.beginBattle();}
            if(health<=0){g.poisonTurns=g.frostTurns=0;}
            g.enemyName=health>0?limited(enemy.optString("name",""),70):"";g.enemyHp=health;
            g.enemyMaxHp=health>0?requiredEnemyInt(enemy,"maxHp",health,50000):0;
            g.enemyDamage=health>0?requiredEnemyInt(enemy,"damage",0,1000):0;
            // Preserve native quest/dragon encounter identity and enable engine combat.
            if(health>0){
                if(g.battleKind.isEmpty() || !"COMBAT".equals(g.mode))g.battleKind="AI";
                g.mode="COMBAT";
            }else{
                g.battleKind="";
                if("COMBAT".equals(g.mode))g.mode="WORLD";
            }
        }
        g.maxHp=maxHp;g.hp=hp;g.maxStamina=maxSt;g.stamina=st;g.maxMagicka=maxMg;g.magicka=mg;
        g.gold=cap((long)gold+questGoldReward,0,Math.max(10000000,g.gold));
        g.day=day;g.hour=nextHour;g.reputation=rep;g.attack=attack;g.armor=armor;g.potions=pots;g.manaPotions=manaPots;
        if(place!=null){g.region=place.region;g.progress.location=place.name;}
        if(xp+questXpReward>0)g.addXp(xp+questXpReward);
        g.questStage=Math.min(4,g.questStage+q);
        g.progress.dragonStage=Math.min(DragonArc.FINAL_STAGE,g.progress.dragonStage+d);
        if(skills!=null)for(int i=0;i<skills.length();i++){
            JSONObject sk=skills.optJSONObject(i);
            g.progress.skillExperience(g,sk.optInt("index"),change(sk,"xp",250));
        }
        if(!quest.isEmpty()){
            String state=questStatus;
            g.aiJournal=quest+" | "+state;
            if("COMPLETED".equals(state)&&!g.progress.completed.contains(quest))g.progress.completed.add(quest);
            if("FAILED".equals(state)&&!g.progress.failed.contains(quest))g.progress.failed.add(quest);
        }
        if(validatedQuests!=null){
            java.util.Iterator<String> ids=validatedQuests.keys();
            while(ids.hasNext()){
                String id=ids.next();
                g.progress.aiQuestRecords.put(id,validatedQuests.getJSONObject(id));
            }
        }
        if(nextSceneState!=null)g.sceneState=nextSceneState;
        if(e.opt("katherineIntent") instanceof String)KatherineInitiative.apply(g,e.optString("katherineIntent"),acted);
        if(!journal.isEmpty()){g.progress.recent=journal;if(g.aiJournal.isEmpty())g.aiJournal=journal;}
        AiEquipmentActions.commit(g,equipmentChanges);
        g.progress.sync(g);
    }
}
