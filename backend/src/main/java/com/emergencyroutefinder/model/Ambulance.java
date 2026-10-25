package com.emergencyroutefinder.model;

/**
 * Ambulance — concrete subclass of EmergencyVehicle.
 *
 * Demonstrates: Inheritance, method overriding, OOP hierarchy
 */
public class Ambulance extends EmergencyVehicle {

    public Ambulance() {
        super();
    }

    public Ambulance(String vehicleNumber, Long workerId) {
        super(vehicleNumber, VehicleType.AMBULANCE, workerId);
    }

    @Override
    public String getVehicleCategory() {
        return "Ambulance";
    }

    @Override
    public String getPrimaryEmergencyType() {
        return "MEDICAL";
    }
}
