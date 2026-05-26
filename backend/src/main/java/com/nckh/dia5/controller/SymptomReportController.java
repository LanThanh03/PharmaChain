package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.dto.common.PageResponse;
import com.nckh.dia5.dto.medical.SymptomReportRequest;
import com.nckh.dia5.model.UserSymptomReport;
import com.nckh.dia5.service.SymptomReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/symptom-reports")
@RequiredArgsConstructor
public class SymptomReportController {

    private final SymptomReportService symptomReportService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserSymptomReport>> reportSymptom(
            @Valid @RequestBody SymptomReportRequest request) {
        UserSymptomReport response = symptomReportService.reportSymptom(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Báo cáo triệu chứng thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserSymptomReport>>> getUserSymptomReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "reportedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserSymptomReport> reports = symptomReportService.getUserSymptomReports(pageable);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(reports)));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<UserSymptomReport>>> getSymptomReportsBySession(
            @PathVariable String sessionId) {
        List<UserSymptomReport> response = symptomReportService.getSymptomReportsBySession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<String>>> getUserSessionIds() {
        List<String> response = symptomReportService.getUserSessionIds();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getUserSymptomReportCount() {
        Long count = symptomReportService.getUserSymptomReportCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
