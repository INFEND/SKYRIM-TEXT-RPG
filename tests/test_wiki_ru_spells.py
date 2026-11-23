"""Canonical Russian spell names must not break historical inventory and save IDs."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ITEMS = ROOT / "app/src/main/java/dev/infend/skyrimtext/ItemCatalog.java"
BOOKS = ROOT / "app/src/main/java/dev/infend/skyrimtext/LibraryItemCatalog.java"

# Names compared against the five Russian The Elder Scrolls Wiki school articles.
SPELL_NAMES = {
    "Пепельный кокон": "Пепельная скорлупа",
    "Хаос": "Катавасия",
    "Вызов костяного": "Вызов костяного человека",
    "Вызов туманного": "Вызов туманного человека",
    "Вызов яростного": "Вызов яростного человека",
    "Реанимация": "Призрак",
    "Изгнание высших даэдра": "Высылка даэдра",
    "Подчинение даэдра": "Приказ даэдра",
    "Грозовой плащ": "Плащ молний",
    "Стена огня": "Стена пламени",
    "Стена молний": "Грозовая стена",
    "Гроза": "Гроза с молниями",
    "Лечащие руки": "Исцеляющие руки",
    "Залечивание ран": "Излечение от ран",
    "Лечение других": "Лечение ближних",
    "Великое лечение": "Высшее лечение",
    "Отпугивание высшей нежити": "Отпугивание старшей нежити",
    "Изгнание младшей нежити": "Отталкивание младшей нежити",
    "Изгнание нежити": "Отталкивание нежити",
    "Круг стража": "Защитный круг",
    "Солнечный огонь": "Огонь солнца",
    "Проклятие вампиров": "Бич вампиров",
}

class RussianSpellTomeNames(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.items = ITEMS.read_text(encoding="utf-8")
        cls.books = BOOKS.read_text(encoding="utf-8")

    def test_all_spell_names_have_exact_canonical_mapping(self):
        case_count = 0
        for old, canonical in SPELL_NAMES.items():
            for prefix in ("Том заклинаний: ", "Свиток: "):
                historical_id = prefix + old
                if 'add("' + historical_id + '"' not in self.books:
                    continue
                with self.subTest(item=historical_id):
                    self.assertIn(
                        'case "' + historical_id + '": return "' + prefix + canonical + '";',
                        self.items)
                case_count += 1
        self.assertEqual(case_count, 24, "An expected spell item disappeared or changed ID")

    def test_historical_save_keys_are_not_rewritten(self):
        for old, canonical in SPELL_NAMES.items():
            for prefix in ("Том заклинаний: ", "Свиток: "):
                if 'add("' + prefix + old + '"' not in self.books:
                    continue
                with self.subTest(item=prefix + old):
                    self.assertNotIn('add("' + prefix + canonical + '"', self.books)
        self.assertIn("items.put(name,new Item(name,visibleName", self.items)
        self.assertIn("DISPLAY_ALIASES.putIfAbsent(visibleName,name)", self.items)
        self.assertIn("String legacyId=DISPLAY_ALIASES.get(id)", self.items)

    def test_spell_learning_description_uses_visible_spell_name(self):
        self.assertIn('String prefix="Том заклинаний: ";', self.items)
        self.assertIn('effect=effect.replace("«"+oldSpell+"»","«"+canonicalSpell+"»");', self.items)
        self.assertIn("put(name,category,icon,weight,cost,desc,effect,stat,skill);", self.items)

if __name__ == "__main__":
    unittest.main()
