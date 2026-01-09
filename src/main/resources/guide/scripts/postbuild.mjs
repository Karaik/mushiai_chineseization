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

try {
  const html = await fs.readFile(source, 'utf8');
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
