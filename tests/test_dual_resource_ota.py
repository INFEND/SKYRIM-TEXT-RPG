"""Canonical OTA always points to the lightweight APK, including legacy readers."""
import pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
class DualOtaTest(unittest.TestCase):
    def test_thin_is_default_without_local_readiness(self):
        ota=(JAVA/"UpdateManager.java").read_text()
        self.assertIn("if(coreOnly)return this",ota)
        self.assertNotIn("!ResourceStore.canUseThin(context)",ota)
        self.assertIn("trustedThinApk(url,versionName)",ota)
        self.assertIn("verifyApk(context,temp,r.versionCode)",ota)
        self.assertNotIn("download(release.fullVersion())",(JAVA/"SkyrimOtaController.java").read_text())
    def test_release_publishes_identical_aliases_and_verifies_contents(self):
        wf=(ROOT/".github/workflows/android-ota.yml").read_text()
        self.assertIn('scripts/build_resource_catalog.py --commit "$GITHUB_SHA" --thin',wf)
        self.assertIn('cp dist/SKYRIM-TEXT-RPG.apk dist/SKYRIM-TEXT-RPG-thin.apk',wf)
        self.assertLess(wf.index('tools/verify_resource_apks.py'),wf.index('name: Publish OTA release'))
        gen=(ROOT/"tools/build_manifest.py").read_text()
        self.assertIn('"coreOnly": True',gen)
        self.assertNotIn('payload["thinApk"]',gen)
        self.assertIn('"apkUrl": f"{base}v{version}/SKYRIM-TEXT-RPG.apk"',gen)
if __name__=="__main__":unittest.main()
