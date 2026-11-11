"""Play desktop (Chromebook / large-screen) listing frames for Web Manage.

Sources are the four browser captures shared 2026-11-10. The approvals frame
covers bank-message text and note names before it is composed.

Output: 1920x1080 RGB PNG in docs/play-listing/desktop.
"""
from __future__ import annotations

import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(r"e:\STM\MMR")
ASSETS = Path(r"C:\Users\ramch\.cursor\projects\e-STM-MMR\assets")
OUT = ROOT / "docs" / "play-listing" / "desktop"
BUILD = ROOT / "docs" / "play-listing" / "build" / "desktop"
FONT = Path(r"C:\Users\ramch\.cursor\projects\e-STM-MMR\agent-tools\fonts\Roboto-Black.ttf")
FONT_R = Path(r"C:\Users\ramch\.cursor\projects\e-STM-MMR\agent-tools\fonts\Roboto-Regular.ttf")
CHROME = Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe")

SOURCES = {
    "sign-in": ASSETS / "image-0590569c-c04a-40cb-aa90-0f40470d5052.png",
    "approvals": ASSETS / "image-f57a72cc-d691-40ed-87ff-cda6615f811d.png",
    "transactions": ASSETS / "image-9d339de9-a180-4aab-baa6-66ab5dde5798.png",
    "spending": ASSETS / "image-10c3d6df-d1e3-4430-83cd-0833cb4dfaee.png",
}

# Five approval cards. Cover the reference line and the bank-message box only.
CARD_BANDS = [
    (64, 230),
    (242, 408),
    (420, 586),
    (598, 764),
    (776, 980),
]
NOTE_BANDS = [
    (74, 200),
    (250, 400),
    (428, 580),
    (606, 760),
    (786, 960),
]

POSTERS = [
    ("01-open-on-your-laptop", "Open it on<br><em>your laptop</em>", "sign-in"),
    ("02-review-before-you-save", "Review bank text<br><em>before you save it</em>", "approvals"),
    ("03-edit-from-the-browser", "Edit the month<br><em>from the browser</em>", "transactions"),
    ("04-month-as-a-chart", "The month as<br><em>one chart</em>", "spending"),
]


def mask_approvals(src: Path) -> Image.Image:
    im = Image.open(src).convert("RGB")
    draw = ImageDraw.Draw(im)
    font = ImageFont.truetype(str(FONT_R), 13)
    for x0, x1 in CARD_BANDS:
        draw.rectangle((x0, 146, x1, 194), fill=(248, 250, 249))
        draw.text((x0 + 8, 162), "Sample bank message", font=font, fill=(120, 124, 130))
    for x0, x1 in NOTE_BANDS:
        draw.rectangle((x0, 452, x1, 478), fill=(255, 255, 255))
        draw.text((x0, 456), "Sample", font=font, fill=(40, 40, 44))
    return im


def save_screen(name: str) -> str:
    src = SOURCES[name]
    image = mask_approvals(src) if name == "approvals" else Image.open(src).convert("RGB")
    path = BUILD / f"{name}.jpg"
    image.save(path, quality=93, subsampling=0)
    return f"{name}.jpg"


HTML = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  @font-face {{
    font-family: Poster;
    src: url("../Roboto-Black.ttf") format("truetype");
    font-weight: 900;
  }}
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  html, body {{ width: 1920px; height: 1080px; overflow: hidden; }}
  .frame {{
    width: 1920px; height: 1080px; position: relative; overflow: hidden;
    background:
      radial-gradient(900px 520px at 100% 0%, rgba(226, 70, 96, 0.16), transparent 62%),
      radial-gradient(800px 500px at 0% 100%, rgba(106, 78, 93, 0.12), transparent 58%),
      linear-gradient(180deg, #fff 0%, #fbf6f7 100%);
  }}
  h1 {{
    position: absolute; top: 48px; left: 80px; right: 80px;
    font-family: Poster, sans-serif;
    font-size: 72px; font-weight: 900; letter-spacing: -2.2px;
    line-height: 0.96; text-align: center; color: #161216;
  }}
  h1 em {{ font-style: normal; color: #e24660; }}
  .window {{
    position: absolute; left: 80px; right: 80px; top: 230px;
    background: #fff; border-radius: 18px;
    box-shadow: 0 30px 70px rgba(22, 8, 16, 0.18);
    overflow: hidden;
  }}
  .window img {{ width: 100%; height: auto; display: block; }}
</style>
</head>
<body>
  <div class="frame">
    <h1>{title}</h1>
    <div class="window"><img src="{screen}" alt=""></div>
  </div>
</body>
</html>
"""


def main() -> None:
    BUILD.mkdir(parents=True, exist_ok=True)
    OUT.mkdir(parents=True, exist_ok=True)
    for file, title, source in POSTERS:
        screen = save_screen(source)
        html_path = BUILD / f"{file}.html"
        html_path.write_text(
            HTML.format(title=title, screen=screen),
            encoding="utf-8",
        )
        png = OUT / f"{file}.png"
        subprocess.run(
            [
                str(CHROME),
                "--headless=new",
                "--disable-gpu",
                "--hide-scrollbars",
                "--force-device-scale-factor=1",
                "--window-size=1920,1080",
                "--default-background-color=FFFFFFFF",
                "--virtual-time-budget=4000",
                f"--screenshot={png}",
                html_path.as_uri(),
            ],
            check=True,
        )
        with Image.open(png) as im:
            flat = im.convert("RGB")
            flat.save(png)
            print(png.name, flat.size)


if __name__ == "__main__":
    main()
