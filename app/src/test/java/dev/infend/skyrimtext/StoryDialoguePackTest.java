package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.*;

public class StoryDialoguePackTest {
    private static final String SCENES="\"scenes\":[{\"id\":1,\"title\":\"Глава\"}]";
    private static String pack(String segments){return "{"+SCENES+",\"segments\":["+segments+"]}";}
    @Test public void readsOrderedSpeakersAndMarksUncertainty() throws Exception {
        String json=pack("{\"id\":1,\"scene_id\":1,\"speaker\":\"А\",\"reading_text\":\"Первое\"},"
            +"{\"id\":2,\"scene_id\":1,\"requires_manual_review\":true,\"utterances\":[{\"speaker\":\"Б\",\"text\":\"Второе\"},{\"speaker\":\"В\",\"text\":\"Третье\"}]},"
            +"{\"id\":3,\"scene_id\":1,\"exclude_from_reading\":true}");
        StoryDialoguePack p=StoryDialoguePack.parse(json);
        assertEquals(2,p.segments);assertEquals(1,p.uncertainSegments);
        assertEquals(3,p.chapter(1).lines().size());
        assertEquals("В",p.chapter(1).lines().get(2).speaker);
        assertTrue(p.chapter(1).lines().get(1).uncertain);
        assertEquals(0,p.chapter(1).page(999));
    }
    @Test(expected=org.json.JSONException.class) public void rejectsUnknownChapter() throws Exception {
        StoryDialoguePack.parse(pack("{\"id\":1,\"scene_id\":2,\"speaker\":\"А\",\"reading_text\":\"Текст\"}"));
    }
    @Test(expected=org.json.JSONException.class) public void rejectsDuplicateIds() throws Exception {
        String s="{\"id\":1,\"scene_id\":1,\"speaker\":\"А\",\"reading_text\":\"Текст\"}";
        StoryDialoguePack.parse(pack(s+","+s));
    }
    @Test(expected=org.json.JSONException.class) public void rejectsMissingText() throws Exception {
        StoryDialoguePack.parse(pack("{\"id\":1,\"scene_id\":1,\"speaker\":\"А\"}"));
    }
}
