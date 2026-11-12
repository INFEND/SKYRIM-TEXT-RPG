import pathlib,re,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class LibraryItemTest(unittest.TestCase):
    def test_entries(self):
        src=(ROOT/"LibraryItemCatalog.java").read_text()
        rows=re.findall(r'add\("([^"]+)","([^"]+)",',src)
        self.assertEqual(len(rows),191)
        self.assertEqual(len({n for n,c in rows}),len(rows))
        for category,count in {"Тома заклинаний":107,"Книги":35,"Свитки":21,"Разное":24,"Квестовые предметы":4}.items():
            self.assertEqual(sum(c==category for n,c in rows),count)
        self.assertIn("LibraryItemCatalog.register()",(ROOT/"ItemCatalog.java").read_text())
        for school in ("Изменение","Иллюзия","Колдовство","Разрушение","Восстановление"):
            self.assertIn(school+" | обучение",src)
if __name__=="__main__":unittest.main()
