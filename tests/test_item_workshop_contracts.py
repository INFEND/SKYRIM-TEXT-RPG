"""Contracts for manual, Work-backed and visual custom item creation."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1] / "app/src/main/java/dev/infend/skyrimtext"
ITEMS=(ROOT/"CompensationPowers.java").read_text(encoding="utf-8")
CATALOG=(ROOT/"ItemCatalog.java").read_text(encoding="utf-8")
MAIN=(ROOT/"MainActivity.java").read_text(encoding="utf-8")
AI=(ROOT/"ItemWorkshopAi.java").read_text(encoding="utf-8")
ART=(ROOT/"ItemArtworkStorage.java").read_text(encoding="utf-8")
TYPES=(ROOT/"ItemWorkshopSpec.java").read_text(encoding="utf-8")

class ExpandedItemWorkshop(unittest.TestCase):
    def test_legacy_ids_preserved(self):
        self.assertIn('ITEMS={"Клинок северной зари","Кираса сумеречного стража","Кольцо зимнего пламени"}',ITEMS)
        self.assertIn('return i<3?ITEMS[i]:"fix_custom_"+i;',ITEMS)
        self.assertIn('public static final int SLOTS=12;',ITEMS)
        self.assertIn('public static final int ITEM_SLOTS=64;',ITEMS)

    def test_types_are_extended_without_changing_legacy_first_three(self):
        self.assertIn('"Одноручное оружие","Кираса","Кольцо"',TYPES)
        for category in ('"Двуручное оружие"','"Щит"','"Посох"',
                         '"Сапоги"','"Ожерелье"','"Зелье"','"Инструмент"'):
            self.assertIn(category,TYPES)
        self.assertIn('ItemWorkshopSpec.SLOT[kind]',CATALOG)

    def test_metadata_survives_saves_and_inventory_registration(self):
        for key in ('itemValue','itemRarity','itemEffects','itemArtPrompts','itemArtPaths'):
            self.assertIn('.put("'+key+'"',ITEMS)
            self.assertIn('str(j,"'+key+'"',ITEMS) if key.startswith("itemA") or key=="itemEffects" else self.assertIn('num(j,"'+key+'"',ITEMS)
        self.assertIn('ItemCatalog.registerCustom(itemId(i)',ITEMS)
        self.assertIn('game.compensation.attachArtwork(slot,path',MAIN)

    def test_ai_suggestions_never_directly_commit_game_state(self):
        self.assertIn('chatGptOAuth.getToken',MAIN)
        self.assertIn('ItemWorkshopAi.draft(token,model,prompt',MAIN)
        self.assertIn('setPositiveButton(editing?"Сохранить":"Создать за 50 F"',MAIN)
        self.assertIn('game.compensation.createDetailedItem',MAIN)
        self.assertNotIn('GameEngine',AI)
        self.assertIn('put("store",false).put("stream",true)',AI)
        self.assertIn('response.completed',AI)

    def test_images_use_work_and_manual_import_not_paid_api(self):
        self.assertNotIn('/v1/images/generations', AI)
        self.assertNotIn('AiKeyVault.read(app)', AI)
        self.assertNotIn('Платная генерация изображения', MAIN)
        self.assertNotIn('ГЕНЕРИРОВАТЬ ЧЕРЕЗ API', MAIN)
        self.assertIn('WorkImageEditor.open(this,"item"', MAIN)
        self.assertIn('new File(context.getFilesDir(),"item_art")', ART)
        self.assertIn('Bitmap.CompressFormat.WEBP', ART)

    def test_import_and_item_card_image_are_wired(self):
        self.assertIn('Intent.ACTION_OPEN_DOCUMENT',MAIN)
        self.assertIn('REQUEST_ITEM_ARTWORK',MAIN)
        self.assertIn('ItemArtworkStorage.preview(this',MAIN)
        self.assertIn('Редактировать предмет',MAIN)
        self.assertIn('Изображение предмета',MAIN)

if __name__=="__main__": unittest.main()
