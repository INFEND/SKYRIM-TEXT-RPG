from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"

class KatherineInventoryPortalTest(unittest.TestCase):
    def test_healing_synchronizes_inventory(self):
        source = (JAVA / "KatherineCompanion.java").read_text(encoding="utf-8")
        heal = source.split('case "KATH:heal":', 1)[1].split("return true;", 1)[0]
        self.assertIn("g.potions>0", heal)
        self.assertIn("g.potions--;hp=Math.min(maxHp(),hp+40);g.progress.sync(g);", heal)

    def test_portal_registers_same_discovery_as_travel(self):
        source = (JAVA / "KatherineCompanion.java").read_text(encoding="utf-8")
        portal = source.split('case "KATH:portal":', 1)[1].split("return true;", 1)[0]
        self.assertIn('g.sceneState.confirm("sublocation", "atlas_" + Integer.toHexString(p.name.hashCode()), p.name, "VISITED")', portal)
        self.assertIn("catch (org.json.JSONException ignored)", portal)
        self.assertIn("!portalUsed&&k.mana>=35", portal)
        self.assertIn("k.mana-=35;portalUsed=true;", portal)

if __name__ == "__main__":
    unittest.main()
