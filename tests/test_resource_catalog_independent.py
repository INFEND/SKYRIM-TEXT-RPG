import importlib.util,pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
spec=importlib.util.spec_from_file_location("catalog",ROOT/"scripts/build_resource_catalog.py")
catalog=importlib.util.module_from_spec(spec);spec.loader.exec_module(catalog)
class IndependentCatalog(unittest.TestCase):
    def test_every_gallery_has_a_signed_caption_mapping(self):
        registry=catalog.gallery_registry()
        self.assertGreaterEqual(len(registry),29)
        self.assertGreaterEqual(sum(len(e["captions"]) for e in registry),166)
        self.assertTrue(all(len(e["captions"])==len(e["archives"]) for e in registry))
        by_slug={e["slug"]:e for e in registry}
        self.assertEqual(len(by_slug["riverwood"]["captions"]),5)
        self.assertEqual(by_slug["riften"]["archives"][3],"riften_extra.zip")
        self.assertEqual(by_slug["highhrothgar"]["archives"][6],"highhrothgar_extra.zip")
    def test_signed_catalog_is_verified_and_reloaded(self):
        store=(JAVA/"ResourceStore.java").read_text()
        signed=(JAVA/"SignedResourceCatalog.java").read_text()
        ui=(JAVA/"ResourceManagerDialog.java").read_text()
        workflow=(ROOT/".github/workflows/resource-catalog.yml").read_text()
        for token in ("SignedResourceCatalog.verify","remote.revision<before.revision","SignedResourceCatalog.save","LocationGalleryView.applyCatalog","State.NEW","autoNew(app)"):
            self.assertIn(token,store)
        self.assertIn("verify.verify(signature)",signed)
        self.assertIn("GET_SIGNING_CERTIFICATES",signed)
        self.assertIn("resource-catalog.json",workflow)
        for token in ("rebuildGroups()","Новые: ","Обновить: ","Повреждены: ","automaticError"):
            self.assertIn(token,ui)
    def test_exact_map_markers_are_downloadable(self):
        data=catalog.build("a"*40,True)
        paths={e["path"] for e in data["files"]}
        for stem in ("settlement_icons","location_icons"):
            name=stem+".webp"
            self.assertIn("media/mapicons/"+name,paths)
            self.assertEqual((catalog.ASSETS/"media/mapicons"/name).read_bytes(),(ROOT/"app/src/main/res/drawable-nodpi"/name).read_bytes())

    def test_manual_check_reports_freshness_and_counts(self):
        store=(JAVA/"ResourceStore.java").read_text()
        signed=(JAVA/"SignedResourceCatalog.java").read_text()
        ui=(JAVA/"ResourceManagerDialog.java").read_text()
        self.assertIn("setUseCaches(false)",signed)
        self.assertIn("?check=",signed)
        self.assertIn("Серверный каталог старее установленного",store)
        self.assertIn("Каталог актуален",ui)
        self.assertIn("catalogReport",ui)
        self.assertIn("case NEW:fresh++",ui)
