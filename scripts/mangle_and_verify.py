#!/usr/bin/env python3
"""Mangle QR pictures with ImageMagick and cross-verify decoders.

Builds a matrix of {encoder} x {mangling} x {decoder}:

- Encoders: this project (clojure -M:emit-symbol, PBM -> PNG via
  ImageMagick) and qrencode (independent implementation).
- Manglings: ImageMagick transforms - blur, noise, pixelation, rotation,
  shear, perspective, low contrast, dimming, quiet-zone cropping, and an
  overlay blot.
- Decoders: this project (clojure -M:decode-image, one JVM for the whole
  batch), zbarimg, and OpenCV's QRCodeDetector.

Every decoder failure is reported. The exit status enforces only the
REQUIRED set: this project's decoder must read every mangled picture of
both encoders except the declared stretch manglings, and the foreign
decoders must read this project's clean symbols. Stretch results are
informative - they compare robustness rather than gate it.

Usage: python3 scripts/mangle_and_verify.py [--keep DIR]
"""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

import cv2

REPOSITORY_ROOT = Path(__file__).resolve().parent.parent

PAYLOADS = {
    "numeric": ("31415926535897932384626433832795", "m"),
    "url": ("https://example.com/qr?id=1234567890&lang=cs", "m"),
    "czech": ("Příliš žluťoučký kůň 042", "q"),
}

# qrencode emits raw UTF-8 octets without an ECI header; our decoder then
# reads them as ISO/IEC 8859-1 text, so byte-exactness is checked through
# the octets instead of the payload string for non-ASCII qrencode symbols.
QRENCODE_PAYLOADS = {"numeric", "url"}

MANGLINGS = {
    "clean": [],
    "blur": ["-blur", "0x1.5"],
    "noise": ["-attenuate", "0.5", "+noise", "Gaussian"],
    "pixelate": ["-resize", "25%", "-scale", "400%"],
    "rotate": ["-background", "white", "-rotate", "17"],
    "shear": ["-background", "white", "-shear", "8x4"],
    "contrast": ["-brightness-contrast", "-20x-55"],
    "dim": ["-modulate", "45"],
    "crop": ["-shave", "20x20"],
}

# Stretch manglings are reported but do not gate the exit status.
STRETCH = {"noise", "pixelate"}

# ImageMagick 7 installs `magick`; ImageMagick 6 (Ubuntu 24.04's package)
# only installs `convert`. Every invocation here uses the shared
# `input [operations] output` form both entry points accept.
MAGICK = "magick" if shutil.which("magick") else "convert"


def run(command: list[str], **kwargs) -> subprocess.CompletedProcess:
    return subprocess.run(command, capture_output=True, text=True, **kwargs)


def emit_ours(payload: str, level: str, target: Path) -> None:
    pbm = target.with_suffix(".pbm")
    completed = run(
        ["clojure", "-M:emit-symbol", payload, level, str(pbm)],
        cwd=REPOSITORY_ROOT,
    )
    if completed.returncode != 0:
        raise RuntimeError(f"emit-symbol failed: {completed.stderr}")
    # Force 8-bit grayscale: a bilevel PNG quantizes later Gaussian noise
    # into full-swing salt-and-pepper, which no analog camera produces.
    run([MAGICK, str(pbm), "-depth", "8", "-type", "Grayscale",
         str(target)], check=True)


def emit_qrencode(payload: str, target: Path) -> None:
    run(
        ["qrencode", "-s", "8", "-m", "4", "-o", str(target), payload],
        check=True,
    )


def mangle(source: Path, arguments: list[str], target: Path) -> None:
    run([MAGICK, str(source), *arguments, str(target)], check=True)


def overlay_blot(source: Path, target: Path) -> None:
    probe = cv2.imread(str(source))
    height, width = probe.shape[:2]
    x, y, radius = int(width * 0.55), int(height * 0.55), int(width * 0.03)
    run(
        [
            MAGICK, str(source),
            "-fill", "white",
            "-draw", f"circle {x},{y} {x + radius},{y}",
            str(target),
        ],
        check=True,
    )


def decode_ours(paths: list[Path]) -> dict[str, dict]:
    completed = run(
        ["clojure", "-M:decode-image", *map(str, paths)],
        cwd=REPOSITORY_ROOT,
    )
    if completed.returncode != 0:
        raise RuntimeError(f"decode-image failed: {completed.stderr}")
    results: dict[str, dict] = {}
    for line in completed.stdout.splitlines():
        if line.startswith("{"):
            record = json.loads(line)
            results[record["path"]] = record
    return results


def decode_zbar(path: Path) -> str | None:
    completed = run(["zbarimg", "--quiet", "--raw", str(path)])
    return completed.stdout.strip() or None


def decode_opencv(path: Path) -> str | None:
    image = cv2.imread(str(path))
    if image is None:
        return None
    text, *_ = cv2.QRCodeDetector().detectAndDecode(image)
    return text or None


def our_read_matches(record: dict, payload: str, encoder: str) -> bool:
    if record.get("status") != "ok":
        return False
    if record.get("payload") == payload:
        return True
    # Foreign UTF-8 without ECI: compare octets.
    octets = record.get("octets")
    return (
        encoder == "qrencode"
        and octets is not None
        and bytes(octets) == payload.encode("utf-8")
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--keep", type=Path, default=None,
                        help="write images here instead of a temp dir")
    options = parser.parse_args()

    workdir = options.keep or Path(tempfile.mkdtemp(prefix="qrity-mangle-"))
    workdir.mkdir(parents=True, exist_ok=True)

    cases = []  # (case-id, encoder, payload, mangling, path)
    for name, (payload, level) in PAYLOADS.items():
        for encoder in ("qrity", "qrencode"):
            if encoder == "qrencode" and name not in QRENCODE_PAYLOADS:
                continue
            clean = workdir / f"{name}-{encoder}-clean.png"
            if encoder == "qrity":
                emit_ours(payload, level, clean)
            else:
                emit_qrencode(payload, clean)
            for mangling, arguments in MANGLINGS.items():
                target = workdir / f"{name}-{encoder}-{mangling}.png"
                if mangling != "clean":
                    mangle(clean, arguments, target)
                cases.append((f"{name}/{encoder}/{mangling}",
                              encoder, payload, mangling, target))
            blot = workdir / f"{name}-{encoder}-blot.png"
            overlay_blot(clean, blot)
            cases.append((f"{name}/{encoder}/blot",
                          encoder, payload, "blot", blot))

    ours = decode_ours([path for *_, path in cases])

    required_failures = []
    print(f"{'case':38} {'qrity':>18} {'zbar':>6} {'opencv':>6}")
    for case_id, encoder, payload, mangling, path in cases:
        record = ours.get(str(path), {"status": "missing"})
        ok = our_read_matches(record, payload, encoder)
        note = "ok" if ok else record.get("status", "mismatch")
        if not ok and record.get("status") == "ok":
            note = "mismatch"
        if ok and (record.get("errors") or record.get("erasures")):
            note = (f"ok (+{record.get('errors', 0)}e"
                    f"/{record.get('erasures', 0)}x)")
        zbar_ok = decode_zbar(path) == payload
        opencv_ok = decode_opencv(path) == payload
        print(f"{case_id:38} {note:>18} "
              f"{'ok' if zbar_ok else '-':>6} "
              f"{'ok' if opencv_ok else '-':>6}")

        if not ok and mangling not in STRETCH:
            required_failures.append(f"qrity failed {case_id}: {note}")
        if encoder == "qrity" and mangling == "clean":
            if not zbar_ok:
                required_failures.append(f"zbar failed {case_id}")
            if not opencv_ok and "czech" not in case_id:
                # OpenCV's decoder predates common ECI handling.
                required_failures.append(f"opencv failed {case_id}")

    print()
    if required_failures:
        for failure in required_failures:
            print(f"REQUIRED FAILURE: {failure}")
        return 1
    print(f"All required readings passed across {len(cases)} pictures "
          f"(images in {workdir})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
