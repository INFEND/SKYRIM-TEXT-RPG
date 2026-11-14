package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FactionDialogueRuntimeTest {
 @Test public void everyChapterHasTwoSpeakersAndPerkDependencies() {
   FactionStories s=new FactionStories();int total=0;
   for(int f=0;f<7;f++){assertEquals(s.count(f),FactionDialogue.count(f));total+=s.count(f);for(int c=0;c<s.count(f);c++)assertTrue(FactionDialogue.scene(f,c).contains(">**"));}
   assertEquals(67,total);
   RpgProgress p=new RpgProgress();p.perkPoints=10;p.skillLevels[0]=100;
   assertTrue(p.buyCombatPerk(0));assertFalse(p.advanced.canBuy(p,AdvancedPerks.BLADESMAN));
   assertTrue(p.advanced.buy(p,AdvancedPerks.FIGHTING));assertTrue(p.advanced.buy(p,AdvancedPerks.BLADESMAN));assertFalse(p.advanced.buy(p,AdvancedPerks.BLADESMAN));
   assertTrue(RpgProgress.fromJson(p.toJson()).advanced.has(AdvancedPerks.BLADESMAN));
 }
}
