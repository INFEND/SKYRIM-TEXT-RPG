package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class NpcProfilesCompletionTest {
    @Test public void newProfilesAndAliases(){
        assertEquals("Данмер",NpcProfiles.find("Галдрус Хлерву").race);
        assertSame(NpcProfiles.find("Илдари Саротрил"),NpcProfiles.find("Ildari Sarothril"));
        assertEquals("Редгард",NpcProfiles.find("Эбонитовый воин").race);
        assertEquals("Женский",NpcProfiles.find("Минорна").gender);
        assertSame(NpcProfiles.find("Гьялунд Морской Волк"),NpcProfiles.find("Гьялунд Пуд Соли"));
        assertFalse(NpcProfiles.find("Кнут").child);
        assertTrue(NpcProfiles.find("Гралнах").child);
    }
}
