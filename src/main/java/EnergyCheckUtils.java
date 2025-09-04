import java.lang.reflect.Field;

public class EnergyCheckUtils {
    public native static int scale(int freq);
    public native static int[] freqAvailable();

    public native static double[] GetPackagePowerSpec();
    public native static double[] GetDramPowerSpec();
    public native static void SetPackagePowerLimit(int socketId, int level, double costomPower);
    public native static void SetPackageTimeWindowLimit(int socketId, int level, double costomTimeWin);
    public native static void SetDramTimeWindowLimit(int socketId, int level, double costomTimeWin);
    public native static void SetDramPowerLimit(int socketId, int level, double costomPower);
    public native static int ProfileInit();
    public native static int GetSocketNum();
    public native static String EnergyStatCheck();
    public native static void ProfileDealloc();
    public native static void SetPowerLimit(int ENABLE);
    
    public static int wraparoundValue;
    public static int socketNum;
    
    static {
        System.setProperty("java.library.path", System.getProperty("user.dir"));
        try {
            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);
        } catch (Exception e) { }

        System.loadLibrary("CPUScaler");
        wraparoundValue = ProfileInit();
        socketNum = GetSocketNum();
    }

    /**
     * Initialize energy monitoring
     */
    public static void init() {
        // ProfileInit is already called in static block, but we can call it again if needed
        ProfileInit();
    }

    /**
     * Cleanup energy monitoring resources
     */
    public static void dealloc() {
        ProfileDealloc();
    }

    /**
     * Get current energy stats as a formatted string for compatibility with existing benchmark code
     * @return energy statistics formatted as a comma-separated string starting with package energy
     */
    public static String getEnergyStats() {
        double[] stats = getEnergyStatsArray();
        if (stats.length >= 3) {
            // Return package energy first (index 2), then dram (index 0), then cpu (index 1)
            return String.format("%.9f,%.9f,%.9f", stats[2], stats[0], stats[1]);
        } else {
            return "0.0,0.0,0.0";
        }
    }

    /**
     * @return an array of current energy information.
     * The first entry is: Dram/uncore gpu energy(depends on the cpu architecture.
     * The second entry is: CPU energy
     * The third entry is: Package energy
     */
    public static double[] getEnergyStatsArray() {
        socketNum = GetSocketNum();
        String EnergyInfo = EnergyStatCheck();
        
        /*One Socket*/
        if(socketNum == 1) {
            double[] stats = new double[3];
            String[] energy = EnergyInfo.split("#");

            stats[0] = Double.parseDouble(energy[0]);
            stats[1] = Double.parseDouble(energy[1]);
            stats[2] = Double.parseDouble(energy[2]);

            return stats;

        } else {
        /*Multiple sockets*/
            String[] perSockEner = EnergyInfo.split("@");
            double[] stats = new double[3*socketNum];
            int count = 0;

            for(int i = 0; i < perSockEner.length; i++) {
                String[] energy = perSockEner[i].split("#");
                for(int j = 0; j < energy.length; j++) {
                    count = i * 3 + j;	//accumulative count
                    stats[count] = Double.parseDouble(energy[j]);
                }
            }
            return stats;
        }
    }

    public static void main(String[] args) {
        double[] before = getEnergyStatsArray();
        try {
            Thread.sleep(10000);
        } catch(Exception e) {
        }
        double[] after = getEnergyStatsArray();
        for(int i = 0; i < socketNum; i++) {
            System.out.println("Power consumption of dram: " + (after[0] - before[0]) / 10.0 + " power consumption of cpu: " + (after[1] - before[1]) / 10.0 + " power consumption of package: " + (after[2] - before[2]) / 10.0);
        }
        ProfileDealloc();
    }
}
