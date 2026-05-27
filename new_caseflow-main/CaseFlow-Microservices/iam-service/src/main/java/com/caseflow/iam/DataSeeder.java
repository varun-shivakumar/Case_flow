package com.caseflow.iam;

import com.caseflow.iam.entity.User;
import com.caseflow.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j @Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@caseflow.com")) {
            String adminId = "SUP_ADMIN_" + (userRepository.countByRole(User.Role.ADMIN) + 1);
            userRepository.save(User.builder()
                .userId(adminId)
                .name("Super Admin").email("admin@caseflow.com").phone("9000000001")
                .password(passwordEncoder.encode("admin123"))
                .role(User.Role.ADMIN).status(User.Status.ACTIVE).build());
            log.info(">>> Admin user seeded successfully");
        }
    }
}
