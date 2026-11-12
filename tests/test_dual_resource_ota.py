"""Dual APK publication and legacy compatibility."""
import pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
class DualOtaTest(unittest.TestCase):
    def test_thin_is_opt_in_by_device_readiness(self):
        ota=(JAVA/"UpdateManager.java").read_text()
        store=(JAVA/"ResourceStore.java").read_text()
        policy=(JAVA/"OtaUrlPolicy.java").read_text()
        self.assertIn('!ResourceStore.canUseThin(context)',ota)
        self.assertIn('if(!a.bundled)return true',store)
        self.assertIn('!removed(app,e)&&(!local(app,e)||!valid(file(app,e),e))',store)
        self.assertIn('trustedThinApk(url,versionName)',ota)
        self.assertIn('SKYRIM-TEXT-RPG-thin.apk',policy)
        self.assertIn('verifyApk(activity,apk,r.versionCode)',ota)
        self.assertIn('release.fullVersion()', (JAVA/"SkyrimOtaController.java").read_text())
    def test_release_publishes_both_and_verifies_contents(self):
        wf=(ROOT/".github/workflows/android-ota.yml").read_text()
        self.assertIn('dist/SKYRIM-TEXT-RPG.apk',wf)
        self.assertIn('dist/SKYRIM-TEXT-RPG-thin.apk',wf)
        self.assertIn('tools/verify_resource_apks.py',wf)
        self.assertLess(wf.index('tools/verify_resource_apks.py'),wf.index('name: Publish OTA release'))
        gen=(ROOT/"tools/build_manifest.py").read_text()
        self.assertIn('payload["thinApk"]',gen)
        self.assertIn('"apkUrl": f"{base}v{version}/SKYRIM-TEXT-RPG.apk"',gen)
if __name__=="__main__":unittest.main()
