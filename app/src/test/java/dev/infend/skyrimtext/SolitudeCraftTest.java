package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class SolitudeCraftTest {
    private GameEngine city(int d){
        GameEngine g=new GameEngine();g.start("Тест","Норд","Воин");g.region=3;g.progress.location="Солитьюд";g.mode="WORLD";g.hour=9;g.stamina=100;
        g.choose("SOLITUDE:open");g.choose("SOLITUDE:visit:"+d);return g;
    }
    @Test public void smithConsumesRealMaterialsAndTime() throws Exception {
        GameEngine g=city(5);g.progress.quantity("Железный слиток",2);g.progress.quantity("Кожаные полоски",1);
        int before=g.progress.quantity("Железный меч");
        g.choose("SOLITUDE:craft:make:0");
        assertEquals(before+1,g.progress.quantity("Железный меч"));assertEquals(0,g.progress.quantity("Железный слиток"));assertEquals(0,g.progress.quantity("Кожаные полоски"));assertEquals(10,g.hour);
        g=GameEngine.fromJson(g.toJson().toString());g.choose("SOLITUDE:craft:make:0");assertEquals(before+1,g.progress.quantity("Железный меч"));
    }
    @Test public void wrongDistrictClosedAndOverweightCannotCraft() {
        GameEngine g=city(0);g.progress.quantity("Железный слиток",2);g.progress.quantity("Кожаные полоски",1);
        g.choose("SOLITUDE:craft:make:0");assertEquals(2,g.progress.quantity("Железный слиток"));
        g.choose("SOLITUDE:back");g.choose("SOLITUDE:visit:5");g.hour=23;
        g.choose("SOLITUDE:craft:make:0");assertEquals(2,g.progress.quantity("Железный слиток"));
        g.hour=9;g.progress.quantity("Стальной слиток",100);
        g.choose("SOLITUDE:craft:make:0");assertEquals(2,g.progress.quantity("Железный слиток"));assertEquals(1,g.progress.quantity("Кожаные полоски"));
    }
    @Test public void brewUsesExistingPotionCounterAndCannotReplay() throws Exception {
        GameEngine g=city(0);g.choose("SOLITUDE:store:1");g.progress.quantity("Пшеница",1);g.progress.quantity("Голубой горноцвет",1);
        int before=g.potions;g.choose("SOLITUDE:craft:brew");assertEquals(before+1,g.potions);assertEquals("SOLITUDE:store:1",g.mode);assertEquals(10,g.hour);
        assertEquals(0,g.progress.quantity("Пшеница"));assertEquals(0,g.progress.quantity("Голубой горноцвет"));
        g=GameEngine.fromJson(g.toJson().toString());g.choose("SOLITUDE:craft:brew");assertEquals(before+1,g.potions);
    }
}
