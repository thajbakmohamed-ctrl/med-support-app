
package com.app.med_support.response;

public class HospitalDepartmentResponse {

    private Long id;
    private String departmentName;
    private String description;
    private Long hospitalId;

    public HospitalDepartmentResponse(Long id,
            String departmentName, String description, Long hospitalId) {

        this.id = id;
        this.departmentName = departmentName;
        this.description = description;
        this.hospitalId = hospitalId;
    }

    public Long getId() {
        return id;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getDescription() {
        return description;
    }

    public Long getHospitalId() {
        return hospitalId;
    }
}
