#!/usr/bin/env python3
"""Build the catalog embedded in the signed APK. Optional assets stay in Git."""
import argparse, hashlib, json, pathlib, re, subprocess
ROOT=pathlib.Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets"
CITIES=dict(zip("whiterun windhelm winterhold dawnstar markarth morthal riverwood riften solitude falkreath helgen ivarstead rorikstead dragonbridge karthwasten shorsstone kynesgrove darkwater stonehills ravenrock telmithryn skaal".split(),
    "Вайтран|Виндхельм|Винтерхолд|Данстар|Маркарт|Морфал|Ривервуд|Рифтен|Солитьюд|Фолкрит|Хелген|Айварстед|Рорикстед|Драконий Мост|Картвастен|Шорский Камень|Кинсгроув|Чёрный Брод|Каменные Холмы|Воронья Скала|Тел-Митрин|Деревня скаалов".split("|")))
MAPS={"skyrim_geographic.webp":"Карта Скайрима","skyrim_user_mosaic.webp":"Дополнительная карта Скайрима","solstheim_terrain.webp":"Карта Солстейма"}
def build(commit, thin=False):
    if not re.fullmatch(r"[0-9a-f]{40}",commit):raise ValueError("Invalid commit")
    records=[]
    def add(path,group,label):
        f=ASSETS/path
        records.append(dict(path=path,group=group,label=label,size=f.stat().st_size,sha256=hashlib.sha256(f.read_bytes()).hexdigest()))
    for f in sorted((ASSETS/"gallery").glob("*.zip")):
        city=f.stem.removesuffix("_extra")
        add("gallery/"+f.name,"gallery:"+city,CITIES.get(city,city))
    for name,label in MAPS.items():add("maps/"+name,"map:"+name,label)
    for ext in ("json","webp"):add("icons/skyrim_icons_358_exact."+ext,"icons:358","Атлас 358 иконок")
    if not records:raise ValueError("No optional assets")
    return dict(schema=1,commit=commit,bundled=not thin,files=records)
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--commit",default=None)
    parser.add_argument("--thin",action="store_true",help="Remove optional assets from this disposable CI checkout after creating manifest")
    args=parser.parse_args()
    commit=args.commit or subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
    data=build(commit,args.thin)
    (ASSETS/"resource_manifest.json").write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    if args.thin:
        for e in data["files"]:(ASSETS/e["path"]).unlink()
    print("Resource catalog:",len(data["files"]),"files;",sum(e["size"] for e in data["files"]),"bytes; bundled=",data["bundled"])
if __name__=="__main__":main()
