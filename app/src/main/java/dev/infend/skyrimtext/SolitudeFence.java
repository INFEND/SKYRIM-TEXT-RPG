package dev.infend.skyrimtext;

/** Optional authored illicit trade; only explicitly marked stolen goods are accepted. */
final class SolitudeFence {
    private static final String PREFIX="SOLITUDE:fence:";
    private static final String[] ITEMS={"Похищенное кольцо","Похищенный амулет"};
    private static final int[] PRICE={12,24};
    private int lastDay;
    private static boolean available(GameEngine g){
        return SolitudeCity.here(g)&&SolitudeCourt.ready(g)&&"SOLITUDE:7".equals(g.mode)&&g.hour>=20&&g.hour<23;
    }
    private boolean canSell(GameEngine g,int i){
        return available(g)&&lastDay<g.day&&g.progress.quantity(ITEMS[i])>0&&g.gold<=Integer.MAX_VALUE-PRICE[i];
    }
    void choices(GameEngine g,GameEngine.Scene s){
        for(int i=0;i<ITEMS.length;i++)if(canSell(g,i))s.add(PREFIX+i,"Сбыть: "+ITEMS[i],PRICE[i]+" септимов | Авторская встреча с перекупщиком | Один предмет в день");
    }
    boolean choose(GameEngine g,String a){
        if(!a.startsWith(PREFIX))return false;
        for(int i=0;i<ITEMS.length;i++)if(a.equals(PREFIX+i)&&canSell(g,i)){
            if(!g.progress.removeItem(ITEMS[i],1))return true;
            lastDay=g.day;g.gold+=PRICE[i];
            g.message="Перекупщик забирает вещь. Получено "+PRICE[i]+" септимов. Штрафы остаются в силе.";
            g.progress.recent=g.message;g.progress.sync(g);g.recordVisualEvent(g.message);return true;
        }
        return true;
    }
    int save(){return lastDay;}
    void restore(int day){lastDay=Math.max(0,day);}
}
