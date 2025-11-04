#!/bin/bash
# Script to verify and complete the switch from intel_pstate to acpi-cpufreq driver

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================"
echo "Switch from intel_pstate to acpi-cpufreq"
echo "========================================"
echo ""

# Check current driver
CURRENT_DRIVER=""
if [ -f /sys/devices/system/cpu/cpu0/cpufreq/scaling_driver ]; then
    CURRENT_DRIVER=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_driver)
    echo "Current CPU frequency driver: $CURRENT_DRIVER"
else
    echo "WARNING: Could not detect current CPU frequency driver"
fi

echo ""

# Check if intel_pstate is disabled in GRUB
if grep -q "intel_pstate=disable" /etc/default/grub; then
    echo "✓ GRUB configuration: intel_pstate=disable is present"
    
    # Check if GRUB has been updated
    if grep -q "intel_pstate=disable" /boot/grub/grub.cfg; then
        echo "✓ GRUB configuration has been updated"
    else
        echo "✗ GRUB configuration needs to be updated"
        echo ""
        echo "ACTION REQUIRED: Run the following command to update GRUB:"
        echo "  sudo update-grub"
        echo ""
    fi
else
    echo "✗ GRUB configuration: intel_pstate=disable is NOT present"
    echo ""
    echo "ERROR: /etc/default/grub needs to be modified"
    echo "Add 'intel_pstate=disable' to GRUB_CMDLINE_LINUX_DEFAULT"
fi

echo ""

# Check if system needs reboot
if [ "$CURRENT_DRIVER" = "intel_pstate" ]; then
    if grep -q "intel_pstate=disable" /boot/grub/grub.cfg; then
        echo "✗ System needs to be rebooted for changes to take effect"
        echo ""
        echo "ACTION REQUIRED: Reboot your system with:"
        echo "  sudo reboot"
    fi
elif [ "$CURRENT_DRIVER" = "acpi-cpufreq" ]; then
    echo "✓ SUCCESS: acpi-cpufreq driver is active!"
    echo ""
    echo "Available governors:"
    if [ -f /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors ]; then
        cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors
    fi
    echo ""
    echo "You can now use the cpu_governor_manager.sh script to manage CPU frequencies"
else
    echo "? Unknown driver: $CURRENT_DRIVER"
fi

echo ""
echo "========================================"
echo "Summary of steps to switch drivers:"
echo "========================================"
echo "1. Modify /etc/default/grub to add intel_pstate=disable"
echo "2. Run: sudo update-grub"
echo "3. Run: sudo reboot"
echo "4. After reboot, verify with: cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_driver"
echo ""
echo "To ensure acpi-cpufreq module is available, you may also need:"
echo "  sudo modprobe acpi-cpufreq"
echo ""


