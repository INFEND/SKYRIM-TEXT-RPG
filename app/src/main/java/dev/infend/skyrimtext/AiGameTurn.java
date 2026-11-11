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
        public final String title,detail,skill;
        public final int chance;
        Option(String t,String d,String s,int c){title=t;detail=d;skill=s;chance=c;}
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
        if(n<0||n>100||n!=Math.rint(n))throw new JSONException("Неверный шанс действия");
        return (int)n;
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
        if(response==null)throw new JSONException("Пустой ответ ИИ");
        JSONObject json=extractObject(response);
        String scene=limited(json.optString("scene",""),30000);
        JSONArray array=json.optJSONArray("choices");
        if(scene.length()<200)throw new JSONException("ИИ вернул слишком короткую сцену");
        if(array==null||array.length()!=10)throw new JSONException("Нужно ровно десять вариантов ответа");
        AiGameTurn turn=new AiGameTurn(scene,limited(json.optString("journal",""),500),
                probability(json,"customChance"),json.optJSONObject("effects"));
        HashSet<String> unique=new HashSet<>();
        for(int i=0;i<10;i++){
            JSONObject c=array.optJSONObject(i);
            if(c==null)throw new JSONException("Ошибка варианта "+(i+1));
            String title=limited(c.optString("title",""),110);
            String detail=limited(c.optString("detail",""),270);
            if(title.length()<4||detail.length()<8||!unique.add(title.toLowerCase(java.util.Locale.ROOT)))
                throw new JSONException("Действия пусты либо повторяются");
            turn.choices.add(new Option(title,detail,limited(c.optString("skill",""),65),
                    probability(c,"chance")));
        }
        return turn;
    }
    /** Short, isolated AI request. The returned chance is locked before a roll. */
    public static int parseChance(String response)throws JSONException{
        if(response==null||response.length()>3000)throw new JSONException("Нет оценки шанса ИИ");
        String s=response.trim();
        int begin=s.indexOf('{'),end=s.lastIndexOf('}');
        if(begin<0||end<=begin)throw new JSONException("ИИ не прислал шанс JSON");
        JSONObject obj=new JSONObject(s.substring(begin,end+1));
        if(!obj.has("customChance"))throw new JSONException("В ответе нет customChance");
        return probability(obj,"customChance");
    }
    public static int roll(){return DICE.nextInt(100)+1;}
    public static boolean success(int chance,int roll){return chance<0||roll<=chance;}
    public JSONObject save()throws JSONException{
        JSONObject out=new JSONObject().put("scene",scene).put("journal",journal);
        JSONArray array=new JSONArray();
        for(Option o:choices){
            JSONObject c=new JSONObject().put("title",o.title).put("detail",o.detail).put("skill",o.skill);
            c.put("chance",o.chance<0?JSONObject.NULL:o.chance);array.put(c);
        }
        return out.put("choices",array);
    }
    public static AiGameTurn restore(JSONObject json){
        if(json==null)return null;
        try{return parse(json.toString());}catch(Exception e){return null;}
    }
    /** Render only the scene string as it arrives; never show raw JSON to player. */
    public static String previewScene(String partial){
        if(partial==null)return "";
        int pos=partial.indexOf("\"scene\"");
        if(pos<0)return "";
        pos=partial.indexOf(':',pos+7);if(pos<0)return "";
        pos=partial.indexOf('"',pos+1);if(pos<0)return "";
        StringBuilder out=new StringBuilder();boolean esc=false;
        for(int i=pos+1;i<partial.length()&&out.length()<30000;i++){
            char c=partial.charAt(i);
            if(esc){
                if(c=='n')out.append('\n');else if(c=='t')out.append(' ');
                else if(c=='"')out.append('"');else if(c=='\\')out.append('\\');
                else if(c=='u'&&i+4<partial.length()){
                    try{out.append((char)Integer.parseInt(partial.substring(i+1,i+5),16));i+=4;}
                    catch(Exception ignored){break;}
                }esc=false;
            }else if(c=='\\')esc=true;
            else if(c=='"')break;
            else out.append(c);
        }
        return out.toString();
    }
    private static int change(JSONObject e,String name,int limit)throws JSONException{
        if(!e.has(name))return 0;
        Object raw=e.opt(name);
        if(!(raw instanceof Number))throw new JSONException("Параметр не число: "+name);
        double v=((Number)raw).doubleValue();
        if(v!=Math.rint(v)||Math.abs(v)>limit)throw new JSONException("Недопустимое изменение "+name);
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
    public void apply(GameEngine g,boolean acted)throws JSONException{
        if(!acted||effects==null)return;
        JSONObject e=effects;
        int maxHp=cap((long)g.maxHp+change(e,"maxHpDelta",50),1,10000);
        int maxSt=cap((long)g.maxStamina+change(e,"maxStaminaDelta",50),1,10000);
        int maxMg=cap((long)g.maxMagicka+change(e,"maxMagickaDelta",50),1,10000);
        int hp=cap((long)g.hp+change(e,"hpDelta",500),0,maxHp);
        int st=cap((long)g.stamina+change(e,"staminaDelta",500),0,maxSt);
        int mg=cap((long)g.magicka+change(e,"magickaDelta",500),0,maxMg);
        int gold=cap((long)g.gold+change(e,"goldDelta",5000),0,10000000);
        int addHours=change(e,"hourDelta",24);
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
                changes.put(name,changes.getOrDefault(name,0)+change(item,"delta",50));
            }
            double weight=g.progress.carryWeight();
            for(String name:changes.keySet()){
                int amount=g.progress.quantity(name)+changes.get(name);
                if(amount<0||amount>9999)throw new JSONException("Неверное количество "+name);
                weight+=changes.get(name)*ItemCatalog.find(name).weight;
            }
            if(weight>50.0001 || weight < -0.0001)throw new JSONException("Недопустимый вес рюкзака");
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
                int ix=sk.optInt("index",-1);
                if(ix<0||ix>=RpgContent.SKILLS.length||change(sk,"xp",250)<0)
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
            java.util.HashSet<String> factIds=new java.util.HashSet<>();
            for(int i=0;i<facts.length();i++){
                JSONObject fact=facts.optJSONObject(i);
                if(fact==null)throw new JSONException("Неверная запись факта сцены");
                String kind=fact.optString("kind","");
                String id=fact.optString("id","");
                String title=fact.optString("title","");
                String status=fact.optString("status","");
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
        // Commit only after all model-controlled fields have passed validation.
        for(String name:inventoryChanges.keySet())
            g.progress.quantity(name,g.progress.quantity(name)+inventoryChanges.get(name));
        if(enemy!=null){
            int health=cap(enemy.optInt("hp",0),0,50000);
            g.enemyName=health>0?limited(enemy.optString("name",""),70):"";g.enemyHp=health;
            g.enemyMaxHp=health>0?requiredEnemyInt(enemy,"maxHp",health,50000):0;
            g.enemyDamage=health>0?requiredEnemyInt(enemy,"damage",0,1000):0;
            g.battleKind=health>0?"AI":"";
        }
        g.maxHp=maxHp;g.hp=hp;g.maxStamina=maxSt;g.stamina=st;g.maxMagicka=maxMg;g.magicka=mg;
        g.gold=gold;g.day=day;g.hour=nextHour;g.reputation=rep;g.attack=attack;g.armor=armor;g.potions=pots;g.manaPotions=manaPots;
        if(place!=null){g.region=place.region;g.progress.location=place.name;}
        if(xp>0)g.addXp(xp);
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
        if(nextSceneState!=null)g.sceneState=nextSceneState;
        if(!journal.isEmpty()){g.progress.recent=journal;if(g.aiJournal.isEmpty())g.aiJournal=journal;}
        g.progress.sync(g);
    }
}
