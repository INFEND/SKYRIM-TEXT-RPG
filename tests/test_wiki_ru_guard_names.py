"""Wiki evidence for Skyrim guards and Dragonborn Redoran guards."""
import csv, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
LABELS=[
    ("Стражник", "Guard", "Стражник", "Стража (Skyrim)", "Skyrim"),
    ("Стражник Вайтрана", "Whiterun Guard", "Стражник Вайтрана", "Стража (Skyrim)", "Skyrim"),
    ("Стражник Солитьюда", "Solitude Guard", "Стражник Солитьюда", "Стража (Skyrim)", "Skyrim"),
    ("Стражник Виндхельма", "Windhelm Guard", "Стражник Виндхельма", "Стража (Skyrim)", "Skyrim"),
    ("Стражник Рифтена", "Riften Guard", "Стражник Рифтена", "Стража (Skyrim)", "Skyrim"),
    ("Стражник Маркарта", "Markarth Guard", "Стражник Маркарта", "Стража (Skyrim)", "Skyrim"),
    ("Стражник Фолкрита", "Falkreath Guard", "Стражник Фолкрита", "Стража (Skyrim)", "Skyrim"),
    ("Стражник Морфала", "Morthal Guard", "Стражник Морфала", "Стража (Skyrim)", "Skyrim"),
    ("Стражник Данстара", "Dawnstar Guard", "Стражник Данстара", "Стража (Skyrim)", "Skyrim"),
    ("Стражник Винтерхолда", "Winterhold Guard", "Стражник Винтерхолда", "Стража (Skyrim)", "Skyrim"),
    ("Стражник Вороньей Скалы", "Redoran Guard", "Редоранская стража", "Редоранская стража (Dragonborn)", "Dragonborn"),
]
class GuardNames(unittest.TestCase):
    def test_all_eleven_guard_identities_have_corresponding_evidence(self):
        self.assertEqual(11,len(LABELS))
        def load(name):
            with (ROOT/"tools/localization"/name).open(encoding="utf-8",newline="") as src:
                return {(r["kind"],r["id"]):r for r in csv.DictReader(src,delimiter="\t")}
        evidence, catalog=load("evidence.tsv"),load("catalog.tsv")
        for key,en,ru,wiki,edition in LABELS:
            with self.subTest(english=en):
                e=evidence["npc-template",key]
                self.assertEqual(en,e["english"])
                self.assertEqual(ru,e["canonical"])
                self.assertEqual(wiki,e["wiki"])
                self.assertEqual(edition,e["edition"])
                self.assertEqual("",e["review"])
                self.assertEqual(ru,catalog["npc-template",key]["name"])
if __name__=="__main__":unittest.main()
