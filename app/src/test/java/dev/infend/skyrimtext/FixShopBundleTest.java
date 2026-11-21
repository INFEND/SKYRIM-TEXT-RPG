package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopBundleTest {
 @Test public void bundleIsAtomicAndRefundable() throws Exception {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";
  for(FixShopCatalog.Bundle b:FixShopCatalog.BUNDLES)assertTrue(b.name,FixShopCatalog.validBundle(b));
  FixShopCatalog.Bundle b=FixShopCatalog.BUNDLES[0];assertTrue(g.fixShop.reserveBundle(g,b));int before=g.progress.quantity(b.items[0]);
  assertFalse(g.fixShop.redeem(g,0));assertEquals(before,g.progress.quantity(b.items[0]));
  assertTrue(g.fixShop.refund(g,0));assertTrue(g.fixShop.reserveBundle(g,b));g.progress.fixCarryBonus=500;
  GameEngine h=GameEngine.fromJson(g.toJson().toString());assertTrue(h.fixShop.redeem(h,1));assertEquals(before+1,h.progress.quantity(b.items[0]));assertFalse(h.fixShop.redeem(h,1));assertFalse(h.fixShop.refund(h,1));
 }
 @Test public void membershipDoesNotCrossCivilWar() {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";
  int xp=g.xp;assertFalse(g.factions.fixMembership(g,4,true));assertFalse(g.factions.fixMembership(g,5,true));assertFalse(g.factions.fixMembership(g,4,false));assertFalse(g.factions.fixMembership(g,5,true));assertEquals(xp,g.xp);
 }
}
