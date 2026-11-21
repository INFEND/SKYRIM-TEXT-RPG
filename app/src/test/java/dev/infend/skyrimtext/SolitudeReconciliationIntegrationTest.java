package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;

public class SolitudeReconciliationIntegrationTest {
    private GameEngine city() {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";
        g.hour=10;g.gold=10000;g.stamina=100;
        return g;
    }
    @Test public void hostileMerchantBlocksTradeUntilReconciliationAfterReload() throws Exception {
        GameEngine g=city();g.sceneState.confirm("npc","solitude_sayma","Сайма","HOSTILE");
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:store:2");
        int gold=g.gold,items=g.progress.quantity("Отмычка");
        g.choose("SOLITUDE:buy:2:0");
        assertEquals(gold,g.gold);assertEquals(items,g.progress.quantity("Отмычка"));
        g=GameEngine.fromJson(g.toJson().toString());
        assertTrue(g.scene().choices.stream().anyMatch(c->c.action.equals("SOLITUDE:merchant:reconcile:2")));
        assertTrue("Reconciliation must remain visible when the shop is closed to hostile customers",
            g.scene().choices.stream().anyMatch(c->c.action.equals("SOLITUDE:merchant:reconcile:2")));
        int stamina=g.stamina,hour=g.hour;
        g.choose("SOLITUDE:merchant:reconcile:2");
        assertEquals(stamina-10,g.stamina);assertEquals(hour+1,g.hour);
        assertEquals("INFORMED",g.sceneState.npcStatusByName("Сайма"));
        g.choose("SOLITUDE:merchant:reconcile:2");
        assertEquals(stamina-10,g.stamina);
        g.choose("SOLITUDE:buy:2:0");
        assertEquals(items+1,g.progress.quantity("Отмычка"));assertTrue(g.gold<gold);
    }
    @Test public void hostileSupervisorBlocksShiftUntilReconciliation() throws Exception {
        GameEngine g=city();g.sceneState.confirm("npc","solitude_vittoria","Виттория Вичи","HOSTILE");
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:7");
        int gold=g.gold;
        g.choose("SOLITUDE:job:14");assertEquals(gold,g.gold);
        assertTrue(g.scene().choices.stream().anyMatch(c->c.action.equals("SOLITUDE:supervisor:reconcile:7")));
        g.choose("SOLITUDE:supervisor:reconcile:7");
        assertEquals("INFORMED",g.sceneState.npcStatusByName("Виттория Вичи"));
        g.choose("SOLITUDE:job:14");assertTrue(g.gold>gold);
    }
    @Test public void deadMerchantCannotReconcile() throws Exception {
        GameEngine g=city();g.sceneState.confirm("npc","solitude_sayma","Сайма","DEAD");
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:store:2");
        assertFalse(g.scene().choices.stream().anyMatch(c->c.action.equals("SOLITUDE:merchant:reconcile:2")));
        int stamina=g.stamina;
        g.choose("SOLITUDE:merchant:reconcile:2");
        assertEquals(stamina,g.stamina);assertEquals("DEAD",g.sceneState.npcStatusByName("Сайма"));
    }
}
