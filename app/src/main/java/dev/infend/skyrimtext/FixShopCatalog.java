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
    static final int MAX_QUANTITY=100,MAX_PRICE=100000;
    static String category(ItemCatalog.Item i){
        switch(i.category){
            case "Оружие":return "Оружие";
            case "Броня":return "Броня";
            case "Одежда":case "Украшения":return "Внешность";
            case "Боеприпасы":return "Боеприпасы";
            case "Материалы":return "Материалы";
            case "Ингредиенты":return "Ингредиенты";
            case "Еда":case "Напитки":return "Провизия";
            case "Зелья":case "Яды":return "Зелья и яды";
            case "Инструменты":return "Инструменты";
            case "Драгоценности":case "Драгоценные камни":return "Драгоценности";
            default:return "";
        }
    }
    static int rarity(ItemCatalog.Item i){if(i.value>=2000)return 4;if(i.value>=600)return 3;if(i.value>=150)return 2;if(i.value>=40)return 1;return 0;}
    static int maxPacks(Offer o){return valid(o)?Math.min(MAX_QUANTITY/o.quantity,MAX_PRICE/o.price):0;}
    static Offer multiply(Offer o,int packs){if(packs<1||packs>maxPacks(o))return null;return new Offer(o.item,o.quantity*packs,o.price*packs);}
    static String productId(Offer o){return FixShopState.key(o.title(),o.price);}
    public static boolean valid(Offer o){
        if(o==null||o.quantity<1||o.quantity>100||o.price<1||o.price>100000||o.item==null||o.item.length()>150)return false;
        ItemCatalog.Item i=ItemCatalog.find(o.item);
        return i!=null&&CompensationPowers.itemIndex(o.item)<0&&i.value>0&&
          !category(i).isEmpty()&&!"Этериевый щит".equals(o.item)&&!"Этериевый посох".equals(o.item)&&!"Зачарованное ожерелье".equals(o.item)&&!"Ингредиенты Балбуса".equals(o.item)&&
          !"Походный клинок".equals(o.item)&&!"Дорожная одежда".equals(o.item)&&!"Стальной меч".equals(o.item)&&!"Стальная кираса".equals(o.item);
    }
    static final class Bundle {
        final String name;final int price;final String[] items;
        Bundle(String name,int price,String... items){this.name=name;this.price=price;this.items=items;}
        String description(){return String.join(", ",items);}
    }
    // Old bundle names and prices remain stable so pending saved vouchers are redeemable.
    static final Bundle[] LEGACY_BUNDLES={
        new Bundle("Облачение Чёрной крепости",650,"Эбонитовый меч","Эбонитовая кираса","Эбонитовый шлем","Эбонитовые перчатки","Эбонитовые сапоги"),
        new Bundle("Снаряжение Лунной стражи",450,"Стеклянный меч","Стеклянная кираса","Стеклянный шлем","Стеклянные перчатки","Стеклянные сапоги"),
        new Bundle("Клинки Обливиона",500,"Даэдрический меч","Даэдрический кинжал","Даэдрическая булава"),
        new Bundle("Северный охотник",200,"Кожаная кираса","Кожаный шлем","Кожаные сапоги","Стальной лук")
    };
    private static void bundle(java.util.List<Bundle> out,String name,String... items){
        long value=0;
        for(String id:items){
            ItemCatalog.Item item=ItemCatalog.find(id);
            if(item==null||item.value<=0)return;
            value+=item.value;
        }
        int price=(int)Math.min(200000L,Math.max(20L,value/8L));
        Bundle candidate=new Bundle(name,price,items);
        if(validBundle(candidate))out.add(candidate);
    }
    private static Bundle[] completeBundles(){
        ArrayList<Bundle> out=new ArrayList<>();
        java.util.Collections.addAll(out,LEGACY_BUNDLES);
        String[] base={"Железный","Стальной","Орочий","Двемерский","Эльфийский","Стеклянный","Эбонитовый","Даэдрический","Драконий","Нордский","Сталгримовый"};
        String[] female={"Железная","Стальная","Орочья","Двемерская","Эльфийская","Стеклянная","Эбонитовая","Даэдрическая","Драконья","Нордская резная","Сталгримовая"};
        String[] plural={"Железные","Стальные","Орочьи","Двемерские","Эльфийские","Стеклянные","Эбонитовые","Даэдрические","Драконьи","Нордские резные","Сталгримовые"};
        for(int i=0;i<base.length;i++){
            String noun=base[i],adjective=female[i].replace(" резная","");
            bundle(out,noun+" оружейный набор",
                noun+" меч",noun+" боевой топор",noun+" кинжал",
                adjective+" булава",noun+" двуручный меч",adjective+" секира",
                noun+" боевой молот",noun+" лук");
            String helmet=i==9?"Нордский резной шлем":noun+" шлем";
            if(i==10)bundle(out,noun+" комплект доспехов",helmet,female[i]+" кираса",plural[i]+" перчатки",plural[i]+" сапоги");
            else bundle(out,noun+" комплект доспехов",helmet,female[i]+" кираса",plural[i]+" перчатки",plural[i]+" сапоги",noun+" щит");
        }
        bundle(out,"Кожаный комплект доспехов","Кожаный шлем","Кожаная кираса","Кожаные наручи","Кожаные сапоги");
        bundle(out,"Комплект доспехов из шкур","Шлем из шкур","Кираса из шкур","Наручи из шкур","Сапоги из шкур","Щит из шкур");
        bundle(out,"Стальной пластинчатый комплект доспехов","Стальной пластинчатый шлем","Стальная пластинчатая кираса","Стальные пластинчатые перчатки","Стальные пластинчатые сапоги");
        bundle(out,"Драконий чешуйчатый комплект доспехов","Драконий чешуйчатый шлем","Драконья чешуйчатая кираса","Драконьи чешуйчатые перчатки","Драконьи чешуйчатые сапоги","Драконий чешуйчатый щит");
        bundle(out,"Костяной комплект доспехов","Костяной шлем","Костяная кираса","Костяные перчатки","Костяные сапоги","Костяной щит");
        bundle(out,"Тяжёлый хитиновый комплект доспехов","Тяжёлый хитиновый шлем","Тяжёлая хитиновая кираса","Тяжёлые хитиновые перчатки","Тяжёлые хитиновые сапоги");
        return out.toArray(new Bundle[0]);
    }
    static final Bundle[] BUNDLES=completeBundles();
    static boolean validBundle(Bundle b){
        if(b==null||b.price<1||b.items.length<2||b.items.length>12)return false;
        java.util.HashSet<String> unique=new java.util.HashSet<>();
        for(String id:b.items){
            ItemCatalog.Item item=ItemCatalog.find(id);
            if(!unique.add(id)||item==null||item.value<=0||
                    CompensationPowers.itemIndex(id)>=0||
                    !("Оружие".equals(item.category)||"Броня".equals(item.category)))return false;
        }
        return true;
    }
    static Bundle bundle(String name){for(Bundle b:BUNDLES)if(b.name.equals(name))return b;return null;}
    static boolean redeemBundle(GameEngine g,Bundle b){
        if(!validBundle(b))return false;
        double weight=0;for(String id:b.items){ItemCatalog.Item item=ItemCatalog.find(id);weight+=item.weight;if(g.progress.quantity(id)==Integer.MAX_VALUE)return false;}
        if(g.progress.carryWeight()+weight>g.progress.carryLimit()+0.00001)return false;
        for(String id:b.items)g.progress.quantity(id,g.progress.quantity(id)+1);return true;
    }
    static ArrayList<Offer> clothing(){
        ArrayList<Offer> offers=new ArrayList<>();for(ItemCatalog.Item i:ItemCatalog.all())if("Одежда".equals(i.category)||"Украшения".equals(i.category)){Offer o=new Offer(i.id,1,Math.max(10,Math.min(150,i.value/5)));if(valid(o)&&i.id.equals(i.name))offers.add(o);}return offers;
    }
    static String artPath(String title){return "media/shop/art_"+java.util.UUID.nameUUIDFromBytes(title.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-","")+".webp";}
    public static ArrayList<Offer> featured(){
        ArrayList<Offer> a=new ArrayList<>();
        for(ItemCatalog.Item i:ItemCatalog.all()){
            if(!i.id.equals(i.name)||category(i).isEmpty())continue;
            // Preserve the original price for high-tier goods already in existing saves.
            String n=i.name.toLowerCase(java.util.Locale.ROOT);
            boolean elite=("Оружие".equals(i.category)||"Броня".equals(i.category))&&(n.startsWith("даэдр")||n.startsWith("эбонит")||n.startsWith("дракон")||n.startsWith("сталгрим"));
            int price=elite?Math.max(50,Math.min(500,i.value/10)):"Внешность".equals(category(i))?Math.max(10,Math.min(150,i.value/5)):Math.max(1,Math.min(500,(i.value+9)/10));
            Offer o=new Offer(i.id,1,price);if(valid(o))a.add(o);
        }return a;
    }
    static boolean fits(GameEngine g,Offer o){
        if(!valid(o))return false;ItemCatalog.Item i=ItemCatalog.find(o.item);
        return g.progress.quantity(o.item)<=Integer.MAX_VALUE-o.quantity&&(i.weight<=0||g.progress.carryWeight()+i.weight*o.quantity<=g.progress.carryLimit()+0.00001);
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
        if(!fits(g,o))return false;
        // These inventory entries are mirrors of the engine counters.
        if("Лечебное зелье".equals(o.item)){if(g.potions>Integer.MAX_VALUE-o.quantity)return false;g.potions+=o.quantity;g.progress.sync(g);return true;}
        if("Зелье магии".equals(o.item)){if(g.manaPotions>Integer.MAX_VALUE-o.quantity)return false;g.manaPotions+=o.quantity;g.progress.sync(g);return true;}
        return g.progress.addItem(o.item,o.quantity);
    }
}
