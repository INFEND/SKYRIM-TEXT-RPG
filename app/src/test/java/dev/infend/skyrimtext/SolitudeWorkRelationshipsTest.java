package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** A city shift changes the shared NPC ledger, not merely global hold reputation. */
public class SolitudeWorkRelationshipsTest {
    private GameEngine atMarket() {
        GameEngine g=new GameEngine();
        g.start("Тест","Норд","Воин");
        g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=10;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");
        return g;
    }
    private int relation(GameEngine g,String name) {
        JSONArray facts=g.sceneState.npcFacts();
        for(int i=0;i<facts.length();i++){
            JSONObject f=facts.optJSONObject(i);
            if(f!=null&&name.equals(f.optString("title")))return f.optInt("relation",50);
        }
        return -1;
    }
    @Test public void paidShiftPersistsAndCannotRepeat() throws Exception {
        GameEngine g=atMarket();
        g.choose("SOLITUDE:job:16");
        int first=relation(g,"Яла");
        assertTrue("Market supervisor must be remembered",first>=51);
        g.choose("SOLITUDE:job:16");
        assertEquals(first,relation(g,"Яла"));
        g=GameEngine.fromJson(g.toJson().toString());
        assertEquals(first,relation(g,"Яла"));
        g.choose("SOLITUDE:job:16");
        assertEquals(first,relation(g,"Яла"));
    }
    @Test public void voluntaryShiftHasLargerRelationshipEffectAndNoWages() throws Exception {
        GameEngine paid=atMarket(),volunteer=atMarket();
        int initialGold=volunteer.gold;
        paid.choose("SOLITUDE:job:16");
        volunteer.choose("SOLITUDE:volunteer:16");
        assertEquals(initialGold,volunteer.gold);
        assertEquals("Paid shift starts with MET (50) and adds one point",51,relation(paid,"Яла"));
        assertEquals("Volunteer help starts with HELPED (70) and adds three points",73,relation(volunteer,"Яла"));
        assertTrue("Voluntary help must produce a stronger relationship",relation(volunteer,"Яла")>relation(paid,"Яла"));
        int after=relation(volunteer,"Яла");
        volunteer.choose("SOLITUDE:volunteer:16");
        assertEquals(after,relation(volunteer,"Яла"));
        volunteer=GameEngine.fromJson(volunteer.toJson().toString());
        assertEquals("Volunteer relationship must survive save/load",after,relation(volunteer,"Яла"));
        volunteer.choose("SOLITUDE:volunteer:16");
        assertEquals("Reload must not permit repeated relationship credit",after,relation(volunteer,"Яла"));
    }
}
