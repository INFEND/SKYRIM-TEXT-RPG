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
}
