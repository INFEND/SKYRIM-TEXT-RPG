import pathlib,unittest,re
ROOT=pathlib.Path(__file__).resolve().parents[1]
APP=ROOT/"app/src/main/java/dev/infend/skyrimtext"
ENGINE=ROOT/"engine/src/main"
class LocalEngineActivation(unittest.TestCase):
    def test_activation_has_foreground_entry_and_signature_protected_service(self):
        manifest=(ENGINE/"AndroidManifest.xml").read_text()
        self.assertIn("android.intent.category.LAUNCHER",manifest)
        self.assertIn("EngineActivationActivity",manifest)
        self.assertIn('android:permission="dev.infend.skyrimtext.permission.LOCAL_ENGINE"',manifest)
        self.assertIn("checkSignatures(getCallingPackage(),getPackageName())",(ENGINE/"java/dev/infend/skyrimtext/EngineActivationActivity.java").read_text())
    def test_bind_failure_has_bounded_retry(self):
        client=(APP/"LocalAiClient.kt").read_text()
        self.assertIn("activationAttempted=true",client)
        self.assertIn("if(!activationAttempted&&pending!=null",client)
        self.assertIn("onEngineActivationResult",client)
        self.assertIn("binding!==this",client)
        self.assertIn("ui.postDelayed(timeout,10000L)",client)
        self.assertIn("onEngineActivationResult(resultCode==RESULT_OK)",(APP/"MainActivity.java").read_text())
    def test_engine_version_matches_metadata(self):
        code=int(re.search(r"versionCode\s+(\d+)",(ROOT/"engine/build.gradle").read_text()).group(1))
        self.assertIn("VERSION="+str(code)+";",(APP/"LocalAiEngineModule.java").read_text())
        self.assertIn('"versionCode":version',(ROOT/".github/workflows/android-ota.yml").read_text())
