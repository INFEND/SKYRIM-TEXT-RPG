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
