"""Regression contracts for SKYRIM's opt-in OpenAI Images API + manual Work flow."""
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"
EDITOR = (JAVA / "WorkImageEditor.java").read_text(encoding="utf-8")
CLIENT = (JAVA / "ImageApiClient.java").read_text(encoding="utf-8")
PROVIDER = (JAVA / "ImageShareProvider.java").read_text(encoding="utf-8")
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

    def test_work_session_does_not_become_api_credential(self):
        self.assertNotIn('chatGptOAuth', CLIENT)
        self.assertNotIn('ChatGptOAuth', CLIENT)
        self.assertNotIn('getToken(', CLIENT)
        self.assertIn('launchChatGpt(host', EDITOR)
        self.assertIn('Intent.ACTION_SEND', EDITOR)
        self.assertIn('FLAG_GRANT_READ_URI_PERMISSION', EDITOR)
        self.assertIn('EXTRA_STREAM', EDITOR)

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
        self.assertIn('public static final int REQUEST_WORK_SHARE', EDITOR)

    def test_shared_artwork_is_private_and_read_only(self):
        self.assertIn('android:exported="false"', MANIFEST)
        self.assertIn('android:grantUriPermissions="true"', MANIFEST)
        self.assertIn('android:name=".ImageShareProvider"', MANIFEST)
        self.assertIn('ParcelFileDescriptor.MODE_READ_ONLY', PROVIDER)
        self.assertIn('if (!"r".equals(mode))', PROVIDER)
        self.assertIn('getPathSegments().size() != 2', PROVIDER)
        self.assertIn('matches("[0-9a-f]{64}")', PROVIDER)

if __name__ == "__main__":
    unittest.main()
