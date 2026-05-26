package com.nckh.dia5.service;

import com.nckh.dia5.dto.medical.SymptomReportRequest;
import com.nckh.dia5.handler.ResourceNotFoundException;
import com.nckh.dia5.model.Symptom;
import com.nckh.dia5.model.User;
import com.nckh.dia5.model.UserSymptomReport;
import com.nckh.dia5.repository.SymptomRepository;
import com.nckh.dia5.repository.UserSymptomReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomReportService {

    private final UserSymptomReportRepository symptomReportRepository;
    private final SymptomRepository symptomRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserSymptomReport reportSymptom(SymptomReportRequest request) {
        User user = authService.getCurrentUser();

        Symptom symptom = symptomRepository.findById(request.getSymptomId())
                .orElseThrow(() -> new ResourceNotFoundException("Symptom", "id", request.getSymptomId()));

        UserSymptomReport report = new UserSymptomReport();
        report.setUser(user);
        report.setSymptom(symptom);
        report.setSessionId(request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString());
        report.setSeverity(request.getSeverity());
        report.setDurationHours(request.getDurationHours());
        report.setFrequency(request.getFrequency());
        report.setLocationBodyPart(request.getLocationBodyPart());
        report.setQualityDescription(request.getQualityDescription());
        report.setOnsetType(request.getOnsetType());

        // Convert lists to JSON strings
        try {
            if (request.getTriggers() != null && !request.getTriggers().isEmpty()) {
                report.setTriggers(objectMapper.writeValueAsString(request.getTriggers()));
            }
            if (request.getAssociatedSymptoms() != null && !request.getAssociatedSymptoms().isEmpty()) {
                report.setAssociatedSymptoms(objectMapper.writeValueAsString(request.getAssociatedSymptoms()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error converting lists to JSON", e);
            throw new RuntimeException("Error processing symptom data");
        }

        UserSymptomReport savedReport = symptomReportRepository.save(report);
        log.info("Symptom reported: user={}, symptom={}, session={}", user.getId(), symptom.getId(),
                report.getSessionId());

        return savedReport;
    }

    public Page<UserSymptomReport> getUserSymptomReports(Pageable pageable) {
        User user = authService.getCurrentUser();
        return symptomReportRepository.findByUserId(user.getId(), pageable);
    }

    public List<UserSymptomReport> getSymptomReportsBySession(String sessionId) {
        User user = authService.getCurrentUser();
        return symptomReportRepository.findByUserIdAndSessionId(user.getId(), sessionId);
    }

    public List<String> getUserSessionIds() {
        User user = authService.getCurrentUser();
        return symptomReportRepository.findDistinctSessionIdsByUserId(user.getId());
    }

    public Long getUserSymptomReportCount() {
        User user = authService.getCurrentUser();
        return symptomReportRepository.countByUserId(user.getId());
    }
}
