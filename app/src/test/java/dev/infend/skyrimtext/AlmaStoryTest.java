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
    @Test public void romanceNotRequiredAndLossIsFinal(){
        GameEngine g=start();g.choose("ALMA:open");for(int i=0;i<11;i++)g.choose("ALMA:cool:"+i);
        g.choose("ALMA:finish:eternal");assertEquals(11,g.alma.stage);
        g.choose("ALMA:finish:part");assertEquals(12,g.alma.stage);g.choose("ALMA:exit");g.choose("ALMA:open");assertEquals("WORLD",g.mode);
    }
}
