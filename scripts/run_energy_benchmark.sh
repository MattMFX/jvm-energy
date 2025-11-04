#!/bin/bash

# Energy Measurement Benchmark Runner
# A convenient script to run JMH energy benchmarks with customizable parameters

set -e

# Default values
ALGORITHM="unified"
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
FREQ_START="500"
FREQ_END="3500"
FREQ_STEP=""
RESTORE_GOVERNOR=true
BENCHMARK_FILTER=""

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
    -a, --algorithm ALGO        Algorithm to benchmark (default: unified)
                                
                                Options:
                                - unified: runs all benchmarks (DEFAULT)
                                - all: runs 3 sorting algorithms only (legacy)
                                - Individual: quick_sort, bubble_sort, merge_sort,
                                  nbody, spectralnorm, binarytrees, mandelbrot,
                                  fannkuchredux, fasta, knucleotide
    --filter "ALGO1,ALGO2"      Filter benchmarks by prefix (comma-separated)
                                Examples: "nbody" runs all nbody variants
                                          "nbody,mandelbrot" runs all nbody and mandelbrot
                                This works only with -a unified
    -s, --size SIZE             Input size parameter (default: 10000)
                                For sorting: array size
                                For other algorithms: algorithm-specific parameter
    -w, --warmup-iter N         Warmup iterations (default: 50)
    -wt, --warmup-time N        Warmup time in seconds (default: 5)
    -m, --measurement-iter N    Measurement iterations (default: 5)
    -mt, --measurement-time N   Measurement time in seconds (default: 5)
    -f, --forks N               Number of forks (default: 1)
    -c, --core N                Pin to specific CPU core (optional)
    -o, --output FILE           Save output to file (optional, auto-generates timestamp if not provided)
    -j, --jvm-opts "OPTS"       Additional JVM options (e.g., "-Xmx4g -Xms4g")
    --jmh-opts "OPTS"           Additional JMH options
    --freq-start MHZ            Starting CPU frequency in MHz (default: 500)
    --freq-end MHZ              Ending CPU frequency in MHz (default: 3500)
    --freq-step MHZ             Frequency step size in MHz (optional, runs single frequency if not set)
    --no-restore                Don't restore original CPU governor after benchmark

${BLUE}Examples:${NC}
    # Run all enabled benchmarks (default) - each gets full 5 seconds
    $0

    # Or explicitly
    $0 -a unified

    # Legacy: Run only 3 sorting algorithms
    $0 -a all

    # Run only quicksort on array size 50000
    $0 -a quick_sort -s 50000

    # Run nbody benchmark with 50000000 iterations
    $0 -a nbody -s 50000000

    # Run benchmarks across frequencies from 1000 to 3000 MHz in 500 MHz steps
    # Each algorithm runs separately at each frequency
    $0 --freq-start 1000 --freq-end 3000 --freq-step 500

    # Run bubble sort with frequency stepping every 250 MHz
    $0 -a bubble_sort --freq-step 250

    # Run with reduced iterations for quick testing with frequency steps
    $0 -w 10 -m 3 --freq-start 1500 --freq-end 2500 --freq-step 500

    # Run on core 0, with frequency stepping
    $0 -c 0 --freq-step 1000 -o results/my_benchmark.log

    # Run all algorithms with custom parameters and frequency stepping
    $0 -s 20000 -w 30 -wt 3 -m 10 -mt 3 -f 2 --freq-step 500
    
    # Run only nbody variants (all 3: nbody_v1, nbody_v5, nbody_v8)
    # Each variant runs separately and gets full execution time
    $0 --filter "nbody"
    
    # Run nbody and mandelbrot variants
    $0 --filter "nbody,mandelbrot"

${BLUE}Notes:${NC}
    - The project is always rebuilt before running ('mvn clean package')
    - Each algorithm runs SEPARATELY and gets its FULL execution time allocation
      (e.g., with 5s measurement time, each algorithm gets 5s, not shared)
    - Energy measurement requires jRAPL to be properly configured
    - Energy measurement requires root/sudo access (script uses sudo automatically)
    - If output file is specified without path, it will be saved in results/
    - Hyperthreading is automatically disabled during benchmarks (not restored)
    - Frequency management requires root/sudo access and sets CPU governor to 'userspace'
    - If --freq-step is not specified, benchmark runs at current CPU frequency
    - Original CPU governor is restored after benchmark (unless --no-restore is used)

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
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
        --freq-start)
            FREQ_START="$2"
            shift 2
            ;;
        --freq-end)
            FREQ_END="$2"
            shift 2
            ;;
        --freq-step)
            FREQ_STEP="$2"
            shift 2
            ;;
        --no-restore)
            RESTORE_GOVERNOR=false
            shift
            ;;
        --filter)
            BENCHMARK_FILTER="$2"
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

# Generate a single timestamp for this benchmark session
# This ensures all frequency runs write to the same CSV file
ENERGY_TIMESTAMP=$(date +"%Y-%m-%d_%H-%M")

# CPU Frequency and Hyperthreading Management Functions
ORIGINAL_GOVERNORS=()
ORIGINAL_MIN_FREQS=()
ORIGINAL_MAX_FREQS=()

disable_hyperthreading() {
    echo -e "${BLUE}Disabling hyperthreading...${NC}"
    
    if [ -f /sys/devices/system/cpu/smt/control ]; then
        echo "off" | sudo tee /sys/devices/system/cpu/smt/control > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}Hyperthreading disabled${NC}"
        else
            echo -e "${YELLOW}Failed to disable hyperthreading${NC}"
        fi
    else
        echo -e "${YELLOW}SMT control not available on this system${NC}"
    fi
}

save_cpu_state() {
    echo -e "${BLUE}Saving current CPU state...${NC}"
    for cpu_dir in /sys/devices/system/cpu/cpu*/cpufreq/; do
        if [ -d "$cpu_dir" ]; then
            ORIGINAL_GOVERNORS+=("$(cat "${cpu_dir}scaling_governor" 2>/dev/null || true)")
            ORIGINAL_MIN_FREQS+=("$(cat "${cpu_dir}scaling_min_freq" 2>/dev/null || true)")
            ORIGINAL_MAX_FREQS+=("$(cat "${cpu_dir}scaling_max_freq" 2>/dev/null || true)")
        fi
    done
}

restore_cpu_state() {
    if [ "$RESTORE_GOVERNOR" = false ]; then
        echo -e "${YELLOW}Skipping CPU state restoration (--no-restore flag)${NC}"
        return
    fi
    
    echo -e "${BLUE}Restoring original CPU state...${NC}"
    local idx=0
    for cpu_dir in /sys/devices/system/cpu/cpu*/cpufreq/; do
        if [ -d "$cpu_dir" ] && [ $idx -lt ${#ORIGINAL_GOVERNORS[@]} ]; then
            echo "${ORIGINAL_MIN_FREQS[$idx]}" | sudo tee "${cpu_dir}scaling_min_freq" > /dev/null 2>&1 || true
            echo "${ORIGINAL_MAX_FREQS[$idx]}" | sudo tee "${cpu_dir}scaling_max_freq" > /dev/null 2>&1 || true
            echo "${ORIGINAL_GOVERNORS[$idx]}" | sudo tee "${cpu_dir}scaling_governor" > /dev/null 2>&1 || true
            idx=$((idx + 1))
        fi
    done
    echo -e "${GREEN}CPU state restored${NC}"
}

set_cpu_governor() {
    local governor="$1"
    echo -e "${BLUE}Setting CPU governor to '$governor'...${NC}"
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
        echo "$governor" | sudo tee "$cpu" > /dev/null 2>&1 || true
    done
}

set_cpu_frequency() {
    local freq_mhz="$1"
    local freq_khz=$((freq_mhz * 1000))
    
    echo -e "${BLUE}Setting CPU frequency to ${freq_mhz} MHz...${NC}"
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq; do
        echo "$freq_khz" | sudo tee "$cpu" > /dev/null 2>&1 || true
    done
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_max_freq; do
        echo "$freq_khz" | sudo tee "$cpu" > /dev/null 2>&1 || true
    done
    
    # Verify frequency was set
    sleep 0.5
    local actual_freq=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq 2>/dev/null || echo "0")
    local actual_mhz=$((actual_freq / 1000))
    echo -e "${GREEN}CPU frequency set to ${actual_mhz} MHz${NC}"
}

get_current_cpu_frequency() {
    local freq=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq 2>/dev/null || echo "0")
    echo $((freq / 1000))
}

# Setup cleanup trap
cleanup() {
    echo ""
    echo -e "${YELLOW}Cleaning up...${NC}"
    restore_cpu_state
}
trap cleanup EXIT

echo -e "${GREEN}=== Energy Measurement Benchmark Runner ===${NC}\n"

# Always rebuild the project
echo -e "${YELLOW}Rebuilding project...${NC}"
mvn clean package
echo -e "${GREEN}Build complete!${NC}\n"

# Check if benchmark jar exists
if [ ! -f "target/benchmarks.jar" ]; then
    echo -e "${RED}Error: target/benchmarks.jar not found after build${NC}"
    echo -e "${YELLOW}Build may have failed. Check output above.${NC}"
    exit 1
fi

# Validate algorithm selection
case $ALGORITHM in
    all|quick_sort|bubble_sort|merge_sort|unified|nbody|spectralnorm|binarytrees|mandelbrot|fannkuchredux|fasta|knucleotide)
        ;;
    *)
        echo -e "${RED}Error: Invalid algorithm '$ALGORITHM'${NC}"
        echo -e "${YELLOW}Valid options: all, quick_sort, bubble_sort, merge_sort, unified,${NC}"
        echo -e "${YELLOW}                nbody, spectralnorm, binarytrees, mandelbrot,${NC}"
        echo -e "${YELLOW}                fannkuchredux, fasta, knucleotide${NC}"
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
    # Auto-generate output file with timestamp (use same timestamp as energy CSV for consistency)
    mkdir -p results
    OUTPUT_FILE="results/benchmark_${ALGORITHM}_${ENERGY_TIMESTAMP}.log"
    echo -e "${BLUE}Output will be saved to: $OUTPUT_FILE${NC}\n"
fi

# Function to get list of algorithms to benchmark
get_algorithms_to_run() {
    if [ "$ALGORITHM" = "unified" ]; then
        # Get all enabled algorithms from BenchmarkConfig
        if [ -n "$BENCHMARK_FILTER" ]; then
            # Use filter if provided
            java -cp target/classes dev.matheus.energy.BenchmarkLister "$BENCHMARK_FILTER" 2>/dev/null
        else
            # Get all enabled benchmarks
            java -cp target/classes dev.matheus.energy.BenchmarkLister 2>/dev/null
        fi
    elif [ "$ALGORITHM" = "all" ]; then
        # Legacy: list sorting algorithms
        echo "quick_sort"
        echo "bubble_sort"
        echo "merge_sort"
    else
        # Single algorithm (could be sorting or unified)
        case $ALGORITHM in
            quick_sort|bubble_sort|merge_sort)
                echo "$ALGORITHM"
                ;;
            *)
                # For new algorithms, treat as filter
                java -cp target/classes dev.matheus.energy.BenchmarkLister "$ALGORITHM" 2>/dev/null
                ;;
        esac
    fi
}

# Build benchmark class and pattern based on algorithm type
determine_benchmark_class() {
    local algo_name="$1"
    
    # Check if this is a legacy sorting algorithm
    case $algo_name in
        quick_sort|bubble_sort|merge_sort)
            echo "EnergyMeasuredSortingBenchmark"
            echo "${algo_name}_energy"
            ;;
        *)
            # New unified benchmark
            echo "UnifiedEnergyBenchmark"
            echo ".*"
            ;;
    esac
}

# Get list of algorithms to run
echo -e "${BLUE}Discovering algorithms to benchmark...${NC}"
ALGORITHMS_TO_RUN=($(get_algorithms_to_run))

if [ ${#ALGORITHMS_TO_RUN[@]} -eq 0 ]; then
    echo -e "${RED}Error: No algorithms found to benchmark!${NC}"
    echo -e "${YELLOW}Check your algorithm selection or BenchmarkConfig.java${NC}"
    exit 1
fi

echo -e "${GREEN}Found ${#ALGORITHMS_TO_RUN[@]} algorithm(s) to benchmark:${NC}"
for algo in "${ALGORITHMS_TO_RUN[@]}"; do
    echo -e "  - $algo"
done
echo ""

# Base JVM options (without frequency and algorithm filter)
BASE_JVM_OPTS="-Djava.library.path=. -Djmh.ignoreLock=true -Dbenchmark.output.timestamp=${ENERGY_TIMESTAMP}"
if [ -n "$EXTRA_JVM_OPTS" ]; then
    BASE_JVM_OPTS="$BASE_JVM_OPTS $EXTRA_JVM_OPTS"
fi

# Determine if we're doing frequency stepping
if [ -n "$FREQ_STEP" ]; then
    USE_FREQ_STEPPING=true
    # Calculate frequency array
    FREQUENCIES=()
    for ((freq=$FREQ_START; freq<=$FREQ_END; freq+=$FREQ_STEP)); do
        FREQUENCIES+=($freq)
    done
    TOTAL_FREQ_RUNS=${#FREQUENCIES[@]}
else
    USE_FREQ_STEPPING=false
    TOTAL_FREQ_RUNS=1
fi

# Calculate total number of benchmark runs
TOTAL_ALGO_RUNS=${#ALGORITHMS_TO_RUN[@]}
if [ "$USE_FREQ_STEPPING" = true ]; then
    TOTAL_RUNS=$((TOTAL_FREQ_RUNS * TOTAL_ALGO_RUNS))
else
    TOTAL_RUNS=$TOTAL_ALGO_RUNS
fi

# Print configuration
echo -e "${BLUE}Benchmark Configuration:${NC}"
echo -e "  Algorithm Mode:      $ALGORITHM"
[ -n "$BENCHMARK_FILTER" ] && echo -e "  ${GREEN}Benchmark Filter:    $BENCHMARK_FILTER${NC}"
echo -e "  ${GREEN}Algorithms to Run:   ${#ALGORITHMS_TO_RUN[@]}${NC}"
echo -e "  Input Size:          $ARRAY_SIZE"
echo -e "  Warmup:              $WARMUP_ITERATIONS iterations × ${WARMUP_TIME}s"
echo -e "  Measurement:         $MEASUREMENT_ITERATIONS iterations × ${MEASUREMENT_TIME}s"
echo -e "  Forks:               $FORKS"
[ -n "$CPU_CORE" ] && echo -e "  CPU Core:            $CPU_CORE"
[ -n "$EXTRA_JVM_OPTS" ] && echo -e "  Extra JVM Options:   $EXTRA_JVM_OPTS"
[ -n "$EXTRA_JMH_OPTS" ] && echo -e "  Extra JMH Options:   $EXTRA_JMH_OPTS"
echo -e "  Energy CSV:          results/energy_${ENERGY_TIMESTAMP}.csv"
if [ "$USE_FREQ_STEPPING" = true ]; then
    echo -e "  ${GREEN}Frequency Range:     ${FREQ_START} - ${FREQ_END} MHz (step: ${FREQ_STEP} MHz)${NC}"
    echo -e "  ${GREEN}Frequency Steps:     ${TOTAL_FREQ_RUNS}${NC}"
    echo -e "  ${GREEN}Frequencies:         ${FREQUENCIES[*]} MHz${NC}"
    echo -e "  ${GREEN}Total Runs:          ${TOTAL_RUNS} (${TOTAL_FREQ_RUNS} frequencies × ${TOTAL_ALGO_RUNS} algorithms)${NC}"
else
    echo -e "  ${GREEN}Total Runs:          ${TOTAL_RUNS}${NC}"
fi
echo ""

# Setup output file header
echo "# Benchmark started at $(date)" > "$OUTPUT_FILE"
echo "# Configuration: algorithm=$ALGORITHM, size=$ARRAY_SIZE, warmup=$WARMUP_ITERATIONS×${WARMUP_TIME}s, measurement=$MEASUREMENT_ITERATIONS×${MEASUREMENT_TIME}s, forks=$FORKS" >> "$OUTPUT_FILE"
[ -n "$CPU_CORE" ] && echo "# CPU Core: $CPU_CORE" >> "$OUTPUT_FILE"
if [ "$USE_FREQ_STEPPING" = true ]; then
    echo "# Frequency Range: ${FREQ_START}-${FREQ_END} MHz, Step: ${FREQ_STEP} MHz" >> "$OUTPUT_FILE"
fi
echo "" >> "$OUTPUT_FILE"

# Function to run a single algorithm benchmark
run_algorithm_benchmark() {
    local algo_name="$1"
    local freq="$2"  # Can be empty if not using frequency stepping
    local run_number="$3"
    local total_runs="$4"
    
    # Determine benchmark class and pattern for this algorithm
    local bench_info=($(determine_benchmark_class "$algo_name"))
    local bench_class="${bench_info[0]}"
    local bench_pattern="${bench_info[1]}"
    
    # Build JMH options based on benchmark class
    local jmh_opts
    if [ "$bench_class" = "UnifiedEnergyBenchmark" ]; then
        jmh_opts="-p size=$ARRAY_SIZE"
    else
        jmh_opts="-p arraySize=$ARRAY_SIZE"
    fi
    jmh_opts="$jmh_opts -wi $WARMUP_ITERATIONS -w ${WARMUP_TIME}s"
    jmh_opts="$jmh_opts -i $MEASUREMENT_ITERATIONS -r ${MEASUREMENT_TIME}s"
    jmh_opts="$jmh_opts -f $FORKS"
    jmh_opts="$jmh_opts $EXTRA_JMH_OPTS"
    
    # Build JVM options with algorithm filter
    local jvm_opts="$BASE_JVM_OPTS -Dbenchmark.filter=${algo_name}"
    if [ -n "$freq" ]; then
        jvm_opts="$jvm_opts -Dbenchmark.cpu.frequency=${freq}"
    fi
    
    # Build execution command
    local exec_cmd
    if [ -n "$CPU_CORE" ]; then
        exec_cmd="sudo taskset -c $CPU_CORE java $jvm_opts -jar target/benchmarks.jar ${bench_class}.$bench_pattern $jmh_opts"
    else
        exec_cmd="sudo java $jvm_opts -jar target/benchmarks.jar ${bench_class}.$bench_pattern $jmh_opts"
    fi
    
    # Display banner
    local banner_text="Running ${algo_name}"
    if [ -n "$freq" ]; then
        banner_text="$banner_text @ ${freq} MHz"
    fi
    banner_text="$banner_text (${run_number}/${total_runs})"
    
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}${banner_text}${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo ""
    
    echo -e "${YELLOW}Executing: ${algo_name}${NC}"
    echo -e "  $exec_cmd"
    echo ""
    
    # Log to output file
    echo "# ========================================" >> "$OUTPUT_FILE"
    echo "# Running: ${algo_name}" >> "$OUTPUT_FILE"
    [ -n "$freq" ] && echo "# Frequency: ${freq} MHz" >> "$OUTPUT_FILE"
    echo "# Run: ${run_number}/${total_runs}" >> "$OUTPUT_FILE"
    echo "# Command: $exec_cmd" >> "$OUTPUT_FILE"
    echo "# ========================================" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    
    # Run benchmark
    if ! eval "$exec_cmd" 2>&1 | tee -a "$OUTPUT_FILE"; then
        echo ""
        echo -e "${RED}✗ Benchmark failed for ${algo_name}!${NC}"
        echo -e "${YELLOW}Check $OUTPUT_FILE for details${NC}"
        return 1
    fi
    
    echo ""
    echo -e "${GREEN}✓ Completed ${algo_name} (${run_number}/${total_runs})${NC}"
    return 0
}

# Run benchmark
echo -e "${YELLOW}Note: Running with sudo for RAPL energy measurements${NC}"

if [ "$USE_FREQ_STEPPING" = true ]; then
    # Save current CPU state BEFORE disabling hyperthreading
    save_cpu_state
    
    # Disable hyperthreading before running benchmarks
    disable_hyperthreading
    echo ""
    
    # Set governor to userspace for manual frequency control
    set_cpu_governor "userspace"
    echo ""
    
    # Run benchmark for each frequency and algorithm combination
    CURRENT_RUN=0
    for freq in "${FREQUENCIES[@]}"; do
        echo ""
        echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
        echo -e "${BLUE}║  Setting CPU Frequency: ${freq} MHz$(printf '%*s' $((31 - ${#freq})) '')║${NC}"
        echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
        
        # Set CPU frequency
        set_cpu_frequency "$freq"
        
        # Run each algorithm at this frequency
        for algo in "${ALGORITHMS_TO_RUN[@]}"; do
            CURRENT_RUN=$((CURRENT_RUN + 1))
            
            if ! run_algorithm_benchmark "$algo" "$freq" "$CURRENT_RUN" "$TOTAL_RUNS"; then
                exit 1
            fi
            
            # Small delay between benchmarks
            sleep 1
        done
    done
    
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}All benchmarks completed successfully!${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
else
    # Single run without frequency stepping
    # Disable hyperthreading before running benchmarks
    disable_hyperthreading
    echo ""
    
    echo -e "${GREEN}Starting benchmarks...${NC}"
    
    # Get current frequency for logging
    CURRENT_FREQ=$(get_current_cpu_frequency)
    echo -e "${BLUE}Current CPU Frequency: ${CURRENT_FREQ} MHz${NC}"
    
    # Run each algorithm
    CURRENT_RUN=0
    for algo in "${ALGORITHMS_TO_RUN[@]}"; do
        CURRENT_RUN=$((CURRENT_RUN + 1))
        
        if ! run_algorithm_benchmark "$algo" "$CURRENT_FREQ" "$CURRENT_RUN" "$TOTAL_RUNS"; then
            exit 1
        fi
        
        # Small delay between benchmarks
        sleep 1
    done
    
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}All benchmarks completed successfully!${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
fi

echo ""
echo -e "${BLUE}Results saved to:${NC}"
echo -e "  JMH Log:    $OUTPUT_FILE"
echo -e "  Energy CSV: results/energy_${ENERGY_TIMESTAMP}.csv"

