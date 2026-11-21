package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.*;

/** Offline regression coverage of eight real-inventory resident favors. */
public class SolitudeFavorsTest {
    private GameEngine city(int district){
        GameEngine g=new GameEngine();
        g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=12;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:"+district);return g;
    }
    @Test public void deliveryAndPaymentPersistAndNeverDuplicate() throws Exception{
        GameEngine g=city(6);
        assertTrue(g.progress.addItem("Пшеница",3));
        int gold=g.gold,amount=g.progress.quantity("Пшеница");
        g.choose("SOLITUDE:favor:0:accept");
        assertEquals("ACTIVE",g.progress.questOutcome("solitude_favor_sorex"));
        g.choose("SOLITUDE:favor:0:deliver");
        assertEquals(amount-3,g.progress.quantity("Пшеница"));
        assertEquals(13,g.hour);
        assertEquals("COMPLETED",g.progress.questOutcome("solitude_favor_sorex_delivered"));
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:favor:0:deliver");
        assertEquals(amount-3,g.progress.quantity("Пшеница"));
        g.choose("SOLITUDE:favor:0:pay");
        assertEquals(gold+45,g.gold);
        assertEquals("COMPLETED",g.progress.questOutcome("solitude_favor_sorex"));
        assertEquals("HELPED",g.sceneState.npcStatusByName("Сорекс Виний"));
        g.choose("SOLITUDE:favor:0:pay");g.choose("SOLITUDE:favor:0:gift");
        assertEquals(gold+45,g.gold);
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals("COMPLETED",g.progress.questOutcome("solitude_favor_sorex"));
    }
    @Test public void donationsImproveRelationsWithoutPayingGold() throws Exception{
        GameEngine g=city(6);
        g.progress.addItem("Факел",2);
        int gold=g.gold,rep=g.holdReputation[3],n=g.progress.quantity("Факел");
        g.choose("SOLITUDE:favor:7:accept");
        g.choose("SOLITUDE:favor:7:deliver");
        g.choose("SOLITUDE:favor:7:gift");
        assertEquals(gold,g.gold);
        assertEquals(rep+3,g.holdReputation[3]);
        assertEquals(n-2,g.progress.quantity("Факел"));
        assertEquals("HELPED",g.sceneState.npcStatusByName("Лисетта"));
        g.choose("SOLITUDE:resident:memory:13");
        assertTrue(g.message.contains("Помнит помощь"));
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals("COMPLETED",g.progress.questOutcome("solitude_favor_lisette"));
        g.choose("SOLITUDE:favor:7:gift");assertEquals(gold,g.gold);
    }
    @Test public void rejectsRemoteLateAndMissingResources() throws Exception{
        GameEngine g=city(0);
        int gold=g.gold;
        g.choose("SOLITUDE:favor:0:accept");
        assertNull(g.progress.questOutcome("solitude_favor_sorex"));
        g.choose("SOLITUDE:favor:1:accept");
        g.choose("SOLITUDE:favor:1:deliver");
        assertEquals("ACTIVE",g.progress.questOutcome("solitude_favor_addvar"));
        assertNull(g.progress.questOutcome("solitude_favor_addvar_delivered"));
        g.progress.addItem("Соль",2);int salt=g.progress.quantity("Соль");
        g.hour=16;
        g.choose("SOLITUDE:favor:1:deliver");
        assertEquals(salt,g.progress.quantity("Соль"));
        assertNull(g.progress.questOutcome("solitude_favor_addvar_delivered"));
        g.hour=12;
        g.sceneState.confirm("npc","solitude_addvar","Аддвар","DEAD");
        g.choose("SOLITUDE:favor:1:deliver");
        assertEquals(salt,g.progress.quantity("Соль"));
        assertEquals(gold,g.gold);
        assertNull(g.progress.questOutcome("solitude_favor_addvar_delivered"));
    }
    @Test public void allEightRequestsHaveCatalogItemsAndMatchingResidents() throws Exception{
        for(int i=0;i<SolitudeFavors.count();i++){
            assertNotNull(ItemCatalog.find(SolitudeFavors.item(i)));
            int resident=SolitudeFavors.resident(i),district=SolitudeResidents.district(resident,12);
            assertTrue(district>=0);
            GameEngine g=city(district);
            assertTrue(g.progress.addItem(SolitudeFavors.item(i),SolitudeFavors.amount(i)));
            String action="SOLITUDE:favor:"+i+":";
            g.choose(action+"accept");g.choose(action+"deliver");
            assertTrue(g.message.contains("принимает припасы"));
            g.choose(action+"gift");
            assertEquals("HELPED",g.sceneState.npcStatusByName(SolitudeResidents.NAMES[resident]));
        }
    }
}
