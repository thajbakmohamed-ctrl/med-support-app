package com.medsupport.identity_service.config;

import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration

public class AdminDataSeeder {

    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {


            String email = System.getenv("ADMIN_EMAIL");
            String password = System.getenv("ADMIN_PASSWORD");
            String phone = System.getenv("ADMIN_PHONE");

            // Check if environment variables are available
            System.out.println("ADMIN EMAIL EXISTS: " + (email != null && !email.isBlank()));
            System.out.println("ADMIN PASSWORD EXISTS: " + (password != null && !password.isBlank()));
            System.out.println("ADMIN PHONE EXISTS: " + (phone != null && !phone.isBlank()));


            if (email == null || email.isBlank()
                    || password == null || password.isBlank() || phone == null || phone.isBlank()) {

                throw new IllegalStateException("Admin configuration is missing");
            }

            if (userRepository.existsByEmail(email)) {
                return;
            }


            if (userRepository.existsByPhoneNumber(phone)) {
                throw new IllegalStateException("Admin phone number already exists");
            }


            User admin = new User();

            admin.setName("System Administrator");
            admin.setEmail(email);
            admin.setPhoneNumber(phone);
            admin.setHashedPassword(passwordEncoder.encode(password));
            admin.setRole("ADMIN");
            admin.setStatus("ACTIVE");
            admin.setEmailVerified(true);

            userRepository.save(admin);
        };
    }
}

