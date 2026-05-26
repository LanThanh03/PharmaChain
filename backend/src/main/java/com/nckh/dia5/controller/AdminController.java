package com.nckh.dia5.controller;

import com.nckh.dia5.repository.BlockchainEventRepository;
import com.nckh.dia5.repository.BlockchainTransactionRepository;
import com.nckh.dia5.repository.DrugBatchRepository;
import com.nckh.dia5.repository.UserRepository;
import com.nckh.dia5.repository.ManufacturerUserRepository;
import com.nckh.dia5.repository.DistributorUserRepository;
import com.nckh.dia5.repository.PharmacyUserRepository;
import com.nckh.dia5.model.BlockchainTransaction;
import com.nckh.dia5.model.User;
import com.nckh.dia5.model.ManufacturerUser;
import com.nckh.dia5.model.DistributorUser;
import com.nckh.dia5.model.PharmacyUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DrugBatchRepository drugBatchRepository;

    @Autowired
    private BlockchainTransactionRepository blockchainTransactionRepository;

    @Autowired
    private ManufacturerUserRepository manufacturerUserRepository;

    @Autowired
    private DistributorUserRepository distributorUserRepository;

    @Autowired
    private PharmacyUserRepository pharmacyUserRepository;

    @Autowired
    private BlockchainEventRepository blockchainEventRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> response = new HashMap<>();

        // 1. Get Stats
        long totalUsers = userRepository.count() + 
                         manufacturerUserRepository.count() + 
                         distributorUserRepository.count() + 
                         pharmacyUserRepository.count();
        long totalBatches = drugBatchRepository.count();

        response.put("totalUsers", totalUsers);
        response.put("totalBatches", totalBatches);
        response.put("activeNodes", 4); // Simulated connected nodes
        response.put("networkStatus", "Healthy");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getRecentTransactions() {
        // 1. Khởi tạo danh sách kết quả
        List<Map<String, Object>> txList = new ArrayList<>();

        // 2. Lấy giao dịch từ bảng BlockchainTransaction (Ledger trực tiếp)
        blockchainTransactionRepository
                .findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp")))
                .getContent().forEach(tx -> txList.add(mapToTxMap(tx)));

        // 3. Lấy giao dịch từ bảng BlockchainEvent (Dữ liệu quét từ Blockchain)
        blockchainEventRepository.findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "blockNumber")))
                .getContent().forEach(event -> {
                    // Tránh trùng lặp nếu TX đã có trong Ledger
                    boolean exists = txList.stream().anyMatch(tx -> event.getTransactionHash().equals(tx.get("fullHash")));
                    if (!exists) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", event.getTransactionHash().substring(0, 6) + "..." + event.getTransactionHash().substring(event.getTransactionHash().length() - 4));
                        map.put("fullHash", event.getTransactionHash());
                        map.put("type", event.getEventType());
                        map.put("from", event.getFromAddress() != null && event.getFromAddress().length() > 6 ? 
                                       event.getFromAddress().substring(0, 6) + "..." : "Blockchain");
                        map.put("status", "Success");
                        map.put("gas", "N/A");
                        map.put("time", "Block: " + event.getBlockNumber());
                        map.put("rawDate", java.time.LocalDateTime.now()); // Fallback time
                        txList.add(map);
                    }
                });

        // 4. Lấy thêm các mã TX từ bảng DrugBatch
        drugBatchRepository.findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .filter(b -> b.getTransactionHash() != null)
                .forEach(b -> {
                    boolean exists = txList.stream().anyMatch(tx -> b.getTransactionHash().equals(tx.get("fullHash")));
                    if (!exists) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", b.getTransactionHash().substring(0, 6) + "..." + b.getTransactionHash().substring(b.getTransactionHash().length() - 4));
                        map.put("fullHash", b.getTransactionHash());
                        map.put("type", "Create Batch");
                        map.put("from", b.getManufacturerAddress() != null ? b.getManufacturerAddress().substring(0, 6) + "..." : "System");
                        map.put("status", "Success");
                        map.put("gas", "N/A");
                        map.put("time", b.getCreatedAt() != null ? b.getCreatedAt().toString().replace("T", " ") : "Vừa xong");
                        map.put("rawDate", b.getCreatedAt());
                        txList.add(map);
                    }
                });

        // 4. Sắp xếp lại toàn bộ list theo thời gian mới nhất (sử dụng rawDate nếu có, hoặc parse từ time) và lấy 20 dòng đầu
        txList.sort((a, b) -> {
            Object dateA = a.get("rawDate");
            Object dateB = b.get("rawDate");
            
            if (dateA instanceof java.time.LocalDateTime && dateB instanceof java.time.LocalDateTime) {
                return ((java.time.LocalDateTime) dateB).compareTo((java.time.LocalDateTime) dateA);
            }
            
            // Fallback nếu không có rawDate
            String timeA = (String) a.get("time");
            String timeB = (String) b.get("time");
            return timeB.compareTo(timeA);
        });

        List<Map<String, Object>> finalList = txList.stream().limit(20).collect(Collectors.toList());

        return ResponseEntity.ok(finalList);
    }

    @GetMapping("/users/all")
    public ResponseEntity<?> getAllSystemUsers() {
        List<Map<String, Object>> userList = new ArrayList<>();

        // Add Manufacturer Users
        manufacturerUserRepository.findAll().forEach(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("name", u.getName());
            map.put("role", u.getRole().name());
            map.put("company", u.getCompanyName());
            map.put("status", u.getIsVerified() ? "Verified" : "Pending");
            map.put("wallet", u.getWalletAddress());
            userList.add(map);
        });

        // Add Distributor Users
        distributorUserRepository.findAll().forEach(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("name", u.getName());
            map.put("role", "DISTRIBUTOR");
            map.put("company", u.getCompanyName());
            map.put("status", u.getIsVerified() ? "Verified" : "Pending");
            map.put("wallet", u.getWalletAddress());
            userList.add(map);
        });

        // Add Pharmacy Users
        pharmacyUserRepository.findAll().forEach(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId().toString()); // Convert Long to String
            map.put("email", u.getEmail());
            map.put("name", u.getPharmacyName());
            map.put("role", "PHARMACY");
            map.put("company", u.getPharmacyName());
            map.put("status", u.getIsActive() ? "Verified" : "Pending");
            map.put("wallet", u.getWalletAddress());
            userList.add(map);
        });

        return ResponseEntity.ok(userList);
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/users/create")
    public ResponseEntity<?> createSystemUser(@RequestBody Map<String, String> payload) {
        String role = payload.get("role");
        String email = payload.get("email");
        String password = payload.get("password");
        String name = payload.get("name");
        String companyName = payload.get("companyName");
        String walletAddress = payload.get("walletAddress");
        
        String encodedPassword = passwordEncoder.encode(password);
        String id = java.util.UUID.randomUUID().toString();

        if (role.equals("MANUFACTURER")) {
            ManufacturerUser u = ManufacturerUser.builder()
                .id(id)
                .email(email)
                .passwordHash(encodedPassword)
                .name(name)
                .companyName(companyName)
                .role(com.nckh.dia5.model.UserRole.MANUFACTURER)
                .walletAddress(walletAddress)
                .isVerified(true)
                .createdAt(java.time.LocalDateTime.now())
                .build();
            manufacturerUserRepository.save(u);
        } else if (role.equals("DISTRIBUTOR")) {
            DistributorUser u = DistributorUser.builder()
                .id(id)
                .email(email)
                .passwordHash(encodedPassword)
                .name(name)
                .companyName(companyName)
                .role(com.nckh.dia5.model.UserRole.DISTRIBUTOR)
                .walletAddress(walletAddress)
                .isVerified(true)
                .createdAt(java.time.LocalDateTime.now())
                .build();
            distributorUserRepository.save(u);
        } else if (role.equals("PHARMACY")) {
            PharmacyUser u = new PharmacyUser();
            u.setEmail(email);
            u.setPassword(encodedPassword);
            u.setPharmacyName(companyName);
            u.setPharmacyCode(java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            u.setWalletAddress(walletAddress);
            u.setIsActive(true);
            pharmacyUserRepository.save(u);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/verify/{id}")
    public ResponseEntity<?> toggleUserVerification(@PathVariable String id, @RequestParam String role) {
        boolean newState = false;
        if (role.equals("MANUFACTURER")) {
            ManufacturerUser u = manufacturerUserRepository.findById(id).orElseThrow();
            u.setIsVerified(!u.getIsVerified());
            manufacturerUserRepository.save(u);
            newState = u.getIsVerified();
        } else if (role.equals("DISTRIBUTOR")) {
            DistributorUser u = distributorUserRepository.findById(id).orElseThrow();
            u.setIsVerified(!u.getIsVerified());
            distributorUserRepository.save(u);
            newState = u.getIsVerified();
        } else if (role.equals("PHARMACY")) {
            PharmacyUser u = pharmacyUserRepository.findById(Long.parseLong(id)).orElseThrow();
            u.setIsActive(!u.getIsActive());
            pharmacyUserRepository.save(u);
            newState = u.getIsActive();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isVerified", newState);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> mapToTxMap(BlockchainTransaction tx) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tx.getTransactionHash() != null ? 
                     tx.getTransactionHash().substring(0, 6) + "..." + tx.getTransactionHash().substring(tx.getTransactionHash().length() - 4) 
                     : "Pending...");
        map.put("fullHash", tx.getTransactionHash());
        map.put("type", tx.getFunctionName() != null ? tx.getFunctionName() : "Contract Interaction");
        map.put("from", tx.getFromAddress() != null ? 
                       tx.getFromAddress().substring(0, 6) + "..." + tx.getFromAddress().substring(tx.getFromAddress().length() - 4)
                       : "System");
        String statusStr = "Unknown";
        if (tx.getStatus() != null) {
            switch (tx.getStatus()) {
                case SUCCESS: statusStr = "Success"; break;
                case FAILED: statusStr = "Failed"; break;
                case REVERTED: statusStr = "Reverted"; break;
                case PENDING: statusStr = "Pending"; break;
                default: statusStr = tx.getStatus().name();
            }
        }
        map.put("status", statusStr);
        map.put("gas", tx.getGasUsed() != null ? String.format("%,d", tx.getGasUsed()) : "N/A");
        map.put("time", tx.getTimestamp() != null ? tx.getTimestamp().toString().replace("T", " ") : "Vừa xong");
        map.put("rawDate", tx.getTimestamp());
        return map;
    }
}
