package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class LivingSafetyTest {
 @Test public void lockedRollsSurviveRetry(){LockedChecks checks=new LockedChecks();checks.add(1,"Тест",0,1);checks.add(2,"Успех",100,100);String original=checks.toString();LockedChecks restored=LockedChecks.restore(original);assertNotNull(restored);assertEquals(original,restored.toString());assertTrue(restored.hasSuccess());assertNull(LockedChecks.restore("[{\"index\":1,\"title\":\"X\",\"chance\":40,\"roll\":0}]"));}
 @Test public void progressionNeverOverflows(){GameEngine g=new GameEngine();g.level=99;g.xp=9899;g.maxHp=Integer.MAX_VALUE-1;g.maxMagicka=Integer.MAX_VALUE-1;g.maxStamina=Integer.MAX_VALUE-1;g.addXp(1);assertEquals(100,g.level);assertEquals(Integer.MAX_VALUE,g.maxHp);g.addXp(Integer.MAX_VALUE);g.addXp(100);assertEquals(Integer.MAX_VALUE,g.xp);}
 @Test public void skillPurchaseAndPersistence() throws Exception {GameEngine g=new GameEngine();g.started=true;g.level=1;g.xp=0;g.progress.skillLevels[0]=1;g.progress.skillXp[0]=0;assertTrue(g.progress.buyFixSkill(g,0,false));assertEquals(2,g.level);assertEquals(2,g.progress.skillLevel(0));assertEquals(1,g.fixSpent);GameEngine loaded=GameEngine.fromJson(g.toJson().toString());assertEquals(1,loaded.fixSpent);assertEquals(2,loaded.progress.skillLevel(0));}
 @Test public void noDuplicateOrOverflow(){GameEngine g=new GameEngine();assertFalse(g.compensation.createPower(CompensationPowers.NAMES[0],0,40,20));assertFalse(g.compensation.createItem(g,CompensationPowers.ITEMS[0],0,40,1,""));assertEquals(Integer.MAX_VALUE,GameEngine.nonnegativeAdd(Integer.MAX_VALUE-1,100));}
}
