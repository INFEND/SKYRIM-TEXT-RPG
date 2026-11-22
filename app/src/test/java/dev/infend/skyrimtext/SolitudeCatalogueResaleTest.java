package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;

/** Items sold beyond a merchant's depleted catalogue must remain available for repurchase. */
public class SolitudeCatalogueResaleTest {
    @Test public void catalogueSurplusCanBeBoughtBackAfterSave() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;g.gold=10000;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:5");g.choose("SOLITUDE:store:4");
        String id="Железный слиток";g.progress.quantity(id,2);
        g.choose("SOLITUDE:sell:4:0:max");
        assertEquals(0,g.progress.quantity(id));
        String scene=g.scene().toString();
        assertTrue("Sold stock must be available to buy back",scene.contains("Выкупить"));
        g=GameEngine.fromJson(g.toJson().toString());
        int before=g.gold;
        g.choose("SOLITUDE:rebuy:4:"+id);
        assertEquals(1,g.progress.quantity(id));
        assertTrue(g.gold<before);
        g.day+=2;
        g.choose("SOLITUDE:rebuy:4:"+id);
        assertEquals("Two-day stock refresh expires resale inventory",1,g.progress.quantity(id));
    }
}
