package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopPartyTest {
 @Test public void partyCapacityAndSave() throws Exception {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");FixShopWorld w=g.fixShop.world;
  assertTrue(w.buyUnique(0));assertTrue(w.buyUnique(1));assertTrue(w.buyUnique(2));assertFalse(w.buyUnique(0));
  assertTrue(w.recruit(0));assertFalse(w.recruit(1));assertTrue(w.buySlot());assertTrue(w.recruit(1));
  assertEquals(20,w.companionDamage(2,100));assertEquals(0,w.companionDamage(1,100));
  GameEngine h=GameEngine.fromJson(g.toJson().toString());assertEquals(2,h.fixShop.world.partySize());assertTrue(h.fixShop.world.inParty(1));assertTrue(h.fixShop.world.dismiss(0));assertTrue(h.fixShop.world.inParty(1));assertEquals(12,h.fixShop.world.companionDamage(2,100));
  assertEquals(0,new GameEngine().fixShop.world.partySize());
 }
}
