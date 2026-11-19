package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeResidentsTest {
    @Test public void itinerariesAreExclusiveAndMemorySurvivesTravel() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        g.choose("SOLITUDE:resident:talk:1");
        assertEquals("MET",g.sceneState.npcStatusByName("Сорекс Виний"));
        int gold=g.gold,rep=g.holdReputation[3];
        g.hour=11;String before=g.message;g.choose("SOLITUDE:resident:talk:1");assertEquals(before,g.message);
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:6");
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:resident:memory:1");assertTrue(g.message.contains("50/100"));
        g.choose("SOLITUDE:resident:talk:1");assertTrue(g.message.contains("Снова встретились"));
        assertEquals(gold,g.gold);assertEquals(rep,g.holdReputation[3]);
    }
    @Test public void unavailableResidentsCannotBeContacted() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=12;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:2");
        g.sceneState.confirm("npc","existing_elisif","Элисиф Прекрасная","DEAD");
        String before=g.message;g.choose("SOLITUDE:resident:talk:0");assertEquals(before,g.message);
        g.choose("SOLITUDE:resident:talk:4");assertEquals("MET",g.sceneState.npcStatusByName("Сибилла Стентор"));
        assertEquals(-1,SolitudeResidents.district(3,20));
        assertEquals(3,SolitudeResidents.district(3,16));
        assertEquals(6,SolitudeResidents.district(3,17));
    }
}
