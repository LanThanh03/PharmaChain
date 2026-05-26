package com.nckh.dia5.service;

import com.nckh.dia5.dto.medical.ProvinceResponse;
import com.nckh.dia5.model.Province;
import com.nckh.dia5.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvinceService {

    private final ProvinceRepository provinceRepository;

    public List<ProvinceResponse> getAllProvinces() {
        List<Province> provinces = provinceRepository.findAllOrderByName();
        return provinces.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProvinceResponse> getProvincesByRegion(Province.Region region) {
        List<Province> provinces = provinceRepository.findByRegion(region);
        return provinces.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProvinceResponse> getProvincesByClimate(Province.Climate climate) {
        List<Province> provinces = provinceRepository.findByClimate(climate);
        return provinces.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProvinceResponse mapToResponse(Province province) {
        return ProvinceResponse.builder()
                .id(province.getId())
                .name(province.getName())
                .code(province.getCode())
                .region(province.getRegion())
                .climate(province.getClimate())
                .build();
    }
}
