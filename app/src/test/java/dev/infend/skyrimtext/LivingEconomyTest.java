package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class LivingEconomyTest {
 @Test public void accrualIsIdempotent(){GameEngine g=new GameEngine();g.gold=2000;g.day=1;assertTrue(g.economy.invest(g,1000));assertEquals(1000,g.gold);g.day=4;g.economy.sync(g.day);assertEquals(60,g.economy.accrued);g.economy=LivingEconomy.fromJson(g.economy.toJson());assertEquals(60,g.economy.claim(g));assertEquals(0,g.economy.claim(g));assertEquals(1060,g.gold);assertTrue(g.economy.withdraw(g,1000));assertEquals(2060,g.gold);}
 @Test public void relationshipPersistsWithoutReplay() throws Exception {SceneState s=new SceneState();s.confirm("npc","test","Тест","MET");s.confirm("npc","test","Тест","HELPED");assertEquals(70,s.npcFacts().getJSONObject(0).getInt("relation"));s=SceneState.fromJson(s.toJson());s.confirm("npc","test","Тест","HELPED");assertEquals(70,s.npcFacts().getJSONObject(0).getInt("relation"));s.confirm("npc","test","Тест","HOSTILE");assertEquals(30,s.npcFacts().getJSONObject(0).getInt("relation"));}
}
