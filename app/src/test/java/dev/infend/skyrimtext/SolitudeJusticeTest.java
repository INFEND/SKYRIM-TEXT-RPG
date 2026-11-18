package dev.infend.skyrimtext;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;
public class SolitudeJusticeTest {
    private GameEngine city(int fine) throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.gold=1000;
        JSONObject state=new JSONObject();state.put("roadBounties",new org.json.JSONArray(new int[]{0,0,0,fine,0,0,0,0,0,0}));g.road=RoadEvents.restore(state);return g;
    }
    private void enter(GameEngine g){g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:1");g.choose(SolitudeJustice.MODE);}
    @Test public void fineUsesExistingLedgerAndCannotBePaidTwice() throws Exception {
        GameEngine g=city(250);enter(g);assertEquals(SolitudeJustice.MODE,g.mode);
        g=GameEngine.fromJson(g.toJson().toString());assertEquals(SolitudeJustice.MODE,g.mode);
        g.choose("SOLITUDE:justice:pay");assertEquals(750,g.gold);assertEquals(0,g.road.bounty(3));
        g.choose("SOLITUDE:justice:pay");assertEquals(750,g.gold);
    }
    @Test public void prisonAndConfiscationReuseSentence() throws Exception {
        GameEngine g=city(600);g.progress.addItem("Похищенное кольцо",1);int day=g.day;enter(g);
        g.choose("SOLITUDE:justice:serve");assertEquals(day,g.day);assertEquals(600,g.road.bounty(3));assertEquals(0,g.progress.quantity("Похищенное кольцо"));assertEquals(SolitudeJustice.CELL,g.mode);
        g.choose("GO_WORLD");g.choose("SOLITUDE:back");g.choose("LOC:Вайтран");assertEquals(SolitudeJustice.CELL,g.mode);assertFalse(g.restHours(24,true));
        g.choose("SOLITUDE:cell:day");assertEquals(day+1,g.day);assertEquals(600,g.road.bounty(3));
        g=GameEngine.fromJson(g.toJson().toString());assertEquals(SolitudeJustice.CELL,g.mode);
        g.choose("SOLITUDE:cell:day");assertEquals(day+2,g.day);assertEquals(0,g.road.bounty(3));assertEquals(SolitudeJustice.MODE,g.mode);
        g.choose("SOLITUDE:cell:day");assertEquals(day+2,g.day);
    }
    @Test public void worldSentenceAndEscapeHaveConsequences() throws Exception {
        GameEngine g=city(600);g.choose("ROADJAIL:3");assertEquals(SolitudeJustice.CELL,g.mode);
        int picks=g.progress.quantity("Отмычка");g.choose("SOLITUDE:cell:escape");
        assertEquals(picks-1,g.progress.quantity("Отмычка"));
        assertTrue(g.road.bounty(3)>=650);
        int fine=g.road.bounty(3);g.choose("SOLITUDE:cell:escape");assertEquals(fine,g.road.bounty(3));
    }
    @Test public void wrongDistrictAndInsufficientFundsCannotClearFine() throws Exception {
        GameEngine g=city(250);g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:justice:pay");assertEquals(250,g.road.bounty(3));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:1");g.choose(SolitudeJustice.MODE);g.gold=0;g.choose("SOLITUDE:justice:pay");assertEquals(250,g.road.bounty(3));assertEquals(0,g.gold);
    }
}
