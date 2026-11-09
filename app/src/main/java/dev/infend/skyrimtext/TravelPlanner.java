package dev.infend.skyrimtext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Game-world route network. Region hubs use a simplified logical network,
 * not satellite/cartographic precision. Remote places require travel via hubs.
 */
public final class TravelPlanner {
    private TravelPlanner() {}
    public static final String[] HUBS = {
        "Вайтран", "Рифтен", "Виндхельм", "Солитьюд", "Маркарт",
        "Фолкрит", "Винтерхолд", "Данстар", "Морфал", "Воронья Скала"
    };
    // Main roads; the Windhelm-Solstheim link is a sea journey, not a road.
    private static final int[][] ROAD_CONNECTIONS = {
        {0,1}, {0,2}, {0,4}, {0,5}, {0,7}, {0,8},
        {1,2}, {1,5}, {2,6}, {2,7}, {3,4}, {3,8},
        {4,5}, {4,8}, {6,7}, {7,8}, {2,9}
    };
    public static final int CARRY_LIMIT_KG = 50;
    public static final int FERRY_FARE = 250;

    public static final class Route {
        public final boolean allowed;
        public final String problem;
        public final int days, fare;
        public final List<String> stops;
        private Route(boolean possible, String reason, int travelDays, int fareSeptims, List<String> path) {
            allowed = possible;
            problem = reason;
            days = travelDays;
            fare = fareSeptims;
            stops = Collections.unmodifiableList(path);
        }
        public String summary() {
            if (!allowed) return problem;
            if (days == 0) return "Ты уже находишься здесь.";
            return String.format(Locale.forLanguageTag("ru"),
                "В пути: %d дн.  •  Переправа: %d септимов\n\nМаршрут: %s",
                days, fare, String.join(" → ", stops));
        }
    }
    private static Route blocked(String reason) {
        return new Route(false,reason,0,0,Collections.<String>emptyList());
    }
    private static int region(int value) {
        return Math.max(0,Math.min(value,HUBS.length-1));
    }
    private static int edgeCost(int a,int b) {
        return (a==2 && b==9)||(a==9 && b==2)?2:1;
    }
    private static ArrayList<Integer> regionPath(int source,int destination) {
        int[] best=new int[HUBS.length],previous=new int[HUBS.length];
        boolean[] closed=new boolean[HUBS.length];
        Arrays.fill(best,999999);
        Arrays.fill(previous,-1);
        best[source]=0;
        for(int i=0;i<HUBS.length;i++) {
            int current=-1;
            for(int k=0;k<HUBS.length;k++)
                if(!closed[k]&&(current<0||best[k]<best[current]))current=k;
            if(current<0||best[current]==999999)break;
            if(current==destination)break;
            closed[current]=true;
            for(int[] link:ROAD_CONNECTIONS){
                int to=link[0]==current?link[1]:(link[1]==current?link[0]:-1);
                if(to<0||closed[to])continue;
                int candidate=best[current]+edgeCost(current,to);
                if(candidate<best[to]){best[to]=candidate;previous[to]=current;}
            }
        }
        if(best[destination]>=999999)return new ArrayList<>();
        ArrayList<Integer> path=new ArrayList<>();
        for(int pos=destination;pos>=0;pos=previous[pos])path.add(pos);
        Collections.reverse(path);
        return path;
    }

    public static Route plan(GameEngine game, WorldAtlas.Place target) {
        if(game==null||target==null)return blocked("Неизвестная точка назначения.");
        if(!game.started)return blocked("Сначала создай героя.");
        if(target.type.equals("Иной мир")||target.name.equals("Чёрный Предел")
                ||target.name.equals("Забытая долина"))
            return blocked("Эта область открывается через сюжетные события и специальные переходы.");
        SkyrimMapCatalog.Site current=SkyrimMapCatalog.find(game.progress.location);
        if(game.progress.location.equals(target.name)||(current!=null&&current.name.equals(target.name)))
            return new Route(true,"",0,0,Collections.singletonList(target.name));

        int from=region(current==null?game.region:current.region),to=region(target.region);
        ArrayList<Integer> path=regionPath(from,to);
        if(path.isEmpty())return blocked("Не удалось составить наземный маршрут.");
        int days=0,fare=0;
        ArrayList<String> stops=new ArrayList<>();
        String origin=game.progress.location;
        stops.add(origin);
        if(path.size()>1&&!origin.equals(HUBS[from])){
            stops.add(HUBS[from]);days++;
        }
        for(int i=1;i<path.size();i++){
            int previous=path.get(i-1),current=path.get(i);
            if((previous==2&&current==9)||(previous==9&&current==2))
                fare+=FERRY_FARE;
            days+=edgeCost(previous,current);
            if(!stops.get(stops.size()-1).equals(HUBS[current]))
                stops.add(HUBS[current]);
        }
        if(!stops.get(stops.size()-1).equals(target.name)){
            stops.add(target.name);
            days++;
        }
        days=Math.max(1,days);
        if(game.progress.carryWeight()>CARRY_LIMIT_KG)
            return blocked("Снаряжение весит больше 50 кг. Освободи часть места в инвентаре.");
        if(game.gold<fare)
            return blocked("Для переправы требуется "+fare+" септимов. Сейчас у тебя "+game.gold+".");
        return new Route(true,"",days,fare,stops);
    }
}

