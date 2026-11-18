package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixShopAiContextTest {
 @Test public void currentSaveFactsAndCombatStatus() throws Exception {
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
  assertTrue(FixShopAiContext.context(g,true).contains("не куплены"));
  g.fixShop.gifts.buy(FixShopGifts.TIME);g.fixShop.seals.buy(FixShopSeals.RIPOSTE);
  g.mode="COMBAT";g.enemyHp=30;
  assertTrue(FixShopAiContext.combatAvailable(g,"FIXGIFT:time"));
  g.fixShop.gifts.useBattle(FixShopGifts.TIME);
  assertFalse(FixShopAiContext.combatAvailable(g,"FIXGIFT:time"));
  assertTrue(FixShopAiContext.context(g,false).contains("исчерпан в бою"));
  g.fixShop.seals.toggle(FixShopSeals.RIPOSTE);assertTrue(FixShopAiContext.context(g,false).contains("выкл"));
  GameEngine next=GameEngine.fromJson(g.toJson().toString());assertTrue(FixShopAiContext.context(next,false).contains("Пески времени"));
 }
 @Test public void retryIsStagedAndConsumedOnce(){
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.fixShop.gifts.buy(FixShopGifts.FATE);
  assertTrue(FixShopAiContext.mayRetry(g,50,90,false));assertFalse(FixShopAiContext.mayRetry(g,50,90,true));
  assertFalse(FixShopAiContext.mayRetry(g,0,90,false));assertTrue(FixShopAiContext.mayRetry(g,50,90,false));
  FixShopAiContext.commitRetry(g,g.day);assertFalse(FixShopAiContext.mayRetry(g,50,90,false));g.day++;assertTrue(FixShopAiContext.mayRetry(g,50,90,false));
 }
 @Test public void allPowersFitLocalPrompt(){
  GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");
  for(int i=0;i<FixShopGifts.NAME.length;i++)g.fixShop.gifts.buy(i);
  for(int i=0;i<FixShopSeals.NAME.length;i++)g.fixShop.seals.buy(i);
  String prompt=AiStoryClient.localUserPrompt(g,"","Проверка действия",true);
  assertTrue(prompt.length()<=3200);for(String n:FixShopGifts.NAME)assertTrue(n,prompt.contains(n));for(String n:FixShopSeals.NAME)assertTrue(n,prompt.contains(n));
  assertTrue(prompt.contains("Проверка действия"));
 }
}
