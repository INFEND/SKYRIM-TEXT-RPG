package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class ItachiLifeTest {
 @Test public void roundTripAndLegacySave() throws Exception {
    ItachiStory s=new ItachiStory();s.stage=6;s.romance=true;s.married=true;s.life.sharedHome=true;s.life.independentDays=8;s.life.studies=4;s.life.almaTrust=81;s.life.engagementDay=12;
    ItachiStory restored=ItachiStory.restore(s.save());assertTrue(restored.life.sharedHome);assertEquals(8,restored.life.independentDays);assertEquals(4,restored.life.studies);assertEquals(81,restored.life.almaTrust);assertEquals(12,restored.life.engagementDay);
    assertFalse(ItachiStory.restore(new org.json.JSONObject()).life.sharedHome);
 }
 @Test public void invalidHomeAndItemsRejected() throws Exception {
    ItachiStory s=new ItachiStory();org.json.JSONObject j=new org.json.JSONObject().put("sharedHome",true).put("pack",new org.json.JSONObject().put("Септимы",999)).put("almaTrust",999);
    ItachiLife l=ItachiLife.restore(j,s);assertFalse(l.sharedHome);assertEquals(0,l.pack.length());assertEquals(100,l.almaTrust);l.sharedHome=true;l.homeReturn=true;l.endRomance();assertFalse(l.sharedHome);assertFalse(l.homeReturn);
 }
}
