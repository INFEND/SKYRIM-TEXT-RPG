import pathlib, unittest
JAVA=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class ResourceManagerVisualContract(unittest.TestCase):
    def test_no_platform_status_icons_or_split_window(self):
        s=(JAVA/"ResourceManagerDialog.java").read_text()
        self.assertNotIn("android.R.drawable",s)
        self.assertIn("new Dialog(activity)",s)
        self.assertIn("root.setClipToOutline(true)",s)
        self.assertIn("new ResourceGlyph(glyph",s)
        self.assertIn("new ResourceMeter(activity,accent)",s)
        self.assertIn("currentPartialBytes",s)
        self.assertIn("c.percent.setText",s)
    def test_round_progress_and_outline_check(self):
        meter=(JAVA/"ResourceMeter.java").read_text()
        glyph=(JAVA/"ResourceGlyph.java").read_text()
        self.assertIn("drawRoundRect",meter)
        self.assertIn("Math.min(1f,value)",meter)
        self.assertIn('case "check":line',glyph)
        self.assertIn("Paint.ANTI_ALIAS_FLAG",glyph)

    def test_resource_file_counts_use_ready_entries_not_partial_bytes(self):
        s=(JAVA/"ResourceManagerDialog.java").read_text()
        for token in ('return "["+installed+"/"+server+"]"', 'count.serverFiles++', 'if(ready)count.installedFiles++', 'filePair(allInstalled,allServer)', 'filePair(n.installedFiles,n.serverFiles)', 'amount+=" "+filePair(installedFiles,serverFiles)'):
            self.assertIn(token,s)

    def test_engine_install_status_is_not_duplicated(self):
        s=(JAVA/"ResourceManagerDialog.java").read_text()
        self.assertNotIn('amount+=" · Установить"',s)
        self.assertIn('installPending?"install":"download"',s)
        self.assertIn("Скачан · требуется установка",s)
