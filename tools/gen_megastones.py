"""
Generates the 47 classic (Gen 6 / ORAS) Mega Stones:
  - megastones.json                                    (the manifest the mod loads at init)
  - assets/cobblemon/.../resolvers/megacobble/<sp>.json (substitute-doll placeholder per species)

Stones are vanilla items identified by custom_data (see MegaItems), so no item textures/models/lang
are generated here — appearance comes from an external resource pack via custom_model_data. Command
lang is hand-maintained in en_us.json and is intentionally NOT touched by this script.

Run this first, then gen_custom_megas.py for the Legends Z-A / custom megas (it appends).
"""
import os, json, shutil

ROOT = r"E:/mod dev/mega co/src/main/resources"

# species -> (formName, aspect, primaryType, secondaryType). Types are informational only.
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
# rayquaza intentionally omitted (no Mega Stone - uses Dragon Ascent).
# Custom / Legends Z-A megas are handled by gen_custom_megas.py, which appends to the manifest.

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

# ---- build stone entries ----
stones=[]                      # manifest rows
species_aspects={}             # species -> [aspects] (for resolvers)
for entry in FORMS:
    sp, form, aspect = entry[0], entry[1], entry[2]
    base=STONE_BASE[sp]
    if form in ('Mega-X','Mega-Y'):
        letter=form.split('-')[1]
        name=f"{base} {letter}"; sid=f"{base.lower()}_{letter.lower()}"
    else:
        name=base; sid=base.lower()
    stones.append({"stone":sid,"name":name,"species":sp,"form":form,"aspect":aspect})
    species_aspects.setdefault(sp,[]).append(aspect)

# ---- resolvers (one per species; substitute placeholder for every mega aspect) ----
resdir=f"{ROOT}/assets/cobblemon/bedrock/pokemon/resolvers/megacobble"
if os.path.isdir(resdir): shutil.rmtree(resdir)
os.makedirs(resdir,exist_ok=True)
def subvar(asp, shiny=False):
    return {"aspects":([asp,"shiny"] if shiny else [asp]),"poser":"cobblemon:substitute",
            "model":"cobblemon:substitute.geo",
            "texture":"cobblemon:textures/pokemon/substitute"+("_shiny" if shiny else "")+".png","layers":[]}
for sp,aspects in species_aspects.items():
    variations=[]
    for asp in aspects:
        variations.append(subvar(asp))         # normal substitute
        variations.append(subvar(asp, True))   # shiny substitute (wins for shiny mons via last-match)
    json.dump({"species":f"cobblemon:{sp}","order":5,"variations":variations},
              open(f"{resdir}/{sp}.json","w"),indent=2)

# ---- manifest (loaded by the mod at init) ----
json.dump(stones, open(f"{ROOT}/megastones.json","w"),indent=2)

# remove the old hand-made venusaur resolver folder (superseded by megacobble/venusaur.json)
old=f"{ROOT}/assets/cobblemon/bedrock/pokemon/resolvers/0003_venusaur"
if os.path.isdir(old): shutil.rmtree(old)

print(f"Generated {len(stones)} stones across {len(species_aspects)} species "
      f"(manifest + substitute resolvers; no item assets/lang).")
