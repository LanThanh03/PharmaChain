package com.nckh.dia5.controller;

import com.nckh.dia5.util.RawInputCleaner;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API để test và clean raw input bị corrupt
 */
@RestController
@RequestMapping("/api/raw-input")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RawInputCleanerController {

    private final RawInputCleaner rawInputCleaner;

    /**
     * Clean raw input và extract item codes
     * POST /api/raw-input/clean
     */
    @PostMapping("/clean")
    public ResponseEntity<CleanResult> cleanRawInput(@RequestBody RawInputRequest request) {
        log.info("Received raw input cleaning request (length: {})", 
            request.getRawInput() != null ? request.getRawInput().length() : 0);
        
        String rawInput = request.getRawInput();
        
        // Analyze corruption
        RawInputCleaner.CorruptionStats stats = rawInputCleaner.analyzeInput(rawInput);
        log.info("Corruption stats: {}", stats);
        
        // Extract item codes
        List<String> itemCodes = rawInputCleaner.extractItemCodes(rawInput);
        
        // Get cleaned string
        String cleaned = rawInputCleaner.cleanRawInput(rawInput);
        
        CleanResult result = new CleanResult(
            itemCodes,
            cleaned,
            rawInputCleaner.isCorrupt(rawInput),
            stats
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Analyze raw input without cleaning
     * POST /api/raw-input/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyzeRawInput(@RequestBody RawInputRequest request) {
        log.info("Analyzing raw input (length: {})", 
            request.getRawInput() != null ? request.getRawInput().length() : 0);
        
        String rawInput = request.getRawInput();
        
        RawInputCleaner.CorruptionStats stats = rawInputCleaner.analyzeInput(rawInput);
        boolean isCorrupt = rawInputCleaner.isCorrupt(rawInput);
        
        AnalysisResult result = new AnalysisResult(
            rawInput.length(),
            isCorrupt,
            stats.printableCharacters,
            stats.corruptCharacters,
            stats.controlCharacters,
            stats.getCorruptionPercentage()
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Extract item codes only
     * POST /api/raw-input/extract
     */
    @PostMapping("/extract")
    public ResponseEntity<List<String>> extractItemCodes(@RequestBody RawInputRequest request) {
        log.info("Extracting item codes from raw input");
        
        List<String> itemCodes = rawInputCleaner.extractItemCodes(request.getRawInput());
        
        log.info("Extracted {} item codes", itemCodes.size());
        
        return ResponseEntity.ok(itemCodes);
    }

    // DTOs
    
    @Data
    public static class RawInputRequest {
        private String rawInput;
    }

    @Data
    @AllArgsConstructor
    public static class CleanResult {
        private List<String> itemCodes;
        private String cleanedString;
        private boolean wasCorrupt;
        private RawInputCleaner.CorruptionStats stats;
    }

    @Data
    @AllArgsConstructor
    public static class AnalysisResult {
        private int totalLength;
        private boolean isCorrupt;
        private int printableChars;
        private int corruptChars;
        private int controlChars;
        private double corruptionPercentage;
    }
}

