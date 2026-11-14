package com.medsupport.blood_request_service.response;

import com.medsupport.blood_request_service.model.BloodRequest;
import com.medsupport.blood_request_service.model.Hospital;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BloodRequestResponseTest {
    private BloodRequest request() {
        Hospital hospital = new Hospital();
        hospital.setId(5L);
        BloodRequest request = new BloodRequest();
        request.setId(11L);
        request.setHospital(hospital);
        request.setBloodNeededDate(LocalDate.of(2099, 4, 2));
        request.setBloodRequestStatus("OPEN");
        return request;
    }

    @Test
    void mapsAllPersistedPublicFieldsAndPreservesExistingContract() {
        BloodRequest request = request();
        request.setRequiredBloodType("A+");
        request.setRequiredBloodUnits(3);
        request.setBloodRequestUrgency("HIGH");
        request.setBloodRequestDescription("Donation support requested");
        BloodRequestResponse response = BloodRequestResponse.from(request);
        assertAll(
                () -> assertEquals(11L, response.id()),
                () -> assertEquals(5L, response.hospitalId()),
                () -> assertEquals(request.getBloodNeededDate(), response.neededDate()),
                () -> assertEquals("OPEN", response.bloodRequestStatus()),
                () -> assertEquals("A+", response.requiredBloodType()),
                () -> assertEquals(3, response.requiredBloodUnits()),
                () -> assertEquals("HIGH", response.bloodRequestUrgency()),
                () -> assertEquals("Donation support requested", response.bloodRequestDescription()));
    }

    @Test
    void preservesNullFieldsWithoutInventingDefaults() {
        BloodRequestResponse response = BloodRequestResponse.from(request());
        assertAll(
                () -> assertNull(response.requiredBloodType()),
                () -> assertNull(response.requiredBloodUnits()),
                () -> assertNull(response.bloodRequestUrgency()),
                () -> assertNull(response.bloodRequestDescription()));
    }

    @Test
    void serializesOnlyTheFlatPublicResponseFields() {
        JsonMapper mapper = JsonMapper.builder().build();
        var json = mapper.readTree(mapper.writeValueAsString(
                BloodRequestResponse.from(request())));
        Set<String> fields = new java.util.HashSet<>();
        json.propertyNames().forEach(fields::add);
        assertEquals(Set.of("id", "hospitalId", "neededDate", "bloodRequestStatus",
                "requiredBloodType", "requiredBloodUnits", "bloodRequestUrgency",
                "bloodRequestDescription"), fields);
        assertEquals("2099-04-02", json.get("neededDate").asString());
        assertFalse(json.has("hospital"));
        assertFalse(json.has("donor"));
        assertFalse(json.has("user"));
    }

    @Test
    void retainsTheExistingFourArgumentJavaConstructor() {
        BloodRequestResponse response = new BloodRequestResponse(
                11L, 5L, LocalDate.of(2099, 4, 2), "OPEN");
        assertEquals(11L, response.id());
        assertEquals("OPEN", response.bloodRequestStatus());
        assertNull(response.requiredBloodType());
        assertNull(response.requiredBloodUnits());
        assertNull(response.bloodRequestUrgency());
        assertNull(response.bloodRequestDescription());
    }
}
