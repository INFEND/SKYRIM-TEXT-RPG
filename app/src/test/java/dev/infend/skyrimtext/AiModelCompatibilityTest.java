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
    @Test public void legacySaveSurvivesStricterModelValidation() throws Exception {
        JSONObject saved=AiGameTurn.parseModel(scene().toString(),15).save();
        saved.getJSONArray("choices").getJSONObject(1).put("title","Действие 0!");
        saved.put("scene","Ты замечаешь следы. effects={\"goldDelta\":0}");
        AiGameTurn restored=AiGameTurn.restore(saved);
        assertNotNull(restored);
        assertEquals("Ты замечаешь следы.",restored.scene);
        assertNotNull(AiGameTurn.restore(restored.save()));
        assertEquals(10,restored.choices.size());
    }
    @Test public void metadataIsNotStory() throws Exception {
        JSONObject json=scene();
        String original=json.getString("scene").trim();
        json.put("scene",original+" effects={\"goldDelta\":0}, customChance=15");
        assertEquals(original,AiGameTurn.parseModel(json.toString(),15).scene);
        assertEquals("Текст.",AiGameTurn.previewScene("{\"scene\":\"Текст. effects={"));
    }

    private GameEngine effectGame(){
        GameEngine g=new GameEngine();
        g.start("Герой","Норд","Маг");
        g.enemyHp=0;g.mode="WORLD";g.progress.location="Ривервуд";
        g.hp=50;g.maxHp=100;g.gold=100;g.hour=10;
        return g;
    }
    @Test public void quotedEffectsAreAppliedExactlyOnce() throws Exception {
        GameEngine g=effectGame();
        JSONObject effects=new JSONObject().put("hpDelta","+10").put("goldDelta","-5")
            .put("hourDelta","8.0").put("staminaDelta",JSONObject.NULL)
            .put("magickaDelta","без изменений").put("location","null")
            .put("inventory",JSONObject.NULL).put("sceneFacts","none")
            .put("skills",new JSONArray().put(new JSONObject().put("index","0").put("xp","0")));
        AiGameTurn.parseLocalModel(scene().put("effects",effects).toString(),15).apply(g,true);
        assertEquals(60,g.hp);assertEquals(95,g.gold);assertEquals(18,g.hour);
        assertEquals("Ривервуд",g.progress.location);
    }
    @Test public void allScalarDeltaFieldsAcceptExactZeroAndNull() throws Exception {
        String[] fields={"hpDelta","maxHpDelta","staminaDelta","maxStaminaDelta",
            "magickaDelta","maxMagickaDelta","goldDelta","xpDelta","hourDelta","dayDelta",
            "reputationDelta","attackDelta","armorDelta","potionsDelta","manaPotionsDelta",
            "questStageDelta","dragonStageDelta"};
        JSONObject e=new JSONObject();
        for(int i=0;i<fields.length;i++)e.put(fields[i],i%2==0?"0.0":JSONObject.NULL);
        GameEngine g=effectGame();
        AiGameTurn.parseModel(scene().put("effects",e).toString(),15).apply(g,true);
        assertEquals(50,g.hp);assertEquals(100,g.gold);assertEquals(10,g.hour);
    }
    @Test public void quotedEnemyStatsAreNotRoundedOrLost() throws Exception {
        JSONObject e=new JSONObject().put("enemy",new JSONObject().put("name","Волк")
            .put("hp","15").put("maxHp","20.0").put("damage","+3"));
        GameEngine g=effectGame();
        AiGameTurn.parseLocalModel(scene().put("effects",e).toString(),15).apply(g,true);
        assertEquals(15,g.enemyHp);assertEquals(20,g.enemyMaxHp);assertEquals(3,g.enemyDamage);
    }
    @Test public void invalidEffectsRemainRejectedWithoutMutation() throws Exception {
        for(Object value:new Object[]{"5 HP","5.5","99999999999999999999999999",true,"не знаю","501"}){
            GameEngine g=effectGame();
            JSONObject e=new JSONObject().put("hpDelta",value).put("goldDelta","10");
            try{
                AiGameTurn.parseLocalModel(scene().put("effects",e).toString(),15).apply(g,true);
                fail("Accepted invalid effect: "+value);
            }catch(JSONException expected){}
            assertEquals(50,g.hp);assertEquals(100,g.gold);
        }
    }
    @Test public void invalidRequiredNestedNumbersAreNotDefaulted() throws Exception {
        JSONObject enemy=new JSONObject().put("name","Волк").put("hp",JSONObject.NULL);
        JSONObject skill=new JSONObject().put("index","0.5").put("xp","10");
        for(JSONObject e:new JSONObject[]{new JSONObject().put("enemy",enemy),
                new JSONObject().put("skills",new JSONArray().put(skill))}){
            try{AiGameTurn.parseLocalModel(scene().put("effects",e).toString(),15);fail();}
            catch(JSONException expected){}
        }
    }
    @Test public void exactDecimalChanceIsAcceptedButNotFractionalChance() throws Exception {
        assertEquals(42,AiGameTurn.parseChance("{\"customChance\":\"+42.0%\"}"));
        try{AiGameTurn.parseChance("{\"customChance\":\"42.5%\"}");fail();}
        catch(JSONException expected){}
    }
}
