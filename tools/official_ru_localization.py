#!/usr/bin/env python3
"""Audit Skyrim's official language files and build a PRIVATE Russian dialogue pack.

This utility never downloads, bundles, or redistributes Bethesda's text or audio.
Input files must come from the user's own installation. Dialogue speaker/stage
associations must be verified independently; string IDs alone do not identify actors.
"""
from __future__ import annotations

import argparse
import json
import re
import struct
from collections import defaultdict
from pathlib import Path

MAX_FILE_BYTES = 128 * 1024 * 1024
MAX_STORY_BYTES = 4 * 1024 * 1024
MAX_SEGMENTS = 5000
MAX_CHAPTERS = 33
SOURCE_PATTERN = re.compile(r"^[A-Za-z][A-Za-z0-9_]*$")
STAGE_PATTERN = re.compile(r'new Stage\("([^"]+)",')


class ImportErrorDetail(ValueError):
    """Actionable data validation failure."""


def read_strings(path: Path, encoding: str = "cp1251") -> dict[int, str]:
    """Read Bethesda .STRINGS, .ILSTRINGS and .DLSTRINGS binary string tables."""
    ext = path.suffix.upper()
    if ext not in (".STRINGS", ".ILSTRINGS", ".DLSTRINGS"):
        raise ImportErrorDetail(f"Unsupported string-table extension: {path}")
    if not path.is_file():
        raise ImportErrorDetail(f"Required game file is absent: {path}")
    data = path.read_bytes()
    if len(data) < 8 or len(data) > MAX_FILE_BYTES:
        raise ImportErrorDetail(f"Invalid file size: {path}")
    count, data_size = struct.unpack_from("<II", data, 0)
    table_end = 8 + count * 8
    if count > 1_000_000 or table_end > len(data) or data_size != len(data) - table_end:
        raise ImportErrorDetail(f"Invalid table header/size: {path}")
    entries: dict[int, str] = {}
    for i in range(count):
        entry_id, offset = struct.unpack_from("<II", data, 8 + i * 8)
        start = table_end + offset
        if entry_id in entries or offset >= data_size:
            raise ImportErrorDetail(f"Duplicate ID or invalid offset at entry {i}: {path}")
        if ext == ".STRINGS":
            end = data.find(b"\0", start)
            if end < start or end >= len(data):
                raise ImportErrorDetail(f"Unterminated STRINGS entry {entry_id}: {path}")
            payload = data[start:end]
        else:
            if start + 4 > len(data):
                raise ImportErrorDetail(f"Truncated length at entry {entry_id}: {path}")
            length = struct.unpack_from("<I", data, start)[0]
            if length == 0 or start + 4 + length > len(data):
                raise ImportErrorDetail(f"Invalid length at entry {entry_id}: {path}")
            payload = data[start + 4:start + 4 + length].rstrip(b"\0")
        try:
            entries[entry_id] = payload.decode(encoding, errors="strict").strip()
        except UnicodeDecodeError as exc:
            raise ImportErrorDetail(f"Bad {encoding} encoding in {path}, ID {entry_id}") from exc
    return entries


def parse_id(value: object) -> int:
    if isinstance(value, bool):
        raise ImportErrorDetail("String ID must be a decimal integer or 0x hexadecimal value")
    try:
        raw = str(value).strip()
        number = int(raw, 16 if raw.lower().startswith("0x") else 10)
        if not 0 <= number <= 0xFFFFFFFF:
            raise ValueError("Out of range")
        return number
    except (ValueError, TypeError) as exc:
        raise ImportErrorDetail(f"Invalid 32-bit string ID: {value!r}") from exc


def source_file(directory: Path, source: str, lang: str, ext: str) -> Path:
    if not SOURCE_PATTERN.fullmatch(source):
        raise ImportErrorDetail(f"Unsafe or unknown source name: {source!r}")
    return directory / f"{source}_{lang}.{ext}"


def stage_titles(java_file: Path) -> list[str]:
    titles = STAGE_PATTERN.findall(java_file.read_text(encoding="utf-8"))
    if len(titles) != MAX_CHAPTERS:
        raise ImportErrorDetail(
            f"Expected {MAX_CHAPTERS} stages in {java_file}, got {len(titles)}"
        )
    return titles


def build_story(strings_dir: Path, java_file: Path, manifest: Path, output: Path) -> dict:
    """Build the format understood by StoryDialoguePack.parse and CanonicalCampaign."""
    spec = json.loads(manifest.read_text(encoding="utf-8"))
    rows = spec.get("segments") if isinstance(spec, dict) else None
    if not isinstance(rows, list) or not 33 <= len(rows) <= MAX_SEGMENTS:
        raise ImportErrorDetail("Manifest needs 33..5000 verified dialogue segments")
    titles = stage_titles(java_file)
    cache: dict[str, dict[int, str]] = {}
    seen_chapters: set[int] = set()
    segments = []
    for position, row in enumerate(rows, start=1):
        if not isinstance(row, dict):
            raise ImportErrorDetail(f"Segment {position}: expected an object")
        chapter = row.get("scene_id")
        if type(chapter) is not int or not 1 <= chapter <= MAX_CHAPTERS:
            raise ImportErrorDetail(f"Segment {position}: invalid scene_id")
        source = row.get("source", "Skyrim")
        if not isinstance(source, str) or not SOURCE_PATTERN.fullmatch(source):
            raise ImportErrorDetail(f"Segment {position}: invalid source")
        speaker = row.get("speaker", "")
        if not isinstance(speaker, str) or not speaker.strip() or len(speaker.strip()) > 250:
            raise ImportErrorDetail(f"Segment {position}: verify the speaker name")
        source_line = row.get("line_start")
        if type(source_line) is not int or source_line < 0:
            raise ImportErrorDetail(f"Segment {position}: line_start must be verified")
        string_id = parse_id(row.get("string_id"))
        if source not in cache:
            cache[source] = read_strings(source_file(strings_dir, source, "Russian", "DLSTRINGS"))
        text = cache[source].get(string_id, "")
        if not text or len(text) > 8192:
            raise ImportErrorDetail(
                f"Segment {position}: missing/invalid {source}_Russian.DLSTRINGS ID {string_id}"
            )
        if row.get("exclude_from_reading", False) is not True:
            seen_chapters.add(chapter)
        segments.append({
            "id": position,
            "scene_id": chapter,
            "line_start": source_line,
            "speaker": speaker.strip(),
            "reading_text": text,
            "requires_manual_review": row.get("requires_manual_review", False) is True,
            "exclude_from_reading": row.get("exclude_from_reading", False) is True,
        })
    if seen_chapters != set(range(1, MAX_CHAPTERS + 1)):
        missing = sorted(set(range(1, MAX_CHAPTERS + 1)) - seen_chapters)
        raise ImportErrorDetail(f"Every campaign stage needs a verified line; missing: {missing}")
    pack = {
        "provenance": "user_supplied_official_russian_game_files",
        "scenes": [{"id": i + 1, "title": title} for i, title in enumerate(titles)],
        "segments": segments,
    }
    output_bytes = json.dumps(pack, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    if len(output_bytes) > MAX_STORY_BYTES:
        raise ImportErrorDetail("Private dialogue pack exceeds StoryDialoguePack.MAX_BYTES")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(output_bytes)
    return {"chapters": len(titles), "segments": len(segments), "bytes": len(output_bytes),
            "manual_review": sum(s["requires_manual_review"] for s in segments)}


def normal_name(value: str) -> str:
    return re.sub(r"\s+", " ", value.strip()).casefold().replace("ё", "е")


def audit_names(strings_dir: Path, names_path: Path, output: Path) -> dict:
    """Compare an existing map glossary against paired official STRING IDs.

    An absent/mismatched entry is only a review candidate, never an automatic rename.
    """
    official: dict[str, set[tuple[str, str, int]]] = defaultdict(set)
    paired_sources = []
    for english_file in sorted(strings_dir.glob("*_English.STRINGS")):
        source = english_file.name[: -len("_English.STRINGS")]
        russian_file = source_file(strings_dir, source, "Russian", "STRINGS")
        if not russian_file.is_file():
            continue
        en = read_strings(english_file, "cp1252")
        ru = read_strings(russian_file)
        paired_sources.append(source)
        for entry_id, english in en.items():
            translated = ru.get(entry_id, "").strip()
            if english.strip() and translated:
                official[normal_name(english)].add((translated, source, entry_id))
    if not paired_sources:
        raise ImportErrorDetail("No paired *_English.STRINGS / *_Russian.STRINGS files")
    results = []
    for number, line in enumerate(names_path.read_text(encoding="utf-8-sig").splitlines(), 1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if line.count("|") != 1:
            raise ImportErrorDetail(f"Invalid glossary line {number}: expected English|Russian")
        raw_en, game_ru = (part.strip() for part in line.split("|", 1))
        english = re.sub(r"\s+\((?:DB|DG|HF|CC)\)$", "", raw_en, flags=re.I)
        candidates = sorted(official.get(normal_name(english), set()))
        distinct = {normal_name(name) for name, _, _ in candidates}
        status = ("not_found" if not candidates else
                  "ambiguous" if len(distinct) > 1 else
                  "verified" if normal_name(game_ru) in distinct else "needs_review")
        results.append({
            "line": number, "english": raw_en, "game_russian": game_ru, "status": status,
            "official_candidates": [
                {"text": name, "source": source, "string_id": entry_id}
                for name, source, entry_id in candidates
            ],
        })
    counts = {key: sum(row["status"] == key for row in results)
              for key in ("verified", "needs_review", "ambiguous", "not_found")}
    payload = {"sources": paired_sources, "totals": counts, "entries": results}
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return counts


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    story = sub.add_parser("build-story", help="Build a PRIVATE original-dialogue pack")
    story.add_argument("--strings-dir", required=True, type=Path)
    story.add_argument("--manifest", required=True, type=Path)
    story.add_argument("--output", required=True, type=Path)
    story.add_argument("--canonical-java", type=Path,
                       default=Path("app/src/main/java/dev/infend/skyrimtext/CanonicalCampaign.java"))
    audit = sub.add_parser("audit-names", help="Compare game names without renaming guesses")
    audit.add_argument("--strings-dir", required=True, type=Path)
    audit.add_argument("--names", type=Path, default=Path("tools/map_names_ru.txt"))
    audit.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        if args.command == "build-story":
            result = build_story(args.strings_dir, args.canonical_java, args.manifest, args.output)
        else:
            result = audit_names(args.strings_dir, args.names, args.output)
    except (ImportErrorDetail, OSError, ValueError, KeyError, TypeError) as exc:
        parser.exit(2, f"Official Russian localization: {exc}\n")
    print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
