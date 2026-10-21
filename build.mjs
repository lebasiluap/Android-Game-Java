import { readFile, writeFile } from "node:fs/promises";
const html = await readFile(new URL("index.html", import.meta.url), "utf8");
const css = await readFile(new URL("style.css", import.meta.url), "utf8");
const engine = (
  await readFile(new URL("engine.js", import.meta.url), "utf8")
).replaceAll("export ", "");
const app = (
  await readFile(new URL("app.js", import.meta.url), "utf8")
).replace(/import[\s\S]*?from ['"]\.\/engine\.js['"];?\s*/, "");
const bundled = html
  .replace(
    /<link\s+rel="stylesheet"\s+href="style.css"\s*\/?\s*>/,
    `<style>${css}</style>`,
  )
  .replace(
    /<script type="module" src="app.js"><\/script>/,
    `<script type="module">${engine}\n${app}</script>`,
  );
await writeFile(new URL("Play-Rally-Club.html", import.meta.url), bundled);
console.log("Created standalone Play-Rally-Club.html");
