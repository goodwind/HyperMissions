#!/usr/bin/env python3
"""Draws the HyperMissions icon and the TV banner (PIL only). Run from the repository root:

    python3 tools/make-icons.py

Writes docs/img/icon.png, docs/img/banner.png and app/src/main/res/drawable-xhdpi/banner.png.
The launcher icon itself is the vector app/src/main/res/drawable/ic_launcher.xml (same geometry).
The wordmark uses Noto Sans Bold (SIL Open Font License).
"""
from PIL import Image, ImageDraw, ImageFont
import os

TOP = (15, 23, 42)       # #0F172A
BOTTOM = (67, 56, 202)   # #4338CA
ACCENT = (34, 211, 238)  # #22D3EE
FONT = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
FONT_REGULAR = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"


def gradient(w, h):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = min(1.0, max(0.0, 0.35 * x / w + 0.65 * y / h))
            px[x, y] = tuple(int(TOP[i] + (BOTTOM[i] - TOP[i]) * t) for i in range(3))
    return img


def cubic(p0, p1, p2, p3, n=40):
    out = []
    for i in range(1, n + 1):
        t = i / n
        u = 1 - t
        out.append((
            u**3 * p0[0] + 3 * u**2 * t * p1[0] + 3 * u * t**2 * p2[0] + t**3 * p3[0],
            u**3 * p0[1] + 3 * u**2 * t * p1[1] + 3 * u * t**2 * p2[1] + t**3 * p3[1],
        ))
    return out


def shield_points():
    pts = [(50, 12), (82, 24), (82, 50)]
    pts += cubic((82, 50), (82, 70), (68, 84), (50, 90))
    pts += cubic((50, 90), (32, 84), (18, 70), (18, 50))
    pts += [(18, 24)]
    return pts


def draw_shield(img, x0, y0, unit):
    """The shield with its two switches, in a 100-unit box whose corner is (x0, y0), [unit] pixels a unit."""
    s = 4
    big = Image.new("RGBA", (int(100 * unit * s), int(100 * unit * s)), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    k = unit * s

    def P(p):
        return (p[0] * k, p[1] * k)

    d.polygon([P(p) for p in shield_points()], fill=(255, 255, 255, 255))
    # switch on: indigo track, white knob on the right, cyan ring
    d.rounded_rectangle([33 * k, 35 * k, 67 * k, 51 * k], radius=8 * k, fill=BOTTOM + (255,))
    d.ellipse([(59 - 5.6) * k, (43 - 5.6) * k, (59 + 5.6) * k, (43 + 5.6) * k], fill=(255, 255, 255, 255), outline=ACCENT + (255,), width=int(1.6 * k))
    # switch off: light track, white knob on the left
    d.rounded_rectangle([33 * k, 57 * k, 67 * k, 73 * k], radius=8 * k, fill=(203, 213, 225, 255))
    d.ellipse([(41 - 5.6) * k, (65 - 5.6) * k, (41 + 5.6) * k, (65 + 5.6) * k], fill=(255, 255, 255, 255), outline=(148, 163, 184, 255), width=int(1.2 * k))
    small = big.resize((int(100 * unit), int(100 * unit)), Image.LANCZOS)
    img.alpha_composite(small, (int(x0), int(y0)))


def icon(size=512):
    base = gradient(size, size).convert("RGBA")
    draw_shield(base, 0, 0, size / 100)
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size * 4 - 1, size * 4 - 1], radius=int(size * 4 * 0.22), fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(base, (0, 0), mask)
    return out


def banner(w=640, h=360):
    base = gradient(w, h).convert("RGBA")
    draw_shield(base, 28, 70, 2.2)
    d = ImageDraw.Draw(base)
    d.text((262, 136), "HyperMissions", font=ImageFont.truetype(FONT, 46), fill=(255, 255, 255, 255))
    d.text((264, 204), "App permissions for HyperOS TV", font=ImageFont.truetype(FONT_REGULAR, 23), fill=(199, 210, 254, 255))
    return base.convert("RGB")


if __name__ == "__main__":
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    os.makedirs(os.path.join(root, "docs/img"), exist_ok=True)
    os.makedirs(os.path.join(root, "app/src/main/res/drawable-xhdpi"), exist_ok=True)
    icon().save(os.path.join(root, "docs/img/icon.png"))
    b = banner()
    b.save(os.path.join(root, "docs/img/banner.png"))
    b.save(os.path.join(root, "app/src/main/res/drawable-xhdpi/banner.png"))
    print("ok")
