"""OTA check status belongs to the dedicated dialog, never to Settings."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"

def source(name):
    return (JAVA/name).read_text(encoding="utf-8")


class OnlyOtaDialogDisplaysCheckStatus(unittest.TestCase):
    def test_no_inline_background_status_or_raw_socket_error_in_settings(self):
        ui=source("MainActivity.java")
        settings=ui.split("int updatesStart=content.getChildCount();",1)[1]
        settings=settings.split('foldSettingsSection("updates"',1)[0]
        self.assertIn('actionButton("Проверить обновления"',settings)
        self.assertNotIn("ota_background_result",settings)
        self.assertNotIn("String result=prefs.getString",settings)
        self.assertNotIn("addText(otaOptions,result",settings)
        self.assertIn("OtaUpdateSchedule.configure(this)",settings)

    def test_background_checks_and_scheduler_do_not_save_status_messages(self):
        background=source("BackgroundUpdateJob.java")
        schedule=source("OtaUpdateSchedule.java")
        self.assertNotIn("ota_background_result",background)
        self.assertNotIn("ota_background_result",schedule)
        self.assertNotIn("error.getMessage()",background)
        self.assertIn("UpdateManager.fetchLatest(getApplicationContext())",background)
        self.assertIn("notifyUpdate(release.versionName)",background)
        self.assertIn("jobFinished(params,false)",background)
        self.assertIn("scheduler.schedule(info)",schedule)

    def test_manual_ota_status_and_retries_remain_in_the_update_dialog(self):
        controller=source("SkyrimOtaController.java")
        self.assertIn("display().stage(message",controller)
        self.assertIn("display().fail(message,()->check(true))",controller)
        self.assertIn("display().offer(info.versionName,info.notesFor(BuildConfig.VERSION_CODE)",controller)
        self.assertIn("screen.activeTransfer(s.phase,s.message,s.received,s.total",controller)
        self.assertIn("private OtaProgressDialog display()",controller)
        self.assertIn("if(manual){display().stage",controller)
        self.assertIn("if(updater!=null)updater.attachMini(root)",source("MainActivity.java"))


if __name__=="__main__":
    unittest.main()
