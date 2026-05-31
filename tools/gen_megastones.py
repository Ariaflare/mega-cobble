import struct, zlib, os, math, json, colorsys, shutil

ROOT = r"E:/mod dev/mega co/src/main/resources"

# species -> (formName, aspect, primaryType, secondaryType)
FORMS = [
 ("abomasnow","Mega","mega","grass","ice"),("absol","Mega","mega","dark",None),
 ("aerodactyl","Mega","mega","rock","flying"),("aggron","Mega","mega","steel","rock"),
 ("alakazam","Mega","mega","psychic",None),("altaria","Mega","mega","dragon","fairy"),
 ("ampharos","Mega","mega","electric","dragon"),("audino","Mega","mega","normal","fairy"),
 ("banette","Mega","mega","ghost",None),("beedrill","Mega","mega","bug","poison"),
 ("blastoise","Mega","mega","water",None),("blaziken","Mega","mega","fire","fighting"),
 ("camerupt","Mega","mega","fire","ground"),
 ("charizard","Mega-X","mega_x","fire","dragon"),("charizard","Mega-Y","mega_y","fire","flying"),
 ("diancie","Mega","mega","rock","fairy"),("gallade","Mega","mega","psychic","fighting"),
 ("garchomp","Mega","mega","dragon","ground"),("gardevoir","Mega","mega","psychic","fairy"),
 ("gengar","Mega","mega","ghost","poison"),("glalie","Mega","mega","ice",None),
 ("gyarados","Mega","mega","water","dark"),("heracross","Mega","mega","bug","fighting"),
 ("houndoom","Mega","mega","dark","fire"),("kangaskhan","Mega","mega","normal",None),
 ("latias","Mega","mega","dragon","psychic"),("latios","Mega","mega","dragon","psychic"),
 ("lopunny","Mega","mega","normal","fighting"),("lucario","Mega","mega","fighting","steel"),
 ("manectric","Mega","mega","electric",None),("mawile","Mega","mega","steel","fairy"),
 ("medicham","Mega","mega","fighting","psychic"),("metagross","Mega","mega","steel","psychic"),
 ("mewtwo","Mega-X","mega_x","psychic","fighting"),("mewtwo","Mega-Y","mega_y","psychic",None),
 ("pidgeot","Mega","mega","normal","flying"),("pinsir","Mega","mega","bug","flying"),
 ("sableye","Mega","mega","dark","ghost"),("salamence","Mega","mega","dragon","flying"),
 ("sceptile","Mega","mega","grass","dragon"),("scizor","Mega","mega","bug","steel"),
 ("sharpedo","Mega","mega","water","dark"),("slowbro","Mega","mega","water","psychic"),
 ("steelix","Mega","mega","steel","ground"),("swampert","Mega","mega","water","ground"),
 ("tyranitar","Mega","mega","rock","dark"),("venusaur","Mega","mega","grass","poison"),
]
# rayquaza intentionally omitted (no Mega Stone - uses Dragon Ascent)
# Custom / Legends Z-A megas are handled by gen_custom_megas.py (they need species_additions
# + a custom_mega_showdown.json injection), which appends to the manifest this script writes.

STONE_BASE = {
 'abomasnow':'Abomasite','absol':'Absolite','aerodactyl':'Aerodactylite','aggron':'Aggronite',
 'alakazam':'Alakazite','altaria':'Altarianite','ampharos':'Ampharosite','audino':'Audinite',
 'banette':'Banettite','beedrill':'Beedrillite','blastoise':'Blastoisinite','blaziken':'Blazikenite',
 'camerupt':'Cameruptite','charizard':'Charizardite','diancie':'Diancite','gallade':'Galladite',
 'garchomp':'Garchompite','gardevoir':'Gardevoirite','gengar':'Gengarite','glalie':'Glalitite',
 'gyarados':'Gyaradosite','heracross':'Heracronite','houndoom':'Houndoominite','kangaskhan':'Kangaskhanite',
 'latias':'Latiasite','latios':'Latiosite','lopunny':'Lopunnite','lucario':'Lucarionite',
 'manectric':'Manectite','mawile':'Mawilite','medicham':'Medichamite','metagross':'Metagrossite',
 'mewtwo':'Mewtwonite','pidgeot':'Pidgeotite','pinsir':'Pinsirite','sableye':'Sablenite',
 'salamence':'Salamencite','sceptile':'Sceptilite','scizor':'Scizorite','sharpedo':'Sharpedonite',
 'slowbro':'Slowbronite','steelix':'Steelixite','swampert':'Swampertite','tyranitar':'Tyranitarite',
 'venusaur':'Venusaurite',
}

TYPE_COLOR = {
 'normal':(168,168,120),'fire':(240,128,48),'water':(104,144,240),'grass':(120,200,80),
 'electric':(248,208,48),'ice':(152,216,216),'fighting':(192,48,40),'poison':(160,64,160),
 'ground':(224,192,104),'flying':(168,144,240),'psychic':(248,88,136),'bug':(168,184,32),
 'rock':(184,160,56),'ghost':(112,88,152),'dragon':(112,56,248),'dark':(112,88,72),
 'steel':(184,184,208),'fairy':(238,153,172),None:(210,210,220),
}

def write_png(path, px, w=16, h=16):
    raw=bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w): raw+=bytes(px[y*w+x])
    def ch(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    png=b'\x89PNG\r\n\x1a\n'+ch(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+ch(b'IDAT',zlib.compress(bytes(raw),9))+ch(b'IEND',b'')
    os.makedirs(os.path.dirname(path),exist_ok=True); open(path,'wb').write(png)

T=(0,0,0,0)
def shade(c,f): return (max(0,min(255,int(c[0]*f))),max(0,min(255,int(c[1]*f))),max(0,min(255,int(c[2]*f))),255)
def orb(colorA, colorB):
    px=[T]*256; cx=cy=7.5; R=7.3
    for y in range(16):
        for x in range(16):
            dx,dy=x-cx,y-cy; d=math.hypot(dx,dy)
            if d>R: continue
            sf=1.18-(dx+dy)/22.0
            boundary=cy+2.6*math.sin((x-cx)/2.4)
            c=colorA if y<boundary else colorB
            c=shade(c,sf)
            if d>R-1.1: c=shade(c,0.6)
            if math.hypot(x-5,y-5)<1.6: c=(255,255,255,255)
            px[y*16+x]=c
    return px

# ---- build stone entries ----
stones=[]                      # manifest rows
species_aspects={}             # species -> set of aspects (for resolvers)
for (sp,form,aspect,pt,st) in FORMS:
    base=STONE_BASE[sp]
    if form in ('Mega-X','Mega-Y'):
        letter=form.split('-')[1]
        name=f"{base} {letter}"; sid=f"{base.lower()}_{letter.lower()}"
    else:
        name=base; sid=base.lower()
    stones.append({"stone":sid,"name":name,"species":sp,"form":form,"aspect":aspect})
    species_aspects.setdefault(sp,[]).append(aspect)
    # texture: two-tone orb keyed to typing
    a=TYPE_COLOR.get(pt,TYPE_COLOR[None]); b=TYPE_COLOR.get(st if st else pt,a)
    write_png(f"{ROOT}/assets/megacobble/textures/item/{sid}.png", orb(a,b))
    # item model
    os.makedirs(f"{ROOT}/assets/megacobble/models/item",exist_ok=True)
    json.dump({"parent":"minecraft:item/generated","textures":{"layer0":f"megacobble:item/{sid}"}},
              open(f"{ROOT}/assets/megacobble/models/item/{sid}.json","w"),indent=2)

# ---- resolvers (one per species; substitute placeholder for every mega aspect) ----
resdir=f"{ROOT}/assets/cobblemon/bedrock/pokemon/resolvers/megacobble"
if os.path.isdir(resdir): shutil.rmtree(resdir)
os.makedirs(resdir,exist_ok=True)
for sp,aspects in species_aspects.items():
    variations=[{"aspects":[asp],"poser":"cobblemon:substitute","model":"cobblemon:substitute.geo",
                 "texture":"cobblemon:textures/pokemon/substitute.png","layers":[]} for asp in aspects]
    json.dump({"species":f"cobblemon:{sp}","order":5,"variations":variations},
              open(f"{resdir}/{sp}.json","w"),indent=2)

# ---- lang ----
lang={"item.megacobble.key_stone":"Key Stone","itemGroup.megacobble.mega_stones":"Mega Stones"}
for s in stones: lang[f"item.megacobble.{s['stone']}"]=s["name"]
json.dump(lang, open(f"{ROOT}/assets/megacobble/lang/en_us.json","w"),indent=2,ensure_ascii=False)

# ---- manifest (loaded by the mod at init) ----
json.dump(stones, open(f"{ROOT}/megastones.json","w"),indent=2)

# remove the old hand-made venusaur resolver folder (superseded by megacobble/venusaur.json)
old=f"{ROOT}/assets/cobblemon/bedrock/pokemon/resolvers/0003_venusaur"
if os.path.isdir(old): shutil.rmtree(old)

print(f"Generated {len(stones)} stones across {len(species_aspects)} species.")
print("textures+models per stone, resolvers per species, lang, and megastones.json written.")
