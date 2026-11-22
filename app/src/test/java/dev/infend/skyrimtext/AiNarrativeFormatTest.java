package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AiNarrativeFormatTest {
    @Test public void stripsLocationTimeHeaderAndEmptyParagraphs() {
        assertEquals("Ветер бьёт в лицо.\n**Стражник:**\n**— Стой.**\nТы идёшь дальше.",
            AiNarrativeFormat.compact("**Вайтран, день 32, час 10**\r\n\r\nВетер бьёт в лицо.\n\n**Стражник:**\n\n**— Стой.**\n\n\nТы идёшь дальше.\n"));
    }

    @Test public void handlesVariantsAndLeavesNarrativeTimeIntact() {
        assertEquals("Идёт снег.\nТы понимаешь, что на дворе день 32, и продолжаешь путь.",
            AiNarrativeFormat.compact("📍 Локация: Вайтран | День 32 | Время 10:00\n" +
                "Место: Вайтран\n\nИдёт снег.\nТы понимаешь, что на дворе день 32, и продолжаешь путь."));
        assertEquals("Тишина.\nНочь.",
            AiNarrativeFormat.compact("День 32, час 10\nТишина.\n\nНочь."));
    }

    @Test public void handlesEmptyAndAlreadyCompactText() {
        assertEquals("", AiNarrativeFormat.compact(null));
        assertEquals("Первая строка\nВторая строка",
            AiNarrativeFormat.compact("Первая строка\nВторая строка"));
    }
}
