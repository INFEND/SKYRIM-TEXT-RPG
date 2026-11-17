package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class LivingNeedsTest {
 @Test public void timeAndPersistence(){LivingNeeds n=new LivingNeeds();n.sync(1,8);n.sync(1,18);assertEquals(80,n.food);assertEquals(70,n.water);assertEquals(70,n.awake);n.sync(1,18);assertEquals(80,n.food);n=LivingNeeds.fromJson(n.toJson());n.sync(2,0);assertEquals(68,n.food);n.eat();assertEquals(93,n.food);n.recover(8,true);assertEquals(100,n.awake);}
 @Test public void backwardsClockDoesNotRestore(){LivingNeeds n=new LivingNeeds();n.sync(2,1);n.sync(2,0);assertEquals(100,n.food);n.sync(2,2);assertEquals(98,n.food);}
 @Test public void independentSwitchesFreezeAndRemovePenalties(){LivingNeeds n=new LivingNeeds();n.sync(1,8);n.food=n.water=n.awake=n.rest=0;assertEquals(26,n.penalty());n.setEnabledMask(1<<LivingNeeds.WATER,1,8);assertEquals(8,n.penalty());assertFalse(n.penaltyDetails().contains("Голод"));n.food=60;n.eat();n.tavernRest();assertEquals(60,n.food);n.sync(1,18);assertEquals(60,n.food);assertEquals(70,n.water);n.setEnabledMask(0,1,18);n.sync(3,18);assertEquals(0,n.penalty());assertEquals("",n.penaltyDetails());assertFalse(n.anyEnabled());n=LivingNeeds.fromJson(n.toJson());assertEquals(0,n.enabledMask());n.setEnabledMask(LivingNeeds.ALL,3,18);n.sync(3,19);assertEquals(58,n.food);assertEquals(67,n.water);}
 @Test public void oldSavesAndRecovery(){LivingNeeds n=LivingNeeds.fromJson(new org.json.JSONObject().put("food",10));assertEquals(LivingNeeds.ALL,n.enabledMask());n.setEnabledMask(1<<LivingNeeds.REST,1,8);n.awake=10;n.rest=10;n.recover(2,true);assertEquals(10,n.awake);assertEquals(30,n.rest);assertTrue(n.aiContext().contains("отключено"));}
}
