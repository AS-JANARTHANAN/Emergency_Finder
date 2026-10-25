package com.emergencyroutefinder.controller;

import com.emergencyroutefinder.hospital.HospitalService;
import com.emergencyroutefinder.model.Hospital;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Hospital Controller — nearby hospital search and details.
 */
@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<Hospital>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "20.0") double maxKm) {
        return ResponseEntity.ok(hospitalService.findNearby(lat, lng, maxKm));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hospital> getHospital(@PathVariable Long id) {
        Optional<Hospital> h = hospitalService.findById(id);
        return h.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Hospital>> getAll() {
        return ResponseEntity.ok(hospitalService.findAll());
    }
}
