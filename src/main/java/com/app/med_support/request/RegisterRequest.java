package com.app.med_support.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    // these the informations thats allowed to the user to send them while registration
    private String name;
    private String email;
    // THE REASON THAT I PUT STRING HERE IS BEC THE USER WHEN ITS
    // TIME FOR THE REG IT ENTER THE NORMAL PASS THEN IN THE
    // SERVICE WE WILL MAKE HASHING
    private String password;
    private String phoneNumber;
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
