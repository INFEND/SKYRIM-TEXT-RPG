package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
/** Verifies canonical display names while retaining original template IDs and names. */
public class RussianNpcTemplateLocalizationTest {
    private static final String[][] LABELS={
        {"Фалмер-воитель","Фалмер-мастер теней"},
        {"Фалмер-военачальник","Фалмер-наёмник"},
        {"Фалмер-скрытень","Фалмер-мастер укрытий"},
        {"Фалмер-мракобес","Фалмер-сумрачный страж"},
        {"Фалмер-сумеречный","Фалмер-ночной охотник"},
        {"Гневный человек","Яростный человек"},
        {"Скрытень","Луркер"},
        {"Волк (существо)","Волк"},
        {"Ледяной волк","Снежный волк"},
        {"Мать дымков","Мать-дымок"},
        {"Огонёк","Дымок"},
        {"Кор","Корус"},
        {"Кор-жнец","Корус-жнец"},
        {"Кор-охотник","Корус-охотник"},
        {"Горящий спригган","Горелый спригган"},
        {"Спригган-земная мать","Спригган-мать земли"},
    };
    @Test public void oldNpcTemplateKeysStillOpenTheSameProfile() {
        assertEquals(16,LABELS.length);
        for(String[] row:LABELS){
            NpcProfiles.Profile profile=NpcProfiles.findOriginal(row[0]);
            assertNotNull(row[0],profile);
            assertTrue(profile.template);
            assertEquals(row[0],profile.name);
            assertEquals(row[1],profile.displayName);
            if(!row[0].equals("Волк (существо)"))
                assertSame(row[0],profile,NpcProfiles.findOriginal(row[1]));
            else assertNotSame(profile,NpcProfiles.findOriginal("Волк"));
        }
    }
    @Test public void templateCanonicalNameUsesReviewedDisplayWithoutRewritingSaveKeys() {
        for(String[] row:LABELS){
            NpcProfiles.Profile profile=NpcProfiles.findOriginal(row[0]);
            assertNotNull(row[0],profile);
            assertEquals("Save key changed",row[0],profile.name);
            assertEquals("Canonical speech and UI label differs",row[1],NpcProfiles.canonicalName(row[0]));
            assertEquals("Canonical lookup should be idempotent",row[1],NpcProfiles.canonicalName(row[1]));
        }
        // Two different characters may share a visible name; never overwrite the named NPC.
        assertFalse(NpcProfiles.findOriginal("Волк").template);
        assertTrue(NpcProfiles.findOriginal("Волк (существо)").template);
        assertEquals("Волк",NpcProfiles.canonicalName("Волк (существо)"));
    }
    @Test public void renamedCreaturesAreStillFoundInTheBestiaryByOldLabel() {
        assertSame("Фалмер-воитель",BestiaryCatalog.find("Фалмер-мастер теней"),BestiaryCatalog.find("Фалмер-воитель"));
        assertSame("Фалмер-военачальник",BestiaryCatalog.find("Фалмер-наёмник"),BestiaryCatalog.find("Фалмер-военачальник"));
        assertSame("Гневный человек",BestiaryCatalog.find("Яростный человек"),BestiaryCatalog.find("Гневный человек"));
        assertSame("Скрытень",BestiaryCatalog.find("Луркер"),BestiaryCatalog.find("Скрытень"));
        assertSame("Волк (существо)",BestiaryCatalog.find("Волк"),BestiaryCatalog.find("Волк (существо)"));
        assertSame("Ледяной волк",BestiaryCatalog.find("Снежный волк"),BestiaryCatalog.find("Ледяной волк"));
        assertSame("Мать дымков",BestiaryCatalog.find("Мать-дымок"),BestiaryCatalog.find("Мать дымков"));
        assertSame("Огонёк",BestiaryCatalog.find("Дымок"),BestiaryCatalog.find("Огонёк"));
        assertSame("Кор",BestiaryCatalog.find("Корус"),BestiaryCatalog.find("Кор"));
        assertSame("Кор-охотник",BestiaryCatalog.find("Корус-охотник"),BestiaryCatalog.find("Кор-охотник"));
        assertSame("Горящий спригган",BestiaryCatalog.find("Горелый спригган"),BestiaryCatalog.find("Горящий спригган"));
    }
}
