// Black-box jsQR runner: PNG paths in, JSON lines out. jsqr@1.4.0, pngjs@7.0.0.
import { readFileSync } from 'fs';
import { PNG } from 'pngjs';
import jsQR from 'jsqr';

for (const path of process.argv.slice(2)) {
  try {
    const png = PNG.sync.read(readFileSync(path));
    const started = process.hrtime.bigint();
    const result = jsQR(new Uint8ClampedArray(png.data), png.width, png.height);
    const ms = Number(process.hrtime.bigint() - started) / 1e6;
    console.log(JSON.stringify({ path, ms, payload: result ? result.data : null }));
  } catch (error) {
    console.log(JSON.stringify({ path, error: String(error) }));
  }
}
