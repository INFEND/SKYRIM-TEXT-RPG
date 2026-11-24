#!/usr/bin/env python3
"""Release gate: both legacy download names are byte-identical lightweight APKs."""
import hashlib,json,pathlib,zipfile
from title_art_gate import verify_title_art
ROOT=pathlib.Path(__file__).resolve().parents[1]
apk=ROOT/"dist/SKYRIM-TEXT-RPG.apk"
alias=ROOT/"dist/SKYRIM-TEXT-RPG-thin.apk"
assert hashlib.sha256(apk.read_bytes()).digest()==hashlib.sha256(alias.read_bytes()).digest()
core={"assets/icons/action_choices_283.webp","assets/icons/skyrim_icons_48.webp","assets/lore_bundle.zip","assets/fonts/Sovngarde Light.ttf","assets/fonts/Sovngarde Bold.ttf","assets/fonts/OFL.txt","assets/resource_manifest.json"}
font_spec=json.loads((ROOT/"app/font-choices.json").read_text(encoding="utf-8"))
font_entries=[entry for family in font_spec["families"] for entry in family["files"]]
with zipfile.ZipFile(apk) as z:
    names=set(z.namelist())
    manifest=json.loads(z.read("assets/resource_manifest.json"))
    assert manifest["bundled"] is False
    assert len(manifest["files"])>=89
    for item in manifest["files"]:
        assert "assets/"+item["path"] not in names,item["path"]
    external={e["path"]:e for e in manifest["files"]}
    for entry in font_entries:
        assert "assets/"+entry["asset"] not in names
        assert external[entry["asset"]]["size"]==entry["size"]
        assert len(external[entry["asset"]]["sha256"])==64
    # Menu covers are explicitly bundled by build_resource_catalog.py for offline startup.
    # Verify their exact content before exempting these six paths from the media ban.
    core |= verify_title_art(z, ROOT/"app/src/main/assets")
    assert core<=names
    # The approved action atlas is a required, small offline UI resource, not optional media.
    action_path="assets/icons/action_choices_283.webp"
    action_source=ROOT/"app/src/main/assets/icons/action_choices_283.webp"
    action_data=z.read(action_path)
    assert 0<len(action_data)<=384*1024,action_path
    assert action_data[:4]==b"RIFF" and action_data[8:12]==b"WEBP",action_path
    assert hashlib.sha256(action_data).digest()==hashlib.sha256(action_source.read_bytes()).digest(),action_path
    action_index="assets/icons/action_choices_283.tsv"
    assert len(z.read(action_index).decode("utf-8").splitlines())==284,action_index
    assert "assets/icons/action_choices_LICENSE.txt" in names
    engine=ROOT/"dist/SKYRIM-LOCAL-AI.apk"
    module=json.loads(z.read("assets/engine_module.json"))
    assert module["size"]==engine.stat().st_size and module["sha256"]==hashlib.sha256(engine.read_bytes()).hexdigest()
    assert not any(n.startswith("lib/") and n.endswith(".so") for n in names), "Native libraries leaked into Ultra Lite"
    with zipfile.ZipFile(engine) as native:
        assert any(n.startswith("lib/arm64-v8a/") and n.endswith(".so") for n in native.namelist())
    for info in z.infolist():
        if info.is_dir() or not info.filename.startswith("assets/") or info.filename in core:continue
        assert info.file_size<=64*1024 and not info.filename.lower().endswith((".webp",".png",".jpg",".jpeg",".zip",".mp4",".ogg",".mp3",".gguf")),info.filename
    portraits=json.loads((ROOT/"docs/portrait_compression_report.json").read_text())
    entries={e["path"]:e for e in manifest["files"]}
    assert len(portraits["portraits"])==60
    for p in portraits["portraits"]:
        path="portraits/"+p["file"]
        e=entries[path]
        assert e["size"]==p["encoded_bytes"] and e["sha256"]==p["sha256"],path
    assert "AndroidManifest.xml" in names and "classes.dex" in names
print(f"Verified lightweight APK {apk.stat().st_size} bytes; {len(manifest['files'])} external resources")
