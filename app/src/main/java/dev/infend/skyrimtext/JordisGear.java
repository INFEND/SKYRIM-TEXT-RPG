package dev.infend.skyrimtext;

import org.json.JSONObject;

/** One real inventory item per slot. Default housecarl gear is not lootable. */
final class JordisGear {
    private static final String[] SLOTS={"weapon","shield","body"};
    private static final String[] ALLOWED={
        "Железный меч","Стальной меч","Орочий меч","Двемерский меч","Эльфийский меч","Стеклянный меч","Эбонитовый меч","Даэдрический меч",
        "Железный щит","Стальной щит","Орочий щит","Двемерский щит","Эльфийский щит","Стеклянный щит","Эбонитовый щит","Даэдрический щит",
        "Железная кираса","Стальная кираса","Орочья кираса","Двемерская кираса","Стальная пластинчатая кираса","Эбонитовая кираса","Даэдрическая кираса","Кожаная кираса"
    };
    private final String[] items={"","",""};
    private static int slot(String id){
        boolean allowed=false;for(String a:ALLOWED)if(a.equals(id)){allowed=true;break;}
        if(!allowed)return -1;
        ItemCatalog.Item item=ItemCatalog.find(id);if(item==null||item.baseStat<=0)return -1;
        for(int i=0;i<SLOTS.length;i++)if(SLOTS[i].equals(item.slot()))return i;
        return -1;
    }
    private boolean canGive(GameEngine g,String id){
        int s=slot(id);
        return s>=0&&items[s].isEmpty()&&g.progress.quantity(id)>0&&!g.progress.equipped(id)
            &&g.progress.temperQuality(id)==0&&!g.progress.artifacts.filled(id)
            &&!(g.swordUpgraded&&"Стальной меч".equals(id))&&!(g.armorUpgraded&&"Стальная кираса".equals(id));
    }
    void choices(GameEngine g,GameEngine.Scene scene,String prefix){
        for(int i=0;i<SLOTS.length;i++)if(!items[i].isEmpty())scene.add(prefix+"take:"+i,"Забрать у Йордис: "+items[i],"Вернуть в сумку | с учётом веса");
        for(String id:ALLOWED)if(canGive(g,id))scene.add(prefix+"give:"+id,"Передать Йордис: "+id,"Предмет займёт свободный слот экипировки");
    }
    String action(GameEngine g,String action){
        if(action.startsWith("give:")){
            String id=action.substring(5);if(!canGive(g,id)||!g.progress.removeItem(id,1))return "Передача недоступна.";
            items[slot(id)]=id;return "Йордис экипирует: "+id+".";
        }
        if(action.startsWith("take:")){
            for(int i=0;i<SLOTS.length;i++)if(action.equals("take:"+i)&&!items[i].isEmpty()){
                String id=items[i];if(!g.progress.addItem(id,1))return "В сумке недостаточно места.";
                items[i]="";return "Йордис возвращает: "+id+".";
            }
            return "Этот слот пуст.";
        }
        return null;
    }
    private double stat(int i){ItemCatalog.Item item=ItemCatalog.find(items[i]);return item==null?0:item.baseStat;}
    int damageBonus(){return Math.min(8,(int)Math.ceil(stat(0)/2));}
    int protection(int incoming){return Math.min(Math.max(0,incoming),Math.min(8,(int)((stat(1)+stat(2))/8)));}
    String summary(){return "Оружие: "+name(0)+" | Щит: "+name(1)+" | Броня: "+name(2);}
    private String name(int i){return items[i].isEmpty()?"Личное снаряжение":items[i];}
    JSONObject save(){JSONObject j=new JSONObject();try{for(int i=0;i<SLOTS.length;i++)j.put(SLOTS[i],items[i]);}catch(Exception e){throw new IllegalStateException(e);}return j;}
    static JordisGear restore(JSONObject j){JordisGear x=new JordisGear();if(j!=null)for(int i=0;i<SLOTS.length;i++){String id=j.optString(SLOTS[i],"");if(slot(id)==i)x.items[i]=id;}return x;}
}
