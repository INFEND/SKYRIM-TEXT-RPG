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
}
