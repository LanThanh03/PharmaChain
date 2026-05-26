package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.dto.medical.MedicalSpecialtyResponse;
import com.nckh.dia5.service.MedicalSpecialtyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-specialties")
@RequiredArgsConstructor
public class MedicalSpecialtyController {

    private final MedicalSpecialtyService medicalSpecialtyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicalSpecialtyResponse>>> getAllSpecialties() {
        List<MedicalSpecialtyResponse> response = medicalSpecialtyService.getAllSpecialties();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<MedicalSpecialtyResponse>>> getRootSpecialties() {
        List<MedicalSpecialtyResponse> response = medicalSpecialtyService.getRootSpecialties();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<ApiResponse<List<MedicalSpecialtyResponse>>> getChildSpecialties(
            @PathVariable Integer parentId) {
        List<MedicalSpecialtyResponse> response = medicalSpecialtyService.getChildSpecialties(parentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MedicalSpecialtyResponse>>> searchSpecialties(@RequestParam String keyword) {
        List<MedicalSpecialtyResponse> response = medicalSpecialtyService.searchSpecialties(keyword);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
