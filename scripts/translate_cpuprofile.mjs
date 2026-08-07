#!/usr/bin/env node
// Translate a V8 .cpuprofile taken over an :advanced ClojureScript build
// back to original names and positions through its source map, so the
// artifact people actually run is what gets profiled.
//
//   node scripts/translate_cpuprofile.mjs PROFILE.cpuprofile BUNDLE.js.map
//
// Writes PROFILE.translated.cpuprofile (loadable in Chrome DevTools) and
// prints self/inclusive hotspot tables aggregated by original position.
// Frames inlined by Closure vanish into their callers; attribution is by
// the surviving frame's definition site, which is the honest granularity
// of the shipped artifact. Requires the `source-map` npm package; point
// SOURCE_MAP_PKG at its entry file when it is not resolvable from here:
//   SOURCE_MAP_PKG=/path/node_modules/source-map/source-map.js node ...

import { readFileSync, writeFileSync } from 'fs';

const { SourceMapConsumer } = await import(
  process.env.SOURCE_MAP_PKG ?? 'source-map');

const [profilePath, mapPath] = process.argv.slice(2);
if (!mapPath) {
  console.error('usage: translate_cpuprofile.mjs PROFILE.cpuprofile MAP');
  process.exit(1);
}

const profile = JSON.parse(readFileSync(profilePath, 'utf8'));
const consumer = await new SourceMapConsumer(
  JSON.parse(readFileSync(mapPath, 'utf8')));

for (const node of profile.nodes) {
  const frame = node.callFrame;
  if (!frame.url || frame.lineNumber < 0) continue;
  const original = consumer.originalPositionFor({
    line: frame.lineNumber + 1,
    column: frame.columnNumber,
  });
  if (original.source) {
    const source = original.source.replace(/^.*\/(qrity|cljs|goog)\//, '$1/');
    frame.functionName =
      (original.name || frame.functionName || '(anonymous)') +
      ` @ ${source}:${original.line}`;
  }
}
consumer.destroy();

const out = profilePath.replace(/\.cpuprofile$/, '.translated.cpuprofile');
writeFileSync(out, JSON.stringify(profile));

const children = new Map(profile.nodes.map((n) => [n.id, n.children ?? []]));
const nodes = new Map(profile.nodes.map((n) => [n.id, n]));
const parent = new Map();
for (const [id, cs] of children) for (const c of cs) parent.set(c, id);
const total = profile.nodes.reduce((s, n) => s + (n.hitCount ?? 0), 0);

const inclusive = new Map();
const memo = new Map();
const subtree = (id) => {
  if (memo.has(id)) return memo.get(id);
  const s = (nodes.get(id).hitCount ?? 0) +
    (children.get(id) ?? []).reduce((a, c) => a + subtree(c), 0);
  memo.set(id, s);
  return s;
};
const name = (id) => nodes.get(id).callFrame.functionName || '(anonymous)';
const hasSameAncestor = (id) => {
  const n = name(id);
  for (let p = parent.get(id); p !== undefined; p = parent.get(p))
    if (name(p) === n) return true;
  return false;
};
const self = new Map();
for (const n of profile.nodes) {
  self.set(name(n.id), (self.get(name(n.id)) ?? 0) + (n.hitCount ?? 0));
  if (!hasSameAncestor(n.id))
    inclusive.set(name(n.id), (inclusive.get(name(n.id)) ?? 0) + subtree(n.id));
}

const table = (map, label, filter) => {
  console.log(`-- ${label} --`);
  [...map.entries()]
    .filter(([k]) => !filter || k.includes(filter))
    .sort((a, b) => b[1] - a[1])
    .slice(0, 15)
    .forEach(([k, v]) =>
      console.log(`${((100 * v) / total).toFixed(1).padStart(5)}%  ${k}`));
};
console.log(`total samples: ${total}  (translated -> ${out})`);
table(inclusive, 'inclusive % (qrity)', 'qrity/');
table(self, 'self % (any)');
