#!/usr/bin/env python3
"""Cuts the modular bow and crossbow sprite layers out of the vanilla 1.21.1 bow and crossbow sprites.

Vanilla art is reusable here (author ruling). Each vanilla frame is split by colour and position into the
parts the item model tints (grey ramp, tinted by the part's material) and, for the arrow, a colour layer
copied untouched:

  bow:       limbs (wood and grip wrap)  -> layer 0     string (grey cord)  -> layer 1     arrow -> layer 2 (colour)
  crossbow:  prod -> 0   string -> 1   stock -> 2   lock (nut and trigger plate at the stock's middle) -> 3
             bolt or firework rocket -> layer 4 (colour)

usage: ranged_extract.py <vanilla item texture dir> <output dir>
"""
import os
import sys
from PIL import Image

LEVELS = [68, 127, 178, 216, 255]


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def is_grey(p):
    return abs(p[0] - p[1]) < 3 and abs(p[1] - p[2]) < 3


def load(path):
    return Image.open(path).convert("RGBA")


def px(im):
    return {(x, y): im.getpixel((x, y)) for y in range(16) for x in range(16) if im.getpixel((x, y))[3] > 0}


def snap(g):
    return min(LEVELS, key=lambda l: abs(l - g))


def ramp(pixels_by_layer):
    """Maps each pixel's luminance into the grey ramp, normalised per layer over every frame (min -> outline, max -> 255)."""
    out = {}
    for name, frames in pixels_by_layer.items():
        ls = [lum(p) for f in frames for p in f.values() if not is_grey(p)]
        lo, hi = (min(ls), max(ls)) if ls else (0, 1)
        res = []
        for f in frames:
            d = {}
            for k, p in f.items():
                if is_grey(p):
                    g = 216 if p[0] < 130 else 255  # grip wrap: two light shades
                else:
                    g = snap(LEVELS[0] + (lum(p) - lo) / max(hi - lo, 1) * (255 - LEVELS[0]))
                d[k] = (g, g, g, 255)
            res.append(d)
        out[name] = res
    return out


def string_grey(pixels):
    return {k: (230, 230, 230, 255) for k in pixels}


def write(pixels, path):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for k, p in pixels.items():
        im.putpixel(k, p)
    im.save(path)


# ---- bow -------------------------------------------------------------------------------------------------------

def bow_arrow(k, p):
    x, y = k
    d = x - y
    if is_grey(p) and p[0] >= 170:
        return True  # nock and tip highlights
    if (x, y) in ((3, 2), (3, 3), (4, 3)):
        return True
    return 4 <= y <= 12 and d in (0, 1) and not is_grey(p)


def bow(src, out):
    names = ["bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2"]
    limbs, strings, arrows = [], [], []
    for n in names:
        frame = px(load(f"{src}/{n}.png"))
        l, s, a = {}, {}, {}
        for k, p in frame.items():
            if n != "bow" and bow_arrow(k, p):
                a[k] = p
            elif is_grey(p) and p[0] <= 70:
                s[k] = p
            else:
                l[k] = p
        limbs.append(l)
        strings.append(s)
        arrows.append(a)
    g = ramp({"limbs": limbs})["limbs"]
    for i, n in enumerate(names):
        tag = "" if i == 0 else f"_pulling_{i - 1}"
        write(g[i], f"{out}/bow{tag}_0.png")
        write(string_grey(strings[i]), f"{out}/bow{tag}_1.png")
        if i:
            write(arrows[i], f"{out}/bow{tag}_2.png")


# ---- crossbow --------------------------------------------------------------------------------------------------

def cb_part(k):
    x, y = k
    d = x - y
    if y < 6 or abs(d) > 3:
        return "prod"
    if 9 <= y <= 11 and -1 <= d <= 1 and 8 <= x <= 12:
        return "lock"
    return "stock"


def crossbow(src, out):
    names = ["crossbow_standby", "crossbow_pulling_0", "crossbow_pulling_1", "crossbow_pulling_2"]
    tags = ["", "_p0", "_p1", "_p2"]
    parts = {"prod": [], "stock": [], "lock": []}
    strings = []
    for n in names:
        frame = px(load(f"{src}/{n}.png"))
        s = {}
        d = {"prod": {}, "stock": {}, "lock": {}}
        for k, p in frame.items():
            if is_grey(p):
                s[k] = p
            else:
                d[cb_part(k)][k] = p
        strings.append(s)
        for key in parts:
            parts[key].append(d[key])
    g = ramp(parts)
    base = px(load(f"{src}/crossbow_pulling_2.png"))
    for i, t in enumerate(tags):
        write(g["prod"][i], f"{out}/crossbow{t}_0.png")
        write(string_grey(strings[i]), f"{out}/crossbow{t}_1.png")
        write(g["stock"][i], f"{out}/crossbow{t}_2.png")
        write(g["lock"][i], f"{out}/crossbow{t}_3.png")
    for n, o in (("crossbow_arrow", "crossbow_arrow"), ("crossbow_firework", "crossbow_firework")):
        frame = px(load(f"{src}/{n}.png"))
        write({k: p for k, p in frame.items() if base.get(k) != p}, f"{out}/{o}.png")


if __name__ == "__main__":
    os.makedirs(sys.argv[2], exist_ok=True)
    bow(sys.argv[1], sys.argv[2])
    crossbow(sys.argv[1], sys.argv[2])
