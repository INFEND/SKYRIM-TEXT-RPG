package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class RussianBestiaryIdentityTest {
    @Test public void reveredDragonUsesWikiNameAndKeepsItsOriginalKey() {
        BestiaryCatalog.Entry dragon=BestiaryCatalog.find("Почитаемый дракон");
        assertNotNull(dragon);
        assertSame(dragon,BestiaryCatalog.find("Благородный дракон"));
        assertEquals("Почитаемый дракон",dragon.id);
        assertEquals("Благородный дракон",dragon.name);
        assertSame(NpcProfiles.findOriginal("Revered Dragon"),NpcProfiles.findOriginal(dragon.name));
    }
    @Test public void draugrDeathlordIsNotRenamedInLegacySaves() {
        BestiaryCatalog.Entry draugr=BestiaryCatalog.find("Драугр-повелитель смерти");
        assertNotNull(draugr);
        assertSame(draugr,BestiaryCatalog.find("Драугр-военачальник"));
        assertEquals("Драугр-повелитель смерти",draugr.id);
        assertEquals("Драугр-военачальник",draugr.name);
    }
}
