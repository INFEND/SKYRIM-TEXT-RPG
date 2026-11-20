package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class AiEquipmentActionsTest {
    private AiGameTurn turn(JSONArray gear) throws Exception {
        JSONObject root=new JSONObject();
        root.put("scene",("В мастерской древних двемеров герой осмотрел магический сплав. "+
            "Ремесленник подтвердил, что предмет можно изготовить только из найденного материала. ").repeat(4));
        JSONArray options=new JSONArray();
        for(int i=0;i<10;i++)
            options.put(new JSONObject().put("title","Действие "+i)
                .put("detail","Продолжить осторожный осмотр "+i)
                .put("skill","").put("chance",JSONObject.NULL));
        root.put("choices",options);
        root.put("effects",new JSONObject().put("equipmentChanges",gear));
        root.put("journal","Найден новый образец снаряжения");
        root.put("customChance",JSONObject.NULL);
        return AiGameTurn.parseModel(root.toString(),-2);
    }
    private JSONObject equipment(String mode,String name,int type,int stat,double weight)
            throws Exception {
        return new JSONObject().put("mode",mode).put("name",name)
            .put("type",type).put("stat",stat).put("weight",weight)
            .put("description","Создано в ходе приключения");
    }
    @Test public void createdEquipmentCanBeSavedAndEdited() throws Exception {
        GameEngine g=new GameEngine();
        g.start("Герой","Норд","Воин");g.mode="WORLD";
        turn(new JSONArray().put(equipment("create","Клинок двемерского света",0,45,1.5))).apply(g,true);
        assertTrue(g.compensation.owned[3]);
        assertEquals("Клинок двемерского света",g.compensation.itemNames[3]);
        assertEquals(1,g.progress.quantity("fix_custom_3"));
        assertNotNull(ItemCatalog.find("fix_custom_3"));
        GameEngine loaded=GameEngine.fromJson(g.toJson().toString());
        assertEquals("Клинок двемерского света",loaded.compensation.itemNames[3]);
        JSONObject change=equipment("modify","Клинок улучшенного света",0,58,1.3);
        change.put("slot",3);
        turn(new JSONArray().put(change)).apply(loaded,true);
        assertEquals("Клинок улучшенного света",loaded.compensation.itemNames[3]);
        assertEquals(58,loaded.compensation.itemStat[3]);
        assertEquals(1,loaded.progress.quantity("fix_custom_3"));
    }
    @Test public void invalidBatchChangesNothing() throws Exception {
        GameEngine g=new GameEngine();
        g.start("Герой","Норд","Воин");g.mode="WORLD";
        String before=g.toJson().toString();
        JSONArray batch=new JSONArray();
        batch.put(equipment("create","Проверочный клинок",0,55,1));
        batch.put(equipment("create","Невозможный щит",1,50,90));
        try {
            turn(batch).apply(g,true);
            fail("Bad weight must be rejected before any inventory change");
        }catch(JSONException expected){}
        assertFalse(g.compensation.owned[3]);
        assertEquals(0,g.progress.quantity("fix_custom_3"));
        assertEquals(before,g.toJson().toString());
    }
}
