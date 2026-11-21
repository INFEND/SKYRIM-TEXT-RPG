package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FactionDialogueRuntimeTest {
 @Test public void approachIsSavedAndCanBeChangedWithLowStamina() throws Exception {
   GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="WORLD";g.progress.location="Вайтран";
   assertTrue(g.factionStories.advance(g,0));int basic=g.factionStories.chance(g,0);
   assertTrue(g.factionStories.setApproach(g,0,1));assertEquals(12,g.factionStories.cost(0));assertEquals(Math.min(100,basic+12),g.factionStories.chance(g,0));
   g=GameEngine.fromJson(g.toJson().toString());assertEquals(1,g.factionStories.approach(0));g.stamina=8;
   assertTrue(g.factionStories.setApproach(g,0,2));assertEquals(8,g.factionStories.cost(0));assertEquals(12,g.factionStories.skill(0));
 }
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
