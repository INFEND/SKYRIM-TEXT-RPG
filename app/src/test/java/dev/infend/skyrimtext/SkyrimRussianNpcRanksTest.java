package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
/** Wiki-confirmed enemy rank labels, with historical names retained as stable save keys. */
public class SkyrimRussianNpcRanksTest {
    private static final String[][] RANKS={
        {"Бандит", "Bandit", "Бандит"},
        {"Бандит-изгой", "Bandit Outlaw", "Разбойник"},
        {"Бандит-головорез", "Bandit Thug", "Громила"},
        {"Бандит-разбойник", "Bandit Highwayman", "Грабитель"},
        {"Бандит-грабитель", "Bandit Plunderer", "Головорез"},
        {"Бандит-мародёр", "Bandit Marauder", "Мародёр"},
        {"Главарь бандитов", "Bandit Chief", "Главарь бандитов"},
        {"Ученик некроманта", "Apprentice Necromancer", "Некромант-ученик"},
        {"Адепт некромантии", "Adept Necromancer", "Некромант-адепт"},
        {"Некромант-мастер", "Master Necromancer", "Некромант-мастер"},
        {"Некромант", "Necromancer", "Некромант"},
    };
    @Test public void allEnemyRanksKeepTheirOriginalSaveKeyAndUseRussianTitle() {
        assertEquals(11,RANKS.length);
        for(String[] row:RANKS){
            NpcProfiles.Profile entity=NpcProfiles.findOriginal(row[0]);
            assertNotNull(row[0],entity);
            assertTrue(entity.template);
            assertEquals("Save identity changed",row[0],entity.name);
            assertEquals("Display label wrong",row[2],entity.displayName);
            assertSame("English actor alias broken",entity,NpcProfiles.findOriginal(row[1]));
            assertEquals("Canonical scene name wrong",row[2],NpcProfiles.canonicalName(row[0]));
        }
    }
    @Test public void outlawAndHighwaymanRemainDistinctDespiteOldAmbiguousAlias() {
        NpcProfiles.Profile outlaw=NpcProfiles.findOriginal("Бандит-изгой");
        NpcProfiles.Profile highwayman=NpcProfiles.findOriginal("Бандит-разбойник");
        assertNotSame(outlaw,highwayman);
        assertSame(outlaw,NpcProfiles.findOriginal("Разбойник"));
        assertSame(highwayman,NpcProfiles.findOriginal("Грабитель"));
        assertEquals("Бандит-разбойник",highwayman.name);
    }
}
