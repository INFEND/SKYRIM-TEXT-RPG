package dev.infend.skyrimtext;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AiNpcRewritesTest {
    private static AiGameTurn turn(JSONObject effects)throws Exception{
        JSONObject root=new JSONObject().put("scene",
            "Ты возвращаешься к знакомому месту, где свершилась беда. "+
            "Магический свет постепенно охватывает каменный круг. "+
            "Когда вспышка угасает, свидетельство произошедшего становится частью летописи. "+
            "Каждый присутствующий делает собственные выводы и ждёт твоих следующих решений.");
        JSONArray options=new JSONArray();
        for(int i=0;i<10;i++)options.put(new JSONObject()
            .put("title","Продолжить поиски "+i)
            .put("detail","Осмотреть окружающее пространство "+i)
            .put("chance",JSONObject.NULL).put("skill",""));
        root.put("choices",options).put("effects",effects).put("journal","Завершено волшебное действие");
        return AiGameTurn.parseModel(root.toString(),-2);
    }
    @Test public void confirmedDeadNpcMayReturnWithRelationshipChange()throws Exception{
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";
        g.sceneState.setContext(g.day,g.hour,"Ривервуд");
        g.sceneState.confirm("npc","traveler01","Странник","DEAD");
        JSONArray rewrite=new JSONArray().put(new JSONObject().put("id","traveler01")
            .put("relation",80).put("resurrect",true));
        turn(new JSONObject().put("npcRewrites",rewrite)).apply(g,true);
        assertEquals("MET",g.sceneState.status("npc","traveler01"));
        GameEngine restored=GameEngine.fromJson(g.toJson().toString());
        assertEquals("MET",restored.sceneState.status("npc","traveler01"));
        assertEquals(80,restored.sceneState.npcFacts().getJSONObject(0).getInt("relation"));
    }
    @Test public void invalidSecondRewriteDoesNotModifyTheFirst()throws Exception{
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";
        g.sceneState.confirm("npc","traveler01","Странник","DEAD");
        JSONArray updates=new JSONArray().put(new JSONObject().put("id","traveler01")
            .put("relation",80).put("resurrect",true)).put(new JSONObject()
            .put("id","missing_id").put("relation",90));
        try{turn(new JSONObject().put("npcRewrites",updates)).apply(g,true);
            fail("Cannot mutate partially");
        }catch(JSONException expected){}
        assertEquals("DEAD",g.sceneState.status("npc","traveler01"));
    }
}
