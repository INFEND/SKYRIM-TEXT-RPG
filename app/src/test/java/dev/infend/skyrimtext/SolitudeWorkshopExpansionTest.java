package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeWorkshopExpansionTest {
    private GameEngine city(int d){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;g.stamina=100;g.gold=10000;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:"+d);return g;
    }
    @Test public void shieldRecipeConsumesMaterials(){
        GameEngine g=city(5);g.progress.quantity("Железный слиток",4);g.progress.quantity("Кожаные полоски",1);
        int before=g.progress.quantity("Железный щит");g.choose("SOLITUDE:craft:make:5");
        assertEquals(before+1,g.progress.quantity("Железный щит"));assertEquals(0,g.progress.quantity("Железный слиток"));assertEquals(0,g.progress.quantity("Кожаные полоски"));
    }
    @Test public void protectedItemsUseSharedTradePolicy(){
        GameEngine g=city(0);g.progress.quantity("Похищенное кольцо",1);g.progress.quantity("Похищенный амулет",1);
        assertFalse(g.canTradeItem(ItemCatalog.find("Похищенное кольцо")));assertFalse(g.canTradeItem(ItemCatalog.find("Похищенный амулет")));
        assertFalse(g.canTradeItem(ItemCatalog.find("Лечебное зелье")));
    }
    @Test public void upgradedSwordCannotBeBoughtAgain(){
        GameEngine g=city(5);g.swordUpgraded=true;g.progress.sync(g);g.choose("SOLITUDE:store:4");int gold=g.gold;
        g.choose("SOLITUDE:buy:4:4:max");assertEquals(gold,g.gold);assertEquals(1,g.progress.quantity("Стальной меч"));
    }
}
