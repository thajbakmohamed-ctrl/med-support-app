package com.app.med_support.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    // these the informations thats allowed to the user to send them while registration
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    // THE REASON THAT I PUT STRING HERE IS BEC THE USER WHEN ITS
    // TIME FOR THE REG IT ENTER THE NORMAL PASS THEN IN THE
    // SERVICE WE WILL MAKE HASHING
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{8}$", message = "Phone number must be exactly 8 digits")
    private String phoneNumber;
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(DONOR|HOSPITAL_STAFF)$",
            message = "Role must be DONOR or HOSPITAL_STAFF"
    )
    private String role;
    //these informations the user cant control them
    //id
    //hashedPassword
    //status
    //emailVerified
    //hospital
    //createdAt
    //updatedAt
}
