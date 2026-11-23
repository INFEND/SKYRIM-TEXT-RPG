package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

/** The audit covers the same expanded catalogs and numeric indices that a save uses. */
public class LocalizationCatalogTest {
    private final List<String> rows = new ArrayList<>();

    private void row(String kind, Object id, String name, String scope, String source) {
        rows.add(String.join("\t", kind, id.toString(), name, scope, source));
    }

    private void array(String kind, String[] values, String separator, String source) {
        for (int i = 0; i < values.length; i++) {
            String title = separator.isEmpty() ? values[i] : values[i].split(separator, 2)[0];
            row(kind, i, title, "candidate", source);
        }
    }

    private List<String> catalog() throws Exception {
        for (SkyrimMapCatalog.Site site : SkyrimMapCatalog.all()) {
            assertSame(site, SkyrimMapCatalog.find(site.id));
            row("map", site.id, site.name, site.english.contains("(CC)") ? "creation-club" : "candidate", "SkyrimMapPoints");
        }
        for (WorldAtlas.Place place : WorldAtlas.all()) {
            if (SkyrimMapCatalog.find(place.name) == null) row("atlas", place.name, place.name, "candidate", "WorldAtlas");
        }
        for (NpcProfiles.Profile p : NpcProfiles.originalProfiles()) {
            row(p.template ? "npc-template" : "npc", p.name, p.displayName, "candidate", "NpcProfiles");
        }
        for (NpcProfiles.Profile p : NpcProfiles.authorProfiles()) row("npc", p.name, p.name, "author", "AuthorNpcProfiles");
        for (NpcProfiles.Profile p : NpcProfiles.mentionedProfiles()) row("mention", p.name, p.name, "candidate", "LoreNpcProfiles");
        for (ItemCatalog.Item item : ItemCatalog.all()) {
            String kind = "Тома заклинаний".equals(item.category) ? "tome" : "Свитки".equals(item.category) ? "scroll"
                    : "Оружие".equals(item.category) || "Боеприпасы".equals(item.category) ? "weapon"
                    : "Броня".equals(item.category) || "Одежда".equals(item.category) || "Украшения".equals(item.category) ? "armor"
                    : "Книги".equals(item.category) || "Записки".equals(item.category) ? "book" : "item";
            String scope = item.description.toLowerCase(Locale.ROOT).contains("авторск") ? "author" : "candidate";
            row(kind, item.id, item.name, scope, "ItemCatalog");
            if (kind.equals("tome") && item.name.startsWith("Том заклинаний: ")) {
                row("spell", item.id, item.name.substring("Том заклинаний: ".length()), scope, "LibraryItemCatalog");
            }
        }
        for (BestiaryCatalog.Entry e : BestiaryCatalog.all()) {
            boolean custom = e.origin.equals(BestiaryCatalog.AUTHOR) || e.origin.equals(BestiaryCatalog.GAME);
            row("creature", e.name, e.name, custom ? "author" : "candidate", "BestiaryCatalog");
        }
        // These chapters split a quest into journeys, encounters or an epilogue in the adaptation.
        java.util.Set<Integer> adapted = java.util.Set.of(2, 4, 7, 12, 14, 16, 17, 20, 21, 25, 26, 27, 30, 32, 33);
        for (int i = 1; i <= CanonicalCampaign.COUNT; i++) {
            row("main-quest", i, CanonicalCampaign.title(i), adapted.contains(i) ? "author" : "candidate", "CanonicalCampaign");
        }
        FactionStories stories = new FactionStories();
        for (int f = 0; f < FactionCampaign.NAMES.length; f++) {
            for (int i = 0; i < stories.count(f); i++) {
                JSONArray indices = new JSONArray().put(f, i);
                FactionStories state = FactionStories.fromJson(new JSONObject().put("index", indices));
                row("faction-quest", f + "/" + i, state.chapter(f).title, "candidate", "FactionStories");
            }
        }
        array("faction", FactionCampaign.NAMES, "", "FactionCampaign");
        array("skill", RpgContent.SKILLS, "", "RpgContent");
        array("perk", AdvancedPerks.NAMES, "", "AdvancedPerks");
        for (int i = 0; i < TalentBranches.NAMES.length; i++) {
            for (int rank = 0; rank < TalentBranches.NAMES[i].length; rank++) {
                row("talent-branch", i + "/" + rank, TalentBranches.NAMES[i][rank], "author", "TalentBranches");
            }
        }
        array("race", RpgContent.RACES, "", "RpgContent");
        array("racial-power", RpgContent.POWERS, ":", "RpgContent");
        array("shout", RpgContent.SHOUTS, "\\|", "RpgContent");
        Collections.sort(rows);
        rows.add(0, "kind\tid\tname\tscope\tdeclaration");
        return rows;
    }

    public static void main(String[] args) throws Exception {
        Files.write(Path.of(args[0]), new LocalizationCatalogTest().catalog(), StandardCharsets.UTF_8);
    }

    @Test public void actualRuntimeMatchesAuditedCatalog() throws Exception {
        Path root = Path.of(System.getProperty("user.dir"));
        if (root.endsWith("app")) root = root.getParent();
        Path snapshot = root.resolve("tools/localization/catalog.tsv");
        List<String> actual = catalog();
        if ("1".equals(System.getenv("SKYRIM_RU_REFRESH_CATALOG"))) {
            Files.write(snapshot, actual, StandardCharsets.UTF_8);
        }
        assertEquals(Files.readAllLines(snapshot), actual);
    }
}
