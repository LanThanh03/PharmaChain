package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.DrugProduct;
import com.nckh.dia5.model.ManufacturerUser;
import com.nckh.dia5.model.PharmaCompany;
import com.nckh.dia5.repository.DrugProductRepository;
import com.nckh.dia5.repository.PharmaCompanyRepository;
import com.nckh.dia5.service.ManufacturerAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class DrugProductController {

    private final DrugProductRepository drugProductRepository;
    private final PharmaCompanyRepository pharmaCompanyRepository;
    private final com.nckh.dia5.repository.DrugBatchRepository drugBatchRepository;
    private final ManufacturerAuthService manufacturerAuthService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DrugProduct>>> getAll() {
        ManufacturerUser current = manufacturerAuthService.getCurrentUser();
        if (current == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập", 401));
        }
        
        log.info("Current user: {} (Company: {}, ID: {})", current.getName(), current.getCompanyName(), current.getId());
        
        // First try to find PharmaCompany by manufacturerUserId
        PharmaCompany pharmaCompany = pharmaCompanyRepository.findByManufacturerUserId(current.getId()).orElse(null);
        Long manufacturerId = null;
        
        if (pharmaCompany != null) {
            manufacturerId = pharmaCompany.getId();
            log.info("Found PharmaCompany by userId: {} with ID: {}", pharmaCompany.getName(), manufacturerId);
        } else {
            // Fallback: try to find by company name match
            List<PharmaCompany> companies = pharmaCompanyRepository.findAll();
            for (PharmaCompany company : companies) {
                // Match by company name (case-insensitive, partial match)
                if (current.getCompanyName() != null && 
                    (company.getName().toLowerCase().contains(current.getCompanyName().toLowerCase()) ||
                     current.getCompanyName().toLowerCase().contains(company.getName().toLowerCase()))) {
                    manufacturerId = company.getId();
                    log.info("Matched by name: '{}' matches '{}' with ID: {}", 
                             current.getCompanyName(), company.getName(), manufacturerId);
                    
                    // Update the PharmaCompany to link with this user
                    company.setManufacturerUserId(current.getId());
                    pharmaCompanyRepository.save(company);
                    log.info("Linked PharmaCompany {} to user {}", company.getName(), current.getId());
                    break;
                }
            }
        }
        
        List<DrugProduct> products;
        if (manufacturerId != null) {
            products = drugProductRepository.findAllByManufacturerId(manufacturerId);
            log.info("Found {} products for manufacturer ID: {}", products.size(), manufacturerId);
        } else {
            // If no match found, show all products as fallback
            products = drugProductRepository.findAll();
            log.info("No manufacturer match found for '{}', showing all {} products", 
                     current.getCompanyName(), products.size());
        }

        // Set total batches and produced explicitly
        String walletAddress = current.getWalletAddress(); // get manufacturer wallet address
        if (walletAddress != null) {
            for (DrugProduct p : products) {
                Long batches = drugBatchRepository.countByDrugNameAndManufacturerAddress(p.getName(), walletAddress);
                Long produced = drugBatchRepository.sumQuantityByDrugNameAndManufacturerAddress(p.getName(), walletAddress);
                p.setTotalBatches(batches != null ? batches : 0L);
                p.setTotalProduced(produced != null ? produced : 0L);
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success(products, "Lấy danh sách sản phẩm thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DrugProduct>> create(@RequestBody DrugProduct product) {
        try {
            log.info("Creating product: {}", product.getName());
            
            ManufacturerUser current = manufacturerAuthService.getCurrentUser();
            if (current == null) {
                log.warn("Unauthorized product creation attempt");
                return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập", 401));
            }
            
            log.info("Current manufacturer: {} ({})", current.getName(), current.getId());
            
            // Validate required fields
            if (product.getName() == null || product.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Tên sản phẩm là bắt buộc", 400));
            }
            
            // Ensure PharmaCompany exists for this manufacturer
            Long manufacturerId = ensurePharmaCompanyExists(current);
            product.setManufacturerId(manufacturerId);
            DrugProduct saved = drugProductRepository.save(product);
            
            log.info("Product created successfully: {} with ID: {}", saved.getName(), saved.getId());
            return ResponseEntity.ok(ApiResponse.success(saved, "Tạo sản phẩm thành công"));
            
        } catch (Exception e) {
            log.error("Error creating product: ", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DrugProduct>> update(@PathVariable Long id, @RequestBody DrugProduct product) {
        try {
            log.info("Updating product ID: {}", id);
            
            ManufacturerUser current = manufacturerAuthService.getCurrentUser();
            if (current == null) {
                log.warn("Unauthorized update attempt");
                return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập", 401));
            }
            
            // Find the product by ID first
            return drugProductRepository.findById(id)
                    .map(existing -> {
                        // Verify ownership by checking manufacturerId
                        Long currentManufacturerId = ensurePharmaCompanyExists(current);
                        
                        if (existing.getManufacturerId() != null && 
                            !existing.getManufacturerId().equals(currentManufacturerId)) {
                            log.warn("User {} attempted to update product {} owned by different manufacturer", 
                                     current.getId(), id);
                            return ResponseEntity.status(403)
                                    .<ApiResponse<DrugProduct>>body(ApiResponse.error("Không có quyền cập nhật sản phẩm này", 403));
                        }
                        
                        // Update fields
                        if (product.getName() != null) existing.setName(product.getName());
                        if (product.getActiveIngredient() != null) existing.setActiveIngredient(product.getActiveIngredient());
                        if (product.getDosage() != null) existing.setDosage(product.getDosage());
                        if (product.getUnit() != null) existing.setUnit(product.getUnit());
                        if (product.getCategory() != null) existing.setCategory(product.getCategory());
                        if (product.getDescription() != null) existing.setDescription(product.getDescription());
                        if (product.getStorageConditions() != null) existing.setStorageConditions(product.getStorageConditions());
                        if (product.getShelfLife() != null) existing.setShelfLife(product.getShelfLife());
                        if (product.getStatus() != null) existing.setStatus(product.getStatus());
                        if (product.getImageUrl() != null) existing.setImageUrl(product.getImageUrl());
                        
                        DrugProduct updated = drugProductRepository.save(existing);
                        log.info("Product updated successfully: {} (ID: {})", updated.getName(), updated.getId());
                        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật sản phẩm thành công"));
                    })
                    .orElseGet(() -> {
                        log.warn("Product not found: {}", id);
                        return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy sản phẩm", 404));
                    });
                    
        } catch (Exception e) {
            log.error("Error updating product: ", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        try {
            log.info("Deleting product ID: {}", id);
            
            ManufacturerUser current = manufacturerAuthService.getCurrentUser();
            if (current == null) {
                log.warn("Unauthorized delete attempt");
                return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập", 401));
            }
            
            return drugProductRepository.findById(id)
                    .map(existing -> {
                        // Verify ownership
                        Long currentManufacturerId = ensurePharmaCompanyExists(current);
                        
                        if (existing.getManufacturerId() != null && 
                            !existing.getManufacturerId().equals(currentManufacturerId)) {
                            log.warn("User {} attempted to delete product {} owned by different manufacturer", 
                                     current.getId(), id);
                            return ResponseEntity.status(403)
                                    .<ApiResponse<String>>body(ApiResponse.error("Không có quyền xóa sản phẩm này", 403));
                        }
                        
                        drugProductRepository.delete(existing);
                        log.info("Product deleted successfully: {}", id);
                        return ResponseEntity.ok(ApiResponse.success("", "Xóa sản phẩm thành công"));
                    })
                    .orElseGet(() -> {
                        log.warn("Product not found: {}", id);
                        return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy sản phẩm", 404));
                    });
                    
        } catch (Exception e) {
            log.error("Error deleting product: ", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/debug/all")
    public ResponseEntity<ApiResponse<Object>> debugAll() {
        java.util.Map<String, Object> debugInfo = new java.util.HashMap<>();
        
        // Check all companies
        List<PharmaCompany> allCompanies = pharmaCompanyRepository.findAll();
        debugInfo.put("allCompanies", allCompanies);
        
        // Check all products
        List<DrugProduct> allProducts = drugProductRepository.findAll();
        debugInfo.put("allProducts", allProducts);
        
        return ResponseEntity.ok(ApiResponse.success(debugInfo, "Debug info"));
    }
    
    /**
     * Get or create PharmaCompany for the manufacturer user
     * Returns the PharmaCompany ID
     */
    private Long ensurePharmaCompanyExists(ManufacturerUser user) {
        // First try to find by manufacturerUserId
        PharmaCompany pharmaCompany = pharmaCompanyRepository.findByManufacturerUserId(user.getId())
                .orElse(null);
        
        if (pharmaCompany != null) {
            return pharmaCompany.getId();
        }
        
        // If not found, try to find by name match
        List<PharmaCompany> companies = pharmaCompanyRepository.findAll();
        for (PharmaCompany company : companies) {
            if (user.getCompanyName() != null && 
                (company.getName().toLowerCase().contains(user.getCompanyName().toLowerCase()) ||
                 user.getCompanyName().toLowerCase().contains(company.getName().toLowerCase()))) {
                // Update the company to link with this user
                company.setManufacturerUserId(user.getId());
                pharmaCompanyRepository.save(company);
                log.info("Linked PharmaCompany {} (ID: {}) to user {}", company.getName(), company.getId(), user.getId());
                return company.getId();
            }
        }
        
        // If still not found, create new PharmaCompany (let database generate ID)
        PharmaCompany newCompany = new PharmaCompany();
        newCompany.setName(user.getCompanyName() != null ? user.getCompanyName() : user.getName());
        newCompany.setCompanyType(PharmaCompany.CompanyType.MANUFACTURER);
        newCompany.setAddress(user.getCompanyAddress());
        newCompany.setEmail(user.getEmail());
        newCompany.setLicenseNumber(user.getLicenseNumber());
        newCompany.setWalletAddress(user.getWalletAddress());
        newCompany.setManufacturerUserId(user.getId());
        newCompany.setStatus("ACTIVE");
        
        PharmaCompany saved = pharmaCompanyRepository.save(newCompany);
        log.info("Created new PharmaCompany for manufacturer: {} with ID: {}", user.getName(), saved.getId());
        
        return saved.getId();
    }
}


