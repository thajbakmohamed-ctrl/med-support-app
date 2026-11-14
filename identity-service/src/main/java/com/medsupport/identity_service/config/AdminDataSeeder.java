package com.medsupport.identity_service.config;

import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.function.Function;

@Configuration
@ConditionalOnProperty(prefix = "app.admin.seed", name = "enabled",
        havingValue = "true", matchIfMissing = false)

public class AdminDataSeeder {

    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {

        return seedAdmin(userRepository, passwordEncoder, System::getenv);
    }

    // Allows isolated tests to supply credentials without reading or changing real environment values.
    CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder,
            Function<String, String> environment) {

        return args -> {

            String email = environment.apply("ADMIN_EMAIL");
            String password = environment.apply("ADMIN_PASSWORD");
            String phone = environment.apply("ADMIN_PHONE");

            System.out.println("ADMIN EMAIL EXISTS: " + (email != null && !email.isBlank()));
            System.out.println("ADMIN PASSWORD EXISTS: " + (password != null && !password.isBlank()));
            System.out.println("ADMIN PHONE EXISTS: " + (phone != null && !phone.isBlank()));


            if (email == null || email.isBlank() || password == null || password.isBlank() || phone == null || phone.isBlank()) {

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

