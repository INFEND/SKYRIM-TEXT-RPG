"""Regression checks for the user's optimized offline Skyrim location galleries."""
import io
import pathlib
import re
import unittest
import zipfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets/gallery"
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class LocationGalleryContracts(unittest.TestCase):
    def test_exact_city_membership_and_gallery_count(self):
        expected={
            "whiterun":6,"windhelm":6,"winterhold":6,
            "dawnstar":6,"markarth":6,"morthal":6,
            "riverwood":6,"riften":6,"solitude":6,"falkreath":6,
            "helgen":6,"ivarstead":6,"rorikstead":6,"dragonbridge":6,"karthwasten":6,"shorsstone":6,"kynesgrove":6,"darkwater":6,"stonehills":6,"ravenrock":6,"telmithryn":6,"skaal":6,"highhrothgar":11,"bleakfallsbarrow":6,"blackreach":5,"labyrinthian":6,"collegewinterhold":4,"throatoftheworld":2,"ustengrav":1
        }
        self.assertEqual(len(expected),29)
        self.assertEqual(sum(expected.values()),167)
        for slug,n in expected.items():
            packages=[ASSETS/f"{slug}.zip"]
            if slug in ("riften","riverwood","highhrothgar"):packages.append(ASSETS/f"{slug}_extra.zip")
            observed={}
            for package in packages:
                self.assertTrue(package.is_file(),f"Missing {package}")
                with zipfile.ZipFile(package) as z:
                    self.assertIsNone(z.testzip(),f"Bad ZIP entry in {package}")
                    for name in z.namelist():
                        if not name.endswith(".webp"):continue
                        self.assertNotIn(name,observed,slug+": duplicated photo "+name)
                        image=z.read(name)
                        self.assertGreater(len(image),16000,slug+": suspiciously small photo")
                        self.assertLess(len(image),2_000_000,slug+": exceeds image decode limit")
                        self.assertEqual(image[:4],b"RIFF")
                        self.assertEqual(image[8:12],b"WEBP")
                        observed[name]=True
            self.assertEqual(set(observed),{f"{i:02d}.webp" for i in range(1,n+1)},slug)

    def test_small_assets_and_no_external_sources(self):
        packs=list(ASSETS.glob("*.zip"))
        self.assertEqual(len(packs),32)
        size=sum(x.stat().st_size for x in packs)
        self.assertGreater(size,12_000_000)
        self.assertLess(size,100_000_000,"Compressed artwork unexpectedly inflated")
        viewer=(JAVA/"LocationGalleryView.java").read_text(encoding="utf-8")
        self.assertIn("GestureDetector",viewer)
        self.assertIn("onFling",viewer)
        self.assertIn('return number<=3?"riften.zip":"riften_extra.zip"',viewer)
        self.assertIn('ResourceStore.open(context,"gallery/"+archive(slug,index))',viewer)
        self.assertIn("BitmapFactory.decodeByteArray",viewer)
        self.assertIn("getApplicationContext()",viewer)
        self.assertIn("openFullscreen()",viewer)
        self.assertNotIn("https://",viewer)

    def test_galleries_only_appear_for_matching_location(self):
        ui=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
        viewer=(JAVA/"LocationGalleryView.java").read_text(encoding="utf-8")
        self.assertIn('LocationGalleryView.hasGallery(game.progress.location)',ui)
        self.assertIn('LocationGalleryView.hasGallery(place.name)',ui)
        self.assertIn('new LocationGalleryView(this,place.name,accent(),false)',ui)
        self.assertIn('overview.addView(new LocationGalleryView',ui)
        self.assertIn('String[] CITIES={',viewer)
        self.assertIn('return PHOTO_LABELS[PHOTO_INDEX_BY_CITY.get(locationKey(location))].length',viewer)

if __name__=="__main__":
    unittest.main()
