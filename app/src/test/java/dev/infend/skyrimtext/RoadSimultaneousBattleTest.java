package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoadSimultaneousBattleTest {
    private GameEngine hero(){GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.progress.location="Вайтран";g.region=0;g.mode="ROAD";g.road.id="imperial_prisoner";g.road.destination="Ривервуд";g.hp=g.maxHp=500;for(GameEngine.Choice c:g.scene().choices)if(c.action.endsWith(":free")){g.choose(c.action);break;}return g;}
    @Test public void chooseTargetWithoutTurnAndRetainEachWound() throws Exception {
        GameEngine g=hero();assertEquals(3,g.road.livingCount(g));int first=g.enemyHp;int turns=g.battleTurns;int hp=g.hp;
        String target=null;for(GameEngine.Choice c:g.scene().choices)if(c.action.startsWith("ROAD_TARGET:")&&c.action.endsWith(":2"))target=c.action;assertNotNull(target);g.choose(target);assertEquals(3,g.road.groupCurrent());assertEquals(turns,g.battleTurns);assertEquals(hp,g.hp);
        g.enemyHp=2;g.road.syncGroup(g);g=GameEngine.fromJson(g.toJson().toString());assertEquals(2,g.enemyHp);assertEquals(first,g.road.targetHp(g,0));g.choose("ATTACK");assertEquals("COMBAT",g.mode);assertEquals(2,g.road.livingCount(g));assertEquals(0,g.road.targetHp(g,2));assertTrue(g.hp<hp);assertEquals(0,g.victories);
    }
    @Test public void legacySequentialSaveMigratesWithoutRevivingDefeatedEnemy() throws Exception{
        GameEngine g=hero();g.enemyHp=1;g.choose("ATTACK");org.json.JSONObject j=g.toJson();org.json.JSONObject road=j.getJSONObject("road");road.remove("groupHp");road.remove("groupMax");road.remove("groupDamage");g=GameEngine.fromJson(j.toString());assertEquals(2,g.road.livingCount(g));assertEquals(0,g.road.targetHp(g,0));
    }
}
