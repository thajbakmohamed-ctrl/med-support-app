package com.app.med_support.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Pattern(
            regexp = "^[0-9]{8}$",
            message = "Phone number must be exactly 8 digits"
    )
    private String phoneNumber;
}