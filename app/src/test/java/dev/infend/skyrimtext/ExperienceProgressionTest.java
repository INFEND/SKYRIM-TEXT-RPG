package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class ExperienceProgressionTest {
    @Test public void levelThresholds(){
        assertEquals(100,GameEngine.experienceRequired(1));
        assertEquals(200,GameEngine.experienceRequired(2));
        assertEquals(1000,GameEngine.experienceRequired(10));
        assertEquals(9900,GameEngine.experienceRequired(99));
    }
    @Test public void carriesRemainderAcrossLevels(){
        GameEngine game=new GameEngine();
        game.addXp(99);
        assertEquals(1,game.level);
        assertEquals(99,game.xp);
        game.addXp(1);
        assertEquals(2,game.level);
        assertEquals(0,game.xp);
        game.addXp(250);
        assertEquals(3,game.level);
        assertEquals(50,game.xp);
    }
}
