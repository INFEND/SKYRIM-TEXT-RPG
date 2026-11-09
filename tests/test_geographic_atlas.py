"""Execute the actual Java atlas, pin clustering, aliases and travel planner."""
import hashlib
import json
from pathlib import Path
import re
import math
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class GeographicSource(unittest.TestCase):
    def test_compiled_points_keep_source_coordinates_and_unique_identity(self):
        points=json.loads((ROOT/"tools/map_source/compiled.json").read_text())
        sources={p["id"]:p for file in ("skyrim.json","solstheim.json")
                 for p in json.loads((ROOT/"tools/map_source"/file).read_text())}
        self.assertEqual(len(points),512)
        self.assertEqual(len({p["id"] for p in points}),512)
        self.assertEqual(len({p["nameRu"] for p in points}),512)
        self.assertEqual(sum(p["sheet"]==1 for p in points),57)
        for point in points:
            self.assertEqual((point["x"],point["y"]),(sources[point["id"]]["x"],sources[point["id"]]["y"]))
            self.assertRegex(point["nameRu"],"[А-Яа-яЁё]")
        for name in ("Fort Dawnguard (DG)","Castle Volkihar (DG)",
                     "Heljarchen Hall (HF)","Lakeview Manor (HF)","Windstad Manor (HF)",
                     "Temple of Miraak","Nchardak","Raven Rock"):
            self.assertIn(name,{p["name"] for p in points})

    def test_terrain_files_match_reviewed_inputs(self):
        expected={"skyrim_geographic.webp":"d32690ec652fe6917a7e0a102e024a06c3386ba33593332d00cb2fadbfecf5c8",
                  "solstheim_terrain.webp":"712a0a78afd7de78690ad1c99f25a7d6086b354bf398ab15abc0b3fd9902404d"}
        for filename,digest in expected.items():
            self.assertEqual(hashlib.sha256((ROOT/"app/src/main/assets/maps"/filename).read_bytes()).hexdigest(),digest)

    def test_island_registration_preserves_published_anchors_and_flags_estimates(self):
        points=json.loads((ROOT/"tools/map_source/compiled.json").read_text())
        reference={p["name"]:p for p in json.loads((ROOT/"tools/map_source/solstheim_projection.json").read_text())}
        island=[p for p in points if p["sheet"]==1]
        self.assertEqual(sum(p["pinSource"]=="anchor" for p in island),46)
        self.assertEqual(sum(p["pinSource"]=="interpolated" for p in island),11)
        for point in island:
            self.assertTrue(0<=point["displayX"]<=2048 and 0<=point["displayY"]<=2048)
            if point["pinSource"]!="anchor":continue
            lon,lat=reference[point["name"]]["coordinates"][:2]
            self.assertAlmostEqual(point["displayX"],(lon+180)/360*2048,delta=.00006)
            self.assertAlmostEqual(point["displayY"],(1-math.asinh(math.tan(math.radians(lat)))/math.pi)/2*2048,delta=.00006)
        ui=(JAVA/"MainActivity.java").read_text()
        self.assertIn("!geographic.registered",ui)
        self.assertIn("Положение метки на рельефе ориентировочное",ui)

    def test_generation_is_reproducible(self):
        paths=[JAVA/"SkyrimMapPoints.java",ROOT/"tools/map_source/compiled.json"]
        previous=[p.read_bytes() for p in paths]
        subprocess.run(["python3",str(ROOT/"tools/build_geographic_atlas.py")],check=True,capture_output=True)
        self.assertEqual(previous,[p.read_bytes() for p in paths])

    def test_real_java_models_and_validated_routes(self):
        if shutil.which("javac") is None:self.skipTest("Java model checks run on JDK-equipped CI")
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            for name in ("SkyrimMapCatalog.java","SkyrimMapPoints.java","MapPinLayout.java","TravelPlanner.java"):
                (root/name).write_bytes((JAVA/name).read_bytes())
            # Keep the exact WorldAtlas model and travel integration; remove only
            # the unused legacy Android drawing class and its Android imports.
            atlas=(JAVA/"WorldAtlas.java").read_text().split("    public static final class MapView extends View",1)[0]+"}\n"
            atlas=re.sub(r"^import android\.[^\n]+\n","",atlas,flags=re.M)
            (root/"WorldAtlas.java").write_text(atlas)
            (root/"GameEngine.java").write_text('''package dev.infend.skyrimtext;
class GameEngine {
 boolean started=true; int region=2,gold=250; Progress progress=new Progress();
 static class Progress {String location="Виндхельм"; double weight;
  double carryWeight(){return weight;}}
}''')
            (root/"Probe.java").write_text('''package dev.infend.skyrimtext;
import java.util.*;
public class Probe {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 public static void main(String[] args){
  ArrayList<SkyrimMapCatalog.Site> all=SkyrimMapCatalog.all();
  check(all.size()==512,"catalog size");
  for(SkyrimMapCatalog.Site site:all){
   check(Float.isFinite(site.x)&&Float.isFinite(site.y),"finite coordinate");
   check(site.x>=0&&site.y>=0&&site.x<=(site.sheet==0?4096:2048)&&site.y<=(site.sheet==0?4096:2048),"sheet bounds: "+site.name);
   WorldAtlas.Place place=WorldAtlas.find(site.name);
   check(place!=null&&place.region==site.region,"playable place: "+site.name);
  }
  check(SkyrimMapCatalog.find("Бтардамз").matches("bthardamz"),"English search");
  check(SkyrimMapCatalog.find("ЧЁРНЫЙ БРОД")==SkyrimMapCatalog.find("Черный Брод"),"yo normalization");
  check(WorldAtlas.find("Камень Шора")==WorldAtlas.find("Шорский Камень"),"old save alias");
  check(WorldAtlas.find("Колбьорн")!=null,"island legacy key");
  check(SkyrimMapCatalog.travelDestination(SkyrimMapCatalog.find("To Solstheim (DB)")).equals("Воронья Скала"),"boarding point goes to island");
  check(SkyrimMapCatalog.travelDestination(SkyrimMapCatalog.find("To Skyrim")).equals("Виндхельм"),"return boarding point goes to Windhelm");
  check(WorldAtlas.find("Хелген").region==5,"Helgen travel district");
  check(WorldAtlas.find("Ларгашбур").region==1,"Largashbur corrected geography");
  check(WorldAtlas.find("Лабиринтиан").region==8,"Labyrinthian corrected geography");
  ArrayList<SkyrimMapCatalog.Site> pair=new ArrayList<>();
  pair.add(new SkyrimMapCatalog.Site("a|А|A|Пещера|0|0|0|0"));
  pair.add(new SkyrimMapCatalog.Site("b|Б|B|Пещера|0|8192|0|0"));
  Set<String> hidden=new HashSet<>();
  ArrayList<MapPinLayout.Pin> low=MapPinLayout.build(pair,0,-1,hidden,"","",.1f,0,0,500,500,1);
  check(low.size()==1&&low.get(0).sites.size()==1,"nearby pins are decluttered without clustering");
  ArrayList<MapPinLayout.Pin> shifted=MapPinLayout.build(pair,0,-1,hidden,"","",
      .1f,115,-72,500,500,1);
  check(shifted.size()==low.size(),"camera panning cannot change pin visibility");
  check(shifted.get(0).sites.get(0).id.equals(low.get(0).sites.get(0).id),
      "camera panning cannot reassign a stable map icon");
  check(Math.abs((shifted.get(0).x-low.get(0).x)-115f)<.01f,
      "pin X stays attached to world during camera translation");
  check(Math.abs((shifted.get(0).y-low.get(0).y)+72f)<.01f,
      "pin Y stays attached to world during camera translation");
  MapPinLayout.translate(low,115,-72);
  check(Math.abs(low.get(0).x-shifted.get(0).x)<.01f,
      "translated cache equals independently rebuilt map");
  check(Math.abs(MapPinLayout.sizeDp(1f,400)-10f)<.01f,
      "distant individual marker remains compact");
  check(MapPinLayout.sizeDp(14f,400)<=26.01f,
      "nearby individual marker stops growing at a legible size");
  check(MapPinLayout.sizeDp(4f,400)>MapPinLayout.sizeDp(2f,400),
      "marker size grows smoothly with map zoom");
  check(MapPinLayout.hit(low,low.get(0).x,low.get(0).y,25)==low.get(0),"drawn cluster is tap target");
  ArrayList<MapPinLayout.Pin> high=MapPinLayout.build(pair,0,-1,hidden,"","",1,-1900,-1900,500,500,1);
  check(high.size()==2,"zoom splits cluster into real positions");
  hidden.add("Пещера");
  ArrayList<MapPinLayout.Pin> none=MapPinLayout.build(pair,0,-1,hidden,"","",.1f,0,0,500,500,1);
  check(none.isEmpty()&&MapPinLayout.hit(none,205,205,30)==null,"hidden pins cannot be tapped");
  check(MapPinLayout.build(pair,1,-1,new HashSet<>(),"","",.1f,0,0,500,500,1).isEmpty(),"no cross-sheet pins");
  check(MapPinLayout.build(pair,0,4,new HashSet<>(),"","",.1f,0,0,500,500,1).isEmpty(),"region filter");
  ArrayList<MapPinLayout.Pin> player=MapPinLayout.build(pair,0,4,hidden,"a","",.1f,0,0,500,500,1);
  check(player.size()==1&&player.get(0).player,"player survives category filters");
  GameEngine game=new GameEngine();
  TravelPlanner.Route ferry=TravelPlanner.plan(game,WorldAtlas.find("Деревня скаалов"));
  check(ferry.allowed&&ferry.fare==250&&ferry.stops.contains("Воронья Скала"),"validated ferry");
  game.gold=249;check(!TravelPlanner.plan(game,WorldAtlas.find("Деревня скаалов")).allowed,"ferry payment guard");
  game.gold=1000;game.progress.weight=51;
  check(!TravelPlanner.plan(game,WorldAtlas.find("Форт Гринвол")).allowed,"carry limit");
  game.progress.weight=0;
  check(!TravelPlanner.plan(game,WorldAtlas.find("Забытая долина")).allowed,"story portal guard");
  check(!TravelPlanner.plan(game,WorldAtlas.find("Совнгард")).allowed,"otherworld guard");
  game.region=4;game.progress.location="Виндхельм";
  check(TravelPlanner.plan(game,WorldAtlas.find("Деревня скаалов")).stops.get(0).equals("Виндхельм"),"legacy region/location mismatch");
  game.progress.location="Камень Шора";
  check(TravelPlanner.plan(game,WorldAtlas.find("Шорский Камень")).days==0,"old name does not charge travel");
  System.out.println("512 sites: projection, aliases, filtering, zoom, hit targets, ferry, weight and portals OK");
 }
}''')
            compiled=subprocess.run(["javac","-encoding","UTF-8","-d",str(root),*[str(p) for p in root.glob("*.java")]],capture_output=True,text=True)
            self.assertEqual(compiled.returncode,0,compiled.stderr)
            ran=subprocess.run(["java","-cp",str(root),"dev.infend.skyrimtext.Probe"],capture_output=True,text=True)
            self.assertEqual(ran.returncode,0,ran.stdout+ran.stderr)
            self.assertIn("512 sites",ran.stdout)
