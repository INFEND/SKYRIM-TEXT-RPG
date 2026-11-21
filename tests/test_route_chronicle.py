import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class RouteChronicle(unittest.TestCase):
    def test_route_only_confirmed(self):
        ui=(ROOT/"MainActivity.java").read_text();g=(ROOT/"GameEngine.java").read_text();m=(ROOT/"SkyrimTerrainMapView.java").read_text()
        self.assertIn('after.progress.location.equals(before.stops.get(before.stops.size()-1))',ui)
        self.assertIn('a.sheet!=sheet||b.sheet!=sheet',m)
        self.assertIn('latestVisualRoute()',g)
        self.assertIn('drawVisualRoute(canvas)',m)
        self.assertIn('chronicleDetails(entry)',ui)
        self.assertIn('entry.put("details",details)',g)
