#!/usr/bin/env python3
"""Verify generalized Numeric QR generation across three runtimes and two decoders."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import platform
import shutil
import sys
import tempfile
from typing import Any

from verify_interoperability import (
    VerificationFailure,
    decode_opencv,
    decode_zbar,
    run_command,
    sha256,
)


def repeated_digits(length: int) -> str:
    pattern = "0123456789"
    return (pattern * ((length + len(pattern) - 1) // len(pattern)))[:length]


FIXTURES = (
    {
        "label": "minimum-m",
        "level": "m",
        "payload": "0",
        "expected_version": 1,
        "expected_mask_reference": 1,
    },
    {
        "label": "leading-zero-h",
        "level": "h",
        "payload": "00000001",
        "expected_version": 1,
        "expected_mask_reference": 3,
    },
    {
        "label": "version-2-l",
        "level": "l",
        "payload": repeated_digits(42),
        "expected_version": 2,
        "expected_mask_reference": 5,
    },
    {
        "label": "version-7-q",
        "level": "q",
        "payload": repeated_digits(179),
        "expected_version": 7,
        "expected_mask_reference": 6,
    },
    {
        "label": "version-10-m",
        "level": "m",
        "payload": repeated_digits(433),
        "expected_version": 10,
        "expected_mask_reference": 4,
    },
)

RUNTIMES = {
    "jvm": "scripts/generate-generalized-clojure.sh",
    "node": "scripts/generate-generalized-clojurescript.sh",
    "babashka": "scripts/generate-generalized-babashka.sh",
}

METADATA_PREFIX = "qrity-generalized="


def generation_arguments(run_directory: Path, runtime: str) -> list[str]:
    arguments: list[str] = []
    for fixture in FIXTURES:
        arguments.extend(
            [
                str(fixture["level"]),
                str(fixture["payload"]),
                str(run_directory / f"{runtime}-{fixture['label']}.pbm"),
            ]
        )
    return arguments


def parse_metadata(
    stdout: str, expected_paths: set[Path]
) -> dict[Path, dict[str, Any]]:
    result: dict[Path, dict[str, Any]] = {}
    for line in stdout.splitlines():
        if not line.startswith(METADATA_PREFIX):
            continue
        fields = line.removeprefix(METADATA_PREFIX).split("\t")
        if len(fields) != 5:
            raise VerificationFailure(f"malformed emitter metadata line: {line!r}")
        path_text, version, level, mask_reference, dimension = fields
        path = Path(path_text).resolve()
        if path in result:
            raise VerificationFailure(f"duplicate emitter metadata for {path}")
        result[path] = {
            "version": int(version),
            "error_correction_level": level,
            "mask_reference": int(mask_reference),
            "dimension": int(dimension),
        }
    if set(result) != expected_paths:
        raise VerificationFailure(
            "emitter metadata paths differ from requested artifacts: "
            f"expected {sorted(map(str, expected_paths))!r}, "
            f"got {sorted(map(str, result))!r}"
        )
    return result


def validate_fixture_metadata(
    fixture: dict[str, Any],
    metadata_by_runtime: dict[str, dict[str, Any]],
) -> None:
    metadata_values = list(metadata_by_runtime.values())
    if not metadata_values or any(
        metadata != metadata_values[0] for metadata in metadata_values[1:]
    ):
        raise VerificationFailure(
            "runtime metadata differs for fixture "
            f"{fixture['label']}: {metadata_by_runtime!r}"
        )
    metadata = metadata_values[0]
    expected = {
        "version": fixture["expected_version"],
        "error_correction_level": fixture["level"],
        "mask_reference": fixture["expected_mask_reference"],
        "dimension": 17 + 4 * int(fixture["expected_version"]),
    }
    if metadata != expected:
        raise VerificationFailure(
            f"unexpected metadata for fixture {fixture['label']}: "
            f"expected {expected!r}, got {metadata!r}"
        )


def verify(output_root: Path) -> tuple[Path, dict[str, Any]]:
    repository = Path(__file__).resolve().parent.parent
    output_root.mkdir(parents=True, exist_ok=True)
    run_directory = Path(
        tempfile.mkdtemp(prefix="qrity-generalized-interop-", dir=output_root)
    ).resolve()
    commands: list[dict[str, Any]] = []
    report: dict[str, Any] = {
        "status": "running",
        "repository": str(repository),
        "run_directory": str(run_directory),
        "fixtures": list(FIXTURES),
        "coverage": {
            "error_correction_levels": ["l", "m", "q", "h"],
            "leading_zeros": True,
            "version_transitions": [
                "1-to-2",
                "6-to-7",
                "9-to-10",
            ],
            "version_information_onset": 7,
            "numeric_count_width_transition": "9-to-10",
        },
        "renderer": {
            "format": "Plain PBM P1",
            "pixel_scale": 8,
            "quiet_zone_modules": 4,
            "maximum_line_length": 70,
        },
        "versions": {"python": platform.python_version()},
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
        report["versions"]["babashka"] = run_command(
            ["bb", "--version"], repository, commands
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

        runtime_metadata: dict[str, dict[Path, dict[str, Any]]] = {}
        for runtime, script in RUNTIMES.items():
            expected_paths = {
                (run_directory / f"{runtime}-{fixture['label']}.pbm").resolve()
                for fixture in FIXTURES
            }
            generation = run_command(
                [script] + generation_arguments(run_directory, runtime),
                repository,
                commands,
            )
            runtime_metadata[runtime] = parse_metadata(
                generation.stdout, expected_paths
            )
            if runtime == "node":
                prefix = "qrity-clojurescript-version="
                versions = [
                    line.removeprefix(prefix)
                    for line in generation.stdout.splitlines()
                    if line.startswith(prefix)
                ]
                if len(versions) != 1 or not versions[0]:
                    raise VerificationFailure(
                        "ClojureScript compiler did not report exactly one version"
                    )
                report["versions"]["clojurescript"] = versions[0]

        for fixture in FIXTURES:
            label = str(fixture["label"])
            payload = str(fixture["payload"])
            paths = {
                runtime: run_directory / f"{runtime}-{label}.pbm"
                for runtime in RUNTIMES
            }
            if not all(path.is_file() for path in paths.values()):
                raise VerificationFailure(
                    f"runtime artifact missing for fixture {label}"
                )
            contents = {
                runtime: path.read_bytes() for runtime, path in paths.items()
            }
            if len(set(contents.values())) != 1:
                raise VerificationFailure(
                    f"runtime artifacts differ for fixture {label}"
                )
            hashes = {
                runtime: sha256(path) for runtime, path in paths.items()
            }
            fixture_metadata = {
                runtime: runtime_metadata[runtime][artifact.resolve()]
                for runtime, artifact in paths.items()
            }
            validate_fixture_metadata(fixture, fixture_metadata)

            for runtime, artifact in paths.items():
                metadata = fixture_metadata[runtime]
                report["artifacts"].append(
                    {
                        "label": label,
                        "payload": payload,
                        "runtime": runtime,
                        "path": str(artifact),
                        "bytes": artifact.stat().st_size,
                        "sha256": hashes[runtime],
                        "all_runtime_hashes_equal": len(set(hashes.values())) == 1,
                        "metadata": metadata,
                        "zbar": decode_zbar(
                            artifact, payload, zbarimg, repository, commands
                        ),
                        "opencv": decode_opencv(artifact, payload, opencv),
                    }
                )

        report["summary"] = {
            "fixture_count": len(FIXTURES),
            "runtime_artifact_count": len(FIXTURES) * len(RUNTIMES),
            "decode_assertion_count": len(FIXTURES) * len(RUNTIMES) * 2,
            "runtime_triples_byte_identical": len(FIXTURES),
        }
        report["status"] = "passed"
        return run_directory, report
    except Exception as error:
        report["status"] = "failed"
        report["error"] = f"{type(error).__name__}: {error}"
        print(
            f"generalized interoperability failed; report: {report_path}",
            file=sys.stderr,
        )
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
    print(f"generalized interoperability: {report['status']}")
    print(f"artifacts: {run_directory}")
    print(f"report: {run_directory / 'report.json'}")


if __name__ == "__main__":
    main()
