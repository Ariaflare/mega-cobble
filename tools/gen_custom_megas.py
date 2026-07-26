"""
Generates the Legends Z-A / custom megas through the proven custom-mega pipeline:
  - data/cobblemon/species_additions/<sp>_mega.json   (the mega FORM on the Cobblemon side)
  - custom_mega_showdown.json                          (sim item defs + new-ability JS)
  - the substitute resolver (placeholder visual; real skins come from an external pack)
  - appends the stones to megastones.json + the ability lang (gen_megastones.py runs first)

Stones are vanilla items identified by custom_data, so no item textures/models are generated.
Run AFTER gen_megastones.py (which produces the classic 47). Reads za_megas.json for stats/types/abilities.
"""
import os, json, shutil

ROOT = r"E:/mod dev/mega co/src/main/resources"

# Species we ship a mega for (complete data in za_megas.json) -> canonical Mega Stone name.
# A species with multiple mega formes maps to a {form-name: stone-name} dict (e.g. Raichu X/Y).
STONE_NAMES = {
 'greninja':'Greninjite','meganium':'Meganiumite','feraligatr':'Feraligite','dragonite':'Dragoninite',
 'excadrill':'Excadrite','chandelure':'Chandelurite','chesnaught':'Chesnaughtite','delphox':'Delphoxite',
 'clefable':'Clefablite','victreebel':'Victreebelite','starmie':'Starminite','skarmory':'Skarmorite',
 'froslass':'Froslassite','emboar':'Emboarite','golurk':'Golurkite','floette':'Floettite',
 'meowstic':'Meowsticite','hawlucha':'Hawluchanite','drampa':'Drampanite',
 # Mega Dimension DLC megas now in Pokemon Champions
 'barbaracle':'Barbaraclite','chimecho':'Chimechite','crabominable':'Crabominite','dragalge':'Dragalgite',
 'eelektross':'Eelektrite','falinks':'Falinksite','glimmora':'Glimmorite','malamar':'Malamarite',
 'pyroar':'Pyroarite','scolipede':'Scolipite','scovillain':'Scovillite','scrafty':'Scraftite',
 'staraptor':'Staraptite',
 'raichu':{'Mega-X':'Raichunite X','Mega-Y':'Raichunite Y'},
 # DLC legendaries: shipped with their base ability as a placeholder until Champions assigns one.
 # Garchomp/Lucario/Absol get a NEW "Z" mega distinct from their classic ORAS mega.
 'darkrai':'Darkraite','garchomp':'Garchompite Z','lucario':'Lucarionite Z','heatran':'Heatranite',
 'magearna':'Magearnite','zeraora':'Zeraorite','baxcalibur':'Baxcalibite','golisopod':'Golisopite',
 'tatsugiri':'Tatsugirite','absol':'Absolite Z',
 # Legendary base-game Z-A mega absent from Champions; megas only from Complete Forme (see below).
 'zygarde':'Zygardite',
}

# Stones restricted to a specific form aspect: the held-item manager only exposes them to the
# sim when the Pokemon has the aspect, so other forms can't mega. (Mega Floette = AZ's Eternal.)
REQUIRED_ASPECT = {
 'floette':'flower-eternal',
 'zygarde':'complete-percent',  # Mega Zygarde forms only from the Complete Forme (100%).
}

# Restricted base forms that Cobblemon already ships a model for: don't hijack them with a
# substitute resolver variation. (Eternal Floette has no model -> not listed -> gets a placeholder.)
REQ_BASE_HAS_MODEL = {'zygarde'}

# za_megas ability value -> Showdown ability id, for the brand-new abilities.
NEW_ABILITY_ID = {'mega_sol':'megasol','dragonize':'dragonize','piercing_drill':'piercingdrill',
 'spicy_spray':'spicyspray','fire_mane':'firemane'}

# Display name + description (shown in the summary screen via cobblemon.ability.<id> lang keys).
NEW_ABILITY_DESC = {
 'dragonize': "This Pokemon's Normal-type moves become Dragon-type and gain a small power boost.",
 'piercingdrill': "This Pokemon's contact moves strike through Protect and Detect.",
 'megasol': "This Pokemon's moves act as if harsh sunlight is active: Fire-type power x1.5, Water-type power x0.5, and Solar Beam/Solar Blade fire in one turn with no weather penalty.",
 'spicyspray': "When this Pokemon is hit by a damaging move, the attacker is burned.",
 'firemane': "This Pokemon's Fire-type moves have 1.5x power.",
 'eelevate': "This Pokemon is immune to Ground-type moves, and KOing a Pokemon raises its highest stat by 1.",
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
 # Mega Sol: this Pokemon's moves behave as if harsh sunlight (Sunny Day) is up, without setting
 # weather. Fire x1.5 / Water x0.5 base power; Solar Beam/Solar Blade fire in one turn (no charge,
 # like Power Herb) and skip the rain/sand/snow power penalty.
 'megasol': ("Mega Sol",
   "{ name: 'Mega Sol',"
   " onChargeMove(pokemon, target, move) { if (move.id === 'solarbeam' || move.id === 'solarblade') { this.attrLastMove('[still]'); this.addMove('-anim', pokemon, move.name, target); return false; } },"
   " onBasePowerPriority: 21,"
   " onBasePower(basePower, attacker, defender, move) { if (move.type === 'Fire') { return this.chainModify(1.5); } if (move.type === 'Water') { return this.chainModify(0.5); } if ((move.id === 'solarbeam' || move.id === 'solarblade') && ['raindance', 'primordialsea', 'sandstorm', 'hail', 'snow'].includes(attacker.effectiveWeather())) { return this.chainModify(2); } } }"),
 # Spicy Spray (Mega Scovillain): burn the attacker whenever this Pokemon is hit by a damaging move.
 'spicyspray': ("Spicy Spray",
   "{ name: 'Spicy Spray', onDamagingHit(damage, target, source, move) { source.trySetStatus('brn', target); } }"),
 # Fire Mane (Mega Pyroar): this Pokemon's Fire-type moves deal 1.5x damage.
 'firemane': ("Fire Mane",
   "{ name: 'Fire Mane', onBasePowerPriority: 19,"
   " onBasePower(basePower, attacker, defender, move) { if (move.type === 'Fire') { return this.chainModify(1.5); } } }"),
 # Eelevate (Mega Eelektross): Levitate-style Ground-move immunity + Beast Boost (raise highest stat
 # by 1 on a KO). NOTE: the sim's isGrounded() is hardcoded to the literal 'levitate' id and Cobblemon's
 # injection API can't override it, so the broader "ungrounded" immunities (entry hazards, Arena Trap,
 # terrain) can't be granted without patching the sim. We do the Ground-MOVE immunity faithfully via
 # onTryHit: breakable (Mold Breaker bypasses it) and skipped for moves that ignore Ground immunity
 # (e.g. Thousand Arrows).
 'eelevate': ("Eelevate",
   "{ name: 'Eelevate', flags: { breakable: 1 },"
   " onTryHit(target, source, move) { if (target !== source && move.type === 'Ground' && !(move.ignoreImmunity === true || (move.ignoreImmunity && move.ignoreImmunity['Ground']))) { this.add('-immune', target, '[from] ability: Eelevate'); return null; } },"
   " onSourceAfterFaint(length, target, source, effect) { if (effect && effect.effectType === 'Move') { const bestStat = source.getBestStat(true, true); this.boost({ [bestStat]: length }, source); } } }"),
}

ZA = json.load(open(os.path.join(ROOT,'za_megas.json'),encoding='utf-8'))['megas']

manifest_path = os.path.join(ROOT,'megastones.json')
lang_path = os.path.join(ROOT,'assets/megacobble/lang/en_us.json')
manifest = json.load(open(manifest_path,encoding='utf-8'))
lang = json.load(open(lang_path,encoding='utf-8'))

# clean our generated species_additions so reruns don't duplicate
sa_dir = os.path.join(ROOT,'data/cobblemon/species_additions')
if os.path.isdir(sa_dir): shutil.rmtree(sa_dir)
os.makedirs(sa_dir,exist_ok=True)

def sub(asp, shiny=False):
    return {"aspects":([asp,"shiny"] if shiny else [asp]),"poser":"cobblemon:substitute",
            "model":"cobblemon:substitute.geo",
            "texture":"cobblemon:textures/pokemon/substitute"+("_shiny" if shiny else "")+".png","layers":[]}

def stone_name_for(sp, form):
    sn=STONE_NAMES[sp]
    return sn[form] if isinstance(sn, dict) else sn

# Group the shippable mega entries by species: a species can have more than one mega forme
# (e.g. Raichu -> Mega-X / Mega-Y), which share one resolver + one species_addition but get
# their own Mega Stone each.
from collections import OrderedDict
by_species=OrderedDict()
for e in ZA:
    sp=e['species']
    if sp not in STONE_NAMES: continue
    if e.get('ability') is None or e.get('baseStats') is None: continue
    by_species.setdefault(sp, []).append(e)

held_items = {}
abilities = {}
count=0; num=9300
for sp, entries in by_species.items():
    species_cap=sp.capitalize()
    req = REQUIRED_ASPECT.get(sp)

    # one substitute resolver per species, covering every mega forme's aspect (+ shiny)
    variations=[]
    for e in entries:
        variations += [sub(e['aspect']), sub(e['aspect'], True)]
    # Also override the restricted base form with a substitute -- but only when Cobblemon has no
    # model for it (e.g. AZ's Eternal Floette). Zygarde's Complete Forme IS modeled, so leave it be.
    if req and sp not in REQ_BASE_HAS_MODEL:
        variations += [sub(req), sub(req, True)]
    json.dump({"species":f"cobblemon:{sp}","order":5,"variations":variations},
              open(f"{ROOT}/assets/cobblemon/bedrock/pokemon/resolvers/megacobble/{sp}.json","w"),indent=2)

    # one species_addition per species, holding all of its mega forms (so X/Y live together)
    forms=[]
    for e in entries:
        ab_id=NEW_ABILITY_ID.get(e['ability'], e['ability'])
        form_obj={"name":e['form'],"baseStats":e['baseStats'],"primaryType":e['type1'],
                  "abilities":[ab_id],"aspects":[e['aspect']],"battleOnly":True,
                  "requiredItem":stone_name_for(sp, e['form'])}
        if e.get('type2'): form_obj["secondaryType"]=e['type2']
        forms.append(form_obj)
    json.dump({"target":f"cobblemon:{sp}","forms":forms},
              open(f"{sa_dir}/{sp}_mega.json","w"),indent=2)

    # per-forme: stone manifest entry + sim mega-stone item (+ new-ability injection)
    for e in entries:
        ab_id=NEW_ABILITY_ID.get(e['ability'], e['ability'])
        stone_name=stone_name_for(sp, e['form'])
        stone_id=stone_name.lower().replace(' ','_')
        showdown_item=''.join(ch for ch in stone_name.lower() if ch.isalnum())
        mega_species="%s-%s"%(species_cap, e['form'])   # Glimmora-Mega, Raichu-Mega-X, ...
        entry={"stone":stone_id,"name":stone_name,"species":sp,"form":e['form'],"aspect":e['aspect']}
        if req: entry["requiredAspect"]=req
        if not any(r['stone']==stone_id for r in manifest):
            manifest.append(entry)
        held_items[showdown_item]=("{ name: '%s', megaStone: '%s', megaEvolves: '%s', itemUser: ['%s'], num: %d, gen: 8,"
            " onTakeItem(item, source) { if (item.megaEvolves === source.baseSpecies.baseSpecies) return false; return true; } }"
            % (stone_name, mega_species, species_cap, species_cap, num))
        num+=1
        if ab_id in NEW_ABILITY_JS:
            abilities[ab_id]=NEW_ABILITY_JS[ab_id][1]
            lang[f"cobblemon.ability.{ab_id}"]=NEW_ABILITY_JS[ab_id][0]
            lang[f"cobblemon.ability.{ab_id}.desc"]=NEW_ABILITY_DESC[ab_id]
        count+=1

json.dump(manifest, open(manifest_path,"w"),indent=2)
json.dump(lang, open(lang_path,"w",encoding='utf-8'),indent=2,ensure_ascii=False)

# New abilities ship as Cobblemon datapack ability files: this registers them on the Cobblemon side,
# which is REQUIRED for species_additions referencing them to parse (and for our form-data lookup).
ab_dir = os.path.join(ROOT,'data/megacobble/abilities')
if os.path.isdir(ab_dir): shutil.rmtree(ab_dir)
os.makedirs(ab_dir,exist_ok=True)
for ab_id, js in abilities.items():
    open(f"{ab_dir}/{ab_id}.js","w").write(js + "\n")

# Cobblemon registers those abilities on its own side but does NOT forward them to the bundled sim,
# and it sends every mega forme to the sim with its abilities blanked to "No Ability". So the SAME
# mega-stone items AND ability defs are also injected straight into the sim at battle start (via
# ShowdownService / MegaShowdownInjector) — without the 'abilities' injection the custom ability is
# registered Cobblemon-side but has no effect in battle.
json.dump({
  "_comment":"Mega-stone item + custom-ability definitions for megas not in Cobblemon's bundled sim (Legends Z-A + custom). Injected at battle start via ShowdownService. Top-level keys MUST be valid sim registry types (ability, bagItem, heldItem, move, species). 'heldItem'/'ability' map a Showdown id to a JS def the sim's receiveData() evaluates. Abilities are ALSO shipped as Cobblemon datapack files under data/megacobble/abilities/ so species_additions referencing them parse.",
  "ability":abilities,
  "heldItem":held_items,
}, open(os.path.join(ROOT,'custom_mega_showdown.json'),"w"),indent=2)

print(f"Generated {count} custom megas. manifest now {len(manifest)} stones.")
print(f"  heldItem injections: {len(held_items)} | abilities (datapack + sim): {sorted(abilities.keys())}")
