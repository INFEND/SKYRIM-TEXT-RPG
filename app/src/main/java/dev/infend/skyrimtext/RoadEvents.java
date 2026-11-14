package dev.infend.skyrimtext;
import java.util.Random;

/** One bounded travel encounter after a validated, completed journey. */
final class RoadEvents {
    private static final Random RNG=new Random();
    static String resolve(GameEngine g,TravelPlanner.Route route){
        if(route==null||!route.allowed||route.days<=0)return "";
        int roll=RNG.nextInt(100);
        if(roll>=Math.min(65,20+route.days*10))return "";
        String event;
        if(roll%4==0){
            int cost=Math.min(Math.max(0,g.stamina-1),6+route.days*2);g.stamina-=cost;
            event="Размытый тракт заставил обходить камни. Выносливость −"+cost+".";
        }else if(roll%4==1){
            int gain=Math.min(g.maxStamina-g.stamina,12);g.stamina+=gain;
            event="У придорожного костра удалось передохнуть. Выносливость +"+gain+".";
        }else if(roll%4==2){
            boolean found=g.progress.addItem("Отмычка",1);
            event=found?"У покинутой стоянки найдена отмычка. Добавлена в сумку.":"У покинутой стоянки найдена отмычка, но сумка переполнена.";
        }else{
            g.progress.skillExperience(g,9,12);event="На развилке пришлось читать следы. +12 базового опыта скрытности.";
        }
        g.progress.sync(g);g.recordVisualEvent(event);return event;
    }
}
