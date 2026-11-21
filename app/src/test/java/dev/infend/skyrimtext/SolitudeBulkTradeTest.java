package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeBulkTradeTest {
    private GameEngine city(int store){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;g.gold=10000;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:"+SolitudeShops.district(store));g.choose("SOLITUDE:store:"+store);return g;
    }
    @Test public void bulkStockAndMoneyPersist() throws Exception {
        GameEngine g=city(2);g.progress.quantity("Отмычка",0);
        int gold=g.gold,p=SolitudeShops.price(g,ItemCatalog.find("Отмычка"));
        g.choose("SOLITUDE:buy:2:0:5");assertEquals(5,g.progress.quantity("Отмычка"));
        g.choose("SOLITUDE:buy:2:0:max");assertEquals(8,g.progress.quantity("Отмычка"));assertEquals(gold-8*p,g.gold);
        g=GameEngine.fromJson(g.toJson().toString());g.choose("SOLITUDE:buy:2:0:max");assertEquals(8,g.progress.quantity("Отмычка"));
        int sale=g.salePrice(ItemCatalog.find("Отмычка"));g.choose("SOLITUDE:sell:2:0:max");
        assertEquals(0,g.progress.quantity("Отмычка"));assertEquals(gold-8*p+8*sale,g.gold);
        g.choose("SOLITUDE:sell:2:0:max");assertEquals(gold-8*p+8*sale,g.gold);
    }
    @Test public void revalidatesWeightGoldHoursAndShop() {
        GameEngine g=city(2);g.progress.quantity("Отмычка",0);int p=SolitudeShops.price(g,ItemCatalog.find("Отмычка"));
        g.gold=p*2;g.choose("SOLITUDE:buy:2:0:5");assertEquals(0,g.progress.quantity("Отмычка"));
        g.choose("SOLITUDE:buy:2:0:max");assertEquals(2,g.progress.quantity("Отмычка"));assertEquals(0,g.gold);
        g.gold=10000;g.hour=23;g.choose("SOLITUDE:buy:2:0:max");assertEquals(2,g.progress.quantity("Отмычка"));
        g.hour=9;g.choose("SOLITUDE:buy:4:0:max");assertEquals(2,g.progress.quantity("Отмычка"));
        g.progress.quantity("Железный слиток",1000);int before=g.gold;
        g.choose("SOLITUDE:buy:2:1:max");assertEquals(before,g.gold);
    }
    @Test public void expandedCatalogAndOldStockMigration() throws Exception {
        GameEngine g=city(0);assertTrue(g.scene().choices.stream().anyMatch(c->c.title.contains("Золотое ожерелье")));
        org.json.JSONArray row=new org.json.JSONArray().put(1).put(0).put(0).put(0).put(0);
        org.json.JSONObject old=new org.json.JSONObject().put("sold",new org.json.JSONArray().put(row)).put("cycle",new org.json.JSONArray().put((g.day-1)/2));
        SolitudeShops shop=SolitudeShops.restore(old);
        assertEquals(2,shop.stock(g,0,0));assertEquals(3,shop.stock(g,0,5));
    }
}
