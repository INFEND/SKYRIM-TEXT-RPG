package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
/** Canonical titles are presentation only; save and lookup IDs stay unchanged. */
public class SkyrimRussianGuardNamesTest {
    private static final String[][] GUARDS={
        {"Стражник", "Guard", "Стражник"},
        {"Стражник Вайтрана", "Whiterun Guard", "Стражник Вайтрана"},
        {"Стражник Солитьюда", "Solitude Guard", "Стражник Солитьюда"},
        {"Стражник Виндхельма", "Windhelm Guard", "Стражник Виндхельма"},
        {"Стражник Рифтена", "Riften Guard", "Стражник Рифтена"},
        {"Стражник Маркарта", "Markarth Guard", "Стражник Маркарта"},
        {"Стражник Фолкрита", "Falkreath Guard", "Стражник Фолкрита"},
        {"Стражник Морфала", "Morthal Guard", "Стражник Морфала"},
        {"Стражник Данстара", "Dawnstar Guard", "Стражник Данстара"},
        {"Стражник Винтерхолда", "Winterhold Guard", "Стражник Винтерхолда"},
        {"Стражник Вороньей Скалы", "Redoran Guard", "Редоранская стража"},
    };
    @Test public void allRegionalGuardsKeepTheirOriginalIdentity() {
        assertEquals(11,GUARDS.length);
        for(String[] entry:GUARDS){
            NpcProfiles.Profile p=NpcProfiles.findOriginal(entry[0]);
            assertNotNull(entry[0],p);
            assertTrue(p.template);
            assertEquals(entry[0],p.name);
            assertEquals(entry[2],p.displayName);
            assertSame(entry[1],p,NpcProfiles.findOriginal(entry[1]));
            assertEquals(entry[2],NpcProfiles.canonicalName(entry[0]));
        }
    }
    @Test public void redoranGuardHasBothNewAndOldNames() {
        NpcProfiles.Profile guard=NpcProfiles.findOriginal("Стражник Вороньей Скалы");
        assertSame(guard,NpcProfiles.findOriginal("Raven Rock Guard"));
        assertSame(guard,NpcProfiles.findOriginal("Redoran Guard"));
        assertSame(guard,NpcProfiles.findOriginal("Редоранская стража"));
        assertEquals("Стражник Вороньей Скалы",guard.name);
        assertEquals("Редоранская стража",guard.displayName);
    }
}
