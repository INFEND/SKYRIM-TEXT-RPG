package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeJordisTravelTest {
    @Test public void dismissAwayFromHomePersistsAndStopsCombatSupport() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.fixShop.home.buy(4);g.progress.setQuestOutcome(SolitudeCourt.THANE,"COMPLETED");
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:home");g.choose("SOLITUDE:home:jordis:follow");
        g.choose("GO_WORLD");g.region=0;g.progress.location="Вайтран";
        assertTrue(g.scene().choices.stream().anyMatch(c->c.action.equals("JORDIS:dismiss")));
        assertTrue(g.solitude.jordisDamage(g,2,100)>0);
        g.mode="COMBAT";g.choose("JORDIS:dismiss");assertTrue(g.solitude.jordisDamage(g,2,100)>0);
        g.mode="WORLD";g.choose("JORDIS:talk");g.choose("JORDIS:dismiss");
        assertEquals(0,g.solitude.jordisDamage(g,2,100));
        g=GameEngine.fromJson(g.toJson().toString());assertEquals(0,g.solitude.jordisDamage(g,2,100));
        g.choose("JORDIS:talk");assertFalse(g.scene().choices.stream().anyMatch(c->c.action.equals("JORDIS:dismiss")));
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.choose("SOLITUDE:open");g.choose("SOLITUDE:home");g.choose("SOLITUDE:home:jordis:follow");
        assertTrue(g.solitude.jordisDamage(g,2,100)>0);
    }
}
