import pathlib,re,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class ApparelPotionCatalogTest(unittest.TestCase):
    def test_four_categories(self):
        s=(ROOT/"ApparelAndPotionCatalog.java").read_text()
        rows=re.findall(r'add\("([^"]+)","([^"]+)",',s)
        self.assertEqual(len(rows),339)
        self.assertEqual(len(set(n for n,c in rows)),len(rows))
        for cat,count in {"Одежда":93,"Украшения":56,"Инструменты":10,"Зелья":180}.items():
            self.assertEqual(sum(c==cat for n,c in rows),count)
        for name in ("Кирка","Древняя нордская кирка","Этериевая корона","Одеяние Телванни (Нелот)","Идеальное зелье лечения","Зелье крови","Зелье хождения по воде"):
            self.assertTrue(any(n==name for n,c in rows))
        self.assertIn("ApparelAndPotionCatalog.register()",(ROOT/"ItemCatalog.java").read_text())
if __name__=="__main__":unittest.main()
