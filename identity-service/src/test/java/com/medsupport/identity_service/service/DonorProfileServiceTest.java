
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.exception.ResourceConflictException;
import com.medsupport.identity_service.model.DonorProfile;
import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.DonorProfileRepository;
import com.medsupport.identity_service.repository.UserRepository;
import com.medsupport.identity_service.request.DonorProfileRequest;
import com.medsupport.identity_service.response.DonorProfileResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonorProfileServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "donor@example.com";

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DonorProfileService donorProfileService;

    private User user;
    private DonorProfile profile;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setRole("DONOR");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        user.setCprDocument("uploads/cpr/test.pdf");

        profile = new DonorProfile();
        profile.setUser(user);
        profile.setBloodType("O+");
        profile.setDateOfBirth(LocalDate.of(2000, 5, 10));
        profile.setGender("FEMALE");
        profile.setTotalDonations(0);
        profile.setAvailableForBloodDonation(false);
    }

    // TEST 1: Only donors can create profiles.
    @Test
    void shouldRejectProfileCreationForHospitalStaff() {

        user.setRole("HOSPITAL_STAFF");

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        DonorProfileRequest request =
                mock(DonorProfileRequest.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> donorProfileService.createDonorProfile(
                        EMAIL,
                        request
                )
        );

        verify(donorProfileRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 2: Reject inactive donor accounts.
    @Test
    void shouldRejectProfileCreationForInactiveDonor() {

        user.setStatus("INACTIVE");

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        DonorProfileRequest request =
                mock(DonorProfileRequest.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> donorProfileService.createDonorProfile(
                        EMAIL,
                        request
                )
        );

        verify(donorProfileRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 3: Reject unverified donor accounts.
    @Test
    void shouldRejectProfileCreationForUnverifiedDonor() {

        user.setEmailVerified(false);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        DonorProfileRequest request =
                mock(DonorProfileRequest.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> donorProfileService.createDonorProfile(
                        EMAIL,
                        request
                )
        );

        verify(donorProfileRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 4: Prevent duplicate donor profiles.
    @Test
    void shouldRejectDuplicateDonorProfile() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.existsByUserId(USER_ID))
                .thenReturn(true);

        DonorProfileRequest request =
                mock(DonorProfileRequest.class);

        assertThrows(
                ResourceConflictException.class,
                () -> donorProfileService.createDonorProfile(
                        EMAIL,
                        request
                )
        );

        verify(donorProfileRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 5: Reject missing required donor information.
    @Test
    void shouldRejectIncompleteDonorProfile() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        DonorProfileRequest request =
                mock(DonorProfileRequest.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> donorProfileService.createDonorProfile(
                        EMAIL,
                        request
                )
        );

        verify(donorProfileRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 6: Update donor profile and record Audit Log.
    @Test
    void shouldUpdateDonorProfileAndWriteAuditLog() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile));

        DonorProfileRequest request =
                mock(DonorProfileRequest.class);

        when(request.getBloodType()).thenReturn("A+");
        when(request.getDateOfBirth())
                .thenReturn(LocalDate.of(2000, 5, 10));
        when(request.getGender()).thenReturn("FEMALE");

        when(donorProfileRepository.save(profile))
                .thenReturn(profile);

        DonorProfileResponse response =
                donorProfileService.updateMyDonorProfile(
                        EMAIL,
                        request
                );

        assertNotNull(response);
        assertEquals("A+", profile.getBloodType());

        verify(donorProfileRepository).save(profile);

        verify(auditLogService).saveAuditLog(
                eq("DONOR_PROFILE_UPDATED"),
                eq(EMAIL),
                contains("Donor profile updated")
        );
    }

    // TEST 7: Prevent hospital staff from updating donor profiles.
    @Test
    void shouldRejectProfileUpdateForHospitalStaff() {

        user.setRole("HOSPITAL_STAFF");

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile));

        DonorProfileRequest request =
                mock(DonorProfileRequest.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> donorProfileService.updateMyDonorProfile(
                        EMAIL,
                        request
                )
        );

        verify(donorProfileRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 8: Reject updates when donor profile does not exist.
    @Test
    void shouldRejectUpdateWhenDonorProfileNotFound() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.empty());

        DonorProfileRequest request =
                mock(DonorProfileRequest.class);

        assertThrows(
                NoSuchElementException.class,
                () -> donorProfileService.updateMyDonorProfile(
                        EMAIL,
                        request
                )
        );

        verify(donorProfileRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 9: Reading a donor profile should not write Audit Log.
    @Test
    void shouldGetMyDonorProfileWithoutWritingAuditLog() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile));

        DonorProfileResponse response =
                donorProfileService.getMyDonorProfile(EMAIL);

        assertNotNull(response);

        verify(donorProfileRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 10: Create donor profile successfully and write Audit Log.
    @Test
    void shouldCreateDonorProfileAndWriteAuditLog() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.existsByUserId(USER_ID))
                .thenReturn(false);

        DonorProfileRequest request =
                new DonorProfileRequest();

        request.setBloodType("O+");
        request.setDateOfBirth(LocalDate.of(2000, 5, 10));
        request.setGender("FEMALE");
        request.setWeightKg(60.0);
        request.setMedicalConditions("None");
        request.setMedications("None");
        request.setAllergies("None");

        when(donorProfileRepository.save(
                any(DonorProfile.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        DonorProfile result =
                donorProfileService.createDonorProfile(
                        EMAIL,
                        request
                );

        assertNotNull(result);

        assertEquals(
                "O+",
                result.getBloodType()
        );

        assertEquals(
                60.0,
                result.getWeightKg()
        );

        assertEquals(
                0,
                result.getTotalDonations()
        );

        assertFalse(
                result.isAvailableForBloodDonation()
        );

        assertEquals(
                user.getCprDocument(),
                result.getCprImage()
        );

        verify(donorProfileRepository).save(
                any(DonorProfile.class)
        );

        verify(auditLogService).saveAuditLog(
                eq("DONOR_PROFILE_CREATED"),
                eq(EMAIL),
                contains("Donor profile created for user ID")
        );
    }

}
