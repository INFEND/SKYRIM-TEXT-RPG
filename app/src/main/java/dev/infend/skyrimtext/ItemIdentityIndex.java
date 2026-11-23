package dev.infend.skyrimtext;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Verified item FormIDs are qualified by their source plugin, independent of load order. */
final class ItemIdentityIndex {
    private static final Map<String, String> lookup = new HashMap<>();
    private static final Map<String, String[]> byKey = new HashMap<>();

    static {
        for (String[][] part : RussianItemIdentityData.PARTS) {
            for (String[] row : part) {
                byKey.put(row[0], row);
                index(row[1], row[0]);
                index(row[2], row[0]);
            }
        }
    }

    private ItemIdentityIndex() {}

    private static void index(String alias, String key) {
        if (!alias.isEmpty()) lookup.merge(alias.toLowerCase(Locale.ROOT), key,
            (previous, next) -> previous.equals(next) ? previous : "");
    }

    static String resolve(String input) {
        return input == null ? null : lookup.get(input.trim().toLowerCase(Locale.ROOT));
    }

    static boolean matches(String key, String lowerCaseQuery) {
        String[] row = byKey.get(key);
        return row != null && (row[1].equalsIgnoreCase(lowerCaseQuery)
            || row[2].toLowerCase(Locale.ROOT).contains(lowerCaseQuery));
    }

    static String formOrKey(String key) {
        String[] row = byKey.get(key);
        return row == null ? key : row[1];
    }
}
