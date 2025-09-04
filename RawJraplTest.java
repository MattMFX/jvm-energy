import dev.matheus.energy.jrapl.EnergyCheckUtils;

public class RawJraplTest {
    public static void main(String[] args) {
        try {
            System.out.println("Socket count: " + EnergyCheckUtils.GetSocketNum());
            
            // Call the native method directly to see raw format
            String rawStats = EnergyCheckUtils.EnergyStatCheck();
            System.out.println("Raw native format: [" + rawStats + "]");
            
            // Test our wrapper method
            String wrappedStats = EnergyCheckUtils.getEnergyStats();
            System.out.println("Wrapped format: [" + wrappedStats + "]");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
