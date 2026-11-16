package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoadGroupBattleTest {
    private GameEngine hero(){GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.hp=g.maxHp=500;g.progress.location="Вайтран";g.region=0;g.mode="WORLD";return g;}
    private void setup(GameEngine g,String id){g.road.id=id;g.road.destination="Ривервуд";g.road.segmentFrom="Вайтран";g.road.segmentTo="Ривервуд";g.mode="ROAD";}
    private void choose(GameEngine g,String verb){for(GameEngine.Choice c:g.scene().choices)if(c.action.endsWith(":"+verb)){g.choose(c.action);return;}fail("Missing "+verb);}
    @Test public void prisonerIsNotFreedUntilAllGuardsDefeatedAndSaveWorks() throws Exception {
        GameEngine g=hero();setup(g,"imperial_prisoner");int gold=g.gold;choose(g,"free");assertEquals("COMBAT",g.mode);assertEquals(3,g.road.groupSize());int rep=g.reputation;
        g.enemyHp=1;g.choose("ATTACK");assertEquals("COMBAT",g.mode);assertEquals(2,g.road.groupCurrent());assertEquals(gold,g.gold);assertEquals(rep,g.reputation);
        g=GameEngine.fromJson(g.toJson().toString());assertEquals(2,g.road.groupCurrent());g.enemyHp=1;g.choose("ATTACK");assertEquals("COMBAT",g.mode);assertEquals(3,g.road.groupCurrent());g.enemyHp=1;g.choose("ATTACK");
        assertEquals("RESULT",g.mode);assertEquals(rep+1,g.reputation);assertEquals(5,g.factions.trust(5));assertEquals(-5,g.factions.trust(4));int paid=g.gold;g=GameEngine.fromJson(g.toJson().toString());g.choose("GO_WORLD");assertEquals(rep+1,g.reputation);assertEquals(paid,g.gold);assertFalse(g.road.pending());
    }
    @Test public void companionGiantAndCivilWarConsequences() {
        GameEngine g=hero();setup(g,"giant");choose(g,"fight");g.battleTurns=1;assertTrue(g.road.allyDamage(g,0)>0);
        g=hero();setup(g,"civil_battle");choose(g,"empire");for(int i=0;i<3;i++){g.enemyHp=1;g.choose("ATTACK");}assertEquals(5,g.factions.trust(4));assertEquals(-5,g.factions.trust(5));
    }
    @Test public void nonlethalPrisonerReleaseCostsRealGold() {
        GameEngine g=hero();g.gold=150;setup(g,"storm_prisoner");choose(g,"bribe");assertEquals("RESULT",g.mode);assertEquals(50,g.gold);assertEquals(5,g.factions.trust(4));
    }
}
