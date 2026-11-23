package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeWorkScheduleTest {
    @Test public void workRequiresLivingAvailableSupervisorAndFitsShift(){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=17;g.stamina=100;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        int gold=g.gold;g.choose("SOLITUDE:job:0");assertEquals(gold,g.gold);assertEquals(17,g.hour);
        g.hour=10;g.choose("SOLITUDE:job:0");assertEquals(gold+18,g.gold);assertEquals(12,g.hour);
        g.choose("SOLITUDE:job:0");assertEquals(gold+18,g.gold);
    }
    @Test public void unavailableNpcBlocksWork() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;g.stamina=100;
        g.sceneState.confirm("npc","solitude_jala","Яла","DEAD");
        // A scheduled local resident can legitimately replace Jala. Block both contacts.
        g.sceneState.confirm("npc","solitude_"+SolitudeResidents.IDS[8],SolitudeResidents.NAMES[8],"HOSTILE");
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        int gold=g.gold;g.choose("SOLITUDE:job:0");assertEquals(gold,g.gold);assertEquals(10,g.hour);
    }
}
