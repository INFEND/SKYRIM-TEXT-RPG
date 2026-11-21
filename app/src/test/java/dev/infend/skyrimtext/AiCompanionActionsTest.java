package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.*;

public class AiCompanionActionsTest {
    private GameEngine hero(String location){
        GameEngine g=new GameEngine();g.started=true;g.mode="WORLD";g.progress.location=location;g.setAiNarrativeMode(true);return g;
    }
    @Test public void encounterRequiresCorrectPlaceAndSelectedValidCode(){
        GameEngine g=hero("Вайтран");
        assertFalse(AiCompanionActions.allowed(g,"KATH:help"));
        assertEquals("",AiCompanionActions.execute(g,"KATH:help"));
        g.progress.location="Хижина Анис";
        assertTrue(AiCompanionActions.allowed(g,"KATH:help"));
        assertFalse(AiCompanionActions.execute(g,"KATH:help").isEmpty());
        assertEquals(1,g.katherine.stage);assertEquals("WORLD",g.mode);
        assertEquals("",AiCompanionActions.execute(g,"KATH:help"));
    }
    @Test public void noForcedRomanceOrDisabledNpc(){
        GameEngine g=hero("Рифтен");
        assertFalse(AiCompanionActions.allowed(g,"ALMA:romance"));
        g.alma.enabled=false;
        assertFalse(AiCompanionActions.allowed(g,"ALMA:good:0"));
    }
    @Test public void noUnselectedNativeEffectsAndProtectedFacts(){
        GameEngine g=hero("Ривервуд");
        assertTrue(AiCompanionActions.allowed(g,"ITACHI:help:0"));
        assertEquals(0,g.itachi.stage);
        assertTrue(AiCompanionActions.protectedNpc("itachi","Итачи"));
        assertTrue(AiCompanionActions.protectedNpc("livia","Ливия"));
        assertTrue(AiCompanionActions.protectedNpc("alma","Альма"));
        assertFalse(AiCompanionActions.protectedNpc("innkeeper","Хозяин таверны"));
    }
    @Test public void selectedActionIsSavedWithAiChoice() throws Exception {
        AiGameTurn.Option o=new AiGameTurn.Option("Поговорить","Обсудить прошлое","",72,"","KATH:talk");
        assertEquals("KATH:talk",o.companionAction);assertEquals(-1,o.chance);
        assertEquals("",AiCompanionActions.code("CANON_OPEN"));
        org.json.JSONArray choices=new org.json.JSONArray();
        for(int i=0;i<10;i++)choices.put(new org.json.JSONObject().put("title","Вариант "+i).put("detail","Описание выбора "+i).put("chance",org.json.JSONObject.NULL).put("companionAction",i==0?"KATH:talk":""));
        org.json.JSONObject saved=new org.json.JSONObject().put("scene","Сцена в хижине").put("choices",choices);
        AiGameTurn turn=AiGameTurn.restore(saved);
        assertNotNull(turn);
        assertEquals("KATH:talk",AiGameTurn.restore(turn.save()).choices.get(0).companionAction);
    }
}
