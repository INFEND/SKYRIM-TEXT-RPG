import pathlib,re,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class WorldItemsTest(unittest.TestCase):
    def test_catalog(self):
        s=(ROOT/"WorldItemCatalog.java").read_text()
        rows=re.findall(r'add\("([^"]+)","([^"]+)",',s)
        self.assertEqual(len(rows),358)
        self.assertEqual(len(set(n for n,c in rows)),len(rows))
        for category,count in {"Ингредиенты":111,"Материалы":46,"Драгоценности":12,"Камни душ":12,"Артефакты":2,"Книги":91,"Свитки":15,"Оружие":29,"Броня":25,"Боеприпасы":1,"Еда":9,"Напитки":5}.items():
            self.assertEqual(sum(c==category for n,c in rows),count)
        self.assertIn("WorldItemCatalog.register()",(ROOT/"ItemCatalog.java").read_text())
if __name__=="__main__":unittest.main()
