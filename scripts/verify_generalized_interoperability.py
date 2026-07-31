#!/usr/bin/env python3
"""Verify single-segment QR generation across three runtimes and two decoders."""

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


NUMERIC_FIXTURES = (
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

ALPHANUMERIC_REPERTOIRE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"


def repeated_alphanumeric(length: int) -> str:
    repetitions = (
        length + len(ALPHANUMERIC_REPERTOIRE) - 1
    ) // len(ALPHANUMERIC_REPERTOIRE)
    return (ALPHANUMERIC_REPERTOIRE * repetitions)[:length]


ALPHANUMERIC_FIXTURES = (
    {
        "label": "minimum-m",
        "level": "m",
        "payload": "A",
        "expected_version": 1,
        "expected_mask_reference": 3,
    },
    {
        "label": "table-5-example-q",
        "level": "q",
        "payload": "AC-42",
        "expected_version": 1,
        "expected_mask_reference": 3,
    },
    {
        "label": "all-repertoire-l",
        "level": "l",
        "payload": ALPHANUMERIC_REPERTOIRE,
        "expected_version": 2,
        "expected_mask_reference": 5,
    },
    {
        "label": "maximum-v1-h",
        "level": "h",
        "payload": repeated_alphanumeric(10),
        "expected_version": 1,
        "expected_mask_reference": 7,
    },
    {
        "label": "version-7-q",
        "level": "q",
        "payload": repeated_alphanumeric(109),
        "expected_version": 7,
        "expected_mask_reference": 0,
    },
    {
        "label": "version-10-m",
        "level": "m",
        "payload": repeated_alphanumeric(263),
        "expected_version": 10,
        "expected_mask_reference": 4,
    },
)


def repeated_byte_text(length: int) -> str:
    return "a" * length


BYTE_FIXTURES = (
    {
        "label": "minimum-m",
        "level": "m",
        "payload": "a",
        "expected_version": 1,
        "expected_mask_reference": 5,
    },
    {
        "label": "url-q",
        "level": "q",
        "payload": "https://example.com/",
        "expected_version": 2,
        "expected_mask_reference": 7,
    },
    {
        "label": "query-h",
        "level": "h",
        "payload": "https://x.test/?a=1&b=2",
        "expected_version": 3,
        "expected_mask_reference": 0,
    },
    {
        "label": "maximum-v1-l",
        "level": "l",
        "payload": repeated_byte_text(17),
        "expected_version": 1,
        "expected_mask_reference": 6,
    },
    {
        "label": "version-7-q",
        "level": "q",
        "payload": repeated_byte_text(86),
        "expected_version": 7,
        "expected_mask_reference": 1,
    },
    {
        "label": "version-10-l",
        "level": "l",
        "payload": repeated_byte_text(231),
        "expected_version": 10,
        "expected_mask_reference": 1,
    },
)

RUNTIMES = {
    "jvm": "scripts/generate-generalized-clojure.sh",
    "node": "scripts/generate-generalized-clojurescript.sh",
    "babashka": "scripts/generate-generalized-babashka.sh",
}

METADATA_PREFIX = "qrity-generalized="


def generation_arguments(
    run_directory: Path,
    runtime: str,
    mode: str = "numeric",
    fixtures: tuple[dict[str, Any], ...] = NUMERIC_FIXTURES,
) -> list[str]:
    mode_flags = {"alphanumeric": "--alphanumeric", "byte": "--byte"}
    arguments: list[str] = [mode_flags[mode]] if mode in mode_flags else []
    for fixture in fixtures:
        arguments.extend(
            [
                str(fixture["level"]),
                str(fixture["payload"]),
                str(run_directory / f"{runtime}-{fixture['label']}.pbm"),
            ]
        )
    return arguments


def parse_metadata(
    stdout: str,
    expected_paths: set[Path],
    metadata_prefix: str = METADATA_PREFIX,
) -> dict[Path, dict[str, Any]]:
    result: dict[Path, dict[str, Any]] = {}
    for line in stdout.splitlines():
        if not line.startswith(metadata_prefix):
            continue
        fields = line.removeprefix(metadata_prefix).split("\t")
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


def verify(
    output_root: Path, mode: str = "numeric"
) -> tuple[Path, dict[str, Any]]:
    repository = Path(__file__).resolve().parent.parent
    fixtures_by_mode = {
        "numeric": NUMERIC_FIXTURES,
        "alphanumeric": ALPHANUMERIC_FIXTURES,
        "byte": BYTE_FIXTURES,
    }
    metadata_prefixes = {
        "numeric": METADATA_PREFIX,
        "alphanumeric": "qrity-alphanumeric=",
        "byte": "qrity-byte=",
    }
    fixtures = fixtures_by_mode[mode]
    metadata_prefix = metadata_prefixes[mode]
    output_root.mkdir(parents=True, exist_ok=True)
    run_directory = Path(
        tempfile.mkdtemp(
            prefix=(
                "qrity-generalized-interop-"
                if mode == "numeric"
                else f"qrity-{mode}-interop-"
            ),
            dir=output_root,
        )
    ).resolve()
    commands: list[dict[str, Any]] = []
    report: dict[str, Any] = {
        "status": "running",
        "repository": str(repository),
        "run_directory": str(run_directory),
        "mode": mode,
        "fixtures": list(fixtures),
        "coverage": {
            "error_correction_levels": ["l", "m", "q", "h"],
            "leading_zeros": mode == "numeric",
            "table_5_repertoire": mode == "alphanumeric",
            "default_eci_iso_8859_1": mode == "byte",
            "ascii_url_fixtures": mode == "byte",
            "version_transitions": (
                ["9-to-10"]
                if mode == "byte"
                else ["1-to-2", "6-to-7", "9-to-10"]
            ),
            "versions_exercised": (
                [1, 2, 3, 7, 10] if mode == "byte" else [1, 2, 7, 10]
            ),
            "version_information_onset": 7,
            "count_width_transition": "9-to-10",
            f"{mode}_count_width_transition": "9-to-10",
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
                for fixture in fixtures
            }
            generation = run_command(
                [script]
                + generation_arguments(
                    run_directory, runtime, mode, fixtures
                ),
                repository,
                commands,
            )
            runtime_metadata[runtime] = parse_metadata(
                generation.stdout, expected_paths, metadata_prefix
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

        for fixture in fixtures:
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
            "fixture_count": len(fixtures),
            "runtime_artifact_count": len(fixtures) * len(RUNTIMES),
            "decode_assertion_count": len(fixtures) * len(RUNTIMES) * 2,
            "runtime_triples_byte_identical": len(fixtures),
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
    parser.add_argument(
        "--mode",
        choices=("numeric", "alphanumeric", "byte"),
        default="numeric",
        help="single-segment mode to verify",
    )
    arguments = parser.parse_args()
    try:
        run_directory, report = verify(
            arguments.output_root.resolve(), arguments.mode
        )
    except VerificationFailure:
        raise SystemExit(1) from None
    if arguments.mode == "numeric":
        print(f"generalized interoperability: {report['status']}")
    else:
        print(
            f"generalized {arguments.mode} interoperability: "
            f"{report['status']}"
        )
    print(f"artifacts: {run_directory}")
    print(f"report: {run_directory / 'report.json'}")


if __name__ == "__main__":
    main()
