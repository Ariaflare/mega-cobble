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

    # substitute resolver (placeholder; real skins come from an external resource pack)
    req = REQUIRED_ASPECT.get(sp)
    def sub(asp, shiny=False):
        return {"aspects":([asp,"shiny"] if shiny else [asp]),"poser":"cobblemon:substitute",
                "model":"cobblemon:substitute.geo",
                "texture":"cobblemon:textures/pokemon/substitute"+("_shiny" if shiny else "")+".png","layers":[]}
    variations=[sub(aspect), sub(aspect, True)]
    if req: variations += [sub(req), sub(req, True)]   # restricted base form (e.g. Eternal Floette) too
    json.dump({"species":f"cobblemon:{sp}","order":5,"variations":variations},
              open(f"{ROOT}/assets/cobblemon/bedrock/pokemon/resolvers/megacobble/{sp}.json","w"),indent=2)

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
  "_comment":"Mega-stone item + custom-ability definitions for megas not in Cobblemon's bundled sim (Legends Z-A + custom). Injected at battle start via ShowdownService. 'heldItem'/'abilities' map a Showdown id to a JS def the sim's receiveData() evaluates. Abilities are ALSO shipped as Cobblemon datapack files under data/megacobble/abilities/ so species_additions referencing them parse.",
  "abilities":abilities,
  "heldItem":held_items,
}, open(os.path.join(ROOT,'custom_mega_showdown.json'),"w"),indent=2)

print(f"Generated {count} custom megas. manifest now {len(manifest)} stones.")
print(f"  heldItem injections: {len(held_items)} | abilities (datapack + sim): {sorted(abilities.keys())}")
