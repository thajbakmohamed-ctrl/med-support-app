package com.medsupport.identity_service.config;

import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDataSeederTest {
    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "unit-test-only-password";
    private static final String PHONE = "12345678";

    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    // No component scanning, auto-configuration, SpringApplication, or runner execution.
    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withPropertyValues("app.admin.seed.enabled=")
                .withBean(UserRepository.class, () -> repository)
                .withBean(PasswordEncoder.class, () -> encoder)
                .withUserConfiguration(AdminDataSeeder.class);
    }

    @Test
    void shouldBeDisabledWhenPropertyIsMissing() {
        // Use an empty environment so a developer's ADMIN_SEED_ENABLED cannot affect this test.
        new ApplicationContextRunner()
                .withInitializer(context -> {
                    context.getEnvironment().getPropertySources().remove("systemProperties");
                    context.getEnvironment().getPropertySources().remove("systemEnvironment");
                })
                .withBean(UserRepository.class, () -> repository)
                .withBean(PasswordEncoder.class, () -> encoder)
                .withUserConfiguration(AdminDataSeeder.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.getBeansOfType(AdminDataSeeder.class).isEmpty());
                    assertTrue(context.getBeansOfType(CommandLineRunner.class).isEmpty());
                    verifyNoInteractions(repository, encoder);
                });
    }

    @Test
    void shouldBeDisabledWhenPropertyIsFalse() {
        contextRunner().withPropertyValues("app.admin.seed.enabled=false")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.getBeansOfType(CommandLineRunner.class).isEmpty());
                    verifyNoInteractions(repository, encoder);
                });
    }

    @Test
    void shouldRegisterRunnerOnlyWhenExplicitlyEnabled() {
        contextRunner().withPropertyValues("app.admin.seed.enabled=true")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(AdminDataSeeder.class).size());
                    assertEquals(1, context.getBeansOfType(CommandLineRunner.class).size());
                    // Registering a runner must not execute it in this isolated context.
                    verifyNoInteractions(repository, encoder);
                });
    }

    @Test
    void shouldCreateAdministratorWithEncodedPasswordAndOriginalFlags() throws Exception {
        when(encoder.encode(PASSWORD)).thenReturn("unit-test-encoded-password");
        runner(credentials()).run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        InOrder order = inOrder(repository, encoder);
        order.verify(repository).existsByEmail(EMAIL);
        order.verify(repository).existsByPhoneNumber(PHONE);
        order.verify(encoder).encode(PASSWORD);
        order.verify(repository).save(saved.capture());

        User admin = saved.getValue();
        assertEquals("System Administrator", admin.getName());
        assertEquals(EMAIL, admin.getEmail());
        assertEquals(PHONE, admin.getPhoneNumber());
        assertEquals("unit-test-encoded-password", admin.getHashedPassword());
        assertEquals("ADMIN", admin.getRole());
        assertEquals("ACTIVE", admin.getStatus());
        assertTrue(admin.isEmailVerified());
    }

    @Test
    void shouldLeaveExistingAdministratorUnchanged() throws Exception {
        when(repository.existsByEmail(EMAIL)).thenReturn(true);
        runner(credentials()).run();
        verify(repository).existsByEmail(EMAIL);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(encoder);
    }

    @Test
    void shouldRejectExistingPhoneWithoutEncodingOrSaving() {
        when(repository.existsByPhoneNumber(PHONE)).thenReturn(true);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> runner(credentials()).run());
        assertEquals("Admin phone number already exists", failure.getMessage());
        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void shouldRejectEachMissingCredentialBeforeDatabaseAccess() {
        for (String key : credentials().keySet()) {
            Map<String, String> values = credentials();
            values.remove(key);
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> runner(values).run());
            assertEquals("Admin configuration is missing", failure.getMessage());
        }
        verifyNoInteractions(repository, encoder);
    }

    @Test
    void shouldRejectEachBlankCredentialBeforeDatabaseAccess() {
        for (String key : credentials().keySet()) {
            Map<String, String> values = credentials();
            values.put(key, " ");
            assertThrows(IllegalStateException.class, () -> runner(values).run());
        }
        verifyNoInteractions(repository, encoder);
    }

    private CommandLineRunner runner(Map<String, String> values) {
        return new AdminDataSeeder().seedAdmin(repository, encoder, values::get);
    }

    private Map<String, String> credentials() {
        return new HashMap<>(Map.of(
                "ADMIN_EMAIL", EMAIL,
                "ADMIN_PASSWORD", PASSWORD,
                "ADMIN_PHONE", PHONE));
    }
}
