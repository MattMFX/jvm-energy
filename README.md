# JVM Energy MVP (JMH + RAPL/jRAPL)

MVP to validate measuring power consumption of Java workloads using JMH. Benchmarks compare sorting algorithms. Energy measurement integrates with jRAPL via reflection so the project builds and runs even if jRAPL is not available.

## Prerequisites

- Java 17+ (tested with OpenJDK 21)
- Maven 3.8+
- Linux machine with Intel RAPL support for energy measurement

## Build

```bash
mvn clean package
```

This produces `target/benchmarks.jar` runnable with JMH.

## Run

Time-only sorting benchmarks:

```bash
java -jar target/benchmarks.jar ".*SortingBenchmark.*"
```

Energy-instrumented benchmark (quick sort) that tries to use jRAPL if present:

```bash
java -jar target/benchmarks.jar ".*EnergyMeasuredSortingBenchmark.*"
```

You can add common JMH flags, e.g. `-wi 3 -i 5 -f 1`.

## Energy measurement (jRAPL)

This MVP uses jRAPL by reflection to avoid a hard build dependency. The code looks for `jrapl.EnergyCheckUtils` at runtime. If found, it collects energy before/after and reports the delta via the JMH `Blackhole`. If not found, it runs the benchmark without energy.

To enable energy measurement:

1) Provide jRAPL on the classpath when running. For example, place the jRAPL JARs in a directory and run:

```bash
java -cp target/benchmarks.jar:/path/to/jrapl.jar org.openjdk.jmh.Main ".*EnergyMeasuredSortingBenchmark.*"
```

2) Ensure the CPU supports RAPL and any required permissions/cgroups are in place.

If you prefer, we can switch to a fixed Maven/JitPack coordinate or vendor the library; please confirm which you want.

## Code layout

- `dev.matheus.energy.SortingAlgorithms` – bubble, quick, merge implementations
- `dev.matheus.energy.SortingBenchmark` – time-only benchmarks for the algorithms
- `dev.matheus.energy.EnergyMeasuredSortingBenchmark` – quick sort with optional jRAPL energy

## Notes

- Comments are minimal; code is intended to be straightforward.
- No unit tests for MVP.
- Next steps: wire stable jRAPL dependency, add more workloads, persist results.


