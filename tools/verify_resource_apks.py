#!/usr/bin/env python3
"""Release gate: both legacy download names are byte-identical lightweight APKs."""
import hashlib,json,pathlib,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
apk=ROOT/"dist/SKYRIM-TEXT-RPG.apk"
alias=ROOT/"dist/SKYRIM-TEXT-RPG-thin.apk"
assert hashlib.sha256(apk.read_bytes()).digest()==hashlib.sha256(alias.read_bytes()).digest()
core={"assets/icons/skyrim_icons_48.webp","assets/lore_bundle.zip","assets/fonts/Sovngarde Light.ttf","assets/fonts/Sovngarde Bold.ttf","assets/fonts/OFL.txt","assets/resource_manifest.json"}
font_spec=json.loads((ROOT/"app/font-choices.json").read_text(encoding="utf-8"))
font_entries=[entry for family in font_spec["families"] for entry in family["files"]]
core.update("assets/"+entry["asset"] for entry in font_entries)
with zipfile.ZipFile(apk) as z:
    for entry in font_entries:
        raw=z.read("assets/"+entry["asset"])
        assert len(raw)==entry["size"],entry["asset"]
        assert hashlib.sha1(("blob "+str(len(raw))+"\0").encode()+raw).hexdigest()==entry["gitSha"],entry["asset"]
    names=set(z.namelist())
    manifest=json.loads(z.read("assets/resource_manifest.json"))
    assert manifest["bundled"] is False
    assert len(manifest["files"])>=89
    for item in manifest["files"]:
        assert "assets/"+item["path"] not in names,item["path"]
    assert core<=names
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
