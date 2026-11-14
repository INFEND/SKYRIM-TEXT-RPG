"""Executable contracts for complete new galleries and preservation of existing art."""
import hashlib
import json
import pathlib
import re
import shutil
import subprocess
import tempfile
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/gallery"
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext/LocationGalleryView.java"

def webp_dimensions(data):
    if data[:4] != b"RIFF" or data[8:12] != b"WEBP":
        raise ValueError("Not WebP")
    if int.from_bytes(data[4:8], "little") + 8 != len(data):
        raise ValueError("Truncated RIFF")
    pos = 12
    while pos + 8 <= len(data):
        kind = data[pos:pos+4]
        size = int.from_bytes(data[pos+4:pos+8], "little")
        body = data[pos+8:pos+8+size]
        if len(body) != size:
            raise ValueError("Truncated chunk")
        if kind == b"VP8X":
            return (1+int.from_bytes(body[4:7], "little"),
                    1+int.from_bytes(body[7:10], "little"))
        if kind == b"VP8L" and body[:1] == b"\x2f":
            bits = int.from_bytes(body[1:5], "little")
            return (1+(bits & 0x3fff), 1+((bits >> 14) & 0x3fff))
        if kind == b"VP8 " and body[3:6] == b"\x9d\x01\x2a":
            return (int.from_bytes(body[6:8], "little") & 0x3fff,
                    int.from_bytes(body[8:10], "little") & 0x3fff)
        pos += 8 + size + size % 2
    raise ValueError("No WebP frame")

class GalleryExpansion(unittest.TestCase):
    def test_original_archives_are_byte_identical(self):
        report = json.loads((ROOT/"docs/gallery_expansion_compression.json").read_text())
        baseline = report["previous_archives_sha256"]
        self.assertEqual(len(baseline), 11)
        for name, expected in baseline.items():
            self.assertEqual(hashlib.sha256((ASSETS/name).read_bytes()).hexdigest(), expected, name)

    def test_all_new_images_match_measured_report(self):
        report = json.loads((ROOT/"docs/gallery_expansion_compression.json").read_text())
        self.assertEqual(report["image_count"], 79)
        self.assertEqual(len(report["packs"]), 14)
        observed = set()
        original_bytes = 0
        archive_bytes = 0
        digests = set()
        for pack in report["packs"]:
            path = ASSETS/pack["archive"]
            archive_bytes += path.stat().st_size
            self.assertEqual(path.stat().st_size, pack["archive_bytes"])
            self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), pack["archive_sha256"])
            with zipfile.ZipFile(path) as z:
                self.assertIsNone(z.testzip())
                self.assertEqual(set(z.namelist()), {f'{p["number"]:02d}.webp' for p in pack["photos"]})
                for photo in pack["photos"]:
                    key = (pack["slug"], photo["number"])
                    self.assertNotIn(key, observed)
                    observed.add(key)
                    data = z.read(f'{photo["number"]:02d}.webp')
                    digest = hashlib.sha256(data).hexdigest()
                    self.assertEqual(digest, photo["sha256"])
                    self.assertNotIn(digest, digests, "Repeated artwork")
                    digests.add(digest)
                    self.assertEqual(len(data), photo["encoded_bytes"])
                    self.assertLess(len(data), 2_000_000)
                    width, height = webp_dimensions(data)
                    self.assertEqual([width,height], photo["dimensions"])
                    self.assertLessEqual(width, min(1920, photo["source_dimensions"][0]))
                    self.assertLessEqual(height, min(1080, photo["source_dimensions"][1]))
                    self.assertGreaterEqual(photo["full_resolution_luma_ssim"], .985)
                    self.assertGreaterEqual(photo["worst_128px_luma_ssim"], .965)
                    self.assertLessEqual(photo["chroma_mean_absolute_error"], 2.5)
                    self.assertLessEqual(photo["chroma_95th_percentile_error"], 7)
                    original_bytes += photo["original_bytes"]
        self.assertEqual(len(observed), 79)
        self.assertEqual(original_bytes, report["original_bytes"])
        self.assertEqual(archive_bytes, report["archive_bytes"])

    def test_runtime_aliases_labels_and_archive_routing(self):
        self.assertIsNotNone(shutil.which("javac"), "JDK required for catalog probe")
        viewer = JAVA.read_text(encoding="utf-8")
        catalog = viewer.split("    private static final String[] CITIES={",1)[1]
        catalog = "    private static final String[] CITIES={"+catalog.split(
            "    private static final ExecutorService IO=",1)[0]
        static = viewer.split("    static{",1)[1]
        static = "    static{"+static.split("    private final String location",1)[0]
        methods = []
        for name in ("archive","captionFor"):
            match = re.search(r"    private static String "+name+r"\(.*?\n    \}",viewer,re.S)
            self.assertIsNotNone(match,name)
            methods.append(match.group(0))
        probe = r'''
    public static void main(String[] args){
        if(locations().length!=23)throw new AssertionError("location count");
        for(String name:locations()){
            if(!hasGallery(name)||imageCount(name)!=6)throw new AssertionError(name);
            for(int n=0;n<6;n++){
                if(captionFor(name,n).trim().isEmpty())throw new AssertionError(name);
            }
        }
        String[][] pairs={
            {"Камень Шора","Шорский Камень"},{"Роща Кин","Кинсгроув"},
            {"Черный Брод","Чёрный Брод"},{" каменные холмы ","Каменные Холмы"},
            {" ДРАКОНИЙ МОСТ ","Драконий Мост"},{"High Hrothgar","Высокий Хротгар"}
        };
        for(String[] pair:pairs){
            if(!hasGallery(pair[0])||imageCount(pair[0])!=6)throw new AssertionError(pair[0]);
            if(!ID_BY_CITY.get(locationKey(pair[0])).equals(
                ID_BY_CITY.get(locationKey(pair[1]))))throw new AssertionError("alias");
            if(!captionFor(pair[0],5).equals(captionFor(pair[1],5)))throw new AssertionError("caption");
        }
        if(hasGallery(null)||imageCount(null)!=0||hasGallery("Несуществующее место"))
            throw new AssertionError("unknown location");
        if(!archive("riverwood",5).equals("riverwood.zip")
            ||!archive("riverwood",6).equals("riverwood_extra.zip")
            ||!archive("riften",4).equals("riften_extra.zip"))
            throw new AssertionError("archive routing");
    }
'''
        source = "import java.util.*;\npublic final class GalleryProbe {\n"+catalog+static+"\n".join(methods)+probe+"\n}"
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp)/"GalleryProbe.java"
            path.write_text(source,encoding="utf-8")
            subprocess.run(["javac","-encoding","UTF-8",str(path)],check=True,capture_output=True,text=True)
            subprocess.run(["java","-cp",tmp,"GalleryProbe"],check=True,capture_output=True,text=True)

if __name__ == "__main__":
    unittest.main()
