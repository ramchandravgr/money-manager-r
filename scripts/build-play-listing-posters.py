"""Compose Play phone screenshots in the Gist / Realbyte listing style.

White or ink canvas, a large headline with one rose accent line, a device
frame with a clean status bar, and a floating card where a second feature
needs to sit on top of the screen.

Sources: cleaned SM-S938B captures in docs/device-screenshots/play-listing/device-sm-20261110.
Output: 1080x1920 RGB PNG in docs/play-listing/phone.
"""
from __future__ import annotations

import subprocess
from pathlib import Path

from PIL import Image

ROOT = Path(r"e:\STM\MMR")
SRC = ROOT / "docs" / "device-screenshots" / "play-listing" / "device-sm-20261110"
OUT = ROOT / "docs" / "play-listing" / "phone"
BUILD = ROOT / "docs" / "play-listing" / "build"
FONT = Path(r"C:\Users\ramch\.cursor\projects\e-STM-MMR\agent-tools\fonts\Roboto-Black.ttf")
CHROME = Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe")

# Drop the Samsung status-icon row. The poster draws its own.
TOP = 108
BOTTOM = 2208


def shot(prefix: str) -> Path:
    matches = sorted(SRC.glob(prefix + "_*"))
    if not matches:
        raise FileNotFoundError(prefix)
    return matches[0]


def crop(path: Path, box: tuple[int, int, int, int]) -> Image.Image:
    im = Image.open(path).convert("RGB")
    return im.crop(box)


def save_crop(name: str, image: Image.Image, fill_phone: bool = False) -> str:
    # Phone window is 1080x2100. Shorter screens get padded so cover
    # crops the bottom, not the words on the left and right.
    if fill_phone and (image.width != 1080 or image.height < 2100):
        sample = image.getpixel((image.width // 2, min(24, image.height - 1)))
        canvas = Image.new("RGB", (1080, max(image.height, 2100)), sample)
        canvas.paste(image, (0, 0))
        image = canvas
    path = BUILD / "img" / f"{name}.jpg"
    image.save(path, quality=93, subsampling=0)
    return f"img/{name}.jpg"


POSTERS = [
    {
        "file": "01-know-whats-left",
        "html": "Know what’s left<br><em>this month</em>",
        "theme": "ink",
        "status": "plum",
        "screen": ("01", (0, TOP, 1080, BOTTOM)),
    },
    {
        "file": "02-category-ran-over",
        "html": "Spot the category<br><em>that ran over</em>",
        "theme": "paper",
        "status": "plum",
        "screen": ("05", (0, TOP, 1080, BOTTOM)),
    },
    {
        "file": "03-where-the-month-went",
        "html": "See where<br><em>the month went</em>",
        "theme": "paper",
        "status": "plum",
        "screen": ("03", (0, TOP, 1080, BOTTOM)),
        "card": ("04", (36, 960, 1044, 1560)),
    },
    {
        "file": "04-month-calendar",
        "html": "The whole month<br><em>on one calendar</em>",
        "theme": "paper",
        "status": "plum",
        "screen": ("02", (0, TOP, 1080, BOTTOM)),
    },
    {
        "file": "05-house-and-loan",
        "html": "The house and<br><em>the loan behind it</em>",
        "theme": "paper",
        "status": "plum",
        "screen": ("07", (0, TOP, 1080, BOTTOM)),
        "card": ("09", (40, 470, 1040, 840)),
    },
    {
        "file": "06-backup",
        "html": "A backup on your<br><em>phone or Drive</em>",
        "theme": "paper",
        "status": "plum",
        "screen": ("11", (0, 108, 1080, 2300)),
        "card": ("11", (24, 2310, 1056, 2490)),
    },
    {
        "file": "07-colors-and-lock",
        "html": "Your colors,<br><em>and a lock on open</em>",
        "theme": "paper",
        "status": "light",
        "screen": ("10", (0, 2300, 1080, 4271)),
        "card": ("10", (28, 3848, 1052, 4036)),
    },
    {
        "file": "08-emi",
        "html": "Interest, EMI,<br><em>and what’s still due</em>",
        "theme": "paper",
        "status": "plum",
        "screen": ("09", (0, TOP, 1080, BOTTOM)),
    },
]


HTML = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  @font-face {{
    font-family: Poster;
    src: url("{font}") format("truetype");
    font-weight: 900;
  }}
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  html, body {{
    width: 1080px; height: 1920px; overflow: hidden;
    font-family: Poster, sans-serif;
  }}
  .frame {{
    width: 1080px; height: 1920px; position: relative; overflow: hidden;
  }}
  .frame.paper {{
    background:
      radial-gradient(820px 620px at 100% 0%, rgba(226, 70, 96, 0.16), transparent 62%),
      radial-gradient(900px 780px at -12% 88%, rgba(106, 78, 93, 0.13), transparent 58%),
      linear-gradient(180deg, #fff 0%, #fbf6f7 100%);
  }}
  .frame.ink {{
    background:
      radial-gradient(900px 700px at 80% 8%, rgba(226, 70, 96, 0.38), transparent 60%),
      radial-gradient(700px 600px at 0% 100%, rgba(106, 78, 93, 0.55), transparent 55%),
      #161216;
  }}
  h1 {{
    position: absolute;     top: 64px; left: 48px; right: 48px;
    font-size: 88px; font-weight: 900; letter-spacing: -2.8px;
    line-height: 0.96; text-align: center; color: #161216;
  }}
  h1 em {{ font-style: normal; color: #e24660; }}
  .ink h1 {{ color: #ffffff; }}
  .ink h1 em {{ color: #ffb3c0; }}
  .phone {{
    position: absolute; left: 50%; transform: translateX(-50%);
    top: 292px; width: 860px; height: 1700px;
    background: #0c0c0e;
    border-radius: 68px 68px 0 0;
    padding: 18px 16px 0 16px;
    box-shadow:
      0 50px 90px rgba(22, 8, 16, 0.28),
      0 8px 24px rgba(22, 8, 16, 0.12);
  }}
  .ink .phone {{
    box-shadow:
      0 50px 100px rgba(0, 0, 0, 0.55),
      0 0 0 1px rgba(255, 255, 255, 0.06);
  }}
  .sb {{
    height: 52px; display: flex; align-items: center; justify-content: space-between;
    padding: 0 28px 0 32px; font-size: 22px; font-weight: 900; letter-spacing: 0.2px;
    border-radius: 46px 46px 0 0;
  }}
  .sb.plum {{ background: #6A4E5D; color: #fff; }}
  .sb.light {{ background: #f6f3f4; color: #1a1216; }}
  .sb-icons {{ display: flex; align-items: center; gap: 8px; }}
  .sig {{
    width: 18px; height: 14px; display: inline-block;
    background:
      linear-gradient(#fff, #fff) 0 8px / 3px 6px no-repeat,
      linear-gradient(#fff, #fff) 5px 5px / 3px 9px no-repeat,
      linear-gradient(#fff, #fff) 10px 2px / 3px 12px no-repeat,
      linear-gradient(#fff, #fff) 15px 0 / 3px 14px no-repeat;
  }}
  .light .sig {{
    background:
      linear-gradient(#1a1216, #1a1216) 0 8px / 3px 6px no-repeat,
      linear-gradient(#1a1216, #1a1216) 5px 5px / 3px 9px no-repeat,
      linear-gradient(#1a1216, #1a1216) 10px 2px / 3px 12px no-repeat,
      linear-gradient(#1a1216, #1a1216) 15px 0 / 3px 14px no-repeat;
  }}
  .wifi {{
    width: 18px; height: 14px; border: 2.5px solid currentColor; border-bottom: 0;
    border-radius: 18px 18px 0 0; position: relative; display: inline-block;
  }}
  .wifi:after {{
    content: ""; position: absolute; left: 5px; bottom: -1px;
    width: 4px; height: 4px; border-radius: 50%; background: currentColor;
  }}
  .bat {{
    width: 26px; height: 13px; border: 2px solid currentColor; border-radius: 3px;
    position: relative; display: inline-block;
  }}
  .bat:before {{
    content: ""; position: absolute; left: 2px; top: 2px; bottom: 2px; width: 16px;
    background: currentColor; border-radius: 1px;
  }}
  .bat:after {{
    content: ""; position: absolute; right: -4px; top: 3px; width: 2px; height: 5px;
    background: currentColor; border-radius: 0 1px 1px 0;
  }}
  .screen {{
    width: 100%; height: 1610px; display: block; object-fit: cover; object-position: top center;
    border-radius: 8px 8px 0 0;
  }}
  .card {{
    position: absolute; left: 48px; right: 48px; bottom: 168px;
    background: #fff; border-radius: 32px; padding: 16px;
    box-shadow: 0 24px 60px rgba(22, 8, 16, 0.28);
  }}
  .card img {{
    width: 100%; height: auto; display: block; border-radius: 20px;
  }}
</style>
</head>
<body>
  <div class="frame {theme}">
    <h1>{title}</h1>
    <div class="phone">
      <div class="sb {status}"><span>9:41</span><span class="sb-icons"><i class="sig"></i><i class="wifi"></i><i class="bat"></i></span></div>
      <img class="screen" src="{screen}" alt="">
    </div>
    {card}
  </div>
</body>
</html>
"""


def main() -> None:
    (BUILD / "img").mkdir(parents=True, exist_ok=True)
    OUT.mkdir(parents=True, exist_ok=True)
    font_target = BUILD / "Roboto-Black.ttf"
    if not font_target.exists():
        font_target.write_bytes(FONT.read_bytes())

    for poster in POSTERS:
        prefix, box = poster["screen"]
        screen_src = save_crop(poster["file"] + "-screen", crop(shot(prefix), box), fill_phone=True)
        card_html = ""
        if "card" in poster:
            cprefix, cbox = poster["card"]
            card_src = save_crop(poster["file"] + "-card", crop(shot(cprefix), cbox))
            card_html = f'<div class="card"><img src="{card_src}" alt=""></div>'
        html_path = BUILD / f"{poster['file']}.html"
        html_path.write_text(
            HTML.format(
                font="Roboto-Black.ttf",
                title=poster["html"],
                theme=poster["theme"],
                status=poster["status"],
                screen=screen_src,
                card=card_html,
            ),
            encoding="utf-8",
        )
        png = OUT / f"{poster['file']}.png"
        subprocess.run(
            [
                str(CHROME),
                "--headless=new",
                "--disable-gpu",
                "--hide-scrollbars",
                "--force-device-scale-factor=1",
                "--window-size=1080,1920",
                "--default-background-color=FFFFFFFF",
                "--virtual-time-budget=4000",
                f"--screenshot={png}",
                html_path.as_uri(),
            ],
            check=True,
        )
        with Image.open(png) as im:
            if im.mode != "RGB":
                flat = Image.new("RGB", im.size, (255, 255, 255))
                flat.paste(im, mask=im.split()[-1] if "A" in im.mode else None)
                flat.save(png)
            print(f"{png.name} {im.mode} {im.size}")


if __name__ == "__main__":
    main()
