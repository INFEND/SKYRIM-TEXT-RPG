package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopDecisionTest {
 @Test public void rewriteOnlyRecordedChoice(){
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";g.canonicalStage=27;g.canonicalTreaty=1;g.canonicalPaarthurnax=1;
  int xp=g.xp,gold=g.gold;
  assertFalse(CanonicalCampaign.fixDecision(g,true,3));
  assertFalse(CanonicalCampaign.fixDecision(g,false,2));
  assertEquals(1,g.canonicalTreaty);assertEquals(27,g.canonicalStage);
  assertEquals(xp,g.xp);assertEquals(gold,g.gold);
 }
 @Test public void encounterRequestIsSavedAndCannotBeOverwritten() throws Exception{
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";
  assertFalse(RoadEvents.fixQueue(g,"giant"));
  assertFalse(RoadEvents.fixQueue(g,"wolves"));
  assertFalse(RoadEvents.fixQueue(g,"hunter"));
  GameEngine h=GameEngine.fromJson(g.toJson().toString());
  assertEquals("",h.fixShop.world.queuedEncounter);
 }
}
