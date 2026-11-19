package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeHomeTest {
    private GameEngine city(){GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;g.gold=30000;g.choose("SOLITUDE:open");return g;}
    @Test public void deedRequiresPermissionAndPalaceAndIsUnique(){
        GameEngine g=city();g.choose("SOLITUDE:deed");assertFalse(SolitudeHome.owns(g));
        g.choose("SOLITUDE:visit:2");g.choose("SOLITUDE:deed");assertFalse(SolitudeHome.owns(g));
        g.holdReputation[3]=10;g.choose("SOLITUDE:deed");assertFalse(SolitudeHome.owns(g));
        g.progress.setQuestOutcome(SolitudeCourt.TRIBUTE,"COMPLETED");g.hour=20;g.choose("SOLITUDE:deed");assertFalse(SolitudeHome.owns(g));
        g.hour=10;g.choose("SOLITUDE:deed");assertTrue(g.fixShop.home.owns(4));assertEquals(5000,g.gold);
        g.choose("SOLITUDE:deed");assertEquals(5000,g.gold);
    }
    @Test public void furnishingsAndMealPersist() throws Exception {
        GameEngine g=city();g.fixShop.home.buy(4);g.choose("SOLITUDE:visit:2");g.choose("SOLITUDE:furnish:1");assertEquals(28500,g.gold);
        g.choose("SOLITUDE:furnish:1");assertEquals(28500,g.gold);
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:home");g.syncLivingSystems();g.needs.food=20;g.choose("SOLITUDE:home:meal");assertEquals(45,g.needs.food);
        g=GameEngine.fromJson(g.toJson().toString());assertEquals(SolitudeHome.MODE,g.mode);g.choose("SOLITUDE:home:meal");assertEquals(45,g.needs.food);
    }
    @Test public void lockedHouseCannotRestAndOwnedHouseCan(){
        GameEngine g=city();g.choose("SOLITUDE:home");g.choose("SOLITUDE:home:sleep");assertEquals(10,g.hour);
        g.fixShop.home.buy(4);g.choose("SOLITUDE:home:sleep");assertEquals(18,g.hour);
    }
    @Test public void chestPersistsAndCannotDuplicate() throws Exception {
        GameEngine g=city();g.fixShop.home.buy(4);g.choose("SOLITUDE:home");
        assertTrue(g.progress.addItem("Железный слиток",1));
        g.choose("SOLITUDE:home:deposit:0");assertEquals(0,g.progress.quantity("Железный слиток"));
        g.choose("SOLITUDE:home:deposit:0");
        g=GameEngine.fromJson(g.toJson().toString());assertEquals(SolitudeHome.MODE,g.mode);
        g.choose("SOLITUDE:home:withdraw:0");g.choose("SOLITUDE:home:withdraw:0");
        assertEquals(1,g.progress.quantity("Железный слиток"));
    }
    @Test public void brewRequiresLaboratoryAndIngredients(){
        GameEngine g=city();g.fixShop.home.buy(4);g.choose("SOLITUDE:home");
        assertTrue(g.progress.addItem("Пшеница",1));assertTrue(g.progress.addItem("Голубой горноцвет",1));
        int potions=g.potions;g.choose("SOLITUDE:home:brew");assertEquals(potions,g.potions);
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:2");g.choose("SOLITUDE:furnish:3");
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:home");g.choose("SOLITUDE:home:brew");
        assertEquals(potions+1,g.potions);assertEquals(0,g.progress.quantity("Пшеница"));assertEquals(SolitudeHome.MODE,g.mode);
        g.choose("SOLITUDE:home:brew");assertEquals(potions+1,g.potions);
    }
    @Test public void homeAlchemySharesRecipesAndConsumesTime() throws Exception {
        GameEngine g=city();g.fixShop.home.buy(4);g.choose("SOLITUDE:home");
        g.progress.quantity("Красный горноцвет",1);g.progress.quantity("Снежные ягоды",1);
        int before=g.manaPotions;
        g.choose("SOLITUDE:craft:mix:0");assertEquals(before,g.manaPotions);
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:2");g.choose("SOLITUDE:furnish:3");
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:home");g.stamina=0;
        g.choose("SOLITUDE:craft:mix:0");assertEquals(before,g.manaPotions);
        g.stamina=50;g.choose("SOLITUDE:craft:mix:0");
        assertEquals(before+1,g.manaPotions);assertEquals(11,g.hour);assertEquals(0,g.progress.quantity("Красный горноцвет"));
        assertEquals(0,g.progress.quantity("Снежные ягоды"));assertEquals(SolitudeHome.MODE,g.mode);
        g=GameEngine.fromJson(g.toJson().toString());g.choose("SOLITUDE:craft:mix:0");assertEquals(before+1,g.manaPotions);
    }
    @Test public void separateHomeStudiesPersistAndRejectLateHours() throws Exception {
        GameEngine g=city();g.fixShop.home.buy(4);g.choose("SOLITUDE:visit:2");
        g.choose("SOLITUDE:furnish:3");g.choose("SOLITUDE:furnish:4");
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:home");
        int a=g.progress.skillXp[7],e=g.progress.skillXp[8];
        g.choose("SOLITUDE:home:study:7");g.choose("SOLITUDE:home:study:8");
        assertTrue(g.progress.skillXp[7]>a);assertTrue(g.progress.skillXp[8]>e);
        assertEquals(12,g.hour);
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:home:study:7");g.choose("SOLITUDE:home:study:17");assertEquals(12,g.hour);
        g.day++;g.hour=23;g.choose("SOLITUDE:home:study:8");assertEquals(23,g.hour);
        g.hour=10;g.choose("SOLITUDE:home:study:17");assertEquals(11,g.hour);
    }
    @Test public void bulkChestPreservesGoodsAndRespectsWeight() throws Exception {
        GameEngine g=city();g.fixShop.home.buy(4);g.choose("SOLITUDE:home");
        assertTrue(g.progress.addItem("Золотой слиток",5));
        g.choose("SOLITUDE:home:depositAll:13");assertEquals(0,g.progress.quantity("Золотой слиток"));
        g=GameEngine.fromJson(g.toJson().toString());
        while(g.progress.addItem("Железный слиток",1)){}
        g.choose("SOLITUDE:home:withdrawAll:13");assertEquals(0,g.progress.quantity("Золотой слиток"));
        assertTrue(g.progress.removeItem("Железный слиток",3));
        g.choose("SOLITUDE:home:withdrawAll:13");assertEquals(3,g.progress.quantity("Золотой слиток"));
        assertTrue(g.progress.removeItem("Железный слиток",2));
        g.choose("SOLITUDE:home:withdrawAll:13");assertEquals(5,g.progress.quantity("Золотой слиток"));
        g.choose("SOLITUDE:home:withdrawAll:13");assertEquals(5,g.progress.quantity("Золотой слиток"));
    }
}
