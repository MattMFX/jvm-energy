#!/bin/bash

# Energy Measurement Benchmark Runner
# A convenient script to run JMH energy benchmarks with customizable parameters

set -e

# Default values
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
FREQ_START="500"
FREQ_END="3500"
FREQ_STEP=""
RESTORE_GOVERNOR=true

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
    --freq-start MHZ            Starting CPU frequency in MHz (default: 500)
    --freq-end MHZ              Ending CPU frequency in MHz (default: 3500)
    --freq-step MHZ             Frequency step size in MHz (optional, runs single frequency if not set)
    --no-restore                Don't restore original CPU governor after benchmark

${BLUE}Examples:${NC}
    # Run all algorithms with default settings (no frequency stepping)
    $0

    # Run only quicksort on array size 50000
    $0 -a quick_sort -s 50000

    # Run benchmarks across frequencies from 1000 to 3000 MHz in 500 MHz steps
    $0 --freq-start 1000 --freq-end 3000 --freq-step 500

    # Run bubble sort with frequency stepping every 250 MHz
    $0 -a bubble_sort --freq-step 250

    # Run with reduced iterations for quick testing with frequency steps
    $0 -w 10 -m 3 --freq-start 1500 --freq-end 2500 --freq-step 500

    # Run on core 0, with frequency stepping
    $0 -c 0 --freq-step 1000 -o results/my_benchmark.log

    # Run all algorithms with custom parameters and frequency stepping
    $0 -s 20000 -w 30 -wt 3 -m 10 -mt 3 -f 2 --freq-step 500

${BLUE}Notes:${NC}
    - The project is always rebuilt before running ('mvn clean package')
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
            ORIGINAL_GOVERNORS+=("$(cat "${cpu_dir}scaling_governor" 2>/dev/null)")
            ORIGINAL_MIN_FREQS+=("$(cat "${cpu_dir}scaling_min_freq" 2>/dev/null)")
            ORIGINAL_MAX_FREQS+=("$(cat "${cpu_dir}scaling_max_freq" 2>/dev/null)")
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
            echo "${ORIGINAL_MIN_FREQS[$idx]}" | sudo tee "${cpu_dir}scaling_min_freq" > /dev/null 2>&1
            echo "${ORIGINAL_MAX_FREQS[$idx]}" | sudo tee "${cpu_dir}scaling_max_freq" > /dev/null 2>&1
            echo "${ORIGINAL_GOVERNORS[$idx]}" | sudo tee "${cpu_dir}scaling_governor" > /dev/null 2>&1
            idx=$((idx + 1))
        fi
    done
    echo -e "${GREEN}CPU state restored${NC}"
}

set_cpu_governor() {
    local governor="$1"
    echo -e "${BLUE}Setting CPU governor to '$governor'...${NC}"
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
        echo "$governor" | sudo tee "$cpu" > /dev/null 2>&1
    done
}

set_cpu_frequency() {
    local freq_mhz="$1"
    local freq_khz=$((freq_mhz * 1000))
    
    echo -e "${BLUE}Setting CPU frequency to ${freq_mhz} MHz...${NC}"
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq; do
        echo "$freq_khz" | sudo tee "$cpu" > /dev/null 2>&1
    done
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_max_freq; do
        echo "$freq_khz" | sudo tee "$cpu" > /dev/null 2>&1
    done
    
    # Verify frequency was set
    sleep 0.5
    local actual_freq=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq 2>/dev/null)
    local actual_mhz=$((actual_freq / 1000))
    echo -e "${GREEN}CPU frequency set to ${actual_mhz} MHz${NC}"
}

get_current_cpu_frequency() {
    local freq=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq 2>/dev/null)
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

# Build JMH options (without frequency-specific options)
JMH_OPTS="-p arraySize=$ARRAY_SIZE"
JMH_OPTS="$JMH_OPTS -wi $WARMUP_ITERATIONS -w ${WARMUP_TIME}s"
JMH_OPTS="$JMH_OPTS -i $MEASUREMENT_ITERATIONS -r ${MEASUREMENT_TIME}s"
JMH_OPTS="$JMH_OPTS -f $FORKS"
JMH_OPTS="$JMH_OPTS $EXTRA_JMH_OPTS"

# Base JVM options (without frequency)
BASE_JVM_OPTS="-Djava.library.path=. -Djmh.ignoreLock=true"
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
    TOTAL_RUNS=${#FREQUENCIES[@]}
else
    USE_FREQ_STEPPING=false
    TOTAL_RUNS=1
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
if [ "$USE_FREQ_STEPPING" = true ]; then
    echo -e "  ${GREEN}Frequency Range:     ${FREQ_START} - ${FREQ_END} MHz (step: ${FREQ_STEP} MHz)${NC}"
    echo -e "  ${GREEN}Total Frequency Steps: ${TOTAL_RUNS}${NC}"
    echo -e "  ${GREEN}Frequencies:         ${FREQUENCIES[*]} MHz${NC}"
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

# Disable hyperthreading before running benchmarks
disable_hyperthreading
echo ""

# Run benchmark
echo -e "${YELLOW}Note: Running with sudo for RAPL energy measurements${NC}"

if [ "$USE_FREQ_STEPPING" = true ]; then
    # Save current CPU state
    save_cpu_state
    
    # Set governor to userspace for manual frequency control
    set_cpu_governor "userspace"
    echo ""
    
    # Run benchmark for each frequency
    CURRENT_RUN=0
    for freq in "${FREQUENCIES[@]}"; do
        CURRENT_RUN=$((CURRENT_RUN + 1))
        echo ""
        echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}Running benchmark ${CURRENT_RUN}/${TOTAL_RUNS} at ${freq} MHz${NC}"
        echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
        echo ""
        
        # Set CPU frequency
        set_cpu_frequency "$freq"
        
        # Build JVM options with frequency
        JVM_OPTS="$BASE_JVM_OPTS -Dbenchmark.cpu.frequency=${freq}"
        
        # Build execution command
        if [ -n "$CPU_CORE" ]; then
            EXEC_CMD="sudo taskset -c $CPU_CORE java $JVM_OPTS -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark.$BENCHMARK_PATTERN $JMH_OPTS"
        else
            EXEC_CMD="sudo java $JVM_OPTS -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark.$BENCHMARK_PATTERN $JMH_OPTS"
        fi
        
        echo -e "${YELLOW}Executing at ${freq} MHz:${NC}"
        echo -e "  $EXEC_CMD"
        echo ""
        
        # Log frequency info to output file
        echo "# ========================================" >> "$OUTPUT_FILE"
        echo "# Running at ${freq} MHz (${CURRENT_RUN}/${TOTAL_RUNS})" >> "$OUTPUT_FILE"
        echo "# Command: $EXEC_CMD" >> "$OUTPUT_FILE"
        echo "# ========================================" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        
        # Run benchmark
        if ! eval "$EXEC_CMD" 2>&1 | tee -a "$OUTPUT_FILE"; then
            echo ""
            echo -e "${RED}✗ Benchmark failed at ${freq} MHz!${NC}"
            echo -e "${YELLOW}Check $OUTPUT_FILE for details${NC}"
            exit 1
        fi
        
        echo ""
        echo -e "${GREEN}✓ Completed benchmark at ${freq} MHz (${CURRENT_RUN}/${TOTAL_RUNS})${NC}"
        
        # Small delay between frequency changes
        sleep 1
    done
    
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}All frequency benchmarks completed successfully!${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
else
    # Single run without frequency stepping
    echo -e "${GREEN}Starting benchmark...${NC}\n"
    
    # Get current frequency for logging
    CURRENT_FREQ=$(get_current_cpu_frequency)
    
    # Build JVM options with current frequency
    JVM_OPTS="$BASE_JVM_OPTS -Dbenchmark.cpu.frequency=${CURRENT_FREQ}"
    
    # Build execution command
    if [ -n "$CPU_CORE" ]; then
        EXEC_CMD="sudo taskset -c $CPU_CORE java $JVM_OPTS -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark.$BENCHMARK_PATTERN $JMH_OPTS"
    else
        EXEC_CMD="sudo java $JVM_OPTS -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark.$BENCHMARK_PATTERN $JMH_OPTS"
    fi
    
    echo -e "${YELLOW}Executing:${NC}"
    echo -e "  $EXEC_CMD"
    echo -e "${BLUE}Current CPU Frequency: ${CURRENT_FREQ} MHz${NC}"
    echo ""
    
    echo "# Command: $EXEC_CMD" >> "$OUTPUT_FILE"
    echo "# CPU Frequency: ${CURRENT_FREQ} MHz" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    
    # Run benchmark
    if ! eval "$EXEC_CMD" 2>&1 | tee -a "$OUTPUT_FILE"; then
        echo ""
        echo -e "${RED}✗ Benchmark failed!${NC}"
        echo -e "${YELLOW}Check $OUTPUT_FILE for details${NC}"
        exit 1
    fi
    
    echo ""
    echo -e "${GREEN}✓ Benchmark completed successfully!${NC}"
fi

echo -e "${BLUE}Results saved to: $OUTPUT_FILE${NC}"

