package dev.infend.skyrimtext;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Display labels use the project's original inventory keys, NPC keys and numeric indices. */
final class CanonicalRussian {
    private static final Map<String, String> NPC_IDS = new HashMap<>();
    private static final Map<String, String> NPC_BY_ID = new HashMap<>();

    static {
        for (String[] row : RussianNameData.NPC) {
            if (!row[2].isEmpty()) {
                NPC_IDS.put(row[0], row[2]);
                NPC_BY_ID.put(row[2], row[1]);
            }
        }
    }

    private CanonicalRussian() {}

    static String npcName(String key) {
        return RussianNameData.npcName(key);
    }

    static String mapName(String key) {
        SkyrimMapCatalog.Site site = SkyrimMapCatalog.find(key);
        return site == null ? key : site.name;
    }

    static String itemName(String key) {
        ItemCatalog.Item item = ItemCatalog.find(key);
        return item == null ? key : item.name;
    }

    static String itemReference(String key) {
        ItemCatalog.Item item = ItemCatalog.find(key);
        if (item == null) return key;
        if (!ItemCatalog.sharedDisplayName(item.name)) return item.name;
        return item.name + " [" + ItemIdentityIndex.formOrKey(item.id) + "]";
    }

    // An explicit identifier in a dialogue heading is nominative. Prose, inflected
    // names, free-form NPCs and user-created items have no identifier to substitute.
    static String speakerLine(String raw) {
        String line = raw.trim();
        boolean bold = line.startsWith("**") && line.endsWith("**") && line.length() > 4;
        String heading = bold ? line.substring(2, line.length() - 2) : line;
        if (heading.length() < 8 || heading.length() > 40 || !heading.startsWith("[NPC:") || !heading.endsWith("]:")) return raw;
        String name = NPC_BY_ID.get(heading.substring(5, heading.length() - 2).toUpperCase(Locale.ROOT));
        if (name == null) return raw;
        return bold ? "**" + name + ":**" : name + ":";
    }

    static java.util.List<String> unverifiedSpeakers(String scene) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String title : NpcProfiles.speakers(scene, "")) {
            NpcProfiles.Profile actor = NpcProfiles.findOriginal(title);
            if (NpcProfiles.findAuthor(title) == null && (actor == null || !NPC_IDS.containsKey(actor.name))) {
                result.add(title);
                if (result.size() == 12) break;
            }
        }
        return java.util.Collections.unmodifiableList(result);
    }

    static String speakerContext(GameEngine game) {
        StringBuilder out = new StringBuilder();
        if ("CANON".equals(game.mode)) {
            String speaker = CanonicalCampaign.speakerKey(game.canonicalStage);
            NpcProfiles.Profile profile = NpcProfiles.findOriginal(speaker);
            String id = profile == null ? null : NPC_IDS.get(profile.name);
            if (id != null) out.append(profile.displayName).append(" [NPC:").append(id).append(']');
        }
        if ("COMBAT".equals(game.mode) && game.enemyHp > 0 &&
                !NpcNameMatcher.key(game.enemyName).equals(NpcNameMatcher.key(game.name))) {
            BestiaryCatalog.Entry creature = BestiaryCatalog.find(game.enemyName);
            NpcProfiles.Profile npc = creature == null ? null : NpcProfiles.findOriginal(creature.id);
            String id = npc == null ? null : NPC_IDS.get(npc.name);
            if (id != null && creature.id.equals(npc.name)) {
                out.append(npc.displayName).append(" [NPC:").append(id).append(']');
            }
        }
        if (game.sceneState == null) return out.toString();
        org.json.JSONArray facts = game.sceneState.npcFacts();
        for (int i = 0; i < facts.length(); i++) {
            org.json.JSONObject fact = facts.optJSONObject(i);
            if (fact == null) continue;
            String id = fact.optString("id").toUpperCase(Locale.ROOT);
            String title = fact.optString("title");
            if (NpcNameMatcher.key(title).equals(NpcNameMatcher.key(game.name))) continue;
            NpcProfiles.Profile npc = NpcProfiles.findOriginal(title);
            String actorId = npc == null ? null : NPC_IDS.get(npc.name);
            if (actorId == null) continue;
            if (!id.equals(actorId)) {
                boolean legacyMatch = false;
                for (String[] pair : RussianNameData.ACTOR_REFERENCES) {
                    if (pair[0].equals(id) && pair[1].equals(actorId)) legacyMatch = true;
                }
                if (!legacyMatch) continue;
                id = actorId;
            }
            if (out.indexOf("[NPC:" + id + "]") >= 0) continue;
            if (out.length() > 0) out.append(", ");
            out.append(npc.displayName).append(" [NPC:").append(id).append(']');
            if (out.length() >= 180) break;
        }
        return out.toString();
    }

    static String questTitle(int faction, int stage, String fallback) {
        String entry = faction + "/" + stage;
        for (String[] row : RussianNameData.QUESTS) if (row[0].equals(entry)) return row[1];
        return fallback;
    }

    static final class QuestMatch {
        final int faction, stage;
        final String title, line;
        QuestMatch(String[] row) {
            int slash=row[0].indexOf('/');
            faction=slash<0 ? -1 : Integer.parseInt(row[0].substring(0,slash));
            stage=Integer.parseInt(row[0].substring(slash+1));
            title=row[1];
            line=(faction<0 ? "Главный сюжет" : FactionCampaign.NAMES[faction])+" | "+title;
        }
        String description() {
            if(faction<0)return CanonicalCampaign.goal(stage);
            FactionStories.Chapter chapter=FactionStories.at(faction,stage);
            return chapter.story+"\n\nМесто: "+CanonicalRussian.mapName(chapter.place);
        }
    }

    static java.util.List<QuestMatch> findQuests(String text) {
        String query=SkyrimMapCatalog.normalize(text);
        java.util.List<QuestMatch> matches=new java.util.ArrayList<>();
        if(query.isEmpty())return matches;
        for(String[] row:RussianNameData.QUESTS){
            for(int i=1;i<row.length;i++){
                if(SkyrimMapCatalog.normalize(row[i]).contains(query)){
                    matches.add(new QuestMatch(row));
                    break;
                }
            }
        }
        return matches;
    }

    static java.util.List<Integer> findPerks(String label) {
        java.util.List<Integer> matches = new java.util.ArrayList<>();
        for (int i = 0; i < AdvancedPerks.NAMES.length; i++) {
            if (matchesPerk(i, label)) matches.add(i);
        }
        return matches;
    }

    static boolean matchesPerk(int index, String label) {
        String key = SkyrimMapCatalog.normalize(label);
        if (key.isEmpty()) return false;
        for (String[] row : RussianNameData.PERKS) {
            if (Integer.parseInt(row[0]) == index) {
                return key.equals(SkyrimMapCatalog.normalize(row[1])) ||
                       key.equals(SkyrimMapCatalog.normalize(row[2])) || key.equalsIgnoreCase(row[3]);
            }
        }
        return false;
    }
}
