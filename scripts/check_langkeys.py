#!/usr/bin/env python3
"""List lang keys the datapack needs but en_us.json lacks. Exit 1 when any are missing.
Covers modular_type measures (every id names a figure, family number, block figure or axis with a label), research plans (blurb and treatise name), modular_type (role name+tip, role dims, variant name+desc, trait desc, type name) and gun_option (item name by form).
Also every registered item and block (registry/F*.java, by id; loop-built ids and `_delegate` internals skipped) and every mob effect (FEffects) names itself, and every modular_type's primary-part variant names the item (modular.feedback.archetype.*).
Add a rule below when a new data folder gets player-facing text."""
import json, pathlib, re, sys
R = pathlib.Path("src/main/resources")
lang = json.loads((R / "assets/feedback/lang/en_us.json").read_text())
need = {}  # key -> where
def want(key, where):
    need.setdefault(key, where)
def traits(v):
    t = v.get("trait"); return [x for x in (t.split(",") if isinstance(t, str) else t or [])]
J = pathlib.Path("src/main/java/io/github/soundgoodizerfan/feedback/modular")
figs = set(re.findall(r"^\s+([A-Z][A-Z0-9_]*)[,;]", (J / "bake/ModularFigures.java").read_text().split("enum Fig {")[1].split("public static final int ATTACHMENTS")[0], re.M))
figs |= {f"SUIT_{k}_{i}" for k in ("ATT", "POW", "ARG", "SUP") for i in range(1, 5)}  # declared several to a line
_mm = (J / "type/ModularMeasures.java").read_text()
uses = set(re.findall(r'"([a-z]+\.[a-z]+)"', _mm.split("USES = Set.of(")[1].split(");")[0]))
uses |= set(re.findall(r'"([a-z]+\.[a-z]+)"', _mm.split("MACHINE_USES = Set.of(")[1].split(");")[0]))
# every family number and every block figure carries a row label, whether or not a type lists it yet
for u in sorted(uses):
    want("gui.feedback.blueprint_table." + u + ".label", "ModularMeasures.USES")
for k in sorted(set(re.findall(r'putFloat\("(\w+)"', (J / "bake/BlockFigures.java").read_text()))):
    want("figure.feedback.block." + k, "BlockFigures")
    want("figure.feedback.block." + k + ".label", "BlockFigures")
def measure(n, mid, where):
    """A measures id names a figure, a family number, a block figure or an axis, and each has its label."""
    if mid.startswith("block."):
        want("figure.feedback.block." + mid[6:], where)
        want("figure.feedback.block." + mid[6:] + ".label", where)  # the short name of its row in the figure panel
    elif mid.startswith("axis."):
        want("modular.feedback.axis." + mid[5:], where)
    elif "." in mid:
        if mid not in uses:
            print(f"{mid}  ({where}: not a family number of ModularMeasures.USES)"); sys.exit(1)
        want("gui.feedback.blueprint_table." + mid, where)
        want("gui.feedback.blueprint_table." + mid + ".label", where)  # the short name of its row in the figure panel
    else:
        if mid.upper() not in figs:
            print(f"{mid}  ({where}: not a ModularFigures.Fig)"); sys.exit(1)
        want("modular.feedback.fig." + mid, where)
for f in sorted((R / "data/feedback/modular_type").glob("*.json")):
    d = json.loads(f.read_text()); n = f.stem
    ms = d.get("measures", {})
    for mid in ms.get("show", []) + ms.get("sort", []) + [i for v in ms.get("roles", {}).values() for i in v] \
            + [i for v in ms.get("addons", {}).values() for i in v] + [i for v in ms.get("variants", {}).values() for i in v]:
        measure(n, mid, f"{n} measures")
        if mid.startswith("axis."):  # an axis id must be one some part of the type (a face: of any face) declares
            pool = [d] if not (d.get("family") == "hafted" or d.get("face")) else [json.loads(g.read_text()) for g in (R / "data/feedback/modular_type").glob("*.json")]
            if not any(mid[5:] in r.get("axes", {}) for p in pool for r in p.get("roles", {}).values()):
                print(f"{mid}  ({n} measures: no part of the type has this axis)"); sys.exit(1)
    for des in d.get("names", {}).get("designations", []):
        prefix = "gun" if d.get("family") == "gun" else "armour"  # every non-gun type names its builds under armour.*
        want(f"{prefix}.feedback.designation.{des['name']}", f"{n} designation")
    for rid, role in d.get("roles", {}).items():
        want(f"modular.feedback.role.{role['name']}", f"{n} role {rid}")
        want(f"modular.feedback.role.{role['name']}.tip", f"{n} role {rid}")
        for dim, ax in role.get("axes", {}).items():
            want(f"modular.feedback.axis.{dim}", f"{n} role {rid} axis")
            if ax.get("bands"):  # a banded axis is a plate-style measure the bench explains
                want(f"modular.feedback.axis.{dim}.tip", f"{n} role {rid} axis")
            for b in ax.get("bands", []):
                want(f"modular.feedback.band.{b['name']}", f"{n} axis {dim} band")
                for t in traits(b):
                    want(f"modular.feedback.desc.trait.{t}", f"{n} axis {dim} band {b['name']}")
        part = role.get("part") or {}
        for v in part.get("variants", []):
            want(f"modular.feedback.variant.{v['name']}", f"{n} variant")
            want(f"modular.feedback.desc.variant.{v['name']}", f"{n} variant")
            if rid == d.get("wear_role") and d.get("names", {}).get("designations") and d.get("family") not in ("gun", "flintlock", "launcher"):  # named in an automatic build name
                want(f"modular.feedback.archetype.{v['name']}", f"{n} variant")
            if rid == d.get("primary") and not d.get("strikers"):  # the item's name is its primary part's variant (ModularStacks.name): "Titanium Lance Caster"
                want(f"modular.feedback.archetype.{v['name']}", f"{n} primary-part variant")
            for t in traits(v):
                want(f"modular.feedback.desc.trait.{t}", f"{n} variant {v['name']}")
for f in sorted((R / "data/feedback/gun_option").glob("*.json")):
    d = json.loads(f.read_text())
    for t in traits(d):
        want(f"modular.feedback.desc.trait.{t}", f"gun_option {f.stem}")
for f in sorted((R / "data/feedback/material").glob("*.json")):
    if f.stem.startswith("_"):
        continue  # a kind template, never an item
    want(f"material.feedback.{f.stem}", "material")
for f in sorted((R / "data/feedback/gun_option").glob("*.json")):
    want(f"gun.feedback.fitting.{f.stem}", "gun_option")
for f in sorted((R / "data/feedback/research").glob("*.json")):
    want(f"gui.feedback.research_table.blurb.{f.stem}", "research plan")
    want(f"item.feedback.treatise_{f.stem}", "research plan")
for f in sorted((R / "data/feedback/research/app").glob("*.json")):
    want(f"gui.feedback.research_table.blurb.app.{f.stem}", "app research plan")
    want(f"item.feedback.app.{f.stem}", "app research plan")
for f in sorted((R / "data/feedback/research/attunement").glob("*.json")):
    want(f"gui.feedback.research_table.blurb.attuned.{f.stem}", "attunement research plan")
# every registered item, block and mob effect has its name (registrations whose id is built in a loop end in "_" and are skipped; the delegates are never shown)
RG = pathlib.Path("src/main/java/io/github/soundgoodizerfan/feedback/registry")
for f in sorted(RG.glob("*.java")):
    t = f.read_text()
    for m in re.finditer(r'(?:registerItem|registerSimpleItem|ITEMS\.register|registerSimpleBlockItem)\(\s*"([a-z0-9_]+)"', t):
        i = m.group(1)
        if i.endswith("_") or i.endswith("_delegate"):
            continue
        if f"item.feedback.{i}" not in lang and f"block.feedback.{i}" not in lang:
            want(f"item.feedback.{i}", f"registry/{f.name} item")
    for m in re.finditer(r'(?:registerBlock|registerSimpleBlock|BLOCKS\.register)\(\s*"([a-z0-9_]+)"', t):
        if not m.group(1).endswith("_"):
            want(f"block.feedback.{m.group(1)}", f"registry/{f.name} block")
    if f.name == "FEffects.java":
        for m in re.finditer(r'register\(\s*"([a-z0-9_]+)"', t):
            want(f"effect.feedback.{m.group(1)}", "FEffects")

missing = {k: w for k, w in need.items() if k not in lang}
for k, w in sorted(missing.items()):
    print(f"{k}  ({w})")
print(f"{len(missing)} missing of {len(need)} checked", file=sys.stderr)
sys.exit(1 if missing else 0)
