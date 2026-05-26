package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.dto.medical.ProvinceResponse;
import com.nckh.dia5.model.Province;
import com.nckh.dia5.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provinces")
@RequiredArgsConstructor
public class ProvinceController {

    private final ProvinceService provinceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getAllProvinces() {
        List<ProvinceResponse> response = provinceService.getAllProvinces();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/region/{region}")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getProvincesByRegion(
            @PathVariable Province.Region region) {
        List<ProvinceResponse> response = provinceService.getProvincesByRegion(region);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/climate/{climate}")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getProvincesByClimate(
            @PathVariable Province.Climate climate) {
        List<ProvinceResponse> response = provinceService.getProvincesByClimate(climate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
