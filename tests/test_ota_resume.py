import pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
class ResumeOta(unittest.TestCase):
    def test_service_manifest_and_lifecycle(self):
        m=(ROOT/"app/src/main/AndroidManifest.xml").read_text()
        for token in ("FOREGROUND_SERVICE_DATA_SYNC","WAKE_LOCK",'android:foregroundServiceType="dataSync"'):
            self.assertIn(token,m)
        s=(JAVA/"OtaDownloadService.java").read_text()
        for token in ("startForegroundService","startForeground(ID","PARTIAL_WAKE_LOCK","START_NOT_STICKY","task.cancelled","stopForeground","UpdateManager.downloadResumable","savedRelease"):
            self.assertIn(token,s)
    def test_partial_identity_and_verification(self):
        s=(JAVA/"UpdateManager.java").read_text()
        for token in ('r.sha256+"|"+r.sizeBytes','"Range","bytes="+received+"-"','"Content-Range"','first!=received','new FileOutputStream(temp,received>0)','out.getFD().sync()','new FileInputStream(temp)','verifyApk(context,temp,r.versionCode)','control.check()'):
            self.assertIn(token,s)
        self.assertLess(s.index("verifyApk(context,temp,r.versionCode)"),s.index("Files.move(temp.toPath(),apk.toPath()"))
    def test_mini_and_cancel(self):
        s=(JAVA/"SkyrimOtaController.java").read_text()
        for token in ("attachMini","mini.setOnClickListener(v->open())","OtaDownloadService.cancel(activity)","screen.paused","screen.ready","onHostPause"):
            self.assertIn(token,s)
        self.assertNotIn("UpdateManager.download(activity",s)
        self.assertIn("offered=info",s)
        self.assertIn("else if(offered!=null)showOffer(offered)",s)
        self.assertIn('APK_FILE+".part.apk"',(JAVA/"UpdateManager.java").read_text())
if __name__=="__main__":unittest.main()
