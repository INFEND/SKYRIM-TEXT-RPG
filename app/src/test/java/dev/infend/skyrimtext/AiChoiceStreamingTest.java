package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.*;

public class AiChoiceStreamingTest {
    @Test public void titleAndDetailGrowBeforeJsonCompletes(){
        String start="{\"scene\":\"В Ривервуде тихо.\",\"choices\":[{\"title\":\"";
        assertTrue(AiGameTurn.previewChoices(start).isEmpty());
        assertEquals("П",AiGameTurn.previewChoices(start+"П").get(0).title);
        String detail=start+"Поговорить\",\"detail\":\"Уз";
        assertEquals("Поговорить",AiGameTurn.previewChoices(detail).get(0).title);
        assertEquals("Уз",AiGameTurn.previewChoices(detail).get(0).detail);
        String first=detail+"нать новости\",\"skill\":\"Речь\",\"chance\":45}";
        assertEquals(45,AiGameTurn.previewChoices(first).get(0).chance);
        String second=first+",{\"title\":\"В";
        assertEquals(2,AiGameTurn.previewChoices(second).size());
        assertEquals("В",AiGameTurn.previewChoices(second).get(1).title);
    }
    @Test public void ignoresNestedChoicesAndUnfinishedThinking(){
        assertTrue(AiGameTurn.previewChoices("{\"effects\":{\"choices\":[{\"title\":\"Ложь\"}]}}").isEmpty());
        assertTrue(AiGameTurn.previewChoices("<think>{\"choices\":[{\"title\":\"Ложь\"}").isEmpty());
        assertEquals("Да",AiGameTurn.previewChoices("<think>мысль</think>{\"choices\":[{\"title\":\"Да").get(0).title);
    }
    @Test public void escapesAndBracesInsideStringsDoNotSplitCards(){
        String first="{\"scene\":\"Скобка }\",\"choices\":[{\"title\":\"Спросить\",\"detail\":\"Он сказал: \\\"да\\\" {здесь}\",\"chance\":null}";
        assertEquals("Он сказал: \"да\" {здесь}",AiGameTurn.previewChoices(first).get(0).detail);
        assertEquals(2,AiGameTurn.previewChoices(first+",{\"title\":\"Осмотреть").size());
        assertEquals("А",AiGameTurn.previewChoices("{\"choices\":[{\"title\":\"А\\u04").get(0).title);
        assertEquals("Аб",AiGameTurn.previewChoices("{\"choices\":[{\"title\":\"А\\u0431").get(0).title);
    }
    @Test public void finalParserStillRejectsIncompleteTurn(){
        try{
            AiGameTurn.parse("{\"scene\":\"Текст\",\"choices\":[{\"title\":\"Осмотреть");
            fail("Incomplete preview must never be committed");
        }catch(org.json.JSONException expected){}
    }
    @Test public void localContextPrioritizesLocationAndRecentMemory(){
        GameEngine game=new GameEngine();
        game.progress.location="Ривервуд";
        String prompt=AiStoryClient.localUserPrompt(game,"СТАРЫЙ_МАРКЕР "+"x".repeat(1000)+" ПОСЛЕДНИЙ_МАРКЕР","Действие: поговорить с кузнецом",true);
        assertTrue(prompt.contains("ТЕКУЩЕЕ МЕСТО: Ривервуд"));
        assertTrue(prompt.contains("ПОСЛЕДНИЙ_МАРКЕР"));
        assertFalse(prompt.contains("СТАРЫЙ_МАРКЕР"));
        assertTrue(prompt.endsWith("Действие: поговорить с кузнецом"));
        assertTrue(prompt.length()<=3200);
    }
    @Test public void longActionKeepsLockedOutcomeAtEnd(){
        GameEngine game=new GameEngine();
        String command="НАЧАЛО "+"x".repeat(2200)+" БРОСОК: НЕУДАЧА";
        String prompt=AiStoryClient.localUserPrompt(game,"",command,true);
        assertTrue(prompt.contains("НАЧАЛО"));
        assertTrue(prompt.endsWith("БРОСОК: НЕУДАЧА"));
        assertTrue(prompt.length()<=3200);
    }
}
