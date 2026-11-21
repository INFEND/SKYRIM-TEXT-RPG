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
        assertTrue(NpcProfiles.find("Кнут").child);
        assertTrue(NpcProfiles.find("Гралнах").child);
    }
    @Test public void checkedIdentities(){
        assertTrue(NpcProfiles.find("Knud").child);
        assertEquals("Норд",NpcProfiles.find("Minette Vinius").race);
        assertEquals("Женский",NpcProfiles.find("Michel Lylvieve").gender);
        assertSame(NpcProfiles.find("Геймунда"),NpcProfiles.find("Греймунд"));
        assertSame(NpcProfiles.find("Таэр"),NpcProfiles.find("Тир"));
        assertNotSame(NpcProfiles.find("Гуньяр"),NpcProfiles.find("Гуньяр (Озёрное)"));
        assertEquals("Данмер",NpcProfiles.find("Servos Rendas").race);
        assertEquals("Норд",NpcProfiles.find("Tharstan").race);
    }
}
