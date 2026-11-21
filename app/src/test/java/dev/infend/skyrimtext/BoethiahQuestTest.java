package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class BoethiahQuestTest {
    private GameEngine hero(){GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="WORLD";g.hp=g.maxHp=500;g.gold=600;return g;}
    private void at(GameEngine g,String place){g.progress.location=place;g.mode="WORLD";}
    private void step(GameEngine g,String id){g.choose("ROADSTART:"+id);g.choose("ROADQUEST:"+id);}
    private void shrine(GameEngine g){at(g,"Святилище Боэтии");step(g,"boethiah_trial");assertTrue(RoadQuests.done(g,"boethiah_trial"));}
    @Test public void refusalBlocksHiringAndReward(){GameEngine g=hero();shrine(g);step(g,"boethiah_refuse");at(g,"Виндхельм");g.choose("ROADSTART:boethiah_hire");assertNull(g.progress.questOutcome("lead_boethiah_hire"));assertEquals(0,g.progress.quantity("Эбонитовая кольчуга"));}
    @Test public void paidHireAndReleasePersist() throws Exception {GameEngine g=hero();shrine(g);at(g,"Виндхельм");int gold=g.gold;step(g,"boethiah_hire");assertEquals(gold-500,g.gold);assertEquals("ACTIVE",g.progress.questOutcome("road_boethiah_hireling"));g=GameEngine.fromJson(g.toJson().toString());at(g,"Святилище Боэтии");step(g,"boethiah_release");assertEquals("COMPLETED",g.progress.questOutcome("road_boethiah_hireling"));g.choose("ROADSTART:boethiah_sacrifice");assertNull(g.progress.questOutcome("lead_boethiah_sacrifice"));assertNull(g.progress.questOutcome("road_boethiah_victim"));}
    @Test public void sacrificeUnlocksCultOnlyOnce(){GameEngine g=hero();shrine(g);at(g,"Виндхельм");step(g,"boethiah_hire");at(g,"Святилище Боэтии");step(g,"boethiah_sacrifice");assertEquals("COMPLETED",g.progress.questOutcome("road_boethiah_victim"));assertEquals("ACTIVE",g.progress.questOutcome("lead_boethiah_cult"));g.choose("ROADSTART:boethiah_release");assertNull(g.progress.questOutcome("lead_boethiah_release"));}
    @Test public void mailMustBeWorn(){GameEngine g=hero();at(g,"Хребет Острие Ножа");RoadQuests.offer(g,"boethiah_don");assertTrue(g.progress.addItem("Эбонитовая кольчуга",1));g.choose("ROADQUEST:boethiah_don");assertFalse(RoadQuests.done(g,"boethiah_don"));assertTrue(g.progress.equip("Эбонитовая кольчуга"));g.choose("ROADQUEST:boethiah_don");assertTrue(RoadQuests.done(g,"boethiah_don"));}
    @Test public void bladeAbsorbsHealthOnHit(){GameEngine g=hero();assertTrue(g.progress.addItem("Клинок Горя",1));assertTrue(g.progress.equip("Клинок Горя"));g.hp=300;g.beginRoadBattle("Бандит",500,1,"");g.choose("ATTACK");assertEquals(10,g.lastHealing);assertTrue(g.lastDealt>=10);}
    @Test public void undeadAndElfClassification(){assertTrue(RoadArtifacts.undead("Драугр-повелитель"));assertTrue(RoadArtifacts.elf("Талморский юстициар"));assertFalse(RoadArtifacts.undead("Волк"));assertFalse(RoadArtifacts.elf("Медведь"));}
}
