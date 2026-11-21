package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopDecisionTest {
 @Test public void rewriteOnlyRecordedChoice(){
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";g.fixShop.editorEnabled=true;g.canonicalStage=27;g.canonicalTreaty=1;g.canonicalPaarthurnax=1;
  int xp=g.xp,gold=g.gold;assertTrue(CanonicalCampaign.fixDecision(g,true,3));assertEquals(3,g.canonicalTreaty);assertTrue(CanonicalCampaign.fixDecision(g,false,2));assertEquals(27,g.canonicalStage);assertEquals(xp,g.xp);assertEquals(gold,g.gold);assertFalse(CanonicalCampaign.fixDecision(g,false,2));
 }
 @Test public void encounterRequestIsSavedAndCannotBeOverwritten() throws Exception{
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";g.fixShop.editorEnabled=true;
  assertFalse(RoadEvents.fixQueue(g,"giant"));assertTrue(RoadEvents.fixQueue(g,"wolves"));assertFalse(RoadEvents.fixQueue(g,"hunter"));
  GameEngine h=GameEngine.fromJson(g.toJson().toString());assertEquals("wolves",h.fixShop.world.queuedEncounter);
 }
}
