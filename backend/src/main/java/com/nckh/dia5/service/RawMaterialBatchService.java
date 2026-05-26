package com.nckh.dia5.service;

import com.nckh.dia5.model.RawMaterialBatch;
import com.nckh.dia5.repository.RawMaterialBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RawMaterialBatchService {

    private final RawMaterialBatchRepository repository;

    public List<RawMaterialBatch> getAllBatches() {
        return repository.findAll();
    }

    public RawMaterialBatch getBatchById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional
    public RawMaterialBatch createBatch(RawMaterialBatch batch) {
        if (repository.findByBatchNumber(batch.getBatchNumber()).isPresent()) {
            throw new IllegalArgumentException("Mã lô nguyên liệu đã tồn tại!");
        }
        return repository.save(batch);
    }

    @Transactional
    public RawMaterialBatch updateStatus(Long id, String status, String notes) {
        RawMaterialBatch batch = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô nguyên liệu"));
        
        batch.setStatus(status);
        if (notes != null && !notes.isEmpty()) {
            batch.setQualityControlNotes(notes);
        }
        return repository.save(batch);
    }

    @Transactional
    public void deleteBatch(Long id) {
        RawMaterialBatch batch = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô nguyên liệu"));
        
        if ("APPROVED".equals(batch.getStatus()) && batch.getQuantity() < 0) {
             throw new IllegalArgumentException("Không thể xóa lô nguyên liệu đã được đưa vào sản xuất!");
        }
        repository.deleteById(id);
    }
}
