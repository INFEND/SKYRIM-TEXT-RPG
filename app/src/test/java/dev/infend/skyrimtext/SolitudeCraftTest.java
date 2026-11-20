package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeCraftTest {
    private GameEngine city(int d){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;g.stamina=100;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:"+d);return g;
    }
    @Test public void smithConsumesRealMaterialsAndTime() throws Exception {
        GameEngine g=city(5);g.progress.quantity("Железный слиток",2);g.progress.quantity("Кожаные полоски",1);
        int before=g.progress.quantity("Железный меч");
        g.choose("SOLITUDE:craft:make:0");
        assertEquals(before+1,g.progress.quantity("Железный меч"));assertEquals(0,g.progress.quantity("Железный слиток"));assertEquals(0,g.progress.quantity("Кожаные полоски"));assertEquals(10,g.hour);
        g=GameEngine.fromJson(g.toJson().toString());g.choose("SOLITUDE:craft:make:0");assertEquals(before+1,g.progress.quantity("Железный меч"));
    }
    @Test public void newIronRecipesDoNotRequireSteelSkill() throws Exception {
        GameEngine g=city(5);
        g.progress.quantity("Железный слиток",3);g.progress.quantity("Кожаные полоски",1);
        int before=g.progress.quantity("Железная булава");
        g.choose("SOLITUDE:craft:make:13");
        assertEquals(before+1,g.progress.quantity("Железная булава"));
        assertEquals(0,g.progress.quantity("Железный слиток"));
        assertEquals(0,g.progress.quantity("Кожаные полоски"));
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:craft:make:13");
        assertEquals(before+1,g.progress.quantity("Железная булава"));
    }
    @Test public void expandedRecipeOutputsExistInCatalog() {
        for(String name:new String[]{"Железная булава","Железный двуручный меч","Железная секира","Железный боевой молот","Железные перчатки","Железные сапоги","Стальная булава","Стальной двуручный меч","Стальная секира","Стальной боевой молот","Стальные перчатки","Стальные сапоги"})
            assertNotNull(name,ItemCatalog.find(name));
    }
    @Test public void wrongDistrictClosedAndOverweightCannotCraft() {
        GameEngine g=city(0);g.progress.quantity("Железный слиток",2);g.progress.quantity("Кожаные полоски",1);
        g.choose("SOLITUDE:craft:make:0");assertEquals(2,g.progress.quantity("Железный слиток"));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:5");g.hour=23;
        g.choose("SOLITUDE:craft:make:0");assertEquals(2,g.progress.quantity("Железный слиток"));
        g.hour=9;g.progress.quantity("Стальной слиток",100);
        g.choose("SOLITUDE:craft:make:0");assertEquals(2,g.progress.quantity("Железный слиток"));assertEquals(1,g.progress.quantity("Кожаные полоски"));
    }
    @Test public void brewUsesExistingPotionCounterAndCannotReplay() throws Exception {
        GameEngine g=city(0);g.choose("SOLITUDE:store:1");g.progress.quantity("Пшеница",1);g.progress.quantity("Голубой горноцвет",1);
        int before=g.potions;g.choose("SOLITUDE:craft:brew");assertEquals(before+1,g.potions);assertEquals("SOLITUDE:store:1",g.mode);assertEquals(10,g.hour);
        assertEquals(0,g.progress.quantity("Пшеница"));assertEquals(0,g.progress.quantity("Голубой горноцвет"));
        g=GameEngine.fromJson(g.toJson().toString());g.choose("SOLITUDE:craft:brew");assertEquals(before+1,g.potions);
    }
    @Test public void manaAlchemySynchronizesCounterInventoryAndSave() throws Exception {
        GameEngine g=city(0);
        g.choose("SOLITUDE:store:1");
        g.progress.quantity("Красный горноцвет",1);
        g.progress.quantity("Снежные ягоды",1);
        int before=g.manaPotions;
        g.progress.sync(g);
        double weightBefore=g.progress.carryWeight();
        ItemCatalog.Item a=ItemCatalog.find("Красный горноцвет");
        ItemCatalog.Item b=ItemCatalog.find("Снежные ягоды");
        ItemCatalog.Item potion=ItemCatalog.find("Зелье магии");
        assertNotNull(a);assertNotNull(b);assertNotNull(potion);
        g.choose("SOLITUDE:craft:mix:0");
        assertEquals(before+1,g.manaPotions);
        assertEquals(g.manaPotions,g.progress.quantity("Зелье магии"));
        assertEquals(0,g.progress.quantity("Красный горноцвет"));
        assertEquals(0,g.progress.quantity("Снежные ягоды"));
        assertEquals(weightBefore-a.weight-b.weight+potion.weight,g.progress.carryWeight(),0.011);
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals(before+1,g.manaPotions);
        assertEquals(g.manaPotions,g.progress.quantity("Зелье магии"));
        g.choose("SOLITUDE:craft:mix:0");
        assertEquals(before+1,g.manaPotions);
    }
    @Test public void batchAlchemyStopsAtResourcesAndSurvivesSave() throws Exception {
        GameEngine g=city(0);
        g.choose("SOLITUDE:store:1");
        g.progress.quantity("Красный горноцвет",3);
        g.progress.quantity("Снежные ягоды",3);
        int before=g.manaPotions;
        g.choose("SOLITUDE:craft:mix-batch:0");
        assertEquals(before+3,g.manaPotions);
        assertEquals(0,g.progress.quantity("Красный горноцвет"));
        assertEquals(0,g.progress.quantity("Снежные ягоды"));
        assertEquals(12,g.hour);
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:craft:mix-batch:0");
        assertEquals(before+3,g.manaPotions);
    }
    @Test public void batchAlchemyStopsAtClosingTime() {
        GameEngine g=city(0);
        g.choose("SOLITUDE:store:1");
        g.hour=19;
        g.progress.quantity("Красный горноцвет",5);
        g.progress.quantity("Снежные ягоды",5);
        int before=g.manaPotions;
        g.choose("SOLITUDE:craft:mix-batch:0");
        assertEquals(before+1,g.manaPotions);
        assertEquals(20,g.hour);
        assertEquals(4,g.progress.quantity("Красный горноцвет"));
    }
    @Test public void homeAlchemyUsesConsumedIngredientWeight() throws Exception {
        GameEngine g=city(0);
        g.progress.quantity("Пшеница",1);g.progress.quantity("Голубой горноцвет",1);
        assertTrue(FixShopHome.canBrew(g));
        int before=g.potions;
        assertTrue(FixShopHome.brew(g));
        assertEquals(before+1,g.potions);
        assertEquals(0,g.progress.quantity("Пшеница"));
        g=GameEngine.fromJson(g.toJson().toString());
        assertFalse(FixShopHome.canBrew(g));
    }
    @Test public void batchSmithingStopsWhenMaterialsRunOutAndPersists() throws Exception {
        GameEngine g=city(5);
        g.progress.quantity("Железный слиток",6);
        g.progress.quantity("Кожаные полоски",3);
        int before=g.progress.quantity("Железный меч");
        g.choose("SOLITUDE:craft:make-batch:0");
        assertEquals(before+3,g.progress.quantity("Железный меч"));
        assertEquals(0,g.progress.quantity("Железный слиток"));
        assertEquals(0,g.progress.quantity("Кожаные полоски"));
        assertEquals(12,g.hour);
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:craft:make-batch:0");
        assertEquals(before+3,g.progress.quantity("Железный меч"));
    }
    @Test public void batchSmithingStopsWhenWorkshopCloses() {
        GameEngine g=city(5);
        g.hour=17;
        g.progress.quantity("Железный слиток",10);
        g.progress.quantity("Кожаные полоски",5);
        int before=g.progress.quantity("Железный меч");
        g.choose("SOLITUDE:craft:make-batch:0");
        assertEquals(before+1,g.progress.quantity("Железный меч"));
        assertEquals(18,g.hour);
        assertEquals(8,g.progress.quantity("Железный слиток"));
        g.choose("SOLITUDE:craft:make-batch:0");
        assertEquals(before+1,g.progress.quantity("Железный меч"));
        assertEquals(18,g.hour);
    }
    @Test public void batchSmithingRejectsWrongDistrictAndOverweightWithoutConsumingResources() {
        GameEngine g=city(0);
        g.progress.quantity("Железный слиток",10);
        g.progress.quantity("Кожаные полоски",5);
        int before=g.progress.quantity("Железный меч");
        g.choose("SOLITUDE:craft:make-batch:0");
        assertEquals(before,g.progress.quantity("Железный меч"));
        assertEquals(10,g.progress.quantity("Железный слиток"));
        assertEquals(5,g.progress.quantity("Кожаные полоски"));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:5");
        g.progress.quantity("Стальной слиток",100);
        g.choose("SOLITUDE:craft:make-batch:0");
        assertEquals(before,g.progress.quantity("Железный меч"));
        assertEquals(10,g.progress.quantity("Железный слиток"));
        assertEquals(5,g.progress.quantity("Кожаные полоски"));
    }
    @Test public void batchSmithingStopsAtStaminaBoundaryWithoutExtraConsumption() throws Exception {
        GameEngine g=city(5);
        g.stamina=9;
        g.progress.quantity("Железный слиток",10);
        g.progress.quantity("Кожаные полоски",5);
        int before=g.progress.quantity("Железный меч");
        g.choose("SOLITUDE:craft:make-batch:0");
        assertEquals(before+1,g.progress.quantity("Железный меч"));
        assertEquals(8,g.progress.quantity("Железный слиток"));
        assertEquals(4,g.progress.quantity("Кожаные полоски"));
        assertEquals(4,g.stamina);
        assertEquals(10,g.hour);
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:craft:make-batch:0");
        assertEquals(before+1,g.progress.quantity("Железный меч"));
        assertEquals(8,g.progress.quantity("Железный слиток"));
    }
}
