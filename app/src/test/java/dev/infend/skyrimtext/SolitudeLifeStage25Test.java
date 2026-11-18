package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeLifeStage25Test {
    @Test public void extraGearSlotsPersistAndDoNotDuplicate() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        JordisGear gear=JordisGear.restore(null);
        g.progress.quantity("Железный шлем",1);g.progress.quantity("Железные перчатки",1);g.progress.quantity("Железные сапоги",1);
        gear.action(g,"give:Железный шлем");gear.action(g,"give:Железные перчатки");gear.action(g,"give:Железные сапоги");
        assertEquals(0,g.progress.quantity("Железный шлем"));
        assertEquals(0,g.progress.quantity("Железные перчатки"));
        assertEquals(0,g.progress.quantity("Железные сапоги"));
        assertTrue(gear.protection(20)>0);
        gear=JordisGear.restore(gear.save());
        assertTrue(gear.summary().contains("Железный шлем"));
        assertEquals(1,gear.protection(1));
        gear.action(g,"take:3");gear.action(g,"take:3");
        assertEquals(1,g.progress.quantity("Железный шлем"));
        gear.action(g,"take:4");gear.action(g,"take:5");
        assertEquals(1,g.progress.quantity("Железные перчатки"));
        assertEquals(1,g.progress.quantity("Железные сапоги"));
        assertEquals(0,gear.protection(20));
        org.json.JSONObject old=new org.json.JSONObject().put("weapon","Железный меч");
        assertTrue(JordisGear.restore(old).damageBonus()>0);
    }
    @Test public void npcTopicsRespectDistrictHoursAndRememberQuest() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        g.choose("SOLITUDE:local:1");assertEquals("",g.sceneState.npcStatusByName("Генерал Туллий"));
        g.choose("SOLITUDE:local:0");assertEquals("MET",g.sceneState.npcStatusByName("Яла"));
        g.choose("SOLITUDE:talk:0");assertTrue(g.message.contains("Снова встретились"));
        g.choose("SOLITUDE:memory:0");assertTrue(g.message.contains("50/100"));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:7");
        g.choose("SOLITUDE:quest:0:accept");
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:quest:0:work");
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:7");g.choose("SOLITUDE:quest:0:gift");
        int gold=g.gold,rep=g.holdReputation[3];
        g=GameEngine.fromJson(g.toJson().toString());
        g.choose("SOLITUDE:memory:7");assertTrue(g.message.contains("Пропавшая накладная"));assertTrue(g.message.contains("отказался от платы"));
        g.choose("SOLITUDE:talk:7");assertTrue(g.message.contains("Спасибо за прежнюю помощь"));
        assertEquals(gold,g.gold);assertEquals(rep,g.holdReputation[3]);
        g.hour=23;String before=g.message;g.choose("SOLITUDE:local:7");assertEquals(before,g.message);
    }
}
