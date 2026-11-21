package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudePardonTest {
    @Test public void pardonIsLocalLimitedAndPersists() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode=SolitudeJustice.MODE;
        g.road.addCityBounty(3,50);g.road.addCityBounty(0,25);int gold=g.gold;
        g.choose("SOLITUDE:justice:pardon");assertEquals(50,g.road.bounty(3));
        g.progress.setQuestOutcome(SolitudeCourt.THANE,"COMPLETED");
        g.road.addCityBounty(3,1950);g.choose("SOLITUDE:justice:pardon");assertEquals(2000,g.road.bounty(3));
        g.gold=3000;g.choose("SOLITUDE:justice:pay");g.road.addCityBounty(3,100);gold=g.gold;
        g.choose("SOLITUDE:justice:pardon");assertEquals(0,g.road.bounty(3));assertEquals(25,g.road.bounty(0));assertEquals(gold,g.gold);
        g=GameEngine.fromJson(g.toJson().toString());g.road.addCityBounty(3,10);g.choose("SOLITUDE:justice:pardon");assertEquals(10,g.road.bounty(3));
    }
}
