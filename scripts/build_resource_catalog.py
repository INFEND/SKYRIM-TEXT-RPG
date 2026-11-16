#!/usr/bin/env python3
"""Build the catalog embedded in the signed APK. Optional assets stay in Git."""
import argparse, hashlib, json, pathlib, re, subprocess, urllib.parse, urllib.request, time, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets"
CITIES=dict(zip("whiterun windhelm winterhold dawnstar markarth morthal riverwood riften solitude falkreath helgen ivarstead rorikstead dragonbridge karthwasten shorsstone kynesgrove darkwater stonehills ravenrock telmithryn skaal highhrothgar bleakfallsbarrow blackreach labyrinthian collegewinterhold throatoftheworld ustengrav".split(),
    "Вайтран|Виндхельм|Винтерхолд|Данстар|Маркарт|Морфал|Ривервуд|Рифтен|Солитьюд|Фолкрит|Хелген|Айварстед|Рорикстед|Драконий Мост|Картвастен|Шорский Камень|Кинсгроув|Чёрный Брод|Каменные Холмы|Воронья Скала|Тел-Митрин|Деревня скаалов|Высокий Хротгар|Ветреный пик|Чёрный Предел|Лабиринтиан|Коллегия Винтерхолда|Глотка Мира|Устенгрев".split("|")))
MAPS={"skyrim_geographic.webp":"Карта Скайрима","skyrim_user_mosaic.webp":"Дополнительная карта Скайрима","solstheim_terrain.webp":"Карта Солстейма"}
RACES=dict(zip("nord breton dunmer khajiit imperial orc redguard altmer bosmer argonian".split(),
    "Норды|Бретонцы|Данмеры|Каджиты|Имперцы|Орки|Редгарды|Альтмеры|Босмеры|Аргониане".split("|")))
CORE={"lore_bundle.zip","icons/skyrim_icons_48.webp","fonts/Sovngarde Light.ttf","fonts/Sovngarde Bold.ttf","fonts/OFL.txt"}
MEDIA_RE=re.compile(r"media/[a-z0-9_-]+/[a-z0-9_-]+\.(?:webp|png|jpg|jpeg|zip|ogg|mp3|mp4|json)")
def font_records():
    spec=json.loads((ROOT/"app/font-choices.json").read_text(encoding="utf-8"))
    revision=spec["revision"]
    if not re.fullmatch(r"[0-9a-f]{40}",revision):raise ValueError("Invalid font revision")
    result=[]
    for family in spec["families"]:
        for entry in family["files"]:
            source=entry["path"]
            url="https://raw.githubusercontent.com/google/fonts/"+revision+"/"+urllib.parse.quote(source,safe="/")
            for attempt in range(3):
                try:
                    with urllib.request.urlopen(url,timeout=30) as response:
                        raw=response.read(entry["size"]+1)
                    if len(raw)!=entry["size"] or hashlib.sha1(("blob "+str(len(raw))+"\0").encode()+raw).hexdigest()!=entry["gitSha"]:
                        raise ValueError("Invalid pinned font: "+source)
                    break
                except Exception:
                    if attempt==2:raise
                    time.sleep(attempt+1)
            result.append(dict(path=entry["asset"],group="font:"+family["id"],label="Шрифт: "+family["name"],size=len(raw),sha256=hashlib.sha256(raw).hexdigest(),url=url))
    return result

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
        if group=="mapicons":add(path,"icons:map-markers","Значки карты и поселений")
        else:add(path,"media:"+group,"Дополнительные ресурсы: "+group)
    listed={e["path"] for e in records}
    for f in ASSETS.rglob("*"):
        if not f.is_file():continue
        path=f.relative_to(ASSETS).as_posix()
        if path in listed or path=="resource_manifest.json":continue
        if path not in CORE or f.stat().st_size>2*1024*1024:
            raise ValueError("Asset must be externalized or explicitly approved as core: "+path)
    if not records:raise ValueError("No optional assets")
    return dict(schema=1,commit=commit,bundled=not thin,files=records)
def gallery_registry():
    source=(ROOT/"app/src/main/java/dev/infend/skyrimtext/LocationGalleryView.java").read_text(encoding="utf-8")
    def literals(body):return [json.loads(x) for x in re.findall(r'"(?:\\.|[^"\\])*"',body)]
    names=literals(source.split("String[] CITIES={",1)[1].split("};",1)[0])
    slugs=literals(source.split("String[] SLUGS={",1)[1].split("};",1)[0])
    captions=[literals(x) for x in re.findall(r"\{([^{}]+)\}",source.split("String[][] PHOTO_LABELS={",1)[1].split("};",1)[0])]
    if not len(names)==len(slugs)==len(captions):raise ValueError("Gallery metadata mismatch")
    result=[]
    aliases=re.findall(r'alias\("([^"]+)","([^"]+)"\)',source)
    custom=ROOT/"docs/gallery_registry.json"
    for name,slug,labels in zip(names,slugs,captions):
        result.append(dict(name=name,slug=slug,captions=labels,aliases=[a for a,n in aliases if n==name]))
    if custom.exists():
        for entry in json.loads(custom.read_text(encoding="utf-8")):
            result=[old for old in result if old["slug"]!=entry["slug"]]
            result.append(entry)
    observed=set();registered_names=set()
    for entry in result:
        slug=entry["slug"]
        if not re.fullmatch(r"[a-z0-9_-]+",slug) or slug in observed:raise ValueError("Bad gallery slug "+slug)
        observed.add(slug)
        for name in [entry["name"]]+entry.get("aliases",[]):
            key=name.strip().lower().replace("ё","е")
            if not key or key in registered_names:raise ValueError("Duplicate gallery name "+name)
            registered_names.add(key)
        photos={}
        for file in sorted((ASSETS/"gallery").glob(slug+"*.zip")):
            if file.stem not in (slug,slug+"_extra"):continue
            with zipfile.ZipFile(file) as z:
                if z.testzip():raise ValueError("Damaged gallery "+str(file))
                for item in z.infolist():
                    if item.filename=="manifest.json" and item.file_size<=1_000_000:
                        continue # legacy auxiliary metadata; not a displayed frame
                    if not re.fullmatch(r"\d{2}\.webp",item.filename):raise ValueError("Unexpected gallery member "+item.filename)
                    number=int(item.filename[:2])
                    if number in photos or not 16000<item.file_size<2_000_000:raise ValueError("Invalid gallery frame "+item.filename)
                    data=z.read(item)
                    if data[:4]!=b"RIFF" or data[8:12]!=b"WEBP":raise ValueError("Not WebP "+item.filename)
                    photos[number]=file.name
        labels=entry["captions"]
        if not labels or any(not isinstance(x,str) or not x.strip() for x in labels) or set(photos)!=set(range(1,len(labels)+1)):
            raise ValueError("Captions/frames mismatch "+slug)
        entry["archives"]=[photos[i] for i in range(1,len(labels)+1)]
    for entry in result:
        parent=entry.get("parent","")
        if parent:
            matches=[e for e in result if e["name"].strip().lower().replace("ё","е")==parent.strip().lower().replace("ё","е")]
            if len(matches)!=1 or matches[0] is entry or matches[0].get("parent"):raise ValueError("Invalid gallery parent "+parent)
    actual={p.stem.removesuffix("_extra") for p in (ASSETS/"gallery").glob("*.zip")}
    if actual!=observed:raise ValueError("Unregistered gallery archives "+str(actual^observed))
    return result

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--commit",default=None)
    parser.add_argument("--thin",action="store_true",help="Remove optional assets from this disposable CI checkout after creating manifest")
    args=parser.parse_args()
    commit=args.commit or subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
    data=build(commit,args.thin)
    data["revision"]=int(subprocess.check_output(["git","show","-s","--format=%ct",commit],cwd=ROOT,text=True).strip())
    data["galleries"]=gallery_registry()
    labels={e["slug"]:e["name"] for e in data["galleries"]}
    for e in data["files"]:
        if e["group"].startswith("gallery:"):e["label"]=labels[e["group"].split(":",1)[1]]
    if args.thin:
        data["files"].extend(font_records())
    (ASSETS/"resource_manifest.json").write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    if args.thin:
        for e in data["files"]:
            if "url" not in e:(ASSETS/e["path"]).unlink()
    print("Resource catalog:",len(data["files"]),"files;",sum(e["size"] for e in data["files"]),"bytes; bundled=",data["bundled"])
if __name__=="__main__":main()
