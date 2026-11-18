package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeCanonicalTest {
    @Test public void taarieRequiresRealEquippedOutfitAndPaysOnce() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:store:0");
        g.choose("SOLITUDE:attire:accept");assertEquals("ACTIVE",SolitudeCanonical.attireState(g));
        String outfit=SolitudeCanonical.OUTFIT;assertEquals(1,g.progress.quantity(outfit));assertFalse(g.progress.removeItem(outfit,1));
        g.choose("SOLITUDE:attire:accept");assertEquals(1,g.progress.quantity(outfit));g.choose("SOLITUDE:attire:finish");assertEquals("ACTIVE",SolitudeCanonical.attireState(g));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:2");g.choose("SOLITUDE:attire:show");assertFalse(SolitudeCanonical.shown(g));
        assertTrue(g.progress.equip(outfit));g.hour=20;g.choose("SOLITUDE:attire:show");assertFalse(SolitudeCanonical.shown(g));
        g.hour=10;g.choose("SOLITUDE:attire:show");assertTrue(SolitudeCanonical.shown(g));
        g=GameEngine.fromJson(g.toJson().toString());assertTrue(SolitudeCanonical.shown(g));assertTrue(g.progress.equipped(outfit));assertFalse(g.progress.removeItem(outfit,1));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:store:0");int gold=g.gold,reward=SolitudeCanonical.rumReward(g);
        g.choose("SOLITUDE:attire:finish");assertEquals("COMPLETED",SolitudeCanonical.attireState(g));assertEquals(gold+reward,g.gold);
        g.choose("SOLITUDE:attire:finish");assertEquals(gold+reward,g.gold);assertEquals(1,g.progress.quantity(outfit));assertTrue(g.progress.removeItem(outfit,1));
    }
    @Test public void sorexDeliveryPersistsAndPaysOnlyOnce() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:2");g.choose("SOLITUDE:rum:deliver");assertNull(SolitudeCanonical.rumState(g));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:6");g.hour=3;g.choose("SOLITUDE:rum:accept");assertNull(SolitudeCanonical.rumState(g));
        g.hour=10;g.choose("SOLITUDE:rum:accept");assertEquals("ACTIVE",SolitudeCanonical.rumState(g));
        g.choose("SOLITUDE:rum:deliver");assertEquals("ACTIVE",SolitudeCanonical.rumState(g));
        g=GameEngine.fromJson(g.toJson().toString());assertEquals("ACTIVE",SolitudeCanonical.rumState(g));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:2");g.hour=20;g.choose("SOLITUDE:rum:deliver");assertEquals("ACTIVE",SolitudeCanonical.rumState(g));
        g.hour=10;int original=g.gold;g.gold=Integer.MAX_VALUE;g.choose("SOLITUDE:rum:deliver");assertEquals("ACTIVE",SolitudeCanonical.rumState(g));assertEquals(Integer.MAX_VALUE,g.gold);
        g.gold=original;int rep=g.holdReputation[3],reward=SolitudeCanonical.rumReward(g);
        g.choose("SOLITUDE:rum:deliver");assertEquals("COMPLETED",SolitudeCanonical.rumState(g));assertEquals(original+reward,g.gold);assertEquals(rep+1,g.holdReputation[3]);
        g.choose("SOLITUDE:rum:deliver");g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:6");g.choose("SOLITUDE:rum:accept");
        assertEquals(original+reward,g.gold);assertEquals(rep+1,g.holdReputation[3]);assertEquals("COMPLETED",SolitudeCanonical.rumState(g));
    }
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
