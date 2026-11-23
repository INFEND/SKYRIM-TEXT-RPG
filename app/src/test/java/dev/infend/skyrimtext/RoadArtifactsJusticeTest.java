package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoadArtifactsJusticeTest {
    GameEngine hero(){GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="WORLD";g.hp=g.maxHp=500;g.magicka=g.maxMagicka=100;return g;}
    @Test public void crownRetainsPreviousStoneOnlyWhileWorn() throws Exception {GameEngine g=hero();g.progress.location="Камень Хранитель";g.choose("ROADSTONE:1");g.mode="WORLD";g.progress.location="Камень Коня";g.choose("ROADSTONE:5");assertEquals(150,g.progress.carryLimit(),0.01);assertFalse(g.progress.artifacts.has(g.progress,1));assertTrue(g.progress.addItem("Этериевая корона",1));assertTrue(g.progress.equip("Этериевая корона"));assertTrue(g.progress.artifacts.has(g.progress,1));g=GameEngine.fromJson(g.toJson().toString());assertTrue(g.progress.artifacts.has(g.progress,1));g.progress.unequip("Этериевая корона");assertFalse(g.progress.artifacts.has(g.progress,1));}
    @Test public void soulCanBeCapturedAndReleasedWithoutDestroyingStar() throws Exception {GameEngine g=hero();assertTrue(g.progress.addItem("Звезда Азуры",1));g.beginRoadBattle("Волк",200,1,"");g.choose("ROADSTAR_TRAP");g.enemyHp=1;g.choose("ATTACK");assertTrue(g.progress.artifacts.whiteSoul>0);g=GameEngine.fromJson(g.toJson().toString());g.magicka=0;g.choose("ROADSTAR:release:1");assertTrue(g.magicka>0);assertEquals(0,g.progress.artifacts.whiteSoul);assertEquals(1,g.progress.quantity("Звезда Азуры"));}
    @Test public void roadSentenceRespectsTheJurisdictionOfStolenProperty() throws Exception {
        GameEngine g = hero();
        g.road.addCityBounty(0, 1000);
        g.road.addCityBounty(3, 500);
        assertTrue(g.progress.addItem("Похищенное кольцо", 1));
        g.progress.skillXp[0] = 100;
        int day = g.day;

        g.region = 1;
        g.progress.location = "Рифтен";
        g.choose("ROADJAIL:0");
        assertEquals(day, g.day);
        assertEquals(1000, g.road.bounty(0));

        g.region = 0;
        g.progress.location = "Вайтран";
        g.choose("ROADJAIL:0");
        assertEquals(day + 2, g.day);
        assertEquals(0, g.road.bounty(0));
        assertEquals(500, g.road.bounty(3));
        assertEquals(80, g.progress.skillXp[0]);
        assertEquals(1, g.progress.quantity("Похищенное кольцо"));
        g.choose("ROADJAIL:0");
        assertEquals(day + 2, g.day);

        g = GameEngine.fromJson(g.toJson().toString());
        assertEquals(1, g.progress.quantity("Похищенное кольцо"));
        g.region = 3;
        g.progress.location = "Солитьюд";
        g.choose("ROADJAIL:3");
        assertEquals(SolitudeJustice.CELL, g.mode);
        assertEquals(day + 2, g.day);
        assertEquals(0, g.progress.quantity("Похищенное кольцо"));
        g.choose("SOLITUDE:cell:day");
        assertEquals(day + 3, g.day);
        assertEquals(0, g.road.bounty(3));
        assertEquals(0, g.progress.quantity("Похищенное кольцо"));
    }
    @Test public void mapNamesActivateAllStandingStones(){GameEngine g=hero();String[] names={"Камень Хранитель","Камень Хранитель","Камень Хранитель","Камень Любовника","Камень Коня","Камень Лорда","Камень Леди","Камень Ученика","Камень Атронаха","Камень Ритуала","Камень Змея","Камень Тени","Камень Башни"};for(int i=1;i<=13;i++){g.mode="WORLD";g.progress.location=names[i-1];g.choose("ROADSTONE:"+i);assertEquals(names[i-1],i,g.progress.artifacts.stone);}}
    @Test public void towerCannotBeReactivatedOnSameDay(){GameEngine g=hero();g.progress.artifacts.stone=13;assertTrue(g.progress.artifacts.useTower(g));assertEquals(35,g.progress.artifacts.towerBonus(g,10));g.progress.artifacts.consumeTower(g,10);assertEquals(0,g.progress.artifacts.towerBonus(g,10));assertFalse(g.progress.artifacts.useTower(g));}
}