package dev.infend.skyrimtext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The codex includes player shouts without changing the saved indices of playable abilities. */
final class ShoutLexicon {
    static final class Word {
        final String id, dragon, transcription, meaning;
        final int position;

        Word(String[] row) {
            position = Integer.parseInt(row[1]);
            id = row[2];
            dragon = row[3];
            transcription = row[4];
            meaning = row[5];
        }

        String label() {
            return transcription.isEmpty() ? dragon : transcription;
        }

        String line() {
            String text = transcription.isEmpty() ? dragon : transcription + " (" + dragon + ")";
            return meaning.isEmpty() ? text : text + " — " + meaning;
        }

        boolean matches(String query) {
            return id.equalsIgnoreCase(query) || includes(dragon, query)
                || includes(transcription, query) || includes(meaning, query);
        }
    }

    static final class Entry {
        final String id, name, english;
        final List<Word> words;

        Entry(String[] row, List<Word> orderedWords) {
            id = row[0];
            name = row[1];
            english = row[2];
            words = Collections.unmodifiableList(orderedWords);
        }

        String detail() {
            List<String> lines = new ArrayList<>();
            for (Word word : words) lines.add(word.line());
            return String.join("\n", lines);
        }
    }

    private static final List<Entry> entries;
    private static final Map<String, Entry> exact = new HashMap<>();
    private static final Pattern reference = Pattern.compile(
        "(?iu)(?:\\bкрик\\s+«([^»\\r\\n]{1,64})»|\\[SHOUT:([a-z0-9_]{8,20})\\])");

    static {
        Map<String, List<Word>> groups = new HashMap<>();
        for (String[] row : RussianNameData.WORDS)
            groups.computeIfAbsent(row[0], key -> new ArrayList<>()).add(new Word(row));
        List<Entry> all = new ArrayList<>();
        for (String[] row : RussianNameData.SHOUTS) {
            List<Word> words = groups.get(row[0]);
            words.sort(Comparator.comparingInt(w -> w.position));
            Entry entry = new Entry(row, words);
            all.add(entry);
            exact.put(normalize(entry.id), entry);
            exact.put(normalize(entry.name), entry);
            exact.put(normalize(entry.english), entry);
        }
        all.sort(Comparator.comparing(e -> normalize(e.name)));
        entries = Collections.unmodifiableList(all);
    }

    private ShoutLexicon() {}

    static List<Entry> all() { return entries; }

    static Entry find(String key) { return exact.get(normalize(key)); }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    private static boolean includes(String label, String normalized) {
        return normalize(label).contains(normalized);
    }

    static List<Entry> search(String text) {
        String query = normalize(text);
        if (query.isEmpty()) return entries;
        List<Entry> matches = new ArrayList<>();
        for (Entry e : entries) {
            if (includes(e.name, query) || includes(e.english, query) || e.id.equalsIgnoreCase(query)
                    || e.words.stream().anyMatch(word -> word.matches(query))) matches.add(e);
        }
        return matches;
    }

    static String promptReference(String input) {
        if (input == null) return "";
        Matcher match = reference.matcher(input);
        while (match.find()) {
            Entry entry = find(match.group(1) != null ? match.group(1) : match.group(2));
            if (entry != null) return entry.name + " — " + entry.detail().replace('\n', ' ');
        }
        return "";
    }
}
