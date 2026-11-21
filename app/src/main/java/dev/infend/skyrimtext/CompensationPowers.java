package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Durable custom gear is part of the game save, not a transient AI message.
 * Keep 12 power slots and the first three legacy item IDs unchanged.
 */
public final class CompensationPowers {
    public static final int SLOTS=12;
    public static final int ITEM_SLOTS=64;
    public static final String[] NAMES={"Северная вспышка","Дыхание жизни","Щит сумерек"};
    public static final String[] ITEMS={"Клинок северной зари","Кираса сумеречного стража","Кольцо зимнего пламени"};

    public final int[] ranks=new int[SLOTS],discounts=new int[SLOTS],cooldown=new int[SLOTS],
            powerType=new int[SLOTS],basePower=new int[SLOTS],baseCost=new int[SLOTS];
    public final String[] powerNames=new String[SLOTS];
    public final boolean[] safe=new boolean[SLOTS];

    public final int[] itemType=new int[ITEM_SLOTS],itemStat=new int[ITEM_SLOTS],
            itemValue=new int[ITEM_SLOTS],itemRarity=new int[ITEM_SLOTS];
    public final double[] itemWeight=new double[ITEM_SLOTS];
    public final String[] itemNames=new String[ITEM_SLOTS],itemDescriptions=new String[ITEM_SLOTS],
            itemEffects=new String[ITEM_SLOTS],itemArtPrompts=new String[ITEM_SLOTS],
            itemArtPaths=new String[ITEM_SLOTS];
    public final boolean[] owned=new boolean[ITEM_SLOTS],bound=new boolean[ITEM_SLOTS],
            passive=new boolean[ITEM_SLOTS];

    public CompensationPowers(){
        for(int i=0;i<SLOTS;i++){
            powerType[i]=i%3;basePower[i]=30;baseCost[i]=25;
            powerNames[i]=i<3?NAMES[i]:"";
        }
        for(int i=0;i<ITEM_SLOTS;i++){
            itemType[i]=i%3;
            itemStat[i]=i%3==0?24:i%3==1?32:0;
            itemWeight[i]=i%3==0?6:i%3==1?8:.2;
            itemNames[i]=i<3?ITEMS[i]:"";
            itemDescriptions[i]="";itemEffects[i]="";itemArtPrompts[i]="";itemArtPaths[i]="";
        }
    }
    public static String itemId(int i){return i<3?ITEMS[i]:"fix_custom_"+i;}
    public static int itemIndex(String id){
        if(id==null)return -1;
        for(int i=0;i<ITEM_SLOTS;i++)if(itemId(i).equals(id))return i;
        return -1;
    }
    public static int actionIndex(String action){
        if(action==null||!action.matches("FIX_POWER:(?:[0-9]|1[01])"))return -1;
        return Integer.parseInt(action.substring(10));
    }
    public String powerName(int i){return powerNames[i];}
    public String itemName(int i){return itemNames[i];}
    public int cost(int i){return Math.max(1,baseCost[i]-discounts[i]*5);}
    public int strength(int i){return basePower[i]+15*Math.max(0,ranks[i]-1);}
    public boolean available(GameEngine g,int i){
        return i>=0&&i<SLOTS&&ranks[i]>0&&cooldown[i]==0&&g.magicka>=cost(i)
                &&g.hp>(safe[i]?0:2)&&"COMBAT".equals(g.mode)&&g.enemyHp>0;
    }
    public void tick(){for(int i=0;i<SLOTS;i++)cooldown[i]=Math.max(0,cooldown[i]-1);}
    public String detail(int i){
        return (powerType[i]==0?"Урон ":powerType[i]==1?"Лечение ":"Урон и блокирование, сила ")+
                strength(i)+" | "+cost(i)+" магии | "+(safe[i]?"Без отдачи":"2 здоровья")+" | Откат 3 хода";
    }
    public boolean buyPower(int i){if(i<0||i>=3||ranks[i]!=0)return false;ranks[i]=1;return true;}
    private static String label(String s,int max){return ItemWorkshopSpec.clip(s,max);}
    public boolean createPower(String name,int type,int strength,int cost){
        name=label(name,60);
        if(name.isEmpty()||type<0||type>2||strength<1||strength>500||cost<1||cost>500)return false;
        for(int i=0;i<SLOTS;i++)if(!powerNames[i].isEmpty()&&name.equalsIgnoreCase(powerNames[i]))return false;
        for(int i=3;i<SLOTS;i++)if(ranks[i]==0){
            powerNames[i]=name;powerType[i]=type;basePower[i]=strength;baseCost[i]=cost;ranks[i]=1;
            return true;
        }
        return false;
    }
    public boolean upgrade(int i){if(ranks[i]<1||ranks[i]>=20)return false;ranks[i]++;return true;}
    public boolean reduce(int i){if(ranks[i]<1||cost(i)<=1)return false;discounts[i]++;return true;}
    public boolean removeSide(int i){if(ranks[i]<1||safe[i])return false;safe[i]=true;return true;}
    public boolean buyItem(GameEngine g,int i){
        if(i<0||i>=3||owned[i])return false;
        ItemCatalog.Item item=ItemCatalog.find(itemId(i));
        if(item==null||g.progress.carryWeight()+item.weight>g.progress.carryLimit()+0.00001)return false;
        owned[i]=true;g.progress.quantity(itemId(i),1);return true;
    }
    // Kept for AI story save compatibility (older equipmentChanges schema).
    public boolean createItem(GameEngine g,String name,int type,int stat,double weight,String description){
        return createDetailedItem(g,name,type,stat,weight,0,0,description,"","","");
    }
    public boolean createDetailedItem(GameEngine g,String name,int type,int stat,double weight,
            int value,int rarity,String description,String effect,String artPrompt,String unused){
        name=label(name,60);description=label(description,400);
        effect=label(effect,400);artPrompt=label(artPrompt,900);
        if(name.isEmpty()||!ItemWorkshopSpec.validType(type)||stat<0||stat>500||
            !Double.isFinite(weight)||weight<.01||weight>50||value<0||value>999999||
            rarity<0||rarity>=ItemWorkshopSpec.RARITIES.length||
            g.progress.carryWeight()+weight>g.progress.carryLimit()+0.00001)return false;
        for(int i=0;i<ITEM_SLOTS;i++)if(!itemNames[i].isEmpty()&&name.equalsIgnoreCase(itemNames[i]))return false;
        for(int i=3;i<ITEM_SLOTS;i++)if(!owned[i]){
            itemNames[i]=name;itemDescriptions[i]=description;itemEffects[i]=effect;
            itemType[i]=type;itemStat[i]=ItemWorkshopSpec.normalizedStat(type,stat);
            itemWeight[i]=weight;itemValue[i]=value;itemRarity[i]=rarity;
            itemArtPrompts[i]=artPrompt;itemArtPaths[i]="";
            owned[i]=true;register(i);g.progress.quantity(itemId(i),1);return true;
        }
        return false;
    }
    public boolean updateCustomItem(GameEngine g,int i,String name,int type,int stat,double weight,String description){
        if(i<3||i>=ITEM_SLOTS)return false;
        return updateDetailedItem(g,i,name,type,stat,weight,itemValue[i],itemRarity[i],
                description,itemEffects[i],itemArtPrompts[i]);
    }
    public boolean updateDetailedItem(GameEngine g,int i,String name,int type,int stat,double weight,
            int value,int rarity,String description,String effect,String artPrompt){
        name=label(name,60);description=label(description,400);
        effect=label(effect,400);artPrompt=label(artPrompt,900);
        if(i<3||i>=ITEM_SLOTS||!owned[i]||name.isEmpty()||type!=itemType[i]||
                stat<0||stat>500||!Double.isFinite(weight)||weight<.01||weight>50||
                value<0||value>999999||rarity<0||rarity>=ItemWorkshopSpec.RARITIES.length)return false;
        for(int j=0;j<ITEM_SLOTS;j++)if(j!=i&&!itemNames[j].isEmpty()&&name.equalsIgnoreCase(itemNames[j]))return false;
        if(g.progress.carryWeight()-itemWeight[i]+weight>g.progress.carryLimit()+0.00001)return false;
        itemNames[i]=name;itemDescriptions[i]=description;itemEffects[i]=effect;
        itemStat[i]=ItemWorkshopSpec.normalizedStat(type,stat);itemWeight[i]=weight;
        itemValue[i]=value;itemRarity[i]=rarity;itemArtPrompts[i]=artPrompt;
        register(i);return true;
    }
    public void attachArtwork(int i,String artPath,String prompt){
        if(i<3||i>=ITEM_SLOTS||!owned[i])return;
        itemArtPaths[i]=label(artPath,180);itemArtPrompts[i]=label(prompt,900);
    }
    private void register(int i){
        ItemCatalog.registerCustom(itemId(i),itemNames[i],itemType[i],itemStat[i],
                itemWeight[i],itemValue[i],itemDescriptions[i],itemEffects[i]);
    }
    public void installCatalog(){
        ItemCatalog.clearCustom();
        for(int i=3;i<ITEM_SLOTS;i++)if(owned[i])register(i);
    }
    public boolean bind(int i){if(!owned[i]||bound[i])return false;bound[i]=true;return true;}
    public boolean addPassive(int i){if(!owned[i]||passive[i])return false;passive[i]=true;return true;}
    public boolean active(GameEngine g,int i){
        return owned[i]&&passive[i]&&g.progress.equipped(itemId(i));
    }
    private static int passiveGroup(int type){
        if(type==0||type==3||type==4||type==5||type==16)return 0;
        if(type==1||type==6||type==7||type==8||type==9)return 1;
        return 2;
    }
    public int passiveBonus(GameEngine g,int type){
        int n=0;
        for(int i=0;i<ITEM_SLOTS;i++)if(passiveGroup(itemType[i])==type&&active(g,i))
            n+=type==0?8:type==1?5:3;
        return n;
    }
    public String passiveLabel(int i){
        int group=passiveGroup(itemType[i]);
        return new String[]{"+8 к атаке","−5 входящего урона","+3 к регенерации магии"}[group];
    }
    public void reconcile(GameEngine g){
        for(int i=0;i<ITEM_SLOTS;i++)g.progress.quantity(itemId(i),owned[i]?1:0);
    }
    public JSONObject toJson(){
        JSONObject j=new JSONObject();
        try{
            j.put("ranks",new JSONArray(ranks)).put("discounts",new JSONArray(discounts))
                .put("cooldown",new JSONArray(cooldown)).put("safe",new JSONArray(safe))
                .put("owned",new JSONArray(owned)).put("bound",new JSONArray(bound))
                .put("passive",new JSONArray(passive)).put("powerType",new JSONArray(powerType))
                .put("basePower",new JSONArray(basePower)).put("baseCost",new JSONArray(baseCost))
                .put("powerNames",new JSONArray(powerNames)).put("itemType",new JSONArray(itemType))
                .put("itemStat",new JSONArray(itemStat)).put("itemWeight",new JSONArray(itemWeight))
                .put("itemNames",new JSONArray(itemNames))
                .put("itemDescriptions",new JSONArray(itemDescriptions))
                .put("itemValue",new JSONArray(itemValue)).put("itemRarity",new JSONArray(itemRarity))
                .put("itemEffects",new JSONArray(itemEffects))
                .put("itemArtPrompts",new JSONArray(itemArtPrompts))
                .put("itemArtPaths",new JSONArray(itemArtPaths));
        }catch(Exception ignored){}
        return j;
    }
    public static CompensationPowers fromJson(JSONObject j){
        CompensationPowers c=new CompensationPowers();if(j==null)return c;
        for(int i=0;i<SLOTS;i++){
            c.ranks[i]=num(j,"ranks",i,0,20);c.discounts[i]=num(j,"discounts",i,0,100);
            c.cooldown[i]=num(j,"cooldown",i,0,4);c.safe[i]=flag(j,"safe",i);
            if(i>=3){
                c.powerType[i]=num(j,"powerType",i,i%3,2);
                c.basePower[i]=Math.max(1,num(j,"basePower",i,30,500));
                c.baseCost[i]=Math.max(1,num(j,"baseCost",i,25,500));
                c.powerNames[i]=str(j,"powerNames",i,60);
                if(c.powerNames[i].isEmpty())c.ranks[i]=0;
            }
        }
        for(int i=0;i<ITEM_SLOTS;i++){
            c.owned[i]=flag(j,"owned",i);c.bound[i]=c.owned[i]&&flag(j,"bound",i);
            c.passive[i]=c.owned[i]&&flag(j,"passive",i);
            if(i>=3){
                c.itemType[i]=num(j,"itemType",i,i%3,ItemWorkshopSpec.TYPES.length-1);
                c.itemStat[i]=num(j,"itemStat",i,0,500);
                c.itemNames[i]=str(j,"itemNames",i,60);
                c.itemDescriptions[i]=str(j,"itemDescriptions",i,400);
                c.itemEffects[i]=str(j,"itemEffects",i,400);
                c.itemArtPrompts[i]=str(j,"itemArtPrompts",i,900);
                c.itemArtPaths[i]=str(j,"itemArtPaths",i,180);
                c.itemValue[i]=num(j,"itemValue",i,0,999999);
                c.itemRarity[i]=num(j,"itemRarity",i,0,ItemWorkshopSpec.RARITIES.length-1);
                JSONArray w=j.optJSONArray("itemWeight");
                double weight=w==null?1:w.optDouble(i,1);
                c.itemWeight[i]=Double.isFinite(weight)?Math.max(.01,Math.min(50,weight)):1;
                if(c.itemNames[i].isEmpty())c.owned[i]=false;
            }
        }
        return c;
    }
    private static int num(JSONObject j,String k,int i,int fallback,int max){
        JSONArray a=j.optJSONArray(k);
        return a==null?fallback:Math.max(0,Math.min(max,a.optInt(i,fallback)));
    }
    private static String str(JSONObject j,String k,int i,int max){
        JSONArray a=j.optJSONArray(k);
        return a==null?"":label(a.optString(i,""),max);
    }
    private static boolean flag(JSONObject j,String k,int i){
        JSONArray a=j.optJSONArray(k);return a!=null&&a.optBoolean(i);
    }
}
