package dev.infend.skyrimtext;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AiQuestUpdatesTest {
    private static AiGameTurn turn(JSONArray updates)throws Exception{
        JSONObject root=new JSONObject().put("scene",
            "Ты возвращаешься в город и передаёшь подтверждённые сведения страже. "+
            "Капитан сверяет печати, расспрашивает свидетелей и благодарит за помощь. "+
            "Теперь поиски можно продолжить, и новая цель записана в журнал героя.");
        JSONArray choices=new JSONArray();
        for(int i=0;i<10;i++)choices.put(new JSONObject().put("title","Действие номер "+i)
            .put("detail","Уточнить новое направление поисков "+i)
            .put("skill","").put("chance",JSONObject.NULL));
        root.put("choices",choices);
        root.put("effects",new JSONObject().put("questUpdates",updates));
        root.put("journal","Продолжить расследование");
        return AiGameTurn.parseModel(root.toString(),-2);
    }
    private static JSONObject quest(int stage,String status)throws Exception{
        return new JSONObject().put("id","lost_letter").put("title","Пропавшее письмо")
            .put("objective","Расспросить свидетелей").put("stage",stage).put("status",status);
    }
    @Test public void newQuestStagesPersistAndCannotBeRolledBack()throws Exception{
        GameEngine game=new GameEngine();game.start("Герой","Норд","Воин");game.mode="WORLD";
        turn(new JSONArray().put(quest(1,"ACTIVE"))).apply(game,true);
        assertEquals(1,game.progress.aiQuestRecords.getJSONObject("lost_letter").getInt("stage"));
        turn(new JSONArray().put(quest(3,"COMPLETED"))).apply(game,true);
        GameEngine restored=GameEngine.fromJson(game.toJson().toString());
        assertEquals("COMPLETED",restored.progress.aiQuestRecords.getJSONObject("lost_letter").getString("status"));
        try{turn(new JSONArray().put(quest(0,"ACTIVE"))).apply(restored,true);
            fail("Cannot reset a final quest");
        }catch(JSONException expected){}
        assertEquals(3,restored.progress.aiQuestRecords.getJSONObject("lost_letter").getInt("stage"));
    }
    @Test public void malformedSecondQuestCannotCommitFirst()throws Exception{
        GameEngine game=new GameEngine();game.start("Герой","Норд","Воин");game.mode="WORLD";
        JSONArray updates=new JSONArray().put(quest(1,"ACTIVE")).put(
            new JSONObject().put("id","bad_quest").put("title","")
                .put("stage",1).put("status","ACTIVE"));
        try{turn(updates).apply(game,true);fail("Batch should be atomic");}
        catch(JSONException expected){}
        assertEquals(0,game.progress.aiQuestRecords.length());
    }
    @Test public void promisedQuestRewardsArePaidOnlyOnce()throws Exception{
        GameEngine game=new GameEngine();game.start("Герой","Норд","Воин");game.mode="WORLD";
        int startingGold=game.gold,startingXp=game.xp;
        JSONObject offered=quest(1,"ACTIVE").put("rewardGold",125).put("rewardXp",2);
        turn(new JSONArray().put(offered)).apply(game,true);
        assertEquals(startingGold,game.gold);
        assertEquals(startingXp,game.xp);
        assertEquals(125,game.progress.aiQuestRecords.getJSONObject("lost_letter").getInt("rewardGold"));
        JSONObject finished=quest(3,"COMPLETED").put("rewardGold",125).put("rewardXp",2);
        turn(new JSONArray().put(finished)).apply(game,true);
        assertEquals(startingGold+125,game.gold);
        assertEquals(startingXp+2,game.xp);
        GameEngine restored=GameEngine.fromJson(game.toJson().toString());
        turn(new JSONArray().put(finished)).apply(restored,true);
        assertEquals(startingGold+125,restored.gold);
        assertEquals(startingXp+2,restored.xp);
    }
    @Test public void malformedRewardsNeverGrantQuestMoney()throws Exception{
        GameEngine game=new GameEngine();game.start("Герой","Норд","Воин");game.mode="WORLD";
        int initialGold=game.gold;
        JSONObject invalid=quest(1,"COMPLETED").put("rewardGold","500");
        try{
            turn(new JSONArray().put(invalid)).apply(game,true);
            fail("Invalid reward must be rejected");
        }catch(JSONException expected){}
        assertEquals(initialGold,game.gold);
        assertEquals(0,game.progress.aiQuestRecords.length());
    }
}
