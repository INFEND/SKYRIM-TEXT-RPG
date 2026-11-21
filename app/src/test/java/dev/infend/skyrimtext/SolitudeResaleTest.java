package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeResaleTest {
    @Test public void resalePersistsAndExpires() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;g.gold=10000;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:5");g.choose("SOLITUDE:store:4");
        String id="Железный кинжал";g.progress.quantity(id,2);
        g.choose("SOLITUDE:extraSell:4:"+id+":max");assertEquals(0,g.progress.quantity(id));
        g=GameEngine.fromJson(g.toJson().toString());int gold=g.gold;
        g.choose("SOLITUDE:rebuy:4:"+id);assertEquals(1,g.progress.quantity(id));assertTrue(g.gold<gold);
        g.day+=2;g.choose("SOLITUDE:rebuy:4:"+id);assertEquals(1,g.progress.quantity(id));
    }
}
