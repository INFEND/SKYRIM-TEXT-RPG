package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class KatherineStoryTest {
    @Test public void encounterAndSave() throws Exception {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Маг");
        g.mode="WORLD";g.progress.location="Ривервуд";g.choose("KATH:open");
        assertEquals("WORLD",g.mode);
        g.progress.location="Хижина Анис";g.choose("KATH:open");
        assertEquals("KATHERINE",g.mode);
        g.choose("KATH:help");g.choose("KATH:help");
        assertEquals(1,g.katherine.stage);assertEquals(12,g.katherine.trust);
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals(1,g.katherine.stage);assertEquals("KATHERINE",g.mode);
        g.choose("KATH:listen");g.choose("KATH:study");g.choose("KATH:resolve");
        assertEquals(4,g.katherine.stage);assertEquals("COMPLETED",g.progress.questOutcome(KatherineStory.ID));
        g.choose("KATH:exit");assertEquals("WORLD",g.mode);
        g.katherine.enabled=false;g.choose("KATH:open");assertEquals("WORLD",g.mode);
    }
    @Test public void continuationAndCompanion() throws Exception {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Маг");g.mode="WORLD";g.progress.location="Хижина Анис";
        g.choose("KATH:open");g.choose("KATH:help");g.choose("KATH:listen");g.choose("KATH:study");g.choose("KATH:resolve");g.choose("KATH:recruit");
        assertTrue(g.katherine.active());assertEquals(5,g.katherine.stage);
        g.choose("KATH:college");assertEquals(5,g.katherine.stage);
        g.choose("KATH:exit");g.progress.location="Коллегия Винтерхолда";g.choose("KATH:open");g.choose("KATH:college");
        assertEquals(1,g.katherine.magicRank);g.choose("KATH:exit");g.progress.location="Солитьюд";g.choose("KATH:open");g.choose("KATH:palace");
        g.choose("KATH:exit");g.progress.location="Хижина Анис";g.choose("KATH:open");g.choose("KATH:final");
        assertEquals(8,g.katherine.stage);assertEquals(3,g.katherine.magicRank);assertEquals("COMPLETED",g.progress.questOutcome(KatherineStory.CONTINUATION));
        int trust=g.katherine.trust;g.choose("KATH:talk");g.choose("KATH:talk");assertEquals(Math.min(100,trust+5),g.katherine.trust);
        g.gold=100;g.choose("KATH:gift");g.choose("KATH:gift");assertEquals(75,g.gold);
        g=GameEngine.fromJson(g.toJson().toString());assertTrue(g.katherine.active());assertTrue(g.katherine.memory.length()>0);
        assertTrue(g.katherine.assist(g,1,100)>0);assertEquals(0,g.katherine.assist(g,2,100));assertTrue(g.katherine.protect(15)>0);
        g.choose("KATH:leave");assertFalse(g.katherine.active());assertEquals(0,g.katherine.assist(g,1,100));
    }
    @Test public void finaleManaAndAutonomy() throws Exception {
        GameEngine g=new GameEngine();g.start("Герой","Норд","Маг");g.mode=KatherineStory.MODE;g.progress.location="Хижина Анис";
        KatherineStory k=g.katherine;k.stage=8;k.following=true;k.trust=70;k.respect=60;k.magicRank=3;k.mana=100;
        g.choose("KATH:trail");assertEquals(9,k.stage);
        g.choose("KATH:negotiate");assertEquals(10,k.stage);
        g.choose("KATH:conclude");assertEquals(11,k.stage);assertEquals("COMPLETED",g.progress.questOutcome(KatherineStory.FINALE));
        int before=k.mana;assertTrue(k.assist(g,1,100)>0);assertEquals(before-8,k.mana);
        k.mana=0;assertEquals(0,k.assist(g,3,100));assertEquals(0,k.protect(20));k.rest(2);assertEquals(16,k.mana);
        k.autonomous(g);int count=k.memory.length();k.autonomous(g);assertEquals(count,k.memory.length());
        GameEngine restored=GameEngine.fromJson(g.toJson().toString());assertEquals(11,restored.katherine.stage);assertEquals(16,restored.katherine.mana);
    }
}
