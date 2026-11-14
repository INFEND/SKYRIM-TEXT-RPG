package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FactionStoriesRuntimeTest {
    private GameEngine hero(){GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="WORLD";return g;}
    @Test public void allChaptersReachableAndBattleProgressPersists() throws Exception{
        GameEngine g=hero();int total=0;
        for(int f=0;f<7;f++){
            total+=g.factionStories.count(f);
            // A separate hero per faction avoids intentionally opposing allegiances.
            GameEngine h=hero();
            for(int i=0;i<h.factionStories.count(f);i++){
                FactionStories.Chapter chapter=h.factionStories.chapter(f);WorldAtlas.Place p=WorldAtlas.find(chapter.place);
                assertNotNull(chapter.place,p);assertTrue(chapter.place,TravelPlanner.plan(h,p).allowed);
                h.progress.location=p.name;h.region=p.region;h.mode="WORLD";h.stamina=h.maxStamina;
                assertTrue(h.factionStories.advance(h,f));assertEquals(1,h.factionStories.phase(f));
                if(!chapter.enemy.isEmpty()){
                    assertTrue(h.factionStories.advance(h,f));assertEquals("COMBAT",h.mode);
                    h=GameEngine.fromJson(h.toJson().toString());assertEquals("FACTION",h.battleKind);
                    assertEquals(f,h.factionBattle);h.choose("GO_WORLD");assertEquals("COMBAT",h.mode);
                    h.enemyHp=1;h.choose("ATTACK");assertEquals(2,h.factionStories.phase(f));
                }else{
                    // Unlucky checks may be retried on a later day, never the same day.
                    for(int attempt=0;attempt<1000&&h.factionStories.phase(f)==1;attempt++){
                        h.day++;h.stamina=h.maxStamina;assertTrue(h.factionStories.advance(h,f));
                    }
                    assertEquals(2,h.factionStories.phase(f));
                }
                assertTrue(h.factionStories.advance(h,f));assertEquals(i+1,h.factionStories.done(f));
                h=GameEngine.fromJson(h.toJson().toString());assertEquals(i+1,h.factionStories.done(f));
            }
            assertTrue(h.factionStories.completed(f));assertFalse(h.factionStories.advance(h,f));
        }
        assertEquals(67,total);
    }
    @Test public void rivalFactionBlockedAndOldSaveLoads() throws Exception{
        GameEngine g=hero();g.progress.location="Форт Храгстад";g.region=3;
        assertTrue(g.factionStories.advance(g,4));g.progress.location="Виндхельм";g.region=2;
        assertFalse(g.factionStories.advance(g,5));assertFalse(g.factions.perform(g,5));
        org.json.JSONObject old=g.toJson();old.remove("factionStories");old.remove("factionBattle");
        GameEngine restored=GameEngine.fromJson(old.toString());assertEquals(0,restored.factionStories.done(4));assertEquals(-1,restored.factionBattle);
    }
}
