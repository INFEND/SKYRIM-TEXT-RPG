package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.*;

public class CanonicalCampaignTest {
    @Test public void fullCampaignUsesBattleGatesAndSaves() throws Exception {
        GameEngine g=new GameEngine();
        g.start("Герой","Норд","Воин");
        g.choose("CANON_OPEN");
        assertEquals(1,g.canonicalStage);
        int fights=0;
        for(int i=1;i<=CanonicalCampaign.COUNT;i++){
            assertEquals(i,g.canonicalStage);
            assertEquals("CANON",g.mode);
            String action=g.scene().choices.get(0).action;
            g.choose(action);
            if("COMBAT".equals(g.mode)){
                fights++;
                g.choose("CANON_OPEN");
                assertEquals("COMBAT",g.mode);
                g=GameEngine.fromJson(g.toJson().toString());
                assertEquals("COMBAT",g.mode);
                assertEquals(i,g.canonicalStage);
                g.enemyHp=1;g.choose("ATTACK");
                assertEquals("CANON",g.mode);
                assertEquals(1,g.canonicalPhase);
                g.choose(action); // stale pre-battle button must not repeat fight
                assertEquals(1,g.canonicalPhase);
                g.choose(g.scene().choices.get(0).action);
            }
            assertEquals("COMPLETED",g.progress.questOutcome("canonical_"+i));
            g=GameEngine.fromJson(g.toJson().toString());
        }
        assertTrue(fights>=10);
        assertEquals(34,g.canonicalStage);
        assertEquals(31,g.canonicalShouts);
        assertEquals("WORLD",g.mode);
    }
    @Test public void preparationCannotBeFarmedAndOldSaveIsCompatible() throws Exception {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.choose("CANON_OPEN");
        String alt=g.scene().choices.get(1).action;g.choose(alt);
        int stamina=g.stamina;g.choose(alt);assertEquals(stamina,g.stamina);
        g=GameEngine.fromJson(g.toJson().toString());assertTrue(g.canonicalPrepared);
        assertEquals(1,g.canonicalStage);
        g.choose("GO_WORLD");g.choose("CANON_OPEN");assertEquals(1,g.canonicalStage);
        org.json.JSONObject old=g.toJson();old.remove("canonicalStage");
        assertEquals(0,GameEngine.fromJson(old.toString()).canonicalStage);
    }
}
