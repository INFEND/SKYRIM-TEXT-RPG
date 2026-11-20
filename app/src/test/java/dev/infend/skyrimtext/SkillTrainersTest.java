package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkillTrainersTest {
    @Test public void allEighteenSkillsHaveRealTrainers(){
        assertEquals(60,SkillTrainers.count());
        boolean[] have=new boolean[RpgContent.SKILLS.length];
        for(SkillTrainers.Trainer t:SkillTrainers.all()){
            assertNotNull(t.name,NpcProfiles.find(t.name));
            assertTrue(t.maximum==50||t.maximum==75||t.maximum==90);
            have[t.skill]=true;
        }
        for(int i=0;i<have.length;i++)assertTrue(RpgContent.SKILLS[i],have[i]);
    }
    @Test public void paidTrainingIsAtomicLimitedAndSaveSafe() throws Exception{
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="WORLD";g.gold=100000;
        SkillTrainers.Trainer t=SkillTrainers.find("Аркадия");
        assertNotNull(t);int before=g.progress.skillLevel(t.skill);
        int price=SkillTrainers.quote(g,t,5);assertTrue(price>0);
        assertTrue(SkillTrainers.train(g,t,5));
        assertEquals(before+5,g.progress.skillLevel(t.skill));assertEquals(100000-price,g.gold);
        assertFalse(SkillTrainers.train(g,t,1));
        GameEngine loaded=GameEngine.fromJson(g.toJson().toString());
        assertEquals(0,SkillTrainers.remaining(loaded,t));
        loaded.level++;assertEquals(5,SkillTrainers.remaining(loaded,t));
        int gold=loaded.gold,level=loaded.progress.skillLevel(t.skill);
        assertFalse(SkillTrainers.train(loaded,t,6));
        assertEquals(gold,loaded.gold);assertEquals(level,loaded.progress.skillLevel(t.skill));
    }
    @Test public void capAndInsufficientGoldAreNotBypassed(){
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="WORLD";
        SkillTrainers.Trainer t=SkillTrainers.find("Фендал");
        g.progress.skillLevels[t.skill]=50;g.gold=10000;
        assertEquals(0,SkillTrainers.remaining(g,t));assertFalse(SkillTrainers.train(g,t,1));
        g.progress.skillLevels[t.skill]=20;g.gold=0;
        assertFalse(SkillTrainers.train(g,t,1));assertEquals(20,g.progress.skillLevels[t.skill]);
    }
}
