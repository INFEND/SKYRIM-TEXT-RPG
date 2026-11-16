package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoadEventsTest {
    @Test public void uniqueEncounterPersistsAndCannotReplay() throws Exception {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.progress.location="Ривервуд";g.region=0;g.mode="WORLD";
        g.choose("LOC:Вайтран");assertEquals("ROAD",g.mode);assertEquals("Ривервуд",g.progress.location);
        g=GameEngine.fromJson(g.toJson().toString());assertEquals("ROAD",g.mode);
        String watch=g.scene().choices.get(1).action;g.choose("LOC:Рифтен");assertEquals("ROAD",g.mode);
        g.choose(watch);assertEquals("COMPLETED",g.progress.questOutcome("road_giant"));assertEquals("RESULT",g.mode);
        g.choose(watch);g.choose("GO_WORLD");assertEquals("Вайтран",g.progress.location);assertFalse(g.road.pending());
    }
    @Test public void ciceroHelpSurvivesSaveAndIsSingleUse() throws Exception {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.progress.location="Вайтран";g.region=0;g.mode="WORLD";
        g.choose("LOC:Ферма Лорея");assertEquals("ROAD",g.mode);
        int gold=g.gold;String help=g.scene().choices.get(0).action;g.choose(help);assertTrue(g.gold>gold);int paid=g.gold;
        g=GameEngine.fromJson(g.toJson().toString());g.choose(help);assertEquals(paid,g.gold);g.choose("GO_WORLD");
        assertEquals("Ферма Лорея",g.progress.location);assertEquals("COMPLETED",g.progress.questOutcome("road_cicero"));
    }
    @Test public void canonicalRoadResumesWithoutChangingQuestStage() throws Exception {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.canonicalStage=4;g.progress.location="Ривервуд";g.region=0;g.mode="WORLD";
        g.choose("CANON_OPEN");assertEquals("ROAD",g.mode);g=GameEngine.fromJson(g.toJson().toString());g.choose(g.scene().choices.get(1).action);g.choose("GO_WORLD");assertEquals("CANON",g.mode);assertEquals(4,g.canonicalStage);assertEquals("Драконий Предел",g.progress.location);
    }
}
