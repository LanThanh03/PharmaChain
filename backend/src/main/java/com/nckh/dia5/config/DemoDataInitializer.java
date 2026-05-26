package com.nckh.dia5.config;

import com.nckh.dia5.model.DistributorUser;
import com.nckh.dia5.model.ManufacturerUser;
import com.nckh.dia5.repository.DistributorUserRepository;
import com.nckh.dia5.repository.ManufacturerUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"default", "dev"})
public class DemoDataInitializer implements ApplicationRunner {

    private final ManufacturerUserRepository manufacturerUserRepository;
    private final DistributorUserRepository distributorUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            manufacturerUserRepository.findByEmail("manufacturer@demo.com").ifPresent(user -> {
                user.setPasswordHash(passwordEncoder.encode("demo123"));
                manufacturerUserRepository.save(user);
                log.info("Initialized demo manufacturer password");
            });

            distributorUserRepository.findByEmail("distributor@demo.com").ifPresent(user -> {
                user.setPasswordHash(passwordEncoder.encode("demo123"));
                distributorUserRepository.save(user);
                log.info("Initialized demo distributor password");
            });
        } catch (Exception e) {
            log.warn("Failed to initialize demo passwords: {}", e.getMessage());
        }
    }
}


