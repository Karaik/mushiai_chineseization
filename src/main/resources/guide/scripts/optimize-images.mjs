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

async function listImageFiles(dir) {
  let entries = [];
  try {
    entries = await fs.readdir(dir, { withFileTypes: true });
  } catch (err) {
    console.warn(`[optimize-images] Missing directory: ${err.message}`);
    return [];
  }

  const results = [];
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      const nested = await listImageFiles(fullPath);
      results.push(...nested);
      continue;
    }
    if (!/\.(png|jpg|jpeg)$/i.test(entry.name)) continue;
    results.push(fullPath);
  }
  return results;
}

function isHeadImage(filePath) {
  const normalized = path.resolve(filePath);
  const headRoot = path.resolve(headDir) + path.sep;
  return normalized.startsWith(headRoot);
}

async function convertAllImages() {
  const files = await listImageFiles(imagesDir);
  for (const inputPath of files) {
    const outputPath = inputPath.replace(/\.(png|jpg|jpeg)$/i, '.webp');
    const options = isHeadImage(inputPath) ? { quality: 80 } : { lossless: true };
    await ensureWebp(inputPath, outputPath, options);
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
  const webpSet = new Set(headEntries.filter(name => name.toLowerCase().endsWith('.webp')).map(name => name.toLowerCase()));

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
      const match = avatar.match(/^(.*)\.(png|jpg|jpeg|webp)$/i);
      const base = match ? match[1] : avatar;
      const candidate = `${base}.webp`;
      if (webpSet.has(candidate.toLowerCase())) {
        avatar = candidate;
      } else {
        avatar = '';
      }
    }

    output.push(`${key},${avatar},${episode},${content}`);
  }

  await fs.writeFile(csvPath, output.join('\n'), 'utf8');
}

await convertAllImages();
await updateCsvForWebp();

console.log('[optimize-images] Done');
