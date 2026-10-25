package com.emergencyroutefinder.hospital;

import com.emergencyroutefinder.model.Hospital;
import com.emergencyroutefinder.repository.HospitalRepository;
import com.emergencyroutefinder.util.HaversineUtil;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Hospital Service — finds and sorts nearby hospitals for medical emergencies.
 */
@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    /**
     * Find hospitals near a location, sorted by distance.
     *
     * @param lat     Emergency location latitude
     * @param lng     Emergency location longitude
     * @param maxKm   Maximum search radius in km
     * @return sorted list of hospitals with distanceKm populated
     */
    public List<Hospital> findNearby(double lat, double lng, double maxKm) {
        List<Hospital> hospitals = hospitalRepository.findActiveWithEmergency();

        // Calculate distance for each hospital
        for (Hospital h : hospitals) {
            double dist = HaversineUtil.distanceKm(lat, lng, h.getLat(), h.getLng());
            h.setDistanceKm(Math.round(dist * 10.0) / 10.0); // Round to 1 decimal
        }

        // Filter by radius and sort by distance
        return hospitals.stream()
                .filter(h -> h.getDistanceKm() <= maxKm)
                .sorted(Comparator.comparingDouble(Hospital::getDistanceKm))
                .toList();
    }

    public Optional<Hospital> findById(Long id) {
        return hospitalRepository.findById(id);
    }

    public List<Hospital> findAll() {
        return hospitalRepository.findAllActive();
    }
}
