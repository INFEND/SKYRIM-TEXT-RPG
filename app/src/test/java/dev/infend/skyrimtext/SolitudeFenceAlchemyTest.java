package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeFenceAlchemyTest {
    private GameEngine city(int d,int hour){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=hour;g.stamina=100;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:"+d);return g;
    }
    @Test public void fenceLimitPersistsAndDoesNotClearBounty() throws Exception {
        GameEngine g=city(7,21);g.progress.quantity("Похищенное кольцо",2);g.progress.quantity("Похищенный амулет",1);g.road.addCityBounty(3,25);int gold=g.gold;
        g.choose("SOLITUDE:fence:0");assertEquals(gold+12,g.gold);assertEquals(1,g.progress.quantity("Похищенное кольцо"));assertEquals(25,g.road.bounty(3));
        g=GameEngine.fromJson(g.toJson().toString());g.choose("SOLITUDE:fence:1");assertEquals(gold+12,g.gold);
        g.day++;g.choose("SOLITUDE:fence:1");assertEquals(gold+36,g.gold);assertEquals(0,g.progress.quantity("Похищенный амулет"));
    }
    @Test public void fenceRejectsWrongPlaceAndTime(){
        GameEngine g=city(0,21);g.progress.quantity("Похищенное кольцо",1);int gold=g.gold;
        g.choose("SOLITUDE:fence:0");g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:7");g.hour=12;g.choose("SOLITUDE:fence:0");
        assertEquals(gold,g.gold);assertEquals(1,g.progress.quantity("Похищенное кольцо"));
    }
    @Test public void alchemyConsumesIngredientsAndUsesSharedManaCounter() throws Exception {
        GameEngine g=city(0,9);g.choose("SOLITUDE:store:1");g.progress.quantity("Красный горноцвет",1);g.progress.quantity("Снежные ягоды",1);
        int mana=g.manaPotions;g.choose("SOLITUDE:craft:mix:0");assertEquals(mana+1,g.manaPotions);assertEquals(0,g.progress.quantity("Красный горноцвет"));assertEquals(10,g.hour);
        g=GameEngine.fromJson(g.toJson().toString());g.choose("SOLITUDE:craft:mix:0");assertEquals(mana+1,g.manaPotions);
        g.progress.quantity("Пурпурный горноцвет",1);g.progress.quantity("Лаванда",1);int n=g.progress.quantity("Зелье запаса сил");
        g.choose("SOLITUDE:craft:mix:1");assertEquals(n+1,g.progress.quantity("Зелье запаса сил"));assertEquals(11,g.hour);
    }
}
