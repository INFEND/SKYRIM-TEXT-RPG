package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopAdventureTest {
 @Test public void adventureCanOnlyProgressAtRealLocationsAndPaysOnce() throws Exception {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";
  assertTrue(FixShopAdventure.unlock(g,0));assertFalse(FixShopAdventure.unlock(g,0));
  g.progress.location="Хелген";assertFalse(FixShopAdventure.choose(g,"FIXQUEST:0:meet"));
  g.progress.location=FixShopAdventure.FIRST[0];assertTrue(FixShopAdventure.choose(g,"FIXQUEST:0:meet"));
  assertEquals(2,FixShopAdventure.stage(g,0));g.progress.location=FixShopAdventure.SECOND[0];
  assertTrue(FixShopAdventure.choose(g,"FIXQUEST:0:fight"));assertEquals("COMBAT",g.mode);assertFalse(FixShopAdventure.choose(g,"FIXQUEST:0:truth"));
  g.mode="RESULT";FixShopAdventure.victory(g,"FIX_QUEST:0");assertEquals(3,FixShopAdventure.stage(g,0));
  GameEngine h=GameEngine.fromJson(g.toJson().toString());int gold=h.gold;assertTrue(FixShopAdventure.choose(h,"FIXQUEST:0:truth"));assertEquals(gold+100,h.gold);assertFalse(FixShopAdventure.choose(h,"FIXQUEST:0:reward"));
  assertEquals("COMPLETED",h.progress.questOutcome(FixShopAdventure.id(0)));assertEquals(0,FixShopAdventure.stage(new GameEngine(),0));
 }
}
