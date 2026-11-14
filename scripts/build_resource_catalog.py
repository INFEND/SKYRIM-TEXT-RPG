#!/usr/bin/env python3
"""Build the catalog embedded in the signed APK. Optional assets stay in Git."""
import argparse, hashlib, json, pathlib, re, subprocess
ROOT=pathlib.Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets"
CITIES=dict(zip("whiterun windhelm winterhold dawnstar markarth morthal riverwood riften solitude falkreath helgen ivarstead rorikstead dragonbridge karthwasten shorsstone kynesgrove darkwater stonehills ravenrock telmithryn skaal highhrothgar".split(),
    "Вайтран|Виндхельм|Винтерхолд|Данстар|Маркарт|Морфал|Ривервуд|Рифтен|Солитьюд|Фолкрит|Хелген|Айварстед|Рорикстед|Драконий Мост|Картвастен|Шорский Камень|Кинсгроув|Чёрный Брод|Каменные Холмы|Воронья Скала|Тел-Митрин|Деревня скаалов|Высокий Хротгар".split("|")))
MAPS={"skyrim_geographic.webp":"Карта Скайрима","skyrim_user_mosaic.webp":"Дополнительная карта Скайрима","solstheim_terrain.webp":"Карта Солстейма"}
RACES=dict(zip("nord breton dunmer khajiit imperial orc redguard altmer bosmer argonian".split(),
    "Норды|Бретонцы|Данмеры|Каджиты|Имперцы|Орки|Редгарды|Альтмеры|Босмеры|Аргониане".split("|")))
CORE={"lore_bundle.zip","icons/skyrim_icons_48.webp","fonts/Sovngarde Light.ttf","fonts/Sovngarde Bold.ttf","fonts/OFL.txt"}
MEDIA_RE=re.compile(r"media/[a-z0-9_-]+/[a-z0-9_-]+\.(?:webp|png|jpg|jpeg|zip|ogg|mp3|mp4|json)")
def build(commit, thin=False):
    if not re.fullmatch(r"[0-9a-f]{40}",commit):raise ValueError("Invalid commit")
    records=[]
    def add(path,group,label):
        f=ASSETS/path
        size=f.stat().st_size
        if not 0<size<=100_000_000:raise ValueError("Invalid optional asset size: "+path)
        records.append(dict(path=path,group=group,label=label,size=size,sha256=hashlib.sha256(f.read_bytes()).hexdigest()))
    for f in sorted((ASSETS/"gallery").glob("*.zip")):
        city=f.stem.removesuffix("_extra")
        add("gallery/"+f.name,"gallery:"+city,CITIES.get(city,city))
    for f in sorted((ASSETS/"maps").glob("*.webp")):
        add("maps/"+f.name,"map:"+f.name,MAPS.get(f.name,f.stem))
    for ext in ("json","webp"):add("icons/skyrim_icons_358_exact."+ext,"icons:358","Атлас 358 иконок")
    for f in sorted((ASSETS/"portraits").glob("*.webp")):
        match=re.fullmatch(r"([a-z]+)_(male|female)_([1-3])\.webp",f.name)
        if not match or match[1] not in RACES:raise ValueError("Unrecognized portrait: "+f.name)
        race=match[1]
        add("portraits/"+f.name,"portraits:"+race,"Портреты: "+RACES[race])
    for f in sorted((ASSETS/"media").rglob("*")):
        if not f.is_file():continue
        path=f.relative_to(ASSETS).as_posix()
        if not MEDIA_RE.fullmatch(path):raise ValueError("Unrecognized optional media: "+path)
        group=path.split("/")[1]
        add(path,"media:"+group,"Дополнительные ресурсы: "+group)
    listed={e["path"] for e in records}
    for f in ASSETS.rglob("*"):
        if not f.is_file():continue
        path=f.relative_to(ASSETS).as_posix()
        if path in listed or path=="resource_manifest.json":continue
        if path not in CORE or f.stat().st_size>2*1024*1024:
            raise ValueError("Asset must be externalized or explicitly approved as core: "+path)
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
