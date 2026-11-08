"""Regression protection: spoken dialogue's side stripe must not spill into narration."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SOURCE=(ROOT/"app/src/main/java/dev/infend/skyrimtext/SceneTypography.java")

class DialogueQuoteRangeTests(unittest.TestCase):
    def test_quotation_spans_are_registered_after_text_is_complete(self):
        content=SOURCE.read_text(encoding="utf-8")
        method=content[content.index("public static SpannableStringBuilder render("):]
        self.assertIn("quoteRanges.add(new int[]{start,result.length()});",method)
        self.assertIn("for(int[] range:quoteRanges)",method)
        self.assertIn("new QuoteSpan(accent)",method)
        self.assertLess(method.index('if(i<lines.length-1)result.append'),method.index("for(int[] range:quoteRanges)"))
        self.assertNotIn("result.setSpan(new QuoteSpan(accent),start,result.length()",method)

    def test_spoken_markup_is_only_on_quote_paragraph(self):
        content=SOURCE.read_text(encoding="utf-8")
        self.assertIn('boolean spoken=line.startsWith(">")',content)
        self.assertIn('if(spoken)line=line.substring(1).trim();',content)
        self.assertIn('Spanned.SPAN_PARAGRAPH',content)
        self.assertIn('Spanned.SPAN_EXCLUSIVE_EXCLUSIVE',content)
        self.assertIn('Typeface.ITALIC',content)

if __name__=="__main__":
    unittest.main()
