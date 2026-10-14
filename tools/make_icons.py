"""Рисует значок и баннер ATA TV (PNG) — запускать из корня репозитория: python3 tools/make_icons.py"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

RES = Path("app/src/main/res")
BG = (21, 101, 192)        # синий
SCREEN = (255, 255, 255)
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def draw_tv(draw: ImageDraw.ImageDraw, box, scale: float) -> None:
    """Силуэт телевизора: экран с ножкой."""
    x0, y0, x1, y1 = box
    r = max(2, int(6 * scale))
    draw.rounded_rectangle((x0, y0, x1, y1), radius=r, outline=SCREEN, width=max(2, int(5 * scale)))
    cx = (x0 + x1) / 2
    leg = (y1 - y0) * 0.18
    draw.line((cx, y1, cx, y1 + leg), fill=SCREEN, width=max(2, int(5 * scale)))
    draw.line((cx - leg * 1.6, y1 + leg, cx + leg * 1.6, y1 + leg), fill=SCREEN, width=max(2, int(5 * scale)))


def icon(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = size / 48
    d.rounded_rectangle((0, 0, size - 1, size - 1), radius=int(10 * s), fill=BG)
    draw_tv(d, (8 * s, 9 * s, 40 * s, 32 * s), s)
    font = ImageFont.truetype(FONT, int(8 * s))
    d.text((size / 2, 20.5 * s), "ATA", font=font, fill=SCREEN, anchor="mm")
    return img


def banner() -> Image.Image:
    img = Image.new("RGB", (320, 180), BG)
    d = ImageDraw.Draw(img)
    draw_tv(d, (24, 40, 116, 116), 2.0)
    d.text((70, 78), "ATA", font=ImageFont.truetype(FONT, 22), fill=SCREEN, anchor="mm")
    d.text((214, 90), "ATA TV", font=ImageFont.truetype(FONT, 40), fill=SCREEN, anchor="mm")
    return img


def main() -> None:
    for name, size in DENSITIES.items():
        out = RES / f"mipmap-{name}" / "ic_launcher.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        icon(size).save(out, optimize=True)
    out = RES / "drawable-xhdpi" / "banner.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    banner().save(out, optimize=True)


if __name__ == "__main__":
    main()
