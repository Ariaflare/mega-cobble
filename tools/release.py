#!/usr/bin/env python3
"""
Version manager for Mega Cobble.

`mod_version` in gradle.properties is the single source of truth (it sets the jar name, the in-mod
version via fabric.mod.json's ${version}, and the release). This bumps it, commits, and creates the
matching `vX.Y.Z` git tag. Pushing the tag triggers the GitHub Actions versioned release.

Usage:
  python tools/release.py                  # show current version + recent tags
  python tools/release.py patch            # 0.1.0 -> 0.1.1   (bug fixes)
  python tools/release.py minor            # 0.1.0 -> 0.2.0   (new features)
  python tools/release.py major            # 0.1.0 -> 1.0.0   (breaking / stable)
  python tools/release.py 0.3.0            # set an explicit version
  python tools/release.py minor --push     # also push commit + tag (kicks off the release)
"""
import argparse
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GP = os.path.join(REPO, "gradle.properties")


def git(*args, capture=False):
    return subprocess.run(["git", *args], cwd=REPO, check=True,
                          capture_output=capture, text=True)


def read_version():
    text = open(GP, encoding="utf-8").read()
    m = re.search(r"^mod_version=(.+)$", text, re.M)
    if not m:
        sys.exit("mod_version not found in gradle.properties")
    return m.group(1).strip(), text


def bump(version, part):
    parts = version.split(".")
    if len(parts) != 3 or not all(p.isdigit() for p in parts):
        sys.exit(f"current version {version!r} isn't MAJOR.MINOR.PATCH — pass an explicit version")
    major, minor, patch = map(int, parts)
    if part == "major":
        return f"{major + 1}.0.0"
    if part == "minor":
        return f"{major}.{minor + 1}.0"
    return f"{major}.{minor}.{patch + 1}"


def main():
    ap = argparse.ArgumentParser(description="Bump the mod version and tag a release.")
    ap.add_argument("bump", nargs="?", help="major | minor | patch | an explicit X.Y.Z")
    ap.add_argument("--push", action="store_true", help="push the commit + tag (triggers the release)")
    args = ap.parse_args()

    version, text = read_version()

    if args.bump is None:
        tags = git("tag", "--list", "v*", "--sort=-v:refname", capture=True).stdout.split()
        print(f"current mod_version: {version}")
        print("recent tags: " + (", ".join(tags[:8]) if tags else "(none)"))
        return

    if args.bump in ("major", "minor", "patch"):
        new = bump(version, args.bump)
    elif re.fullmatch(r"\d+\.\d+\.\d+", args.bump):
        new = args.bump
    else:
        sys.exit(f"bump must be major|minor|patch or X.Y.Z, got {args.bump!r}")

    tag = f"v{new}"

    dirty = git("status", "--porcelain", capture=True).stdout.strip()
    if dirty:
        sys.exit("Working tree isn't clean — commit or stash first:\n" + dirty)
    if git("tag", "--list", tag, capture=True).stdout.strip():
        sys.exit(f"Tag {tag} already exists.")

    open(GP, "w", encoding="utf-8", newline="\n").write(
        re.sub(r"^mod_version=.+$", f"mod_version={new}", text, count=1, flags=re.M))

    git("add", "gradle.properties")
    git("commit", "-m", f"Release {tag}")
    git("tag", tag)
    print(f"{version} -> {new}  (committed + tagged {tag})")

    if args.push:
        git("push", "--follow-tags")
        print(f"Pushed. GitHub Actions will build and publish the {tag} release.")
    else:
        print("Next:  git push --follow-tags   (to build + publish the release)")
        print("Tip: add a changelog entry to README.md before releasing.")


if __name__ == "__main__":
    main()
