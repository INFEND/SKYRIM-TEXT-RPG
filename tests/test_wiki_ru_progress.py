"""Measured Wiki audit coverage must be reproducible and fail on source drift."""
import importlib.util
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
SPEC=importlib.util.spec_from_file_location("wiki_ru_progress", ROOT/"tools/wiki_ru_progress.py")
WIKI=importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(WIKI)

class WikiRuProgressContracts(unittest.TestCase):
    def test_source_registry_consistent_and_conservative(self):
        report=WIKI.counts()
        self.assertEqual(report["Карта"]["total"],515)
        self.assertEqual(report["Карта"]["wiki_verified"],15)
        self.assertEqual(report["Именные NPC"]["wiki_verified"],125)
        self.assertEqual(report["Бестиарий"]["wiki_verified"],11)
        self.assertEqual(report["Тома и свитки"]["wiki_verified"],24)
        self.assertEqual(report["Расы и навыки"]["wiki_verified"],28)
        self.assertEqual(report["Сюжетные этапы"]["total"],33)
        self.assertEqual(report["Сюжетные этапы"]["wiki_verified"],18)
        self.assertEqual(report["Шаблоны NPC (вне итога)"]["total"],206)
        overall=report["ИТОГО (учитываемый набор)"]
        categories=[value for key,value in report.items() if not key.startswith((
            "ИТОГО","Шаблоны"))]
        self.assertEqual(sum(x["total"] for x in categories),overall["total"])
        self.assertEqual(sum(x["wiki_verified"] for x in categories),overall["wiki_verified"])
        self.assertEqual(overall["total"]-overall["wiki_verified"],
                         overall["not_individually_checked"])
        self.assertLess(overall["verified_percent"],100.0,
                        "Unreviewed items must not be labeled translated and audited")

    def test_verified_npc_count_is_not_claiming_all_templates(self):
        report=WIKI.counts()
        self.assertGreater(report["Именные NPC"]["total"],1000)
        self.assertEqual(len(WIKI.WHITERUN_VERIFIED),45)
        self.assertEqual(len(WIKI.SOLITUDE_VERIFIED),80)
        self.assertFalse(set(WIKI.WHITERUN_VERIFIED)&set(WIKI.SOLITUDE_VERIFIED))
        self.assertEqual(len(WIKI.MAP_VERIFIED),15)
        self.assertEqual(len(WIKI.MAIN_QUEST_VERIFIED),18)

if __name__=="__main__":
    unittest.main()
