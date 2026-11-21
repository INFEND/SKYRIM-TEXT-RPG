package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;

public class AiScenePreviewTest {
    @Test public void showsFirstCharacterAndProgress(){
        assertEquals("",AiScenePreview.read("{\"scene\":\""));
        assertEquals("Д",AiScenePreview.read("{\"scene\":\"Д"));
        assertEquals("Дракон",AiScenePreview.read("{\"scene\":\"Дракон"));
        assertEquals("Дракон",AiScenePreview.read("{\"scene\":\"Дракон\",\"choices\":["));
    }
    @Test public void escapesAndIncompleteUnicodeAreStable(){
        assertEquals("А",AiScenePreview.read("{\"scene\":\"А\\u04"));
        assertEquals("Аб",AiScenePreview.read("{\"scene\":\"А\\u0431"));
        assertEquals("А",AiScenePreview.read("{\"scene\":\"А\\"));
        assertEquals("А\nБ\"/\\",AiScenePreview.read("{\"scene\":\"А\\nБ\\\"\\/\\\\"));
        assertEquals("А",AiScenePreview.read("{\"scene\":\"А\\uD83D"));
        assertEquals("А😀",AiScenePreview.read("{\"scene\":\"А\\uD83D\\uDE00"));
    }
    @Test public void qwenAssistantPrefillStartsSceneImmediately(){
        String open="<|im_start|>assistant\n<think>\n";
        String direct=AiScenePreview.directPrompt(open);
        assertTrue(direct.endsWith("</think>\n\n"));
        String prefix=AiScenePreview.scenePrefix(direct,true);
        assertEquals("{\"scene\":\"",prefix);
        assertEquals("С",AiScenePreview.read(prefix+"С"));
        assertEquals("Снег",AiScenePreview.read(prefix+"Снег"));
        assertEquals("",AiScenePreview.scenePrefix(direct,false));
        assertEquals("",AiScenePreview.scenePrefix("[INST]x[/INST]",true));
        assertEquals(direct,AiScenePreview.directPrompt(direct));
        assertEquals("Текст",AiScenePreview.read("Ответ: \"JSON\"\n"+prefix+"Текст"));
    }
    @Test public void neverShowsNestedMetadataOrThinking(){
        assertEquals("",AiScenePreview.read("{\"effects\":{\"scene\":\"секрет\"}}"));
        assertEquals("",AiScenePreview.read("<think>{\"scene\":\"мысль"));
        assertEquals("Текст",AiScenePreview.read("<think>мысль</think>{\"scene\":\"Текст"));
        assertEquals("Текст",AiScenePreview.read("{\"journal\":\"\\\"scene\\\"\",\"scene\":\"Текст"));
    }
}
