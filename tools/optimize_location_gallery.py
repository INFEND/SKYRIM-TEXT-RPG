#!/usr/bin/env python3
"""Encode only new location artwork; preserve previously shipped archives.
Requires Pillow, numpy and scipy. See gallery_expansion_compression.json for
full-resolution detail checks, color error, byte counts and source hashes.
"""
import concurrent.futures
import hashlib
import io
import json
import pathlib
import sys
import zipfile
import numpy as np
from PIL import Image, ImageOps
from scipy.ndimage import gaussian_filter

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/gallery"
REPORT = ROOT / "docs/gallery_expansion_compression.json"
STEPS = (76, 80, 84, 88, 92, 96, 100)
FLOOR = {"full_resolution_luma_ssim": .985, "worst_128px_luma_ssim": .965,
         "maximum_chroma_mean_absolute_error": 2.5,
         "maximum_chroma_95th_percentile_error": 7}

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def ssim_map(left, right):
    left, right = left.astype(np.float32), right.astype(np.float32)
    a = gaussian_filter(left, 1.5, truncate=3.5)
    b = gaussian_filter(right, 1.5, truncate=3.5)
    va = gaussian_filter(left*left, 1.5, truncate=3.5)-a*a
    vb = gaussian_filter(right*right, 1.5, truncate=3.5)-b*b
    cov = gaussian_filter(left*right, 1.5, truncate=3.5)-a*b
    score = ((2*a*b+6.5025)*(2*cov+58.5225))/(
        (a*a+b*b+6.5025)*(va+vb+58.5225))
    return score[6:-6, 6:-6]

def scores(source, decoded):
    a, b = np.asarray(source.convert("YCbCr")), np.asarray(decoded.convert("YCbCr"))
    full = ssim_map(a[:,:,0], b[:,:,0])
    patches = [float(full[y:y+128,x:x+128].mean())
               for y in range(0, full.shape[0]-127, 128)
               for x in range(0, full.shape[1]-127, 128)]
    chroma = np.abs(a[:,:,1:].astype(np.int16)-b[:,:,1:].astype(np.int16))
    return {"full_resolution_luma_ssim": float(full.mean()),
            "worst_128px_luma_ssim": min(patches),
            "chroma_mean_absolute_error": float(chroma.mean()),
            "chroma_95th_percentile_error": float(np.percentile(chroma,95))}

def passes(m):
    return (m["full_resolution_luma_ssim"] >= FLOOR["full_resolution_luma_ssim"]
            and m["worst_128px_luma_ssim"] >= FLOOR["worst_128px_luma_ssim"]
            and m["chroma_mean_absolute_error"] <= FLOOR["maximum_chroma_mean_absolute_error"]
            and m["chroma_95th_percentile_error"] <= FLOOR["maximum_chroma_95th_percentile_error"])

def encode(row):
    path = ROOT / row["source"]
    raw = path.read_bytes()
    git_hash = hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
    if git_hash != row["source_git_sha"]:
        raise ValueError("Original source hash mismatch: "+row["source"])
    with Image.open(io.BytesIO(raw)) as opened:
        image = ImageOps.exif_transpose(opened).convert("RGB")
    original_size = image.size
    image.thumbnail((1920,1080), Image.Resampling.LANCZOS)
    trials = []
    for quality in STEPS:
        stream = io.BytesIO()
        image.save(stream,"WEBP",quality=quality,method=6)
        data = stream.getvalue()
        with Image.open(io.BytesIO(data)) as decoded:
            m = scores(image,decoded)
        trials.append({"quality":quality,"bytes":len(data),**m})
        if passes(m): break
    else:
        stream = io.BytesIO()
        image.save(stream,"WEBP",lossless=True,method=6)
        data, quality = stream.getvalue(), "lossless"
        with Image.open(io.BytesIO(data)) as decoded: m = scores(image,decoded)
        if not passes(m): raise ValueError("Quality floor not reached")
    if len(data) >= 2_000_000: raise ValueError("Android image decode limit exceeded")
    info = {"number":row["number"],"caption":row["caption"],
            "source_git_sha":git_hash,"original_sha256":hashlib.sha256(raw).hexdigest(),
            "original_bytes":len(raw),"source_dimensions":original_size,
            "dimensions":image.size,"encoded_bytes":len(data),"quality":quality,
            "sha256":hashlib.sha256(data).hexdigest(),**m,"trials":trials}
    return row["slug"],data,info

def main():
    rows = json.loads((ROOT/sys.argv[1]).read_text(encoding="utf-8"))
    assert len({(r["slug"],r["number"]) for r in rows}) == len(rows)
    report = json.loads(REPORT.read_text()) if REPORT.exists() else {"packs":[]}
    before = {p.name:digest(p) for p in ASSETS.glob("*.zip")}
    baseline = report.get("previous_archives_sha256",before)
    completed = {}
    for pack in report["packs"]:
        assert digest(ASSETS/pack["archive"]) == pack["archive_sha256"]
        for photo in pack["photos"]:
            completed[(pack["slug"],photo["number"])] = photo["source_git_sha"]
    pending = []
    for row in rows:
        key = (row["slug"],row["number"])
        if key in completed:
            assert completed[key] == row["source_git_sha"], key
        else: pending.append(row)
    encoded = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        for slug,data,info in pool.map(encode,pending):
            encoded.setdefault(slug,[]).append((data,info))
            print(f'{slug}/{info["number"]:02d}: q{info["quality"]}, {len(data):,} bytes, '
                  f'full luma SSIM {info["full_resolution_luma_ssim"]:.5f}',flush=True)
    packs = report["packs"]
    for slug,photos in encoded.items():
        name = slug+("_extra" if (ASSETS/f"{slug}.zip").exists() else "")+".zip"
        target = ASSETS/name
        if target.exists(): raise FileExistsError(target)
        with zipfile.ZipFile(target,"w",compression=zipfile.ZIP_STORED) as archive:
            for data,info in sorted(photos,key=lambda p:p[1]["number"]):
                entry = zipfile.ZipInfo(f'{info["number"]:02d}.webp',(2026,11,10,0,0,0))
                entry.compress_type = zipfile.ZIP_STORED
                archive.writestr(entry,data)
        packs.append({"slug":slug,"archive":name,"archive_bytes":target.stat().st_size,
                      "archive_sha256":digest(target),"photos":[p for _,p in photos]})
    for name,value in before.items(): assert digest(ASSETS/name) == value, name
    originals = sum(p["original_bytes"] for pack in packs for p in pack["photos"])
    total = sum(pack["archive_bytes"] for pack in packs)
    count = sum(len(pack["photos"]) for pack in packs)
    assert count == len(rows), (count,len(rows))
    report = {"format":"WebP","max_dimensions":[1920,1080],"image_count":count,
              "original_bytes":originals,"archive_bytes":total,
              "reduction_percent":round((1-total/originals)*100,2),
              "quality_steps":STEPS,"quality_floor":FLOOR,
              "previous_archives_sha256":baseline,
              "note":"Lossy encoding unless quality=lossless. Detail SSIM is evaluated at full "
                     "encoded resolution, with 128px patch checks; chroma error uses the 0-255 scale. "
                     "These metrics do not prove perceptual identity. No upscaling. "
                     "All previously shipped archive bytes are unchanged.",
              "packs":packs}
    REPORT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({k:v for k,v in report.items() if k not in ("packs","previous_archives_sha256")},ensure_ascii=False))

if __name__ == "__main__": main()
