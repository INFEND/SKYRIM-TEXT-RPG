"""Smithing guardrails and fixed-stat equipment additions."""
import pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class Tempering(unittest.TestCase):
    def test_quality_and_resources(self):
        p=(ROOT/"RpgProgress.java").read_text()
        for token in ("TEMPER_LEVELS={0,14,31,65,100,134,168}",'quantity(item.id)!=1','!atForge()','smithingQuality()<=temperQuality(item.id)','removeItem(item.temperMaterial(),1)','tempered.put(item.id,next)','new JSONObject(tempered)','optJSONObject("tempered")','if(n<=0)tempered.remove(id)'):
            self.assertIn(token,p)
        self.assertIn('progress.temperBonus(this)',(ROOT/"ItemCatalog.java").read_text())
        self.assertIn('game.progress.temper(item,game)',(ROOT/"MainActivity.java").read_text())
    def test_fixed_stats(self):
        cat=(ROOT/"ItemCatalog.java").read_text()
        self.assertIn('new double[]{40,38,625}',cat)
        self.assertIn('new double[]{32,34,290}',cat)
        self.assertIn('new double[]{40,35,650}',cat)
        self.assertIn('put("Лук Ауриэля","Артефакты","➶",11,1000',cat)
if __name__=="__main__":unittest.main()
