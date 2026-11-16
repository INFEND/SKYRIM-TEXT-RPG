package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class AlmaStoryTest {
    private GameEngine start(){GameEngine g=new GameEngine();g.start("Герой","Норд","Маг");g.mode="WORLD";g.progress.location="Рифтен";return g;}
    @Test public void romanceAndSave() throws Exception{
        GameEngine g=start();g.progress.location="Ривервуд";g.choose("ALMA:open");assertEquals("WORLD",g.mode);
        g.progress.location="Рифтен";g.choose("ALMA:open");assertEquals("ALMA",g.mode);
        g.choose("ALMA:romance");assertFalse(g.alma.romance);
        g.choose("ALMA:good:0");g.choose("ALMA:good:0");assertEquals(1,g.alma.stage);
        for(int i=1;i<8;i++)g.choose("ALMA:good:"+i);
        g.choose("ALMA:romance");assertTrue(g.alma.romance);
        g.choose("ALMA:join");assertTrue(g.alma.companion);
        g.choose("ALMA:date");int love=g.alma.love;g.choose("ALMA:date");assertEquals(love,g.alma.love);
        g=GameEngine.fromJson(g.toJson().toString());assertEquals(8,g.alma.stage);assertTrue(g.alma.romance);assertEquals(love,g.alma.love);
        for(int i=8;i<11;i++)g.choose("ALMA:good:"+i);
        g.choose("ALMA:finish:eternal");assertEquals("Вечность вдвоём",g.alma.ending);assertEquals("COMPLETED",g.progress.questOutcome(AlmaStory.ID));
        g.choose("ALMA:finish:mortal");assertEquals("Вечность вдвоём",g.alma.ending);
    }
    @Test public void companionEquipmentAndCombatPersist() throws Exception {
        GameEngine g=start();g.choose("ALMA:open");for(int i=0;i<4;i++)g.choose("ALMA:good:"+i);
        g.choose("ALMA:join");g.gold=500;g.choose("ALMA:weapon");g.choose("ALMA:weapon");assertEquals(350,g.gold);
        g.choose("ALMA:armor");assertEquals(230,g.gold);g.choose("ALMA:gift");g.choose("ALMA:gift");assertEquals(200,g.gold);
        g.choose("ALMA:hunt");assertEquals("COMBAT",g.mode);assertEquals("ALMA",g.battleKind);
        int mana=g.alma.mana;assertTrue(g.alma.assist(g,2,30)>0);assertTrue(g.alma.mana<mana);
        g.alma.victory(g);assertTrue(g.alma.hunterDefeated);
        g=GameEngine.fromJson(g.toJson().toString());assertEquals(1,g.alma.weapon);assertEquals(1,g.alma.armor);assertTrue(g.alma.hunterDefeated);assertTrue(g.alma.memory.length()>0);
    }
    @Test public void optionalExpansionAndInventory() throws Exception {
        GameEngine g=start();g.choose("ALMA:open");for(int i=0;i<8;i++)g.choose("ALMA:good:"+i);
        g.choose("ALMA:join");g.choose("ALMA:romance");g.gold=100;g.choose("ALMA:supply");assertEquals(65,g.gold);assertEquals(3,g.alma.supplies);
        g.alma.health=10;g.choose("ALMA:heal");assertEquals(45,g.alma.health);assertEquals(2,g.alma.supplies);
        g.choose("ALMA:serana:0");g.choose("ALMA:serana:0");assertEquals(1,g.alma.seranaStep);
        assertTrue(g.alma.setEnabled(g,false));assertEquals("WORLD",g.mode);assertFalse(g.alma.available(g));assertEquals(0,g.alma.assist(g,2,100));
        g=GameEngine.fromJson(g.toJson().toString());assertFalse(g.alma.enabled);assertTrue(g.alma.romance);assertEquals(2,g.alma.supplies);assertTrue(g.alma.setEnabled(g,true));assertTrue(g.alma.available(g));
        g.choose("ALMA:open");g.progress.location="Винтерхолд";g.choose("ALMA:archive:0");g.choose("ALMA:archive:1");assertEquals(2,g.alma.archiveStep);
        g.progress.location="Рифтен";g.choose("ALMA:seal:0");g.choose("ALMA:seal:1");assertEquals(1,g.alma.talisman);
    }
    @Test public void inventoryWeddingAndIndependentLife() throws Exception{
        GameEngine g=start();g.choose("ALMA:open");for(int i=0;i<11;i++)g.choose("ALMA:good:"+i);
        g.choose("ALMA:join");g.choose("ALMA:romance");g.alma.love=90;g.alma.trust=90;
        g.choose("ALMA:engage");assertTrue(g.alma.engaged);g.choose("ALMA:marry");assertFalse(g.alma.married);g.day++;g.choose("ALMA:marry");assertTrue(g.alma.married);
        g.gold=500;g.choose("ALMA:home");assertTrue(g.alma.sharedHome);assertEquals(0,g.gold);
        int before=g.progress.quantity("Факел");assertTrue(g.alma.transfer(g,"Факел",true));assertEquals(before-1,g.progress.quantity("Факел"));assertTrue(g.alma.transfer(g,"Факел",false));assertEquals(before,g.progress.quantity("Факел"));
        g.choose("ALMA:finish:mortal");g.choose("ALMA:dismiss");g.day++;g.alma.independent(g);assertEquals(1,g.alma.daysIndependent);g.alma.independent(g);assertEquals(1,g.alma.daysIndependent);
        g=GameEngine.fromJson(g.toJson().toString());assertTrue(g.alma.married);assertTrue(g.alma.sharedHome);assertEquals(1,g.alma.daysIndependent);
    }
    @Test public void romanceNotRequiredAndLossIsFinal(){
        GameEngine g=start();g.choose("ALMA:open");for(int i=0;i<11;i++)g.choose("ALMA:cool:"+i);
        g.choose("ALMA:finish:eternal");assertEquals(11,g.alma.stage);
        g.choose("ALMA:finish:part");assertEquals(12,g.alma.stage);g.choose("ALMA:exit");g.choose("ALMA:open");assertEquals("WORLD",g.mode);
    }
}
