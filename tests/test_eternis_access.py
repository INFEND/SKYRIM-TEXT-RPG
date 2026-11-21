import pathlib, unittest

JAVA = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/java/dev/infend/skyrimtext/MainActivity.java"

class EternisAccessTest(unittest.TestCase):
    def setUp(self):
        self.source = JAVA.read_text(encoding="utf-8")

    def test_shop_opens_even_during_active_actions(self):
        opening = self.source.split("private void livingFixShop(){", 1)[1].split("private void fixRenderOffers", 1)[0]
        self.assertIn("if(!game.started)", opening)
        self.assertNotIn("if(!game.started||livingBusy())", opening)
        self.assertIn("Режим просмотра", opening)
        self.assertIn("fixRenderOffers(items)", opening)
        self.assertIn("fixRenderLedger(items)", opening)

    def test_read_only_details_and_mutation_guards(self):
        detail = self.source.split("private void fixOpenOffer(", 1)[1].split("private void livingFixShop", 1)[0]
        self.assertIn("if(!livingBusy()){offer.action.run();return;}", detail)
        self.assertIn("Пока доступен только просмотр", detail)
        self.assertIn("buy.setOnClickListener(v->fixOpenOffer(offer))", self.source)
        self.assertIn("art.setOnClickListener(v->fixOpenOffer(offer))", self.source)
        for mutation in ("game.fixShop.toggle(offer.id())", "game.fixShop.redeem(game,index)", "game.fixShop.refund(game,index)"):
            line = next(x for x in self.source.splitlines() if mutation in x and "fixShopCanChange()" in x)
            self.assertLess(line.index("fixShopCanChange()"), line.index(mutation))
        self.assertIn("if(!fixShopCanChange())return false;if(price<=0", self.source)

    def test_reason_covers_all_existing_locks(self):
        reason = self.source.split("private String fixShopLockReason(){", 1)[1].split("private boolean fixShopCanChange", 1)[0]
        for condition in ("game.solitude.custody()", "aiTurnBusy", "pendingAiAction()", '"COMBAT".equals(game.mode)', "game.road.pending()"):
            self.assertIn(condition, reason)
