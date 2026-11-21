package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopStateTest {
 @Test public void savesAreIsolatedAndOldSavesLoad() throws Exception {
  GameEngine g=new GameEngine();g.fixSpent=150;g.progress.fixCarryBonus=50;
  String id=FixShopState.key("Безграничная ноша",150);g.fixShop.toggle(id);g.fixShop.record("Безграничная ноша","+50",150,1);
  GameEngine h=GameEngine.fromJson(g.toJson().toString());
  assertTrue(h.fixShop.favorite(id));assertEquals(1,h.fixShop.count(id));assertEquals(100,h.progress.carryLimit(),0.001);assertEquals(150,h.fixSpent);
  GameEngine another=new GameEngine();assertFalse(another.fixShop.favorite(id));assertEquals(0,another.fixSpent);assertEquals(50,another.progress.carryLimit(),0.001);
  org.json.JSONObject old=g.toJson();old.remove("fixShop");assertEquals(0,GameEngine.fromJson(old.toString()).fixShop.history().length());
 }
 @Test public void perksRefundOnlySpentPoints(){
  GameEngine g=new GameEngine();g.progress.perkPoints=10;g.progress.skillLevels[0]=100;
  assertTrue(g.progress.buyTalent(g,0));assertTrue(g.progress.branches.buy(g.progress,0));assertTrue(g.progress.resetFixPerks());assertEquals(10,g.progress.perkPoints);assertEquals(0,g.progress.talentRank(0));assertFalse(g.progress.resetFixPerks());
 }
 @Test public void explicitNpcOverrideIsSeparateFromNormalConfirmation() throws Exception {
  SceneState s=new SceneState();s.confirm("npc","test","NPC","DEAD");
  try{s.confirm("npc","test","NPC","MET");fail("Normal confirmation must not resurrect");}catch(org.json.JSONException expected){}
  assertTrue(s.fixNpc("test",50,true));assertEquals("MET",s.status("npc","test"));
  assertTrue(s.fixNpc("test",75,false));assertFalse(s.fixNpc("test",75,false));
  SceneState restored=SceneState.fromJson(s.toJson());assertEquals(75,restored.npcFacts().getJSONObject(0).getInt("relation"));
 }
}
