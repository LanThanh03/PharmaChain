package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.PharmaCompany;
import com.nckh.dia5.repository.PharmaCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pharmacies")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmaCompanyRepository pharmaCompanyRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PharmaCompany>>> getAllPharmacies() {
        log.info("Getting all pharmacies");
        List<PharmaCompany> pharmacies = pharmaCompanyRepository.findByCompanyTypeAndIsActive(
                PharmaCompany.CompanyType.PHARMACY, true);
        return ResponseEntity.ok(ApiResponse.success(pharmacies, "Lấy danh sách hiệu thuốc thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PharmaCompany>> getPharmacyById(@PathVariable Long id) {
        log.info("Getting pharmacy by id: {}", id);
        return pharmaCompanyRepository.findById(id)
                .map(pharmacy -> ResponseEntity.ok(ApiResponse.success(pharmacy, "Lấy thông tin hiệu thuốc thành công")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/wallet/{walletAddress}")
    public ResponseEntity<ApiResponse<PharmaCompany>> getPharmacyByWallet(@PathVariable String walletAddress) {
        log.info("Getting pharmacy by wallet: {}", walletAddress);
        return pharmaCompanyRepository.findByWalletAddress(walletAddress)
                .map(pharmacy -> ResponseEntity.ok(ApiResponse.success(pharmacy, "Lấy thông tin hiệu thuốc thành công")))
                .orElse(ResponseEntity.notFound().build());
    }
}
