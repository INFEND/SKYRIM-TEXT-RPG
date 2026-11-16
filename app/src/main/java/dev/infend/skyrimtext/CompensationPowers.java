package dev.infend.skyrimtext;
import org.json.*;
public final class CompensationPowers {
 public static final int SLOTS=12;
 public static final String[] NAMES={"Северная вспышка","Дыхание жизни","Щит сумерек"};
 public static final String[] ITEMS={"Клинок северной зари","Кираса сумеречного стража","Кольцо зимнего пламени"};
 public final int[] ranks=new int[SLOTS],discounts=new int[SLOTS],cooldown=new int[SLOTS],powerType=new int[SLOTS],basePower=new int[SLOTS],baseCost=new int[SLOTS],itemType=new int[SLOTS],itemStat=new int[SLOTS];
 public final double[] itemWeight=new double[SLOTS];
 public final String[] powerNames=new String[SLOTS],itemNames=new String[SLOTS],itemDescriptions=new String[SLOTS];
 public final boolean[] safe=new boolean[SLOTS],owned=new boolean[SLOTS],bound=new boolean[SLOTS],passive=new boolean[SLOTS];
 public CompensationPowers(){for(int i=0;i<SLOTS;i++){powerType[i]=itemType[i]=i%3;basePower[i]=30;baseCost[i]=25;itemStat[i]=i%3==0?24:i%3==1?32:0;itemWeight[i]=i%3==0?6:i%3==1?8:.2;powerNames[i]=i<3?NAMES[i]:"";itemNames[i]=i<3?ITEMS[i]:"";itemDescriptions[i]="";}}
 public static String itemId(int i){return i<3?ITEMS[i]:"fix_custom_"+i;}
 public static int itemIndex(String id){for(int i=0;i<SLOTS;i++)if(itemId(i).equals(id))return i;return -1;}
 public static int actionIndex(String action){if(action==null||!action.matches("FIX_POWER:(?:[0-9]|1[01])"))return -1;return Integer.parseInt(action.substring(10));}
 public String powerName(int i){return powerNames[i];}
 public String itemName(int i){return itemNames[i];}
 public int cost(int i){return Math.max(1,baseCost[i]-discounts[i]*5);}
 public int strength(int i){return basePower[i]+15*Math.max(0,ranks[i]-1);}
 public boolean available(GameEngine g,int i){return i>=0&&i<SLOTS&&ranks[i]>0&&cooldown[i]==0&&g.magicka>=cost(i)&&g.hp>(safe[i]?0:2)&&"COMBAT".equals(g.mode)&&g.enemyHp>0;}
 public void tick(){for(int i=0;i<SLOTS;i++)cooldown[i]=Math.max(0,cooldown[i]-1);}
 public String detail(int i){return (powerType[i]==0?"Урон ":powerType[i]==1?"Лечение ":"Урон и блокирование, сила ")+strength(i)+" | "+cost(i)+" магии | "+(safe[i]?"Без отдачи":"2 здоровья")+" | Откат 3 хода";}
 public boolean buyPower(int i){if(i<0||i>=3||ranks[i]!=0)return false;ranks[i]=1;return true;}
 private static String label(String s,int max){s=s==null?"":s.trim();return s.length()>max?s.substring(0,max):s;}
 public boolean createPower(String name,int type,int strength,int cost){name=label(name,60);if(name.isEmpty()||type<0||type>2||strength<1||strength>500||cost<1||cost>500)return false;for(int i=0;i<SLOTS;i++)if(ranks[i]>0&&name.equalsIgnoreCase(powerNames[i]))return false;for(int i=3;i<SLOTS;i++)if(ranks[i]==0){powerNames[i]=name;powerType[i]=type;basePower[i]=strength;baseCost[i]=cost;ranks[i]=1;return true;}return false;}
 public boolean upgrade(int i){if(ranks[i]<1||ranks[i]>=20)return false;ranks[i]++;return true;}
 public boolean reduce(int i){if(ranks[i]<1||cost(i)<=1)return false;discounts[i]++;return true;}
 public boolean removeSide(int i){if(ranks[i]<1||safe[i])return false;safe[i]=true;return true;}
 public boolean buyItem(GameEngine g,int i){if(i<0||i>=3||owned[i])return false;ItemCatalog.Item item=ItemCatalog.find(itemId(i));if(item==null||g.progress.carryWeight()+item.weight>50.00001)return false;owned[i]=true;g.progress.quantity(itemId(i),1);return true;}
 public boolean createItem(GameEngine g,String name,int type,int stat,double weight,String description){name=label(name,60);description=label(description,400);if(name.isEmpty()||type<0||type>2||stat<0||stat>500||!Double.isFinite(weight)||weight<.01||weight>50||g.progress.carryWeight()+weight>50.00001)return false;for(int i=0;i<SLOTS;i++)if(owned[i]&&name.equalsIgnoreCase(itemNames[i]))return false;for(int i=3;i<SLOTS;i++)if(!owned[i]){itemNames[i]=name;itemDescriptions[i]=description;itemType[i]=type;itemStat[i]=type==2?0:stat;itemWeight[i]=weight;owned[i]=true;register(i);g.progress.quantity(itemId(i),1);return true;}return false;}
 private void register(int i){ItemCatalog.registerCustom(itemId(i),itemNames[i],itemType[i],itemStat[i],itemWeight[i],itemDescriptions[i]);}
 public void installCatalog(){ItemCatalog.clearCustom();for(int i=3;i<SLOTS;i++)if(owned[i])register(i);}
 public boolean bind(int i){if(!owned[i]||bound[i])return false;bound[i]=true;return true;}
 public boolean addPassive(int i){if(!owned[i]||passive[i])return false;passive[i]=true;return true;}
 public boolean active(GameEngine g,int i){return owned[i]&&passive[i]&&g.progress.equipped(itemId(i));}
 public int passiveBonus(GameEngine g,int type){int n=0;for(int i=0;i<SLOTS;i++)if(itemType[i]==type&&active(g,i))n+=type==0?8:type==1?5:3;return n;}
 public String passiveLabel(int i){return new String[]{"+8 к атаке","−5 входящего урона","+3 к регенерации магии"}[itemType[i]];}
 public void reconcile(GameEngine g){for(int i=0;i<SLOTS;i++)g.progress.quantity(itemId(i),owned[i]?1:0);}
 public JSONObject toJson(){JSONObject j=new JSONObject();try{j.put("ranks",new JSONArray(ranks)).put("discounts",new JSONArray(discounts)).put("cooldown",new JSONArray(cooldown)).put("safe",new JSONArray(safe)).put("owned",new JSONArray(owned)).put("bound",new JSONArray(bound)).put("passive",new JSONArray(passive)).put("powerType",new JSONArray(powerType)).put("basePower",new JSONArray(basePower)).put("baseCost",new JSONArray(baseCost)).put("powerNames",new JSONArray(powerNames)).put("itemType",new JSONArray(itemType)).put("itemStat",new JSONArray(itemStat)).put("itemWeight",new JSONArray(itemWeight)).put("itemNames",new JSONArray(itemNames)).put("itemDescriptions",new JSONArray(itemDescriptions));}catch(Exception ignored){}return j;}
 public static CompensationPowers fromJson(JSONObject j){CompensationPowers c=new CompensationPowers();if(j==null)return c;for(int i=0;i<SLOTS;i++){c.ranks[i]=num(j,"ranks",i,0,20);c.discounts[i]=num(j,"discounts",i,0,100);c.cooldown[i]=num(j,"cooldown",i,0,4);c.safe[i]=flag(j,"safe",i);c.owned[i]=flag(j,"owned",i);c.bound[i]=c.owned[i]&&flag(j,"bound",i);c.passive[i]=c.owned[i]&&flag(j,"passive",i);if(i>=3){c.powerType[i]=num(j,"powerType",i,i%3,2);c.basePower[i]=Math.max(1,num(j,"basePower",i,30,500));c.baseCost[i]=Math.max(1,num(j,"baseCost",i,25,500));c.powerNames[i]=str(j,"powerNames",i,60);c.itemType[i]=num(j,"itemType",i,i%3,2);c.itemStat[i]=num(j,"itemStat",i,0,500);c.itemNames[i]=str(j,"itemNames",i,60);c.itemDescriptions[i]=str(j,"itemDescriptions",i,400);JSONArray w=j.optJSONArray("itemWeight");double weight=w==null?1:w.optDouble(i,1);c.itemWeight[i]=Double.isFinite(weight)?Math.max(.01,Math.min(50,weight)):1;if(c.powerNames[i].isEmpty())c.ranks[i]=0;if(c.itemNames[i].isEmpty())c.owned[i]=false;}}return c;}
 private static int num(JSONObject j,String k,int i,int fallback,int max){JSONArray a=j.optJSONArray(k);return a==null?fallback:Math.max(0,Math.min(max,a.optInt(i,fallback)));}
 private static String str(JSONObject j,String k,int i,int max){JSONArray a=j.optJSONArray(k);return a==null?"":label(a.optString(i,""),max);}
 private static boolean flag(JSONObject j,String k,int i){JSONArray a=j.optJSONArray(k);return a!=null&&a.optBoolean(i);}
}
