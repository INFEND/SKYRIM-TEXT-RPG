import pathlib, re, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class NpcChildProfiles(unittest.TestCase):
    def test_child_profiles_and_safe_menu(self):
        s=(ROOT/"app/src/main/java/dev/infend/skyrimtext/NpcProfiles.java").read_text(encoding="utf-8")
        ui=(ROOT/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text(encoding="utf-8")
        self.assertGreaterEqual(len(re.findall(r'add\("[^"]+","[^"]+","[^"]+","Ребёнок \|',s)), 40)
        self.assertIn('child=role.startsWith("Ребёнок |")',s)
        self.assertIn('if(profile.child)return npcEmblem',ui)
        self.assertIn('child?new int[]{0,2,6,11}',ui)
        self.assertIn('add.add("Фура Моррар"',(ROOT/"app/src/main/java/dev/infend/skyrimtext/LoreNpcProfiles.java").read_text(encoding="utf-8"))
