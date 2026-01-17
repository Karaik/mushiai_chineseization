import { defineConfig } from 'vite';
import path from 'path';
import { viteSingleFile } from 'vite-plugin-singlefile';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  root: '.',
  base: './',
  plugins: [vue(), viteSingleFile()],
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
