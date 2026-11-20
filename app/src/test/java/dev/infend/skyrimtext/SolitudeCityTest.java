package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.*;

public class SolitudeCityTest {
    private GameEngine city(){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=8;return g;
    }
    @Test public void cityMenuAndWorkSurviveReload() throws Exception {
        GameEngine g=city();
        assertTrue(g.scene().choices.stream().anyMatch(c->c.action.equals("SOLITUDE:open")));
        g.choose("SOLITUDE:open");
        for(int d=0;d<8;d++){final String action="SOLITUDE:visit:"+d;assertTrue(g.scene().choices.stream().anyMatch(c->c.action.equals(action)));}
        for(String action:new String[]{SolitudeCanonical.ADDVAR,SolitudeCanonical.CATACOMBS,SolitudeHome.MODE,"GO_WORLD"})assertTrue(g.scene().choices.stream().anyMatch(c->c.action.equals(action)));
        g.choose("SOLITUDE:visit:7");int gold=g.gold, stamina=g.stamina;
        g.choose("SOLITUDE:job:14");assertEquals(gold+30,g.gold);assertEquals(stamina-18,g.stamina);
        assertEquals(11,g.hour);assertEquals("Солитьюд",g.progress.location);
        g=GameEngine.fromJson(g.toJson().toString());assertEquals("SOLITUDE:7",g.mode);
        g.choose("SOLITUDE:job:14");assertEquals(gold+30,g.gold);
        g.day++;g.hour=8;g.choose("SOLITUDE:job:14");assertEquals(gold+60,g.gold);
    }
    @Test public void forgedOrUnavailableActionsDoNotReward(){
        GameEngine g=city();int gold=g.gold;
        g.choose("SOLITUDE:job:14");assertEquals(gold,g.gold);
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        g.choose("SOLITUDE:job:14");g.choose("SOLITUDE:job:00");assertEquals(gold,g.gold);
        g.hour=23;g.choose("SOLITUDE:job:0");assertEquals(gold,g.gold);
        g.hour=8;g.stamina=0;g.choose("SOLITUDE:job:0");assertEquals(gold,g.gold);
        g.choose("GO_WORLD");g.mode="COMBAT";g.choose("SOLITUDE:open");assertEquals("COMBAT",g.mode);
        g.mode="WORLD";g.progress.location="Морфал";g.choose("SOLITUDE:open");assertEquals("WORLD",g.mode);
    }
    @Test public void paidLessonAndExistingShop(){
        GameEngine g=city();g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:5");
        g.gold=0;g.choose("SOLITUDE:job:11");assertEquals(8,g.hour);
        g.gold=40;g.choose("SOLITUDE:job:11");assertEquals(22,g.gold);assertEquals(10,g.hour);
        g.choose("SOLITUDE:shop");assertEquals("SHOP",g.mode);
    }
    @Test public void completedShiftBuildsSharedResidentMemoryWithoutRepeatedCredit() throws Exception {
        GameEngine g=city();
        g.choose("SOLITUDE:open");
        g.choose("SOLITUDE:visit:0");
        g.choose("SOLITUDE:job:0");
        assertEquals("MET",g.sceneState.status("npc","solitude_jala"));
        g=GameEngine.fromJson(g.toJson().toString());
        int gold=g.gold;
        g.choose("SOLITUDE:job:0");
        assertEquals(gold,g.gold);
        assertEquals("MET",g.sceneState.status("npc","solitude_jala"));
        g.day++;
        g.hour=8;
        g.choose("SOLITUDE:job:0");
        assertEquals("INFORMED",g.sceneState.status("npc","solitude_jala"));
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals("INFORMED",g.sceneState.status("npc","solitude_jala"));
    }

    @Test public void dockShiftCreditsVittoriaEvenAfterHerClosingHour() throws Exception {
        GameEngine g=city();g.hour=16;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:7");
        int before=g.gold;
        g.choose("SOLITUDE:job:14");
        assertEquals(19,g.hour);
        assertEquals(before+30,g.gold);
        assertEquals("MET",g.sceneState.status("npc","solitude_vittoria"));
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals("MET",g.sceneState.status("npc","solitude_vittoria"));
        int paid=g.gold;
        g.choose("SOLITUDE:job:14");
        assertEquals(paid,g.gold);
    }
    @Test public void marketShiftCreditsActualSupervisorAtStart() throws Exception {
        GameEngine g=city();g.hour=16;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        g.choose("SOLITUDE:job:0");
        assertEquals(18,g.hour);
        assertEquals("MET",g.sceneState.status("npc","solitude_jala"));
        assertEquals("",g.sceneState.status("npc","solitude_evette"));
    }
}
