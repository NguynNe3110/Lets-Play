# Documentation images

These assets describe the current five-module architecture and sample flows.
The main READMEs embed PNGs; SVGs remain available for editing and scaling.

| File | Purpose |
| --- | --- |
| `hero.png` / `hero.svg` | README header |
| `project-structure.png` / `project-structure.svg` | Module and package ownership |
| `module-dependencies.png` / `module-dependencies.svg` | Seven allowed Gradle project dependency edges |
| `data-flow.png` / `data-flow.svg` | Users request flow and Todo mutation/observation flow |
| `session-state.png` / `session-state.svg` | Loading, Ready, Error, and Retry transitions |
| `social-preview.png` / `social-preview.svg` | 1280 × 640 GitHub social preview artwork |

## Regenerate

From the repository root:

```bash
python -m pip install Pillow
python tools/docs/generate_diagrams.py
```

On Windows, use `py -3` if needed. The generator resolves paths from its own location,
so it can also be run from a different working directory.

Fonts: Segoe UI and Consolas on Windows; DejaVu Sans and DejaVu Sans Mono on Linux.
PNG rendering uses Pillow with supersampling. The script uses the same coordinates and content for SVG output.
Font fallback can produce small text differences when viewing an SVG on another platform.

After regeneration, inspect text readability, arrow direction, node boundaries, and whether the diagrams
still match the implementation. Keep SVG and PNG versions together in a contribution.

Original documentation artwork is distributed under the repository's [MIT License](../../LICENSE).
