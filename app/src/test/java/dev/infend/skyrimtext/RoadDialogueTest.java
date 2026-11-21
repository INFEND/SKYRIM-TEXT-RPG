package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoadDialogueTest {
    @Test public void namedDialoguePersistsAndDoesNotConsumeEncounter() throws Exception{
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="ROAD";g.road.id="maiq";g.road.destination="Ривервуд";String ask=null;
        assertTrue(g.scene().body.contains("М'Айк"));for(GameEngine.Choice c:g.scene().choices)if(c.action.endsWith(":ask"))ask=c.action;assertNotNull(ask);g.choose(ask);assertEquals("ROAD",g.mode);assertTrue(g.scene().body.contains("Ты:"));
        g=GameEngine.fromJson(g.toJson().toString());assertTrue(g.scene().body.contains("Ты:"));for(GameEngine.Choice c:g.scene().choices)assertFalse(c.action.endsWith(":ask"));g.choose(ask);assertEquals("ROAD",g.mode);
        for(GameEngine.Choice c:g.scene().choices)if(c.action.endsWith(":talk")){g.choose(c.action);break;}assertEquals("RESULT",g.mode);assertTrue(g.message.contains("М'Айк"));
    }
    @Test public void dialogueProfilesBelongToExistingEncounters(){assertEquals(RoadEvents.eventCount(),RoadDialogue.count());for(String id:RoadEvents.eventIds()){RoadDialogue.Profile p=RoadDialogue.find(id);if(p!=null){assertFalse(p.answer.isEmpty());assertFalse(p.question.isEmpty());}}}
    @Test public void wildlifeObservationIsNotSpeechAndDoesNotGrantSpeechExperience() throws Exception {GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="ROAD";g.road.id="wolves";g.road.destination="Ривервуд";int xp=g.progress.skillXp[12];String ask=null;for(GameEngine.Choice c:g.scene().choices)if(c.action.endsWith(":ask"))ask=c.action;assertNotNull(ask);g.choose(ask);assertEquals("ROAD",g.mode);assertEquals(xp,g.progress.skillXp[12]);assertTrue(g.scene().body.contains("Наблюдение:"));g=GameEngine.fromJson(g.toJson().toString());assertTrue(g.scene().body.contains("Наблюдение:"));}
}
