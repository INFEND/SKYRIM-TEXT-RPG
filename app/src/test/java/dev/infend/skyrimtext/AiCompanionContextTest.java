package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;

public class AiCompanionContextTest {
    @Test public void disabledCharactersRemainExplicitInLocalContext(){
        GameEngine g=new GameEngine();g.started=true;g.mode="WORLD";
        g.katherine.enabled=false;g.alma.enabled=false;g.itachi.enabled=false;g.livia.enabled=false;
        String roster=AiCompanionActions.compactProfiles(g);
        for(String name:new String[]{"Кэтрин","Альма","Итачи","Ливия"})assertTrue(roster.contains(name+": отключена"));
        assertTrue(roster.length()<310);
        String prompt=AiStoryClient.localUserPrompt(g,"","Продолжить",true);
        assertTrue(prompt.contains(roster));
    }
    @Test public void enabledRosterFitsAndIncludesAllNames(){
        GameEngine g=new GameEngine();g.started=true;g.mode="WORLD";g.progress.location="Вайтран";
        String roster=AiCompanionActions.compactProfiles(g);
        for(String name:new String[]{"Кэтрин","Альма","Итачи","Ливия"})assertTrue(roster.contains(name+":"));
        assertTrue(roster.length()<310);
    }
}
