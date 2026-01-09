import { promises as fs } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import sharp from 'sharp';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(__dirname, '..');
const imagesDir = path.join(rootDir, 'images');
const headDir = path.join(imagesDir, 'head');
const csvPath = path.join(rootDir, 'data.inline.csv');

async function ensureWebp(inputPath, outputPath, options) {
  try {
    const inputStat = await fs.stat(inputPath);
    let outputStat = null;
    try {
      outputStat = await fs.stat(outputPath);
    } catch {
      outputStat = null;
    }

    if (outputStat && outputStat.mtimeMs >= inputStat.mtimeMs) {
      return;
    }

    await sharp(inputPath).webp(options).toFile(outputPath);
  } catch (err) {
    console.warn(`[optimize-images] Skip ${inputPath}: ${err.message}`);
  }
}

async function convertBackgrounds() {
  const backgrounds = [
    { name: 'ev45a.png', quality: 78 },
    { name: 'egg.png', quality: 78 },
    { name: 'chart_back8_1.png', quality: 78 }
  ];

  for (const bg of backgrounds) {
    const inputPath = path.join(imagesDir, bg.name);
    const outputPath = inputPath.replace(/\.png$/i, '.webp');
    await ensureWebp(inputPath, outputPath, { quality: bg.quality });
  }
}

async function convertAvatars() {
  let entries = [];
  try {
    entries = await fs.readdir(headDir);
  } catch (err) {
    console.warn(`[optimize-images] Missing head directory: ${err.message}`);
    return;
  }

  for (const entry of entries) {
    if (!/\.(png|jpg|jpeg)$/i.test(entry)) continue;
    const inputPath = path.join(headDir, entry);
    const outputPath = inputPath.replace(/\.(png|jpg|jpeg)$/i, '.webp');
    await ensureWebp(inputPath, outputPath, { quality: 80 });
  }
}

function normalizeAvatarName(raw) {
  let value = raw.trim();
  if (value.startsWith('"') && value.endsWith('"')) {
    value = value.slice(1, -1);
  }
  value = value.replace(/,([a-z0-9]+)$/i, '.$1');
  return value;
}

async function updateCsvForWebp() {
  let csvText = '';
  try {
    csvText = await fs.readFile(csvPath, 'utf8');
  } catch (err) {
    console.warn(`[optimize-images] Missing CSV: ${err.message}`);
    return;
  }

  let headEntries = [];
  try {
    headEntries = await fs.readdir(headDir);
  } catch {
    headEntries = [];
  }
  const webpSet = new Set(headEntries.filter(name => name.toLowerCase().endsWith('.webp')));

  const lines = csvText.split(/\r?\n/);
  if (lines.length === 0) return;

  const output = [lines[0]];
  for (let i = 1; i < lines.length; i++) {
    const line = lines[i];
    if (!line) continue;

    const safeLine = line.replace(/\"([^\"]+),(png|jpg|jpeg|webp)\"/i, '$1.$2');
    const first = safeLine.indexOf(',');
    const second = safeLine.indexOf(',', first + 1);
    const third = safeLine.indexOf(',', second + 1);

    if (first < 0 || second < 0 || third < 0) {
      output.push(line);
      continue;
    }

    const key = safeLine.slice(0, first);
    const avatarRaw = safeLine.slice(first + 1, second);
    const episode = safeLine.slice(second + 1, third);
    const content = safeLine.slice(third + 1);

    let avatar = normalizeAvatarName(avatarRaw);
    if (avatar) {
      const match = avatar.match(/^(.*)\.(png|jpg|jpeg)$/i);
      if (match) {
        const candidate = `${match[1]}.webp`;
        if (webpSet.has(candidate)) {
          avatar = candidate;
        }
      }
    }

    output.push(`${key},${avatar},${episode},${content}`);
  }

  await fs.writeFile(csvPath, output.join('\n'), 'utf8');
}

await convertBackgrounds();
await convertAvatars();
await updateCsvForWebp();

console.log('[optimize-images] Done');
