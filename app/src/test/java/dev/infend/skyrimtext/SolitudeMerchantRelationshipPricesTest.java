package dev.infend.skyrimtext;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/** Merchant terms follow persistent NPC relationships, without affecting strangers. */
public class SolitudeMerchantRelationshipPricesTest {
    private static int quote(String method,GameEngine g,int shop,ItemCatalog.Item item) throws Exception {
        Method m=SolitudeShops.class.getDeclaredMethod(method,GameEngine.class,int.class,ItemCatalog.Item.class);
        m.setAccessible(true);
        return (Integer)m.invoke(null,g,shop,item);
    }
    @Test public void pricesReactToTrustAndSurviveReload() throws Exception {
        GameEngine g=new GameEngine();
        g.start("Тест","Норд","Воин");
        ItemCatalog.Item item=ItemCatalog.find("Отмычка");
        assertNotNull(item);
        int normalBuy=quote("purchasePrice",g,2,item);
        int normalSell=quote("salePrice",g,2,item);
        g.sceneState.confirm("npc","solitude_sayma","Сайма","MET");
        assertTrue(g.sceneState.fixNpc("solitude_sayma",90,false));
        int trustedBuy=quote("purchasePrice",g,2,item);
        int trustedSell=quote("salePrice",g,2,item);
        assertTrue(trustedBuy<=normalBuy);
        assertTrue(trustedSell>=normalSell);
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals(trustedBuy,quote("purchasePrice",g,2,item));
        assertEquals(trustedSell,quote("salePrice",g,2,item));
        assertTrue(g.sceneState.fixNpc("solitude_sayma",10,false));
        assertTrue(quote("purchasePrice",g,2,item)>=normalBuy);
        assertTrue(quote("salePrice",g,2,item)<=normalSell);
        assertEquals(normalBuy,quote("purchasePrice",g,1,item));
    }
}
