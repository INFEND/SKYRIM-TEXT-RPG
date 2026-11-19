package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeResidentsTest {
    @Test public void additionalResidentsRespectScheduleAndPersist() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=12;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:3");
        g.choose("SOLITUDE:resident:talk:11");
        assertEquals("MET",g.sceneState.npcStatusByName("Гирауд Жиман"));
        g.choose("SOLITUDE:resident:talk:12");
        assertEquals("MET",g.sceneState.npcStatusByName("Инге Шесть Пальцев"));
        g.hour=18;String before=g.message;g.choose("SOLITUDE:resident:talk:12");assertEquals(before,g.message);
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:6");
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:resident:talk:12");assertTrue(g.message.contains("Снова встретились"));
        g.choose("SOLITUDE:resident:talk:13");assertEquals("MET",g.sceneState.npcStatusByName("Лисетта"));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:0");g.hour=10;
        g.choose("SOLITUDE:resident:talk:14");assertEquals("MET",g.sceneState.npcStatusByName("Джала"));
        assertEquals(-1,SolitudeResidents.district(14,18));
    }

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
    @Test public void newResidentsAndTopicsShareMemoryAcrossDistricts() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=12;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        int gold=g.gold,rep=g.holdReputation[3];
        g.choose("SOLITUDE:resident:topic:5");
        assertEquals("MET",g.sceneState.npcStatusByName("Гисли"));
        assertTrue(g.message.contains("городских новостей"));
        g.hour=15;String before=g.message;g.choose("SOLITUDE:resident:topic:5");assertEquals(before,g.message);
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:6");
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:resident:talk:5");assertTrue(g.message.contains("Снова встретились"));
        assertEquals(gold,g.gold);assertEquals(rep,g.holdReputation[3]);
        assertEquals(2,SolitudeResidents.district(6,14));
        assertEquals(7,SolitudeResidents.district(6,15));
        assertEquals(-1,SolitudeResidents.district(6,18));
    }
    @Test public void trustAndAdviceAreGatedAndPersistWithoutRepeatRewards() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        int gold=g.gold,rep=g.holdReputation[3];
        String before=g.message;g.choose("SOLITUDE:resident:advice:1");assertEquals(before,g.message);
        g.choose("SOLITUDE:resident:talk:1");
        g.choose("SOLITUDE:resident:trust:1");
        assertEquals("INFORMED",g.sceneState.npcStatusByName("Сорекс Виний"));
        before=g.message;g.choose("SOLITUDE:resident:trust:1");assertEquals(before,g.message);
        g.hour=11;g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:6");
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:resident:advice:1");assertTrue(g.message.contains("часы смены"));
        g.choose("SOLITUDE:resident:memory:1");assertTrue(g.message.contains("60/100"));
        assertEquals(gold,g.gold);assertEquals(rep,g.holdReputation[3]);
    }
}
