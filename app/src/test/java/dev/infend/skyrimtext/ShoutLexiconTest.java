package dev.infend.skyrimtext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShoutLexiconTest {
    @Test public void everyPlayerShoutHasThreeUniqueOrderedWords() {
        assertEquals(27, ShoutLexicon.all().size());
        Set<String> wordIds = new HashSet<>();
        for (ShoutLexicon.Entry entry : ShoutLexicon.all()) {
            assertSame(entry, ShoutLexicon.find(entry.name));
            assertSame(entry, ShoutLexicon.find(entry.english));
            assertSame(entry, ShoutLexicon.find(entry.id));
            assertEquals(3, entry.words.size());
            for (int i = 0; i < 3; i++) {
                ShoutLexicon.Word word = entry.words.get(i);
                assertEquals(i + 1, word.position);
                assertTrue(wordIds.add(word.id));
                assertEquals(List.of(entry), ShoutLexicon.search(word.id));
                assertTrue(ShoutLexicon.search(word.dragon).contains(entry));
                assertTrue(entry.detail().contains(word.line()));
            }
        }
        assertEquals(81, wordIds.size());
        for (String ability : RpgContent.SHOUTS) {
            assertNotNull(ShoutLexicon.find(ability.split("\\|", 2)[0]));
        }
    }

    @Test public void knownWordAndDlcReferencesResolveInTheCodexAndPrompt() {
        ShoutLexicon.Entry force = ShoutLexicon.find("00013E07");
        assertEquals("Безжалостная сила", force.name);
        assertEquals("Фус (FUS) — Сила\nРо (RO) — Равновесие\nДа (DAH) — Толчок", force.detail());
        assertTrue(ShoutLexicon.search("равновесие").contains(force));
        assertEquals("Разрыв души", ShoutLexicon.find("DAWNGUARD_007CB6").name);
        assertNull(ShoutLexicon.find("DRAGONBORN_007CB6"));
        assertEquals("Мид", ShoutLexicon.find("Ярость боя").words.get(0).label());
        assertTrue(ShoutLexicon.promptReference("Я применяю крик «Безжалостная сила».").contains("Фус (FUS)"));
        assertTrue(ShoutLexicon.promptReference("[SHOUT:DRAGONBORN_0179D8]").contains("Подчинение воли"));
        assertEquals("", ShoutLexicon.promptReference("У моей лошади кличка Циклон."));
        assertEquals("", ShoutLexicon.promptReference("Крик «Мой собственный голос»"));
    }

    @Test public void conflictingRussianFieldsStayOutOfThePlayersText() {
        ShoutLexicon.Word fire = ShoutLexicon.find("Fire Breath").words.get(1);
        assertEquals("Тор (TOOR)", fire.line());
        ShoutLexicon.Word wuld = ShoutLexicon.find("Whirlwind Sprint").words.get(0);
        assertEquals("WULD — Вихрь", wuld.line());
        assertFalse(ShoutLexicon.promptReference("Крик «Огненное дыхание»").contains("Пекло"));
    }
}
