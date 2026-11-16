package dev.infend.skyrimtext;
import org.json.*;
public final class CompensationPowers {
 public static final String[] NAMES={"Северная вспышка","Дыхание жизни","Щит сумерек"};
 public static final String[] ITEMS={"Клинок северной зари","Кираса сумеречного стража","Кольцо зимнего пламени"};
 public final int[] ranks=new int[3],discounts=new int[3],cooldown=new int[3];
 public final boolean[] safe=new boolean[3],owned=new boolean[3],bound=new boolean[3],passive=new boolean[3];
 public static int itemIndex(String id){for(int i=0;i<ITEMS.length;i++)if(ITEMS[i].equals(id))return i;return -1;}
 public static int actionIndex(String action){if(action==null||!action.matches("FIX_POWER:[0-2]"))return -1;return action.charAt(action.length()-1)-'0';}
 public int cost(int i){return Math.max(5,25-discounts[i]*5);}
 public int strength(int i){return 15+15*ranks[i];}
 public boolean available(GameEngine g,int i){return i>=0&&i<3&&ranks[i]>0&&cooldown[i]==0&&g.magicka>=cost(i)&&g.hp>(safe[i]?0:2)&&"COMBAT".equals(g.mode)&&g.enemyHp>0;}
 public void tick(){for(int i=0;i<3;i++)cooldown[i]=Math.max(0,cooldown[i]-1);}
 public String detail(int i){return (i==0?"Урон ":i==1?"Лечение ":"Урон и блокирование, сила ")+strength(i)+" | "+cost(i)+" магии | "+(safe[i]?"Без отдачи":"2 здоровья")+" | Откат 3 хода";}
 public boolean buyPower(int i){if(i<0||i>=3||ranks[i]!=0)return false;ranks[i]=1;return true;}
 public boolean upgrade(int i){if(ranks[i]<1||ranks[i]>=20)return false;ranks[i]++;return true;}
 public boolean reduce(int i){if(ranks[i]<1||discounts[i]>=4)return false;discounts[i]++;return true;}
 public boolean removeSide(int i){if(ranks[i]<1||safe[i])return false;safe[i]=true;return true;}
 public boolean buyItem(GameEngine g,int i){if(i<0||i>=3||owned[i])return false;ItemCatalog.Item item=ItemCatalog.find(ITEMS[i]);if(item==null||g.progress.carryWeight()+item.weight>50.00001)return false;owned[i]=true;g.progress.quantity(ITEMS[i],1);return true;}
 public boolean bind(int i){if(!owned[i]||bound[i])return false;bound[i]=true;return true;}
 public boolean addPassive(int i){if(!owned[i]||passive[i])return false;passive[i]=true;return true;}
 public boolean active(GameEngine g,int i){return owned[i]&&passive[i]&&g.progress.equipped(ITEMS[i]);}
 public void reconcile(GameEngine g){for(int i=0;i<3;i++)g.progress.quantity(ITEMS[i],owned[i]?1:0);}
 public JSONObject toJson(){JSONObject j=new JSONObject();try{j.put("ranks",new JSONArray(ranks)).put("discounts",new JSONArray(discounts)).put("cooldown",new JSONArray(cooldown)).put("safe",new JSONArray(safe)).put("owned",new JSONArray(owned)).put("bound",new JSONArray(bound)).put("passive",new JSONArray(passive));}catch(Exception ignored){}return j;}
 public static CompensationPowers fromJson(JSONObject j){CompensationPowers c=new CompensationPowers();if(j==null)return c;for(int i=0;i<3;i++){c.ranks[i]=num(j,"ranks",i,20);c.discounts[i]=num(j,"discounts",i,4);c.cooldown[i]=num(j,"cooldown",i,4);c.safe[i]=flag(j,"safe",i);c.owned[i]=flag(j,"owned",i);c.bound[i]=c.owned[i]&&flag(j,"bound",i);c.passive[i]=c.owned[i]&&flag(j,"passive",i);}return c;}
 private static int num(JSONObject j,String k,int i,int max){JSONArray a=j.optJSONArray(k);return a==null?0:Math.max(0,Math.min(max,a.optInt(i)));}
 private static boolean flag(JSONObject j,String k,int i){JSONArray a=j.optJSONArray(k);return a!=null&&a.optBoolean(i);}
}
