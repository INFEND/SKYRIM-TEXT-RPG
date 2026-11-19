package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class NpcProfilesExpansionTest {
    @Test public void namesAndAliasesResolveToSameProfile(){
        assertSame(NpcProfiles.find("Эйла Охотница"),NpcProfiles.find("эйла"));
        assertSame(NpcProfiles.find("Мавен Чёрный Вереск"),NpcProfiles.find("Мавен Черный Вереск"));
        assertSame(NpcProfiles.find("Дж'зарго"),NpcProfiles.find("Дж’зарго"));
        assertEquals("Женский",NpcProfiles.find("Лидия").gender);
        assertEquals("Бретон",NpcProfiles.find("Дельфина").race);
        assertEquals("Данмер",NpcProfiles.find("Нелот").race);
        assertEquals("Снежный эльф",NpcProfiles.find("Гелебор").race);
        assertTrue(NpcProfiles.find("Дюрневир").dragon);
        assertNull(NpcProfiles.find("Неизвестный вымышленный персонаж"));
    }
}
