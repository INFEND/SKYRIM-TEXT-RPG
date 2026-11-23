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

    @Test public void sharedDragonBaseRequiresTheIndividualReferenceOrSavedName() throws Exception {
        String base = "DRAGONBORN_03D5B5";
        assertNull(NpcProfiles.findOriginal(base));
        assertEquals("[NPC:" + base + "]:", CanonicalRussian.speakerLine("[NPC:" + base + "]:"));
        assertEquals("Крузикрел:", CanonicalRussian.speakerLine("[NPC:DRAGONBORN_01D573]:"));
        assertEquals("Релоникив:", CanonicalRussian.speakerLine("[NPC:DRAGONBORN_01D572]:"));
        assertSame(NpcProfiles.findOriginal("Релоникрив"), NpcProfiles.findOriginal("DRAGONBORN_01D572"));
        assertNotSame(NpcProfiles.findOriginal("Крузикрел"), NpcProfiles.findOriginal("Релоникив"));
        for (String title : new String[]{"Крузикрел", "Релоникрив", "Мой дракон"}) {
            GameEngine game = new GameEngine();
            game.sceneState.confirm("npc", base, title, "MET");
            game.sceneState = SceneState.fromJson(game.sceneState.toJson());
            String expected = title.equals("Крузикрел") ? "Крузикрел [NPC:DRAGONBORN_01D573]" :
                title.equals("Релоникрив") ? "Релоникив [NPC:DRAGONBORN_01D572]" : "";
            assertEquals(expected, CanonicalRussian.speakerContext(game));
            assertTrue(game.sceneState.contains("npc", base));
        }
    }

    @Test public void aConfirmedNamedCombatantCanSpeakUnderItsOriginalId() {
        GameEngine game = new GameEngine();
        game.mode = "COMBAT";
        game.enemyHp = 25;
        game.enemyName = "Мирмулнир";
        assertEquals("Мирмулнир [NPC:0001CA05]", CanonicalRussian.speakerContext(game));
        game.enemyName = "Мирмулнир-младший";
        assertEquals("", CanonicalRussian.speakerContext(game));
        game.name = game.enemyName = "Мирмулнир";
        assertEquals("", CanonicalRussian.speakerContext(game));
    }

    @Test public void telMithrynOldSaveAndNewMapUseTheSameQuestAndGallery() {
        RoadQuests.Task task = RoadQuests.find("steward_intro");
        assertEquals("Тел-Митрин", task.place);
        GameEngine game = new GameEngine();
        game.progress.location = task.place;
        game.progress = RpgProgress.fromJson(game.progress.toJson());
        assertTrue(RoadQuests.here(game, task));
        WorldAtlas.Place old = WorldAtlas.find(task.place);
        assertEquals("Тель-Митрин", old.name);
        assertSame(old, WorldAtlas.find("Тель-Митрин"));
        assertEquals(1041, old.x);
        assertEquals(457, old.y);
        assertEquals("solstheim-2519", SkyrimMapCatalog.find(task.place).id);
        game.progress.location = old.name;
        assertTrue(RoadQuests.here(game, task));
        assertEquals(old.name, CanonicalRussian.mapName(task.place));
        assertTrue(LocationGalleryView.hasGallery(task.place));
        assertEquals(6, LocationGalleryView.imageCount(old.name));
        assertEquals(6, LocationGalleryView.imageCount(task.place));
        game.progress.location = "Воронья Скала";
        assertFalse(RoadQuests.here(game, task));
    }

    @Test public void bothDiamondsKeepTheirValueWeightAndOriginalSaveKey() throws Exception {
        RpgProgress state = RpgProgress.fromJson(new JSONObject().put("inventory",
            new JSONObject().put("Алмаз", 2).put("Безупречный алмаз", 3)));
        for (String[] names : RussianNameData.ITEMS) {
            ItemCatalog.Item old = ItemCatalog.find(names[0]);
            assertNotNull(old);
            if (ItemCatalog.sharedDisplayName(names[1])) {
                assertSame(old, ItemCatalog.find(ItemIdentityIndex.formOrKey(old.id)));
            } else {
                assertSame(old, ItemCatalog.find(names[1]));
            }
            assertEquals(names[1], old.name);
            assertTrue(old.matches(names[0]));
            assertTrue(old.matches(names[1]));
        }
        assertEquals(2, state.quantity("Бриллиант"));
        assertEquals(3, state.quantity("Безупречный бриллиант"));
        assertEquals(800, ItemCatalog.find("Бриллиант").value);
        assertEquals(0.1, ItemCatalog.find("Бриллиант").weight, 0.0001);
    }

    @Test public void renamedArmorAndJewelryKeepEquipmentSlotsAndEffectsAcrossSaves() throws Exception {
        JSONObject legacy = new JSONObject().put("inventory", new JSONObject()
            .put("Медная диадема с ониксом", 2).put("Кольцо чистейших смесей", 1)
            .put("Серебряный медальон Фьотли", 1).put("Броня Древних Богов", 1));
        RpgProgress progress = RpgProgress.fromJson(legacy);
        assertTrue(progress.equip("Медный обруч с ониксом"));
        assertTrue(progress.equip("Кольцо чистейшей смеси"));
        assertTrue(progress.equip("0007D678"));
        assertTrue(progress.equip("Броня Старых богов"));
        RpgProgress saved = RpgProgress.fromJson(progress.toJson());
        assertEquals("Медная диадема с ониксом", saved.equippedName("head"));
        assertEquals("Медный обруч с ониксом", saved.equippedDisplayName("head"));
        assertEquals("Серебряный кулон Фьотли", saved.equippedDisplayName("neck"));
        assertEquals("Броня Древних Богов", saved.equippedName("body"));
        assertEquals(2, saved.quantity("000166FF"));
        assertEquals(12, saved.gearAlchemy());
        ItemCatalog.Item armor = ItemCatalog.find("000EAFD0");
        assertEquals(3.0, armor.weight, 0.0);
        assertEquals(611, armor.value);
        assertEquals(28.0, armor.baseStat, 0.0);
        assertSame(armor, ItemCatalog.find("Armor of the Old Gods"));
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

    @Test public void raceLabelsLoadAndSaveWithHistoricKeysAndPortraits() throws Exception {
        for(int i=0;i<RpgContent.RACES.length;i++){
            String key=RpgContent.RACES[i];
            String[] race=RussianNameData.RACES[i];
            assertEquals(i,RpgContent.raceIndex(key));
            assertEquals(i,RpgContent.raceIndex(race[0]));
            assertEquals(i,RpgContent.raceIndex(race[2]));
            assertEquals(race[0],RpgContent.raceName(race[1]));
            assertEquals(key,RpgContent.raceKey(race[0]));
            assertEquals(CharacterIdentity.portraitPath(key,"Женский",2),
                CharacterIdentity.portraitPath(race[0],"Женский",2));
            GameEngine g=new GameEngine();
            g.start("Герой",race[0],"Воин");
            assertEquals(key,g.race);
            assertEquals(key,g.toJson().getString("race"));
            assertEquals(key,GameEngine.fromJson(g.toJson().put("race",race[0]).toString()).race);
        }
        assertEquals("Данмер",RpgContent.raceKey("Тёмный эльф"));
        assertEquals("Авторский народ",RpgContent.raceName("Авторский народ"));
    }

    @Test public void oldAndNewQuestTitlesResolveToTheSamePlayableChapter() {
        for(String[] row:RussianNameData.QUESTS){
            CanonicalRussian.QuestMatch q=new CanonicalRussian.QuestMatch(row);
            String actual=q.faction<0 ? CanonicalCampaign.title(q.stage) : FactionStories.at(q.faction,q.stage).title;
            assertEquals(actual,q.title);
            assertFalse(q.description().isEmpty());
            for(String alias:new String[]{row[1],row[2],row[3]}){
                if(alias.isEmpty())continue;
                assertTrue(q.line,CanonicalRussian.findQuests(alias).stream()
                    .anyMatch(hit->hit.stage==q.stage && hit.faction==q.faction && hit.title.equals(actual)));
            }
        }
        assertEquals(2,CanonicalRussian.findQuests("Послание для Вайтрана").size());
        assertEquals(4,CanonicalRussian.findQuests("CWSiegeObj").size());
        assertEquals("Линия крови",FactionStories.at(6,2).title);
        assertEquals("Новый орден",FactionStories.at(6,3).title);
    }

    @Test public void creatureAliasesMatchCombatLabelsAndTheSearchResult() {
        for(String[] row:RussianNameData.CREATURES){
            BestiaryCatalog.Entry e=BestiaryCatalog.find(row[0]);
            assertNotNull(e);
            assertSame(e,BestiaryCatalog.find(row[1]));
            assertEquals(row[0],e.id);
            assertEquals(row[1],e.name);
            assertTrue(BestiaryCatalog.filter(row[0],BestiaryCatalog.ALL).contains(e));
            assertTrue(BestiaryCatalog.filter(row[1],BestiaryCatalog.ALL).contains(e));
            assertEquals(e.name,BestiaryCatalog.displayName(row[0]));
        }
        assertEquals("Мой нетч",BestiaryCatalog.displayName("Мой нетч"));
    }
    @Test public void providerAliasesAreGroupedBeforeInventoryValidation() throws Exception {
        GameEngine g = new GameEngine();
        g.start("Герой", "Норд", "Воин");
        g.progress.quantity("Кольцо золотое", 1);
        JSONArray inv = new JSONArray()
            .put(new JSONObject().put("name", "Кольцо золотое").put("delta", -1))
            .put(new JSONObject().put("name", "Золотое кольцо").put("delta", 1));
        applyInventory(g, inv);
        assertEquals(1, g.progress.quantity("Кольцо золотое"));
        assertFalse(g.progress.inventory.has("Золотое кольцо"));
    }

    private static void applyInventory(GameEngine g, JSONArray inv) throws Exception {
        JSONObject reply = new JSONObject();
        reply.put("scene", "Ты проверяешь записи торговли и осматриваешь кольцо в свете свечи. ".repeat(5));
        JSONArray choices = new JSONArray();
        for (int i=0; i<10; i++) choices.put(new JSONObject().put("title", "Действие " + i)
            .put("detail", "Осмотреть следующую запись " + i).put("chance", JSONObject.NULL).put("skill", ""));
        reply.put("choices", choices).put("effects", new JSONObject().put("inventory", inv))
            .put("customChance", JSONObject.NULL).put("journal", "Проверена запись торговца");
        AiGameTurn.parseModel(reply.toString(), -2).apply(g, true);
    }

    @Test public void bookAndStaffWithSameLabelRetainIndependentSaveSlots() throws Exception {
        ItemCatalog.Item book = ItemCatalog.find("0001afba");
        ItemCatalog.Item staff = ItemCatalog.find("0002ac6f");
        assertNotSame(book, staff);
        assertEquals("Ваббаджек", book.name);
        assertEquals(book.name, staff.name);
        assertEquals("Книги", book.category);
        assertSame(book, ItemCatalog.find("Ваббаджек (книга)"));
        assertSame(staff, ItemCatalog.find("Ваббаджек"));
        assertNull(ItemCatalog.find("Wabbajack"));
        assertEquals(2, ItemCatalog.all().stream().filter(i -> i.name.equals("Ваббаджек")).count());
        assertTrue(book.matches("0001AFBA"));
        assertFalse(staff.matches("0001AFBA"));
        assertEquals("Ваббаджек [0001AFBA]", CanonicalRussian.itemReference(book.id));
        assertEquals("Ваббаджек [0002AC6F]", CanonicalRussian.itemReference(staff.id));
        RpgProgress old = RpgProgress.fromJson(new JSONObject().put("inventory",
            new JSONObject().put(book.id, 2).put(staff.id, 3)));
        assertEquals(2, old.quantity("0001afba"));
        assertEquals(3, old.quantity("0002ac6f"));
        assertTrue(old.removeItem("0001afba", 1));
        RpgProgress restored = RpgProgress.fromJson(old.toJson());
        assertEquals(1, restored.quantity(book.id));
        assertEquals(3, restored.quantity(staff.id));
    }

    @Test public void ambiguousModelEffectsLeaveInventoryUntouchedUntilIdentified() throws Exception {
        GameEngine game = new GameEngine();
        game.start("Герой", "Норд", "Воин");
        JSONArray ambiguous = new JSONArray()
            .put(new JSONObject().put("name", "0001AFBA").put("delta", 1))
            .put(new JSONObject().put("name", "Ваббаджек").put("delta", 1));
        assertThrows(org.json.JSONException.class, () -> applyInventory(game, ambiguous));
        assertEquals(0, game.progress.quantity("0001AFBA"));
        assertEquals(0, game.progress.quantity("0002AC6F"));
        applyInventory(game, new JSONArray().put(new JSONObject().put("name", "0001AFBA").put("delta", 1)));
        assertEquals(1, game.progress.quantity("Ваббаджек (книга)"));
        assertEquals(0, game.progress.quantity("Ваббаджек"));
    }

    @Test public void inventoryLooksUpQualifiedDlcIdsAndPreservesBookVolumes() {
        ItemCatalog.Item steel = ItemCatalog.find("Skyforge Steel Sword");
        assertSame(steel, ItemCatalog.find("Стальной меч Небесной кузницы"));
        assertEquals("Небесный стальной меч", steel.name);
        assertEquals("Петля", ItemCatalog.find("HEARTHFIRE_003011").name);
        assertEquals("Улучшенный арбалет", ItemCatalog.find("DAWNGUARD_00F19E").name);
        assertEquals("Сталгримовая булава", ItemCatalog.find("DRAGONBORN_01CDB7").name);
        assertNull(ItemCatalog.find("XX003011"));
        assertNull(ItemCatalog.find("DAWNGUARD_003011"));
        assertSame(ItemCatalog.find("Гвозди"), ItemCatalog.find("Nails"));
        assertSame(ItemCatalog.find("Аргонианский отчёт, т. 4"), ItemCatalog.find("0001ACE8"));
        assertEquals("Аргонианский доклад, книга 4", ItemCatalog.find("0001ACE8").name);
        assertNotSame(ItemCatalog.find("0001ACE8"), ItemCatalog.find("0001AFFC"));
        assertTrue(ItemCatalog.find("0001AF8A").matches("Песнь Пелинала, т. 1"));
        assertEquals("Том заклинаний: Грозовой разряд", ItemCatalog.find("0010F7F5").name);
        assertTrue(ItemCatalog.find("0010F7F5").effect.contains("«Грозовой разряд»"));
        assertNull(ItemCatalog.find("0010F7EE"));
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
