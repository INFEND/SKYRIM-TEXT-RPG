package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopWorldTest {
 @Test public void customContentIsSaveLocal() throws Exception {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.mode="WORLD";
  FixShopWorld w=g.fixShop.world;assertTrue(w.addCharacter("Эйра","Северная наёмница",1,true));assertFalse(w.addCharacter("Эйра","Повтор",0,true));
  assertTrue(w.recruit(0));assertEquals(12,w.companionDamage(2,100));assertTrue(w.talk(0,g.day));assertFalse(w.talk(0,g.day));
  assertTrue(w.buyMount(2));assertEquals(4,w.travelBonus());assertTrue(w.buyPet(1));assertEquals(5,w.petDamage(3,100));
  assertTrue(w.addPlace("Тайная пещера","Каменный зал","Вайтран"));assertFalse(w.visit(g,0));g.progress.location="Вайтран";assertTrue(w.visit(g,0));
  GameEngine h=GameEngine.fromJson(g.toJson().toString());assertEquals("Эйра",h.fixShop.world.companionName());assertEquals(4,h.fixShop.world.travelBonus());assertEquals(1,h.fixShop.world.places.size());assertTrue(new GameEngine().fixShop.world.characters.isEmpty());
 }
 @Test public void achievementCanOnlyBeClaimedOnce(){
  GameEngine g=new GameEngine();for(int i=0;i<5;i++)g.fixShop.record("Товар "+i,"",1,1);
  assertTrue(g.fixShop.claimCollection(g,5));assertFalse(g.fixShop.claimCollection(g,5));assertFalse(g.fixShop.claimCollection(g,15));
 }
}
