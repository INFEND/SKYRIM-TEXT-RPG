package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeExtraJobsTest {
    private GameEngine city(){GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;g.stamina=100;g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");return g;}
    @Test public void newJobPersistsAndCannotReplay() throws Exception {
        GameEngine g=city();int gold=g.gold;
        g.choose("SOLITUDE:job:24");assertEquals(gold+10,g.gold);assertEquals(11,g.hour);
        g=GameEngine.fromJson(g.toJson().toString());g.choose("SOLITUDE:job:24");assertEquals(gold+10,g.gold);
        g.day++;g.hour=10;g.choose("SOLITUDE:job:24");assertEquals(gold+20,g.gold);
    }
    @Test public void wrongDistrictAndClosedShiftCannotPay(){
        GameEngine g=city();int gold=g.gold;
        g.choose("SOLITUDE:job:25");assertEquals(gold,g.gold);
        g.hour=23;g.choose("SOLITUDE:job:24");assertEquals(gold,g.gold);
        g.hour=10;g.stamina=0;g.choose("SOLITUDE:job:24");assertEquals(gold,g.gold);
    }
}
