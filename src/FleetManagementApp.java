import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

/**
 * Smart Fleet Management System demonstrating the four core pillars of
 * Object-Oriented Programming (OOP) in Java:
 * 1. Abstraction    - Abstract class (Vehicle) and interfaces (Maintainable, ElectricPowered)
 * 2. Encapsulation  - Private fields, defensive validation, and controlled state transitions
 * 3. Inheritance    - Concrete subclasses (ElectricVan, DieselTruck, AutonomousShuttle) extending Vehicle
 * 4. Polymorphism   - Method overriding (runtime dispatch) and method overloading (compile-time)
 */
public class FleetManagementApp {

    // =========================================================================
    // 1. ABSTRACTION: Interfaces & Abstract Base Class
    // =========================================================================

    /**
     * Contract for any asset that requires scheduled maintenance checks.
     */
    public interface Maintainable {
        boolean isMaintenanceDue();
        void recordMaintenance(String serviceNote);
        String getMaintenanceStatus();
    }

    /**
     * Contract for electric-powered vehicles supporting battery management.
     */
    public interface ElectricPowered {
        void chargeBattery(double kwhAdded);
        double getStateOfChargePercent();
    }

    /**
     * Abstract representation of a fleet vehicle.
     * Hides low-level propulsion calculations while enforcing a unified contract.
     */
    public static abstract class Vehicle implements Maintainable {
        // =====================================================================
        // 2. ENCAPSULATION: Private state protected by invariant checks
        // =====================================================================
        private final String vehicleId;
        private String licensePlate;
        private double odometerKm;
        private double lastServiceOdometerKm;
        private final List<String> serviceHistory = new ArrayList<>();

        protected Vehicle(String vehicleId, String licensePlate, double initialOdometerKm) {
            if (vehicleId == null || vehicleId.isBlank()) {
                throw new IllegalArgumentException("Vehicle ID cannot be null or blank.");
            }
            this.vehicleId = vehicleId.trim().toUpperCase();
            setLicensePlate(licensePlate);
            if (initialOdometerKm < 0) {
                throw new IllegalArgumentException("Initial odometer reading cannot be negative.");
            }
            this.odometerKm = initialOdometerKm;
            this.lastServiceOdometerKm = 0.0;
            this.serviceHistory.add("Fleet onboarding completed at " + initialOdometerKm + " km");
        }

        public String getVehicleId() {
            return vehicleId;
        }

        public String getLicensePlate() {
            return licensePlate;
        }

        /**
         * Encapsulated mutator: validates format before updating internal state.
         */
        public final void setLicensePlate(String licensePlate) {
            if (licensePlate == null || !licensePlate.trim().toUpperCase().matches("[A-Z0-9-]{4,12}")) {
                throw new IllegalArgumentException(
                        "Invalid license plate format (expected 4-12 uppercase alphanumeric/hyphen chars): " + licensePlate);
            }
            this.licensePlate = licensePlate.trim().toUpperCase();
        }

        public double getOdometerKm() {
            return odometerKm;
        }

        public double getKmSinceLastService() {
            return odometerKm - lastServiceOdometerKm;
        }

        /**
         * Returns an unmodifiable view so callers cannot corrupt the internal list.
         */
        public List<String> getServiceHistory() {
            return Collections.unmodifiableList(serviceHistory);
        }

        /**
         * Controlled state transition: odometer can only advance forward.
         */
        protected void advanceOdometer(double distanceKm) {
            if (distanceKm <= 0) {
                throw new IllegalArgumentException("Trip distance must be positive.");
            }
            this.odometerKm += distanceKm;
        }

        @Override
        public void recordMaintenance(String serviceNote) {
            Objects.requireNonNull(serviceNote, "Service note cannot be null.");
            this.lastServiceOdometerKm = this.odometerKm;
            this.serviceHistory.add(String.format("[%.1f km] %s", this.odometerKm, serviceNote));
        }

        @Override
        public String getMaintenanceStatus() {
            return String.format("%-8s | %-27s | Plate: %-10s | Odometer: %8.1f km | Since Service: %7.1f km | Due: %s",
                    vehicleId, getVehicleType(), licensePlate, odometerKm, getKmSinceLastService(),
                    isMaintenanceDue() ? "YES" : "NO");
        }

        // Abstract methods that subclasses MUST implement
        public abstract String getVehicleType();
        public abstract double calculateTripCost(double distanceKm);
        protected abstract void consumeEnergy(double distanceKm);

        /**
         * Template method combining shared state updates with polymorphic steps.
         */
        public final double performTrip(double distanceKm) {
            consumeEnergy(distanceKm);
            advanceOdometer(distanceKm);
            return calculateTripCost(distanceKm);
        }

        @Override
        public String toString() {
            return String.format("[%s] ID: %s | Plate: %s | Odometer: %.1f km",
                    getVehicleType(), vehicleId, licensePlate, odometerKm);
        }
    }

    // =========================================================================
    // 3. INHERITANCE: Subclasses specializing Vehicle & implementing interfaces
    // =========================================================================

    /**
     * Concrete subclass representing a battery-electric urban delivery van.
     */
    public static class ElectricVan extends Vehicle implements ElectricPowered {
        private static final double SERVICE_INTERVAL_KM = 15_000.0;
        private static final double COST_PER_KWH = 0.14;

        private final double batteryCapacityKwh;
        private final double efficiencyKwhPerKm;
        private double currentChargeKwh;

        public ElectricVan(String vehicleId, String licensePlate, double initialOdometerKm,
                           double batteryCapacityKwh, double efficiencyKwhPerKm) {
            super(vehicleId, licensePlate, initialOdometerKm);
            if (batteryCapacityKwh <= 0 || efficiencyKwhPerKm <= 0) {
                throw new IllegalArgumentException("Battery capacity and efficiency must be positive.");
            }
            this.batteryCapacityKwh = batteryCapacityKwh;
            this.efficiencyKwhPerKm = efficiencyKwhPerKm;
            this.currentChargeKwh = batteryCapacityKwh; // Starts fully charged
        }

        @Override
        public String getVehicleType() {
            return "Electric Delivery Van";
        }

        @Override
        public double calculateTripCost(double distanceKm) {
            double kwhUsed = distanceKm * efficiencyKwhPerKm;
            return kwhUsed * COST_PER_KWH;
        }

        @Override
        protected void consumeEnergy(double distanceKm) {
            double requiredKwh = distanceKm * efficiencyKwhPerKm;
            if (requiredKwh > currentChargeKwh) {
                throw new IllegalStateException(String.format(
                        "Insufficient battery charge on %s: needs %.1f kWh, available %.1f kWh.",
                        getVehicleId(), requiredKwh, currentChargeKwh));
            }
            currentChargeKwh -= requiredKwh;
        }

        @Override
        public void chargeBattery(double kwhAdded) {
            if (kwhAdded <= 0) {
                throw new IllegalArgumentException("Charge amount must be positive.");
            }
            currentChargeKwh = Math.min(batteryCapacityKwh, currentChargeKwh + kwhAdded);
        }

        @Override
        public double getStateOfChargePercent() {
            return (currentChargeKwh / batteryCapacityKwh) * 100.0;
        }

        @Override
        public boolean isMaintenanceDue() {
            return getKmSinceLastService() >= SERVICE_INTERVAL_KM;
        }
    }

    /**
     * Multi-level inheritance: AutonomousShuttle extends ElectricVan and refines cost calculation.
     */
    public static class AutonomousShuttle extends ElectricVan {
        private static final double TELEMETRY_FEE_PER_KM = 0.08;
        private final int passengerCapacity;

        public AutonomousShuttle(String vehicleId, String licensePlate, double initialOdometerKm,
                                 double batteryCapacityKwh, double efficiencyKwhPerKm, int passengerCapacity) {
            super(vehicleId, licensePlate, initialOdometerKm, batteryCapacityKwh, efficiencyKwhPerKm);
            if (passengerCapacity <= 0) {
                throw new IllegalArgumentException("Passenger capacity must be positive.");
            }
            this.passengerCapacity = passengerCapacity;
        }

        public int getPassengerCapacity() {
            return passengerCapacity;
        }

        @Override
        public String getVehicleType() {
            return "Autonomous Electric Shuttle";
        }

        /**
         * Overrides parent calculation while reusing superclass logic via super.calculateTripCost().
         */
        @Override
        public double calculateTripCost(double distanceKm) {
            return super.calculateTripCost(distanceKm) + (distanceKm * TELEMETRY_FEE_PER_KM);
        }
    }

    /**
     * Concrete subclass representing a heavy-duty long-haul diesel freight truck.
     */
    public static class DieselTruck extends Vehicle {
        private static final double SERVICE_INTERVAL_KM = 10_000.0;
        private static final double DIESEL_PRICE_PER_LITER = 1.15;

        private final double fuelTankCapacityLiters;
        private final double litersPer100Km;
        private final double maxPayloadKg;
        private double currentFuelLiters;

        public DieselTruck(String vehicleId, String licensePlate, double initialOdometerKm,
                           double fuelTankCapacityLiters, double litersPer100Km, double maxPayloadKg) {
            super(vehicleId, licensePlate, initialOdometerKm);
            if (fuelTankCapacityLiters <= 0 || litersPer100Km <= 0 || maxPayloadKg <= 0) {
                throw new IllegalArgumentException("Tank capacity, fuel consumption, and max payload must be positive.");
            }
            this.fuelTankCapacityLiters = fuelTankCapacityLiters;
            this.litersPer100Km = litersPer100Km;
            this.maxPayloadKg = maxPayloadKg;
            this.currentFuelLiters = fuelTankCapacityLiters;
        }

        public void refuel(double liters) {
            if (liters <= 0) {
                throw new IllegalArgumentException("Refuel amount must be positive.");
            }
            currentFuelLiters = Math.min(fuelTankCapacityLiters, currentFuelLiters + liters);
        }

        public double getCurrentFuelLiters() {
            return currentFuelLiters;
        }

        public double getFuelTankCapacityLiters() {
            return fuelTankCapacityLiters;
        }

        public double getMaxPayloadKg() {
            return maxPayloadKg;
        }

        @Override
        public String getVehicleType() {
            return "Heavy Diesel Truck";
        }

        @Override
        public double calculateTripCost(double distanceKm) {
            double litersUsed = (distanceKm / 100.0) * litersPer100Km;
            return litersUsed * DIESEL_PRICE_PER_LITER;
        }

        @Override
        protected void consumeEnergy(double distanceKm) {
            double requiredLiters = (distanceKm / 100.0) * litersPer100Km;
            if (requiredLiters > currentFuelLiters) {
                throw new IllegalStateException(String.format(
                        "Insufficient fuel on %s: needs %.1f L, available %.1f L.",
                        getVehicleId(), requiredLiters, currentFuelLiters));
            }
            currentFuelLiters -= requiredLiters;
        }

        @Override
        public boolean isMaintenanceDue() {
            return getKmSinceLastService() >= SERVICE_INTERVAL_KM;
        }
    }

    // =========================================================================
    // 4. POLYMORPHISM: Compile-Time Overloading & Runtime Dynamic Dispatch
    // =========================================================================

    public static class FleetManager {
        private final List<Vehicle> fleet = new ArrayList<>();

        public void registerVehicle(Vehicle vehicle) {
            Objects.requireNonNull(vehicle, "Vehicle cannot be null.");
            if (findVehicleById(vehicle.getVehicleId()).isPresent()) {
                throw new IllegalArgumentException("Vehicle ID already exists: " + vehicle.getVehicleId());
            }
            fleet.add(vehicle);
        }

        public Optional<Vehicle> findVehicleById(String vehicleId) {
            if (vehicleId == null) {
                return Optional.empty();
            }
            return fleet.stream()
                    .filter(v -> v.getVehicleId().equalsIgnoreCase(vehicleId.trim()))
                    .findFirst();
        }

        /**
         * Compile-time polymorphism (Method Overloading #1): Standard trip dispatch.
         */
        public double dispatchTrip(Vehicle vehicle, double distanceKm) {
            double baseCost = vehicle.performTrip(distanceKm);
            System.out.printf("Dispatched %-36s | Distance: %6.1f km | Cost: $%6.2f%n",
                    vehicle.getVehicleType() + " (" + vehicle.getVehicleId() + ")",
                    distanceKm, baseCost);
            return baseCost;
        }

        /**
         * Compile-time polymorphism (Method Overloading #2): Freight/express trip with surcharge.
         */
        public double dispatchTrip(Vehicle vehicle, double distanceKm, double payloadKg, boolean expressRoute) {
            if (payloadKg < 0) {
                throw new IllegalArgumentException("Payload weight cannot be negative.");
            }
            if (vehicle instanceof DieselTruck truck && payloadKg > truck.getMaxPayloadKg()) {
                throw new IllegalArgumentException("Payload exceeds truck capacity of " + truck.getMaxPayloadKg() + " kg");
            }
            double baseCost = vehicle.performTrip(distanceKm);
            double payloadSurcharge = payloadKg * 0.015;
            double expressMultiplier = expressRoute ? 1.20 : 1.00;
            double totalCost = (baseCost + payloadSurcharge) * expressMultiplier;

            System.out.printf("Dispatched %-36s | Distance: %6.1f km | Payload: %6.0f kg | Express: %-5s | Total Cost: $%6.2f%n",
                    vehicle.getVehicleType() + " (" + vehicle.getVehicleId() + ")",
                    distanceKm, payloadKg, expressRoute, totalCost);
            return totalCost;
        }

        /**
         * Runtime polymorphism: Iterates through abstract Vehicle and Maintainable references
         * and invokes dynamically bound implementations.
         */
        public void printFleetAudit() {
            System.out.println("\n=== FLEET MAINTENANCE & ENERGY AUDIT (Runtime Polymorphism) ===");
            for (Vehicle v : fleet) {
                System.out.println(v.getMaintenanceStatus());
                if (v instanceof ElectricPowered ev) {
                    System.out.printf("   -> Battery State of Charge: %.1f%%%n", ev.getStateOfChargePercent());
                } else if (v instanceof DieselTruck truck) {
                    System.out.printf("   -> Diesel Fuel Remaining:   %.1f / %.1f L (Max Payload: %.0f kg)%n",
                            truck.getCurrentFuelLiters(), truck.getFuelTankCapacityLiters(), truck.getMaxPayloadKg());
                }
            }
        }
    }

    // =========================================================================
    // INTERACTIVE CONSOLE MENU & DEMO RUNNER
    // =========================================================================

    private static void runInteractiveMenu(FleetManager manager, Scanner scanner) {
        while (true) {
            System.out.println("\n=======================================================");
            System.out.println("   SMART FLEET MANAGEMENT CONSOLE (Interactive Mode)   ");
            System.out.println("=======================================================");
            System.out.println("1. View All Vehicles & Maintenance/Energy Status");
            System.out.println("2. Register a New Vehicle (Electric Van / Diesel Truck / Shuttle)");
            System.out.println("3. Dispatch a Standard Trip");
            System.out.println("4. Dispatch a Freight / Express Trip");
            System.out.println("5. Service & Recharge/Refuel a Vehicle");
            System.out.println("0. Exit");
            System.out.print("Select an option (0-5): ");

            if (!scanner.hasNextLine()) {
                break;
            }
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> manager.printFleetAudit();
                    case "2" -> addVehicleInteractive(manager, scanner);
                    case "3" -> dispatchStandardTripInteractive(manager, scanner);
                    case "4" -> dispatchFreightTripInteractive(manager, scanner);
                    case "5" -> serviceVehicleInteractive(manager, scanner);
                    case "0" -> {
                        System.out.println("Exiting Smart Fleet Management System. Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option. Please enter a number between 0 and 5.");
                }
            } catch (Exception e) {
                System.out.println("[Validation / Operational Error]: " + e.getMessage());
            }
        }
    }

    private static void addVehicleInteractive(FleetManager manager, Scanner scanner) {
        System.out.println("\nSelect Vehicle Type to Register:");
        System.out.println("  1. Electric Delivery Van");
        System.out.println("  2. Heavy Diesel Truck");
        System.out.println("  3. Autonomous Electric Shuttle");
        System.out.print("Enter type (1-3): ");
        String type = scanner.nextLine().trim();

        System.out.print("Enter Vehicle ID (e.g., EV-102): ");
        String id = scanner.nextLine().trim();

        System.out.print("Enter License Plate (4-12 uppercase chars/hyphens, e.g., KA01-EV102): ");
        String plate = scanner.nextLine().trim();

        System.out.print("Enter Initial Odometer in km (e.g., 5000): ");
        double odometer = Double.parseDouble(scanner.nextLine().trim());

        switch (type) {
            case "1" -> {
                System.out.print("Enter Battery Capacity in kWh (e.g., 90): ");
                double battery = Double.parseDouble(scanner.nextLine().trim());
                System.out.print("Enter Efficiency in kWh/km (e.g., 0.25): ");
                double eff = Double.parseDouble(scanner.nextLine().trim());
                Vehicle van = new ElectricVan(id, plate, odometer, battery, eff);
                manager.registerVehicle(van);
                System.out.println("Registered: " + van);
            }
            case "2" -> {
                System.out.print("Enter Fuel Tank Capacity in Liters (e.g., 400): ");
                double tank = Double.parseDouble(scanner.nextLine().trim());
                System.out.print("Enter Fuel Burn Rate in Liters/100km (e.g., 28): ");
                double burn = Double.parseDouble(scanner.nextLine().trim());
                System.out.print("Enter Max Payload Capacity in kg (e.g., 18000): ");
                double payload = Double.parseDouble(scanner.nextLine().trim());
                Vehicle truck = new DieselTruck(id, plate, odometer, tank, burn, payload);
                manager.registerVehicle(truck);
                System.out.println("Registered: " + truck);
            }
            case "3" -> {
                System.out.print("Enter Battery Capacity in kWh (e.g., 110): ");
                double battery = Double.parseDouble(scanner.nextLine().trim());
                System.out.print("Enter Efficiency in kWh/km (e.g., 0.22): ");
                double eff = Double.parseDouble(scanner.nextLine().trim());
                System.out.print("Enter Passenger Capacity (e.g., 16): ");
                int seats = Integer.parseInt(scanner.nextLine().trim());
                Vehicle shuttle = new AutonomousShuttle(id, plate, odometer, battery, eff, seats);
                manager.registerVehicle(shuttle);
                System.out.println("Registered: " + shuttle);
            }
            default -> System.out.println("Unknown vehicle type selected.");
        }
    }

    private static void dispatchStandardTripInteractive(FleetManager manager, Scanner scanner) {
        System.out.print("Enter Vehicle ID (e.g., EV-101, DT-202, AS-303): ");
        String id = scanner.nextLine().trim();
        Vehicle vehicle = manager.findVehicleById(id)
                .orElseThrow(() -> new IllegalArgumentException("No vehicle found with ID: " + id));

        System.out.print("Enter Trip Distance in km: ");
        double distance = Double.parseDouble(scanner.nextLine().trim());
        manager.dispatchTrip(vehicle, distance);
    }

    private static void dispatchFreightTripInteractive(FleetManager manager, Scanner scanner) {
        System.out.print("Enter Vehicle ID (e.g., DT-202): ");
        String id = scanner.nextLine().trim();
        Vehicle vehicle = manager.findVehicleById(id)
                .orElseThrow(() -> new IllegalArgumentException("No vehicle found with ID: " + id));

        System.out.print("Enter Trip Distance in km: ");
        double distance = Double.parseDouble(scanner.nextLine().trim());

        System.out.print("Enter Payload Weight in kg: ");
        double payload = Double.parseDouble(scanner.nextLine().trim());

        System.out.print("Express Route? (true/false): ");
        boolean express = Boolean.parseBoolean(scanner.nextLine().trim());

        manager.dispatchTrip(vehicle, distance, payload, express);
    }

    private static void serviceVehicleInteractive(FleetManager manager, Scanner scanner) {
        System.out.print("Enter Vehicle ID to Service & Recharge/Refuel: ");
        String id = scanner.nextLine().trim();
        Vehicle vehicle = manager.findVehicleById(id)
                .orElseThrow(() -> new IllegalArgumentException("No vehicle found with ID: " + id));

        System.out.print("Enter Maintenance Service Note (e.g., Scheduled brake & fluid check): ");
        String note = scanner.nextLine().trim();
        vehicle.recordMaintenance(note.isBlank() ? "Routine scheduled inspection" : note);

        if (vehicle instanceof ElectricPowered ev) {
            System.out.print("Enter kWh to Charge Battery (e.g., 50): ");
            double kwh = Double.parseDouble(scanner.nextLine().trim());
            ev.chargeBattery(kwh);
            System.out.printf("Serviced & charged %s -> Battery at %.1f%%%n",
                    vehicle.getVehicleId(), ev.getStateOfChargePercent());
        } else if (vehicle instanceof DieselTruck truck) {
            System.out.print("Enter Liters of Diesel to Refuel (e.g., 100): ");
            double liters = Double.parseDouble(scanner.nextLine().trim());
            truck.refuel(liters);
            System.out.printf("Serviced & refueled %s -> Fuel at %.1f / %.1f L%n",
                    vehicle.getVehicleId(), truck.getCurrentFuelLiters(), truck.getFuelTankCapacityLiters());
        }
    }

    public static void main(String[] args) {
        FleetManager manager = new FleetManager();

        // 1. Pre-load initial demonstration fleet
        ElectricVan van = new ElectricVan("EV-101", "KA01-EV101", 14_850.0, 90.0, 0.25);
        DieselTruck truck = new DieselTruck("DT-202", "MH12-DT202", 8_900.0, 400.0, 28.0, 18_000.0);
        AutonomousShuttle shuttle = new AutonomousShuttle("AS-303", "DL04-AS303", 3_200.0, 110.0, 0.22, 16);

        manager.registerVehicle(van);
        manager.registerVehicle(truck);
        manager.registerVehicle(shuttle);

        System.out.println("=== INITIAL DEMO: DISPATCHING TRIPS (Compile-Time & Runtime Polymorphism) ===");
        manager.dispatchTrip(van, 180.0);
        manager.dispatchTrip(shuttle, 95.0);
        manager.dispatchTrip(truck, 320.0, 12_500.0, true);

        manager.printFleetAudit();

        System.out.println("\n=== INITIAL DEMO: SERVICING DUE VEHICLES ===");
        if (van.isMaintenanceDue()) {
            van.recordMaintenance("15,000 km scheduled battery & brake inspection");
            van.chargeBattery(45.0);
            System.out.println("Serviced & recharged " + van.getVehicleId() + " -> " + van.getMaintenanceStatus());
            System.out.printf("Updated Battery State of Charge: %.1f%%%n", van.getStateOfChargePercent());
        }

        // 2. Launch interactive menu so you can test any vehicle or input live
        try (Scanner scanner = new Scanner(System.in)) {
            runInteractiveMenu(manager, scanner);
        }
    }
}