package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;

public class SolitudeEncountersTest {
    private GameEngine city(){GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;return g;}
    private void visit(GameEngine g,int district){if("WORLD".equals(g.mode))g.choose("SOLITUDE:open");else g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:"+district);}
    @Test public void cargoQuestHasPersistentStagesAndOneReward() throws Exception{
        GameEngine g=city();visit(g,7);int gold=g.gold;
        g.choose("SOLITUDE:quest:0:finish");assertEquals(gold,g.gold);
        g.choose("SOLITUDE:talk:7");assertEquals("MET",g.sceneState.npcStatusByName("Виттория Вичи"));
        g.choose("SOLITUDE:quest:0:accept");assertEquals("ACTIVE",g.progress.questOutcome("solitude_cargo"));
        g.choose("SOLITUDE:quest:0:work");assertNull(g.progress.questOutcome("solitude_cargo_step"));
        visit(g,0);g.choose("SOLITUDE:quest:0:work");assertEquals(11,g.hour);
        g=GameEngine.fromJson(g.toJson().toString());assertEquals("COMPLETED",g.progress.questOutcome("solitude_cargo_step"));
        visit(g,7);g.choose("SOLITUDE:quest:0:finish");assertEquals(gold+45,g.gold);
        g.choose("SOLITUDE:quest:0:gift");g.choose("SOLITUDE:quest:0:finish");assertEquals(gold+45,g.gold);
        assertEquals("HELPED",g.sceneState.npcStatusByName("Виттория Вичи"));
    }
    @Test public void allFourQuestsSupportUnpaidEnding(){
        for(int i=0;i<SolitudeEncounters.QUESTS.length;i++){
            GameEngine g=city();SolitudeEncounters.Quest q=SolitudeEncounters.QUESTS[i];int gold=g.gold;
            visit(g,q.giver);g.choose("SOLITUDE:quest:"+i+":accept");visit(g,q.target);g.choose("SOLITUDE:quest:"+i+":work");
            visit(g,q.giver);g.choose("SOLITUDE:quest:"+i+":gift");
            assertEquals("COMPLETED",g.progress.questOutcome(q.key()));assertEquals("COMPLETED",g.progress.questOutcome(q.key()+"_gift"));assertEquals(gold,g.gold);assertEquals(q.rep+2,g.holdReputation[3]);
        }
    }
    @Test public void scheduleDeathAndWrongDistrictRejectActions() throws Exception{
        GameEngine g=city();visit(g,7);g.hour=23;g.choose("SOLITUDE:quest:0:accept");assertNull(g.progress.questOutcome("solitude_cargo"));
        g.hour=9;g.sceneState.confirm("npc","solitude_vittoria","Виттория Вичи","DEAD");g.choose("SOLITUDE:quest:0:accept");assertNull(g.progress.questOutcome("solitude_cargo"));
        visit(g,0);g.choose("SOLITUDE:quest:0:accept");assertNull(g.progress.questOutcome("solitude_cargo"));
    }
    private int relation(GameEngine g,String name){
        org.json.JSONArray a=g.sceneState.npcFacts();
        for(int i=0;i<a.length();i++){
            org.json.JSONObject n=a.optJSONObject(i);
            if(n!=null&&name.equals(n.optString("title")))return n.optInt("relation",50);
        }
        return 50;
    }
    @Test public void refusalRecoveryWorkAndGiftHavePersistentNonRepeatableRelations() throws Exception{
        GameEngine g=city();
        visit(g,7);
        g.choose("SOLITUDE:quest:0:accept");
        int before=relation(g,"Виттория Вичи");
        g.choose("SOLITUDE:quest:0:decline");
        assertEquals("DECLINED",g.progress.questOutcome("solitude_cargo"));
        assertEquals(before-10,relation(g,"Виттория Вичи"));
        g.choose("SOLITUDE:quest:0:decline");
        assertEquals(before-10,relation(g,"Виттория Вичи"));
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals(before-10,relation(g,"Виттория Вичи"));
        visit(g,7);
        g.choose("SOLITUDE:quest:0:reconsider");
        assertEquals("ACTIVE",g.progress.questOutcome("solitude_cargo"));
        int restored=relation(g,"Виттория Вичи");
        g.choose("SOLITUDE:quest:0:reconsider");
        assertEquals(restored,relation(g,"Виттория Вичи"));
        visit(g,0);
        g.choose("SOLITUDE:quest:0:work");
        int target=relation(g,"Яла");
        g.choose("SOLITUDE:quest:0:work");
        assertEquals(target,relation(g,"Яла"));
        visit(g,7);
        g.choose("SOLITUDE:quest:0:gift");
        int finished=relation(g,"Виттория Вичи");
        g.choose("SOLITUDE:quest:0:gift");
        assertEquals(finished,relation(g,"Виттория Вичи"));
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals(finished,relation(g,"Виттория Вичи"));
        assertEquals(target,relation(g,"Яла"));
    }
}

