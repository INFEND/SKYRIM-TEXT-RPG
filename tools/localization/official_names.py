#!/usr/bin/env python3
"""Compare project evidence to localized FULL fields from a local, unmodified Skyrim installation."""
import argparse
import gzip
import json
import mmap
import struct
import zlib
from collections import Counter, defaultdict
from pathlib import Path

PLUGINS = ("Skyrim.esm", "Update.esm", "Dawnguard.esm", "HearthFires.esm", "Dragonborn.esm")
DLC = {"Dawnguard": "Dawnguard.esm", "Dragonborn": "Dragonborn.esm", "Hearthfire": "HearthFires.esm"}
TYPES = set("WRLD CELL LCTN REFR NPC_ WEAP ARMO AMMO MISC BOOK ALCH INGR SLGM KEYM SCRL SPEL MGEF "
            "ENCH PERK AVIF SHOU WOOP RACE QUST FACT RELA CLAS ACTI CONT DOOR FLOR FURN".split())
HEADER = 24


def u32(data, offset):
    return struct.unpack_from("<I", data, offset)[0]


def decode(data):
    try:
        return data.decode("utf-8")
    except UnicodeDecodeError:
        return data.decode("cp1251")


def localized_strings(path):
    data = path.read_bytes()
    if len(data) < 8:
        raise ValueError(f"Truncated strings header: {path}")
    count, size = struct.unpack_from("<II", data)
    start = 8 + 8 * count
    if start + size != len(data):
        raise ValueError(f"Invalid strings directory: {path}")
    strings = {}
    for index in range(count):
        key, offset = struct.unpack_from("<II", data, 8 + 8 * index)
        end = data.find(b"\0", start + offset)
        if offset >= size or end < 0 or key in strings:
            raise ValueError(f"Invalid string ID or offset in {path}: {key}")
        strings[key] = decode(data[start + offset:end])
    return strings


def fields(payload):
    offset = 0
    extended = None
    while offset < len(payload):
        if offset + 6 > len(payload):
            raise ValueError("Truncated subrecord header")
        tag, size = struct.unpack_from("<4sH", payload, offset)
        offset += 6
        if extended is not None:
            size, extended = extended, None
        if offset + size > len(payload):
            raise ValueError("Subrecord outside its record")
        value = payload[offset:offset + size]
        offset += size
        if tag == b"XXXX":
            if size != 4:
                raise ValueError("Invalid extended field size")
            extended = u32(value, 0)
        else:
            yield tag, value
    if extended is not None:
        raise ValueError("Extended size without a field")


def records(data):
    position = 0
    ends = [len(data)]
    while ends:
        if position == ends[-1]:
            ends.pop()
            continue
        if position + HEADER > ends[-1]:
            raise ValueError("Invalid master record header")
        tag, size, flags, form = struct.unpack_from("<4sIII", data, position)
        if tag == b"GRUP":
            if size < HEADER or position + size > ends[-1]:
                raise ValueError("Invalid master group size")
            ends.append(position + size)
            position += HEADER
            continue
        position += HEADER
        end = position + size
        if end > ends[-1]:
            raise ValueError("Record exceeds its group")
        kind = tag.decode("ascii")
        if kind == "TES4" or kind in TYPES:
            payload = data[position:end]
            if flags & 0x40000:
                raw = zlib.decompress(payload[4:])
                if len(raw) != u32(payload, 0):
                    raise ValueError("Incorrect uncompressed record size")
                payload = raw
            yield kind, flags, form, payload
        position = end


def read_plugin(path, strings_path, inventory):
    strings = localized_strings(strings_path)
    with path.open("rb") as file, mmap.mmap(file.fileno(), 0, access=mmap.ACCESS_READ) as data:
        stream = records(data)
        tag, flags, _, first = next(stream)
        if tag != "TES4" or not flags & 0x80:
            raise ValueError(f"Expected a localized Skyrim master: {path.name}")
        masters = [v.rstrip(b"\0").decode("ascii") for k, v in fields(first) if k == b"MAST"]
        masters.append(path.name)
        for kind, flags, form, payload in stream:
            owner = masters[form >> 24]
            identity = owner.casefold() + ":" + f"{form & 0xFFFFFF:06X}"
            inventory.pop(identity, None)
            if flags & 0x20:
                continue
            sub = {}
            for key, value in fields(payload):
                if key in (b"EDID", b"FULL", b"XMRK"):
                    sub.setdefault(key, value)
            full = sub.get(b"FULL", b"")
            if not full or (kind == "REFR" and b"XMRK" not in sub):
                continue
            if len(full) != 4:
                raise ValueError(f"Invalid localized FULL in {path.name} for {identity}")
            string_id = u32(full, 0)
            if string_id:
                inventory[identity] = dict(identity=identity, type=kind,
                    editor_id=sub.get(b"EDID", b"").rstrip(b"\0").decode("ascii"),
                    name=strings.get(string_id), string_id=string_id,
                    master=path.name, strings=strings_path.name)


def compare(registry, inventory):
    quests = defaultdict(list)
    for identity, value in inventory.items():
        if value["type"] == "QUST" and value["editor_id"]:
            quests[value["editor_id"].casefold()].append(identity)
    results = []
    for row in registry["entities"]:
        form = row["original_id"]
        ambiguous = False
        if len(form) == 8 and all(c in "0123456789abcdef" for c in form[2:].lower()):
            # An arbitrary load order does not establish which plugin owns a record.
            prefix = form[:2].upper()
            owner = "Skyrim.esm" if prefix == "00" else DLC.get(row.get("edition"))
            match = owner.casefold() + ":" + form[2:].upper() if owner else None
        elif row["category"] == "Задания" and form:
            candidates = quests[form.casefold()]
            ambiguous = len(candidates) > 1
            match = candidates[0] if len(candidates) == 1 else None
        else:
            match = None
        original = inventory.get(match)
        if ambiguous:
            state = "ambiguous-editor-id"
        elif original is None:
            state = "unmapped"
        elif not original["name"]:
            state = "missing-string"
        elif row.get("canonical_variants") or "<" in original["name"]:
            state = "parameterized-title"
        elif all(v["name"] == original["name"] for v in row["project"]):
            state = "matches-official"
        else:
            state = "different"
        results.append(dict(project_identity=row["identity"], original_identity=match, status=state))
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data", required=True, type=Path, help="directory containing the five original ESM files")
    parser.add_argument("--strings", required=True, type=Path, help="directory with Russian .STRINGS extracted from the installation")
    parser.add_argument("--registry", type=Path, default=Path(__file__).with_name("registry.json.gz"))
    parser.add_argument("--out", required=True, type=Path, help="local JSON comparison and named record inventory")
    args = parser.parse_args()
    masters = {p.name.casefold(): p for p in args.data.iterdir() if p.is_file()}
    tables = {p.name.casefold(): p for p in args.strings.iterdir() if p.is_file()}
    inventory = {}
    for plugin in PLUGINS:
        file = masters.get(plugin.casefold())
        name = Path(plugin).stem + "_Russian.STRINGS"
        table = tables.get(name.casefold())
        if not file or not table:
            parser.error(f"Missing original input: {plugin if not file else name}")
        read_plugin(file, table, inventory)
    registry = json.loads(gzip.decompress(args.registry.read_bytes()))
    results = compare(registry, inventory)
    counts = Counter(r["status"] for r in results)
    args.out.write_text(json.dumps(dict(counts=counts, project=results, original=inventory),
                                   ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(dict(counts))


if __name__ == "__main__":
    main()
