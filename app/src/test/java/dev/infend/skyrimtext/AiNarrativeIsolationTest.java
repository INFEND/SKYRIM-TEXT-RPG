package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.*;

public class AiNarrativeIsolationTest {
    private GameEngine hero(){
        GameEngine g=new GameEngine();
        g.started=true;g.mode="WORLD";g.day=8;
        g.katherine.stage=5;g.katherine.following=true;g.katherine.romance=true;g.katherine.trust=80;
        g.alma.stage=8;g.alma.companion=true;g.alma.romance=true;
        return g;
    }
    @Test public void aiSuppressesScriptedInitiativesWithoutLosingRelationships(){
        GameEngine g=hero();g.setAiNarrativeMode(true);
        g.katherine.observeTravel(g);g.katherine.autonomous(g);g.alma.observeTravel(g);
        assertEquals("",g.katherine.lastRemark);assertEquals("",g.alma.lastRemark);
        assertTrue(g.katherine.romance);assertTrue(g.alma.romance);
        assertEquals(80,g.katherine.trust);assertEquals(-1,g.katherine.lastAutonomyDay);
    }
    @Test public void scriptedModeStillGeneratesInitiatives(){
        GameEngine g=hero();g.katherine.autonomous(g);
        assertFalse(g.katherine.lastRemark.isEmpty());
    }
    @Test public void nativeAiInitiativeRemainsAvailable(){
        GameEngine g=hero();g.setAiNarrativeMode(true);
        assertTrue(KatherineInitiative.apply(g,KatherineInitiative.TALK,true));
        assertEquals(g.day,g.katherine.companion.lastAiDay);
        g.katherine.observeTravel(g);g.katherine.autonomous(g);
        assertEquals(KatherineInitiative.TALK,g.katherine.companion.lastAiIntent);
    }
    @Test public void scriptedSceneCannotReplaceAiMode(){
        GameEngine g=hero();g.mode=KatherineStory.MODE;g.setAiNarrativeMode(true);
        assertEquals("WORLD",g.mode);
        g.choose("KATH:open");assertEquals("WORLD",g.mode);
        g.choose("CANON_OPEN");assertEquals("WORLD",g.mode);
        g.setAiNarrativeMode(false);g.choose("KATH:open");
        assertEquals(KatherineStory.MODE,g.mode);
    }
    @Test public void currentBattleIsPreserved(){
        GameEngine g=hero();g.mode="COMBAT";g.battleKind="CANONICAL";g.enemyHp=50;
        g.setAiNarrativeMode(true);
        assertEquals("COMBAT",g.mode);assertTrue(g.nativeSceneRequired());assertEquals(50,g.enemyHp);
    }
}
