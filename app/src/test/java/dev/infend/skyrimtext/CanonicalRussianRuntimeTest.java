package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class CanonicalRussianRuntimeTest {
    @Test public void everyReviewedNpcResolvesToItsOriginalIdentity() {
        for (String[] row : RussianNameData.NPC) {
            NpcProfiles.Profile old = NpcProfiles.findOriginal(row[0]);
            assertNotNull(row[0], old);
            assertSame(row[1], old, NpcProfiles.findOriginal(row[1]));
            assertEquals(row[0], old.name);
            assertEquals(row[1], old.displayName);
            assertEquals(row[0], NpcProfiles.canonicalName(row[1]));
        }
    }

    @Test public void dlcBaseIdsAndEnglishAliasesIdentifyTheSamePerson() {
        NpcProfiles.Profile agmaer = NpcProfiles.findOriginal("Агмейр");
        assertSame(agmaer, NpcProfiles.findOriginal("DAWNGUARD_00336E"));
        assertSame(agmaer, NpcProfiles.findOriginal("Agmaer"));
        assertEquals("Агмейр:", CanonicalRussian.speakerLine("[NPC:DAWNGUARD_00336E]:"));
        assertEquals("[NPC:DRAGONBORN_00336E]:", CanonicalRussian.speakerLine("[NPC:DRAGONBORN_00336E]:"));
        GameEngine game = new GameEngine();
        game.mode = "CANON";
        game.canonicalStage = 5;
        assertTrue(CanonicalRussian.speakerContext(game).contains("Фаренгар Тайный Огонь [NPC:00013BBB]"));
    }

    @Test public void bothDiamondsKeepTheirValueWeightAndOriginalSaveKey() throws Exception {
        RpgProgress state = RpgProgress.fromJson(new JSONObject().put("inventory",
            new JSONObject().put("Алмаз", 2).put("Безупречный алмаз", 3)));
        for (String[] names : RussianNameData.ITEMS) {
            ItemCatalog.Item old = ItemCatalog.find(names[0]);
            assertNotNull(old);
            assertSame(old, ItemCatalog.find(names[1]));
            assertEquals(names[1], old.name);
            assertTrue(old.matches(names[0]));
            assertTrue(old.matches(names[1]));
        }
        assertEquals(2, state.quantity("Бриллиант"));
        assertEquals(3, state.quantity("Безупречный бриллиант"));
        assertEquals(800, ItemCatalog.find("Бриллиант").value);
        assertEquals(0.1, ItemCatalog.find("Бриллиант").weight, 0.0001);
    }

    @Test public void oldDestinationsAndNewLabelsSelectTheSameMarker() {
        for (String[] row : RussianNameData.MAP_ALIASES) {
            SkyrimMapCatalog.Site original = SkyrimMapCatalog.find(row[1]);
            assertSame(row[0], original, SkyrimMapCatalog.find(row[0]));
            assertSame(original, SkyrimMapCatalog.find(original.name));
            assertTrue(original.matches(row[0]));
            assertTrue(original.matches(original.name));
            assertSame(WorldAtlas.find(original.name), WorldAtlas.find(row[0]));
        }
    }

    @Test public void perksKeepTheirIndicesAndBothMatchingSetsRemainDistinct() {
        for (String[] row : RussianNameData.PERKS) {
            int index = Integer.parseInt(row[0]);
            assertEquals(row[1], AdvancedPerks.NAMES[index]);
            assertTrue(CanonicalRussian.matchesPerk(index, row[1]));
            assertTrue(CanonicalRussian.matchesPerk(index, row[3]));
            if (!row[2].isEmpty()) assertTrue(CanonicalRussian.matchesPerk(index, row[2]));
        }
        assertEquals(2, CanonicalRussian.findPerks("Полный комплект").size());
        assertEquals(1, CanonicalRussian.findPerks("Полный тяжёлый комплект").size());
        assertEquals("Страж Рассвета", new FactionStories().chapter(6).title);
    }

    @Test public void oldAndVisibleItemKeysSurviveLoadingAndSaveTogether() throws Exception {
        JSONObject source = new JSONObject()
            .put("inventory", new JSONObject().put("Эбонитовая кираса", 2).put("Эбонитовая броня", 1))
            .put("equipped", new JSONObject().put("body", "Эбонитовая броня"))
            .put("equipmentInitialized", true)
            .put("tempered", new JSONObject().put("Эбонитовая броня", 2));
        RpgProgress p = RpgProgress.fromJson(source);
        assertEquals(3, p.quantity("Эбонитовая броня"));
        assertEquals(3, p.quantity("Эбонитовая кираса"));
        assertFalse(p.inventory.has("Эбонитовая броня"));
        assertEquals("Эбонитовая кираса", p.equippedName("body"));
        assertEquals(2, p.temperQuality("Эбонитовая кираса"));
        p = RpgProgress.fromJson(p.toJson());
        assertEquals(3, p.quantity("Эбонитовая броня"));
        assertTrue(p.removeItem("Эбонитовая броня", 1));
        assertEquals(2, p.quantity("Эбонитовая кираса"));
    }

    @Test public void providerAliasesAreGroupedBeforeInventoryValidation() throws Exception {
        GameEngine g = new GameEngine();
        g.start("Герой", "Норд", "Воин");
        g.progress.quantity("Кольцо золотое", 1);
        JSONArray inv = new JSONArray()
            .put(new JSONObject().put("name", "Кольцо золотое").put("delta", -1))
            .put(new JSONObject().put("name", "Золотое кольцо").put("delta", 1));
        JSONObject reply = new JSONObject();
        reply.put("scene", "Ты проверяешь записи торговли и осматриваешь кольцо в свете свечи. ".repeat(5));
        JSONArray choices = new JSONArray();
        for (int i=0; i<10; i++) choices.put(new JSONObject().put("title", "Действие " + i)
            .put("detail", "Осмотреть следующую запись " + i).put("chance", JSONObject.NULL).put("skill", ""));
        reply.put("choices", choices).put("effects", new JSONObject().put("inventory", inv))
            .put("customChance", JSONObject.NULL).put("journal", "Проверена запись торговца");
        AiGameTurn.parseModel(reply.toString(), -2).apply(g, true);
        assertEquals(1, g.progress.quantity("Кольцо золотое"));
        assertFalse(g.progress.inventory.has("Золотое кольцо"));
    }

    @Test public void identifiedHeadingsChangeWhileProseAndAuthoredSpeakersRemainIntact() {
        assertEquals("**Фаренгар Тайный Огонь:**", CanonicalRussian.speakerLine("**[NPC:00013BBB]:**"));
        String text = "**Мой Фаренгар:**\n**— Отнеси Фаренгару это письмо.**";
        assertEquals(text, AiNarrativeFormat.compact(text));
        assertEquals("**[NPC:DEADBEEF]:**", CanonicalRussian.speakerLine("**[NPC:DEADBEEF]:**"));
        assertEquals("**Кэтрин:**", CanonicalRussian.speakerLine("**Кэтрин:**"));
        assertTrue(CanonicalRussian.unverifiedSpeakers(text).contains("Мой Фаренгар"));
        assertFalse(CanonicalRussian.unverifiedSpeakers("**Кэтрин:**\n**— Я здесь.**").contains("Кэтрин"));
    }
}
