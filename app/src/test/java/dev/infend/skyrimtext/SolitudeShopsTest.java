package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeShopsTest {
    private GameEngine city(){GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;g.gold=10000;return g;}
    private void open(GameEngine g,int n){g.choose("GO_WORLD");g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:"+SolitudeShops.district(n));g.choose("SOLITUDE:store:"+n);}
    @Test public void stockAndBudgetPersistAndRestock() throws Exception {
        GameEngine g=city();open(g,2);int before=g.gold,count=g.progress.quantity("Отмычка"),price=SolitudeShops.price(g,ItemCatalog.find("Отмычка"));
        for(int i=0;i<9;i++)g.choose("SOLITUDE:buy:2:0");
        assertEquals(count+8,g.progress.quantity("Отмычка"));assertEquals(before-8*price,g.gold);
        g=GameEngine.fromJson(g.toJson().toString());assertEquals("SOLITUDE:store:2",g.mode);
        g.choose("SOLITUDE:buy:2:0");assertEquals(count+8,g.progress.quantity("Отмычка"));
        g.day+=2;g.choose("SOLITUDE:buy:2:0");assertEquals(count+9,g.progress.quantity("Отмычка"));
    }
    @Test public void staleClosedAndWrongShopActionsDoNotTrade(){
        GameEngine g=city();open(g,0);int gold=g.gold;
        g.choose("SOLITUDE:buy:2:0");assertEquals(gold,g.gold);
        g.hour=23;g.choose("SOLITUDE:buy:0:0");assertEquals(gold,g.gold);
        g.hour=9;g.gold=0;g.choose("SOLITUDE:buy:0:0");assertEquals(0,g.progress.quantity("Шляпа путника"));
    }
    @Test public void buyingSellingAndSeparateInventories(){
        GameEngine g=city();open(g,0);int gold=g.gold;g.choose("SOLITUDE:buy:0:0");assertEquals(1,g.progress.quantity("Шляпа путника"));assertTrue(g.gold<gold);
        int paid=g.gold;g.choose("SOLITUDE:sell:0:0");assertEquals(0,g.progress.quantity("Шляпа путника"));assertTrue(g.gold>paid);assertTrue(g.gold<gold);
        open(g,1);assertTrue(g.scene().choices.stream().anyMatch(c->c.title.contains("горноцвет")));
    }
}
