"""No side bars; speaker and dialogue must remain on their own lines."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SOURCE=ROOT/"app/src/main/java/dev/infend/skyrimtext/SceneTypography.java"
PROSE=ROOT/"app/src/main/java/dev/infend/skyrimtext/SceneNarrative.java"

class DialogueQuoteRangeTests(unittest.TestCase):
    def test_no_quote_lines_or_paragraph_spans(self):
        source=SOURCE.read_text(encoding="utf-8")
        self.assertNotIn("QuoteSpan",source.split("public final class SceneTypography")[0])
        self.assertNotIn("new QuoteSpan(",source)
        self.assertNotIn("Spanned.SPAN_PARAGRAPH",source)
        self.assertIn('if(quoted)line=line.substring(1).trim();',source)
        self.assertIn('plain.startsWith("— ")',source)
        self.assertIn("boolean speaker=!spoken",source)

    def test_dialogue_linebreak_and_italic_narration(self):
        source=SOURCE.read_text(encoding="utf-8")
        self.assertIn("String[] lines=normalize(raw).split",source)
        self.assertIn('text.replaceAll("([.!?…])',source)
        self.assertIn("new StyleSpan(Typeface.ITALIC)",source)
        self.assertIn("new StyleSpan(Typeface.BOLD)",source)
        self.assertIn("return result;",source)
        prose=PROSE.read_text(encoding="utf-8")
        self.assertIn('return "**"+person+":**\\n**— "+statement+"**";',prose)
        self.assertNotIn('":**\\n>**— "',prose)

if __name__=="__main__":unittest.main()
