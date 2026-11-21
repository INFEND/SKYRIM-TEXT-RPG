from pathlib import Path
import unittest

UI=(Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text(encoding="utf-8")

class JournalLayout(unittest.TestCase):
    def test_switcher_has_no_asymmetric_vertical_child_margins(self):
        journal=UI.split("private void renderJournal(){",1)[1].split("private void foldSettingsSection",1)[0]
        switcher=journal.split("if(journalRelations)",1)[0]
        self.assertIn("switcher.setGravity(Gravity.TOP)",switcher)
        self.assertIn("switcher.setBaselineAligned(false)",switcher)
        self.assertIn("new LinearLayout.LayoutParams(0,-1,1)",switcher)
        self.assertIn("space(content,8)",switcher)
        self.assertNotIn("dp(3),0,dp(3),dp(8)",switcher)

    def test_chronicle_is_collapsed_and_last(self):
        self.assertIn("private boolean chronicleExpanded=false;",UI)
        chronicle=UI.split("private void renderChronicle(){",1)[1].split("private void chronicleDetails",1)[0]
        self.assertIn("if(!visual(\"chronicle\"))return;",chronicle)
        self.assertIn("if(!chronicleExpanded)return;",chronicle)
        self.assertLess(chronicle.index("if(!chronicleExpanded)return;"),chronicle.index("HorizontalScrollView strip"))
        self.assertIn("chronicleExpanded=!chronicleExpanded;render();",chronicle)
        journal=UI.split("private void renderJournal(){",1)[1].split("private void foldSettingsSection",1)[0]
        self.assertTrue(journal.rstrip().endswith("renderChronicle();\n    }"))
        self.assertEqual(journal.count("renderChronicle();"),1)
