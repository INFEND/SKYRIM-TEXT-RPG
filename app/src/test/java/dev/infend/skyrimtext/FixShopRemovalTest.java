package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
public class FixShopRemovalTest {
 @Test public void legacyPartyRemapsAndRefundsExactlyOnce() throws Exception{
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.fixSpent=450;g.fixShop.record("Эйра Снежная Клятва","",450,1);
  JSONObject save=g.toJson(),shop=save.getJSONObject("fixShop"),world=shop.getJSONObject("world");
  JSONArray chars=new JSONArray();chars.put(new JSONObject().put("name","Эйра Снежная Клятва").put("description","Старая").put("role",0).put("companion",true).put("home","Вайтран"));chars.put(new JSONObject().put("name","Мой спутник").put("description","Маг").put("role",1).put("companion",true).put("home","Вайтран"));
  world.put("characters",chars).put("active",0).put("companionSlots",2).put("extraActive",new JSONArray().put(1));
  GameEngine h=GameEngine.fromJson(save.toString());assertEquals(0,h.fixSpent);assertEquals(1,h.fixShop.world.characters.size());assertEquals("Мой спутник",h.fixShop.world.companionName());
  GameEngine k=GameEngine.fromJson(h.toJson().toString());assertEquals(0,k.fixSpent);assertEquals(1,k.fixShop.world.partySize());
 }
 @Test public void homesAndCosmeticsAreLocal() throws Exception{
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");assertTrue(g.fixShop.home.buy(0));assertTrue(g.fixShop.home.buyRoom(g,0));assertTrue(g.fixShop.cosmetics.buy(false,0));assertTrue(g.fixShop.cosmetics.buy(true,1));
  GameEngine h=GameEngine.fromJson(g.toJson().toString());assertTrue(h.fixShop.home.owns(0));assertTrue(h.fixShop.home.room(0));assertEquals("Дитя Севера",h.fixShop.cosmetics.title());assertEquals(1,h.fixShop.cosmetics.frame);assertFalse(new GameEngine().fixShop.home.owns(0));
 }
}
