import { defineConfig } from 'tsdown';

export default defineConfig({
  entry: ['../acidify-core/build/dist/js/productionLibrary/acidify-acidify-core.mjs'],
  format: 'esm',
  clean: true,
  dts: false,
  target: false,
  plugins: [
    {
      name: 'hack-eval-require',
      transform(code, id) {
        const banner =
          `import { createRequire as __cr } from "node:module";\n` +
          `const require = __cr(import.meta.url);\n`;
        const hitRE = /eval\(\s*['"]require['"]\s*\)/
        if (!/\.(mjs|js|mts|ts|jsx|tsx)$/.test(id)) return null;
        if (!hitRE.test(code)) return null;
        if (code.includes("createRequire as __cr") && code.includes("__cr(import.meta.url)")) {
          return null;
        }
        return {
          code: banner + code,
          map: null,
        };
      }
    }
  ],
});