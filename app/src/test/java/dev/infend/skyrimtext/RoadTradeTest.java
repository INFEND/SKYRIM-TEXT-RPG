package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoadTradeTest {
    GameEngine hero(String id){GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.gold=100;g.progress.sync(g);g.road.id=id;g.road.destination="Ривервуд";g.mode="ROAD";return g;}
    String action(GameEngine g,String suffix){for(GameEngine.Choice c:g.scene().choices)if(c.action.endsWith(":"+suffix))return c.action;fail("Missing "+suffix);return "";}
    @Test public void roadTradeChangesRealPotionStockAndSurvivesSave() throws Exception{
        GameEngine g=hero("peddler");int before=g.potions;String buy=action(g,"trade_0");g.choose(buy);assertEquals("ROAD",g.mode);assertEquals(65,g.gold);assertEquals(before+1,g.potions);assertEquals(g.potions,g.progress.quantity("Лечебное зелье"));
        g=GameEngine.fromJson(g.toJson().toString());g.choose(buy);assertEquals(65,g.gold);assertEquals(before+1,g.potions);
        g.choose(action(g,"trade_1"));assertEquals(35,g.gold);assertEquals(g.manaPotions,g.progress.quantity("Зелье магии"));
    }
    @Test public void insufficientGoldDoesNotConsumeOffer(){
        GameEngine g=hero("peddler");g.gold=0;String buy=action(g,"trade_0");int before=g.potions;g.choose(buy);assertEquals(0,g.gold);assertEquals(before,g.potions);g.gold=35;g.choose(buy);assertEquals(0,g.gold);assertEquals(before+1,g.potions);
    }
    @Test public void hunterSellsAndBuysRealMaterials(){
        GameEngine g=hero("hunter");int before=g.progress.quantity("Кожа");g.choose(action(g,"trade_0"));assertEquals(88,g.gold);assertEquals(before+1,g.progress.quantity("Кожа"));g.choose(action(g,"trade_2"));assertEquals(94,g.gold);assertEquals(before,g.progress.quantity("Кожа"));
    }
    @Test public void blessingAndRefugeeHaveIndependentOutcomes(){
        GameEngine g=hero("vigilants");g.hp=50;g.choose(action(g,"blessing"));assertEquals(65,g.hp);assertEquals("RESULT",g.mode);
        g=hero("farmer_refugee");int rep=g.reputation;g.choose(action(g,"guide"));assertEquals(rep+1,g.reputation);assertEquals("RESULT",g.mode);
    }
}
