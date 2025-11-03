#!/bin/bash

# Energy Measurement Benchmark Runner
# A convenient script to run JMH energy benchmarks with customizable parameters

set -e

# Default values
REBUILD=false
ALGORITHM="all"
ARRAY_SIZE="10000"
WARMUP_ITERATIONS="50"
WARMUP_TIME="5"
MEASUREMENT_ITERATIONS="5"
MEASUREMENT_TIME="5"
FORKS="1"
CPU_CORE=""
OUTPUT_FILE=""
EXTRA_JVM_OPTS=""
EXTRA_JMH_OPTS=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Help function
show_help() {
    cat << EOF
${GREEN}Energy Measurement Benchmark Runner${NC}

${BLUE}Usage:${NC}
    $0 [OPTIONS]

${BLUE}Options:${NC}
    -h, --help                  Show this help message
    -r, --rebuild               Rebuild the project before running
    -a, --algorithm ALGO        Algorithm to benchmark (default: all)
                                Options: all, quick_sort, bubble_sort, merge_sort
    -s, --size SIZE             Array size (default: 10000)
    -w, --warmup-iter N         Warmup iterations (default: 50)
    -wt, --warmup-time N        Warmup time in seconds (default: 5)
    -m, --measurement-iter N    Measurement iterations (default: 5)
    -mt, --measurement-time N   Measurement time in seconds (default: 5)
    -f, --forks N               Number of forks (default: 1)
    -c, --core N                Pin to specific CPU core (optional)
    -o, --output FILE           Save output to file (optional, auto-generates timestamp if not provided)
    -j, --jvm-opts "OPTS"       Additional JVM options (e.g., "-Xmx4g -Xms4g")
    --jmh-opts "OPTS"           Additional JMH options

${BLUE}Examples:${NC}
    # Run all algorithms with default settings
    $0

    # Run only quicksort on array size 50000
    $0 -a quick_sort -s 50000

    # Run with reduced iterations for quick testing
    $0 -w 10 -m 3

    # Rebuild, run on core 0, and save output
    $0 -r -c 0 -o results/my_benchmark.log

    # Run bubble sort with custom JVM options
    $0 -a bubble_sort -j "-Xmx8g -Xms8g"

    # Run all algorithms with custom parameters
    $0 -s 20000 -w 30 -wt 3 -m 10 -mt 3 -f 2

${BLUE}Notes:${NC}
    - This script requires the project to be built at least once
    - Energy measurement requires jRAPL to be properly configured
    - If output file is specified without path, it will be saved in results/
    - Using --rebuild will run 'mvn clean package' first

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -r|--rebuild)
            REBUILD=true
            shift
            ;;
        -a|--algorithm)
            ALGORITHM="$2"
            shift 2
            ;;
        -s|--size)
            ARRAY_SIZE="$2"
            shift 2
            ;;
        -w|--warmup-iter)
            WARMUP_ITERATIONS="$2"
            shift 2
            ;;
        -wt|--warmup-time)
            WARMUP_TIME="$2"
            shift 2
            ;;
        -m|--measurement-iter)
            MEASUREMENT_ITERATIONS="$2"
            shift 2
            ;;
        -mt|--measurement-time)
            MEASUREMENT_TIME="$2"
            shift 2
            ;;
        -f|--forks)
            FORKS="$2"
            shift 2
            ;;
        -c|--core)
            CPU_CORE="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -j|--jvm-opts)
            EXTRA_JVM_OPTS="$2"
            shift 2
            ;;
        --jmh-opts)
            EXTRA_JMH_OPTS="$2"
            shift 2
            ;;
        *)
            echo -e "${RED}Error: Unknown option $1${NC}"
            echo "Use -h or --help for usage information"
            exit 1
            ;;
    esac
done

# Navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${GREEN}=== Energy Measurement Benchmark Runner ===${NC}\n"

# Rebuild if requested
if [ "$REBUILD" = true ]; then
    echo -e "${YELLOW}Rebuilding project...${NC}"
    mvn clean package
    echo -e "${GREEN}Build complete!${NC}\n"
fi

# Check if benchmark jar exists
if [ ! -f "target/benchmarks.jar" ]; then
    echo -e "${RED}Error: target/benchmarks.jar not found${NC}"
    echo -e "${YELLOW}Run with --rebuild flag or execute: mvn clean package${NC}"
    exit 1
fi

# Validate algorithm selection
case $ALGORITHM in
    all|quick_sort|bubble_sort|merge_sort)
        ;;
    *)
        echo -e "${RED}Error: Invalid algorithm '$ALGORITHM'${NC}"
        echo -e "${YELLOW}Valid options: all, quick_sort, bubble_sort, merge_sort${NC}"
        exit 1
        ;;
esac

# Validate CPU core if specified
if [ -n "$CPU_CORE" ]; then
    NUM_CORES=$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo "unknown")
    if [ "$NUM_CORES" != "unknown" ]; then
        if ! [[ "$CPU_CORE" =~ ^[0-9]+$ ]] || [ "$CPU_CORE" -ge "$NUM_CORES" ]; then
            echo -e "${RED}Error: Invalid CPU core $CPU_CORE${NC}"
            echo -e "${YELLOW}Available cores: 0-$((NUM_CORES-1))${NC}"
            exit 1
        fi
    fi
fi

# Setup output file if requested
if [ -n "$OUTPUT_FILE" ]; then
    # If output file doesn't have a path, save to results/
    if [[ "$OUTPUT_FILE" != *"/"* ]]; then
        mkdir -p results
        OUTPUT_FILE="results/$OUTPUT_FILE"
    fi
    # Create directory if it doesn't exist
    mkdir -p "$(dirname "$OUTPUT_FILE")"
    echo -e "${BLUE}Output will be saved to: $OUTPUT_FILE${NC}\n"
else
    # Auto-generate output file with timestamp
    TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
    mkdir -p results
    OUTPUT_FILE="results/benchmark_${ALGORITHM}_${TIMESTAMP}.log"
    echo -e "${BLUE}Output will be saved to: $OUTPUT_FILE${NC}\n"
fi

# Build benchmark pattern
if [ "$ALGORITHM" = "all" ]; then
    BENCHMARK_PATTERN=".*energy.*"
else
    BENCHMARK_PATTERN="${ALGORITHM}_energy"
fi

# Build JMH command
JMH_OPTS="-p arraySize=$ARRAY_SIZE"
JMH_OPTS="$JMH_OPTS -wi $WARMUP_ITERATIONS -w ${WARMUP_TIME}s"
JMH_OPTS="$JMH_OPTS -i $MEASUREMENT_ITERATIONS -r ${MEASUREMENT_TIME}s"
JMH_OPTS="$JMH_OPTS -f $FORKS"
JMH_OPTS="$JMH_OPTS $EXTRA_JMH_OPTS"

# Build JVM options
JVM_OPTS="-Djava.library.path=. -Djmh.ignoreLock=true"
if [ -n "$EXTRA_JVM_OPTS" ]; then
    JVM_OPTS="$JVM_OPTS $EXTRA_JVM_OPTS"
fi

# Build execution command
if [ -n "$CPU_CORE" ]; then
    EXEC_CMD="taskset -c $CPU_CORE java $JVM_OPTS -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark.$BENCHMARK_PATTERN $JMH_OPTS"
else
    EXEC_CMD="java $JVM_OPTS -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark.$BENCHMARK_PATTERN $JMH_OPTS"
fi

# Print configuration
echo -e "${BLUE}Benchmark Configuration:${NC}"
echo -e "  Algorithm:           $ALGORITHM"
echo -e "  Array Size:          $ARRAY_SIZE"
echo -e "  Warmup:              $WARMUP_ITERATIONS iterations × ${WARMUP_TIME}s"
echo -e "  Measurement:         $MEASUREMENT_ITERATIONS iterations × ${MEASUREMENT_TIME}s"
echo -e "  Forks:               $FORKS"
[ -n "$CPU_CORE" ] && echo -e "  CPU Core:            $CPU_CORE"
[ -n "$EXTRA_JVM_OPTS" ] && echo -e "  Extra JVM Options:   $EXTRA_JVM_OPTS"
[ -n "$EXTRA_JMH_OPTS" ] && echo -e "  Extra JMH Options:   $EXTRA_JMH_OPTS"
echo ""

# Show command that will be executed
echo -e "${YELLOW}Executing:${NC}"
echo -e "  $EXEC_CMD"
echo ""

# Run benchmark and save output
echo -e "${GREEN}Starting benchmark...${NC}\n"
echo "# Benchmark started at $(date)" > "$OUTPUT_FILE"
echo "# Configuration: algorithm=$ALGORITHM, size=$ARRAY_SIZE, warmup=$WARMUP_ITERATIONS×${WARMUP_TIME}s, measurement=$MEASUREMENT_ITERATIONS×${MEASUREMENT_TIME}s, forks=$FORKS" >> "$OUTPUT_FILE"
[ -n "$CPU_CORE" ] && echo "# CPU Core: $CPU_CORE" >> "$OUTPUT_FILE"
echo "# Command: $EXEC_CMD" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Run and tee output to both console and file
if eval "$EXEC_CMD" 2>&1 | tee -a "$OUTPUT_FILE"; then
    echo ""
    echo -e "${GREEN}✓ Benchmark completed successfully!${NC}"
    echo -e "${BLUE}Results saved to: $OUTPUT_FILE${NC}"
else
    echo ""
    echo -e "${RED}✗ Benchmark failed!${NC}"
    echo -e "${YELLOW}Check $OUTPUT_FILE for details${NC}"
    exit 1
fi

