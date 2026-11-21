package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopWorldExtraTest {
 @Test public void purchasedMountsRemainOwned() throws Exception {
  GameEngine g=new GameEngine();FixShopWorld w=g.fixShop.world;
  assertTrue(w.buyMount(1));assertTrue(w.buyMount(2));assertFalse(w.buyMount(1));assertTrue(w.selectMount(1));assertEquals(3,w.travelBonus());
  assertTrue(w.buyPet(1));assertTrue(w.buyPet(2));assertTrue(w.selectPet(1));
  GameEngine h=GameEngine.fromJson(g.toJson().toString());assertTrue(h.fixShop.world.ownMount(2));assertTrue(h.fixShop.world.ownPet(2));assertEquals(3,h.fixShop.world.travelBonus());
 }
 @Test public void customQuestCannotPayTwice() throws Exception {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";g.progress.location="Вайтран";FixShopWorld w=g.fixShop.world;
  assertTrue(w.addQuest("Письмо","Доставить письмо","Вайтран","Ривервуд"));assertFalse(w.questAction(g,0,true));assertTrue(w.questAction(g,0,false));
  g.progress.location="Ривервуд";assertTrue(w.questAction(g,0,true));assertFalse(w.questAction(g,0,true));
  GameEngine h=GameEngine.fromJson(g.toJson().toString());assertEquals(2,h.fixShop.world.quests.get(0).optInt("stage"));
 }
}
