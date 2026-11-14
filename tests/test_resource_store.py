"""Resource migration and signed manifest contract; original files stay available to tests."""
import importlib.util, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
spec=importlib.util.spec_from_file_location("resource_catalog",ROOT/"scripts/build_resource_catalog.py")
catalog=importlib.util.module_from_spec(spec);spec.loader.exec_module(catalog)
class ResourceStoreTest(unittest.TestCase):
    def test_catalog_and_thin(self):
        bridge=catalog.build("a"*40);thin=catalog.build("a"*40,True)
        self.assertTrue(bridge["bundled"]);self.assertFalse(thin["bundled"])
        self.assertEqual(bridge["files"],thin["files"])
        self.assertGreaterEqual(len(bridge["files"]),89)
        self.assertEqual(len({e["path"] for e in bridge["files"]}),len(bridge["files"]))
        portraits=[e for e in bridge["files"] if e["path"].startswith("portraits/")]
        self.assertEqual(len(portraits),60)
        self.assertEqual(len({e["group"] for e in portraits}),10)
        self.assertTrue(all(len(e["sha256"])==64 and e["size"]>0 for e in bridge["files"]))
        self.assertNotIn("lore_bundle.zip",{e["path"] for e in bridge["files"]})
    def test_store_and_ota(self):
        s=(JAVA/"ResourceStore.java").read_text()
        for token in ("getFilesDir()","MessageDigest.getInstance","ATOMIC_MOVE","removed:","Content-Range","TRANSPORT_WIFI","copyBundled"):
            self.assertIn(token,s)
        ui=(JAVA/"MainActivity.java").read_text();ota=(JAVA/"UpdateManager.java").read_text()
        self.assertIn("ResourceStore.start(this)",ui);self.assertIn("Управление ресурсами",ui)
        self.assertIn("ResourceStore.migrate(a",ota)
        self.assertIn("automaticAllowed",s)
        self.assertIn("!removed(app,e)",s)
        self.assertIn("autoUpdate(app)",s)
        self.assertIn("ResourceStore.requestGallery",(JAVA/"LocationGalleryView.java").read_text())
        self.assertIn("initialGroup",(JAVA/"ResourceManagerDialog.java").read_text())
        self.assertLess(ota.index("ResourceStore.migrate(a"),ota.index("launchVerifiedInstaller(a);"))
        self.assertIn("ResourceStore.open(context,IMAGE)",(JAVA/"SkyrimIconAtlas.java").read_text())
        self.assertIn("ResourceStore.open(app,TEXTURES[requested])",(JAVA/"SkyrimTerrainMapView.java").read_text())
        p=(JAVA/"CharacterPortraitView.java").read_text()
        self.assertIn("ResourceStore.open(app,assetPath)",p)
        self.assertNotIn("app.getAssets().open(assetPath)",p)
        self.assertIn("ResourceStore.generation()",p)
        self.assertIn("ResourceStore.portraitGroup",ui)
if __name__=="__main__":unittest.main()
