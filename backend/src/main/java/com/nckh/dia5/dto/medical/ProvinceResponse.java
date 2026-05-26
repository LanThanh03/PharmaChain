package com.nckh.dia5.dto.medical;

import com.nckh.dia5.model.Province;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceResponse {
    private Integer id;
    private String name;
    private String code;
    private Province.Region region;
    private Province.Climate climate;
    private String endemicDiseases;
}
