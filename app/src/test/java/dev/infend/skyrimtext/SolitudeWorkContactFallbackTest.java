package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.*;

/** Work uses the actual available contact and never bypasses a hostile resident. */
public class SolitudeWorkContactFallbackTest {
    private GameEngine market() {
        GameEngine g=new GameEngine();
        g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        return g;
    }
    @Test public void scheduledResidentCanSuperviseWhenMarketSupervisorIsHostile() throws Exception {
        GameEngine g=market();
        g.sceneState.confirm("npc","solitude_jala","Яла","HOSTILE");
        int gold=g.gold,day=g.day;
        assertTrue(SolitudeResidents.present(g,8,0));
        g.choose("SOLITUDE:job:16");
        assertEquals(gold+9,g.gold);
        assertEquals("HOSTILE",g.sceneState.npcStatusByName("Яла"));
        assertEquals("MET",g.sceneState.npcStatusByName(SolitudeResidents.NAMES[8]));
        g.choose("SOLITUDE:job:16");
        assertEquals(gold+9,g.gold);
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals("HOSTILE",g.sceneState.npcStatusByName("Яла"));
        assertEquals("MET",g.sceneState.npcStatusByName(SolitudeResidents.NAMES[8]));
        g.choose("SOLITUDE:job:16");
        assertEquals(gold+9,g.gold);
    }
    @Test public void hostileResidentCannotReplaceHostileSupervisor() throws Exception {
        GameEngine g=market();
        g.sceneState.confirm("npc","solitude_jala","Яла","HOSTILE");
        g.sceneState.confirm("npc","solitude_"+SolitudeResidents.IDS[8],SolitudeResidents.NAMES[8],"HOSTILE");
        int gold=g.gold,stamina=g.stamina,hour=g.hour;
        g.choose("SOLITUDE:job:16");
        assertEquals(gold,g.gold);
        assertEquals(stamina,g.stamina);
        assertEquals(hour,g.hour);
    }
}
