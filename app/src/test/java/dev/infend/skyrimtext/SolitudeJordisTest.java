package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeJordisTest {
    @Test public void housecarlRequiresTitleAndPersists() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.fixShop.home.buy(4);g.choose("SOLITUDE:open");g.choose("SOLITUDE:home");g.choose("SOLITUDE:home:jordis:follow");
        assertEquals(0,g.solitude.jordisDamage(g,2,100));
        g.progress.setQuestOutcome(SolitudeCourt.THANE,"COMPLETED");g.choose("SOLITUDE:home:jordis:follow");
        assertTrue(g.solitude.jordisDamage(g,2,100)>0);assertEquals(0,g.solitude.jordisDamage(g,1,100));assertEquals(1,g.solitude.jordisDamage(g,2,1));
        g=GameEngine.fromJson(g.toJson().toString());assertTrue(g.solitude.jordisDamage(g,2,100)>0);
        g.choose("SOLITUDE:home:jordis:stay");assertEquals(0,g.solitude.jordisDamage(g,2,100));
    }
    @Test public void bladeRewardOnlyOnce() {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.progress.setQuestOutcome(SolitudeWolf.KEY,"COMPLETED");g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:2");
        g.choose("SOLITUDE:court:blade");assertEquals(0,g.progress.quantity(SolitudeCourt.BLADE));
        g.progress.setQuestOutcome(SolitudeCourt.THANE,"COMPLETED");g.choose("SOLITUDE:court:blade");g.choose("SOLITUDE:court:blade");
        assertEquals(1,g.progress.quantity(SolitudeCourt.BLADE));assertTrue(SolitudeCourt.done(g,SolitudeCourt.BLADE_REWARD));
    }
}
