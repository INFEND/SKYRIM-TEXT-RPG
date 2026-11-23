package dev.infend.skyrimtext;

import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

public class PerkLexiconTest {
    @Test public void everyPublishedRankFindsOneFamilyInTheRightTree() {
        assertEquals(199, PerkLexicon.all().size());
        int[] counts = new int[20];
        Set<String> ids = new HashSet<>();
        for (PerkLexicon.Entry family : PerkLexicon.all()) {
            counts[family.tree] += family.forms.size();
            assertTrue(PerkLexicon.search(family.name, family.tree).contains(family));
            assertTrue(PerkLexicon.search(family.english, family.tree).contains(family));
            for (String form : family.forms) {
                assertTrue(ids.add(form));
                List<PerkLexicon.Entry> hits = PerkLexicon.search(form, -1);
                assertEquals(1, hits.size());
                assertSame(family, hits.get(0));
                assertTrue(PerkLexicon.search(form, (family.tree + 1) % 20).isEmpty());
            }
        }
        assertArrayEquals(new int[]{21,19,16,13,12,10,10,15,13,13,11,12,13,17,13,14,13,16,11,11}, counts);
        assertEquals(273, ids.size());
    }

    @Test public void sameRussianNamesRemainSeparateAndHistoricalAliasesChooseTheRightTree() {
        assertEquals(2, PerkLexicon.search("Полный комплект", -1).size());
        PerkLexicon.Entry heavy = PerkLexicon.search("Полный тяжёлый комплект", -1).get(0);
        assertSame(heavy, PerkLexicon.search("00107832", -1).get(0));
        assertNotSame(heavy, PerkLexicon.search("00051B17", -1).get(0));
        assertEquals(Arrays.asList("000BABE4","00079343","00079342","00079344","00079345"),
            PerkLexicon.search("Armsman", -1).get(0).forms);
        PerkLexicon.Entry beast = PerkLexicon.search("Dawnguard:011CFB", -1).get(0);
        assertEquals("Звериная сила", beast.name);
        assertTrue(beast.details().contains("Ранг 4 | DAWNGUARD_011CFB"));
        assertTrue(PerkLexicon.search("Dragonborn:011CFB", -1).isEmpty());
    }

    @Test public void promptsUseOnlyIdentifiedPerksAndLeaveAuthoredProseAlone() {
        assertEquals("Сильная рука | Одноручное оружие | ранг 3", PerkLexicon.promptReference("[PERK:00079342]"));
        assertEquals("Хватка вампира | Вампир-лорд", PerkLexicon.promptReference("способность «Хватка вампира»"));
        assertEquals("", PerkLexicon.promptReference("перк «Полный комплект»"));
        assertEquals("", PerkLexicon.promptReference("Сильной рукой он взял меч."));
        assertEquals("", PerkLexicon.promptReference("Мой перк «Тихая искра», [PERK:ABCD1234]"));
    }
}
