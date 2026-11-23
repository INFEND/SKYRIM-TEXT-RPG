#!/usr/bin/env python3
"""Audit actual Android catalog identities, Russian Wiki evidence and textual uses."""
import argparse
import csv
import gzip
import json
import re
import subprocess
import zipfile
from collections import Counter, defaultdict
from pathlib import Path
from urllib.parse import quote

from localization.build_names import DATA, JAVA, ROOT, evidence, java_source

REGISTRY = DATA / "registry.json.gz"
TEXT = {".java", ".kt", ".json", ".xml", ".txt", ".py", ".md", ".yml", ".yaml", ".gradle", ".properties", ".csv", ".tsv", ".sh", ".kts", ".toml", ".pro", ".html", ".js", ".jsonl"}
GROUPS = {"map": "Локации", "atlas": "Локации", "npc": "NPC", "npc-template": "NPC", "mention": "NPC",
          "weapon": "Оружие", "armor": "Броня", "item": "Предметы", "book": "Книги", "tome": "Книги",
          "scroll": "Предметы", "spell": "Заклинания", "shout": "Крики и расовые таланты",
          "racial-power": "Крики и расовые таланты", "magic-school": "Навыки и перки", "author-power": "Крики и расовые таланты", "skill": "Навыки и перки", "perk": "Навыки и перки",
          "talent-branch": "Навыки и перки", "creature": "Бестиарий", "main-quest": "Задания",
          "faction-quest": "Задания", "faction": "Фракции", "race": "Расы"}
WORD = re.compile(r"[\w]+|[^\w\s]", re.UNICODE)
WIKI = "https://elderscrolls.fandom.com/ru/wiki/"
FOLDER = "app/src/main/java/dev/infend/skyrimtext/"


def catalog():
    with (DATA / "catalog.tsv").open(encoding="utf-8") as stream:
        entries = list(csv.DictReader(stream, delimiter="\t"))
    item_keys = {r["id"] for r in entries if r["kind"] in {"item", "book", "weapon", "armor", "tome", "scroll"}}
    archive = "app/src/main/assets/lore_bundle.zip"
    with zipfile.ZipFile(ROOT / archive) as packed:
        for name in packed.namelist():
            if name.startswith("lore/Способности/") and name.endswith(".txt"):
                entries.append(dict(kind="author-power", id=name, name=Path(name).stem, scope="author",
                                    declaration=archive + "!" + name))
        for item in json.loads(packed.read("catalog/user_foods.json")):
            if item["name"] in item_keys:
                continue
            item_keys.add(item["name"])
            entries.append(dict(kind="item", id=item["name"], name=item["name"], scope="author",
                                declaration=archive + "!catalog/user_foods.json"))
    return entries


def sources():
    command = ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"]
    files = subprocess.check_output(command, cwd=ROOT).decode().split("\0")
    for path in sorted(set(filter(None, files))):
        if path.startswith("tools/localization/") or path in {"tools/audit_localization.py", "docs/WIKI_RU_REGISTRY.md"}:
            continue
        file = ROOT / path
        if not file.is_file() or "/build/" in path or path.startswith("build/"):
            continue
        if file.suffix == ".zip":
            with zipfile.ZipFile(file) as archive:
                for member in archive.namelist():
                    if Path(member).suffix.lower() in TEXT:
                        yield path + "!" + member, archive.read(member).decode("utf-8-sig")
        elif file.suffix.lower() in TEXT and file.name != "RussianNameData.java":
            yield path, file.read_text(encoding="utf-8-sig")


def tokens(s):
    return WORD.findall(s.lower().replace("ё", "е").replace("’", "'"))


# Only extract prose from an explicitly player-facing field. English lookup literals,
# resource names, tests and comments remain searchable under files, outside this queue.
UI_LITERAL = re.compile(r'(?:\b(?:setText|setTitle|setMessage|setHint|addText)\s*\(\s*|'
                        r'\b(?:message|dialogue|intro)\s*=\s*|'
                        r'"(?:dialogue|subtitle|text|description|intro|narrative|goal)"\s*:\s*)"((?:\\.|[^"\\])*)"')
XML_STRING = re.compile(r'<string\b[^>]*>([^<]*)</string>')


def player_literals(path, body):
    if not path.startswith("app/src/main/"):
        return
    if path.endswith((".java", ".kt", ".json")):
        pattern = UI_LITERAL
    elif path.endswith(".xml"):
        pattern = XML_STRING
    else:
        return
    for line, raw in enumerate(body.splitlines(), 1):
        if raw.lstrip().startswith(("//", "*", "/*")):
            continue
        for match in pattern.finditer(raw):
            yield line, match[1]


def find_mentions(words, trie):
    for i, word in enumerate(words):
        node = trie.get(word)
        j = i + 1
        while node is not None:
            for uid, role in node.get(None, ()):
                yield i, j, uid, role
            node = node.get(words[j]) if j < len(words) else None
            j += 1


def collect_uses(records):
    trie = {}
    for uid, rec in records.items():
        labels = [(rec["canonical_ru"], "canonical"), (rec["english"], "english")]
        for use in rec["project"]:
            labels += [(use["canonical_ru"], "canonical"), (use["english"], "english")]
            labels += [(use["name"], "display"), *((a, "historical") for a in use["aliases"])]
        for label, role in labels:
            if not label:
                continue
            node = trie
            for word in tokens(label):
                node = node.setdefault(word, {})
            node.setdefault(None, set()).add((uid, role))
    found = defaultdict(lambda: defaultdict(set))
    scanned = []
    review = set()
    for path, body in sources():
        scanned.append(path)
        for _, _, uid, role in find_mentions(tokens(body), trie):
            found[uid][path].add(role)
        for line, literal in player_literals(path, body):
            words = tokens(literal)
            mentions = list(find_mentions(words, trie))
            covered = {(i, j, uid) for i, j, uid, role in mentions if role == "canonical"}
            for i, j, uid, role in mentions:
                if role not in {"historical", "english"} or records[uid]["status"] not in {"corrected", "verified"}:
                    continue
                if any(other == uid and start <= i and j <= end for start, end, other in covered):
                    continue
                review.add((path, line, uid, role, " ".join(words[i:j])))
    return scanned, found, [dict(file=p, line=n, identity=u, role=r, match=m) for p, n, u, r, m in sorted(review)]



def maps_and_profiles():
    maps = {p["id"]: p for p in json.loads((ROOT / "tools/map_source/compiled.json").read_text())}
    aliases = defaultdict(list)
    site_by_en = {p["name"]: p["id"] for p in maps.values()}
    for name, en in re.findall(r'alias\("([^"]+)","([^"]+)"\)', (JAVA / "SkyrimMapCatalog.java").read_text()):
        if en in site_by_en:
            aliases["map", site_by_en[en]].append(name)
    for old, new in re.findall(r'alias\("([^"]+)","([^"]+)"\)', (JAVA / "BestiaryCatalog.java").read_text()):
        aliases["creature", new].append(old)
    for method, args in re.findall(r'^\s*(add|addTemplate)\((.+?)\);\s*$', (JAVA / "NpcProfiles.java").read_text(), re.M):
        fields = re.findall(r'"([^"]*)"', args)
        if len(fields) >= 4:
            aliases["npc" if method == "add" else "npc-template", fields[0]].extend(fields[4:])
    return maps, aliases


def audit():
    proof = {(e["kind"], e["id"]): e for e in evidence()}
    rows = catalog()
    index = {(r["kind"], r["id"]): r for r in rows}
    if len(index) != len(rows):
        raise ValueError("Duplicate project keys in runtime catalog")
    if missing := proof.keys() - index.keys():
        raise ValueError(f"Evidence refers to absent runtime identities: {sorted(missing)}")
    maps, aliases = maps_and_profiles()
    records = {}
    errors = []
    profiles = {(e["canonical"], e["english"].casefold()): k for k, e in proof.items()
                if k[0] == "creature" and e["english"] and not e["review"]}
    skills = {e["canonical"]: e for e in proof.values() if e["kind"] == "skill" and not e["review"]}
    grouped = sorted(index.items(), key=lambda pair: pair[0][0] in {"npc-template", "magic-school"})
    for (kind, key), row in grouped:
        ev = proof.get((kind, key), {})
        if kind == "magic-school":
            ev = skills.get(row["name"], {})
        alias = list(aliases[kind, key])
        if kind == "npc" and ev.get("english") and not ev.get("review"):
            alias.append(ev["english"])
        if ev.get("prior"):
            alias.append(ev["prior"])
        if kind in {"weapon", "armor", "item", "book", "tome", "scroll"} and key != row["name"]:
            alias.append(key)
        if kind == "npc-template" and not ev:
            # The same creature can appear in two registries. Require both its Russian label
            # and the English creature alias explicitly registered by the NPC template.
            linked = {profiles[row["name"], a.casefold()] for a in alias if (row["name"], a.casefold()) in profiles}
            if len(linked) == 1:
                ev = proof[next(iter(linked))]
        en = ev.get("english") or (maps[key]["name"] if kind == "map" else next(
            (a for a in alias if re.fullmatch(r"[A-Za-z][A-Za-z '\-]+", a)), ""))
        form = ev.get("original_id", "")
        page = ev.get("wiki", "")
        uid = kind + ":" + key
        if ev and not ev.get("review"):
            uid = ("record:" + (ev["edition"] + ":" if form.upper().startswith("XX") else "") + form
                   if form else ev["kind"] + ":wiki:" + page + ":" + en)
        status = ("author" if row["scope"] == "author" else "creation-club" if row["scope"] == "creation-club"
                  else "conflict" if ev.get("review") else "unverified" if not ev
                  else "corrected" if ev["prior"] else "verified")
        if ev and not ev.get("review") and row["name"] != ev["canonical"]:
            errors.append(f"{kind}/{key}: {row['name']} != {ev['canonical']}")
        loc = row["declaration"] if "!" in row["declaration"] else FOLDER + row["declaration"] + ".java"
        ref = WIKI + quote(page.replace(" ", "_"), safe=":'()!,") if page else ""
        if ref and ev.get("revision"):
            ref += "?oldid=" + ev["revision"]
        rec = records.setdefault(uid, dict(identity=uid, category=GROUPS[kind], english=en, original_id=form,
            canonical_ru=ev.get("canonical", "") if not ev.get("review") else "", source=ref,
            source_type="ru-wiki" if ref else "", corroboration=ev.get("corroboration", ""),
            edition=ev.get("edition", ""), status=status, review=ev.get("review", ""), candidate_name=ev.get("canonical", "") if ev.get("review") else "",
            project=[], files={}))
        if rec["status"] == "verified" and status == "corrected":
            rec["status"] = "corrected"
        rec["project"].append(dict(kind=kind, id=key, name=row["name"], canonical_ru=ev.get("canonical", "") if not ev.get("review") else "",
                                   english=en, source=ref, corroboration=ev.get("corroboration", ""), aliases=sorted(set(alias)-{row["name"]}), declaration=loc))
    for record in records.values():
        record["multiple_project_entries"] = len(record["project"]) > 1
        record["conflict"] = record["status"] == "conflict"
        titles = sorted({slot["canonical_ru"] for slot in record["project"] if slot["canonical_ru"]})
        if len(titles) > 1:
            if record["original_id"] not in {"CWObj", "CWFortSiegeFort", "CWSiegeObj"}:
                errors.append("Different labels for one identity without a verified variant: " + record["identity"])
            record["canonical_variants"] = titles
            record["canonical_ru"] = ""
            record["english"] = ""
        if any(slot["kind"] == "race" for slot in record["project"]):
            record["source_type"] = "game-menu-screenshot+ru-wiki"
    scanned, uses, review_queue = collect_uses(records)
    for uid, rec in records.items():
        files = uses[uid]
        for slot in rec["project"]:
            files[slot["declaration"]].add("declaration")
        rec["files"] = {p: sorted(v) for p, v in sorted(files.items())}
    counts = defaultdict(Counter)
    for rec in records.values():
        counts[rec["category"]][rec["status"]] += 1
    report = dict(version=2, display_literals_for_review=review_queue, basis="Project catalog identities, not an exhaustive export of the original game.",
                  counts=dict(sorted(counts.items())), scanned=scanned,
                  entities=[records[key] for key in sorted(records)])
    return report, errors


def markdown_table(counts):
    headings = ["Категория", "Всего", "Проверено", "Исправлено", "Осталось", "Из них конфликтов", "Готовность"]
    lines = ["| " + " | ".join(headings) + " |", "|---|" + "---:|" * (len(headings) - 1)]
    totals = sum((Counter(values) for values in counts.values()), Counter())
    order = dict.fromkeys(GROUPS.values())
    for category in [*order, "**Итого**"]:
        if category != "**Итого**" and category not in counts:
            continue
        values = totals if category == "**Итого**" else Counter(counts[category])
        verified = values["verified"] + values["corrected"]
        pending = values["unverified"] + values["conflict"]
        total = verified + pending
        percent = f"{100 * verified / total:.2f}%".replace(".", ",") if total else "—"
        row = (category, total, verified, values["corrected"], pending, values["conflict"], percent)
        lines.append("| " + " | ".join(map(str, row)) + " |")
    lines.append("| ИИ-генерация | — | — | — | — | — | Открытая проверка текста |")
    return "\n".join(lines)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--live", action="store_true", help="check the live sources without requiring the archived occurrence list to match")
    p.add_argument("--write", action="store_true", help="refresh the checked-in compressed JSON")
    p.add_argument("--json", type=Path, help="also export the complete, human-readable JSON")
    p.add_argument("--markdown", action="store_true", help="print the coverage table by unique original identity")
    args = p.parse_args()
    report, errors = audit()
    if (JAVA / "RussianNameData.java").read_text() != java_source():
        errors.append("Regenerate RussianNameData.java")
    compact = json.dumps(report, ensure_ascii=False, separators=(",", ":")).encode()
    if args.json:
        args.json.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if args.write:
        # Empty filename and fixed timestamp make the committed artifact reproducible.
        with REGISTRY.open("wb") as dest, gzip.GzipFile(filename="", mode="wb", fileobj=dest, mtime=0) as packed:
            packed.write(compact)
    elif not args.live and (not REGISTRY.exists() or gzip.decompress(REGISTRY.read_bytes()) != compact):
        errors.append("The registry is stale; run python tools/audit_localization.py --write")
    if args.markdown:
        print(markdown_table(report["counts"]))
    else:
        print(json.dumps(report["counts"], ensure_ascii=False, indent=2))
        print(f'{len(report["entities"])} identities; {len(report["scanned"])} text resources')
    if errors:
        raise SystemExit("\n".join(errors))


if __name__ == "__main__":
    main()
