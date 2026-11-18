package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeWolfTest {
    @Test public void caveCombatAndReportAreSequentialAndPersist() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.choose("WOLF:guard");assertEquals("WORLD",g.mode);
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:2");g.choose("SOLITUDE:wolf:finish");assertNull(SolitudeWolf.state(g));
        g.choose("SOLITUDE:wolf:accept");assertTrue(SolitudeWolf.active(g));g.choose("GO_WORLD");g.choose("WOLF:guard");assertEquals("WORLD",g.mode);
        // Simulate arrival through the existing atlas without depending on random road encounters.
        g.progress.location=SolitudeWolf.CAVE;g.choose("WOLF:ritual");assertEquals("WORLD",g.mode);
        g.choose("WOLF:guard");assertEquals("COMBAT",g.mode);assertEquals(SolitudeWolf.KIND+"guard",g.battleKind);
        g.choose("WOLF:ritual");assertEquals(SolitudeWolf.KIND+"guard",g.battleKind);
        g.enemyHp=1;g.choose("ATTACK");assertTrue(SolitudeWolf.done(g,SolitudeWolf.GUARD));assertFalse(SolitudeWolf.cleared(g));
        g.choose("GO_WORLD");g.choose("WOLF:guard");assertEquals("WORLD",g.mode);
        g.choose("WOLF:ritual");assertEquals("COMBAT",g.mode);
        g=GameEngine.fromJson(g.toJson().toString());assertEquals(SolitudeWolf.KIND+"ritual",g.battleKind);assertEquals("COMBAT",g.mode);
        g.enemyHp=1;g.choose("ATTACK");assertTrue(SolitudeWolf.cleared(g));
        g.choose("GO_WORLD");g.choose("WOLF:ritual");assertEquals("WORLD",g.mode);
        g.progress.location="Солитьюд";g.hour=10;g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:2");
        int before=g.gold,reward=SolitudeWolf.reward(g);g.choose("SOLITUDE:wolf:finish");assertTrue(SolitudeWolf.done(g,SolitudeWolf.KEY));assertEquals(before+reward,g.gold);
        g.choose("SOLITUDE:wolf:finish");g.choose("SOLITUDE:wolf:accept");assertEquals(before+reward,g.gold);
    }
}
