package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItachiStoryTest {
 @Test public void persistenceAndDisabledCombat() throws Exception {
    ItachiStory s=new ItachiStory();s.stage=6;s.following=true;s.romance=true;s.trust=83;s.energy=45;s.ending="Новый дом";
    s=ItachiStory.restore(s.save());assertTrue(s.active());assertTrue(s.romance);assertEquals(83,s.trust);assertEquals(45,s.energy);
    s.enabled=false;assertFalse(s.active());assertEquals(0,s.assist(8,99));assertEquals(0,s.protect(30));assertEquals(45,s.energy);
 }
 @Test public void energyAndDamageBounds(){
    ItachiStory s=new ItachiStory();s.stage=6;s.following=true;
    assertEquals(3,s.assist(8,3));assertEquals(65,s.energy);assertTrue(s.lastPower.contains("Сусаноо"));
    assertEquals(0,s.assist(9,20));assertEquals(1,s.protect(2));assertEquals(60,s.energy);
    s.energy=0;assertTrue(s.assist(2,99)>0);assertEquals(0,s.energy);assertEquals(0,s.protect(10));
    s.rest(100);assertEquals(100,s.energy);
 }
 @Test public void invalidSaveCannotRecruitEarly() throws Exception {
    ItachiStory s=ItachiStory.restore(new org.json.JSONObject().put("stage",1).put("following",true).put("romance",true).put("energy",-50));
    assertFalse(s.active());assertFalse(s.romance);assertEquals(0,s.energy);
 }
}
