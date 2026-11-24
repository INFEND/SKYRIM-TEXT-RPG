package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlayerChanceTest {
    @Test public void ordinaryActionIsGuaranteedEvenWhenModelDisagrees(){
        GameEngine g=new GameEngine();
        assertEquals(-1,PlayerChance.calculate(g,5,12,"Пойти к Кэтрин и сесть рядом","Подойти к ней за стойкой и предложить разговор").chance);
    }
    @Test public void higherSkillImprovesDifficultCheck(){
        GameEngine g=new GameEngine();
        g.progress.skillLevels[12]=15;
        int low=PlayerChance.calculate(g,5,12,"Убедить стражника","Сложные переговоры").chance;
        g.progress.skillLevels[12]=100;
        PlayerChance.Result high=PlayerChance.calculate(g,5,12,"Убедить стражника","Сложные переговоры");
        assertTrue(high.chance>low);assertTrue(high.chance>=90);
        assertTrue(high.explanation.contains("ИТОГ: "+high.chance+"%"));
    }
}
