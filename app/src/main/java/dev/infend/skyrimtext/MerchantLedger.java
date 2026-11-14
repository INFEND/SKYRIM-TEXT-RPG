package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local merchant economy; one ledger per region, refreshed every two game days. */
final class MerchantLedger {
    static final int RESTOCK_DAYS=2, INITIAL_GOLD=500, MAX_GOLD=100000;
    static final String[] KEYS={"HP","MP","SWORD","ARMOR","Отмычка","Факел","Железный слиток","Стальной слиток","Кожа","Кожаные полоски","Зелье запаса сил","Серебряный слиток","Корундовый слиток","Костная мука","Стальной болт","Двемерский металлический слиток","Хитиновая пластина","Сталгрим"};
    static final int[] INITIAL={5,4,1,1,12,5,6,4,3,8,3,2,3,4,12,3,3,1};
    static int capacity(int region,int cycle,int i){
        if(i<11)return INITIAL[i];
        if(i>=16)return region==9?INITIAL[i]:0;
        if(i==15)return region==4?INITIAL[i]:0;
        return (region+cycle+i)%3!=0?INITIAL[i]:0;
    }
    private static final class Shop {
        int cycle,gold=INITIAL_GOLD; final int[] stock=new int[KEYS.length];
        Shop(int cycle,int region){this.cycle=cycle;for(int i=0;i<stock.length;i++)stock[i]=capacity(region,cycle,i);}
    }
    private final Map<Integer,Shop> shops=new LinkedHashMap<>();
    private Shop at(int region,int day){
        int key=Math.max(0,Math.min(GameEngine.REGIONS.length-1,region)),cycle=(Math.max(1,day)-1)/RESTOCK_DAYS;
        Shop s=shops.get(key);if(s==null||s.cycle!=cycle){s=new Shop(cycle,key);shops.put(key,s);}return s;
    }
    private static int index(String key){for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return i;return -1;}
    int budget(int region,int day){return at(region,day).gold;}
    int stock(int region,int day,String key){int i=index(key);return i<0?0:at(region,day).stock[i];}
    int restockDay(int day){long next=((long)(Math.max(1,day)-1)/RESTOCK_DAYS+1)*RESTOCK_DAYS+1;return (int)Math.min(Integer.MAX_VALUE,next);}
    boolean purchase(int region,int day,String key,int price){
        int i=index(key);Shop s=at(region,day);if(i<0||price<=0||s.stock[i]<=0)return false;
        s.stock[i]--;s.gold=(int)Math.min(MAX_GOLD,(long)s.gold+price);return true;
    }
    boolean pay(int region,int day,int price){
        Shop s=at(region,day);if(price<=0||s.gold<price)return false;s.gold-=price;return true;
    }
    JSONObject toJson(){JSONObject out=new JSONObject();try{for(Map.Entry<Integer,Shop> e:shops.entrySet()){
        Shop s=e.getValue();out.put(String.valueOf(e.getKey()),new JSONObject().put("cycle",s.cycle).put("gold",s.gold).put("stock",new JSONArray(s.stock)));
    }}catch(Exception ignored){}return out;}
    static MerchantLedger fromJson(JSONObject obj){
        MerchantLedger ledger=new MerchantLedger();if(obj==null)return ledger;
        for(int i=0;i<GameEngine.REGIONS.length;i++){JSONObject value=obj.optJSONObject(String.valueOf(i));if(value==null)continue;
            Shop s=new Shop(Math.max(0,value.optInt("cycle",0)),i);s.gold=Math.max(0,Math.min(MAX_GOLD,value.optInt("gold",INITIAL_GOLD)));
            JSONArray stock=value.optJSONArray("stock");if(stock!=null)for(int n=0;n<KEYS.length;n++)s.stock[n]=Math.max(0,Math.min(capacity(i,s.cycle,n),stock.optInt(n,s.stock[n])));
            ledger.shops.put(i,s);
        }return ledger;
    }
}
