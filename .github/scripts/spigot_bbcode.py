"""Turns README.md and the release notes of one version into BBCode for SpigotMC.

SpigotMC has no API for posting updates, so the release workflow attaches these files to the GitHub
release and they are pasted by hand. Spigot does not render Markdown, SVG images or tables, so
badges become PNG images and tables become lists.

Usage: python spigot_bbcode.py <version> <output folder>
"""
import re
import sys
from pathlib import Path

HEADING_COLOR = "#9b59ff"

# The badges in the README are SVG; these PNG versions of the same badges show on Spigot.
PNG_BADGES = [
    (re.compile(r"_vector\.svg$"), "_46h.png"),
    (re.compile(r"/compact\.svg$"), "/compact.png"),
]


def png(url):
    for pattern, replacement in PNG_BADGES:
        url = pattern.sub(replacement, url)
    return url


def inline(text):
    text = re.sub(r"`([^`]+)`", r"[FONT=Courier New]\1[/FONT]", text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"[B]\1[/B]", text)
    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r"[URL='\2']\1[/URL]", text)
    return text


def centered_images(html):
    """The images of an HTML block, keeping their links."""
    parts = []
    for link, image in re.findall(r'(?:<a href="([^"]+)">\s*)?<img [^>]*src="([^"]+)"[^>]*>', html):
        tag = f"[IMG]{png(image)}[/IMG]"
        parts.append(f"[URL='{link}']{tag}[/URL]" if link else tag)
    return "[CENTER]" + " ".join(parts) + "[/CENTER]"


def table(rows):
    """A Markdown table as a list: the first column in bold, the others after it."""
    items = []
    for row in rows[2:]:
        cells = [inline(cell.strip()) for cell in row.strip().strip("|").split("|")]
        items.append(f"[*][B]{cells[0]}[/B]: " + " - ".join(cells[1:]))
    return "[LIST]\n" + "\n".join(items) + "\n[/LIST]"


def convert(markdown, heading_sizes=(6, 5, 4)):
    lines = markdown.replace("\r\n", "\n").split("\n")
    out = []
    index = 0
    while index < len(lines):
        line = lines[index]
        stripped = line.strip()

        if stripped.startswith("```"):
            code = []
            index += 1
            while index < len(lines) and not lines[index].strip().startswith("```"):
                code.append(lines[index])
                index += 1
            out.append("[CODE]" + "\n".join(code) + "[/CODE]")
        elif stripped.startswith("<img"):
            out.append(centered_images(line))
        elif stripped.startswith("<p"):
            block = [line]
            while "</p>" not in block[-1] and index + 1 < len(lines):
                index += 1
                block.append(lines[index])
            out.append(centered_images("\n".join(block)))
        elif stripped == "---":
            out.append("")
        elif stripped.startswith("|"):
            rows = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                rows.append(lines[index])
                index += 1
            out.append(table(rows))
            continue
        elif stripped.startswith("- "):
            items = []
            while index < len(lines) and lines[index].strip().startswith("- "):
                items.append("[*]" + inline(lines[index].strip()[2:]))
                index += 1
            out.append("[LIST]\n" + "\n".join(items) + "\n[/LIST]")
            continue
        elif match := re.match(r"^(#{1,3}) (.*)$", stripped):
            size = heading_sizes[len(match.group(1)) - 1]
            title = inline(match.group(2))
            if len(match.group(1)) == 2:
                title = f"[COLOR={HEADING_COLOR}]{title}[/COLOR]"
            out.append(f"[SIZE={size}][B]{title}[/B][/SIZE]")
        else:
            out.append(inline(line))
        index += 1

    # Spigot shows every line break, so runs of blank lines are folded into one.
    return re.sub(r"\n{3,}", "\n\n", "\n".join(out)).strip() + "\n"


def release_notes(changelog, version):
    match = re.search(rf"^## \[{re.escape(version)}\][^\n]*\n(.*?)(?=^## \[|\Z)", changelog, re.S | re.M)
    if not match:
        sys.exit(f"CHANGELOG.md has no section for {version}")
    return match.group(1).strip()


def main():
    version, folder = sys.argv[1], Path(sys.argv[2])
    folder.mkdir(parents=True, exist_ok=True)
    readme = Path("README.md").read_text(encoding="utf-8")
    changelog = Path("CHANGELOG.md").read_text(encoding="utf-8")
    (folder / "spigot-description.bbcode").write_text(convert(readme), encoding="utf-8")
    (folder / "spigot-update.bbcode").write_text(convert(release_notes(changelog, version), (5, 4, 4)), encoding="utf-8")


if __name__ == "__main__":
    main()
