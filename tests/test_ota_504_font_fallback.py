"""OTA survives transient GitHub 504 and matches Sovngarde app typography."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"


def code(name):
    return (JAVA/name).read_text(encoding="utf-8")


class Ota504AndTypography(unittest.TestCase):
    def test_dialog_uses_app_font_everywhere(self):
        ui=code("OtaProgressDialog.java")
        self.assertIn("v.setTypeface(SovngardeTypeface.face(activity,bold))",ui)
        self.assertNotIn("v.setTypeface(Typeface.SERIF",ui)
        self.assertIn('details.setText(message)',ui)
        self.assertIn("notesText=text(",ui)

    def test_bounded_retry_and_non_retryable_failures(self):
        ota=code("UpdateManager.java")
        self.assertIn("private static final int MANIFEST_ATTEMPTS=3",ota)
        self.assertIn("private static final int APK_ATTEMPTS=3",ota)
        self.assertIn("static final class HttpStatusException extends IOException",ota)
        self.assertIn("status==408||status==429||status>=500",ota)
        self.assertIn("e instanceof SocketTimeoutException",ota)
        self.assertIn("private static void retryPause(int attempt)",ota)
        self.assertIn('report(reporter,"Повтор подключения к серверу: "',ota)
        self.assertIn("if(!transientNetworkFailure(problem)||attempt==MANIFEST_ATTEMPTS)throw problem;",ota)
        self.assertIn("if(!transientNetworkFailure(unavailable)||attempt>=APK_ATTEMPTS)break;",ota)

    def test_pinned_github_fallback_and_signed_apk_integrity(self):
        ota=code("UpdateManager.java")
        urls=code("OtaUrlPolicy.java")
        workflow=(ROOT/".github/workflows/android-ota.yml").read_text(encoding="utf-8")
        self.assertIn("readManifestRetried(MANIFEST_URL,reporter)",ota)
        self.assertIn("readLatestPublishedTag()",ota)
        self.assertIn("OtaUrlPolicy.manifestForTag(expectedTag)",ota)
        self.assertIn("readManifestRetried(OtaUrlPolicy.MANIFEST_MIRROR,reporter)",ota)
        self.assertIn('if(expectedTag!=null&&!expectedTag.equals("v"+mirror.versionName))',ota)
        self.assertIn('pinned.equals(a.optString("browser_download_url"))',ota)
        self.assertIn("OtaUrlPolicy.trustedApk(apkUrl,versionName)",ota)
        self.assertIn("TRUSTED_CERT_SHA256",ota)
        self.assertIn('MessageDigest.getInstance("SHA-256")',ota)
        self.assertIn("raw.githubusercontent.com/INFEND/SKYRIM-TEXT-RPG/ota-manifest/update.json",urls)
        self.assertIn('gh api --method POST "repos/${GITHUB_REPOSITORY}/git/refs"',workflow)
        self.assertIn('base64 -w0 dist/update.json',workflow)
        self.assertIn('Publish trusted manifest mirror',workflow)
        self.assertLess(workflow.index("name: Publish OTA release"),workflow.index("name: Publish trusted manifest mirror"))
        self.assertNotIn('InetAddress.getAllByName("github.com")',ota)

if __name__=="__main__":
    unittest.main()
