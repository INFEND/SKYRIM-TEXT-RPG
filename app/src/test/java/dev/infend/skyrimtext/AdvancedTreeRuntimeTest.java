package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class AdvancedTreeRuntimeTest {
    @Test public void everyNodeHasValidPrerequisiteAndRoundTrips() throws Exception {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.progress.perkPoints=500;
        assertEquals(55,AdvancedPerks.NAMES.length);
        assertEquals(55,AdvancedPerks.SKILL.length);assertEquals(55,AdvancedPerks.NEED.length);assertEquals(55,AdvancedPerks.EFFECT.length);
        for(int s=0;s<18;s++){g.progress.skillLevels[s]=100;assertTrue(g.progress.buyTalent(g,s));}
        for(int i=0;i<55;i++){assertTrue(AdvancedPerks.NAMES[i],g.progress.advanced.buy(g.progress,i));assertFalse(g.progress.advanced.buy(g.progress,i));}
        GameEngine restored=GameEngine.fromJson(g.toJson().toString());
        for(int i=0;i<55;i++)assertTrue(restored.progress.advanced.has(i));
        assertEquals(6,restored.magicCost());assertTrue(restored.healingAmount()>45);
    }
    @Test public void decisionsPersist() throws Exception {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="WORLD";g.progress.location="Данстар";g.region=7;
        org.json.JSONObject data=new org.json.JSONObject().put("index",new org.json.JSONArray(new int[]{0,0,0,8,0,0,0})).put("phase",new org.json.JSONArray(new int[]{0,0,0,2,0,0,0}));
        g.factionStories=FactionStories.fromJson(data);
        assertTrue(g.factionStories.decide(g,3,0));assertFalse(g.factionStories.decide(g,3,1));assertEquals(1,g.factionStories.cicero());
        g=GameEngine.fromJson(g.toJson().toString());assertEquals(1,g.factionStories.cicero());
        assertTrue(g.factionStories.advance(g,3));assertEquals(9,g.factionStories.done(3));
        assertTrue(g.factionStories.dialogue(3).contains("остался в живых"));
    }
}
