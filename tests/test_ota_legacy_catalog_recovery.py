"""An invalid legacy catalog may not permanently block verified in-app OTA installs."""
import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
CODE=(ROOT/"app/src/main/java/dev/infend/skyrimtext/UpdateManager.java").read_text(encoding="utf-8")
STORE=(ROOT/"app/src/main/java/dev/infend/skyrimtext/ResourceStore.java").read_text(encoding="utf-8")
class OtaLegacyCatalogRecoveryTest(unittest.TestCase):
    def test_migration_failure_proposes_confirmed_install(self):
        self.assertIn("ResourceStore.migrate(a,",CODE)
        self.assertIn("offerInstallWithoutMigration(a,friendly(e))",CODE)
        self.assertIn('setTitle("Не удалось перенести ресурсы")',CODE)
        self.assertIn('setPositiveButton("Установить без переноса"',CODE)
        self.assertIn('setNegativeButton("Отмена",null)',CODE)
        self.assertNotIn('migration.fail("Не удалось сохранить ресурсы:',CODE)
    def test_install_must_still_validate_apk_before_and_after_confirmation(self):
        self.assertIn("verifyApk(a,apk,archive.getLongVersionCode())",CODE)
        self.assertIn('if(!Arrays.equals(oldSigners,newSigners))',CODE)
        self.assertIn('!TRUSTED_CERT_SHA256.equals(sha256OfSignature(newSigners[0]))',CODE)
        self.assertIn('setPositiveButton("Установить без переноса",(dialog,which)->launchVerifiedInstaller(a))',CODE)
        self.assertGreaterEqual(CODE.count("verifyApk(a,apk,archive.getLongVersionCode())"),2)
        self.assertIn("Uri.parse(\"content://\"+a.getPackageName()+\".updates/apk\")",CODE)
        self.assertNotIn("deleteDatabase(",CODE)
    def test_existing_migration_remains_checksum_verified(self):
        self.assertIn("copyBundled(c,e,new AtomicBoolean(false),false)",STORE)
        self.assertIn("if(!valid(temp,e))",STORE)
if __name__=="__main__":unittest.main()
