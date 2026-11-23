package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class WikiRuMapCompatibilityTest {
    private static final String[][] CASES={
        {"Merryfair Farm","skyrim-118","Веселая ферма","Весёлая ферма"},
        {"Driftshade Refuge","skyrim-145","Дрифтшейд - Приют","Дрифтшейд — Приют"},
        {"Evergreen Grove","skyrim-163","Вечнозеленая роща","Вечнозелёная роща"},
        {"Black-Briar Lodge","skyrim-277","Резиденция Черный Вереск","Резиденция «Чёрный Вереск»"},
        {"Chillfurrow Farm","skyrim-349","Ферма Холодная борозда","Ферма «Холодная Борозда»"},
        {"Snow Veil Sanctum","skyrim-227","Снежная Завеса - Святилище","Снежная Завеса — Святилище"},
        {"Sundered Towers","skyrim-263","Разделенные башни","Разделённые башни"},
        {"Bleakwind Basin","skyrim-159","Водоем Холодный Ветер","Водоём Холодный Ветер"},
        {"Bleakwind Bluff","skyrim-262","Утес Холодный Ветер","Утёс Холодный Ветер"},
        {"Serpent's Bluff Redoubt","skyrim-2","Оплот Змеиный Утес","Оплот Змеиный Утёс"},
        {"Journeyman's Nook","skyrim-214","Уголок подмастерья","Приют Подмастерья"},
        {"Ruunvald Excavation (DG)","skyrim-2289","Раскопки Рунвальда","Рунвальд — Раскопки"},
        {"Darkwater Crossing","skyrim-212","Черный Брод","Чёрный Брод"},
        {"Stonehill Bluff","skyrim-151","Утес Каменный Холм","Утёс Каменный Холм"},
        {"Wreck Of The Brinehammer","skyrim-311","Место крушения Соленого молота","Место крушения «Солёного молота»"},
        {"Wreck of the Winter War","skyrim-307","Место крушения Зимней Войны","Место крушения «Зимней войны»"},
        {"Wreck of the Icerunner","skyrim-312","Место крушения Бегущего по льду","Место крушения «Бегущего по льду»"},
        {"Redwater Den (DG)","skyrim-2288","Логово Красная Вода","Красноводное логово"},
        {"Frostmoon Crag","solstheim-2532","Ущелье Фростмуна","Утёс Морозной Луны"},
    };
    @Test public void historicalNamesStillMatchOriginalMarker() {
        assertEquals(19,CASES.length);
        for(String[] row:CASES){
            SkyrimMapCatalog.Site site=SkyrimMapCatalog.find(row[0]);
            assertNotNull(row[0],site);
            assertEquals(row[1],site.id);
            assertEquals(row[3],site.name);
            assertSame(site,SkyrimMapCatalog.find(row[2]));
            assertSame(site,SkyrimMapCatalog.find(row[3]));
            assertTrue(row[2],site.matches(row[2]));
            assertTrue(row[3],site.matches(row[3]));
        }
    }
}
