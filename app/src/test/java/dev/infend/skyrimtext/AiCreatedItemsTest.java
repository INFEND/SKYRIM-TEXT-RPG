package dev.infend.skyrimtext;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class AiCreatedItemsTest {
    private AiGameTurn turn(String name,int delta)throws Exception{
        JSONArray choices=new JSONArray();
        for(int i=0;i<10;i++)choices.put(new JSONObject().put("title","Действие "+i).put("detail","Продолжить осмотр "+i).put("skill","").put("chance",JSONObject.NULL));
        JSONObject root=new JSONObject().put("scene","Ты осматриваешь мастерскую и внимательно изучаешь вещи. ".repeat(8)).put("choices",choices)
            .put("effects",new JSONObject().put("inventory",new JSONArray().put(new JSONObject().put("name",name).put("delta",delta))))
            .put("journal","Изменение инвентаря").put("customChance",JSONObject.NULL);
        return AiGameTurn.parseModel(root.toString(),-2);
    }
    @Test public void missingDressIsCreatedAndRestored()throws Exception{
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="WORLD";
        String name="Шёлковое платье INFEND";
        turn(name,1).apply(g,true);
        assertEquals(1,g.progress.quantity(name));
        assertEquals("Одежда",ItemCatalog.find(name).category);
        GameEngine loaded=GameEngine.fromJson(g.toJson().toString());
        assertEquals(1,loaded.progress.quantity(name));assertNotNull(ItemCatalog.find(name));
        turn("Шелковое платье INFEND",1).apply(loaded,true);
        assertEquals(2,loaded.progress.quantity(name));
        turn(name,-1).apply(loaded,true);assertEquals(1,loaded.progress.quantity(name));
        ItemCatalog.restoreAiItems(null);
    }
    @Test public void missingItemCannotBeRemoved()throws Exception{
        GameEngine g=new GameEngine();g.start("Герой","Норд","Воин");g.mode="WORLD";
        try{turn("Несуществующая вещь INFEND",-1).apply(g,true);fail("Must not remove an absent item");}
        catch(JSONException expected){assertNull(ItemCatalog.find("Несуществующая вещь INFEND"));}
    }
}
