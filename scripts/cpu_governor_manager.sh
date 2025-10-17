CPU_BASE_PATH="/sys/devices/system/cpu"

show_status() {
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
        if [ -f "$cpu" ]; then
            cpu_num=$(echo "$cpu" | grep -o 'cpu[0-9]*' | grep -o '[0-9]*')
            governor=$(cat "$cpu")
            freq=$(cat "/sys/devices/system/cpu/cpu$cpu_num/cpufreq/scaling_cur_freq" 2>/dev/null)
            freq_mhz=$((freq / 1000))
            echo "CPU$cpu_num: $governor ($freq_mhz MHz)"
        fi
    done
}

show_frequency_info() {
    echo "CPU Frequency Information:"
    echo "=========================="
    for cpu_dir in /sys/devices/system/cpu/cpu*/cpufreq/; do
        if [ -d "$cpu_dir" ]; then
            cpu_num=$(echo "$cpu_dir" | grep -o 'cpu[0-9]*' | grep -o '[0-9]*')
            
            # Hardware limits
            hw_min_freq=$(cat "$cpu_dir/cpuinfo_min_freq" 2>/dev/null)
            hw_max_freq=$(cat "$cpu_dir/cpuinfo_max_freq" 2>/dev/null)
            
            # Current scaling limits
            scale_min_freq=$(cat "$cpu_dir/scaling_min_freq" 2>/dev/null)
            scale_max_freq=$(cat "$cpu_dir/scaling_max_freq" 2>/dev/null)
            
            # Current frequency
            cur_freq=$(cat "$cpu_dir/scaling_cur_freq" 2>/dev/null)
            
            # Convert to MHz
            hw_min_mhz=$((hw_min_freq / 1000))
            hw_max_mhz=$((hw_max_freq / 1000))
            scale_min_mhz=$((scale_min_freq / 1000))
            scale_max_mhz=$((scale_max_freq / 1000))
            cur_mhz=$((cur_freq / 1000))
            
            echo "CPU$cpu_num:"
            echo "  Hardware Range: $hw_min_mhz - $hw_max_mhz MHz"
            echo "  Scaling Range:  $scale_min_mhz - $scale_max_mhz MHz"
            echo "  Current Freq:   $cur_mhz MHz"
            echo ""
        fi
    done
}

set_governor() {
    local governor="$1"
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
        echo "$governor" > "$cpu" 2>/dev/null
    done
    echo "Set all CPUs to $governor governor"
}

set_frequency() {
    local freq_mhz="$1"
    local freq_khz=$((freq_mhz * 1000))

    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq; do
        echo "$freq_khz" > "$cpu" 2>/dev/null
    done
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_max_freq; do
        echo "$freq_khz" > "$cpu" 2>/dev/null
    done
    echo "Set all CPUs to $freq_mhz MHz"
}

restore_default_range() {
    for cpu_dir in /sys/devices/system/cpu/cpu*/cpufreq/; do
        if [ -f "$cpu_dir/cpuinfo_min_freq" ] && [ -f "$cpu_dir/cpuinfo_max_freq" ]; then
            min_freq=$(cat "$cpu_dir/cpuinfo_min_freq")
            max_freq=$(cat "$cpu_dir/cpuinfo_max_freq")
            echo "$min_freq" > "$cpu_dir/scaling_min_freq" 2>/dev/null
            echo "$max_freq" > "$cpu_dir/scaling_max_freq" 2>/dev/null
        fi
    done
    echo "Restored default frequency range for all CPUs"
}

case "$1" in
    "status")
        show_status
        ;;
    "freq-info"|"frequencies")
        show_frequency_info
        ;;
    "performance")
        set_governor "performance"
        ;;
    "powersave")
        set_governor "powersave"
        ;;
    "ondemand")
        set_governor "ondemand"
        ;;
    "userspace")
        set_governor "userspace"
        ;;
    "governor")
        set_governor "$2"
        ;;
    "frequency")
        set_frequency "$2"
        ;;
    "restore")
        restore_default_range
        ;;
    *)
        echo "Usage: $0 [status|freq-info|performance|powersave|ondemand|userspace|governor <name>|frequency <mhz>|restore]"
        echo "Examples:"
        echo "  $0 status              # Show current status"
        echo "  $0 freq-info           # Show detailed frequency information"
        echo "  $0 performance         # Set performance governor"
        echo "  $0 governor conservative # Set specific governor"
        echo "  $0 frequency 2400      # Set 2400 MHz frequency"
        echo "  $0 restore             # Restore default frequency range"
        ;;
esac1805
