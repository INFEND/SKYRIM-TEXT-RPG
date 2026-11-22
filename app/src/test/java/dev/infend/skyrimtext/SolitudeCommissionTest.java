package dev.infend.skyrimtext;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeCommissionTest {
    @Test public void workDaysPersistAndCannotDoubleCount() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.hour=10;g.stamina=100;
        g.mode="WORLD";g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        g.choose("SOLITUDE:job:0");
        g.choose("SOLITUDE:job:16");
        JSONObject first=g.solitude.save();
        assertEquals(1,first.optJSONArray("workDays").optInt(0));
        for(int n=0;n<2;n++){g.day++;g.hour=10;g.stamina=100;g.choose("SOLITUDE:job:0");}
        JSONObject saved=g.solitude.save();
        assertEquals(3,saved.optJSONArray("workDays").optInt(0));
        assertEquals(3,SolitudeCity.restore(saved).save().optJSONArray("workDays").optInt(0));
        g.choose("SOLITUDE:work-reward:0");
        int paid=g.gold;
        g.choose("SOLITUDE:work-reward:0");
        assertEquals(paid,g.gold);
        assertTrue(g.solitude.save().optJSONArray("workRewarded").optBoolean(0));
        assertEquals("HELPED",g.sceneState.status("npc","solitude_jala"));
    }
    @Test public void commissionCountsServiceNotLeisureOrTraining(){
        int[] service={0,3,4,7,9,10,12,14,16,18,20,22,23,24,25,26,27,28,29,30,31};
        for(int job:service)assertTrue("service job "+job,SolitudeCity.commissionEligible(job));
        int[] excluded={1,2,5,6,8,11,13,17,19};
        for(int job:excluded)assertFalse("non-service job "+job,SolitudeCity.commissionEligible(job));
        assertFalse(SolitudeCity.commissionEligible(-1));
        assertFalse(SolitudeCity.commissionEligible(32));
    }
    @Test public void trainingAndMealsDoNotAdvanceMultiDayCommission(){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.hour=10;g.stamina=100;g.gold=1000;
        g.mode="WORLD";g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:6");
        g.choose("SOLITUDE:job:13");
        assertEquals(0,g.solitude.save().optJSONArray("workDays").optInt(6));
        g.day++;g.hour=10;g.stamina=100;
        g.choose("SOLITUDE:job:13");
        assertEquals(0,g.solitude.save().optJSONArray("workDays").optInt(6));
        g.choose("SOLITUDE:job:12");
        assertEquals(1,g.solitude.save().optJSONArray("workDays").optInt(6));
        assertEquals(1,SolitudeCity.restore(g.solitude.save()).save().optJSONArray("workDays").optInt(6));
    }
    @Test public void cityLedgerReportsAllDistrictsWithoutPayingRemotely(){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.hour=10;g.stamina=100;
        g.mode="WORLD";g.choose("SOLITUDE:open");
        int before=g.gold;
        g.choose("SOLITUDE:work-ledger");
        assertTrue(g.message.contains("Главные ворота"));
        assertTrue(g.message.contains("Гавань Солитьюда"));
        assertTrue(g.message.contains("0/3"));
        assertEquals(before,g.gold);
        g.choose("SOLITUDE:visit:0");
        g.choose("SOLITUDE:job:0");
        g.choose("SOLITUDE:back");
        g.choose("SOLITUDE:work-ledger");
        assertTrue(g.message.contains("1/3 смен"));
        assertEquals(before+18,g.gold);
    }
    @Test public void oldSavesHaveNoEarnedCommission(){
        JSONObject saved=SolitudeCity.restore(new JSONObject()).save();
        assertEquals(0,saved.optJSONArray("workDays").optInt(0));
        assertFalse(saved.optJSONArray("workRewarded").optBoolean(0));
    }
    @Test public void dayZeroShiftCountsAndDonationCannotBePaidTwice() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.day=0;g.hour=10;g.stamina=100;
        g.mode="WORLD";g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        g.choose("SOLITUDE:job:0");
        assertEquals(1,g.solitude.save().optJSONArray("workDays").optInt(0));
        for(int n=1;n<=2;n++){g.day=n;g.hour=10;g.stamina=100;g.choose("SOLITUDE:job:0");}
        assertEquals(3,g.solitude.save().optJSONArray("workDays").optInt(0));
        int gold=g.gold,rep=g.holdReputation[3];
        g.choose("SOLITUDE:work-donate:0");
        assertEquals(gold,g.gold);
        assertEquals(Math.min(100,rep+4),g.holdReputation[3]);
        assertTrue(g.solitude.save().optJSONArray("workRewarded").optBoolean(0));
        g.choose("SOLITUDE:work-reward:0");
        g.choose("SOLITUDE:work-donate:0");
        assertEquals(gold,g.gold);
        assertEquals(Math.min(100,rep+4),g.holdReputation[3]);
        assertTrue(SolitudeCity.restore(g.solitude.save()).save().optJSONArray("workRewarded").optBoolean(0));
    }
    @Test public void legacyCommissionSaveStartsWithNoWorkedDay() {
        JSONObject old=new JSONObject();
        SolitudeCity restored=SolitudeCity.restore(old);
        assertEquals(-1,restored.save().optJSONArray("workLastDay").optInt(0));
        assertEquals(0,restored.save().optJSONArray("workDays").optInt(0));
    }

}
