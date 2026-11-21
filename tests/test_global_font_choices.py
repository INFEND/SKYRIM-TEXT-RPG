import json
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class GlobalFontChoices(unittest.TestCase):
    def test_all_families_are_pinned_with_licenses(self):
        spec=json.loads((ROOT/"app/font-choices.json").read_text())
        self.assertEqual(len(spec["families"]),11)
        self.assertRegex(spec["revision"],r"^[0-9a-f]{40}$")
        font=(JAVA/"SovngardeTypeface.java").read_text()
        for family in spec["families"]:
            self.assertIn('"'+family["id"]+'"',font)
            paths={entry["asset"].split("/")[-1] for entry in family["files"]}
            self.assertTrue({"regular.ttf","OFL.txt"}<=paths)
            for entry in family["files"]:
                self.assertRegex(entry["gitSha"],r"^[0-9a-f]{40}$")
                self.assertGreater(entry["size"],0)
        self.assertIn("data[\"files\"].extend(font_records())",(ROOT/"scripts/build_resource_catalog.py").read_text())
        self.assertIn("ResourceStore.localFile(context,path)",font)
    def test_settings_and_global_surfaces(self):
        main=(JAVA/"MainActivity.java").read_text()
        font=(JAVA/"SovngardeTypeface.java").read_text()
        self.assertIn("chooseGameFont()",main)
        self.assertIn("SovngardeTypeface.select(this,id);picker.dismiss();render();",main)
        self.assertIn('"font:"+id',main)
        self.assertIn("SovngardeTypeface.preview(sample,id",main)
        self.assertNotIn("NATIVE_ITEM_FONT",main)
        self.assertIn("putString(PREF,selected)",font)
        self.assertIn("watchTree(root)",main)
        self.assertIn("watchTree(window.getDecorView())",(JAVA/"RuneDialogBuilder.java").read_text())
        self.assertIn("watchDialog(dialog)",(JAVA/"OtaProgressDialog.java").read_text())
        self.assertNotIn("setTypeface(android.graphics.Typeface.DEFAULT)",(JAVA/"SkyrimTerrainMapView.java").read_text())
