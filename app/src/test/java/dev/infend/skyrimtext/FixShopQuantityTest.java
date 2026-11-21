package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopQuantityTest {
 @Test public void bulkPurchaseAndRefund() throws Exception{
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";g.progress.fixCarryBonus=500;
  FixShopCatalog.Offer o=new FixShopCatalog.Offer("Железный слиток",1,2);assertEquals(100,FixShopCatalog.maxPacks(o));
  assertTrue(g.fixShop.reserve(g,o,25));assertEquals(50,g.fixSpent);assertEquals(25,g.fixShop.purchasedQuantity(o.item));assertEquals(1,g.fixShop.count(FixShopCatalog.productId(o)));
  assertTrue(g.fixShop.refund(g,0));assertEquals(0,g.fixSpent);assertEquals(0,g.fixShop.purchasedQuantity(o.item));
  assertTrue(g.fixShop.reserve(g,o,5));GameEngine h=GameEngine.fromJson(g.toJson().toString());assertTrue(h.fixShop.redeem(h,1));assertEquals(5,h.progress.quantity(o.item));assertFalse(h.fixShop.redeem(h,1));assertFalse(h.fixShop.reserve(h,o,101));
 }
 @Test public void potionOrderSurvivesSync(){
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";g.progress.fixCarryBonus=100;
  FixShopCatalog.Offer o=new FixShopCatalog.Offer("Лечебное зелье",1,4);int before=g.potions;
  assertTrue(g.fixShop.reserve(g,o,10));assertTrue(g.fixShop.redeem(g,0));g.progress.sync(g);assertEquals(before+10,g.potions);assertEquals(g.potions,g.progress.quantity(o.item));
 }
 @Test public void catalogIsExpandedAndExcludesSpecialEquipment(){
  assertTrue(FixShopCatalog.featured().size()>100);assertFalse(FixShopCatalog.valid(new FixShopCatalog.Offer("Этериевый щит",1,10)));assertNull(FixShopCatalog.multiply(new FixShopCatalog.Offer("Железный слиток",20,10),6));
 }
}
