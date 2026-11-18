package dev.infend.skyrimtext;
import org.json.JSONArray;
import org.json.JSONObject;

/** Separate merchant inventories and purses; no global merchant state is reused. */
final class SolitudeShops {
    static final String MODE="SOLITUDE:store:";
    static final String[] NAMES={"Сияющие одежды","Ароматы Анжелины","Всякая всячина","Лучник","Кузница Солитьюда"};
    static final String[] OWNERS={"Таари","Анжелина Моррар","Сайма","Фихада","Бейранд"};
    private static final String[][] ITEMS={
        {"Шляпа путника","Сапоги странника","Кожаные перчатки","Кольцо серебряное","Кольцо золотое"},
        {"Зелье запаса сил","Голубой горноцвет","Красный горноцвет","Пурпурный горноцвет","Соль"},
        {"Отмычка","Факел","Пшеница","Соль","Кожаные полоски"},
        {"Железный лук","Стальной лук","Железная стрела","Стальная стрела"},
        {"Железный слиток","Стальной слиток","Кожаные полоски","Железный меч","Стальной меч"}
    };
    private static final int[] FUNDS={500,500,700,600,800};
    private final int[][] sold=new int[NAMES.length][];
    private final int[] budget=FUNDS.clone();
    private final int[] cycle=new int[NAMES.length];
    SolitudeShops(){for(int i=0;i<sold.length;i++){sold[i]=new int[ITEMS[i].length];cycle[i]=-1;}}
    static int index(String mode){for(int i=0;i<NAMES.length;i++)if((MODE+i).equals(mode))return i;return -1;}
    static int district(int s){return s==4?5:0;}
    static boolean open(GameEngine g,int s){
        String status=g.sceneState.npcStatusByName(OWNERS[s]);
        return g.hour>=8&&g.hour<20&&!"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
    }
    private void refresh(GameEngine g,int s){
        int now=(Math.max(1,g.day)-1)/2;
        if(cycle[s]!=now){cycle[s]=now;java.util.Arrays.fill(sold[s],0);budget[s]=FUNDS[s];}
    }
    static int price(GameEngine g,ItemCatalog.Item item){
        return (int)Math.max(1,Math.min(Integer.MAX_VALUE,((long)item.value*Math.max(100,130-g.progress.skillLevel(12)/5)+99)/100));
    }
    static int capacity(String id){return id.endsWith("стрела")?20:id.endsWith("слиток")||id.equals("Отмычка")?8:3;}
    int stock(GameEngine g,int s,int i){refresh(g,s);return Math.max(0,capacity(ITEMS[s][i])-sold[s][i]);}
    private boolean canSell(GameEngine g,ItemCatalog.Item item,int s){
        return item!=null&&item.value>0&&g.progress.quantity(item.id)>0&&!g.progress.equipped(item.id)&&g.progress.temperQuality(item.id)==0&&!g.progress.artifacts.filled(item.id)
            &&!item.id.equals("Дорожная одежда")&&!item.id.equals("Походный клинок")&&!(g.swordUpgraded&&item.id.equals("Стальной меч"))&&!(g.armorUpgraded&&item.id.equals("Стальная кираса"))
            &&budget[s]>=g.salePrice(item)&&g.gold<=Integer.MAX_VALUE-g.salePrice(item);
    }
    GameEngine.Scene scene(GameEngine g,int s){
        refresh(g,s);
        GameEngine.Scene scene=new GameEngine.Scene("СОЛИТЬЮД | ТОРГОВЛЯ",NAMES[s],OWNERS[s]+" | Кошелёк: "+budget[s]+" септимов.\nПополнение через каждые два игровых дня.\n\n"+g.message);
        if(open(g,s))for(int i=0;i<ITEMS[s].length;i++){
            String id=ITEMS[s][i];ItemCatalog.Item item=ItemCatalog.find(id);if(item==null)continue;
            int n=stock(g,s,i),p=price(g,item);
            if(n>0&&g.gold>=p&&g.progress.carryWeight()+item.weight<=g.progress.carryLimit()+0.00001)
                scene.add("SOLITUDE:buy:"+s+":"+i,"Купить: "+item.name,p+" септимов | В наличии: "+n);
            if(canSell(g,item,s))scene.add("SOLITUDE:sell:"+s+":"+i,"Продать: "+item.name,g.salePrice(item)+" септимов | У тебя: "+g.progress.quantity(id));
        }
        else scene=new GameEngine.Scene(scene.kicker,scene.title,scene.body+"\n\nЛавка закрыта или торговец недоступен.");
        if(s==0)SolitudeCanonical.attireChoices(g,scene);
        if(s==1)SolitudeCanonical.shopChoices(g,scene);
        return scene.add("SOLITUDE:back","Вернуться на улицу","Покинуть лавку").add("GO_WORLD","Завершить прогулку","Вернуться к приключению");
    }
    void choose(GameEngine g,int s,String action){
        refresh(g,s);if(!open(g,s))return;
        for(int i=0;i<ITEMS[s].length;i++){
            String id=ITEMS[s][i];ItemCatalog.Item item=ItemCatalog.find(id);if(item==null)continue;
            if(action.equals("SOLITUDE:buy:"+s+":"+i)){
                int p=price(g,item);
                if(stock(g,s,i)<=0||g.gold<p||budget[s]>Integer.MAX_VALUE-p||!g.progress.addItem(id,1))return;
                sold[s][i]++;g.gold-=p;budget[s]+=p;g.message="Куплено: "+item.name+" | −"+p+" септимов.";
            }else if(action.equals("SOLITUDE:sell:"+s+":"+i)){
                if(!canSell(g,item,s)||!g.progress.removeItem(id,1))return;
                int p=g.salePrice(item);budget[s]-=p;g.gold+=p;sold[s][i]=Math.max(0,sold[s][i]-1);g.message="Продано: "+item.name+" | +"+p+" септимов.";
            }else continue;
            g.progress.recent=g.message;g.recordVisualEvent(NAMES[s]+": "+g.message);g.progress.sync(g);return;
        }
    }
    JSONObject save(){JSONObject o=new JSONObject();try{JSONArray rows=new JSONArray();for(int[] a:sold)rows.put(new JSONArray(a));o.put("sold",rows).put("budget",new JSONArray(budget)).put("cycle",new JSONArray(cycle));}catch(Exception ignored){}return o;}
    static SolitudeShops restore(JSONObject o){SolitudeShops s=new SolitudeShops();if(o==null)return s;JSONArray rows=o.optJSONArray("sold"),b=o.optJSONArray("budget"),c=o.optJSONArray("cycle");for(int i=0;i<NAMES.length;i++){if(b!=null)s.budget[i]=Math.max(0,b.optInt(i,FUNDS[i]));if(c!=null)s.cycle[i]=Math.max(-1,c.optInt(i,-1));JSONArray row=rows==null?null:rows.optJSONArray(i);if(row!=null)for(int k=0;k<s.sold[i].length;k++)s.sold[i][k]=Math.max(0,Math.min(capacity(ITEMS[i][k]),row.optInt(k,0)));}return s;}
}
