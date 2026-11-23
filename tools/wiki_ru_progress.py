#!/usr/bin/env python3
"""Conservative, reproducible audit of individually Wiki-checked Skyrim RU labels.

This is a *verification registry*, not a claim that unreviewed Russian labels
are wrong. Procedurally generated equipment, custom mods and freeform AI output
are outside the static direct-registration denominator.
"""
from __future__ import annotations
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"
def source(filename: str) -> str:
    return (JAVA / filename).read_text(encoding="utf-8")

MAP_VERIFIED = {
    "Goldenglow Estate": "Поместье «Златоцвет»",
    "The Guardian Stones": "Камни-хранители",
    "Kynesgrove": "Роща Кин",
    "Shor's Stone": "Камень Шора",
    "Sarethi Farm": "Ферма Сарети",
    "Broken Oar Grotto": "Грот Сломанное Весло",
    "Mzinchaleft": "Мзинчалефт",
    "Nightgate Inn": "Таверна «Ночные ворота»",
    "Mixwater Mill": "Лесопилка «Пирожок»",
    "Darkwater Pass": "Чёрный проход",
    "Duskglow Crevice": "Закатная расселина",
    "Brinewater Grotto": "Грот Солёная Вода",
    "Reachwater Rock": "Озёрный утёс",
    "Shadowgreen Cavern": "Пещера Зелёная Тень",
    "Knifepoint Ridge": "Хребет Остриё Ножа",
}
# Exact NPC names in the Russian Wiki's "Вайтран (Skyrim)" resident list.
WHITERUN_VERIFIED = (
    "Балгруф Старший","Дагни","Фротар","Нелкир","Хронгар",
    "Фаренгар Тайный Огонь","Провентус Авениччи","Айрилет",
    "Легат Квентин Сципий","Фианна","Герда","Командир Кай",
    "Кодлак Белая Грива","Скьор","Эйла Охотница","Фаркас","Вилкас",
    "Атис","Ньяда Каменная Рука","Торвар","Рия",
    "Вигнар Серая Грива","Брилл","Тильма Усталая","Белетор","Сигурд",
    "Адрианна Авениччи","Ульфберт Разъярённый Медведь","Аркадия",
    "Йорлунд Серая Грива","Алем","Анориат","Элриндир","Дженасса",
    "Назим","Хульда","Микаэль","Садия","Синмир",
    "Утгерд Несломленная","Даника Свет Весны","Морис Жондрель",
    "Аколит Йенссен","Андурс","Лидия",
)
BESTIARY_VERIFIED = (
    "Ледяной тролль","Ледяное привидение","Дракон-скелет",
    "Тролль в броне","Гаргулья","Гончая смерти","Туманный человек",
    "Яростный человек","Медведь-оборотень","Горелый спригган",
    "Порождение пепла",
)
# Original main-story quest names per Wiki's «На свободу!» navigation.
MAIN_QUEST_VERIFIED = (
    "На свободу!","Перед бурей","Ветреный пик","Дракон в небе",
    "Путь Голоса","Рог Юргена","Клинок во тьме",
    "Дипломатическая неприкосновенность","Крыса, загнанная в угол",
    "Стена Алдуина","Глотка Мира","Древнее знание",
    "Проклятие Алдуина","Падший","Бесконечная пора",
    "Дом Пожирателя мира","Совнгард","Драконоборец",
)
# Already correctly named reference artifacts (not renamed by this project).
EXISTING_ITEM_VERIFIED = (
    "Сияние Рассвета","Бритва Мерунеса","Булава Молаг Бала",
    "Лук Ауриэля","Шкура Спасителя","Вутрад",
)

def quoted_array(java: str, name: str) -> list[str]:
    pattern = r"public static final String\[\] " + re.escape(name) + r"=\{(.*?)\};"
    match = re.search(pattern, java, re.S)
    if match is None:
        raise AssertionError("Cannot locate array " + name)
    return re.findall(r'"([^"]*)"', match.group(1))

def counts() -> dict[str, dict[str, int | float]]:
    map_entries = {
        fields[2]: fields[1]
        for line in source("SkyrimMapPoints.java").splitlines()
        if (match := re.match(r'\s*"((?:skyrim|solstheim)-[^"]+)"', line))
        for fields in [match.group(1).split("|")]
    }
    npc_text = source("NpcProfiles.java")
    npc_entries = re.findall(r'^\s*add\("([^"]+)"', npc_text, re.M)
    npc_templates = re.findall(r'^\s*addTemplate\("([^"]+)"', npc_text, re.M)
    bestiary_text = source("BestiaryCatalog.java")
    beast_entries = re.findall(r'^\s*[abd]\([a-z]+,"([^"|]+)\|', bestiary_text, re.M)
    beast_entries += re.findall(r'^\s*o\("([^"|]+)\|', bestiary_text, re.M)
    beast_entries += re.findall(r'^\s*add\(men,GAME,"([^"|]+)\|', bestiary_text, re.M)
    catalog_files = (
        "ItemCatalog.java","WorldItemCatalog.java","ApparelAndPotionCatalog.java",
        "CommonItemCatalog.java","LibraryItemCatalog.java",
    )
    direct = set()
    for filename in catalog_files:
        direct.update(re.findall(r'^\s*(?:add|put|registerMissing)\("([^"]+)"',
                                 source(filename), re.M))
    magic = {item for item in direct
             if item.startswith(("Том заклинаний: ","Свиток: "))}
    other_items = direct - magic
    mappings = dict(re.findall(r'case "([^"]+)": return "([^"]+)";',
                               source("ItemCatalog.java")))
    magic_verified = magic.intersection(mappings)
    item_verified = other_items.intersection(mappings).union(
        other_items.intersection(EXISTING_ITEM_VERIFIED))
    rpg = source("RpgContent.java")
    races = quoted_array(rpg, "RACES")
    skills = quoted_array(rpg, "SKILLS")
    quest_titles = re.findall(r'new Stage\("([^"]+)"', source("CanonicalCampaign.java"))
    # Fail loudly when previously Wiki-checked canon labels disappear.
    for english, canonical in MAP_VERIFIED.items():
        assert map_entries.get(english) == canonical, (english, canonical)
    assert set(WHITERUN_VERIFIED).issubset(npc_entries), "NPC Wiki registry drift"
    assert set(BESTIARY_VERIFIED).issubset(beast_entries), "Bestiary Wiki registry drift"
    assert set(MAIN_QUEST_VERIFIED).issubset(quest_titles), "Quest labels drift"
    assert len(races) == 10 and len(skills) == 18
    assert len(magic_verified) == 24, "Spell-to-tome alias mappings drift"

    raw = {
        "Карта": (len(map_entries), len(MAP_VERIFIED)),
        "Именные NPC": (len(npc_entries), len(WHITERUN_VERIFIED)),
        "Бестиарий": (len(beast_entries), len(BESTIARY_VERIFIED)),
        "Предметы (прямые регистрации)": (len(other_items), len(item_verified)),
        "Тома и свитки": (len(magic), len(magic_verified)),
        "Расы и навыки": (len(races) + len(skills), len(races) + len(skills)),
        "Сюжетные этапы": (len(quest_titles), len(MAIN_QUEST_VERIFIED)),
    }
    out = {}
    for key, (total, verified) in raw.items():
        assert 0 <= verified <= total, (key, total, verified)
        out[key] = {
            "total": total,
            "wiki_verified": verified,
            "not_individually_checked": total - verified,
            "verified_percent": round(100 * verified / total, 1) if total else 0,
            "unchecked_percent": round(100 * (total - verified) / total, 1) if total else 0,
        }
    total = sum(x["total"] for x in out.values())
    verified = sum(x["wiki_verified"] for x in out.values())
    out["ИТОГО (учитываемый набор)"] = {
        "total": total, "wiki_verified": verified,
        "not_individually_checked": total - verified,
        "verified_percent": round(100 * verified / total, 1),
        "unchecked_percent": round(100 * (total - verified) / total, 1),
    }
    # Templates are measured separately: these are intentionally NOT canonically
    # named people; they describe generic guards, bandits and monsters.
    out["Шаблоны NPC (вне итога)"] = {
        "total": len(npc_templates), "wiki_verified": 0,
        "not_individually_checked": len(npc_templates),
        "verified_percent": 0, "unchecked_percent": 100,
    }
    return out

def main():
    report = counts()
    print("КОНСЕРВАТИВНЫЙ РЕЕСТР СВЕРКИ С РУССКОЙ SKYRIM WIKI")
    print("Категория | Всего | Сверено | Пока без отдельной сверки | % сверено")
    for category, row in report.items():
        print(f"{category} | {row['total']} | {row['wiki_verified']} | "
              f"{row['not_individually_checked']} | {row['verified_percent']:.1f}%")
    print("\nВАЖНО: это покрытие ПОШТУЧНО ДОКУМЕНТИРОВАННОЙ СВЕРКОЙ, "
          "не процент неправильных имён и не процент переведённого интерфейса.")
    print("Не входят процедурно сгенерированные комплекты снаряжения, "
          "авторские названия, свободный текст ИИ и иные Java-каталоги.")
    path = ROOT / "docs/wiki_ru_progress_latest.json"
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n",
                    encoding="utf-8")

if __name__ == "__main__":
    main()
