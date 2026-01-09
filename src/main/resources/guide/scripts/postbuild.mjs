import { promises as fs } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { minify } from 'html-minifier-terser';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(__dirname, '..');
const docsDir = path.resolve(rootDir, '../../../../docs');
const source = path.join(docsDir, '汉化感言.html');
const target = path.join(docsDir, 'index.html');
const assetsDir = path.join(docsDir, 'assets');
const iconPath = path.join(rootDir, 'icon.ico');

async function injectFavicon(html) {
  let iconData = null;
  try {
    iconData = await fs.readFile(iconPath);
  } catch (err) {
    console.warn(`[postbuild] Missing icon: ${err.message}`);
    return html;
  }

  const favicon = `<link rel="icon" type="image/x-icon" href="data:image/x-icon;base64,${iconData.toString('base64')}">`;
  const cleaned = html.replace(/<link[^>]*rel=["']icon["'][^>]*>\\s*/gi, '');
  if (cleaned.includes('</head>')) {
    return cleaned.replace('</head>', `${favicon}</head>`);
  }
  return cleaned;
}

try {
  let html = await fs.readFile(source, 'utf8');
  html = await injectFavicon(html);
  const minified = await minify(html, {
    collapseWhitespace: true,
    removeComments: true,
    minifyCSS: true,
    minifyJS: true,
    removeRedundantAttributes: true,
    removeEmptyAttributes: true,
    collapseBooleanAttributes: true,
    useShortDoctype: true
  });

  await fs.writeFile(target, minified, 'utf8');
  if (source !== target) {
    await fs.rm(source, { force: true });
  }
  await fs.rm(assetsDir, { recursive: true, force: true });
  console.log('[postbuild] Wrote docs/index.html');
} catch (err) {
  console.warn(`[postbuild] Skip: ${err.message}`);
}
