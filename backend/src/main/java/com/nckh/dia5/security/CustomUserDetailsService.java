package com.nckh.dia5.security;

import com.nckh.dia5.model.DistributorUser;
import com.nckh.dia5.model.ManufacturerUser;
import com.nckh.dia5.model.PharmacyUser;
import com.nckh.dia5.model.User;
import com.nckh.dia5.repository.DistributorUserRepository;
import com.nckh.dia5.repository.ManufacturerUserRepository;
import com.nckh.dia5.repository.PharmacyUserRepository;
import com.nckh.dia5.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DistributorUserRepository distributorUserRepository;
    private final ManufacturerUserRepository manufacturerUserRepository;
    private final PharmacyUserRepository pharmacyUserRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // SIÊU TỐI ƯU: Hỗ trợ tài khoản Admin hệ thống ảo
        if ("admin@pharmachain.com".equals(email)) {
            return org.springframework.security.core.userdetails.User.builder()
                    .username("admin@pharmachain.com")
                    .password("$2a$10$8.UnVuG9HHgffUDAlk8qnO52f3Hj66i9st/+9as.f4v.L.20XwLsy") // password: admin123
                    .roles("ADMIN")
                    .build();
        }

        // First try to find in manufacturer users
        ManufacturerUser manufacturerUser = manufacturerUserRepository.findByEmail(email).orElse(null);
        if (manufacturerUser != null) {
            return manufacturerUser;
        }

        // Then try distributor users
        DistributorUser distributorUser = distributorUserRepository.findByEmail(email).orElse(null);
        if (distributorUser != null) {
            return distributorUser;
        }

        // Then try pharmacy users
        PharmacyUser pharmacyUser = pharmacyUserRepository.findByEmail(email).orElse(null);
        if (pharmacyUser != null) {
            return pharmacyUser;
        }

        // Finally try regular users
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return user;
    }

    @Transactional
    public UserDetails loadUserById(String id) {
        // SIÊU TỐI ƯU: Hỗ trợ tài khoản Admin hệ thống ảo
        if ("SYSTEM_ADMIN".equals(id) || "admin@pharmachain.com".equals(id)) {
            return org.springframework.security.core.userdetails.User.builder()
                    .username("admin@pharmachain.com")
                    .password("$2a$10$8.UnVuG9HHgffUDAlk8qnO52f3Hj66i9st/+9as.f4v.L.20XwLsy")
                    .roles("ADMIN")
                    .build();
        }

        // First try manufacturer users
        ManufacturerUser manufacturerUser = manufacturerUserRepository.findById(id).orElse(null);
        if (manufacturerUser != null) {
            return manufacturerUser;
        }

        // Then try distributor users
        DistributorUser distributorUser = distributorUserRepository.findById(id).orElse(null);
        if (distributorUser != null) {
            return distributorUser;
        }

        // Then try pharmacy users
        PharmacyUser pharmacyUser = pharmacyUserRepository.findById(Long.parseLong(id)).orElse(null);
        if (pharmacyUser != null) {
            return pharmacyUser;
        }

        // Finally try regular users
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return user;
    }
}
