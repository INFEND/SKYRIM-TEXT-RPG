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
    @Test public void encountersHaveNoMinimumLevel() {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.level=1;g.hour=23;
        assertTrue(RoadEvents.availableAt(g,"boethiah",0));assertTrue(RoadEvents.availableAt(g,"giant_spider",0));assertTrue(RoadEvents.availableAt(g,"giant_spider_duel",0));assertTrue(RoadEvents.availableAt(g,"gargoyles",0));assertTrue(RoadEvents.availableAt(g,"island_mutiny",9));assertTrue(RoadEvents.availableAt(g,"college",0));assertFalse(RoadEvents.availableAt(g,"island_mutiny",0));assertFalse(RoadEvents.availableAt(g,"gargoyles",9));assertTrue(RoadEvents.eventCount()>=160);
        g.hour=12;assertFalse(RoadEvents.availableAt(g,"gargoyles",0));assertTrue(RoadEvents.availableAt(g,"vampire_sun",0));
    }
    @Test public void oneTimeIslandEncountersStayResolved() {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");assertTrue(RoadEvents.availableAt(g,"island_flying_wizard",9));g.progress.setQuestOutcome("road_island_flying_wizard","COMPLETED");assertFalse(RoadEvents.availableAt(g,"island_flying_wizard",9));
    }
    @Test public void allCatalogEntriesHaveIdenticalEligibilityAtLevelsOneAndHundred() {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");java.util.List<String> ids=RoadEvents.eventIds();assertEquals(ids.size(),new java.util.HashSet<>(ids).size());
        for(int hour:new int[]{2,12,23})for(int region=0;region<10;region++)for(String id:ids){g.hour=hour;g.level=1;boolean first=RoadEvents.availableAt(g,id,region);g.level=100;assertEquals(id+" @ "+region,first,RoadEvents.availableAt(g,id,region));}
    }
}
