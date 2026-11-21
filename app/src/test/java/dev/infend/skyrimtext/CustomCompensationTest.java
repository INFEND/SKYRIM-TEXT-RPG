package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class CustomCompensationTest {
 @Test public void customRoundTrip() throws Exception {GameEngine g=new GameEngine();assertTrue(g.compensation.createPower("Искра",0,50,10));assertTrue(g.compensation.createItem(g,"Мой клинок",0,40,2,"Описание"));assertTrue(g.progress.equip(CompensationPowers.itemId(3)));GameEngine loaded=GameEngine.fromJson(g.toJson().toString());assertEquals("Искра",loaded.compensation.powerName(3));assertEquals("Мой клинок",ItemCatalog.find(CompensationPowers.itemId(3)).name);assertTrue(loaded.progress.equipped(CompensationPowers.itemId(3)));assertFalse(loaded.progress.removeItem(CompensationPowers.itemId(3),1));}
 @Test public void rejectBadParameters(){GameEngine g=new GameEngine();assertFalse(g.compensation.createPower("X",0,0,10));assertFalse(g.compensation.createItem(g,"X",0,20,Double.NaN,""));assertEquals(-1,CompensationPowers.actionIndex("FIX_POWER:12"));assertEquals(11,CompensationPowers.actionIndex("FIX_POWER:11"));}
}
