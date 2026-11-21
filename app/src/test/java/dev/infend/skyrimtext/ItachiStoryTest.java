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
    assertEquals(3,s.assist(8,3));assertEquals(65,s.energy);assertTrue(s.lastPower.contains("Сусаноо"));assertEquals(20,s.eyeStrain);assertEquals(3,s.shield);
    assertEquals(0,s.assist(9,20));assertEquals(2,s.protect(2));assertEquals(65,s.energy);s.beginBattle();
    s.energy=0;assertTrue(s.assist(2,99)>0);assertEquals(0,s.energy);assertEquals(0,s.protect(10));
    s.rest(100);assertEquals(100,s.energy);
 }
 @Test public void distinctEffectsAndProgress() throws Exception {
    ItachiStory s=new ItachiStory();s.stage=6;s.following=true;
    s.assist(6,99);assertEquals(3,s.burn);assertTrue(s.ongoingDamage(99)>0);assertEquals(2,s.burn);
    s.assist(4,99);assertEquals(1,s.stun);assertEquals(12,s.protect(12,true));assertEquals(0,s.stun);
    s.energy=100;s.assist(8,99);assertTrue(s.shield>0);s.nextTarget();assertEquals(0,s.burn);
    s.gain(100);assertEquals(2,s.level);s.love=80;s.romance=true;s.engaged=true;
    s=ItachiStory.restore(s.save());assertEquals(2,s.level);assertEquals(80,s.love);assertTrue(s.engaged);
 }
 @Test public void invalidSaveCannotRecruitEarly() throws Exception {
    ItachiStory s=ItachiStory.restore(new org.json.JSONObject().put("stage",1).put("following",true).put("romance",true).put("energy",-50));
    assertFalse(s.active());assertFalse(s.romance);assertEquals(0,s.energy);
 }
}
