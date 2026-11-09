#!/usr/bin/env python3
"""Compile a pinned snapshot of geographic facts, never synthetic grid positions.

Run locally after reviewing tools/map_source and map_names_ru.txt. The APK does
not download map data. Positions remain in the source game's world coordinates.
"""
import json
import math
import pathlib
import re
import itertools

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tools/map_source"
OUT = ROOT / "app/src/main/java/dev/infend/skyrimtext"
names = json.loads((SOURCE / "reference_names_ru.json").read_text())
for line in (ROOT / "tools/map_names_ru.txt").read_text().splitlines():
    if line.strip():
        en, ru = line.split("|", 1)
        names[en] = ru
deities = {"Akatosh":"Акатоша", "Arkay":"Аркея", "Dibella":"Дибеллы",
           "Julianos":"Джулианоса", "Kynareth":"Кинарет", "Mara":"Мары",
           "Stendarr":"Стендарра", "Talos":"Талоса", "Zenithar":"Зенитара"}
mounds = {
    "Bilegulch Ridge":"Желчный хребет", "Bloodlet Peaks":"Кровавые пики",
    "Evergreen Woods":"Вечнозелёные леса", "Great Henge Resurrection":"Большой кромлех",
    "Karth River Forest":"Лес у реки Карт", "Karthspire Bluffs":"Утёсы Картспайра",
    "Lone Mountain":"Одинокая гора", "Ragnvald Vale":"Долина Рагнвальда",
    "Reachwater Pass":"Озёрный перевал", "Robber's Gorge Bluffs":"Утёсы Теснины грабителя",
    "Rorikstead Resurrection":"Рорикстед", "Yorgrim Resurrection":"Йоргрим",
    "Frozen Shoals":"Замёрзшие отмели", "Temple Foothills":"Подножие храма"}
types = {
    "aedric-shrine":"Святилище", "all-maker-stone":"Камень", "camp":"Лагерь",
    "castle":"Дворец", "cave":"Пещера", "city":"Столица", "clearing":"Поляна",
    "college":"Коллегия", "daedric-shrine":"Святилище", "dock":"Причал",
    "dragon-lair":"Логово дракона", "dragon-mound":"Драконий курган",
    "dwarven-ruin":"Двемерские руины", "farm":"Ферма", "fort":"Форт",
    "giant-camp":"Лагерь великанов", "grove":"Роща", "imperial-camp":"Имперский лагерь",
    "landmark":"Достопримечательность", "lighthouse":"Маяк", "mine":"Шахта",
    "nordic-dwelling":"Руины", "nordic-ruin":"Руины", "nordic-tower":"Башня",
    "orc-stronghold":"Поселение орков", "pass":"Переход", "settlement":"Поселение",
    "shack":"Дом", "shipwreck":"Кораблекрушение", "stable":"Конюшня",
    "standing-stone":"Камень", "stormcloak-camp":"Лагерь Братьев Бури",
    "telvanni-tower":"Башня", "temple":"Храм", "to-skyrim":"Переход",
    "to-solstheim":"Причал", "town":"Деревня", "watchtower":"Башня",
    "wheat-mill":"Ферма", "wood-mill":"Лесопилка", "region":"Озеро"}
# Existing save/story keys keep their spelling, even when a reference differs.
legacy = {
    "Shor's Stone":"Шорский Камень", "The Guardian Stones":"Камень Хранитель",
    "Mzinchaleft":"Руины Мзинчалефт", "Dark Brotherhood Sanctuary":"Святилище Тёмного Братства",
    "Kynesgrove":"Кинсгроув", "Shrine of Azura":"Святилище Азуры",
    "Shrine of Mehrunes Dagon":"Святилище Мерунеса Дагона",
    "Sacellum of Boethiah":"Святилище Боэтии", "Solitude Lighthouse":"Маяк Солитьюда",
    "Fort Dunstad":"Форт Данстад", "Sarethi Farm":"Саретийская ферма",
    "Hob's Fall Cave":"Пещера Хоба", "Broken Oar Grotto":"Сломанное Весло",
    "Kolbjorn Barrow":"Колбьорн", "Frostmoon Crag":"Ущелье Фростмуна"}

def translate(en):
    if en.startswith("Shrine of "):
        match = re.fullmatch(r"Shrine of (\w+)( \d+)?", en)
        if match and match[1] in deities:
            return "Святилище " + deities[match[1]] + (match[2] or "")
    if en.startswith("Dragon Mound"):
        ending = en[len("Dragon Mound"):].strip(": ")
        return "Драконий курган " + mounds.get(ending, ending)
    if en not in names:
        raise ValueError("Missing reviewed Russian name: " + en)
    return legacy.get(en, names[en]).replace('\\"', '«', 1).replace('\\"', '»').replace('"','')

main = json.loads((SOURCE / "skyrim.json").read_text())
island = json.loads((SOURCE / "solstheim.json").read_text())
capitals = ["Whiterun","Riften","Windhelm","Solitude","Markarth","Falkreath","Winterhold","Dawnstar","Morthal"]
hubs = [next(p for p in main if p["name"]==name) for name in capitals]
# Travel districts use the existing game's nine transport hubs. They are an
# estimate for route costs, not a claim of authoritative hold-border polygons.
def district(p, sheet):
    if sheet: return 9
    n=p["name"]
    if n in capitals:return capitals.index(n)
    if n in {"Helgen","Fort Neugrad","Half-Moon Mill","Lakeview Manor (HF)"}:return 5
    if n in {"Ivarstead","Shor's Stone","Sarethi Farm","Largashbur","Fort Dawnguard (DG)","Dayspring Canyon (DG)"}:return 1
    if n in {"College of Winterhold","Shrine of Azura","Saarthal"}:return 6
    if n in {"Labyrinthian","Ustengrav","Windstad Manor (HF)"}:return 8
    if n in {"Fort Dunstad","Heljarchen Hall (HF)","Shrine of Mehrunes Dagon"}:return 7
    if n in {"High Hrothgar","Throat of the World","Riverwood","Bleak Falls Barrow","Rorikstead"}:return 0
    return min(range(9),key=lambda i:math.hypot(p["x"]-hubs[i]["x"],p["y"]-hubs[i]["y"]))

points=[]
seen=set()
for sheet, collection in enumerate((main,island)):
    for p in collection:
        category=p["category"]
        if category not in types:continue
        if p["name"]=="2920, Morning Star, v1":continue # item misclassified by upstream
        if category=="region" and not p["name"].startswith("Lake "):continue
        key=(sheet,p["name"],p["x"],p["y"])
        if key in seen:continue # same war camp appears under both factions
        seen.add(key)
        kind=types[category]
        if p["name"] in {"High Hrothgar","Sky Haven Temple"}:kind="Святилище"
        if p["name"] in {"Darkwater Crossing","Stonehills"}:kind="Поселение"
        if p["name"]=="Throat of the World":kind="Гора"
        ru=translate(p["name"])
        if p["name"].endswith(" Military Camp"):
            ru += " — " + ("Империя" if category=="imperial-camp" else "Братья Бури")
        if sheet and p["name"].startswith("Shrine of "):ru += " (Солстейм)"
        points.append(dict(p,nameRu=ru,type=kind,sheet=sheet,region=district(p,sheet)))

def key(name):return name.casefold().replace("’", "'")
known={key(p["name"]):p for p in points if p["sheet"]==1}
anchors=[]
for ref in json.loads((SOURCE/"solstheim_projection.json").read_text()):
    point=known.get(key(ref["name"]))
    if point is None:continue
    lon,lat=ref["coordinates"][:2]
    px=(lon+180)/360*2048
    py=(1-math.asinh(math.tan(math.radians(lat)))/math.pi)/2*2048
    anchors.append((point["x"],point["y"],px,py))
    point.update(displayX=px,displayY=py,pinSource="anchor")
assert len(anchors)>=40,"Too few independent Solstheim calibration anchors"

def interpolate(x,y):
    # A tilted terrain view is not an orthographic map. Use the nearest
    # containing triangle of published control points, never random offsets.
    best=None
    for a,b,c in itertools.combinations(anchors,3):
        denominator=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
        if abs(denominator)<1:continue
        wa=((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/denominator
        wb=((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/denominator
        wc=1-wa-wb
        distance=sum((p[0]-x)**2+(p[1]-y)**2 for p in (a,b,c))
        outside=max(0,-wa,-wb,-wc)
        score=distance*(1+10000*outside)
        rank=(outside>1e-6,score)
        if best is None or rank<best[0]:
            best=(rank,wa*a[2]+wb*b[2]+wc*c[2],wa*a[3]+wb*b[3]+wc*c[3])
    return best[1],best[2]

for point in points:
    if point["sheet"]==0:
        point.update(displayX=2048+point["x"]/128,displayY=2048-point["y"]/128,pinSource="geographic")
    elif "displayX" not in point:
        x,y=interpolate(point["x"],point["y"])
        point.update(displayX=x,displayY=y,pinSource="interpolated")
    edge=4096 if point["sheet"]==0 else 2048
    assert 0<=point["displayX"]<=edge and 0<=point["displayY"]<=edge

rows=[]
for p in points:
    row="|".join(str(x) for x in (p["id"],p["nameRu"],p["name"],p["type"],p["sheet"],p["x"],p["y"],p["region"],round(p["displayX"],4),round(p["displayY"],4),p["pinSource"]))
    rows.append("        "+json.dumps(row,ensure_ascii=False)+",")
java="package dev.infend.skyrimtext;\n\n/** Generated by tools/build_geographic_atlas.py from reviewed source facts. */\nfinal class SkyrimMapPoints {\n    static final String[] ROWS={\n"+"\n".join(rows)+"\n    };\n    private SkyrimMapPoints(){}\n}\n"
(OUT/"SkyrimMapPoints.java").write_text(java)
(SOURCE/"compiled.json").write_text(json.dumps(points,ensure_ascii=False,indent=2)+"\n")
print("Compiled",len(points),"geographic locations:",sum(p["sheet"]==0 for p in points),"Skyrim,",sum(p["sheet"]==1 for p in points),"Solstheim")
