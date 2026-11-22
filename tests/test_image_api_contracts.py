"""Regression contracts for SKYRIM's opt-in Images API and local image imports."""
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"
EDITOR = (JAVA / "WorkImageEditor.java").read_text(encoding="utf-8")
CLIENT = (JAVA / "ImageApiClient.java").read_text(encoding="utf-8")
MAIN = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
MANIFEST = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

class ImagesIntegration(unittest.TestCase):
    def test_api_generation_and_edits_are_explicit(self):
        self.assertIn('ROOT + (editing ? "edits" : "generations")', CLIENT)
        self.assertIn('https://api.openai.com/v1/images/', CLIENT)
        self.assertIn('AiKeyVault.read(app)', CLIENT)
        self.assertIn('requestTarget(kind, id, name, description', EDITOR)
        self.assertIn('setPositiveButton("Запустить платный запрос"', EDITOR)
        self.assertIn('setPositiveButton("Заменить основное"', EDITOR)
        self.assertIn('setNeutralButton("Добавить в галерею"', EDITOR)
        self.assertIn('setNegativeButton("Не сохранять"', EDITOR)

    def test_image_generation_uses_only_explicit_images_api_credentials(self):
        self.assertNotIn('chatGptOAuth', CLIENT)
        self.assertNotIn('ChatGptOAuth', CLIENT)
        self.assertNotIn('getToken(', CLIENT)
        self.assertIn('AiKeyVault.read(app)', CLIENT)
        self.assertIn('Intent.ACTION_OPEN_DOCUMENT', EDITOR)
        self.assertNotIn('launchChatGpt(host', EDITOR)
        self.assertNotIn('Intent.ACTION_SEND', EDITOR)

    def test_api_results_are_bounded_and_saved_atomically_after_review(self):
        self.assertIn('MAX_IMAGE = 24 * 1024 * 1024', CLIENT)
        self.assertIn('MAX_RESPONSE = 96 * 1024 * 1024', CLIENT)
        self.assertIn('AtomicBoolean RUNNING', CLIENT)
        self.assertIn('setInstanceFollowRedirects(false)', CLIENT)
        self.assertIn('readLimitedResponse(conn.getInputStream()', CLIENT)
        self.assertIn('output.size() + n > limit', CLIENT)
        self.assertIn('output_format", "webp"', CLIENT)
        self.assertIn('new FileOutputStream(dest)', EDITOR)
        self.assertIn('writeMetadata(host, kind, id, meta)', EDITOR)
        self.assertIn('for (File file : newFiles) file.delete()', EDITOR)
        self.assertIn('openai-images-api:', EDITOR)

    def test_existing_location_npc_item_hooks_are_preserved(self):
        self.assertIn('WorkImageEditor.primaryView(this,"location"', MAIN)
        self.assertIn('WorkImageEditor.primaryView(this,"npc"', MAIN)
        self.assertIn('WorkImageEditor.primaryView(this,"item"', MAIN)
        self.assertIn('WorkImageEditor.onActivityResult(this,requestCode', MAIN)
        self.assertIn('public static final int REQUEST_API_EDIT', EDITOR)
        self.assertNotIn('public static final int REQUEST_WORK_SHARE', EDITOR)

    def test_images_are_imported_locally_without_obsolete_share_provider(self):
        self.assertNotIn('android:name=".ImageShareProvider"', MANIFEST)
        self.assertFalse((JAVA / "ImageShareProvider.java").exists())
        self.assertIn('Intent.ACTION_OPEN_DOCUMENT', EDITOR)
        self.assertIn('getFilesDir()', EDITOR)
        self.assertIn('writeMetadata(host, kind, id, meta)', EDITOR)

if __name__ == "__main__":
    unittest.main()
