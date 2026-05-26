package com.nckh.dia5.dto.medical;

import com.nckh.dia5.model.UserSymptomReport;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SymptomReportRequest {
    
    private String sessionId;

    @NotNull(message = "Symptom ID is required")
    private Integer symptomId;

    private Integer severity;
    private Integer durationHours;
    private UserSymptomReport.Frequency frequency;
    private String locationBodyPart;
    private String qualityDescription;
    private UserSymptomReport.OnsetType onsetType;
    private List<String> triggers;
    private List<String> associatedSymptoms;
}
