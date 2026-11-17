package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopResidenceTest {
 @Test public void brewingConsumesIngredientsAndRequiresOwnedHome(){
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";g.progress.location="Вайтран";g.progress.fixCarryBonus=100;
  assertFalse(FixShopHome.choose(g,"FIXRESIDENCE:brew"));assertTrue(g.fixShop.home.buy(0));assertTrue(g.fixShop.home.buyRoom(g,2));
  assertTrue(g.progress.addItem("Пшеница",1));assertTrue(g.progress.addItem("Голубой горноцвет",1));int n=g.potions;
  assertTrue(FixShopHome.choose(g,"FIXRESIDENCE:brew"));assertEquals(n+1,g.potions);assertEquals(0,g.progress.quantity("Пшеница"));assertFalse(FixShopHome.choose(g,"FIXRESIDENCE:brew"));
 }
 @Test public void artworkPathsAreStableAndSafe(){assertEquals(FixShopCatalog.artPath("Нити судьбы"),FixShopCatalog.artPath("Нити судьбы"));assertNotEquals(FixShopCatalog.artPath("Нити судьбы"),FixShopCatalog.artPath("Сердце Довакина"));assertTrue(FixShopCatalog.artPath("../").matches("media/shop/art_[a-f0-9]{32}\\.webp"));}
}
