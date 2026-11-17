package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopGiftsTest {
 @Test public void ownershipAndDailyLimitsPersist() throws Exception{
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");FixShopGifts f=g.fixShop.gifts;
  assertTrue(f.buy(FixShopGifts.FATE));assertFalse(f.buy(FixShopGifts.FATE));assertFalse(f.retry(1,0,100));assertTrue(f.retry(1,50,90));assertFalse(f.retry(1,50,90));assertTrue(f.retry(2,50,90));
  assertTrue(f.buy(FixShopGifts.PHOENIX));g.hp=0;assertTrue(f.rescue(g));assertTrue(g.hp>0);g.hp=0;assertFalse(f.rescue(g));g.hp=1;
  assertTrue(f.buy(FixShopGifts.MENTOR));assertEquals(25,f.skillBonus(100));f.toggle(FixShopGifts.MENTOR);
  GameEngine h=GameEngine.fromJson(g.toJson().toString());assertTrue(h.fixShop.gifts.owns(FixShopGifts.MENTOR));assertFalse(h.fixShop.gifts.on(FixShopGifts.MENTOR));assertFalse(h.fixShop.gifts.retry(2,50,90));assertFalse(new GameEngine().fixShop.gifts.owns(FixShopGifts.FATE));
 }
 @Test public void bloodExchangeIsOncePerBattle(){
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="COMBAT";g.magicka=0;FixShopGifts f=g.fixShop.gifts;f.buy(FixShopGifts.BLOOD);
  int hp=g.hp;assertTrue(f.blood(g));assertTrue(g.hp<hp);assertTrue(g.magicka>0);assertFalse(f.blood(g));f.beginBattle();assertTrue(f.canBlood(g));
 }
 @Test public void pocketDoesNotRequireHouse(){
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";assertFalse(g.fixShop.world.storageAccess(g));g.fixShop.gifts.buy(FixShopGifts.POCKET);assertTrue(g.fixShop.world.storageAccess(g));g.fixShop.gifts.toggle(FixShopGifts.POCKET);assertFalse(g.fixShop.world.storageAccess(g));
 }
}
