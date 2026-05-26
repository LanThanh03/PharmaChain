package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.Medication;
import com.nckh.dia5.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationRepository medicationRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Medication>>> getAllMedications() {
        log.info("Getting all medications");
        List<Medication> medications = medicationRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(medications, "Lấy danh sách thuốc thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Medication>> getMedicationById(@PathVariable Integer id) {
        log.info("Getting medication by id: {}", id);
        return medicationRepository.findById(id)
                .map(medication -> ResponseEntity.ok(ApiResponse.success(medication, "Lấy thông tin thuốc thành công")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Medication>>> searchMedications(@RequestParam String keyword) {
        log.info("Searching medications with keyword: {}", keyword);
        List<Medication> medications = medicationRepository.findAll().stream()
                .filter(m -> m.getName().toLowerCase().contains(keyword.toLowerCase()) || 
                           (m.getGenericName() != null && m.getGenericName().toLowerCase().contains(keyword.toLowerCase())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(medications, "Tìm kiếm thuốc thành công"));
    }

    @GetMapping("/drug-class/{drugClass}")
    public ResponseEntity<ApiResponse<List<Medication>>> getMedicationsByDrugClass(@PathVariable String drugClass) {
        log.info("Getting medications by drug class: {}", drugClass);
        List<Medication> medications = medicationRepository.findByDrugClass(drugClass);
        return ResponseEntity.ok(ApiResponse.success(medications, "Lấy danh sách thuốc theo phân loại thành công"));
    }
}
