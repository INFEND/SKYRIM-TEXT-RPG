import struct
import sys
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
import zlib

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from localization import official_names as official


def field(tag, value):
    return struct.pack("<4sH", tag, len(value)) + value


def record(tag, flags, form, payload):
    return struct.pack("<4sIII8x", tag, len(payload), flags, form) + payload


def localized_master(path, masters, objects):
    header = record(b"TES4", 0x80, 0, b"".join(field(b"MAST", n.encode() + b"\0") for n in masters))
    entries = b""
    for kind, flags, form, editor, string_id in objects:
        data = field(b"EDID", editor.encode() + b"\0") + field(b"FULL", struct.pack("<I", string_id))
        if flags & 0x40000:
            data = struct.pack("<I", len(data)) + zlib.compress(data)
        entries += record(kind, flags, form, data)
    path.write_bytes(header + struct.pack("<4sI16x", b"GRUP", 24 + len(entries)) + entries)


def table(path, value):
    data = value.encode("cp1251") + b"\0"
    path.write_bytes(struct.pack("<IIII", 1, len(data), 72, 0) + data)


class OfficialGameNames(unittest.TestCase):
    def test_individual_dragons_are_never_verified_by_their_shared_base(self):
        actor = dict(identity="dragon", category="NPC", original_id="XX01D572", edition="Dragonborn",
                     actor_reference={"base_id": "XX03D5B5"}, project=[{"name": "Релоникив"}])
        inventory = {"dragonborn.esm:03D5B5": {"type": "NPC_", "editor_id": "", "name": "Дракон"}}
        result = official.compare({"entities": [actor]}, inventory)[0]
        self.assertEqual("dragonborn.esm:01D572", result["original_identity"])
        self.assertEqual("reference-name-requires-review", result["status"])

    def test_words_use_separate_localized_strings_for_name_and_meaning(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            master, strings = root / "Skyrim.esm", root / "Skyrim_Russian.STRINGS"
            name, meaning = "Фус".encode("cp1251") + b"\0", "Сила".encode("cp1251") + b"\0"
            strings.write_bytes(struct.pack("<IIIIII", 2, len(name + meaning), 9, 0, 10, len(name)) + name + meaning)
            word = field(b"FULL", struct.pack("<I", 9)) + field(b"TNAM", struct.pack("<I", 10))
            master.write_bytes(record(b"TES4", 0x80, 0, b"") + record(b"WOOP", 0, 0x13E22, word))
            inv = {}
            official.read_plugin(master, strings, inv)
            self.assertEqual(10, inv["skyrim.esm:013E22"]["translation_id"])
            row = dict(identity="fus", category="Слова силы", original_id="00013E22",
                       project=[{"name": "Фус"}], word_of_power={"meaning": "Сила"})
            result = official.compare({"entities": [row]}, inv)[0]
            self.assertEqual("matches-official", result["status"])
            row["word_of_power"]["meaning"] = "Иное слово"
            result = official.compare({"entities": [row]}, inv)[0]
            self.assertEqual("matches-official", result["word_name_status"])
            self.assertEqual("different", result["status"])
            row["word_of_power"]["meaning"] = ""
            self.assertEqual("needs-review", official.compare({"entities": [row]}, inv)[0]["meaning_status"])

    def test_overrides_keep_form_origin_and_use_the_winning_plugins_string(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            inv = {}
            for name, masters, entries, ru in (
                ("Skyrim.esm", [], [(b"NPC_", 0, 0x17, "Actor", 72)], "Прежнее"),
                ("Update.esm", ["Skyrim.esm"], [(b"NPC_", 0x40000, 0x17, "Actor", 72)], "Новое"),
                ("Dragonborn.esm", ["Skyrim.esm", "Update.esm"], [(b"BOOK", 0, 0x02000017, "Readable", 72)], "Книга"),
            ):
                master = root / name
                strings = root / (master.stem + "_Russian.STRINGS")
                localized_master(master, masters, entries)
                table(strings, ru)
                official.read_plugin(master, strings, inv)
            self.assertEqual({"skyrim.esm:000017", "dragonborn.esm:000017"}, set(inv))
            self.assertEqual("Новое", inv["skyrim.esm:000017"]["name"])
            self.assertEqual("Update.esm", inv["skyrim.esm:000017"]["master"])
            self.assertEqual("Книга", inv["dragonborn.esm:000017"]["name"])
            data = {"entities": [dict(identity="a", original_id="XX000017", category="Книги",
                                      edition="Dragonborn", project=[{"name": "Книга"}])]}
            self.assertEqual("matches-official", official.compare(data, inv)[0]["status"])
            data["entities"][0]["edition"] = ""
            self.assertEqual("unmapped", official.compare(data, inv)[0]["status"])

    def test_deleted_and_unknown_strings_do_not_reuse_previous_translation(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            m = root / "Skyrim.esm"
            t = root / "Skyrim_Russian.STRINGS"
            localized_master(m, [], [(b"QUST", 0, 2, "Missing", 999), (b"NPC_", 0x20, 1, "Old", 72)])
            table(t, "Пример")
            inv = {"skyrim.esm:000001": {"name": "Удалён"}}
            official.read_plugin(m, t, inv)
            self.assertNotIn("skyrim.esm:000001", inv)
            self.assertIsNone(inv["skyrim.esm:000002"]["name"])
            data = {"entities": [dict(identity="q", original_id="Missing", category="Задания", project=[])]}
            self.assertEqual("missing-string", official.compare(data, inv)[0]["status"])
            inv["dragonborn.esm:000001"] = dict(inv["skyrim.esm:000002"])
            self.assertEqual("ambiguous-editor-id", official.compare(data, inv)[0]["status"])
