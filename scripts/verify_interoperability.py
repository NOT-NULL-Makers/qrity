#!/usr/bin/env python3
"""Generate QRity PBMs through both runtimes and decode every artifact twice."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import tempfile
from typing import Any


PAYLOADS = (
    ("minimum", "0"),
    ("leading-zero", "00000001"),
    ("annex-i-2", "01234567"),
    ("ordinary", "8675309"),
    ("maximum", "1234567890123456789012345678901234"),
)


class VerificationFailure(RuntimeError):
    """An evidence step failed or returned a value outside its exact contract."""


def run_command(
    command: list[str], repository: Path, commands: list[dict[str, Any]]
) -> subprocess.CompletedProcess[str]:
    completed = subprocess.run(
        command,
        cwd=repository,
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    commands.append(
        {
            "argv": command,
            "exit": completed.returncode,
            "stdout": completed.stdout,
            "stderr": completed.stderr,
        }
    )
    if completed.returncode != 0:
        raise VerificationFailure(
            f"command failed with exit {completed.returncode}: {command!r}"
        )
    return completed


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def decode_zbar(
    artifact: Path,
    expected: str,
    zbarimg: Path,
    repository: Path,
    commands: list[dict[str, Any]],
) -> dict[str, Any]:
    completed = run_command(
        [
            str(zbarimg),
            "--nodbus",
            "--quiet",
            "--raw",
            "--oneshot",
            "-Sdisable",
            "-Sqrcode.enable",
            str(artifact),
        ],
        repository,
        commands,
    )
    decoded = completed.stdout.splitlines()
    if decoded != [expected]:
        raise VerificationFailure(
            f"ZBar mismatch for {artifact}: expected {expected!r}, got {decoded!r}"
        )
    return {"decoded": decoded[0], "exact_match": True}


def decode_opencv(
    artifact: Path, expected: str, opencv: Any
) -> dict[str, Any]:
    image = opencv.imread(str(artifact), opencv.IMREAD_GRAYSCALE)
    if image is None:
        raise VerificationFailure(f"OpenCV could not read {artifact}")
    decoded, points, straight = opencv.QRCodeDetector().detectAndDecode(image)
    if points is None:
        raise VerificationFailure(f"OpenCV did not detect a QR quadrangle in {artifact}")
    if decoded == "":
        raise VerificationFailure(f"OpenCV detected but did not decode {artifact}")
    if decoded != expected:
        raise VerificationFailure(
            f"OpenCV mismatch for {artifact}: expected {expected!r}, got {decoded!r}"
        )
    return {
        "decoded": decoded,
        "exact_match": True,
        "points": points.tolist(),
        "straight_shape": None if straight is None else list(straight.shape),
    }


def generation_arguments(run_directory: Path, runtime: str) -> list[str]:
    arguments: list[str] = []
    for label, payload in PAYLOADS:
        arguments.extend(
            [payload, str(run_directory / f"{runtime}-{label}.pbm")]
        )
    return arguments


def verify(output_root: Path) -> tuple[Path, dict[str, Any]]:
    repository = Path(__file__).resolve().parent.parent
    output_root.mkdir(parents=True, exist_ok=True)
    run_directory = Path(
        tempfile.mkdtemp(prefix="qrity-interop-", dir=output_root)
    ).resolve()
    commands: list[dict[str, Any]] = []
    report: dict[str, Any] = {
        "status": "running",
        "repository": str(repository),
        "run_directory": str(run_directory),
        "payloads": [{"label": label, "payload": value} for label, value in PAYLOADS],
        "renderer": {
            "format": "Plain PBM P1",
            "pixel_scale": 8,
            "quiet_zone_modules": 4,
            "maximum_line_length": 70,
        },
        "versions": {
            "python": platform.python_version(),
        },
        "commands": commands,
        "artifacts": [],
    }
    report_path = run_directory / "report.json"

    try:
        try:
            import cv2 as opencv
        except (ImportError, ModuleNotFoundError) as error:
            raise VerificationFailure(
                "required Python module is missing: cv2 (python3-opencv)"
            ) from error

        report["versions"]["opencv"] = opencv.__version__
        zbarimg_name = shutil.which("zbarimg")
        if zbarimg_name is None:
            raise VerificationFailure("required decoder is missing from PATH: zbarimg")
        zbarimg = Path(zbarimg_name).resolve()
        report["executables"] = {
            "python": sys.executable,
            "zbarimg": str(zbarimg),
        }

        report["versions"]["zbar"] = run_command(
            [str(zbarimg), "--version"], repository, commands
        ).stdout.strip()
        report["versions"]["node"] = run_command(
            ["node", "--version"], repository, commands
        ).stdout.strip()
        java_version = run_command(
            ["java", "-version"], repository, commands
        )
        report["versions"]["java"] = (
            java_version.stderr.strip() or java_version.stdout.strip()
        )
        report["versions"]["clojure_cli"] = run_command(
            ["clojure", "-Sdescribe"], repository, commands
        ).stdout.strip()
        report["versions"]["clojure"] = run_command(
            ["clojure", "-M", "-e", "(print (clojure-version))"],
            repository,
            commands,
        ).stdout.strip()

        run_command(
            ["scripts/generate-clojure.sh"]
            + generation_arguments(run_directory, "jvm"),
            repository,
            commands,
        )
        node_generation = run_command(
            ["scripts/generate-clojurescript.sh"]
            + generation_arguments(run_directory, "node"),
            repository,
            commands,
        )
        cljs_version_prefix = "qrity-clojurescript-version="
        cljs_version_lines = [
            line.removeprefix(cljs_version_prefix)
            for line in node_generation.stdout.splitlines()
            if line.startswith(cljs_version_prefix)
        ]
        if len(cljs_version_lines) != 1 or not cljs_version_lines[0]:
            raise VerificationFailure(
                "ClojureScript compiler did not report exactly one version"
            )
        report["versions"]["clojurescript"] = cljs_version_lines[0]

        for label, payload in PAYLOADS:
            jvm_path = run_directory / f"jvm-{label}.pbm"
            node_path = run_directory / f"node-{label}.pbm"
            if not jvm_path.is_file() or not node_path.is_file():
                raise VerificationFailure(
                    f"runtime artifact missing for payload label {label}"
                )
            jvm_hash = sha256(jvm_path)
            node_hash = sha256(node_path)
            if jvm_path.read_bytes() != node_path.read_bytes():
                raise VerificationFailure(
                    f"JVM and Node artifacts differ for payload label {label}"
                )

            for runtime, artifact, artifact_hash in (
                ("jvm", jvm_path, jvm_hash),
                ("node", node_path, node_hash),
            ):
                report["artifacts"].append(
                    {
                        "label": label,
                        "payload": payload,
                        "runtime": runtime,
                        "path": str(artifact),
                        "bytes": artifact.stat().st_size,
                        "sha256": artifact_hash,
                        "runtime_pair_sha256_equal": jvm_hash == node_hash,
                        "zbar": decode_zbar(
                            artifact, payload, zbarimg, repository, commands
                        ),
                        "opencv": decode_opencv(artifact, payload, opencv),
                    }
                )

        report["summary"] = {
            "payload_count": len(PAYLOADS),
            "runtime_artifact_count": len(PAYLOADS) * 2,
            "decode_assertion_count": len(PAYLOADS) * 2 * 2,
            "runtime_pairs_byte_identical": len(PAYLOADS),
        }
        report["status"] = "passed"
        return run_directory, report
    except Exception as error:
        report["status"] = "failed"
        report["error"] = f"{type(error).__name__}: {error}"
        print(f"interoperability failed; report: {report_path}", file=sys.stderr)
        raise
    finally:
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output-root",
        type=Path,
        default=Path("/tmp"),
        help="parent directory for a new non-destructive evidence directory",
    )
    arguments = parser.parse_args()
    try:
        run_directory, report = verify(arguments.output_root.resolve())
    except VerificationFailure:
        raise SystemExit(1) from None
    print(f"interoperability: {report['status']}")
    print(f"artifacts: {run_directory}")
    print(f"report: {run_directory / 'report.json'}")


if __name__ == "__main__":
    main()
