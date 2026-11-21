package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeJordisGearTest {
    @Test public void transferSaveReturnAndCombat() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.fixShop.home.buy(4);g.progress.setQuestOutcome(SolitudeCourt.THANE,"COMPLETED");
        g.progress.quantity("Железный меч",2);g.progress.quantity("Железный щит",1);
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:home");g.choose("SOLITUDE:home:jordis:follow");
        int base=g.solitude.jordisDamage(g,2,100);
        g.choose("SOLITUDE:home:jordis:give:Железный меч");
        assertEquals(1,g.progress.quantity("Железный меч"));
        assertTrue(g.solitude.jordisDamage(g,2,100)>base);
        g.choose("SOLITUDE:home:jordis:give:Железный меч");assertEquals(1,g.progress.quantity("Железный меч"));
        g.choose("SOLITUDE:home:jordis:give:Железный щит");
        assertEquals(0,g.progress.quantity("Железный щит"));
        assertTrue(g.solitude.jordisProtection(g,20,true)>0);
        assertEquals(0,g.solitude.jordisProtection(g,20,false));
        g.choose("GO_WORLD");g.region=0;g.progress.location="Вайтран";
        g=GameEngine.fromJson(g.toJson().toString());
        assertTrue(g.solitude.jordisDamage(g,2,100)>base);
        g.mode="COMBAT";g.choose("JORDIS:take:0");assertEquals(1,g.progress.quantity("Железный меч"));
        g.mode="WORLD";g.progress.quantity("Железный слиток",100);
        g.choose("JORDIS:take:0");assertEquals(1,g.progress.quantity("Железный меч"));
        g.progress.quantity("Железный слиток",0);
        g.choose("JORDIS:take:0");assertEquals(2,g.progress.quantity("Железный меч"));
        assertEquals(base,g.solitude.jordisDamage(g,2,100));
        g.choose("JORDIS:take:0");assertEquals(2,g.progress.quantity("Железный меч"));
        g.choose("JORDIS:dismiss");assertEquals(0,g.solitude.jordisProtection(g,20,true));
    }
    @Test public void oldSaveAndEquippedItemAreSafe() {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        JordisGear gear=JordisGear.restore(null);
        assertEquals(0,gear.damageBonus());
        g.progress.quantity("Железный меч",1);assertTrue(g.progress.equip("Железный меч"));
        gear.action(g,"give:Железный меч");assertEquals(1,g.progress.quantity("Железный меч"));
        gear.action(g,"give:Клинок Хаафингара");assertEquals(0,gear.damageBonus());
    }
}
