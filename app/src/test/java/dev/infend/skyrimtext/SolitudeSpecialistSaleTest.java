package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeSpecialistSaleTest {
    @Test public void craftedWeaponCanBeSoldOnlyToRelevantMerchant() throws Exception {
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;
        g.progress.quantity("Железный кинжал",2);
        int before=g.gold,p=g.salePrice(ItemCatalog.find("Железный кинжал"));
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:0");g.choose("SOLITUDE:store:1");
        g.choose("SOLITUDE:extraSell:1:Железный кинжал:max");assertEquals(2,g.progress.quantity("Железный кинжал"));assertEquals(before,g.gold);
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:5");g.choose("SOLITUDE:store:4");
        g.choose("SOLITUDE:extraSell:4:Железный кинжал:max");assertEquals(0,g.progress.quantity("Железный кинжал"));assertEquals(before+2*p,g.gold);
        g=GameEngine.fromJson(g.toJson().toString());g.choose("SOLITUDE:extraSell:4:Железный кинжал:max");assertEquals(before+2*p,g.gold);
    }
    @Test public void closedShopAndEquippedItemAreProtected(){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:5");g.choose("SOLITUDE:store:4");
        g.progress.quantity("Железный кинжал",1);g.hour=23;g.choose("SOLITUDE:extraSell:4:Железный кинжал:max");assertEquals(1,g.progress.quantity("Железный кинжал"));
        g.hour=9;g.progress.equip("Железный кинжал");g.choose("SOLITUDE:extraSell:4:Железный кинжал:max");assertEquals(1,g.progress.quantity("Железный кинжал"));
    }
}
