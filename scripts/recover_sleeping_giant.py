#!/usr/bin/env python3
"""Recover verified original Sleeping Giant Inn frame without re-encoding."""
import hashlib,json,pathlib,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
def sha(b):return hashlib.sha256(b).hexdigest()
def main():
    history=ROOT/"artwork_history/gallery/riverwood_extra.zip"
    report=json.loads((ROOT/"docs/gallery_expansion_compression.json").read_text())
    old=next(p for p in report["packs"] if p["archive"]=="riverwood_extra.zip")
    frame=old["photos"][0]
    assert sha(history.read_bytes())==old["archive_sha256"] and frame["number"]==6
    with zipfile.ZipFile(history) as z:
        assert z.testzip() is None
        data=z.read("06.webp")
    assert sha(data)==frame["sha256"] and len(data)==frame["encoded_bytes"]
    target=ROOT/"app/src/main/assets/gallery/sleeping_giant_inn.zip"
    with zipfile.ZipFile(target,"w",compression=zipfile.ZIP_STORED) as z:
        item=zipfile.ZipInfo("01.webp",date_time=(2026,11,16,0,0,0))
        z.writestr(item,data)
    with zipfile.ZipFile(target) as z:assert z.testzip() is None and z.read("01.webp")==data
    registry=ROOT/"docs/gallery_registry.json"
    entries=json.loads(registry.read_text())
    entries=[e for e in entries if e["slug"]!="sleeping_giant_inn"]
    entries.append({"name":"Таверна «Спящий великан»","slug":"sleeping_giant_inn","parent":"Ривервуд","aliases":["Спящий великан","Sleeping Giant Inn","Таверна Спящий великан"],"captions":["Общий зал и центральный очаг"]})
    registry.write_text(json.dumps(entries,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    out={"source_archive":str(history.relative_to(ROOT)),"source_member":"06.webp","source_image_sha256":frame["sha256"],"archive":str(target.relative_to(ROOT)),"archive_sha256":sha(target.read_bytes()),"archive_bytes":target.stat().st_size,"image_bytes":len(data),"dimensions":frame["dimensions"],"quality":frame["quality"],"full_resolution_luma_ssim":frame["full_resolution_luma_ssim"],"reencoded":False}
    (ROOT/"docs/sleeping_giant_recovery.json").write_text(json.dumps(out,ensure_ascii=False,indent=2)+"\n")
    print(json.dumps(out,ensure_ascii=False))
if __name__=="__main__":main()
