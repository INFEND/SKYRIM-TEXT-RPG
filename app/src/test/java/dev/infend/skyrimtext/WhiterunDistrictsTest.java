package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;

public class WhiterunDistrictsTest {
    private GameEngine city(){GameEngine g=new GameEngine();g.started=true;g.region=0;g.progress.location="Вайтран";g.mode="WORLD";return g;}
    @Test public void districtsAndLocalMovement(){
        GameEngine g=city();
        int day=g.day,gold=g.gold;
        g.choose("CITY_OPEN");assertEquals("CITY_MAP",g.mode);assertEquals(5,g.scene().choices.size());
        g.choose("CITY:wind");assertEquals("CITY",g.mode);
        g.choose("CITY:skyforge");assertTrue(g.locationLabel().contains("Небесная кузница"));
        g.choose("CITY:mare");assertEquals("skyforge",g.whiterunSection);
        assertEquals("Вайтран",g.progress.location);assertEquals(day,g.day);assertEquals(gold,g.gold);
        g.choose("GO_WORLD");assertTrue(g.scene().title.contains("Небесная кузница"));
    }
    @Test public void saveAndInvalidLocation() throws Exception {
        GameEngine g=city();g.choose("CITY_OPEN");g.choose("CITY:cloud");g.choose("CITY:throne");
        GameEngine restored=GameEngine.fromJson(g.toJson().toString());assertEquals("throne",restored.whiterunSection);assertEquals("CITY",restored.mode);
        restored.choose("GO_WORLD");restored.progress.location="Ривервуд";restored.choose("CITY_OPEN");assertEquals("WORLD",restored.mode);
        assertEquals("Ривервуд",restored.locationLabel());
    }
}
