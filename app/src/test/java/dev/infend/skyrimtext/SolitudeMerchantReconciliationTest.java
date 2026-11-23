package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;

/** Complete conflict -> reconciliation -> trading -> save/reload cycle. */
public class SolitudeMerchantReconciliationTest {
    @Test public void conflictReconciliationRestoresNeutralTermsAcrossSave() throws Exception {
        GameEngine g=new GameEngine();
        g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";
        g.hour=9;g.gold=10000;g.stamina=100;
        g.sceneState.setContext(g.day,g.hour,"Солитьюд");
        g.sceneState.confirm("npc","solitude_taarie","Таари","HOSTILE");
        g.choose("GO_WORLD");g.choose("SOLITUDE:open");
        g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:store:0");
        int before=g.gold;
        g.choose("SOLITUDE:buy:0:0");
        assertEquals("Hostility must block purchases",before,g.gold);
        g.choose("SOLITUDE:merchant:reconcile:0");
        assertEquals("INFORMED",g.sceneState.npcStatusByName("Таари"));
        assertEquals(50,g.sceneState.npcFacts().getJSONObject(0).getInt("relation"));
        assertEquals(90,g.stamina);
        int neutralPrice=SolitudeShops.price(g,ItemCatalog.find("Шляпа путника"));
        g.choose("SOLITUDE:buy:0:0");
        assertEquals(before-neutralPrice,g.gold);
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals(50,g.sceneState.npcFacts().getJSONObject(0).getInt("relation"));
        assertEquals("INFORMED",g.sceneState.npcStatusByName("Таари"));
        int savedGold=g.gold;
        g.choose("SOLITUDE:merchant:reconcile:0");
        assertEquals(savedGold,g.gold);
    }
}
