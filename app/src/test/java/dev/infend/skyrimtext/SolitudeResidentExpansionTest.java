package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeResidentExpansionTest {
    private GameEngine city(int district,int hour){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=hour;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:"+district);return g;
    }
    @Test public void guardMemoryFollowsSchedule() throws Exception {
        GameEngine g=city(1,10);int gold=g.gold;
        g.choose("SOLITUDE:resident:talk:7");assertEquals("MET",g.sceneState.npcStatusByName("Ахтар"));
        g.hour=18;String before=g.message;g.choose("SOLITUDE:resident:talk:7");assertEquals(before,g.message);
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:6");g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:resident:memory:7");assertTrue(g.message.contains("50/100"));assertEquals(gold,g.gold);
    }
    @Test public void newResidentsRespectAvailabilityAndExistingMemory() throws Exception {
        GameEngine g=city(0,12);
        g.sceneState.confirm("npc","existing_evette","Иветта Сан","MET");
        g.choose("SOLITUDE:resident:trust:8");assertEquals("INFORMED",g.sceneState.npcStatusByName("Иветта Сан"));
        g.sceneState.confirm("npc","existing_noster","Ностер Орлиный Глаз","DEAD");
        String before=g.message;g.choose("SOLITUDE:resident:talk:10");assertEquals(before,g.message);
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:3");g.choose("SOLITUDE:resident:talk:9");
        assertEquals("MET",g.sceneState.npcStatusByName("Илди"));
        assertEquals(3,SolitudeResidents.district(9,16));assertEquals(6,SolitudeResidents.district(9,17));assertEquals(-1,SolitudeResidents.district(9,20));
    }
}
