package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeLifeStage28Test {
    @Test public void appendedWorkRespectsDistrictAndDailyLimit() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        int gold=g.gold;
        g.choose("SOLITUDE:job:23");assertEquals(gold,g.gold);
        g.choose("SOLITUDE:job:16");assertEquals(gold+9,g.gold);assertEquals(11,g.hour);
        g.choose("SOLITUDE:job:16");assertEquals(gold+9,g.gold);
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:job:16");assertEquals(gold+9,g.gold);
    }
    @Test public void malformedAndFutureJobHistoryIsBounded() throws Exception {
        org.json.JSONArray days=new org.json.JSONArray().put(-7).put(org.json.JSONObject.NULL).put("invalid").put(4);
        for(int i=4;i<40;i++)days.put(i+1);
        org.json.JSONArray restored=SolitudeCity.restore(new org.json.JSONObject().put("lastDay",days)).save().getJSONArray("lastDay");
        assertEquals(32,restored.length());
        assertEquals(-1,restored.getInt(0));assertEquals(-1,restored.getInt(1));assertEquals(-1,restored.getInt(2));
        for(int i=3;i<32;i++)assertEquals(i+1,restored.getInt(i));
    }
    @Test public void oldSaveKeepsOriginalJobHistoryAndNewJobsStartUnused() throws Exception {
        for(int oldSize:new int[]{16,24,32}){
            org.json.JSONArray days=new org.json.JSONArray();for(int i=0;i<oldSize;i++)days.put(i+1);
            SolitudeCity city=SolitudeCity.restore(new org.json.JSONObject().put("lastDay",days));
            org.json.JSONArray restored=city.save().getJSONArray("lastDay");
            assertEquals(32,restored.length());
            for(int i=0;i<oldSize;i++)assertEquals(i+1,restored.getInt(i));
            for(int i=oldSize;i<32;i++)assertEquals(-1,restored.getInt(i));
        }
    }
}
