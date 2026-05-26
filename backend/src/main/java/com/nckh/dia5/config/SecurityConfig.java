package com.nckh.dia5.config;

import com.nckh.dia5.security.CustomUserDetailsService;
import com.nckh.dia5.security.JwtAuthenticationEntryPoint;
import com.nckh.dia5.security.JwtAuthenticationFilter;
import com.nckh.dia5.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/upload/**").permitAll() // File upload - MUST be first
                        .requestMatchers("/uploads/**").permitAll() // Static uploaded files
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/auth/**").permitAll()
                        .requestMatchers("/api/distributor/auth/**").permitAll()
                        .requestMatchers("/api/manufacturer/auth/**").permitAll()
                        .requestMatchers("/api/pharmacy/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/blockchain/drugs/debug/**").permitAll() // Debug endpoints
                        .requestMatchers("/api/products/debug/**").permitAll() // Products debug endpoints
                        .requestMatchers("/api/blockchain/drugs/batches").permitAll() // Temporary for testing
                        .requestMatchers("/api/blockchain/drugs/batches/**").permitAll() // Batch lookup for pharmacy receive
                        .requestMatchers("/api/blockchain/drugs/shipments/**").permitAll() // Temporary for testing
                        .requestMatchers("/api/blockchain/drugs/distributors").permitAll() // Get distributors for frontend
                        .requestMatchers("/api/blockchain/drugs/stats").permitAll() // Dashboard stats
                        .requestMatchers("/api/blockchain/drugs/distributor/**").permitAll() // Distributor tracking endpoints
                        .requestMatchers("/api/batches/**").permitAll() // Batch items management
                        .requestMatchers("/api/product-items/**").permitAll() // Product items
                        .requestMatchers("/api/public/verify/**").permitAll() // Public product verification
                        .requestMatchers("/api/pharmacy/inventory/**").permitAll() // Pharmacy inventory endpoints
                        .requestMatchers("/api/pharmacy/dispense-with-instructions/**").permitAll() // Pharmacy dispense with instructions
                        .requestMatchers("/api/distributor/inventory/**").permitAll() // Distributor inventory endpoints
                        .requestMatchers("/api/warehouse/**").permitAll() // Warehouse endpoints
                        .requestMatchers("/api/ai/**").permitAll() // AI drug consultation endpoints
                        .requestMatchers("/api/app/**").permitAll() // Mobile app endpoints
                        .requestMatchers("/api/v1/admin/**").permitAll() // Admin Dashboard
                        .requestMatchers("/error").permitAll() // Error page
                        // All other endpoints require authentication
                        .anyRequest().authenticated());

        // Enable authentication
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
