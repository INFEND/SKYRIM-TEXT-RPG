package dev.infend.skyrimtext;

import java.util.regex.Pattern;

/** Display-safe formatting of generated prose. Game-state metadata remains in the prompt. */
final class AiNarrativeFormat {
    private static final Pattern LOCATION_TIME = Pattern.compile(
        "(?iu)^(?:(?:место|локация)\\s*:\\s*)?" +
        "(?:[\\p{L}\\p{N}«»'() .-]{2,80}\\s*[,|;·—–-]\\s*)?" +
        "(?:день|сутки)\\s*:?\\s*\\d{1,4}\\s*[,|;·—–-]\\s*" +
        "(?:(?:час|время)\\s*:?\\s*)?\\d{1,2}(?::\\d{2})?\\s*[.!]?$");
    private static final Pattern LABEL = Pattern.compile(
        "(?iu)^(?:место|локация|время|день|час)\\s*:\\s*[^.!?]{1,80}$");

    private AiNarrativeFormat() {}

    /** Remove blank paragraphs and standalone place/day/time headings, not narrative facts. */
    static String compact(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] lines = raw.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder cleaned = new StringBuilder(raw.length());
        for (String line : lines) {
            String current = line.trim();
            if (current.isEmpty()) continue;
            String header = current.replaceAll("[*#_]", "").trim()
                .replaceAll("^[>\\p{So}\\s]+", "").trim();
            if (LOCATION_TIME.matcher(header).matches() || LABEL.matcher(header).matches()) continue;
            if (cleaned.length() > 0) cleaned.append('\n');
            cleaned.append(current);
        }
        return cleaned.toString();
    }
}
