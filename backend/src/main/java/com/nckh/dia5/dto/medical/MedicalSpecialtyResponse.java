package com.nckh.dia5.dto.medical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalSpecialtyResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer parentSpecialtyId;
    private String parentSpecialtyName;
}
