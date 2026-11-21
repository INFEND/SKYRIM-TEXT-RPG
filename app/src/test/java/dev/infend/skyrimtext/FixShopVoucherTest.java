package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopVoucherTest {
 @Test public void reserveRefundAndRedeemOnlyOnce() throws Exception{
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";g.progress.fixCarryBonus=500;
  FixShopCatalog.Offer o=new FixShopCatalog.Offer("Железный меч",1,10);
  assertTrue(FixShopCatalog.valid(o));assertTrue(g.fixShop.reserve(g,o));assertEquals(10,g.fixSpent);
  assertTrue(g.fixShop.refund(g,0));assertEquals(0,g.fixSpent);assertFalse(g.fixShop.redeem(g,0));
  assertTrue(g.fixShop.reserve(g,o));GameEngine h=GameEngine.fromJson(g.toJson().toString());
  int before=h.progress.quantity(o.item);assertTrue(h.fixShop.redeem(h,1));assertEquals(before+1,h.progress.quantity(o.item));assertFalse(h.fixShop.redeem(h,1));assertFalse(h.fixShop.refund(h,1));
 }
 @Test public void teleportOnlyVisited() {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";g.fixShop.editorEnabled=true;
  assertFalse(g.fixTeleport("Вайтран"));g.visitedVisual.add("Вайтран");assertTrue(g.fixTeleport("Вайтран"));assertEquals("Вайтран",g.progress.location);
 }
}
