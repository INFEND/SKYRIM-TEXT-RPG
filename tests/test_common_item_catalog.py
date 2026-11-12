import pathlib,re,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class CommonCatalogTest(unittest.TestCase):
    def test_new_records(self):
        src=(ROOT/"CommonItemCatalog.java").read_text()
        rows=re.findall(r'add\("([^"]+)","([^"]+)",',src)
        self.assertEqual(len(rows),378)
        self.assertEqual(len({n for n,c in rows}),len(rows))
        for category,count in {"Ключи":223,"Книги":67,"Разное":51,"Квестовые предметы":24,"Одежда":10,"Инструменты":1,"Артефакты":2}.items():
            self.assertEqual(sum(c==category for n,c in rows),count)
        self.assertIn('CommonItemCatalog.register()',(ROOT/"ItemCatalog.java").read_text())
        self.assertIn('if(!items.containsKey(name))put',(ROOT/"ItemCatalog.java").read_text())
        self.assertEqual(len(re.findall(r'"Ключи",0,0,',src)),223)
if __name__=="__main__":unittest.main()
