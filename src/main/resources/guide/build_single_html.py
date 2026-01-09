import base64
import csv
import json
import re
from pathlib import Path
from typing import Dict, List


IMAGE_MIME = {
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".gif": "image/gif",
    ".webp": "image/webp",
    ".svg": "image/svg+xml",
}


def find_repo_root(start: Path) -> Path:
    for parent in [start] + list(start.parents):
        if (parent / ".git").exists() or (parent / "pom.xml").exists():
            return parent
    return start


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def to_data_uri(path: Path) -> str:
    mime = IMAGE_MIME.get(path.suffix.lower(), "application/octet-stream")
    data = base64.b64encode(path.read_bytes()).decode("ascii")
    return f"data:{mime};base64,{data}"


def inline_css_urls(css_text: str, css_path: Path) -> str:
    def replacer(match: re.Match) -> str:
        raw = match.group(1).strip().strip("'\"")
        if raw.startswith("data:") or raw.startswith("http"):
            return f"url({raw})"
        target = (css_path.parent / raw).resolve()
        if not target.exists():
            return match.group(0)
        return f"url({to_data_uri(target)})"

    return re.sub(r"url\(([^)]+)\)", replacer, css_text)


def minify_css(css_text: str) -> str:
    css_text = re.sub(r"/\*.*?\*/", "", css_text, flags=re.S)
    css_text = re.sub(r"\s+", " ", css_text)
    css_text = re.sub(r"\s*([{}:;,])\s*", r"\1", css_text)
    css_text = css_text.replace(";}", "}")
    return css_text.strip()


def extract_avatar_names(csv_path: Path) -> List[str]:
    if not csv_path.exists():
        return []
    names: List[str] = []
    with csv_path.open("r", encoding="utf-8") as fh:
        reader = csv.DictReader(fh)
        for row in reader:
            avatar = (row.get("Avatar") or "").strip()
            if avatar:
                names.append(avatar)
    return names


def build_image_map(guide_dir: Path, avatar_names: List[str]) -> Dict[str, str]:
    images: Dict[str, str] = {}
    head_dir = guide_dir / "images" / "head"
    base_images_dir = guide_dir / "images"

    for name in avatar_names:
        candidate = head_dir / name
        if candidate.exists():
            images[name] = to_data_uri(candidate)

    background = base_images_dir / "chart_back8_1.png"
    if background.exists():
        images[background.name] = to_data_uri(background)

    return images


def patch_js(js_text: str, image_map: Dict[str, str]) -> str:
    csv_match = re.search(r"const csvContent = `[\s\S]*?`;", js_text)
    if not csv_match:
        raise ValueError("csvContent block not found in app.js")

    map_json = json.dumps(image_map, ensure_ascii=False)
    map_code = (
        f"\nconst INLINE_IMAGE_MAP = {map_json};\n"
        "function resolveImageSrc(name) {\n"
        "    if (!name) return '';\n"
        "    if (name.startsWith('data:')) return name;\n"
        "    return INLINE_IMAGE_MAP[name] || '';\n"
        "}\n"
    )
    js_text = js_text[: csv_match.end()] + map_code + js_text[csv_match.end() :]

    js_text = js_text.replace(
        "img.src = `images/head/${node.runtimeAvatar}`;",
        "const avatarSrc = resolveImageSrc(node.runtimeAvatar);\n            img.src = avatarSrc || `images/head/${node.runtimeAvatar}`;",
        1,
    )
    js_text = js_text.replace(
        "dialogArtImg.src = 'images/chart_back8_1.png';",
        "dialogArtImg.src = resolveImageSrc('chart_back8_1.png');",
        1,
    )

    return js_text


def minify_html(html_text: str) -> str:
    blocks: Dict[str, str] = {}
    def stash(match: re.Match) -> str:
        key = f"@@BLOCK{len(blocks)}@@"
        blocks[key] = match.group(0)
        return key

    html_text = re.sub(r"<script[\s\S]*?</script>", stash, html_text, flags=re.I)
    html_text = re.sub(r"<style[\s\S]*?</style>", stash, html_text, flags=re.I)

    html_text = re.sub(r"<!--.*?-->", "", html_text, flags=re.S)
    html_text = re.sub(r">\s+<", "><", html_text)
    html_text = re.sub(r"\s{2,}", " ", html_text)
    html_text = html_text.strip()

    for key, block in blocks.items():
        html_text = html_text.replace(key, block)
    return html_text


def main() -> None:
    guide_dir = Path(__file__).resolve().parent
    repo_root = find_repo_root(guide_dir)
    docs_dir = repo_root / "docs"
    docs_dir.mkdir(parents=True, exist_ok=True)

    html_files = list(guide_dir.glob("*.html"))
    if not html_files:
        raise SystemExit("No HTML file found in guide directory.")
    html_path = html_files[0]

    css_path = guide_dir / "css" / "css.css"
    js_path = guide_dir / "js" / "app.js"
    csv_path = guide_dir / "data.inline.csv"

    html_text = read_text(html_path)
    css_text = inline_css_urls(read_text(css_path), css_path)
    css_text = minify_css(css_text)

    avatar_names = extract_avatar_names(csv_path)
    image_map = build_image_map(guide_dir, avatar_names)
    js_text = patch_js(read_text(js_path), image_map)

    html_text = re.sub(
        r"<link[^>]*href=\"css/css.css\"[^>]*>",
        f"<style>{css_text}</style>",
        html_text,
        count=1,
    )
    html_text = re.sub(
        r"<script[^>]*src=\"js/app.js\"[^>]*></script>",
        f"<script>{js_text}</script>",
        html_text,
        count=1,
    )

    html_text = minify_html(html_text)

    output_path = docs_dir / "index.html"
    output_path.write_text(html_text, encoding="utf-8")
    print(f"Wrote {output_path}")


if __name__ == "__main__":
    main()
