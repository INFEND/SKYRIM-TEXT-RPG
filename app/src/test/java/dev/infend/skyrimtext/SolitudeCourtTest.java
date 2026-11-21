package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeCourtTest {
    private GameEngine city(){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.progress.setQuestOutcome(SolitudeWolf.KEY,"COMPLETED");return g;
    }
    private void palace(GameEngine g){g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:2");}
    @Test public void tributeRequiresRealShrineAndGrantsHousePermission() throws Exception {
        GameEngine g=city();g.gold=30000;palace(g);g.choose("SOLITUDE:deed");assertFalse(SolitudeHome.owns(g));
        g.choose("SOLITUDE:court:tribute");assertTrue(SolitudeCourt.active(g,SolitudeCourt.TRIBUTE));assertEquals(1,g.progress.quantity(SolitudeCourt.HORN));
        assertFalse(g.progress.removeItem(SolitudeCourt.HORN,1));g.choose("SOLITUDE:court:tribute_finish");assertFalse(SolitudeHome.eligible(g));
        g.choose("GO_WORLD");g.choose("COURT:offer");assertFalse(SolitudeCourt.done(g,SolitudeCourt.OFFER));
        assertNotNull(WorldAtlas.find(SolitudeCourt.SHRINE));g.region=0;g.progress.location=SolitudeCourt.SHRINE;g.choose("COURT:offer");
        assertTrue(SolitudeCourt.done(g,SolitudeCourt.OFFER));assertEquals(0,g.progress.quantity(SolitudeCourt.HORN));
        g=GameEngine.fromJson(g.toJson().toString());assertTrue(SolitudeCourt.done(g,SolitudeCourt.OFFER));
        g.region=3;g.progress.location="Солитьюд";palace(g);g.choose("SOLITUDE:court:tribute_finish");assertTrue(SolitudeHome.eligible(g));
        g.choose("SOLITUDE:deed");assertTrue(SolitudeHome.owns(g));assertEquals(5000,g.gold);g.choose("SOLITUDE:deed");assertEquals(5000,g.gold);
    }
    @Test public void thaneRequiresFiveDistinctFavorsAndHouse() throws Exception {
        GameEngine g=city();palace(g);
        g.progress.setQuestOutcome(SolitudeCourt.TRIBUTE,"COMPLETED");
        for(String id:new String[]{SolitudeCanonical.KEY,SolitudeCanonical.RUM,SolitudeCanonical.WINE,SolitudeCanonical.ATTIRE})g.progress.setQuestOutcome(id,"COMPLETED");
        g.choose("SOLITUDE:court:thane");assertFalse(SolitudeCourt.done(g,SolitudeCourt.THANE));
        g.progress.setQuestOutcome(SolitudeCanonical.GRACE,"COMPLETED");
        g.choose("SOLITUDE:court:thane");assertFalse(SolitudeCourt.done(g,SolitudeCourt.THANE));
        g.fixShop.home.buy(4);g.choose("SOLITUDE:court:thane");assertTrue(SolitudeCourt.done(g,SolitudeCourt.THANE));
        g=GameEngine.fromJson(g.toJson().toString());assertTrue(SolitudeCourt.done(g,SolitudeCourt.THANE));
        assertEquals(5,SolitudeCourt.favors(g));
    }
    @Test public void queenRequiresLetterPriestBattlesAndBlessing() throws Exception {
        GameEngine g=city();g.level=1;g.choose("COURT:letter");assertFalse(SolitudeCourt.done(g,SolitudeCourt.LETTER));
        g.level=10;g.choose("COURT:letter");assertTrue(SolitudeCourt.done(g,SolitudeCourt.LETTER));palace(g);
        g.choose("SOLITUDE:court:queen");assertTrue(SolitudeCourt.active(g,SolitudeCourt.QUEEN));g.choose("SOLITUDE:court:queen_finish");assertFalse(SolitudeCourt.done(g,SolitudeCourt.QUEEN));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:4");g.choose("SOLITUDE:court:guard");assertEquals("SOLITUDE:4",g.mode);
        g.choose("SOLITUDE:back");g.choose(SolitudeCanonical.CATACOMBS);g.choose("SOLITUDE:court:styrr");assertTrue(SolitudeCourt.done(g,SolitudeCourt.KEY));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:4");g.choose("SOLITUDE:court:guard");assertEquals("COMBAT",g.mode);
        g.enemyHp=1;g.choose("ATTACK");assertTrue(SolitudeCourt.done(g,SolitudeCourt.GUARD));
        g.choose("GO_WORLD");g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:4");g.choose("SOLITUDE:court:boss");assertEquals("COMBAT",g.mode);
        g=GameEngine.fromJson(g.toJson().toString());g.enemyHp=1;g.choose("ATTACK");assertTrue(SolitudeCourt.done(g,SolitudeCourt.BOSS));
        g.choose("GO_WORLD");g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:4");g.choose("SOLITUDE:court:skull");assertEquals(1,g.progress.quantity(SolitudeCourt.SKULL));
        assertFalse(g.progress.removeItem(SolitudeCourt.SKULL,1));g.choose("SOLITUDE:back");g.choose(SolitudeCanonical.CATACOMBS);g.choose("SOLITUDE:court:bless");assertTrue(SolitudeCourt.done(g,SolitudeCourt.BLESS));assertEquals(0,g.progress.quantity(SolitudeCourt.SKULL));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:2");int gold=g.gold,reward=SolitudeWolf.reward(g);
        g.choose("SOLITUDE:court:queen_finish");assertTrue(SolitudeCourt.done(g,SolitudeCourt.QUEEN));assertEquals(1,g.progress.quantity(SolitudeCourt.SHIELD));assertEquals(gold+reward,g.gold);
        g.choose("SOLITUDE:court:queen_finish");assertEquals(gold+reward,g.gold);assertEquals(1,g.progress.quantity(SolitudeCourt.SHIELD));
    }
}
