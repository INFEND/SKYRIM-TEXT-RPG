import json,hashlib,pathlib,subprocess
root=pathlib.Path(".")
st=root/"artwork_staging"
old=json.loads((root/"docs/gallery_expansion_compression.json").read_text())
rows=json.loads((st/"rows.json").read_text())
assert len(rows)==29
for r in rows:
    path=root/r["source"]
    data=b"".join((st/f'{r["id"]}.{i}.bin').read_bytes() for i in range(2))
    assert data[:8]==b'\x89PNG\r\n\x1a\n'
    actual=hashlib.sha1(b"blob "+str(len(data)).encode()+b"\0"+data).hexdigest()
    assert actual==r["source_git_sha"], (r["id"],actual,r["source_git_sha"])
    path.write_bytes(data)
assert sum((root/r["source"]).stat().st_size for r in rows)==92634593
existing=[dict(slug=p["slug"],number=i["number"],caption=i["caption"],source="unused",source_git_sha=i["source_git_sha"]) for p in old["packs"] for i in p["photos"]]
assert len(existing)==79
(st/"all.json").write_text(json.dumps(existing+rows,ensure_ascii=False))
subprocess.run(["python","tools/optimize_location_gallery.py","artwork_staging/all.json"],check=True)
