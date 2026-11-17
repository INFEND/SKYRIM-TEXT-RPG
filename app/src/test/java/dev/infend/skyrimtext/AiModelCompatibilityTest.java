package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.junit.Test;
import static org.junit.Assert.*;

public class AiModelCompatibilityTest {
    private JSONObject scene() throws Exception {
        StringBuilder story=new StringBuilder();
        for(int i=0;i<10;i++)story.append("Ты осматриваешь лагерь и замечаешь следы на земле. ");
        JSONArray choices=new JSONArray();
        for(int i=0;i<10;i++)choices.put(new JSONObject().put("title","Действие "+i)
            .put("detail","Подробное описание "+i).put("chance",i==0?"15%":"null"));
        return new JSONObject().put("scene",story.toString()).put("choices",choices)
            .put("effects",new JSONObject()).put("customChance","15");
    }
    @Test public void modelStringsAndNullAliases() throws Exception {
        AiGameTurn turn=AiGameTurn.parseModel(scene().toString(),15);
        assertEquals(15,turn.customChance);
        assertEquals(15,turn.choices.get(0).chance);
        assertEquals(-1,turn.choices.get(1).chance);
        assertEquals(42,AiGameTurn.parseChance("{\"customChance\":\"42%\"}"));
        assertEquals(-1,AiGameTurn.parseChance("{\"customChance\":\"без проверки\"}"));
    }
    @Test public void missingEchoDoesNotChangeLockedChance() throws Exception {
        JSONObject json=scene();
        json.remove("customChance");
        assertEquals(15,AiGameTurn.parseModel(json.toString(),15).customChance);
    }
    @Test(expected=JSONException.class) public void conflictingChanceRejected() throws Exception {
        AiGameTurn.parseModel(scene().toString(),5);
    }
    @Test(expected=JSONException.class) public void invalidChoiceChanceRejected() throws Exception {
        JSONObject json=scene();
        json.getJSONArray("choices").getJSONObject(0).put("chance","не знаю");
        AiGameTurn.parseModel(json.toString(),15);
    }
    @Test(expected=JSONException.class) public void duplicateChoicesRejected() throws Exception {
        JSONObject json=scene();
        json.getJSONArray("choices").getJSONObject(1).put("title","Действие 0!");
        AiGameTurn.parseModel(json.toString(),15);
    }
    @Test(expected=JSONException.class) public void malformedEffectsRejected() throws Exception {
        JSONObject json=scene();
        json.put("effects","goldDelta=500");
        AiGameTurn.parseModel(json.toString(),15);
    }
    @Test public void metadataIsNotStory() throws Exception {
        JSONObject json=scene();
        String original=json.getString("scene").trim();
        json.put("scene",original+" effects={\"goldDelta\":0}, customChance=15");
        assertEquals(original,AiGameTurn.parseModel(json.toString(),15).scene);
        assertEquals("Текст.",AiGameTurn.previewScene("{\"scene\":\"Текст. effects={"));
    }
}
