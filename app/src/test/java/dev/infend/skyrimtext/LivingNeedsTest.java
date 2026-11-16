package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class LivingNeedsTest {
 @Test public void timeAndPersistence(){LivingNeeds n=new LivingNeeds();n.sync(1,8);n.sync(1,18);assertEquals(80,n.food);assertEquals(70,n.water);assertEquals(70,n.awake);n.sync(1,18);assertEquals(80,n.food);n=LivingNeeds.fromJson(n.toJson());n.sync(2,0);assertEquals(68,n.food);n.eat();assertEquals(93,n.food);n.recover(8,true);assertEquals(100,n.awake);}
 @Test public void backwardsClockDoesNotRestore(){LivingNeeds n=new LivingNeeds();n.sync(2,1);n.sync(2,0);assertEquals(100,n.food);n.sync(2,2);assertEquals(98,n.food);}
}
