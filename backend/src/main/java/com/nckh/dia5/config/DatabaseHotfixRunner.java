package com.nckh.dia5.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHotfixRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        fixOwnershipHistoryForeignKey();
        fixDrugVerificationRecordsForeignKey();
        dropProblematicTriggers();
    }

    private void fixOwnershipHistoryForeignKey() {
        try {
            jdbcTemplate.execute("ALTER TABLE ownership_history DROP FOREIGN KEY ownership_history_ibfk_1");
            log.info("Dropped FK ownership_history_ibfk_1");
        } catch (Exception e) {
            log.warn("Skipping drop FK ownership_history_ibfk_1: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE ownership_history ADD CONSTRAINT ownership_history_ibfk_1 FOREIGN KEY (batch_id) REFERENCES drug_batches (id)");
            log.info("Added FK ownership_history_ibfk_1 -> drug_batches(id)");
        } catch (Exception e) {
            log.warn("Skipping add FK ownership_history_ibfk_1: {}", e.getMessage());
        }
    }

    private void fixDrugVerificationRecordsForeignKey() {
        try {
            jdbcTemplate.execute("ALTER TABLE drug_verification_records DROP FOREIGN KEY drug_verification_records_ibfk_1");
            log.info("Dropped FK drug_verification_records_ibfk_1");
        } catch (Exception e) {
            log.warn("Skipping drop FK drug_verification_records_ibfk_1: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE drug_verification_records ADD CONSTRAINT drug_verification_records_ibfk_1 FOREIGN KEY (batch_id) REFERENCES drug_batches (id)");
            log.info("Added FK drug_verification_records_ibfk_1 -> drug_batches(id)");
        } catch (Exception e) {
            log.warn("Skipping add FK drug_verification_records_ibfk_1: {}", e.getMessage());
        }
    }

    private void dropProblematicTriggers() {
        try {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS create_ownership_history");
            log.info("Dropped trigger create_ownership_history");
        } catch (Exception e) {
            log.warn("Skipping drop trigger create_ownership_history: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS update_batch_status_on_delivery");
            log.info("Dropped trigger update_batch_status_on_delivery");
        } catch (Exception e) {
            log.warn("Skipping drop trigger update_batch_status_on_delivery: {}", e.getMessage());
        }
    }
}


