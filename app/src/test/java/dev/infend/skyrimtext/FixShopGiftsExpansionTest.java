package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopGiftsExpansionTest {
 @Test public void purchasesAndBattleUsesArePersistent() throws Exception {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");FixShopGifts f=g.fixShop.gifts;
  assertEquals(24,FixShopGifts.NAME.length);assertEquals(FixShopGifts.NAME.length,FixShopGifts.DESC.length);assertEquals(FixShopGifts.NAME.length,FixShopGifts.PRICE.length);
  assertTrue(f.buy(FixShopGifts.AEGIS));assertEquals(50,f.absorb(70));assertEquals(0,f.absorb(70));
  assertTrue(f.buy(FixShopGifts.TIME));assertTrue(f.useBattle(FixShopGifts.TIME));assertFalse(f.useBattle(FixShopGifts.TIME));
  assertTrue(f.buy(FixShopGifts.AMBUSH));assertEquals(40,f.ambush(100));assertEquals(0,f.ambush(100));
  GameEngine h=GameEngine.fromJson(g.toJson().toString());assertFalse(h.fixShop.gifts.availableBattle(FixShopGifts.TIME));h.fixShop.gifts.beginBattle();assertTrue(h.fixShop.gifts.availableBattle(FixShopGifts.TIME));
  assertFalse(new GameEngine().fixShop.gifts.owns(FixShopGifts.AEGIS));
 }
 @Test public void travelAndCostsUseOwnedEnabledGifts(){
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");FixShopGifts f=g.fixShop.gifts;
  assertEquals(9,f.days(9));assertEquals(300,f.fare(300));f.buy(FixShopGifts.FERRY);f.buy(FixShopGifts.WIND);assertEquals(0,f.fare(300));assertEquals(6,f.days(9));assertEquals(1,f.days(1));f.toggle(FixShopGifts.WIND);assertEquals(9,f.days(9));
  int cost=g.magicCost();f.buy(FixShopGifts.ARCHMAGE);assertTrue(g.magicCost()<cost);assertTrue(g.magicCost()>0);
 }
 @Test public void escapeAndRestAreDailyAndModeLimited(){
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");FixShopGifts f=g.fixShop.gifts;f.buy(FixShopGifts.ESCAPE);g.mode="COMBAT";g.battleKind="CANONICAL";assertFalse(f.escape(g));g.battleKind="WILD";assertTrue(f.escape(g));g.mode="COMBAT";assertFalse(f.escape(g));
  f.buy(FixShopGifts.BREATH);g.hp=1;assertFalse(f.breath(g));g.mode="WORLD";assertTrue(f.breath(g));assertTrue(g.hp>1);g.hp=1;assertFalse(f.breath(g));
 }
}
