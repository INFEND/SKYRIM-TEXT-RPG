package dev.infend.skyrimtext;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Importable item vouchers use existing engine items only; no arbitrary executable effects. */
public final class FixShopCatalog {
    public static final class Offer {
        public final String item;public final int quantity,price;
        Offer(String item,int quantity,int price){this.item=item;this.quantity=quantity;this.price=price;}
        public String key(){return item+" / "+quantity+" / "+price;}
        public String title(){ItemCatalog.Item i=ItemCatalog.find(item);return (i==null?item:i.name)+(quantity>1?" ×"+quantity:"");}
    }
    public static boolean valid(Offer o){
        if(o==null||o.quantity<1||o.quantity>100||o.price<1||o.price>100000||o.item.length()>150)return false;
        ItemCatalog.Item i=ItemCatalog.find(o.item);
        return i!=null&&CompensationPowers.itemIndex(o.item)<0&&i.value>0&&
          ("Оружие".equals(i.category)||"Броня".equals(i.category)||"Украшения".equals(i.category)||"Одежда".equals(i.category)||"Боеприпасы".equals(i.category)||"Артефакты".equals(i.category))&&
          !"Походный клинок".equals(o.item)&&!"Дорожная одежда".equals(o.item);
    }
    public static ArrayList<Offer> featured(){
        ArrayList<Offer> a=new ArrayList<>();
        for(ItemCatalog.Item i:ItemCatalog.all()){
            if(a.size()>=24)break;
            String n=i.name.toLowerCase(java.util.Locale.ROOT);
            // Ordinary equipment only: quest artefacts keep their normal acquisition path.
            if(!("Оружие".equals(i.category)||"Броня".equals(i.category)||"Украшения".equals(i.category)))continue;
            if(!(n.startsWith("даэдр")||n.startsWith("эбонит")||n.startsWith("дракон")||n.startsWith("сталгрим")))continue;
            Offer o=new Offer(i.id,1,Math.max(50,Math.min(500,i.value/10)));if(valid(o))a.add(o);
        }return a;
    }
    public static ArrayList<Offer> parse(String json) throws Exception{
        if(json==null||json.length()>65536)throw new IllegalArgumentException("Слишком большой каталог");
        JSONArray a=new JSONArray(json);if(a.length()>100)throw new IllegalArgumentException("Не более 100 товаров");
        LinkedHashMap<String,Offer> out=new LinkedHashMap<>();
        for(int n=0;n<a.length();n++){JSONObject e=a.getJSONObject(n);Offer o=new Offer(e.getString("item"),e.getInt("quantity"),e.getInt("price"));
            if(!valid(o)||"Артефакты".equals(ItemCatalog.find(o.item).category))throw new IllegalArgumentException("Недоступный товар: "+o.item);
            if(out.put(o.key(),o)!=null)throw new IllegalArgumentException("Повтор товара: "+o.item);
        }return new ArrayList<>(out.values());
    }
    public static JSONArray encode(java.util.List<Offer> offers){JSONArray a=new JSONArray();for(Offer o:offers)try{a.put(new JSONObject().put("item",o.item).put("quantity",o.quantity).put("price",o.price));}catch(Exception ignored){}return a;}
    public static boolean redeem(GameEngine g,Offer o){
        return valid(o)&&g.progress.addItem(o.item,o.quantity);
    }
}
