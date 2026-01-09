import { defineConfig } from 'vite';
import path from 'path';
import { viteSingleFile } from 'vite-plugin-singlefile';

export default defineConfig({
  root: '.',
  base: './',
  plugins: [viteSingleFile()],
  build: {
    outDir: path.resolve(__dirname, '../../../../docs'),
    emptyOutDir: false,
    assetsInlineLimit: 100000000,
    cssCodeSplit: false,
    rollupOptions: {
      input: {
        index: path.resolve(__dirname, '汉化感言.html')
      }
    }
  }
});
