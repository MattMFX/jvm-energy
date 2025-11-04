# Benchmarks

Each file implements `BenchmarkAlgorithm` with:
- `getName()` - benchmark identifier
- `getEffectiveSize(int)` - returns actual size to use (most return fixed values)
- `execute(int)` - the measured code
- `setup(int)` - optional pre-execution setup (not measured)

Add new benchmarks by implementing the interface and registering in `BenchmarkConfig.java`.
