package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class NpcMemoryTest {
 @Test public void memoryRoundTrip() throws Exception {SceneState s=new SceneState();s.setContext(2,8,"Вайтран");s.confirm("npc","test","Тест","MET");s.setContext(3,12,"Ривервуд");s.confirm("npc","test","Тест","HELPED");s=SceneState.fromJson(s.toJson());org.json.JSONObject n=s.npcFacts().getJSONObject(0);assertEquals(70,n.getInt("relation"));assertEquals(3,n.getInt("lastDay"));assertEquals("Ривервуд",n.getString("lastLocation"));assertEquals(2,n.getJSONArray("history").length());assertTrue(s.confirmedContext().contains("70/100")); }
 @Test public void largerBoundAndTerminalDeath() throws Exception {SceneState s=new SceneState();for(int i=0;i<256;i++)s.confirm("npc","id_"+i,"NPC "+i,"MET");assertEquals(256,SceneState.fromJson(s.toJson()).npcFacts().length());try{s.confirm("npc","overflow","Overflow","MET");fail();}catch(org.json.JSONException expected){}s.confirm("npc","id_0","NPC 0","DEAD");try{s.confirm("npc","id_0","NPC 0","MET");fail();}catch(org.json.JSONException expected){} }
}
