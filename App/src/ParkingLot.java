import java.util.*;

enum SpotStatus { EMPTY, OCCUPIED, DELETED }

class ParkingSpot {
    String licensePlate;
    long entryTime;
    SpotStatus status;

    ParkingSpot() {
        this.status = SpotStatus.EMPTY;
    }
}

public class ParkingLot {
    private ParkingSpot[] spots;
    private int totalSpots = 500;
    private int occupiedCount = 0;
    private int totalProbes = 0;
    private Map<Integer, Integer> hourlyOccupancy = new HashMap<>();

    public ParkingLot() {
        spots = new ParkingSpot[totalSpots];
        for (int i = 0; i < totalSpots; i++) {
            spots[i] = new ParkingSpot();
        }
    }

    private int hash(String licensePlate) {
        return Math.abs(licensePlate.hashCode()) % totalSpots;
    }

    public String parkVehicle(String licensePlate) {
        int index = hash(licensePlate);
        int probes = 0;

        while (spots[index].status == SpotStatus.OCCUPIED) {
            index = (index + 1) % totalSpots;
            probes++;
        }

        spots[index].licensePlate = licensePlate;
        spots[index].entryTime = System.currentTimeMillis();
        spots[index].status = SpotStatus.OCCUPIED;
        occupiedCount++;
        totalProbes += probes;

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        hourlyOccupancy.put(hour, hourlyOccupancy.getOrDefault(hour, 0) + 1);

        return "Assigned spot #" + index + " (" + probes + " probes)";
    }

    public String exitVehicle(String licensePlate) {
        int index = hash(licensePlate);

        while (spots[index].status != SpotStatus.EMPTY) {
            if (spots[index].licensePlate.equals(licensePlate)) {
                long duration = System.currentTimeMillis() - spots[index].entryTime;
                double hours = duration / (1000.0 * 60 * 60);
                double fee = hours * 5.0; // $5 per hour

                spots[index].status = SpotStatus.EMPTY;
                spots[index].licensePlate = null;
                occupiedCount--;

                return "Spot #" + index + " freed, Duration: " +
                        String.format("%.2f", hours) + "h, Fee: $" + String.format("%.2f", fee);
            }
            index = (index + 1) % totalSpots;
        }
        return "Vehicle not found";
    }

    public String getStatistics() {
        double occupancy = (occupiedCount * 100.0) / totalSpots;
        double avgProbes = occupiedCount == 0 ? 0 : (totalProbes * 1.0 / occupiedCount);

        int peakHour = hourlyOccupancy.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);

        return String.format("Occupancy: %.1f%%, Avg Probes: %.2f, Peak Hour: %d:00",
                occupancy, avgProbes, peakHour);
    }

    // For testing
    public static void main(String[] args) throws InterruptedException {
        ParkingLot lot = new ParkingLot();
        System.out.println(lot.parkVehicle("ABC-1234")); // spot #127
        System.out.println(lot.parkVehicle("ABC-1235")); // linear probe
        System.out.println(lot.parkVehicle("XYZ-9999")); // further probe
        Thread.sleep(2000); // simulate parking duration
        System.out.println(lot.exitVehicle("ABC-1234")); // fee calculation
        System.out.println(lot.getStatistics()); // occupancy stats
    }
}