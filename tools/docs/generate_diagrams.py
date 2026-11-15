"""Render repository documentation as editable SVGs and matching high-resolution PNGs.

Run from any directory: py -3 tools/docs/generate_diagrams.py
Dependency: Pillow. Fonts are taken from Windows, with DejaVu fallbacks on Linux.
"""

from html import escape
from math import atan2, cos, hypot, pi, sin
from pathlib import Path
import os

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs" / "images"
SCALE = 2
BG, WHITE, INK, MUTED = "#F5F7FB", "#FFFFFF", "#152B42", "#56667A"
LINE, TEAL, PURPLE, BLUE = "#DCE3ED", "#087F8C", "#7354B6", "#315FC4"
TINT, LILAC = "#E7F4F3", "#F0EBFA"


def font_path(bold=False, mono=False):
    windows = Path(os.environ.get("WINDIR", "C:/Windows")) / "Fonts"
    candidates = [
        windows / ("consola.ttf" if mono else "seguisb.ttf" if bold else "segoeui.ttf"),
        Path("/usr/share/fonts/truetype/dejavu") /
        ("DejaVuSansMono.ttf" if mono else "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return str(candidate)
    raise RuntimeError("Install Segoe UI or DejaVu fonts before regenerating diagrams")


class Canvas:
    def __init__(self, width, height, background=BG):
        self.width, self.height = width, height
        self.image = Image.new("RGB", (width * SCALE, height * SCALE), background)
        self.draw = ImageDraw.Draw(self.image)
        self.svg = [
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
            f'viewBox="0 0 {width} {height}" role="img">',
            f'<rect width="{width}" height="{height}" fill="{background}"/>',
        ]

    def rect(self, x, y, w, h, fill=WHITE, stroke=LINE, radius=20):
        self.svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{radius}" '
                        f'fill="{fill}" stroke="{stroke}" stroke-width="1.5"/>')
        self.draw.rounded_rectangle(
            tuple(int(v * SCALE) for v in (x, y, x + w, y + h)),
            radius=int(radius * SCALE), fill=fill, outline=stroke, width=2 * SCALE,
        )

    def text(self, x, y, value, size=26, color=INK, bold=False, mono=False, center=False):
        font = ImageFont.truetype(font_path(bold, mono), size * SCALE)
        anchor = "middle" if center else "start"
        family = "Consolas,DejaVu Sans Mono,monospace" if mono else "Segoe UI,Arial,sans-serif"
        # y is the top of a line. Match the SVG baseline to the actual font ascent.
        ascent = font.getmetrics()[0] / SCALE
        self.svg.append(f'<text x="{x}" y="{y + ascent}" text-anchor="{anchor}" '
                        f'font-family="{family}" font-size="{size}" font-weight="{600 if bold else 400}" '
                        f'fill="{color}">{escape(value)}</text>')
        self.draw.text((x * SCALE, y * SCALE), value, font=font, fill=color,
                       anchor="mt" if center else "lt")

    def arrow(self, points, color=TEAL, width=3, dashed=False):
        encoded = " ".join(f"{x},{y}" for x, y in points)
        dash = ' stroke-dasharray="9 8"' if dashed else ""
        self.svg.append(f'<polyline points="{encoded}" fill="none" stroke="{color}" '
                        f'stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round"{dash}/>')
        if dashed:
            for start, end in zip(points, points[1:]):
                distance = hypot(end[0] - start[0], end[1] - start[1])
                for offset in range(0, int(distance), 17):
                    a, b = offset / distance, min(offset + 9, distance) / distance
                    self.draw.line([
                        ((start[0] + (end[0] - start[0]) * a) * SCALE,
                         (start[1] + (end[1] - start[1]) * a) * SCALE),
                        ((start[0] + (end[0] - start[0]) * b) * SCALE,
                         (start[1] + (end[1] - start[1]) * b) * SCALE),
                    ], fill=color, width=width * SCALE)
        else:
            self.draw.line([(x * SCALE, y * SCALE) for x, y in points], fill=color,
                           width=width * SCALE, joint="curve")
        end, previous = points[-1], points[-2]
        angle = atan2(end[1] - previous[1], end[0] - previous[0])
        triangle = [end] + [(end[0] - 15 * cos(angle + delta), end[1] - 15 * sin(angle + delta))
                            for delta in (-pi / 7, pi / 7)]
        self.svg.append(f'<polygon points="{" ".join(f"{x},{y}" for x, y in triangle)}" fill="{color}"/>')
        self.draw.polygon([(int(x * SCALE), int(y * SCALE)) for x, y in triangle], fill=color)

    def header(self, number, title, subtitle):
        self.text(70, 35, f"KOTLIN MVVM BASE / {number}", 19, TEAL, bold=True)
        self.text(70, 76, title, 44, bold=True)
        self.text(70, 137, subtitle, 25, MUTED)

    def node(self, x, y, w, h, title, detail, fill=TINT, accent=TEAL):
        self.rect(x, y, w, h, fill=fill, stroke=LINE)
        self.text(x + w / 2, y + 18, title, 29, accent, bold=True, center=True)
        self.text(x + w / 2, y + 65, detail, 21, MUTED, center=True)

    def save(self, name, title):
        OUT.mkdir(parents=True, exist_ok=True)
        self.svg.insert(1, f"<title>{escape(title)}</title>")
        (OUT / f"{name}.svg").write_text("\n".join(self.svg + ["</svg>"]) + "\n", encoding="utf-8")
        self.image.resize((self.width, self.height), Image.Resampling.LANCZOS).save(OUT / f"{name}.png")
        print(f"{name}: {self.width}x{self.height}, SVG + PNG")


def hero(name="hero", width=1600, height=500):
    c = Canvas(width, height, "#14263D")
    c.rect(60, 57, 67, 67, fill="#263D58", stroke="#42617F", radius=16)
    c.text(93, 63, "K", 42, "#8AE0CF", bold=True, center=True)
    c.text(151, 72, "ANDROID STARTER", 22, "#A2B7D0", bold=True)
    c.text(60, 159, "Kotlin MVVM Base", 66 if width == 1600 else 60, WHITE, bold=True)
    c.text(63, 260, "Five modules. Clear boundaries.", 34, "#9BE2D4")
    c.text(63, 316, "A practical starting point for Android.", 27, "#B9C8DB")
    for x, label, w in [(63, "Compose", 156), (235, "Hilt", 95), (346, "StateFlow", 180), (542, "Clean Architecture", 288)]:
        c.rect(x, 395 if height == 500 else 465, w, 49, "#203B56", "#37516D", radius=12)
        c.text(x + w / 2, 403 if height == 500 else 473, label, 22, "#E5EFF9", center=True)
    if width == 1600:
        for x, y, w, label, detail in [
            (1100, 74, 350, "app", "Android + Hilt"),
            (1000, 216, 240, "domain", "Business rules"),
            (1260, 216, 240, "data", "Repositories"),
            (1000, 358, 240, "shared", "Kotlin utilities"),
            (1260, 358, 240, "resources", "Theme + assets"),
        ]:
            c.rect(x, y, w, 106, "#203B56", "#37516D", radius=16)
            c.text(x + w / 2, y + 12, label, 30, "#B0EBDD", bold=True, center=True)
            c.text(x + w / 2, y + 61, detail, 20, "#B9C8DB", center=True)
    else:
        c.text(65, 570, "NguynNe3110 / Kotlin-multi-module", 23, "#9DB1CB")
    c.save(name, "Kotlin MVVM Base: Compose, Hilt and five architecture layers")


def structure():
    c = Canvas(1600, 1080)
    c.header("01", "Project structure", "Five Gradle modules, organized by layer. Screens stay in app/ui.")
    c.rect(70, 210, 700, 790)
    c.text(107, 238, ":app", 38, TEAL, bold=True)
    c.text(107, 293, "Android application / composition root", 24, MUTED)
    lines = [
        "src/main/java/com/example/base/", "├─ di/           Hilt providers", "├─ navigation/   Routes + NavHost",
        "├─ session/      DataStore adapter", "├─ ui/", "│  ├─ base/      State + effects",
        "│  ├─ todo/      Screen + ViewModel", "│  └─ users/     Screen + ViewModel",
        "├─ MainActivity.kt", "└─ MainApplication.kt", "", "src/test/       JVM + Robolectric",
    ]
    for i, line in enumerate(lines):
        c.text(107, 365 + i * 43, line, 25, INK, mono=True)
    for y, title, subtitle, lines in [
        (210, ":domain", "Kotlin/JVM / business contracts", ["model/    repository/    session/", "usecase/  usecase/base/"]),
        (470, ":data", "Kotlin/JVM / implementations", ["api/  datasource/  mapper/", "model/  repository/"]),
    ]:
        c.rect(820, y, 710, 230)
        c.text(857, y + 25, title, 38, PURPLE, bold=True)
        c.text(857, y + 80, subtitle, 24, MUTED)
        for i, line in enumerate(lines):
            c.text(857, y + 133 + i * 39, line, 25, mono=True)
    for y, title, detail in [(730, ":shared", "Result, errors, retry, logging, dispatchers"),
                              (880, ":resources", "Android library / theme, colors, strings")]:
        c.rect(820, y, 710, 120)
        c.text(857, y + 15, title, 33, BLUE, bold=True)
        c.text(857, y + 70, detail, 24, MUTED)
    c.text(70, 1032, "Each module owns its build script and source set. Hilt wiring lives in app/di.", 24, MUTED)
    c.save("project-structure", "Project structure of the five Gradle modules")


def dependencies():
    c = Canvas(1600, 1030)
    c.header("02", "Module dependencies", "Arrows mean Gradle project dependencies, not runtime data flow.")
    for points, color in [
        ([(690, 320), (470, 440)], TEAL), ([(910, 320), (1170, 440)], TEAL),
        ([(640, 265), (130, 265), (130, 785), (260, 785)], BLUE),
        ([(960, 265), (1470, 265), (1470, 785), (1340, 785)], BLUE),
        ([(420, 550), (420, 730)], TEAL),
        ([(580, 495), (820, 495), (820, 760), (1020, 760)], PURPLE),
        ([(580, 785), (1020, 785)], TEAL),
    ]:
        c.arrow(points, color)
    c.node(640, 210, 320, 110, ":app", "Android + Hilt", fill=TINT)
    c.node(260, 440, 320, 110, ":data", "Kotlin/JVM", fill=LILAC, accent=PURPLE)
    c.node(1020, 440, 320, 110, ":resources", "Android library", fill="#EBF0FC", accent=BLUE)
    c.node(260, 730, 320, 110, ":domain", "Kotlin/JVM", fill=LILAC, accent=PURPLE)
    c.node(1020, 730, 320, 110, ":shared", "Kotlin/JVM", fill=LILAC, accent=PURPLE)
    c.text(800, 365, "app wires concrete implementations", 21, MUTED, center=True)
    c.text(625, 820, "domain depends only on shared", 23, MUTED)
    c.rect(70, 900, 1460, 80, fill=WHITE)
    c.text(103, 922, "verifyModuleBoundaries checks these edges. Screen and ViewModel code uses domain contracts.", 25)
    c.save("module-dependencies", "All seven permitted dependencies between the five modules")


def data_flow():
    c = Canvas(1600, 1300)
    c.header("03", "From user action to UI state", "Two sample flows: paged network reads and observable local writes.")
    c.rect(70, 210, 1460, 390)
    c.text(105, 236, "01 / USERS", 24, TEAL, bold=True)
    c.text(105, 279, "A one-shot request returns a mapped page of domain models.", 25, MUTED)
    xs = [110, 400, 690, 980, 1270]
    labels = [("Screen", "Compose"), ("ViewModel", "StateFlow"), ("GetUsers", "Use case"),
              ("Repository", "DTO → domain"), ("Retrofit", "GET /users")]
    for a, b in zip(xs, xs[1:]):
        c.arrow([(a + 220, 403), (b, 403)])
    for x, (title, detail) in zip(xs, labels):
        c.node(x, 350, 220, 110, title, detail, fill=TINT)
    c.arrow([(1380, 460), (1380, 535), (510, 535), (510, 460)], BLUE, dashed=True)
    c.text(650, 490, "Response → mapping → Result<PageResult<User>>", 24, BLUE)
    c.text(110, 550, "ViewModel updates UiState; Compose renders the new page.", 23, MUTED)

    c.rect(70, 640, 1460, 590)
    c.text(105, 666, "02 / TODO", 24, PURPLE, bold=True)
    c.text(105, 709, "Writes and list observation have separate outcomes.", 25, MUTED)
    xs = [110, 470, 830, 1190]
    labels = [("Screen", "Saved draft"), ("ViewModel", "Submit action"),
              ("Add / Toggle", "Use case"), ("Repository", "Data source")]
    for a, b in zip(xs, xs[1:]):
        c.arrow([(a + 280, 823), (b, 823)], PURPLE)
    for x, (title, detail) in zip(xs, labels):
        c.node(x, 770, 280, 110, title, detail, fill=LILAC, accent=PURPLE)
    c.arrow([(1270, 880), (1270, 940), (610, 940), (610, 880)], BLUE, dashed=True)
    c.text(710, 893, "Mutation result: clear draft only on success", 23, BLUE)
    c.arrow([(1420, 880), (1420, 1030)], PURPLE)
    for x, (title, detail) in zip(xs, [
        ("Compose", "Render list"), ("UiState", "ViewModel"),
        ("ObserveTodos", "Flow use case"), ("Source of truth", "Flow<List<Todo>>"),
    ]):
        c.node(x, 1030, 280, 110, title, detail, fill=TINT)
    for left, right in zip(xs, xs[1:]):
        c.arrow([(right, 1083), (left + 280, 1083)])
    c.text(110, 1172, "No read-after-write in ViewModel. A list read error never replays a successful mutation.", 24, MUTED)
    c.text(70, 1250, "Solid arrows: calls / observation. Dashed arrows: results. Domain contracts keep layers separate.", 23, MUTED)
    c.save("data-flow", "Users request flow and Todo mutation plus observation flow")


def session():
    c = Canvas(1600, 820)
    c.header("04", "Session-driven startup", "The app observes one SessionState instead of running a startup task framework.")
    c.arrow([(1230, 340), (1230, 225), (275, 225), (275, 340)], BLUE)
    c.text(605, 185, "Retry → restart collection", 25, BLUE)
    c.arrow([(450, 395), (625, 395)])
    c.text(475, 350, "Read OK", 23, TEAL)
    c.arrow([(975, 395), (1150, 395)], PURPLE)
    c.text(994, 350, "Read error", 22, PURPLE)
    c.arrow([(275, 465), (275, 605), (1325, 605), (1325, 465)], PURPLE)
    c.text(665, 566, "Initial read failure", 24, PURPLE)
    c.node(100, 340, 350, 125, "Loading", "Show progress")
    c.node(625, 340, 350, 125, "Ready", "Session + expiration flag")
    c.node(1150, 340, 350, 125, "Error", "Show Retry", fill=LILAC, accent=PURPLE)
    c.rect(70, 670, 1460, 100)
    c.text(105, 689, "A read failure also wakes waiting save / clear / expire calls with the error.", 25)
    c.text(105, 729, "Expiration is persistent session state; it does not depend on a snackbar collector.", 23, MUTED)
    c.save("session-state", "Session loading, ready, error and retry transitions")


if __name__ == "__main__":
    hero()
    hero("social-preview", 1280, 640)
    structure()
    dependencies()
    data_flow()
    session()
