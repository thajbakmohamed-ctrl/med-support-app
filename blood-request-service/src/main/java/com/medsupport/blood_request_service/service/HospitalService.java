
package com.medsupport.blood_request_service.service;

import com.medsupport.blood_request_service.model.Hospital;
import com.medsupport.blood_request_service.repository.HospitalRepository;
import com.medsupport.blood_request_service.request.HospitalRequest;
import com.medsupport.blood_request_service.response.HospitalResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public record Department(Long id, Long hospitalId, String name, String description) {}

    @Transactional(readOnly = true)
    public List<Department> getDepartments(Long hospitalId) {
        findHospital(hospitalId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT id, hospital_id, name, description FROM p4_hospital_department WHERE hospital_id=:hospital ORDER BY id")
                .setParameter("hospital", hospitalId).getResultList();
        return rows.stream().map(r -> new Department(((Number)r[0]).longValue(), ((Number)r[1]).longValue(),
                (String)r[2], (String)r[3])).toList();
    }

    @Transactional
    public Department createDepartment(Long hospitalId, String name, String description) {
        if (!findHospital(hospitalId).isHospitalActive())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hospital is inactive");
        Number id = (Number) entityManager.createNativeQuery(
                "INSERT INTO p4_hospital_department(hospital_id,name,description) VALUES(:hospital,:name,:description) RETURNING id")
                .setParameter("hospital", hospitalId).setParameter("name", name.trim())
                .setParameter("description", description).getSingleResult();
        return new Department(id.longValue(), hospitalId, name.trim(), description);
    }

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames={"hospitals","hospital"}, allEntries=true)
    public HospitalResponse createHospital(HospitalRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Hospital information is required");
        }

        Hospital hospital = new Hospital();

        hospital.setHospitalName(request.getHospitalName());
        hospital.setHospitalLocation(request.getHospitalLocation());
        hospital.setHospitalPhone(request.getHospitalPhone());
        hospital.setHospitalDescription(request.getHospitalDescription());


        hospital.setHospitalActive(false);
        return HospitalResponse.from(hospitalRepository.save(hospital));
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames={"hospitals","hospital"}, allEntries=true)
    public HospitalResponse updateHospital(Long hospitalId, HospitalRequest request) {

        if (hospitalId == null || request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Hospital ID and information are required");
        }

        Hospital hospital = findHospital(hospitalId);

        hospital.setHospitalName(request.getHospitalName());
        hospital.setHospitalLocation(request.getHospitalLocation());
        hospital.setHospitalPhone(request.getHospitalPhone());
        hospital.setHospitalDescription(request.getHospitalDescription());

        return HospitalResponse.from(hospitalRepository.save(hospital));
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames={"hospitals","hospital"}, allEntries=true)
    public HospitalResponse activateHospital(Long hospitalId) {
        Hospital hospital = findHospital(hospitalId);

        hospital.setHospitalActive(true);
        return HospitalResponse.from(hospitalRepository.save(hospital));
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames={"hospitals","hospital"}, allEntries=true)
    public HospitalResponse deactivateHospital(Long hospitalId) {

        Hospital hospital = findHospital(hospitalId);

        hospital.setHospitalActive(false);

        return HospitalResponse.from(hospitalRepository.save(hospital));
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames="hospital", key="#hospitalId")
    public HospitalResponse getHospitalById(Long hospitalId) {

        return HospitalResponse.from(findHospital(hospitalId));
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames="hospitals")
    public List<HospitalResponse> getAllHospitals() {

        return hospitalRepository.findAll()
                .stream()
                .map(HospitalResponse::from)
                .toList();
    }

    private Hospital findHospital(Long hospitalId) {

        if (hospitalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hospital ID is required");
        }

        return hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Hospital not found"));
    }
}
