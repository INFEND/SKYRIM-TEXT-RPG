import pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
APP=ROOT/"app/src/main/java/dev/infend/skyrimtext"
ENGINE=ROOT/"engine/src/main/java/dev/infend/skyrimtext"
class UltraLiteEngine(unittest.TestCase):
    def test_engine_is_separate_and_guarded(self):
        self.assertNotIn("llamacpp-kotlin",(ROOT/"app/build.gradle").read_text())
        self.assertIn("llamacpp-kotlin",(ROOT/"engine/build.gradle").read_text())
        self.assertFalse((APP/"LocalAiInferenceService.kt").exists())
        self.assertIn("checkSignatures(message.sendingUid",(ENGINE/"LocalAiInferenceService.kt").read_text())
        self.assertIn('android:protectionLevel="signature"',(ROOT/"app/src/main/AndroidManifest.xml").read_text())
        self.assertIn('android:permission="dev.infend.skyrimtext.permission.LOCAL_ENGINE"',(ROOT/"engine/src/main/AndroidManifest.xml").read_text())
        self.assertIn("Arrays.equals(x,y)",(APP/"LocalAiEngineModule.java").read_text())
        self.assertEqual((APP/"AiScenePreview.java").read_bytes(),(ENGINE/"AiScenePreview.java").read_bytes())
    def test_release_has_native_asset_and_no_native_main(self):
        workflow=(ROOT/".github/workflows/android-ota.yml").read_text()
        self.assertIn("dist/SKYRIM-LOCAL-AI.apk",workflow)
        self.assertIn("engine_module.json",workflow)
        self.assertIn("Native libraries leaked into Ultra Lite",(ROOT/"tools/verify_resource_apks.py").read_text())
