package com.nckh.dia5.service;

import com.nckh.dia5.dto.medical.MedicalSpecialtyResponse;
import com.nckh.dia5.model.MedicalSpecialty;
import com.nckh.dia5.repository.MedicalSpecialtyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalSpecialtyService {

    private final MedicalSpecialtyRepository medicalSpecialtyRepository;

    public List<MedicalSpecialtyResponse> getAllSpecialties() {
        List<MedicalSpecialty> specialties = medicalSpecialtyRepository.findAll();
        return specialties.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MedicalSpecialtyResponse> getRootSpecialties() {
        List<MedicalSpecialty> specialties = medicalSpecialtyRepository.findRootSpecialties();
        return specialties.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MedicalSpecialtyResponse> getChildSpecialties(Integer parentId) {
        List<MedicalSpecialty> specialties = medicalSpecialtyRepository.findByParentSpecialtyId(parentId);
        return specialties.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MedicalSpecialtyResponse> searchSpecialties(String keyword) {
        List<MedicalSpecialty> specialties = medicalSpecialtyRepository.searchByKeyword(keyword);
        return specialties.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private MedicalSpecialtyResponse mapToResponse(MedicalSpecialty specialty) {
        return MedicalSpecialtyResponse.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .description(specialty.getDescription())
                .parentSpecialtyId(
                        specialty.getParentSpecialty() != null ? specialty.getParentSpecialty().getId() : null)
                .parentSpecialtyName(
                        specialty.getParentSpecialty() != null ? specialty.getParentSpecialty().getName() : null)
                .build();
    }
}
