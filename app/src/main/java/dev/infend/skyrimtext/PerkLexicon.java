package dev.infend.skyrimtext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reference information about original perk ranks, independent of purchased RPG talents. */
final class PerkLexicon {
    static final class Entry {
        final int tree;
        final String name, english;
        final List<String> forms;
        private final List<String> aliases = new ArrayList<>();

        Entry(String[] row, List<String> ranks) {
            tree = Integer.parseInt(row[0]);
            name = row[3];
            english = row[4];
            forms = Collections.unmodifiableList(ranks);
            aliases.add(name);
            aliases.add(english);
        }

        String treeName() {
            if (tree < RpgContent.SKILLS.length) return RpgContent.SKILLS[tree];
            return tree == 18 ? "Вервольф" : "Вампир-лорд";
        }

        String label() { return name + " | " + treeName(); }

        String details() {
            StringBuilder out = new StringBuilder(treeName());
            for (int i = 0; i < forms.size(); i++) {
                out.append("\nРанг ").append(i + 1).append(" | ").append(forms.get(i));
            }
            return out.toString();
        }

        private int matchQuality(String query) {
            boolean partial = false;
            for (String label : aliases) {
                String value = key(label);
                if (value.equals(query)) return 2;
                if (value.contains(query)) partial = true;
            }
            return partial ? 1 : 0;
        }
    }

    private static final List<Entry> entries;
    private static final Map<String, Entry> byForm = new HashMap<>();
    private static final Pattern reference = Pattern.compile(
        "(?iu)\\[PERK:([A-Z0-9_:]+)\\]|(?<![\\p{L}])(?:перк|способность)\\s+«([^»\\n]{1,70})»");

    static {
        Map<String, List<String[]>> families = new LinkedHashMap<>();
        for (String[] row : RussianPerkRankData.ROWS) {
            families.computeIfAbsent(row[0] + "/" + row[4], unused -> new ArrayList<>()).add(row);
        }
        List<Entry> sorted = new ArrayList<>();
        for (List<String[]> rows : families.values()) {
            rows.sort(Comparator.comparingInt(row -> Integer.parseInt(row[1])));
            List<String> ranks = new ArrayList<>();
            for (String[] row : rows) {
                if (Integer.parseInt(row[1]) != ranks.size() + 1 || !row[3].equals(rows.get(0)[3])) {
                    throw new IllegalStateException("Несогласованные ранги способности " + row[2]);
                }
                ranks.add(row[2]);
            }
            Entry entry = new Entry(rows.get(0), ranks);
            sorted.add(entry);
            for (String id : ranks) {
                if (byForm.put(key(id), entry) != null) throw new IllegalStateException("Повтор FormID " + id);
            }
        }
        for (String[] old : RussianNameData.PERKS) {
            Entry entry = byForm.get(key(old[3]));
            if (entry != null && !old[2].isEmpty()) entry.aliases.add(old[2]);
        }
        sorted.sort(Comparator.comparingInt((Entry e) -> e.tree).thenComparing(e -> key(e.name)));
        entries = Collections.unmodifiableList(sorted);
    }

    private PerkLexicon() {}

    static List<Entry> all() { return entries; }

    private static String key(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    private static String formKey(String text) { return key(text).replace("dawnguard:", "dawnguard_"); }

    static List<Entry> search(String text, int tree) {
        String query = key(text);
        Entry form = byForm.get(formKey(text));
        if (form != null) {
            return tree < 0 || tree == form.tree ? Collections.singletonList(form) : Collections.emptyList();
        }
        List<Entry> exact = new ArrayList<>(), partial = new ArrayList<>();
        for (Entry entry : entries) {
            if (tree >= 0 && tree != entry.tree) continue;
            int quality = entry.matchQuality(query);
            if (quality == 2) exact.add(entry);
            else if (quality == 1) partial.add(entry);
        }
        return exact.isEmpty() ? partial : exact;
    }

    static String promptReference(String text) {
        if (text == null) return "";
        Matcher match = reference.matcher(text);
        while (match.find()) {
            if (match.group(1) != null) {
                String form = formKey(match.group(1));
                Entry entry = byForm.get(form);
                if (entry == null) continue;
                int rank = entry.forms.indexOf(form.toUpperCase(Locale.ROOT)) + 1;
                return entry.label() + " | ранг " + rank;
            }
            String name = key(match.group(2));
            Entry known = null;
            for (Entry entry : entries) {
                if (!key(entry.name).equals(name)) continue;
                if (known != null) { known = null; break; }
                known = entry;
            }
            if (known != null) return known.label();
        }
        return "";
    }
}
