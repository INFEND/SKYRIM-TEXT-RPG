"""Static regression contracts for the manual ChatGPT Work artwork editor."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/dev/infend/skyrimtext"
UI = (APP / "MainActivity.java").read_text(encoding="utf-8")
EDITOR = (APP / "WorkImageEditor.java").read_text(encoding="utf-8")

class WorkImageEditorContracts(unittest.TestCase):
    def test_editor_uses_explicit_api_and_safe_local_import(self):
        self.assertIn('ClipboardManager', EDITOR)
        self.assertIn('Intent.ACTION_OPEN_DOCUMENT', EDITOR)
        self.assertIn('REQUEST_API_EDIT', EDITOR)
        self.assertNotIn('getLaunchIntentForPackage("com.openai.chatgpt")', EDITOR)
        self.assertNotIn('Intent.ACTION_SEND', EDITOR)

    def test_editable_prompt_and_location_city_series(self):
        self.assertIn('editor.addTextChangedListener', EDITOR)
        self.assertIn('editor.setText(prompt(kind, name, facts, requested[0]))', EDITOR)
        self.assertIn('for (int n : new int[]{1, 3, 5})', EDITOR)

    def test_safe_local_webp_import_with_gallery_and_restore(self):
        self.assertIn('Bitmap.CompressFormat.WEBP', EDITOR)
        self.assertIn('getFilesDir()', EDITOR)
        self.assertIn('makePrimary', EDITOR)
        self.assertIn('Вернуть встроенное изображение', EDITOR)
        self.assertIn('setPositiveButton("Сделать основным"', EDITOR)
        self.assertIn('setNeutralButton("Удалить"', EDITOR)

    def test_npc_item_city_and_location_wired(self):
        self.assertIn('WorkImageEditor.onActivityResult', UI)
        self.assertIn('WorkImageEditor.open(this,"npc"', UI)
        self.assertIn('WorkImageEditor.open(this,"item"', UI)
        self.assertIn('WorkImageEditor.open(this,"location"', UI)
        self.assertIn('WorkImageEditor.primaryView(this,"npc"', UI)
        self.assertIn('WorkImageEditor.primaryView(this,"item"', UI)

if __name__ == "__main__":
    unittest.main()
