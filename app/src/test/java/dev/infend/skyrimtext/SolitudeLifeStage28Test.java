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
    @Test public void oldSaveKeepsOriginalJobHistoryAndNewJobsStartUnused() throws Exception {
        org.json.JSONArray days=new org.json.JSONArray();for(int i=0;i<16;i++)days.put(1);
        SolitudeCity city=SolitudeCity.restore(new org.json.JSONObject().put("lastDay",days));
        org.json.JSONArray restored=city.save().getJSONArray("lastDay");
        assertEquals(24,restored.length());
        for(int i=0;i<16;i++)assertEquals(1,restored.getInt(i));
        for(int i=16;i<24;i++)assertEquals(0,restored.getInt(i));
    }
}
