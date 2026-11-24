#!/usr/bin/env python3
"""Build the catalog embedded in the signed APK. Optional assets stay in Git."""
import argparse, hashlib, json, pathlib, re, subprocess, urllib.parse, urllib.request, time, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets"
CITIES=dict(zip("whiterun windhelm winterhold dawnstar markarth morthal riverwood riften solitude falkreath helgen ivarstead rorikstead dragonbridge karthwasten shorsstone kynesgrove darkwater stonehills ravenrock telmithryn skaal highhrothgar bleakfallsbarrow blackreach labyrinthian collegewinterhold throatoftheworld ustengrav".split(),
    "Вайтран|Виндхельм|Винтерхолд|Данстар|Маркарт|Морфал|Ривервуд|Рифтен|Солитьюд|Фолкрит|Хелген|Айварстед|Рорикстед|Драконий Мост|Картвастен|Шорский Камень|Кинсгроув|Чёрный Брод|Каменные Холмы|Воронья Скала|Тель-Митрин|Деревня скаалов|Высокий Хротгар|Ветреный пик|Чёрный Предел|Лабиринтиан|Коллегия Винтерхолда|Глотка Мира|Устенгрев".split("|")))
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
            result.append(dict(path=entry["asset"],group="font:"+family["id"],label="Шрифт: "+family["name"],size=len(raw),sha256=hashlib.sha256(raw).hexdigest(),url=url,units=1))
    return result

def build(commit, thin=False):
    if not re.fullmatch(r"[0-9a-f]{40}",commit):raise ValueError("Invalid commit")
    records=[]
    def add(path,group,label):
        f=ASSETS/path
        size=f.stat().st_size
        if not 0<size<=100_000_000:raise ValueError("Invalid optional asset size: "+path)
        units=1
        if path.endswith(".zip"):
            with zipfile.ZipFile(f) as z:
                if path.startswith("gallery/"):
                    units=sum(bool(re.fullmatch(r"\d{2}\.webp",item.filename)) for item in z.infolist())
                else:
                    units=sum(not item.is_dir() for item in z.infolist())
                if units<1:raise ValueError("Empty resource archive: "+path)
        elif path=="icons/skyrim_icons_358_exact.webp":
            index=json.loads((ASSETS/"icons/skyrim_icons_358_exact.json").read_text(encoding="utf-8"))
            units=len(index["icons"])
            if units!=index["total"] or units!=index["unique"]:raise ValueError("Icon count mismatch")
        elif path=="icons/skyrim_icons_358_exact.json":
            units=0 # auxiliary index is required by the image, not an extra icon
        elif path in ("media/mapicons/settlement_icons.webp","media/mapicons/location_icons.webp"):
            cls="SettlementIcons" if "settlement" in path else "LocationTypeIcons"
            source=(ROOT/"app/src/main/java/dev/infend/skyrimtext"/(cls+".java")).read_text(encoding="utf-8")
            slots=re.findall(r'case PREFIX\+"[^"]+":return (\d+);',source)
            units=len(set(slots))
            if units<1 or set(map(int,slots))!=set(range(units)):raise ValueError("Icon slots mismatch: "+path)
        records.append(dict(path=path,group=group,label=label,size=size,sha256=hashlib.sha256(f.read_bytes()).hexdigest(),units=units))
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
        elif group=="katherine":
            if path.endswith("/gallery.zip") and hashlib.sha256(f.read_bytes()).hexdigest()!="d68789d37c7dff7ffd8d517169b1ee974df65f56c815ef6131263e9eb6f68fa8":
                raise ValueError("Katherine gallery differs from the supplied archive")
            add(path,"media:katherine","Кэтрин")
        elif group=="alma":
            if path!="media/alma/gallery.zip" or hashlib.sha256(f.read_bytes()).hexdigest()!="4910508a80bdcce0f01b802bad50bce41c6a0cafbf2c8a6fcfdfa0a861d25f81":
                raise ValueError("Alma gallery differs from the supplied archive")
            add(path,"media:alma","Альма | Алая Бессмертная")
        elif group=="livia":
            if path!="media/livia/portrait.webp":raise ValueError("Invalid Livia portrait path")
            if hashlib.sha256(f.read_bytes()).hexdigest()!="70941905be1882e6c050b0d93d65cbf0badb8a35879ac8ce2f565f37f5de849b":raise ValueError("Livia portrait differs from the prepared user image")
            add(path,"media:livia","Ливия | Имперский капитан")
        elif group=="npc":
            allowed={"astrid.webp","vex.webp","karliah.webp","nocturnal.webp","elisif.webp"}
            if f.name not in allowed:raise ValueError("Unknown NPC appearance: "+path)
            add(path,"media:npc","Индивидуальные портреты NPC")
        elif group=="serana":
            if path!="media/serana/gallery.zip" or hashlib.sha256(f.read_bytes()).hexdigest()!="be77a3f3aa45a80767e43c5200a4440df97109a704b22cc16da7679abfc9bfaa":
                raise ValueError("Serana gallery differs from the supplied archive")
            add(path,"media:serana","Серана")
        elif group=="elisif":
            expected={"media/elisif/gallery.zip":"8e6c4f75b0cd8f93b650a4b62908851e0ca1c4ae254fd12d0b6d00b47a790e1a",
                      "media/elisif/portrait.png":"ebccdfe23435e7412d343ad885a4ab089f0250c20f6bdee08546221ef3b2b2aa"}
            if path not in expected or hashlib.sha256(f.read_bytes()).hexdigest()!=expected[path]:
                raise ValueError("Elisif artwork differs from the supplied originals")
            add(path,"media:elisif","Элисиф Прекрасная")
        elif group=="whiterun":add(path,"media:whiterun","Вайтран · Главные ворота и Главная улица")
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
        if "cityPlaces" in entry:
            places=entry["cityPlaces"]
            if entry.get("parent") or not isinstance(places,list) or len(places)!=len(labels) or any(not isinstance(p,str) for p in places):
                raise ValueError("Invalid city frames "+slug)
            keys=[p.strip().lower().replace("ё","е") for p in places if p.strip()]
            if len(keys)!=len(set(keys)):raise ValueError("Duplicate city place "+slug)
        entry["archives"]=[photos[i] for i in range(1,len(labels)+1)]
        if slug=="whiterun":
            # User-supplied Whiterun gate and street replace only frames 1/2.
            # Keep the six original archive frames untouched for compatibility.
            replacement="media/whiterun/replacement.zip"
            with zipfile.ZipFile(ASSETS/replacement) as z:
                if z.testzip() or set(z.namelist())!={"01.webp","02.webp"}:
                    raise ValueError("Invalid Whiterun replacement archive")
                for number in (1,2):
                    data=z.read(f"{number:02d}.webp")
                    if not 16000<len(data)<2_000_000 or data[:4]!=b"RIFF" or data[8:12]!=b"WEBP":
                        raise ValueError("Invalid Whiterun replacement frame")
            entry["archives"][:2]=[replacement,replacement]
    for entry in result:
        parent=entry.get("parent","")
        if parent:
            matches=[e for e in result if e["name"].strip().lower().replace("ё","е")==parent.strip().lower().replace("ё","е")]
            if len(matches)!=1 or matches[0] is entry or matches[0].get("parent"):raise ValueError("Invalid gallery parent "+parent)
    actual={p.stem.removesuffix("_extra") for p in (ASSETS/"gallery").glob("*.zip")}
    if actual!=observed:raise ValueError("Unregistered gallery archives "+str(actual^observed))
    return result

def validate_archive_references(data):
    """Fail release generation if a gallery references absent or undersized archives."""
    from collections import Counter
    entries={entry["path"]:entry for entry in data["files"]}
    frames=Counter(
        archive if archive.startswith("media/") else "gallery/"+archive
        for gallery in data["galleries"] for archive in gallery["archives"]
    )
    for path,number in frames.items():
        entry=entries.get(path)
        if entry is None or number>entry["units"]:
            raise ValueError("Gallery archive mismatch: "+path)
    for path in entries:
        if path.startswith("gallery/") and frames[path]<1:
            raise ValueError("Unreferenced gallery archive: "+path)


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--commit",default=None)
    parser.add_argument("--thin",action="store_true",help="Remove optional assets from this disposable CI checkout after creating manifest")
    args=parser.parse_args()
    commit=args.commit or subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
    data=build(commit,args.thin)
    data["revision"]=int(subprocess.check_output(["git","show","-s","--format=%ct",commit],cwd=ROOT,text=True).strip())
    data["galleries"]=gallery_registry()
    validate_archive_references(data)
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
