package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeMerchantDialogueTest {
    @Test public void merchantMemorySurvivesSaveAndRespectsShop() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:store:0");
        int gold=g.gold,rep=g.holdReputation[3];
        g.choose("SOLITUDE:merchant:talk:1");assertEquals("",g.sceneState.npcStatusByName("Анжелина Моррар"));
        g.choose("SOLITUDE:merchant:talk:0");assertEquals("MET",g.sceneState.npcStatusByName("Таари"));
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:merchant:memory:0");assertTrue(g.message.contains("50/100"));
        g.choose("SOLITUDE:merchant:talk:0");assertTrue(g.message.contains("Снова встретились"));
        assertEquals(gold,g.gold);assertEquals(rep,g.holdReputation[3]);
        g.hour=23;String before=g.message;g.choose("SOLITUDE:merchant:talk:0");assertEquals(before,g.message);
    }
    @Test public void blacksmithUsesExistingNpcIdentity() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:5");g.choose("SOLITUDE:talk:5");
        g.choose("SOLITUDE:store:4");g.choose("SOLITUDE:merchant:talk:4");
        assertTrue(g.message.contains("Снова встретились"));
        int count=0;org.json.JSONArray facts=g.sceneState.npcFacts();
        for(int i=0;i<facts.length();i++)if("Бейранд".equals(facts.getJSONObject(i).optString("title")))count++;
        assertEquals(1,count);
        assertFalse(SolitudeEncounters.present(g,-1));
    }
}
