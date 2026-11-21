package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;

/** Resident reconciliation must share the same clock and save contract as city jobs. */
public class SolitudeResidentClockTest {
    private GameEngine city(int hour) {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";
        g.hour=hour;g.stamina=100;g.gold=1000;
        return g;
    }
    @Test public void reconciliationSurvivesSaveAndCannotPayTwice() throws Exception {
        GameEngine g=city(10);
        g.sceneState.confirm("npc","solitude_addvar","Аддвар","HOSTILE");
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        int stamina=g.stamina, gold=g.gold;
        g.choose("SOLITUDE:resident:reconcile:2");
        assertEquals(11,g.hour);assertEquals(stamina-10,g.stamina);
        assertEquals(gold,g.gold);
        assertEquals("INFORMED",g.sceneState.npcStatusByName("Аддвар"));
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals(11,g.hour);
        int after=g.stamina;
        g.choose("SOLITUDE:resident:reconcile:2");
        assertEquals(after,g.stamina);assertEquals(gold,g.gold);
    }
    @Test public void residentCannotStartReconciliationAcrossDeparture() throws Exception {
        GameEngine g=city(16);
        g.sceneState.confirm("npc","solitude_addvar","Аддвар","HOSTILE");
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        int stamina=g.stamina;
        g.choose("SOLITUDE:resident:reconcile:2");
        assertEquals(16,g.hour);assertEquals(stamina,g.stamina);
        assertEquals("HOSTILE",g.sceneState.npcStatusByName("Аддвар"));
    }
}
