package dev.infend.skyrimtext;
import org.json.JSONObject;

/** Local improvements share the existing residence ownership bit, not a second deed. */
final class SolitudeHome {
    static final String MODE="SOLITUDE:home";
    static final int PRICE=25000, HOUSE=4;
    private static final String[] ROOMS={"Спальня","Кухня","Гостиная","Алхимическая лаборатория","Лаборатория зачарования"};
    private static final int[] COST={2000,1500,2000,2500,2500};
    private int rooms,lastMeal=-1,lastStudy=-1;
    // Only fungible, non-quest goods; equipment and engine-backed counters are excluded.
    private final JSONObject chest=new JSONObject();
    private static final String[] GOODS={"Железный слиток","Стальной слиток","Кожаные полоски","Пшеница","Голубой горноцвет","Красный горноцвет","Пурпурный горноцвет","Соль","Факел","Отмычка","Железная стрела","Стальная стрела","Серебряный слиток","Золотой слиток","Эбонитовый слиток","Ртутный слиток","Двемерский металлический слиток","Орихалковый слиток","Корундовый слиток","Кожа","Лунный сахар","Стальной болт"};
    private static final int LIMIT=10000;
    int stored(String id){return chest.optInt(id,0);}
    private void setStored(String id,int n){try{if(n==0)chest.remove(id);else chest.put(id,n);}catch(Exception e){throw new IllegalStateException(e);}}
    private boolean transfer(GameEngine g,String a){
        for(int i=0;i<GOODS.length;i++){
            String id=GOODS[i];int n=stored(id);
            if(a.equals("SOLITUDE:home:deposit:"+i)){
                if(n<LIMIT&&g.progress.removeItem(id,1)){setStored(id,n+1);report(g,"В сундук: "+id);}
                return true;
            }
            if(a.equals("SOLITUDE:home:depositAll:"+i)){
                int amount=Math.min(LIMIT-n,g.progress.quantity(id));
                if(amount>0&&g.progress.removeItem(id,amount)){setStored(id,n+amount);report(g,"В сундук: "+id+" ×"+amount);}
                return true;
            }
            if(a.equals("SOLITUDE:home:withdrawAll:"+i)){
                ItemCatalog.Item item=ItemCatalog.find(id);if(item==null)return true;
                int capacity=item.weight<=0?n:(int)Math.max(0,Math.floor((g.progress.carryLimit()-g.progress.carryWeight()+0.00001)/item.weight));
                int amount=Math.min(n,Math.min(capacity,Integer.MAX_VALUE-g.progress.quantity(id)));
                if(amount>0&&g.progress.addItem(id,amount)){setStored(id,n-amount);report(g,"Из сундука: "+id+" ×"+amount);}
                return true;
            }
            if(a.equals("SOLITUDE:home:withdraw:"+i)){
                if(n>0&&g.progress.addItem(id,1)){setStored(id,n-1);report(g,"Из сундука: "+id);}
                return true;
            }
        }
        return false;
    }
    static boolean owns(GameEngine g){return g.fixShop.home.owns(HOUSE);}
    static boolean steward(GameEngine g){
        String status=g.sceneState.npcStatusByName("Фолк Огнебород");
        return g.hour>=9&&g.hour<17&&!"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
    }
    static boolean eligible(GameEngine g){return g.holdReputation[3]>=10;}
    boolean room(int i){return (rooms&(1<<i))!=0;}
    void palace(GameEngine g,GameEngine.Scene s){
        if(!steward(g))return;
        if(!owns(g))s.add("SOLITUDE:deed","Купить Высокий шпиль",PRICE+" септимов | "+(eligible(g)?"Разрешение получено":"Нужна репутация Хаафингара 10 (адаптация)"));
        else for(int i=0;i<ROOMS.length;i++)if(!room(i))s.add("SOLITUDE:furnish:"+i,"Обустроить: "+ROOMS[i],COST[i]+" септимов");
    }
    boolean palaceAction(GameEngine g,String a){
        if(!a.equals("SOLITUDE:deed")&&!a.startsWith("SOLITUDE:furnish:"))return false;
        if(!steward(g))return true;
        if(a.equals("SOLITUDE:deed")){
            if(!owns(g)&&eligible(g)&&g.gold>=PRICE&&g.fixShop.home.buy(HOUSE)){g.gold-=PRICE;report(g,"Фолк передаёт тебе ключи от Высокого шпиля.");}
            return true;
        }
        for(int i=0;i<ROOMS.length;i++)if(a.equals("SOLITUDE:furnish:"+i)&&owns(g)&&!room(i)&&g.gold>=COST[i]){
            g.gold-=COST[i];rooms|=1<<i;report(g,"Обустроено: "+ROOMS[i]);break;
        }
        return true;
    }
    GameEngine.Scene scene(GameEngine g){
        GameEngine.Scene s=new GameEngine.Scene("СОЛИТЬЮД | ДОМ","Высокий шпиль",owns(g)?"За дверью твоего дома стихает шум столицы.\n\n"+g.message:"Поместье закрыто. О покупке можно договориться с Фолком Огнебородом в Синем дворце.");
        if(owns(g)){
            for(int i=0;i<GOODS.length;i++){
                String id=GOODS[i];int n=stored(id);
                if(g.progress.quantity(id)>0&&n<LIMIT)s.add("SOLITUDE:home:deposit:"+i,"Положить: "+id,"В сумке: "+g.progress.quantity(id)+" | В сундуке: "+n);
                if(g.progress.quantity(id)>1&&n<LIMIT)s.add("SOLITUDE:home:depositAll:"+i,"Положить всё: "+id,"До "+Math.min(LIMIT-n,g.progress.quantity(id))+" шт.");
                if(n>0)s.add("SOLITUDE:home:withdraw:"+i,"Забрать: "+id,"В сундуке: "+n);
                if(n>1)s.add("SOLITUDE:home:withdrawAll:"+i,"Забрать всё: "+id,"До "+n+" шт. | С учётом веса");
            }
            if(room(3)&&FixShopHome.canBrew(g))s.add("SOLITUDE:home:brew","Сварить лечебное зелье","Пшеница + голубой горноцвет");
            s.add("SOLITUDE:home:sleep","Поспать","8 часов | бесплатно");
            if(room(1)&&lastMeal!=g.day)s.add("SOLITUDE:home:meal","Поесть и выпить воды","Один раз в день");
            if(room(3)&&lastStudy!=g.day)s.add("SOLITUDE:home:study:7","Практиковать алхимию","1 час | один урок в день");
            if(room(4)&&lastStudy!=g.day)s.add("SOLITUDE:home:study:17","Изучить зачарование","1 час | один урок в день");
        }
        return s.add("SOLITUDE:back","На улицы Солитьюда","Покинуть поместье").add("GO_WORLD","Завершить прогулку","Вернуться к приключению");
    }
    void choose(GameEngine g,String a){
        if(!owns(g))return;
        if(transfer(g,a))return;
        if(a.equals("SOLITUDE:home:brew")){
            if(room(3)&&FixShopHome.brew(g)){g.mode=MODE;report(g,"Ты приготовил лечебное зелье в Высоком шпиле.");}
            return;
        }
        if(a.equals("SOLITUDE:home:sleep")){
            if(!g.restHours(8,true))return;
            if(room(0)){g.hp=g.maxHp;g.magicka=g.maxMagicka;g.stamina=g.maxStamina;}
            report(g,"Ты отдохнул в собственном доме.");return;
        }
        if(a.equals("SOLITUDE:home:meal")&&room(1)&&lastMeal!=g.day){g.syncLivingSystems();lastMeal=g.day;g.needs.eat();g.needs.drink();report(g,"Ты поел и выпил воды дома.");return;}
        boolean alchemy=a.equals("SOLITUDE:home:study:7"),enchant=a.equals("SOLITUDE:home:study:17");
        if((alchemy&&room(3)||enchant&&room(4))&&lastStudy!=g.day){
            if(!g.restHours(1,false))return;lastStudy=g.day;
            String name=alchemy?"Алхимия":"Зачарование";
            for(int i=0;i<RpgContent.SKILLS.length;i++)if(name.equalsIgnoreCase(RpgContent.SKILLS[i])){g.progress.skillExperience(g,i,25);break;}
            report(g,"Домашняя практика: "+name);
        }
    }
    private static void report(GameEngine g,String m){g.message=m;g.progress.recent=m;g.recordVisualEvent(m);g.progress.sync(g);}
    JSONObject save(){JSONObject j=new JSONObject();try{j.put("rooms",rooms).put("lastMeal",lastMeal).put("lastStudy",lastStudy).put("chest",chest);}catch(Exception ignored){}return j;}
    static SolitudeHome restore(JSONObject j){SolitudeHome x=new SolitudeHome();if(j!=null){x.rooms=j.optInt("rooms")&31;x.lastMeal=j.optInt("lastMeal",-1);x.lastStudy=j.optInt("lastStudy",-1);JSONObject c=j.optJSONObject("chest");if(c!=null)for(String id:GOODS)x.setStored(id,Math.max(0,Math.min(LIMIT,c.optInt(id,0))));}return x;}
}
