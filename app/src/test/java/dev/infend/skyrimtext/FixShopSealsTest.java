package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopSealsTest {
 @Test public void combatPowersAndSave() throws Exception {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");FixShopSeals f=g.fixShop.seals;
  assertTrue(f.buy(FixShopSeals.RIPOSTE));assertEquals(0,f.riposte(60));f.defend();assertEquals(20,f.riposte(60));assertEquals(0,f.riposte(60));
  assertTrue(f.buy(FixShopSeals.MANA_WARD));g.magicka=30;assertEquals(20,f.ward(g,50));assertEquals(10,g.magicka);assertEquals(0,f.ward(g,50));
  GameEngine h=GameEngine.fromJson(g.toJson().toString());assertTrue(h.fixShop.seals.owns(FixShopSeals.RIPOSTE));assertEquals(0,h.fixShop.seals.ward(h,40));h.fixShop.seals.beginBattle();assertEquals(10,h.fixShop.seals.ward(h,40));
 }
 @Test public void dailyAndLoadoutAreSaveLocal() throws Exception {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";FixShopSeals f=g.fixShop.seals;f.buy(FixShopSeals.ARSENAL);assertTrue(f.saveSet(g,0));f.buy(FixShopSeals.MEDITATION);assertTrue(f.meditate(g,0));assertFalse(f.meditate(g,0));GameEngine h=GameEngine.fromJson(g.toJson().toString());assertFalse(h.fixShop.seals.meditate(h,0));assertTrue(h.fixShop.seals.loadSet(h,0));assertFalse(new GameEngine().fixShop.seals.owns(FixShopSeals.ARSENAL));
 }
 @Test public void tradeAndTravelKeepBounds(){FixShopSeals s=new FixShopSeals();s.buy(FixShopSeals.TRADE);assertEquals(85,s.price(100,60));assertEquals(95,s.price(100,95));assertEquals(1,s.price(1,1));s.buy(FixShopSeals.QUIET);assertEquals(30,s.encounterChance(60));}
}
