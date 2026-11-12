#!/usr/bin/env python3
"""Optimize original imagegen portraits for the game's 3:4 selection/profile views.

Usage: python3 tools/optimize_character_portraits.py /path/to/portrait_sources.json
The input is a list of {id, path} records for the 60 original PNGs. Sources are
never changed. Reuses the gallery encoder's full-resolution SSIM/chroma checks.
Requires Pillow, numpy and scipy; these are authoring dependencies only.
"""
import concurrent.futures
import hashlib
import io
import json
import pathlib
import sys
from PIL import Image, ImageOps
from optimize_location_gallery import scores, passes, FLOOR

ROOT=pathlib.Path(__file__).resolve().parents[1]
TARGET=ROOT/'app/src/main/assets/portraits'
DIMENSIONS=(576,768)
STEPS=(78,82,86,90,94,95,96,97,98)

def encode(row):
    source=pathlib.Path(row['path'])
    raw=source.read_bytes()
    with Image.open(io.BytesIO(raw)) as original:
        image=ImageOps.exif_transpose(original).convert('RGB')
    source_dimensions=image.size
    # All source portraits are generated at 3:4. Preserve the complete head/ears.
    if abs(image.width/image.height-3/4)>.005:
        raise ValueError('Unexpected portrait aspect ratio: '+row['id'])
    if image.width<DIMENSIONS[0] or image.height<DIMENSIONS[1]:
        raise ValueError('Portrait source is too small: '+row['id'])
    image=image.resize(DIMENSIONS,Image.Resampling.LANCZOS)
    trials=[]
    for quality in STEPS:
        stream=io.BytesIO()
        image.save(stream,'WEBP',quality=quality,method=6)
        encoded=stream.getvalue()
        with Image.open(io.BytesIO(encoded)) as decoded: metric=scores(image,decoded)
        trials.append(dict(quality=quality,bytes=len(encoded),**metric))
        if passes(metric): break
    else:
        stream=io.BytesIO();image.save(stream,'WEBP',lossless=True,method=6)
        encoded=stream.getvalue();quality='lossless'
        with Image.open(io.BytesIO(encoded)) as decoded: metric=scores(image,decoded)
        assert passes(metric)
    return encoded,dict(id=row['id'],file=row['id']+'.webp',
        original_bytes=len(raw),original_sha256=hashlib.sha256(raw).hexdigest(),
        source_dimensions=source_dimensions,dimensions=DIMENSIONS,
        encoded_bytes=len(encoded),sha256=hashlib.sha256(encoded).hexdigest(),
        quality=quality,**metric,trials=trials)

def main():
    rows=json.loads(pathlib.Path(sys.argv[1]).read_text())
    races='nord breton dunmer khajiit imperial orc redguard altmer bosmer argonian'.split()
    expected={f'{r}_{g}_{i}' for r in races for g in ('male','female') for i in (1,2,3)}
    assert len(rows)==60 and {r['id'] for r in rows}==expected
    TARGET.mkdir(parents=True,exist_ok=True)
    entries=[]
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        for data,entry in pool.map(encode,sorted(rows,key=lambda r:r['id'])):
            target=TARGET/entry['file']
            if target.exists() and target.read_bytes()!=data:
                raise FileExistsError('Refusing to replace a different portrait: '+str(target))
            target.write_bytes(data);entries.append(entry)
            print(f"{entry['id']}: {len(data):,} bytes, q{entry['quality']}, SSIM {entry['full_resolution_luma_ssim']:.5f}",flush=True)
    original=sum(e['original_bytes'] for e in entries)
    total=sum(e['encoded_bytes'] for e in entries)
    report=dict(format='WebP',image_count=len(entries),dimensions=DIMENSIONS,
        original_bytes=original,encoded_bytes=total,reduction_percent=round((1-total/original)*100,2),
        quality_steps=STEPS,quality_floor=FLOOR,
        note='Lossy WebP unless quality is lossless. Quality is measured against the Lanczos-downscaled '
             '576x768 RGB reference, not the original larger PNG. No upscaling or face cropping. '
             'Full-resolution luma SSIM, worst 128px patch and chroma error are checked. '
             'These measurements do not prove perceptual identity. All 60 portraits remain bundled '
             'in both full and thin APKs so new-game selection is available offline.',
        portraits=entries)
    (ROOT/'docs/portrait_compression_report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k!='portraits'},ensure_ascii=False))

if __name__=='__main__':main()
