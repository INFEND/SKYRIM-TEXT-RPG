package dev.infend.skyrimtext;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeCommissionTest {
    @Test public void workDaysPersistAndCannotDoubleCount(){
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
    }
    @Test public void oldSavesHaveNoEarnedCommission(){
        JSONObject saved=SolitudeCity.restore(new JSONObject()).save();
        assertEquals(0,saved.optJSONArray("workDays").optInt(0));
        assertFalse(saved.optJSONArray("workRewarded").optBoolean(0));
    }
}