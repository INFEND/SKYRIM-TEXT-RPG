package dev.infend.skyrimtext;
import org.json.JSONArray;
import org.json.JSONObject;

/** Separate merchant inventories and purses; no global merchant state is reused. */
final class SolitudeShops {
    static final String MODE="SOLITUDE:store:";
    static final String[] NAMES={"Сияющие одежды","Ароматы Анжелины","Всякая всячина","Лучник","Кузница Солитьюда"};
    static final String[] OWNERS={"Таари","Анжелина Моррар","Сайма","Фихада","Бейранд"};
    private static final String[][] ITEMS={
        {"Шляпа путника","Сапоги странника","Кожаные перчатки","Кольцо серебряное","Кольцо золотое","Золотое ожерелье","Серебряное ожерелье","Диадема из камня","Плащ паломника"},
        {"Зелье запаса сил","Голубой горноцвет","Красный горноцвет","Пурпурный горноцвет","Соль","Лаванда","Чеснок","Снежные ягоды","Крыло голубой бабочки","Грибы имп","Корень Нирна","Жир тролля"},
        {"Отмычка","Факел","Пшеница","Соль","Кожаные полоски","Кожа","Гранат","Аметист","Рубин","Сапфир","Малый камень душ (заполненный)"},
        {"Железный лук","Стальной лук","Железная стрела","Стальная стрела","Длинный лук","Охотничий лук","Орочий лук","Двемерский лук","Эльфийский лук"},
        {"Железный слиток","Стальной слиток","Кожаные полоски","Железный меч","Стальной меч","Кожа","Корундовый слиток","Железный щит","Стальной щит","Железный шлем","Стальной шлем","Железная кираса","Стальная кираса","Железный боевой топор","Стальной боевой топор"}
    };
    private static final String[] TOPICS={
        "Для двора важны и ткань, и покрой. Но даже дорожная одежда должна хорошо сидеть.",
        "В моей лавке есть травы и зелья. Не всякий красивый цветок безопасен для алхимика.",
        "Путешественники часто забывают факелы и отмычки. Лучше проверить сумку до выхода из города.",
        "Лук выбирают по руке, а стрелы — по задаче. Следи за тетивой в сырую погоду.",
        "Прежде чем покупать клинок, проверь его баланс. Хорошая сталь не заменяет умения."
    };
    private static final String[] IDS={"taarie","angeline","sayma","fihada","beirand"};
    private static final int[] FUNDS={500,500,700,600,800};
    private final java.util.List<java.util.LinkedHashMap<String,Integer>> resale=new java.util.ArrayList<>();
    private final int[][] sold=new int[NAMES.length][];
    private final int[] budget=FUNDS.clone();
    private final int[] cycle=new int[NAMES.length];
    SolitudeShops(){for(int i=0;i<sold.length;i++){sold[i]=new int[ITEMS[i].length];cycle[i]=-1;resale.add(new java.util.LinkedHashMap<>());}}
    static int index(String mode){for(int i=0;i<NAMES.length;i++)if((MODE+i).equals(mode))return i;return -1;}
    static int district(int s){return s==4?5:0;}
    private static boolean onDuty(GameEngine g,int s){return s>=0&&s<NAMES.length&&g.hour>=8&&g.hour<20;}
    static boolean open(GameEngine g,int s){
        String status=g.sceneState.npcStatusByName(OWNERS[s]);
        return g.hour>=8&&g.hour<20&&!"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
    }
    private static String npcKey(GameEngine g,int s){
        JSONArray facts=g.sceneState.npcFacts();
        for(int i=0;i<facts.length();i++){JSONObject f=facts.optJSONObject(i);if(f!=null&&OWNERS[s].equalsIgnoreCase(f.optString("title")))return f.optString("id");}
        return "solitude_"+IDS[s];
    }
    private static JSONObject fact(GameEngine g,int s){
        String key=npcKey(g,s);JSONArray facts=g.sceneState.npcFacts();
        for(int i=0;i<facts.length();i++){JSONObject f=facts.optJSONObject(i);if(f!=null&&key.equals(f.optString("id")))return f;}
        return null;
    }
    private static void conversation(GameEngine g,int s,boolean memory){
        JSONObject f=fact(g,s);
        if(memory&&f==null)return;
        try{
            String key=npcKey(g,s);g.sceneState.setContext(g.day,g.hour,"Солитьюд");
            if(f==null)g.sceneState.confirm("npc",key,OWNERS[s],"MET");else if(!memory)g.sceneState.revisitNpc(key);
        }catch(org.json.JSONException ignored){}
        String line=memory?"Отношение: "+f.optInt("relation",50)+"/100 | Последняя встреча: день "+f.optInt("lastDay"):
            (f==null?"":"Снова встретились. ")+TOPICS[s];
        g.message=OWNERS[s]+": "+line;g.progress.recent=g.message;g.recordVisualEvent(g.message);g.progress.sync(g);
    }
    private void refresh(GameEngine g,int s){
        int now=(Math.max(1,g.day)-1)/2;
        if(cycle[s]!=now){cycle[s]=now;java.util.Arrays.fill(sold[s],0);budget[s]=FUNDS[s];resale.get(s).clear();}
    }
    static int price(GameEngine g,ItemCatalog.Item item){
        return (int)Math.max(1,Math.min(Integer.MAX_VALUE,((long)item.value*Math.max(100,130-g.progress.skillLevel(12)/5)+99)/100));
    }
    // Personal merchant terms use the same persistent NPC memory as dialogue and jobs.
    // Unmet merchants retain the historical price, preserving old saves and economy.
    private static int relation(GameEngine g,int s){
        JSONArray facts=g.sceneState.npcFacts();
        for(int i=0;i<facts.length();i++){
            JSONObject f=facts.optJSONObject(i);
            if(f!=null&&OWNERS[s].equalsIgnoreCase(f.optString("title")))
                return Math.max(0,Math.min(100,f.optInt("relation",50)));
        }
        return 50;
    }
    private static int purchasePrice(GameEngine g,int s,ItemCatalog.Item item){
        int r=relation(g,s);
        int percent=r>=75?90:r<=25?115:100;
        return (int)Math.max(1,Math.min(Integer.MAX_VALUE,((long)price(g,item)*percent+99)/100));
    }
    private static int salePrice(GameEngine g,int s,ItemCatalog.Item item){
        int r=relation(g,s);
        int percent=r>=75?110:r<=25?85:100;
        return (int)Math.max(1,Math.min(Integer.MAX_VALUE,((long)g.salePrice(item)*percent)/100));
    }
    static int capacity(String id){return id.endsWith("стрела")?20:id.endsWith("слиток")||id.equals("Отмычка")?8:3;}
    int stock(GameEngine g,int s,int i){refresh(g,s);return Math.max(0,capacity(ITEMS[s][i])-sold[s][i]);}
    private boolean canSell(GameEngine g,ItemCatalog.Item item,int s){
        return g.canTradeItem(item)
            &&budget[s]>=salePrice(g,s,item)&&g.gold<=Integer.MAX_VALUE-salePrice(g,s,item);
    }
    private int maxBuy(GameEngine g,int s,int i,ItemCatalog.Item item){return maxPurchase(g,s,item,stock(g,s,i));}
    private int maxPurchase(GameEngine g,int s,ItemCatalog.Item item,int available){
        if(g.progress.temperQuality(item.id)>0||g.progress.artifacts.filled(item.id)||(g.swordUpgraded&&"Стальной меч".equals(item.id))||(g.armorUpgraded&&"Стальная кираса".equals(item.id)))return 0;
        int p=purchasePrice(g,s,item);
        int weight=item.weight<=0?99:(int)Math.max(0,Math.floor((g.progress.carryLimit()-g.progress.carryWeight()+0.00001)/item.weight));
        return Math.max(0,Math.min(99,Math.min(available,Math.min(weight,Math.min(g.gold/p,Math.min((Integer.MAX_VALUE-budget[s])/p,Integer.MAX_VALUE-g.progress.quantity(item.id)))))));
    }
    private int maxSell(GameEngine g,int s,ItemCatalog.Item item){
        if(!canSell(g,item,s))return 0;
        int p=salePrice(g,s,item);if(p<=0)return 0;
        return Math.max(0,Math.min(99,Math.min(g.progress.quantity(item.id),Math.min(budget[s]/p,(Integer.MAX_VALUE-g.gold)/p))));
    }
    private static boolean extraAccepted(int s,ItemCatalog.Item item){
        if(item==null)return false;
        for(String id:ITEMS[s])if(id.equals(item.id))return false;
        String c=item.category;
        switch(s){
            case 0:return "Одежда".equals(c)||"Украшения".equals(c);
            case 1:return "Ингредиенты".equals(c);
            case 2:return "Материалы".equals(c)||"Драгоценности".equals(c)||"Инструменты".equals(c);
            case 3:return "Оружие".equals(c)&&(item.id.toLowerCase(java.util.Locale.ROOT).contains("лук")||item.id.toLowerCase(java.util.Locale.ROOT).contains("арбалет"));
            case 4:return "Броня".equals(c)||"Материалы".equals(c)||("Оружие".equals(c)&&!item.id.toLowerCase(java.util.Locale.ROOT).contains("лук")&&!item.id.toLowerCase(java.util.Locale.ROOT).contains("арбалет"));
            default:return false;
        }
    }
    private static void tradeChoices(GameEngine.Scene scene,String action,String title,int price,int max,String detail){
        if(max<1)return;
        scene.add(action,title,price+" септимов | "+detail);
        if(max>5)scene.add(action+":5",title+" ×5",(5L*price)+" септимов");
        if(max>1)scene.add(action+":max",title+" ×"+max,(long)max*price+" септимов | Максимум сейчас");
    }
    private static int amount(String action,String base,int max){
        if(action.equals(base))return max>=1?1:0;
        if(action.equals(base+":5"))return max>=5?5:0;
        if(action.equals(base+":max"))return max;
        return 0;
    }
    GameEngine.Scene scene(GameEngine g,int s){
        refresh(g,s);
        GameEngine.Scene scene=new GameEngine.Scene("СОЛИТЬЮД | ТОРГОВЛЯ",NAMES[s],OWNERS[s]+" | Кошелёк: "+budget[s]+" септимов.\nПополнение через каждые два игровых дня.\n\n"+g.message);
        if(onDuty(g,s)&&"HOSTILE".equals(g.sceneState.npcStatusByName(OWNERS[s])))
            scene.add("SOLITUDE:merchant:reconcile:"+s,"Примириться: "+OWNERS[s],"Авторское действие | 1 час | 10 сил | без награды");
        if(open(g,s)){
            JSONObject f=fact(g,s);
            scene.add("SOLITUDE:merchant:talk:"+s,"Поговорить: "+OWNERS[s],f==null?"Знакомство | авторский диалог":"Отношение: "+f.optInt("relation",50)+"/100");
            if(f!=null)scene.add("SOLITUDE:merchant:memory:"+s,"Вспомнить прошлую встречу","Общая память NPC");
        }
        if(open(g,s))for(int i=0;i<ITEMS[s].length;i++){
            String id=ITEMS[s][i];ItemCatalog.Item item=ItemCatalog.find(id);if(item==null)continue;
            int n=stock(g,s,i),p=price(g,item);
            tradeChoices(scene,"SOLITUDE:buy:"+s+":"+i,"Купить: "+item.name,p,maxBuy(g,s,i,item),"В наличии: "+n);
            tradeChoices(scene,"SOLITUDE:sell:"+s+":"+i,"Продать: "+item.name,salePrice(g,s,item),maxSell(g,s,item),"У тебя: "+g.progress.quantity(id));
        }
        // Keep the existing scene: rebuilding it here discards the reconciliation action.
        // The body already explains that the merchant is unavailable through the absence of trade choices.
        if(open(g,s))for(ItemCatalog.Item item:g.progress.owned("Все","Название"))
            if(extraAccepted(s,item))tradeChoices(scene,"SOLITUDE:extraSell:"+s+":"+item.id,"Продать: "+item.name,salePrice(g,s,item),maxSell(g,s,item),"У тебя: "+g.progress.quantity(item.id)+" | Скупка по специализации");
        if(open(g,s))for(java.util.Map.Entry<String,Integer> e:resale.get(s).entrySet()){
            ItemCatalog.Item item=ItemCatalog.find(e.getKey());if(item==null)continue;
            tradeChoices(scene,"SOLITUDE:rebuy:"+s+":"+item.id,"Выкупить: "+item.name,purchasePrice(g,s,item),maxPurchase(g,s,item,e.getValue()),"В наличии: "+e.getValue());
        }
        if(s==1||s==4)SolitudeCraft.choices(g,scene);
        if(s==0)SolitudeCanonical.attireChoices(g,scene);
        if(s==1)SolitudeCanonical.shopChoices(g,scene);
        return scene.add("SOLITUDE:back","Вернуться на улицу","Покинуть лавку").add("GO_WORLD","Завершить прогулку","Вернуться к приключению");
    }
    void choose(GameEngine g,int s,String action){
        refresh(g,s);
        if(action.equals("SOLITUDE:merchant:reconcile:"+s)){
            if(!onDuty(g,s)||!"HOSTILE".equals(g.sceneState.npcStatusByName(OWNERS[s])))return;
            if(g.hour>=23||g.stamina<10){g.message="Для примирения нужны один час и 10 единиц запаса сил.";return;}
            try{
                g.sceneState.setContext(g.day,g.hour,"Солитьюд");
                g.sceneState.confirm("npc",npcKey(g,s),OWNERS[s],"INFORMED");
            }catch(org.json.JSONException ignored){return;}
            g.stamina-=10;g.hour++;g.syncLivingSystems();
            g.message=OWNERS[s]+": Конфликт улажен. Торговля снова доступна.";
            g.progress.recent=g.message;g.recordVisualEvent(g.message);g.progress.sync(g);return;
        }
        if(!open(g,s))return;
        if(action.equals("SOLITUDE:merchant:talk:"+s)){conversation(g,s,false);return;}
        if(action.equals("SOLITUDE:merchant:memory:"+s)){conversation(g,s,true);return;}
        for(java.util.Map.Entry<String,Integer> e:resale.get(s).entrySet()){
            ItemCatalog.Item item=ItemCatalog.find(e.getKey());if(item==null)continue;
            int n=amount(action,"SOLITUDE:rebuy:"+s+":"+item.id,maxPurchase(g,s,item,e.getValue()));if(n<=0)continue;
            int total=purchasePrice(g,s,item)*n;if(!g.progress.addItem(item.id,n))return;
            if(n==e.getValue())resale.get(s).remove(item.id);else e.setValue(e.getValue()-n);
            g.gold-=total;budget[s]+=total;g.message="Выкуплено: "+item.name+" ×"+n+" | −"+total+" септимов.";
            g.progress.recent=g.message;g.recordVisualEvent(NAMES[s]+": "+g.message);g.progress.sync(g);return;
        }
        for(ItemCatalog.Item item:g.progress.owned("Все","Название")){
            if(!extraAccepted(s,item))continue;
            int n=amount(action,"SOLITUDE:extraSell:"+s+":"+item.id,maxSell(g,s,item));
            if(n<=0)continue;
            int total=salePrice(g,s,item)*n;
            if(!g.progress.removeItem(item.id,n))return;
            resale.get(s).merge(item.id,n,(a,b)->GameEngine.nonnegativeAdd(a,b));
            budget[s]-=total;g.gold+=total;g.message="Продано: "+item.name+" ×"+n+" | +"+total+" септимов.";
            g.progress.recent=g.message;g.recordVisualEvent(NAMES[s]+": "+g.message);g.progress.sync(g);return;
        }
        for(int i=0;i<ITEMS[s].length;i++){
            String id=ITEMS[s][i];ItemCatalog.Item item=ItemCatalog.find(id);if(item==null)continue;
            int buy=amount(action,"SOLITUDE:buy:"+s+":"+i,maxBuy(g,s,i,item));
            int sell=amount(action,"SOLITUDE:sell:"+s+":"+i,maxSell(g,s,item));
            if(buy>0){
                int total=purchasePrice(g,s,item)*buy;
                if(!g.progress.addItem(id,buy))return;
                sold[s][i]+=buy;g.gold-=total;budget[s]+=total;g.message="Куплено: "+item.name+" ×"+buy+" | −"+total+" септимов.";
            }else if(sell>0){
                int total=salePrice(g,s,item)*sell;
                if(!g.progress.removeItem(id,sell))return;
                budget[s]-=total;g.gold+=total;int returned=Math.min(sold[s][i],sell);sold[s][i]-=returned;int surplus=sell-returned;if(surplus>0)resale.get(s).merge(id,surplus,(x,y)->GameEngine.nonnegativeAdd(x,y));g.message="Продано: "+item.name+" ×"+sell+" | +"+total+" септимов.";
            }else continue;
            g.progress.recent=g.message;g.recordVisualEvent(NAMES[s]+": "+g.message);g.progress.sync(g);return;
        }
    }
    JSONObject save(){JSONObject o=new JSONObject();try{JSONArray rows=new JSONArray();for(int[] a:sold)rows.put(new JSONArray(a));JSONArray extra=new JSONArray();for(java.util.Map<String,Integer> m:resale)extra.put(new JSONObject(m));o.put("resale",extra);o.put("sold",rows).put("budget",new JSONArray(budget)).put("cycle",new JSONArray(cycle));}catch(Exception ignored){}return o;}
    static SolitudeShops restore(JSONObject o){SolitudeShops s=new SolitudeShops();if(o==null)return s;JSONArray extra=o.optJSONArray("resale");JSONArray rows=o.optJSONArray("sold"),b=o.optJSONArray("budget"),c=o.optJSONArray("cycle");for(int i=0;i<NAMES.length;i++){JSONObject m=extra==null?null:extra.optJSONObject(i);if(m!=null){java.util.Iterator<String> keys=m.keys();while(keys.hasNext()){String id=keys.next();int n=m.optInt(id,0);if(n>0&&(extraAccepted(i,ItemCatalog.find(id))||java.util.Arrays.asList(ITEMS[i]).contains(id)))s.resale.get(i).put(id,n);}}if(b!=null)s.budget[i]=Math.max(0,b.optInt(i,FUNDS[i]));if(c!=null)s.cycle[i]=Math.max(-1,c.optInt(i,-1));JSONArray row=rows==null?null:rows.optJSONArray(i);if(row!=null)for(int k=0;k<s.sold[i].length;k++)s.sold[i][k]=Math.max(0,Math.min(capacity(ITEMS[i][k]),row.optInt(k,0)));}return s;}
}
