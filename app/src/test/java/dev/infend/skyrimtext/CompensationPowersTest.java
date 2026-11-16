package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class CompensationPowersTest {
 @Test public void abilitiesAndItemsRoundTrip() throws Exception {GameEngine g=new GameEngine();assertTrue(g.compensation.buyPower(0));assertFalse(g.compensation.buyPower(0));assertTrue(g.compensation.upgrade(0));assertTrue(g.compensation.reduce(0));assertEquals(20,g.compensation.cost(0));assertTrue(g.compensation.buyItem(g,0));assertFalse(g.progress.removeItem(CompensationPowers.ITEMS[0],1));GameEngine loaded=GameEngine.fromJson(g.toJson().toString());assertEquals(2,loaded.compensation.ranks[0]);assertEquals(1,loaded.progress.quantity(CompensationPowers.ITEMS[0]));}
 @Test public void extremeChances(){GameEngine g=new GameEngine();assertFalse(g.rollCheck(0));assertTrue(g.rollCheck(100));assertTrue(g.lastCheckRoll>=1&&g.lastCheckRoll<=100);}
}
