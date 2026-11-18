package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeCanonicalTest {
    @Test public void angelineQuestPersistsAndCannotRepeat() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:store:1");
        g.choose("SOLITUDE:angeline:finish");assertFalse("COMPLETED".equals(SolitudeCanonical.state(g)));
        g.choose("SOLITUDE:angeline:accept");assertEquals("ACTIVE",SolitudeCanonical.state(g));
        g.choose("SOLITUDE:angeline:aldis");assertFalse(SolitudeCanonical.informed(g));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:1");g.hour=23;g.choose("SOLITUDE:angeline:aldis");assertFalse(SolitudeCanonical.informed(g));
        g.hour=10;g.choose("SOLITUDE:angeline:aldis");assertTrue(SolitudeCanonical.informed(g));
        g=GameEngine.fromJson(g.toJson().toString());assertTrue(SolitudeCanonical.informed(g));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:store:1");
        int rep=g.holdReputation[3];g.choose("SOLITUDE:angeline:finish");assertEquals("COMPLETED",SolitudeCanonical.state(g));assertEquals(rep+1,g.holdReputation[3]);
        g.choose("SOLITUDE:angeline:finish");g.choose("SOLITUDE:angeline:accept");assertEquals(rep+1,g.holdReputation[3]);assertEquals("COMPLETED",SolitudeCanonical.state(g));
    }
}
