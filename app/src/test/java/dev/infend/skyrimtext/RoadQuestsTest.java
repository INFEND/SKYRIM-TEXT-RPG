package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoadQuestsTest {
    private GameEngine hero(){GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="WORLD";return g;}
    @Test public void everyTaskHasRealAtlasDestination(){for(RoadQuests.Task t:RoadQuests.TASKS)assertNotNull(t.place,WorldAtlas.find(t.place));}
    @Test public void treasureCanOnlyBeClaimedAtDestinationAndOnce() throws Exception {
        GameEngine g=hero();RoadQuests.lead(g,"treasure");int gold=g.gold;
        g.progress.location="Ривервуд";g.choose("ROADQUEST:treasure");assertEquals(gold,g.gold);
        g.progress.location="Привал Хамвира";g.choose("ROADQUEST:treasure");assertEquals("COMPLETED",g.progress.questOutcome("lead_treasure"));assertEquals(gold+80,g.gold);
        g=GameEngine.fromJson(g.toJson().toString());g.choose("ROADQUEST:treasure");assertEquals(gold+80,g.gold);
    }
    @Test public void questBattleRequiresVictoryBeforeClaim() throws Exception {
        GameEngine g=hero();RoadQuests.lead(g,"mistwatch");g.progress.location="Туманная застава";g.mode="WORLD";
        g.choose("ROADQUEST:mistwatch");assertEquals("COMBAT",g.mode);assertEquals("ACTIVE",g.progress.questOutcome("lead_mistwatch"));
        g=GameEngine.fromJson(g.toJson().toString());g.enemyHp=1;g.choose("ATTACK");assertEquals("COMPLETED",g.progress.questOutcome("road_cleared_mistwatch"));
        g.choose("ROADQUEST:mistwatch");assertEquals("COMPLETED",g.progress.questOutcome("lead_mistwatch"));
    }
    @Test public void arvalQuestIsSaveableAndAwardsMountOnce() throws Exception {
        GameEngine g=hero();assertTrue(g.road.beginArvak(g));g.choose(g.scene().choices.get(0).action);assertEquals("ROAD",g.mode);g=GameEngine.fromJson(g.toJson().toString());g.choose(g.scene().choices.get(0).action);assertTrue(g.road.hasMount(g));g.choose("GO_WORLD");assertFalse(g.road.beginArvak(g));
    }
    @Test public void chainedTasksUnlockOnlyAfterPreviousStageAndSave() throws Exception {GameEngine g=hero();RoadQuests.offer(g,"afflicted");assertNull(g.progress.questOutcome("lead_afflicted_ruins"));g.progress.location="Святилище Периайта";g.choose("ROADQUEST:afflicted");assertEquals("ACTIVE",g.progress.questOutcome("lead_afflicted_ruins"));g=GameEngine.fromJson(g.toJson().toString());g.progress.location="Бтардамз";g.mode="WORLD";g.choose("ROADQUEST:afflicted_ruins");assertEquals("COMBAT",g.mode);g.enemyHp=1;g.choose("ATTACK");g.choose("ROADQUEST:afflicted_ruins");assertEquals("ACTIVE",g.progress.questOutcome("lead_afflicted_return"));g.progress.location="Святилище Периайта";g.mode="WORLD";g.choose("ROADQUEST:afflicted_return");assertEquals("COMPLETED",g.progress.questOutcome("lead_afflicted_return"));}
    @Test public void prerequisitesAndBranchExclusivityPersist() throws Exception {GameEngine g=hero();assertFalse(RoadEvents.availableAt(g,"deep_in_cups",0));assertFalse(RoadEvents.availableAt(g,"louis_revenge",1));assertFalse(RoadEvents.availableAt(g,"island_morgrul",9));g.progress.location="Рифтен";g.choose("ROADSTART:frost_keep");assertNull(g.progress.questOutcome("lead_frost_keep"));g.progress.setQuestOutcome("lead_frost_lodge","COMPLETED");g.progress.addItem("Родословная Мороза",1);g.choose("ROADSTART:frost_keep");g.choose("ROADQUEST:frost_keep");assertTrue(RoadEvents.availableAt(g,"louis_revenge",1));assertTrue(g.road.hasMount(g));g=GameEngine.fromJson(g.toJson().toString());g.mode="WORLD";g.choose("ROADSTART:frost_maven");assertNull(g.progress.questOutcome("lead_frost_maven"));assertFalse(RoadEvents.availableAt(g,"maven_revenge",1));}
    @Test public void debtPaymentStopsCollectors(){GameEngine g=hero();g.progress.location="Воронья Скала";g.gold=1200;RoadQuests.offer(g,"mogrul_debt");assertTrue(RoadEvents.availableAt(g,"island_morgrul",9));g.choose("ROADDEBT:pay");assertEquals(200,g.gold);assertFalse(RoadEvents.availableAt(g,"island_morgrul",9));g.choose("ROADDEBT:pay");assertEquals(200,g.gold);}
}
