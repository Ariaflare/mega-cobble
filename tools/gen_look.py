#!/usr/bin/env python3
"""
Scaffold a custom "look" for an existing Cobblemon species.

A look is a forced aspect that the resolver maps to a model / texture / animation. This writes two
things and nothing else (the command lever + catalog already exist):

  1. a variation in the target species' megacobble resolver, keyed on the look's aspect, pointing at
     either an existing Cobblemon model or custom resource-pack assets;
  2. an entry in src/main/resources/variants.json so `/megacobble variant apply <id>` knows it.

Usage:
  # reuse an existing Cobblemon Pokemon's model/texture/animation (no new art):
  python tools/gen_look.py <look_id> <target_species> cobblemon:<source_species> [--label "Nice Name"]

  # point at custom resource-pack assets:
  python tools/gen_look.py <look_id> <target_species> custom \
      --poser cobblemon:<poser> --model cobblemon:<model>.geo \
      --texture cobblemon:textures/pokemon/<path>.png [--label "..."]

Example (make a Venusaur render as a Blastoise on `/megacobble variant apply blastoise_skin`):
  python tools/gen_look.py blastoise_skin venusaur cobblemon:blastoise --label "Blastoise Skin"
"""
import argparse
import glob
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
COBBLE_SRC = os.path.join(os.path.dirname(REPO), "cobblemon-src")
RESOLVERS = os.path.join(REPO, "src/main/resources/assets/cobblemon/bedrock/pokemon/resolvers/megacobble")
VARIANTS_JSON = os.path.join(REPO, "src/main/resources/variants.json")
COBBLE_RESOLVERS = os.path.join(
    COBBLE_SRC, "common/src/main/resources/assets/cobblemon/bedrock/pokemon/resolvers")


def base_assets_of(source_species):
    """Read the source species' base (aspects:[]) variation to copy its poser/model/texture ids."""
    matches = glob.glob(os.path.join(COBBLE_RESOLVERS, f"*_{source_species}", "*base*.json"))
    if not matches:
        sys.exit(f"Could not find a base resolver for cobblemon:{source_species} under {COBBLE_RESOLVERS}")
    data = json.load(open(matches[0], encoding="utf-8"))
    for v in data.get("variations", []):
        if not v.get("aspects"):  # the base variation
            return {"poser": v["poser"], "model": v["model"], "texture": v["texture"]}
    sys.exit(f"No base variation in {matches[0]}")


def title(look_id):
    return " ".join(w.capitalize() for w in look_id.replace("-", "_").split("_"))


def upsert_resolver(target_species, aspect, assets):
    path = os.path.join(RESOLVERS, f"{target_species}.json")
    if os.path.exists(path):
        data = json.load(open(path, encoding="utf-8"))
    else:
        data = {"species": f"cobblemon:{target_species}", "order": 5, "variations": []}
    variations = data.setdefault("variations", [])
    variations = [v for v in variations if v.get("aspects") != [aspect]]  # replace if present
    variations.append({
        "aspects": [aspect],
        "poser": assets["poser"],
        "model": assets["model"],
        "texture": assets["texture"],
        "layers": [],
    })
    data["variations"] = variations
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(data, open(path, "w", encoding="utf-8"), indent=2)
    return path


def upsert_catalog(look_id, label, target_species, aspect):
    data = json.load(open(VARIANTS_JSON, encoding="utf-8"))
    entry = {"id": look_id, "label": label, "kind": "look",
             "species": target_species, "aspects": [aspect]}
    variants = [v for v in data.get("variants", []) if v.get("id") != look_id]
    variants.append(entry)
    data["variants"] = variants
    json.dump(data, open(VARIANTS_JSON, "w", encoding="utf-8"), indent=2)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("look_id")
    ap.add_argument("target_species")
    ap.add_argument("source", help="cobblemon:<species> to reuse its assets, or 'custom' with --model/--poser/--texture")
    ap.add_argument("--label")
    ap.add_argument("--poser")
    ap.add_argument("--model")
    ap.add_argument("--texture")
    args = ap.parse_args()

    aspect = args.look_id
    if args.source.startswith("cobblemon:"):
        assets = base_assets_of(args.source.split(":", 1)[1])
    else:
        if not (args.poser and args.model and args.texture):
            sys.exit("custom source requires --poser, --model and --texture")
        assets = {"poser": args.poser, "model": args.model, "texture": args.texture}

    label = args.label or title(args.look_id)
    resolver_path = upsert_resolver(args.target_species, aspect, assets)
    upsert_catalog(args.look_id, label, args.target_species, aspect)
    print(f"Look '{args.look_id}' -> species '{args.target_species}'")
    print(f"  assets: {assets}")
    print(f"  resolver: {resolver_path}")
    print(f"  catalog:  {VARIANTS_JSON}")
    print(f"Apply in-game: /megacobble variant apply {args.look_id}")


if __name__ == "__main__":
    main()
