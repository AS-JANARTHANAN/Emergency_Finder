package com.emergencyroutefinder.model;

/**
 * FireEngine — concrete subclass of EmergencyVehicle.
 *
 * Demonstrates: Inheritance, method overriding, OOP hierarchy
 */
public class FireEngine extends EmergencyVehicle {

    public FireEngine() {
        super();
    }

    public FireEngine(String vehicleNumber, Long workerId) {
        super(vehicleNumber, VehicleType.FIRE_ENGINE, workerId);
    }

    @Override
    public String getVehicleCategory() {
        return "Fire Engine";
    }

    @Override
    public String getPrimaryEmergencyType() {
        return "FIRE";
    }
}
