package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorldWeatherTest {
    @Test public void sameGameStateIsStable(){
        WorldWeather a=WorldWeather.at(0,"Вайтран",12,14);
        WorldWeather b=WorldWeather.at(0,"Вайтран",12,14);
        assertEquals(a.weather,b.weather);
        assertEquals(a.temperature,b.temperature);
        assertEquals("14:00",a.time());
    }
    @Test public void mountainIsColderAndNightUsesMoon(){
        WorldWeather warm=WorldWeather.at(0,"Вайтран",1,2);
        WorldWeather cold=WorldWeather.at(0,"Высокий Хротгар",1,2);
        assertTrue(cold.temperature<warm.temperature);
        assertEquals("moon",warm.timeGlyph());
        assertEquals("sun",WorldWeather.at(0,"Вайтран",1,12).timeGlyph());
        assertEquals("Под землёй",WorldWeather.at(0,"Чёрный Предел",1,12).weather);
    }
}
