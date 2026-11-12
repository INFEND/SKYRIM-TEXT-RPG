#!/usr/bin/env python3
"""Verify actual release APK contents before publishing either artifact."""
import hashlib,json,pathlib,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
full=ROOT/"dist/SKYRIM-TEXT-RPG.apk"
thin=ROOT/"dist/SKYRIM-TEXT-RPG-thin.apk"
with zipfile.ZipFile(full) as a,zipfile.ZipFile(thin) as b:
    c=json.loads(a.read("assets/resource_manifest.json"))
    d=json.loads(b.read("assets/resource_manifest.json"))
    assert c["bundled"] is True and d["bundled"] is False
    assert c["commit"]==d["commit"] and c["files"]==d["files"]
    assert len(c["files"])>=29
    for item in c["files"]:
        path="assets/"+item["path"]
        data=a.read(path)
        assert len(data)==item["size"] and hashlib.sha256(data).hexdigest()==item["sha256"],path
        assert path not in b.namelist(),path
    for path in ("assets/icons/skyrim_icons_48.webp","assets/lore_bundle.zip","assets/fonts/Sovngarde Light.ttf","assets/fonts/Sovngarde Bold.ttf","assets/fonts/OFL.txt","AndroidManifest.xml","classes.dex"):
        assert a.read(path)==b.read(path),path
assert thin.stat().st_size<full.stat().st_size
print(f"Verified full {full.stat().st_size} bytes, thin {thin.stat().st_size} bytes")
