package com.projects.sharefiles.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for simple file uploads
                .authorizeHttpRequests(auth -> auth
                        // 1. Lock down the Admin endpoints
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 2. Allow everything else (Uploads, Downloads, UI)
                        .anyRequest().permitAll()
                )
                // 3. Enable Basic Auth (Allows us to send password via headers in JS)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("secret123") // <--- YOUR HARDCODED PASSWORD HERE
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}