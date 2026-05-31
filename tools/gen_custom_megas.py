"""
Generates the Legends Z-A / custom megas through the proven custom-mega pipeline:
  - data/cobblemon/species_additions/<sp>_mega.json   (the mega FORM on the Cobblemon side)
  - custom_mega_showdown.json                          (sim item defs + new-ability JS)
  - per-stone item texture/model/lang + substitute resolver
  - appends the stones to megastones.json + lang (which gen_megastones.py writes first)

Run AFTER gen_megastones.py (which produces the classic 47). Reads za_megas.json for stats/types/abilities.
"""
import struct, zlib, os, math, json, shutil

ROOT = r"E:/mod dev/mega co/src/main/resources"

# Species we ship a mega for (complete data in za_megas.json) -> canonical Mega Stone name.
STONE_NAMES = {
 'greninja':'Greninjite','meganium':'Meganiumite','feraligatr':'Feraligite','dragonite':'Dragoninite',
 'excadrill':'Excadrite','chandelure':'Chandelurite','chesnaught':'Chesnaughtite','delphox':'Delphoxite',
 'clefable':'Clefablite','victreebel':'Victreebelite','starmie':'Starminite','skarmory':'Skarmorite',
 'froslass':'Froslassite','emboar':'Emboarite','golurk':'Golurkite','floette':'Floettite',
 'meowstic':'Meowsticite','hawlucha':'Hawluchanite','drampa':'Drampanite',
}

# Stones restricted to a specific form aspect: the held-item manager only exposes them to the
# sim when the Pokemon has the aspect, so other forms can't mega. (Mega Floette = AZ's Eternal.)
REQUIRED_ASPECT = {
 'floette':'flower-eternal',
}

# za_megas ability value -> Showdown ability id, for the brand-new abilities.
NEW_ABILITY_ID = {'mega_sol':'megasol','dragonize':'dragonize','piercing_drill':'piercingdrill'}

# Display name + description (shown in the summary screen via cobblemon.ability.<id> lang keys).
NEW_ABILITY_DESC = {
 'dragonize': "This Pokemon's Normal-type moves become Dragon-type and gain a small power boost.",
 'piercingdrill': "This Pokemon's contact moves strike through Protect and Detect.",
 'megasol': "Weather-dependent moves act as if harsh sunlight is active. (Work in progress.)",
}

# Minimal, original implementations of the new abilities (the sim eval()s these).
NEW_ABILITY_JS = {
 'dragonize': ("Dragonize",
   "{ name: 'Dragonize', onModifyTypePriority: -1,"
   " onModifyType(move, pokemon) { if (move.type === 'Normal' && move.id !== 'weatherball' && !(move.isZ && move.category !== 'Status')) { move.type = 'Dragon'; move.typeChangerBoosted = this.effect; } },"
   " onBasePowerPriority: 23,"
   " onBasePower(bp, pokemon, target, move) { if (move.typeChangerBoosted === this.effect) return this.chainModify([4915, 4096]); } }"),
 'piercingdrill': ("Piercing Drill",
   "{ name: 'Piercing Drill', onModifyMove(move) { if (move.flags['contact']) { move.flags['protect'] = 0; } } }"),
 # Mega Sol is a complex weather-treatment ability; ship a registered stub so the forme resolves,
 # full effect TODO.
 'megasol': ("Mega Sol",
   "{ name: 'Mega Sol' }"),
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
def orb(a, b):
    px=[T]*256; cx=cy=7.5; R=7.3
    for y in range(16):
        for x in range(16):
            dx,dy=x-cx,y-cy; d=math.hypot(dx,dy)
            if d>R: continue
            sf=1.18-(dx+dy)/22.0
            boundary=cy+2.6*math.sin((x-cx)/2.4)
            c=a if y<boundary else b
            c=shade(c,sf)
            if d>R-1.1: c=shade(c,0.6)
            if math.hypot(x-5,y-5)<1.6: c=(255,255,255,255)
            px[y*16+x]=c
    return px

ZA = json.load(open(os.path.join(ROOT,'za_megas.json'),encoding='utf-8'))['megas']

manifest_path = os.path.join(ROOT,'megastones.json')
lang_path = os.path.join(ROOT,'assets/megacobble/lang/en_us.json')
manifest = json.load(open(manifest_path,encoding='utf-8'))
lang = json.load(open(lang_path,encoding='utf-8'))

# clean our generated species_additions so reruns don't duplicate
sa_dir = os.path.join(ROOT,'data/cobblemon/species_additions')
if os.path.isdir(sa_dir): shutil.rmtree(sa_dir)
os.makedirs(sa_dir,exist_ok=True)

held_items = {}
abilities = {}
count=0; num=9300
for e in ZA:
    sp=e['species']
    if sp not in STONE_NAMES: continue
    if e.get('ability') is None or e.get('baseStats') is None: continue
    stone_name=STONE_NAMES[sp]; stone_id=stone_name.lower()
    showdown_item=''.join(ch for ch in stone_name.lower() if ch.isalnum())
    species_cap=sp.capitalize()
    form=e['form']; aspect=e['aspect']
    t1=e['type1']; t2=e.get('type2')
    ab=e['ability']; ab_id=NEW_ABILITY_ID.get(ab, ab)

    # asset: orb texture + model + lang + resolver
    write_png(f"{ROOT}/assets/megacobble/textures/item/{stone_id}.png",
              orb(TYPE_COLOR.get(t1,TYPE_COLOR[None]), TYPE_COLOR.get(t2 if t2 else t1, TYPE_COLOR.get(t1))))
    json.dump({"parent":"minecraft:item/generated","textures":{"layer0":f"megacobble:item/{stone_id}"}},
              open(f"{ROOT}/assets/megacobble/models/item/{stone_id}.json","w"),indent=2)
    req = REQUIRED_ASPECT.get(sp)
    sub = lambda asp: {"aspects":[asp],"poser":"cobblemon:substitute","model":"cobblemon:substitute.geo",
                       "texture":"cobblemon:textures/pokemon/substitute.png","layers":[]}
    variations=[sub(aspect)]
    if req: variations.append(sub(req))   # show the restricted base form (e.g. Eternal Floette) as the doll too
    json.dump({"species":f"cobblemon:{sp}","order":5,"variations":variations},
              open(f"{ROOT}/assets/cobblemon/bedrock/pokemon/resolvers/megacobble/{sp}.json","w"),indent=2)
    lang[f"item.megacobble.{stone_id}"]=stone_name

    # cobblemon species_addition (the mega FORM, with requiredItem so Cobblemon feeds the forme to the sim)
    form_obj={"name":form,"baseStats":e['baseStats'],"primaryType":t1,
              "abilities":[ab_id],"aspects":[aspect],"battleOnly":True,"requiredItem":stone_name}
    if t2: form_obj["secondaryType"]=t2
    json.dump({"target":f"cobblemon:{sp}","forms":[form_obj]},
              open(f"{sa_dir}/{sp}_mega.json","w"),indent=2)

    # stone manifest entry (item + held-item-manager)
    entry={"stone":stone_id,"name":stone_name,"species":sp,"form":form,"aspect":aspect}
    if req: entry["requiredAspect"]=req
    if not any(r['stone']==stone_id for r in manifest):
        manifest.append(entry)

    # sim injections: mega-stone item, and the new ability if needed
    held_items[showdown_item]=("{ name: '%s', megaStone: '%s-Mega', megaEvolves: '%s', itemUser: ['%s'], num: %d, gen: 8,"
        " onTakeItem(item, source) { if (item.megaEvolves === source.baseSpecies.baseSpecies) return false; return true; } }"
        % (stone_name, species_cap, species_cap, species_cap, num))
    num+=1
    if ab_id in NEW_ABILITY_JS:
        abilities[ab_id]=NEW_ABILITY_JS[ab_id][1]
        lang[f"cobblemon.ability.{ab_id}"]=NEW_ABILITY_JS[ab_id][0]
        lang[f"cobblemon.ability.{ab_id}.desc"]=NEW_ABILITY_DESC[ab_id]
    count+=1

json.dump(manifest, open(manifest_path,"w"),indent=2)
json.dump(lang, open(lang_path,"w"),indent=2,ensure_ascii=False)

# New abilities ship as Cobblemon datapack ability files: this registers them on the Cobblemon
# side (so species_additions referencing them parse) AND Cobblemon forwards them to the sim.
ab_dir = os.path.join(ROOT,'data/megacobble/abilities')
if os.path.isdir(ab_dir): shutil.rmtree(ab_dir)
os.makedirs(ab_dir,exist_ok=True)
for ab_id, js in abilities.items():
    open(f"{ab_dir}/{ab_id}.js","w").write(js + "\n")

# The sim injector only needs to provide the mega-stone items (Cobblemon won't send our items;
# abilities are now handled via the datapack above).
json.dump({
  "_comment":"Mega-stone item definitions for megas not in Cobblemon's bundled sim (Legends Z-A + custom). Injected at battle start via ShowdownService. 'heldItem' maps a Showdown item id to a JS item def the sim's receiveData() evaluates.",
  "heldItem":held_items,
}, open(os.path.join(ROOT,'custom_mega_showdown.json'),"w"),indent=2)

print(f"Generated {count} custom megas. manifest now {len(manifest)} stones.")
print(f"  heldItem injections: {len(held_items)} | datapack abilities: {sorted(abilities.keys())}")
